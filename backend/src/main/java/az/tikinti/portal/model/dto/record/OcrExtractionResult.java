package az.tikinti.portal.model.dto.record;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record OcrExtractionResult(
        BigDecimal amount,
        String currency,
        LocalDate invoiceDate,
        String supplierName,
        String supplierTaxOrNationalId,
        List<LineItem> lineItems,
        String suggestedCategoryCode,
        Map<String, Double> confidence
) {
    public record LineItem(String description, int quantity, BigDecimal unitPrice) {}

    public boolean hasLowConfidence() {
        if (confidence == null) return true;
        return confidence.values().stream().anyMatch(v -> v < 0.6);
    }
}
