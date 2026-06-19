package az.tikinti.portal.dao.repository.building;

import az.tikinti.portal.dao.entity.building.BuildingEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BuildingRepository
        extends JpaRepository<BuildingEntity, UUID>, JpaSpecificationExecutor<BuildingEntity> {
}
