package az.tikinti.portal.controller.expense;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.expense.ExpenseFilterRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseMediaRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseRequest;
import az.tikinti.portal.model.dto.request.expense.ExpenseUpdateRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.record.ExpenseAuditResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseMediaResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import az.tikinti.portal.service.expense.ExpenseService;
import java.util.List;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping("/search")
    public ResponseEntity<PageableResponse<ExpenseResponse>> search(
            Authentication authentication,
            @Valid @RequestBody ExpenseFilterRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(expenseService.search(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(CREATED).body(expenseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable UUID id, @Valid @RequestBody ExpenseUpdateRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ExpenseResponse> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.approve(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ExpenseResponse> pay(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.markAsPaid(id));
    }

    @PostMapping("/{id}/dispute")
    public ResponseEntity<ExpenseResponse> dispute(
            @PathVariable UUID id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(expenseService.dispute(id, reason));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ExpenseResponse> reject(
            @PathVariable UUID id, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(expenseService.reject(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        expenseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/media")
    public ResponseEntity<ExpenseMediaResponse> addMedia(
            @PathVariable UUID id, @Valid @RequestBody ExpenseMediaRequest request) {
        return ResponseEntity.status(CREATED).body(expenseService.addMedia(id, request));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public ResponseEntity<Void> removeMedia(@PathVariable UUID id, @PathVariable UUID mediaId) {
        expenseService.removeMedia(id, mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ExpenseAuditResponse>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(expenseService.getHistory(id));
    }
}
