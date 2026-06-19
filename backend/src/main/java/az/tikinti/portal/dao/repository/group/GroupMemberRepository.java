package az.tikinti.portal.dao.repository.group;

import az.tikinti.portal.dao.entity.group.GroupMemberEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, UUID> {

    Optional<GroupMemberEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}
