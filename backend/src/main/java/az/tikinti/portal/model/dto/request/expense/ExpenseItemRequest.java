package az.tikinti.portal.model.dto.request.expense;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExpenseItemRequest {

    @NotBlank
    private String description;

    @Min(1)
    private int quantity;

    @NotNull
    @Positive
    private BigDecimal unitPrice;
}
