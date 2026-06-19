package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierLedgerRow(
        UUID supplierId,
        String name,
        BigDecimal totalAdvancedPaid,
        BigDecimal retainageHeldAmount,
        BigDecimal totalApprovedSpend,
        BigDecimal balanceOwed
) {}
