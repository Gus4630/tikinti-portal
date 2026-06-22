package az.tikinti.portal.model.dto.request.building;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class BuildingCategoryBudgetRequest {

    @NotNull
    @Positive
    private BigDecimal budgetLimit;

    private String notes;
}
