package az.tikinti.portal.service.building;

import az.tikinti.portal.dao.entity.building.BuildingCategoryBudgetEntity;
import az.tikinti.portal.dao.repository.building.BuildingCategoryBudgetRepository;
import az.tikinti.portal.model.dto.request.building.BuildingCategoryBudgetRequest;
import az.tikinti.portal.model.dto.response.building.BuildingCategoryBudgetResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingCategoryBudgetService {

    private final BuildingCategoryBudgetRepository budgetRepository;
    private final BuildingService buildingService;
    private final az.tikinti.portal.service.category.CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<BuildingCategoryBudgetResponse> getByBuilding(UUID buildingId) {
        buildingService.getExistingEntity(buildingId); // validate building exists
        return budgetRepository.findByBuildingIdAndIsActiveTrue(buildingId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public BuildingCategoryBudgetResponse upsert(UUID buildingId, UUID categoryId,
                                                  BuildingCategoryBudgetRequest request) {
        var building = buildingService.getExistingEntity(buildingId);
        var category = categoryService.getExistingEntity(categoryId);

        var existing = budgetRepository.findByBuildingIdAndCategoryIdAndIsActiveTrue(buildingId, categoryId);
        BuildingCategoryBudgetEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setBudgetLimit(request.getBudgetLimit());
            entity.setNotes(request.getNotes());
            log.info("Updated budget for building {} category {}", buildingId, categoryId);
        } else {
            entity = BuildingCategoryBudgetEntity.builder()
                    .building(building)
                    .category(category)
                    .budgetLimit(request.getBudgetLimit())
                    .notes(request.getNotes())
                    .build();
            log.info("Created budget for building {} category {}", buildingId, categoryId);
        }
        return toResponse(budgetRepository.save(entity));
    }

    @Transactional
    public void delete(UUID buildingId, UUID categoryId) {
        budgetRepository.findByBuildingIdAndCategoryIdAndIsActiveTrue(buildingId, categoryId)
                .ifPresent(e -> {
                    e.setIsActive(false);
                    log.info("Removed budget for building {} category {}", buildingId, categoryId);
                });
    }

    private BuildingCategoryBudgetResponse toResponse(BuildingCategoryBudgetEntity e) {
        var cat = e.getCategory();
        return BuildingCategoryBudgetResponse.builder()
                .id(e.getId())
                .buildingId(e.getBuilding().getId())
                .categoryId(cat.getId())
                .categoryItemCode(cat.getItemCode())
                .categoryItemName(cat.getItemName())
                .level1(cat.getLevel1())
                .level2(cat.getLevel2())
                .level3(cat.getLevel3())
                .budgetLimit(e.getBudgetLimit())
                .notes(e.getNotes())
                .build();
    }
}
