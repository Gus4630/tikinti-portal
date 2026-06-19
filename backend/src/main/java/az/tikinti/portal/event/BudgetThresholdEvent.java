package az.tikinti.portal.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetThresholdEvent(
        UUID buildingId,
        String buildingName,
        String categoryCode,
        String categoryName,
        BigDecimal budgetLimit,
        BigDecimal actualSpend,
        int thresholdPercent
) {}
