package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;

public record BudgetVsActualRow(
        String itemCode,
        String level1,
        String level2,
        String level3,
        String itemName,
        BigDecimal budgetLimit,
        BigDecimal actualSpend,
        BigDecimal variance,
        BigDecimal usedPercent
) {}
