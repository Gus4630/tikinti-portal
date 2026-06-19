package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpendForecastResult(
        BigDecimal totalApprovedSpend,
        LocalDate firstExpenseDate,
        BigDecimal burnRatePerDay,
        BigDecimal projectedTotal
) {}
