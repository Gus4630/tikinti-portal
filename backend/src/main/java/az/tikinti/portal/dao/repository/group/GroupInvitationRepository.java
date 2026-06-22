package az.tikinti.portal.dao.repository.group;

import az.tikinti.portal.dao.entity.group.GroupInvitationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitationEntity, UUID> {

    List<GroupInvitationEntity> findAllByInvitedUserIdAndStatusAndIsActiveTrue(UUID invitedUserId, String status);

    Optional<GroupInvitationEntity> findByIdAndInvitedUserIdAndIsActiveTrue(UUID id, UUID invitedUserId);

    Optional<GroupInvitationEntity> findByGroupIdAndInvitedUserIdAndStatus(UUID groupId, UUID invitedUserId, String status);

    List<GroupInvitationEntity> findByGroupIdAndStatusAndIsActiveTrue(UUID groupId, String status);
}
