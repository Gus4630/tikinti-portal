package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;

public record PlatformStatsResponse(
        BigDecimal totalManagedExpense,
        long activeProjectCount,
        long supplierCount
) {}
