package az.tikinti.portal.model.dto.request.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ExpenseUpdateRequest {

    @NotNull
    private UUID categoryId;

    private UUID supplierId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private LocalDate expenseDate;

    private String notes;

    @Valid
    private List<ExpenseItemRequest> items;
}
