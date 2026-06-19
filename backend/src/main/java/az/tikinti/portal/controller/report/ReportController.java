package az.tikinti.portal.controller.report;

import az.tikinti.portal.model.dto.record.BudgetVsActualRow;
import az.tikinti.portal.model.dto.record.CostPerM2Result;
import az.tikinti.portal.model.dto.record.MonthlyExportRow;
import az.tikinti.portal.model.dto.record.SpendForecastResult;
import az.tikinti.portal.model.dto.record.SupplierLedgerRow;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import az.tikinti.portal.service.building.BuildingService;
import az.tikinti.portal.service.report.ReportService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final BuildingService buildingService;

    @GetMapping("/budget-vs-actual")
    public ResponseEntity<List<BudgetVsActualRow>> getBudgetVsActual(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getBudgetVsActual(buildingId));
    }

    @GetMapping("/phase-rollup")
    public ResponseEntity<Map<String, BigDecimal>> getPhaseRollup(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getPhaseRollup(buildingId));
    }

    @GetMapping("/supplier-ledger")
    public ResponseEntity<List<SupplierLedgerRow>> getSupplierLedger(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getSupplierLedger(buildingId));
    }

    @GetMapping("/spend-forecast")
    public ResponseEntity<SpendForecastResult> getSpendForecast(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getSpendForecast(buildingId));
    }

    @GetMapping("/cost-per-m2")
    public ResponseEntity<CostPerM2Result> getCostPerM2(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getCostPerM2(buildingId, buildingService));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyTrend(@RequestParam UUID buildingId) {
        return ResponseEntity.ok(reportService.getMonthlyTrend(buildingId));
    }

    @GetMapping("/disputed")
    public ResponseEntity<PageableResponse<ExpenseResponse>> getDisputed(
            @RequestParam UUID buildingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        return ResponseEntity.ok(reportService.getDisputed(buildingId, page, perPage));
    }

    @GetMapping("/export/monthly")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam UUID buildingId,
            @RequestParam int year,
            @RequestParam int month) {

        List<MonthlyExportRow> rows = reportService.getMonthlyExport(buildingId, year, month);
        List<ExcelRow> excelRows = rows.stream().map(ExcelRow::from).collect(Collectors.toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, ExcelRow.class).sheet("Monthly Expenses").doWrite(excelRows);

        String filename = "expenses-%d-%02d.xlsx".formatted(year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }

    @Data
    public static class ExcelRow {
        @ExcelProperty("Date") private String expenseDate;
        @ExcelProperty("Category Code") private String categoryCode;
        @ExcelProperty("Category") private String categoryName;
        @ExcelProperty("Supplier") private String supplierName;
        @ExcelProperty("Amount") private BigDecimal amount;
        @ExcelProperty("Currency") private String currency;
        @ExcelProperty("Exchange Rate") private BigDecimal exchangeRate;
        @ExcelProperty("Amount (AZN)") private BigDecimal amountBaseCurrency;
        @ExcelProperty("Status") private String status;
        @ExcelProperty("Notes") private String notes;

        static ExcelRow from(MonthlyExportRow r) {
            ExcelRow row = new ExcelRow();
            row.expenseDate = r.expenseDate();
            row.categoryCode = r.categoryCode();
            row.categoryName = r.categoryName();
            row.supplierName = r.supplierName();
            row.amount = r.amount();
            row.currency = r.currency();
            row.exchangeRate = r.exchangeRate();
            row.amountBaseCurrency = r.amountBaseCurrency();
            row.status = r.status();
            row.notes = r.notes();
            return row;
        }
    }
}
