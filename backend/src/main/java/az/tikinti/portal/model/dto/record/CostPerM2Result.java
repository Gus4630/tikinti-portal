package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;
import java.util.UUID;

public record CostPerM2Result(
        UUID buildingId,
        String buildingName,
        BigDecimal floorAreaM2,
        BigDecimal totalSpend,
        BigDecimal costPerM2
) {}
