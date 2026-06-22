package az.tikinti.portal.dao.repository.building;

import az.tikinti.portal.dao.entity.building.BuildingCategoryBudgetEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingCategoryBudgetRepository extends JpaRepository<BuildingCategoryBudgetEntity, UUID> {

    Optional<BuildingCategoryBudgetEntity> findByBuildingIdAndCategoryIdAndIsActiveTrue(UUID buildingId, UUID categoryId);

    List<BuildingCategoryBudgetEntity> findByBuildingIdAndIsActiveTrue(UUID buildingId);
}
