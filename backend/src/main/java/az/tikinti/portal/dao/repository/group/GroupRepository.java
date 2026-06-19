package az.tikinti.portal.dao.repository.group;

import az.tikinti.portal.dao.entity.group.GroupEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, UUID>,
        JpaSpecificationExecutor<GroupEntity> {
}
