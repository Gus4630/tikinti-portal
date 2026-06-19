package az.tikinti.portal.service.invoice;

import az.tikinti.portal.config.RabbitConfig;
import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.repository.category.CategoryRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.exception.AlreadyExistsException;
import az.tikinti.portal.exception.CommonException;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.exception.model.constant.ErrorCode;
import az.tikinti.portal.exception.model.constant.ErrorMessage;
import az.tikinti.portal.model.dto.record.InvoiceMessage;
import az.tikinti.portal.model.enums.ExpenseStatus;
import az.tikinti.portal.service.building.BuildingService;
import az.tikinti.portal.service.file.FileStorageService;
import az.tikinti.portal.util.HashUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceUploadService {

    private final FileStorageService fileStorageService;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final BuildingService buildingService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public UUID upload(MultipartFile file, UUID buildingId, UUID categoryId) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new CommonException(ErrorCode.BAD_REQUEST, "Cannot read uploaded file: " + e.getMessage());
        }

        String contentHash = HashUtil.sha256Hex(bytes);
        expenseRepository.findByContentHash(contentHash).ifPresent(existing ->
                { throw AlreadyExistsException.of(ErrorMessage.DUPLICATE_INVOICE_MESSAGE, existing.getId()); });

        var building = buildingService.getExistingEntity(buildingId);
        var category = categoryId != null
                ? categoryRepository.findById(categoryId)
                        .orElseThrow(() -> DataNotFoundException.of("Category {0} not found", categoryId))
                : null;

        String filePath = fileStorageService.store(file);

        var expense = ExpenseEntity.builder()
                .building(building)
                .category(category)
                .amount(BigDecimal.ZERO)
                .currency("AZN")
                .exchangeRate(BigDecimal.ONE)
                .amountBaseCurrency(BigDecimal.ZERO)
                .contentHash(contentHash)
                .imageUrl(filePath)
                .status(ExpenseStatus.UPLOADED)
                .expenseDate(LocalDate.now())
                .build();

        var saved = expenseRepository.save(expense);
        UUID expenseId = saved.getId();
        log.info("Invoice saved for expense {}, will queue after commit", expenseId);

        // Publish AFTER the transaction commits so the worker can find the expense in DB
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(
                        RabbitConfig.INVOICE_EXCHANGE,
                        RabbitConfig.INVOICE_QUEUE,
                        new InvoiceMessage(expenseId, filePath));
                log.info("Invoice queued for OCR: expense {}", expenseId);
            }
        });

        return expenseId;
    }
}
