package az.tikinti.portal.model.dto.response.expense;

import az.tikinti.portal.model.enums.ExpenseCreationType;
import az.tikinti.portal.model.enums.ExpenseStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class ExpenseResponse {

    private UUID id;
    private UUID buildingId;
    private String buildingName;
    private UUID categoryId;
    private String categoryItemCode;
    private String categoryItemName;
    private UUID supplierId;
    private String supplierName;
    private BigDecimal amount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal amountBaseCurrency;
    private String contentHash;
    private String imageUrl;
    private ExpenseStatus status;
    private ExpenseCreationType creationType;
    private String notes;
    private LocalDate expenseDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExpenseMediaResponse> media;
    private List<ExpenseItemResponse> items;
}
