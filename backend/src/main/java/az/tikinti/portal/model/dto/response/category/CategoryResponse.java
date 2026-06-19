package az.tikinti.portal.model.dto.response.category;

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
public class CategoryResponse {

    private UUID id;
    private String itemCode;
    private String level1;
    private String level2;
    private String level3;
    private String itemName;
    private String itemDescription;
    private BigDecimal budgetLimit;
    private Integer displayOrder;
    private Boolean isActive;
}
