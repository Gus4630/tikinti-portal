package az.tikinti.portal.model.dto.request.expense;

import az.tikinti.portal.model.dto.request.building.BuildingMediaRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ExpenseRequest {

    @NotNull
    private UUID buildingId;

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

    private List<ExpenseMediaRequest> media;
}
