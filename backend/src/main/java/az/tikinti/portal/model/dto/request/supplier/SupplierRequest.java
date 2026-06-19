package az.tikinti.portal.model.dto.request.supplier;

import az.tikinti.portal.model.enums.SupplierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank
    private String name;

    @NotNull
    private SupplierType supplierType;

    private String taxId;
    private String nationalId;
    private String phoneNumber;
    private BigDecimal totalAdvancedPaid;
    private BigDecimal retainagePercentage;
    private BigDecimal retainageHeldAmount;
}
