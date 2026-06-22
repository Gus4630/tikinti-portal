package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierLedgerRow(
        UUID supplierId,
        String name,
        BigDecimal totalInvoiced,    // APPROVED + PAID
        BigDecimal totalPaid,        // PAID only
        BigDecimal retainageHeldAmount,
        BigDecimal balanceOwed       // totalInvoiced - totalPaid
) {}
