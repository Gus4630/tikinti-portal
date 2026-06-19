package az.tikinti.portal.model.dto.response.supplier;

import az.tikinti.portal.model.enums.SupplierType;
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
public class SupplierResponse {

    private UUID id;
    private String name;
    private SupplierType supplierType;
    private String taxId;
    private String nationalId;
    private String phoneNumber;
    private BigDecimal totalAdvancedPaid;
    private BigDecimal retainagePercentage;
    private BigDecimal retainageHeldAmount;
    private Boolean isActive;
}
