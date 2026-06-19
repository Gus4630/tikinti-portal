package az.tikinti.portal.controller.supplier;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.supplier.SupplierFilterRequest;
import az.tikinti.portal.model.dto.request.supplier.SupplierRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.supplier.SupplierResponse;
import az.tikinti.portal.service.supplier.SupplierService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping("/search")
    public ResponseEntity<PageableResponse<SupplierResponse>> search(
            @Valid @RequestBody SupplierFilterRequest request) {
        return ResponseEntity.ok(supplierService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(CREATED).body(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
