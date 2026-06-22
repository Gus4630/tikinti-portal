package az.tikinti.portal.dao.repository.building;

import az.tikinti.portal.dao.entity.building.BuildingEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuildingRepository
        extends JpaRepository<BuildingEntity, UUID>, JpaSpecificationExecutor<BuildingEntity> {

    @Query("SELECT b.id FROM BuildingEntity b WHERE b.group.id IN :groupIds")
    List<UUID> findIdsByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    long countByIsActiveTrue();
}
