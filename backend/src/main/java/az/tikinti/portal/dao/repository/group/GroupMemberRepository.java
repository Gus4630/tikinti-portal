package az.tikinti.portal.dao.repository.group;

import az.tikinti.portal.dao.entity.group.GroupMemberEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, UUID> {

    Optional<GroupMemberEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);

    Optional<GroupMemberEntity> findByGroupIdAndUserIdAndIsActiveTrue(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserIdAndIsActiveTrue(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserIdAndMemberRoleAndIsActiveTrue(UUID groupId, UUID userId, String memberRole);

    @Query("SELECT m.group.id FROM GroupMemberEntity m WHERE m.user.id = :userId AND m.isActive = true")
    List<UUID> findGroupIdsByUserId(@Param("userId") UUID userId);
}
