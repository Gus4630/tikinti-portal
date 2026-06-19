package az.tikinti.portal.service.ocr;

import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.model.dto.record.OcrExtractionResult;
import java.util.List;

public interface InvoiceExtractionService {
    OcrExtractionResult extract(byte[] fileBytes, String mimeType, List<CategoryEntity> categories);
}
