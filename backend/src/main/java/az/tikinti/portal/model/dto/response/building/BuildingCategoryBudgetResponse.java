package az.tikinti.portal.model.dto.response.building;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BuildingCategoryBudgetResponse {

    private UUID id;
    private UUID buildingId;
    private UUID categoryId;
    private String categoryItemCode;
    private String categoryItemName;
    private String level1;
    private String level2;
    private String level3;
    private BigDecimal budgetLimit;
    private String notes;
}
