package az.tikinti.portal.controller.file;

import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.service.expense.ExpenseService;
import az.tikinti.portal.service.file.FileStorageService;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final ExpenseService expenseService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{expenseId}/invoice")
    public ResponseEntity<Resource> getInvoice(
            @PathVariable UUID expenseId,
            Authentication authentication) {

        var expense = expenseService.getExistingEntity(expenseId);

        if (expense.getImageUrl() == null) {
            throw DataNotFoundException.of("No invoice file for expense {0}", expenseId);
        }

        Resource resource = fileStorageService.loadAsResource(expense.getImageUrl());

        String contentType = detectContentType(expense.getImageUrl());
        String filename = "invoice-" + expenseId + getExtension(expense.getImageUrl());

        log.info("Serving invoice file for expense {} to user {}", expenseId, authentication.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String detectContentType(String filePath) {
        try {
            String detected = Files.probeContentType(Paths.get(filePath));
            return detected != null ? detected : "application/octet-stream";
        } catch (Exception e) {
            String lower = filePath.toLowerCase();
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            return "application/octet-stream";
        }
    }

    private String getExtension(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot >= 0 ? filePath.substring(dot) : "";
    }
}
