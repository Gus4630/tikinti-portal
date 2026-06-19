package az.tikinti.portal.model.dto.request.building;

import az.tikinti.portal.model.dto.request.PageableRequest;
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
public class BuildingFilterRequest extends PageableRequest {

    private String name;
    private Boolean isActive;
}
