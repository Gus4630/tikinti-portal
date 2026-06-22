package az.tikinti.portal.model.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    @Size(max = 40)
    private String itemCode;

    @NotBlank
    @Size(max = 100)
    private String level1;

    @NotBlank
    @Size(max = 100)
    private String level2;

    @NotBlank
    @Size(max = 100)
    private String level3;

    @NotBlank
    @Size(max = 200)
    private String itemName;

    private String itemDescription;
    private Integer displayOrder;
}
