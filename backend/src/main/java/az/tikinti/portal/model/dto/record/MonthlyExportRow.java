package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;

public record MonthlyExportRow(
        String expenseDate,
        String categoryCode,
        String categoryName,
        String supplierName,
        BigDecimal amount,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal amountBaseCurrency,
        String status,
        String notes
) {}
