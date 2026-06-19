package az.tikinti.portal.service.report;

import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.dao.repository.category.CategoryRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.repository.supplier.SupplierRepository;
import az.tikinti.portal.mapper.expense.ExpenseMapper;
import az.tikinti.portal.model.dto.record.BudgetVsActualRow;
import az.tikinti.portal.model.dto.record.CostPerM2Result;
import az.tikinti.portal.model.dto.record.MonthlyExportRow;
import az.tikinti.portal.model.dto.record.SpendForecastResult;
import az.tikinti.portal.model.dto.record.SupplierLedgerRow;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import az.tikinti.portal.model.enums.ExpenseStatus;
import java.util.Map;
import az.tikinti.portal.service.building.BuildingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final SupplierRepository supplierRepository;
    private final ExpenseMapper expenseMapper;

    public List<BudgetVsActualRow> getBudgetVsActual(UUID buildingId) {
        List<CategoryEntity> categories = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();

        return categories.stream().map(cat -> {
            BigDecimal actual = expenseRepository.sumAmountBaseCurrencyByBuildingAndCategory(
                    buildingId, cat.getId(), ExpenseStatus.APPROVED);
            BigDecimal budget = cat.getBudgetLimit() != null ? cat.getBudgetLimit() : BigDecimal.ZERO;
            BigDecimal variance = budget.subtract(actual);
            BigDecimal usedPct = budget.compareTo(BigDecimal.ZERO) > 0
                    ? actual.divide(budget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            return new BudgetVsActualRow(
                    cat.getItemCode(), cat.getLevel1(), cat.getLevel2(), cat.getLevel3(),
                    cat.getItemName(), budget, actual, variance, usedPct);
        }).toList();
    }

    public Map<String, BigDecimal> getPhaseRollup(UUID buildingId) {
        return getBudgetVsActual(buildingId).stream().collect(
                Collectors.groupingBy(BudgetVsActualRow::level1,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, BudgetVsActualRow::actualSpend, BigDecimal::add)));
    }

    public List<SupplierLedgerRow> getSupplierLedger(UUID buildingId) {
        List<Object[]> spendRows = expenseRepository.sumAmountBaseCurrencyBySupplierAndBuilding(
                buildingId, ExpenseStatus.APPROVED);

        Map<UUID, BigDecimal> spendBySupplier = new HashMap<>();
        spendRows.forEach(row -> spendBySupplier.put((UUID) row[0], (BigDecimal) row[1]));

        List<SupplierLedgerRow> result = new ArrayList<>();
        supplierRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .forEach(supplier -> {
                    BigDecimal spend = spendBySupplier.getOrDefault(supplier.getId(), BigDecimal.ZERO);
                    BigDecimal balance = spend.subtract(supplier.getTotalAdvancedPaid());
                    result.add(new SupplierLedgerRow(
                            supplier.getId(), supplier.getName(),
                            supplier.getTotalAdvancedPaid(), supplier.getRetainageHeldAmount(),
                            spend, balance));
                });
        return result;
    }

    public SpendForecastResult getSpendForecast(UUID buildingId) {
        BigDecimal totalSpend = expenseRepository.sumAmountBaseCurrencyByBuilding(
                buildingId, ExpenseStatus.APPROVED);
        LocalDate firstDate = expenseRepository.findFirstExpenseDateByBuilding(buildingId);

        if (firstDate == null || totalSpend.compareTo(BigDecimal.ZERO) == 0) {
            return new SpendForecastResult(totalSpend, null, BigDecimal.ZERO, null);
        }

        long days = ChronoUnit.DAYS.between(firstDate, LocalDate.now());
        if (days < 1) days = 1;

        BigDecimal burnRate = totalSpend.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
        return new SpendForecastResult(totalSpend, firstDate, burnRate, null);
    }

    public CostPerM2Result getCostPerM2(UUID buildingId, BuildingService buildingService) {
        var building = buildingService.getExistingEntity(buildingId);
        BigDecimal totalSpend = expenseRepository.sumAmountBaseCurrencyByBuilding(
                buildingId, ExpenseStatus.APPROVED);

        BigDecimal costPerM2 = BigDecimal.ZERO;
        if (building.getFloorAreaM2() != null && building.getFloorAreaM2().compareTo(BigDecimal.ZERO) > 0) {
            costPerM2 = totalSpend.divide(building.getFloorAreaM2(), 2, RoundingMode.HALF_UP);
        }

        return new CostPerM2Result(buildingId, building.getName(), building.getFloorAreaM2(),
                totalSpend, costPerM2);
    }

    public PageableResponse<ExpenseResponse> getDisputed(UUID buildingId, int page, int perPage) {
        var pageable = PageRequest.of(page, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = expenseRepository.findAllByBuildingIdAndStatusAndIsActiveTrue(
                buildingId, ExpenseStatus.DISPUTED, pageable);
        return expenseMapper.toResponse(result);
    }

    public List<Map<String, Object>> getMonthlyTrend(UUID buildingId) {
        List<Object[]> rows = expenseRepository.sumMonthlyByBuilding(buildingId, ExpenseStatus.APPROVED.name());
        return rows.stream().map(row -> Map.<String, Object>of(
                "month", row[0].toString(),
                "total", row[1]
        )).toList();
    }

    public List<MonthlyExportRow> getMonthlyExport(UUID buildingId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return expenseRepository.findByBuildingAndDateRange(buildingId, start, end).stream()
                .map(e -> new MonthlyExportRow(
                        e.getExpenseDate().toString(),
                        e.getCategory() != null ? e.getCategory().getItemCode() : "",
                        e.getCategory() != null ? e.getCategory().getItemName() : "",
                        e.getSupplier() != null ? e.getSupplier().getName() : "",
                        e.getAmount(),
                        e.getCurrency(),
                        e.getExchangeRate(),
                        e.getAmountBaseCurrency(),
                        e.getStatus() != null ? e.getStatus().name() : "",
                        e.getNotes()))
                .toList();
    }
}
