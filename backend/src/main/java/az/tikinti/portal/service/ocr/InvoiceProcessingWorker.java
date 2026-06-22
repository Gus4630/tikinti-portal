package az.tikinti.portal.service.ocr;

import static az.tikinti.portal.model.constant.CbarConstants.BASE_CURRENCY;
import static az.tikinti.portal.model.enums.ExpenseStatus.APPROVED;
import static az.tikinti.portal.model.enums.ExpenseStatus.DISPUTED;
import static az.tikinti.portal.model.enums.ExpenseStatus.OCR_PROCESSING;
import static az.tikinti.portal.model.enums.ExpenseStatus.PENDING_REVIEW;
import static az.tikinti.portal.model.enums.SupplierType.COMPANY;

import az.tikinti.portal.config.RabbitConfig;
import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import az.tikinti.portal.dao.repository.category.CategoryRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.repository.supplier.SupplierRepository;
import az.tikinti.portal.event.ExpenseDisputedEvent;
import az.tikinti.portal.event.ExpensePendingReviewEvent;
import az.tikinti.portal.exception.NonRetryableOcrException;
import az.tikinti.portal.mapper.expense.ExpenseItemMapper;
import az.tikinti.portal.model.dto.record.InvoiceMessage;
import az.tikinti.portal.model.dto.record.OcrExtractionResult;
import az.tikinti.portal.service.file.FileStorageService;
import az.tikinti.portal.service.fx.CurrencyRateService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceProcessingWorker {

    private static final int ANOMALY_MIN_SAMPLES = 3;
    private static final double ANOMALY_MAD_MULTIPLIER = 3.0;

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final FileStorageService fileStorageService;
    private final CurrencyRateService currencyRateService;
    private final ExpenseItemMapper expenseItemMapper;
    private final GeminiFlashLiteExtractionService primaryOcr;
    private final GeminiFlashExtractionService fallbackOcr;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(noRollbackFor = AmqpRejectAndDontRequeueException.class)
    @RabbitListener(queues = RabbitConfig.INVOICE_QUEUE)
    public void process(InvoiceMessage message) {
        MDC.put("traceId", UUID.randomUUID().toString());
        try {
            doProcess(message);
        } finally {
            MDC.remove("traceId");
        }
    }

    private void doProcess(InvoiceMessage message) {
        log.info("Processing invoice for expense {}", message.expenseId());

        var expense = expenseRepository.findById(message.expenseId())
                .orElseThrow(() -> new AmqpRejectAndDontRequeueException(
                        "Expense not found: " + message.expenseId()));

        expense.setStatus(OCR_PROCESSING);
        expenseRepository.save(expense);

        byte[] fileBytes;
        try {
            fileBytes = fileStorageService.readBytes(message.filePath());
        } catch (NonRetryableOcrException e) {
            log.error("Cannot read invoice file {}: {}", message.filePath(), e.getMessage());
            expense.setStatus(PENDING_REVIEW);
            expense.setNotes("File read error: " + e.getMessage());
            expenseRepository.save(expense);
            throw new AmqpRejectAndDontRequeueException("Cannot read file: " + message.filePath(), e);
        }

        String mimeType = detectMimeType(message.filePath());
        var categories = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();

        OcrExtractionResult result;
        try {
            result = primaryOcr.extract(fileBytes, mimeType, categories);
            if (result.hasLowConfidence()) {
                log.info("Low confidence on primary OCR for expense {}, using fallback", message.expenseId());
                result = fallbackOcr.extract(fileBytes, mimeType, categories);
            }
        } catch (NonRetryableOcrException e) {
            log.error("Non-retryable OCR failure for expense {}: {}", message.expenseId(), e.getMessage());
            expense.setStatus(PENDING_REVIEW);
            expense.setNotes("OCR failed: " + e.getMessage());
            expenseRepository.save(expense);
            throw new AmqpRejectAndDontRequeueException("Non-retryable OCR error", e);
        } catch (Exception e) {
            log.warn("Primary OCR failed for expense {}, trying fallback: {}", message.expenseId(), e.getMessage());
            try {
                result = fallbackOcr.extract(fileBytes, mimeType, categories);
            } catch (Exception fe) {
                log.error("Fallback OCR also failed for expense {}: {}", message.expenseId(), fe.getMessage());
                expense.setStatus(PENDING_REVIEW);
                expense.setNotes("OCR failed: " + fe.getMessage());
                expenseRepository.save(expense);
                throw new AmqpRejectAndDontRequeueException("OCR failed on both primary and fallback", fe);
            }
        }

        hydrateExpense(expense, result);
        checkAnomaly(expense);

        expenseRepository.save(expense);
        log.info("Processed invoice for expense {} → status {}", expense.getId(), expense.getStatus());

        publishEvent(expense);
    }

    private void hydrateExpense(ExpenseEntity expense, OcrExtractionResult result) {
        if (result.amount() != null && result.amount().compareTo(BigDecimal.ZERO) > 0) {
            expense.setAmount(result.amount());
        }

        if (result.invoiceDate() != null) {
            expense.setExpenseDate(result.invoiceDate());
        }

        if (result.currency() != null) {
            String currency = result.currency().toUpperCase();
            expense.setCurrency(currency);

            if (!BASE_CURRENCY.equals(currency) && expense.getAmount() != null) {
                try {
                    BigDecimal[] fx = currencyRateService.exchangeCurrency(
                            currency, BASE_CURRENCY,
                            expense.getAmount(),
                            expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now());
                    expense.setAmountBaseCurrency(fx[0]);
                    expense.setExchangeRate(fx[1]);
                } catch (Exception e) {
                    log.warn("FX conversion failed for expense {}: {}", expense.getId(), e.getMessage());
                    expense.setAmountBaseCurrency(expense.getAmount());
                    expense.setExchangeRate(BigDecimal.ONE);
                }
            } else if (expense.getAmount() != null) {
                expense.setAmountBaseCurrency(expense.getAmount());
                expense.setExchangeRate(BigDecimal.ONE);
            }
        }

        if (result.suggestedCategoryCode() != null) {
            categoryRepository.findByItemCode(result.suggestedCategoryCode())
                    .ifPresent(expense::setCategory);
        }

        if (result.supplierName() != null && !result.supplierName().isBlank()) {
            var supplier = supplierRepository.findByNameIgnoreCase(result.supplierName())
                    .orElseGet(() -> {
                        var builder = SupplierEntity.builder()
                                .name(result.supplierName())
                                .supplierType(COMPANY)
                                .totalAdvancedPaid(BigDecimal.ZERO)
                                .retainagePercentage(BigDecimal.ZERO)
                                .retainageHeldAmount(BigDecimal.ZERO);
                        if (result.supplierTaxOrNationalId() != null) {
                            builder.taxId(result.supplierTaxOrNationalId());
                        }
                        return supplierRepository.save(builder.build());
                    });
            expense.setSupplier(supplier);
        }

        if (result.lineItems() != null && !result.lineItems().isEmpty()) {
            if (expense.getCategory() != null) {
                var items = new ArrayList<>(result.lineItems().stream()
                        .map(li -> expenseItemMapper.toEntity(li, expense))
                        .toList());
                expense.getItems().clear();
                expense.getItems().addAll(items);
            } else {
                String notes = result.lineItems().stream()
                        .map(li -> li.description() +
                                (li.quantity() > 1 ? " x" + li.quantity() : "") +
                                (li.unitPrice().compareTo(BigDecimal.ZERO) > 0 ? " @ " + li.unitPrice() : ""))
                        .collect(Collectors.joining("; "));
                expense.setNotes(notes);
            }
        }

        if (expense.getStatus() != DISPUTED) {
            expense.setStatus(PENDING_REVIEW);
        }
    }

    private void checkAnomaly(ExpenseEntity expense) {
        if (expense.getCategory() == null) return;

        List<BigDecimal> amounts = expenseRepository.findAmountsInBaseCurrencyByCategory(
                expense.getCategory().getId(), APPROVED);

        if (amounts.size() < ANOMALY_MIN_SAMPLES) return;

        BigDecimal median = median(amounts);
        BigDecimal mad = mad(amounts, median);
        BigDecimal threshold = mad.multiply(BigDecimal.valueOf(ANOMALY_MAD_MULTIPLIER));

        if (expense.getAmountBaseCurrency() != null &&
                expense.getAmountBaseCurrency().subtract(median).abs().compareTo(threshold) > 0) {
            expense.setStatus(DISPUTED);
            log.warn("Expense {} flagged DISPUTED via OCR worker — anomaly detected", expense.getId());
        }
    }

    private void publishEvent(ExpenseEntity expense) {
        String buildingName = expense.getBuilding() != null ? expense.getBuilding().getName() : "Unknown";
        String categoryName = expense.getCategory() != null ? expense.getCategory().getItemName() : "Unknown";
        String amount = expense.getAmountBaseCurrency() != null
                ? expense.getAmountBaseCurrency().toPlainString() : "0";

        if (expense.getStatus() == DISPUTED) {
            eventPublisher.publishEvent(new ExpenseDisputedEvent(
                    expense.getId(), buildingName, categoryName, amount, expense.getNotes()));
        } else if (expense.getStatus() == PENDING_REVIEW) {
            eventPublisher.publishEvent(new ExpensePendingReviewEvent(
                    expense.getId(), buildingName, categoryName, amount));
        }
    }

    private String detectMimeType(String filePath) {
        try {
            String detected = Files.probeContentType(Paths.get(filePath));
            return detected != null ? detected : "application/octet-stream";
        } catch (Exception e) {
            String lower = filePath.toLowerCase();
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }

    private BigDecimal median(List<BigDecimal> sorted) {
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return sorted.get(mid - 1).add(sorted.get(mid))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return sorted.get(mid);
    }

    private BigDecimal mad(List<BigDecimal> values, BigDecimal median) {
        return median(values.stream()
                .map(v -> v.subtract(median).abs())
                .sorted()
                .toList());
    }
}
