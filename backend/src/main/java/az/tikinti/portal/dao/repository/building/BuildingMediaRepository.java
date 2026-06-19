package az.tikinti.portal.dao.repository.building;

import az.tikinti.portal.dao.entity.building.BuildingMediaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuildingMediaRepository extends JpaRepository<BuildingMediaEntity, UUID> {

    List<BuildingMediaEntity> findAllByBuildingIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID buildingId);
}
