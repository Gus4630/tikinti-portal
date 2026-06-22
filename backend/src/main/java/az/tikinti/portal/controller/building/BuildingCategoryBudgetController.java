package az.tikinti.portal.controller.building;

import az.tikinti.portal.model.dto.request.building.BuildingCategoryBudgetRequest;
import az.tikinti.portal.model.dto.response.building.BuildingCategoryBudgetResponse;
import az.tikinti.portal.service.building.BuildingCategoryBudgetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/buildings/{buildingId}/budgets")
public class BuildingCategoryBudgetController {

    private final BuildingCategoryBudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BuildingCategoryBudgetResponse>> getAll(@PathVariable UUID buildingId) {
        return ResponseEntity.ok(budgetService.getByBuilding(buildingId));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<BuildingCategoryBudgetResponse> upsert(
            @PathVariable UUID buildingId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody BuildingCategoryBudgetRequest request) {
        return ResponseEntity.ok(budgetService.upsert(buildingId, categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID buildingId,
            @PathVariable UUID categoryId) {
        budgetService.delete(buildingId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
