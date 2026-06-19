package az.tikinti.portal.service.expense;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.DUPLICATE_INVOICE_MESSAGE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.EXPENSE_NOT_FOUND_MESSAGE;
import static az.tikinti.portal.model.constant.CbarConstants.BASE_CURRENCY;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseMediaEntity;
import az.tikinti.portal.dao.repository.expense.ExpenseMediaRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.specification.ExpenseSpecification;
import az.tikinti.portal.event.BudgetThresholdEvent;
import az.tikinti.portal.event.ExpenseDisputedEvent;
import az.tikinti.portal.exception.AlreadyExistsException;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.expense.ExpenseMapper;
import az.tikinti.portal.model.dto.request.expense.ExpenseFilterRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseMediaRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.record.ExpenseAuditResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseMediaResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import az.tikinti.portal.model.enums.ExpenseStatus;
import az.tikinti.portal.service.building.BuildingService;
import az.tikinti.portal.dao.repository.category.CategoryRepository;
import az.tikinti.portal.service.category.CategoryService;
import az.tikinti.portal.service.fx.CurrencyRateService;
import az.tikinti.portal.service.supplier.SupplierService;
import az.tikinti.portal.util.PageUtil;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExpenseService {

    private static final int ANOMALY_MIN_SAMPLES = 3;
    private static final double ANOMALY_MAD_MULTIPLIER = 3.0;

    private final ExpenseRepository expenseRepository;
    private final ExpenseMediaRepository expenseMediaRepository;
    private final ExpenseMapper expenseMapper;
    private final BuildingService buildingService;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final SupplierService supplierService;
    private final CurrencyRateService currencyRateService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public PageableResponse<ExpenseResponse> search(ExpenseFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);
        var spec = ExpenseSpecification.getSpecification(request);
        var page = expenseRepository.findAll(spec, pageable);
        var response = expenseMapper.toResponse(page);
        response.getContent().forEach(e -> e.setMedia(getMediaForExpense(e.getId())));
        return response;
    }

    public ExpenseResponse getById(UUID id) {
        var response = expenseMapper.toResponse(getExistingEntity(id));
        response.setMedia(getMediaForExpense(id));
        return response;
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        // SHA-256 dedup based on a stable content key (building + category + amount + date + supplier)
        String contentKey = buildContentKey(request);
        String contentHash = az.tikinti.portal.util.HashUtil.sha256Hex(
                contentKey.getBytes(StandardCharsets.UTF_8));

        expenseRepository.findByContentHash(contentHash).ifPresent(existing -> {
            throw AlreadyExistsException.of(DUPLICATE_INVOICE_MESSAGE, existing.getId());
        });

        var building = buildingService.getExistingEntity(request.getBuildingId());
        var category = categoryService.getExistingEntity(request.getCategoryId());

        // FX conversion
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal amountBase = request.getAmount();
        String currency = request.getCurrency().toUpperCase();

        if (!BASE_CURRENCY.equals(currency)) {
            BigDecimal[] result = currencyRateService.exchangeCurrency(
                    currency, BASE_CURRENCY, request.getAmount(), request.getExpenseDate());
            amountBase = result[0];
            exchangeRate = result[1];
        }

        var entity = ExpenseEntity.builder()
                .building(building)
                .category(category)
                .supplier(request.getSupplierId() != null
                        ? supplierService.getExistingEntity(request.getSupplierId()) : null)
                .amount(request.getAmount())
                .currency(currency)
                .exchangeRate(exchangeRate)
                .amountBaseCurrency(amountBase)
                .contentHash(contentHash)
                .status(ExpenseStatus.APPROVED)
                .notes(request.getNotes())
                .expenseDate(request.getExpenseDate())
                .build();

        var saved = expenseRepository.save(entity);

        if (request.getMedia() != null) {
            saveMedia(saved, request.getMedia());
        }

        // Anomaly detection — may flip status to DISPUTED
        checkAnomaly(saved);

        if (saved.getStatus() == ExpenseStatus.APPROVED) {
            checkBudgetThreshold(saved);
        }

        log.info("Created Expense {} for building {} category {}", saved.getId(),
                request.getBuildingId(), category.getItemCode());

        var response = expenseMapper.toResponse(saved);
        response.setMedia(getMediaForExpense(saved.getId()));
        return response;
    }

    @Transactional
    public ExpenseResponse approve(UUID id) {
        var entity = getExistingEntity(id);
        entity.setStatus(ExpenseStatus.APPROVED);
        log.info("Approved Expense {}", id);
        checkBudgetThreshold(entity);
        return buildResponse(entity);
    }

    @Transactional
    public ExpenseResponse dispute(UUID id, String reason) {
        var entity = getExistingEntity(id);
        entity.setStatus(ExpenseStatus.DISPUTED);
        if (reason != null) entity.setNotes(reason);
        log.info("Disputed Expense {}", id);
        publishDisputedEvent(entity);
        return buildResponse(entity);
    }

    @Transactional
    public ExpenseResponse reject(UUID id, String reason) {
        var entity = getExistingEntity(id);
        entity.setStatus(ExpenseStatus.REJECTED);
        if (reason != null) entity.setNotes(reason);
        log.info("Rejected Expense {}", id);
        return buildResponse(entity);
    }

    public List<ExpenseAuditResponse> getHistory(UUID id) {
        getExistingEntity(id); // ensure it exists
        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> revisions = reader.createQuery()
                .forRevisionsOfEntity(ExpenseEntity.class, false, true)
                .add(AuditEntity.id().eq(id))
                .getResultList();

        List<ExpenseAuditResponse> history = new ArrayList<>();
        for (Object[] row : revisions) {
            ExpenseEntity state = (ExpenseEntity) row[0];
            org.hibernate.envers.DefaultRevisionEntity rev =
                    (org.hibernate.envers.DefaultRevisionEntity) row[1];
            RevisionType type = (RevisionType) row[2];

            history.add(new ExpenseAuditResponse(
                    rev.getId(),
                    type.name(),
                    LocalDateTime.ofEpochSecond(rev.getTimestamp() / 1000, 0, ZoneOffset.UTC),
                    state.getAmount(),
                    state.getCurrency(),
                    state.getAmountBaseCurrency(),
                    state.getStatus(),
                    state.getNotes(),
                    state.getExpenseDate(),
                    state.getCategory() != null ? state.getCategory().getId() : null,
                    state.getSupplier() != null ? state.getSupplier().getId() : null
            ));
        }
        return history;
    }

    @Transactional
    public ExpenseMediaResponse addMedia(UUID expenseId, ExpenseMediaRequest request) {
        var expense = getExistingEntity(expenseId);
        var media = expenseMapper.toMediaEntity(request);
        media.setExpense(expense);
        var saved = expenseMediaRepository.save(media);
        log.info("Added media {} to Expense {}", saved.getId(), expenseId);
        return expenseMapper.toMediaResponse(saved);
    }

    @Transactional
    public void removeMedia(UUID expenseId, UUID mediaId) {
        var media = expenseMediaRepository.findById(mediaId)
                .filter(m -> m.getExpense().getId().equals(expenseId))
                .orElseThrow(() -> DataNotFoundException.of("Media {0} not found on expense {1}", mediaId, expenseId));
        media.setIsActive(false);
        log.info("Removed media {} from Expense {}", mediaId, expenseId);
    }

    @Transactional
    public void delete(UUID id) {
        var entity = getExistingEntity(id);
        entity.setIsActive(false);
        log.info("Soft-deleted Expense {}", id);
    }

    public ExpenseEntity getExistingEntity(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of(EXPENSE_NOT_FOUND_MESSAGE,
                        BaseEntity.Fields.id, id));
    }

    private void checkAnomaly(ExpenseEntity saved) {
        List<BigDecimal> amounts = expenseRepository.findAmountsInBaseCurrencyByCategory(
                saved.getCategory().getId(), ExpenseStatus.APPROVED);

        // Exclude the newly saved expense from the sample
        amounts = amounts.stream()
                .filter(a -> !a.equals(saved.getAmountBaseCurrency()))
                .toList();

        if (amounts.size() < ANOMALY_MIN_SAMPLES) return;

        BigDecimal median = median(amounts);
        BigDecimal mad = mad(amounts, median);
        BigDecimal threshold = mad.multiply(BigDecimal.valueOf(ANOMALY_MAD_MULTIPLIER));

        if (saved.getAmountBaseCurrency().subtract(median).abs().compareTo(threshold) > 0) {
            saved.setStatus(ExpenseStatus.DISPUTED);
            log.warn("Expense {} flagged as DISPUTED — amount {} deviates from median {} by MAD threshold {}",
                    saved.getId(), saved.getAmountBaseCurrency(), median, threshold);
            publishDisputedEvent(saved);
        }
    }

    private void checkBudgetThreshold(ExpenseEntity expense) {
        var category = expense.getCategory();
        if (category.getBudgetLimit() == null || category.getBudgetLimit().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal actual = expenseRepository.sumAmountBaseCurrencyByBuildingAndCategory(
                expense.getBuilding().getId(), category.getId(), ExpenseStatus.APPROVED);
        BigDecimal budget = category.getBudgetLimit();
        BigDecimal pct = actual.divide(budget, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        int pctInt = pct.intValue();
        if (pctInt >= 100) {
            eventPublisher.publishEvent(new BudgetThresholdEvent(
                    expense.getBuilding().getId(), expense.getBuilding().getName(),
                    category.getItemCode(), category.getItemName(), budget, actual, 100));
        } else if (pctInt >= 80) {
            eventPublisher.publishEvent(new BudgetThresholdEvent(
                    expense.getBuilding().getId(), expense.getBuilding().getName(),
                    category.getItemCode(), category.getItemName(), budget, actual, 80));
        }
    }

    private void publishDisputedEvent(ExpenseEntity entity) {
        String buildingName = entity.getBuilding() != null ? entity.getBuilding().getName() : "";
        String categoryName = entity.getCategory() != null ? entity.getCategory().getItemName() : "";
        String amount = entity.getAmountBaseCurrency() != null ? entity.getAmountBaseCurrency().toPlainString() : "0";
        eventPublisher.publishEvent(new ExpenseDisputedEvent(
                entity.getId(), buildingName, categoryName, amount, entity.getNotes()));
    }

    private BigDecimal median(List<BigDecimal> sorted) {
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return sorted.get(mid - 1).add(sorted.get(mid))
                    .divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        }
        return sorted.get(mid);
    }

    private BigDecimal mad(List<BigDecimal> values, BigDecimal median) {
        List<BigDecimal> deviations = values.stream()
                .map(v -> v.subtract(median).abs())
                .sorted()
                .toList();
        return median(deviations);
    }

    private String buildContentKey(ExpenseRequest r) {
        return r.getBuildingId() + "|" + r.getCategoryId() + "|" +
               (r.getSupplierId() != null ? r.getSupplierId() : "none") + "|" +
               r.getAmount().toPlainString() + "|" + r.getCurrency() + "|" + r.getExpenseDate();
    }

    private List<ExpenseMediaResponse> getMediaForExpense(UUID expenseId) {
        return expenseMediaRepository
                .findAllByExpenseIdAndIsActiveTrueOrderByDisplayOrderAsc(expenseId)
                .stream().map(expenseMapper::toMediaResponse).toList();
    }

    private void saveMedia(ExpenseEntity expense, List<ExpenseMediaRequest> mediaRequests) {
        mediaRequests.forEach(req -> {
            var media = expenseMapper.toMediaEntity(req);
            media.setExpense(expense);
            expenseMediaRepository.save(media);
        });
    }

    private ExpenseResponse buildResponse(ExpenseEntity entity) {
        var response = expenseMapper.toResponse(entity);
        response.setMedia(getMediaForExpense(entity.getId()));
        return response;
    }
}
