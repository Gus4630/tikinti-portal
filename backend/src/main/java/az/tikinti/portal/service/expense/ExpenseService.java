package az.tikinti.portal.service.expense;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.DUPLICATE_INVOICE_MESSAGE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.EXPENSE_NOT_FOUND_MESSAGE;
import static az.tikinti.portal.model.constant.CbarConstants.BASE_CURRENCY;
import static az.tikinti.portal.util.HashUtil.*;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseMediaEntity;
import az.tikinti.portal.dao.repository.building.BuildingCategoryBudgetRepository;
import az.tikinti.portal.dao.repository.building.BuildingRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseItemRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseMediaRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.repository.group.GroupMemberRepository;
import az.tikinti.portal.dao.specification.ExpenseSpecification;
import az.tikinti.portal.event.BudgetThresholdEvent;
import az.tikinti.portal.event.ExpenseDisputedEvent;
import az.tikinti.portal.exception.AlreadyExistsException;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.expense.ExpenseItemMapper;
import az.tikinti.portal.mapper.expense.ExpenseMapper;
import az.tikinti.portal.model.dto.request.expense.ExpenseFilterRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseMediaRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseUpdateRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.record.ExpenseAuditResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseItemResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseMediaResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import az.tikinti.portal.model.enums.ExpenseCreationType;
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
    private final ExpenseItemRepository expenseItemRepository;
    private final ExpenseMediaRepository expenseMediaRepository;
    private final ExpenseMapper expenseMapper;
    private final ExpenseItemMapper expenseItemMapper;
    private final BuildingService buildingService;
    private final BuildingRepository buildingRepository;
    private final BuildingCategoryBudgetRepository buildingCategoryBudgetRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final SupplierService supplierService;
    private final CurrencyRateService currencyRateService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public PageableResponse<ExpenseResponse> search(ExpenseFilterRequest request, UUID userId) {
        var groupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        var allowedBuildingIds = groupIds.isEmpty()
                ? List.<UUID>of()
                : buildingRepository.findIdsByGroupIds(groupIds);
        Pageable pageable = PageUtil.createPageable(request);
        log.info("Searching expenses with filters {} for user {}, allowedBuildingIds={}", request, userId, allowedBuildingIds);
        var spec = ExpenseSpecification.getSpecification(request, allowedBuildingIds);
        var page = expenseRepository.findAll(spec, pageable);
        var response = expenseMapper.toResponse(page);
        response.getContent().forEach(e -> {
            e.setMedia(getMediaForExpense(e.getId()));
            e.setItems(getItemsForExpense(e.getId()));
        });
        return response;
    }

    public ExpenseResponse getById(UUID id) {
        var response = expenseMapper.toResponse(getExistingEntity(id));
        response.setMedia(getMediaForExpense(id));
        response.setItems(getItemsForExpense(id));
        return response;
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        // SHA-256 dedup based on a stable content key (building + category + amount + date + supplier)
        String contentKey = buildContentKey(request); // note: amount may be null here; resolved later
        String contentHash = sha256Hex(
                contentKey.getBytes(StandardCharsets.UTF_8));

        expenseRepository.findByContentHash(contentHash).ifPresent(existing -> {
            throw AlreadyExistsException.of(DUPLICATE_INVOICE_MESSAGE, existing.getId());
        });

        var building = buildingService.getExistingEntity(request.getBuildingId());
        var category = categoryService.getExistingEntity(request.getCategoryId());

        // If amount is not supplied, derive it from items
        BigDecimal resolvedAmount = request.getAmount();
        if ((resolvedAmount == null || resolvedAmount.compareTo(BigDecimal.ZERO) == 0)
                && request.getItems() != null && !request.getItems().isEmpty()) {
            resolvedAmount = request.getItems().stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (resolvedAmount == null || resolvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive or items with unit prices must be provided");
        }

        // FX conversion
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal amountBase = resolvedAmount;
        String currency = request.getCurrency().toUpperCase();

        if (!BASE_CURRENCY.equals(currency)) {
            BigDecimal[] result = currencyRateService.exchangeCurrency(
                    currency, BASE_CURRENCY, resolvedAmount, request.getExpenseDate());
            amountBase = result[0];
            exchangeRate = result[1];
        }

        var entity = ExpenseEntity.builder()
                .building(building)
                .category(category)
                .supplier(request.getSupplierId() != null
                        ? supplierService.getExistingEntity(request.getSupplierId()) : null)
                .amount(resolvedAmount)
                .currency(currency)
                .exchangeRate(exchangeRate)
                .amountBaseCurrency(amountBase)
                .contentHash(contentHash)
                .status(ExpenseStatus.APPROVED)
                .creationType(ExpenseCreationType.MANUAL)
                .notes(request.getNotes())
                .expenseDate(request.getExpenseDate())
                .build();

        var saved = expenseRepository.save(entity);

        if (request.getMedia() != null) {
            saveMedia(saved, request.getMedia());
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            request.getItems().stream()
                    .map(itemReq -> expenseItemMapper.toEntity(itemReq, saved))
                    .forEach(expenseItemRepository::save);
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
        response.setItems(getItemsForExpense(saved.getId()));
        return response;
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseUpdateRequest request) {
        var entity = getExistingEntity(id);

        expenseMapper.updateEntityFromRequest(request, entity);

        entity.setCategory(categoryService.getExistingEntity(request.getCategoryId()));
        entity.setSupplier(request.getSupplierId() != null
                ? supplierService.getExistingEntity(request.getSupplierId()) : null);

        String currency = request.getCurrency().toUpperCase();
        entity.setCurrency(currency);
        if (!BASE_CURRENCY.equals(currency)) {
            BigDecimal[] fx = currencyRateService.exchangeCurrency(
                    currency, BASE_CURRENCY, request.getAmount(), request.getExpenseDate());
            entity.setAmountBaseCurrency(fx[0]);
            entity.setExchangeRate(fx[1]);
        } else {
            entity.setAmountBaseCurrency(request.getAmount());
            entity.setExchangeRate(BigDecimal.ONE);
        }

        if (request.getItems() != null) {
            entity.getItems().clear();
            request.getItems().stream()
                    .map(itemReq -> expenseItemMapper.toEntity(itemReq, entity))
                    .forEach(entity.getItems()::add);
        }

        log.info("Updated Expense {}", id);
        return buildResponse(entity);
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
    public ExpenseResponse markAsPaid(UUID id) {
        var entity = getExistingEntity(id);
        if (entity.getStatus() != ExpenseStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED expenses can be marked as PAID");
        }
        entity.setStatus(ExpenseStatus.PAID);
        log.info("Marked Expense {} as PAID", id);
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
        var perBuildingBudget = buildingCategoryBudgetRepository
                .findByBuildingIdAndCategoryIdAndIsActiveTrue(expense.getBuilding().getId(), category.getId());
        if (perBuildingBudget.isEmpty()) return;

        BigDecimal actual = expenseRepository.sumAmountBaseCurrencyByBuildingAndCategory(
                expense.getBuilding().getId(), category.getId(), ExpenseStatus.APPROVED);
        BigDecimal budget = perBuildingBudget.get().getBudgetLimit();
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
        // When amount is null, derive a stable key from items total for dedup purposes
        String amountKey;
        if (r.getAmount() != null) {
            amountKey = r.getAmount().toPlainString();
        } else if (r.getItems() != null && !r.getItems().isEmpty()) {
            BigDecimal itemsTotal = r.getItems().stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amountKey = itemsTotal.toPlainString();
        } else {
            amountKey = "0";
        }
        return r.getBuildingId() + "|" + r.getCategoryId() + "|" +
               (r.getSupplierId() != null ? r.getSupplierId() : "none") + "|" +
               amountKey + "|" + r.getCurrency() + "|" + r.getExpenseDate();
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

    private List<ExpenseItemResponse> getItemsForExpense(UUID expenseId) {
        return expenseItemRepository.findByExpenseIdAndIsActiveTrue(expenseId)
                .stream().map(expenseItemMapper::toResponse).toList();
    }

    private ExpenseResponse buildResponse(ExpenseEntity entity) {
        var response = expenseMapper.toResponse(entity);
        response.setMedia(getMediaForExpense(entity.getId()));
        response.setItems(getItemsForExpense(entity.getId()));
        return response;
    }
}
