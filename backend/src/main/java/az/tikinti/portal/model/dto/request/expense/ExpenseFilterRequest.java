package az.tikinti.portal.model.dto.request.expense;

import az.tikinti.portal.model.dto.request.PageableRequest;
import az.tikinti.portal.model.enums.ExpenseStatus;
import java.time.LocalDate;
import java.util.UUID;
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
public class ExpenseFilterRequest extends PageableRequest {

    private UUID buildingId;
    private UUID categoryId;
    private UUID supplierId;
    private ExpenseStatus status;
    private String currency;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Boolean isActive;
}
