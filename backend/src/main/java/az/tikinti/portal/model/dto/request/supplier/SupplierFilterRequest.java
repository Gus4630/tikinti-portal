package az.tikinti.portal.model.dto.request.supplier;

import az.tikinti.portal.model.dto.request.PageableRequest;
import az.tikinti.portal.model.enums.SupplierType;
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
public class SupplierFilterRequest extends PageableRequest {

    private String name;
    private SupplierType supplierType;
    private Boolean isActive;
}
