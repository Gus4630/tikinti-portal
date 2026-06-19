package az.tikinti.portal.controller.invoice;

import az.tikinti.portal.service.invoice.InvoiceUploadService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
public class InvoiceUploadController {

    private final InvoiceUploadService invoiceUploadService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, UUID>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("buildingId") UUID buildingId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {

        UUID expenseId = invoiceUploadService.upload(file, buildingId, categoryId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("expenseId", expenseId));
    }
}
