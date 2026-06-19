package az.tikinti.portal.model.dto.request.category;

import az.tikinti.portal.model.dto.request.PageableRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CategoryFilterRequest extends PageableRequest {

    private List<String> level1s;
    private List<String> level2s;
    private String itemName;
    private String itemCode;
    private Boolean isActive;
}
