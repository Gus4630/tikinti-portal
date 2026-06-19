package az.tikinti.portal.model.dto.record;

import az.tikinti.portal.model.enums.ExpenseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseAuditResponse(
        int revision,
        String revisionType,
        LocalDateTime revisionTimestamp,
        BigDecimal amount,
        String currency,
        BigDecimal amountBaseCurrency,
        ExpenseStatus status,
        String notes,
        LocalDate expenseDate,
        UUID categoryId,
        UUID supplierId
) {}
