package az.tikinti.portal.service.group;

import az.tikinti.portal.dao.entity.auth.UserEntity;
import az.tikinti.portal.dao.entity.group.GroupEntity;
import az.tikinti.portal.dao.entity.group.GroupInvitationEntity;
import az.tikinti.portal.dao.entity.group.GroupMemberEntity;
import az.tikinti.portal.dao.repository.auth.UserRepository;
import az.tikinti.portal.dao.repository.group.GroupInvitationRepository;
import az.tikinti.portal.dao.repository.group.GroupMemberRepository;
import az.tikinti.portal.dao.repository.group.GroupRepository;
import az.tikinti.portal.exception.AlreadyExistsException;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.exception.ForbiddenException;
import az.tikinti.portal.mapper.group.GroupInvitationMapper;
import az.tikinti.portal.mapper.group.GroupMapper;
import az.tikinti.portal.model.dto.request.group.GroupInvitationRequest;
import az.tikinti.portal.model.dto.request.group.GroupRequest;
import az.tikinti.portal.model.dto.response.group.GroupInvitationResponse;
import az.tikinti.portal.model.dto.response.group.GroupMemberResponse;
import az.tikinti.portal.model.dto.response.group.GroupResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupService {

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String DECLINED = "DECLINED";
    private static final String REVOKED = "REVOKED";
    private static final String OWNER_ROLE = "OWNER";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;
    private final GroupInvitationMapper invitationMapper;

    public List<GroupResponse> findAll(UUID userId) {
        List<UUID> memberGroupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        return groupRepository.findAllById(memberGroupIds).stream()
                .filter(g -> Boolean.TRUE.equals(g.getIsActive()))
                .map(g -> {
                    GroupResponse r = groupMapper.toResponse(g);
                    r.setMembers(g.getMembers().stream()
                            .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                            .map(groupMapper::toMemberResponse)
                            .toList());
                    return r;
                })
                .toList();
    }

    public GroupResponse getById(UUID id) {
        GroupEntity entity = getExisting(id);
        GroupResponse r = groupMapper.toResponse(entity);
        r.setMembers(entity.getMembers().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .map(groupMapper::toMemberResponse)
                .toList());
        return r;
    }

    @Transactional
    public GroupResponse create(GroupRequest request, UUID creatorId) {
        GroupEntity entity = GroupEntity.builder()
                .name(request.getName())
                .build();
        GroupEntity saved = groupRepository.save(entity);

        UserEntity creator = userRepository.findById(creatorId)
                .orElseThrow(() -> DataNotFoundException.of("User {0} not found", creatorId));
        GroupMemberEntity ownerMember = GroupMemberEntity.builder()
                .group(saved)
                .user(creator)
                .memberRole(OWNER_ROLE)
                .build();
        groupMemberRepository.save(ownerMember);

        log.info("Created Group {} with owner {}", saved.getId(), creatorId);
        GroupResponse r = groupMapper.toResponse(saved);
        r.setMembers(List.of(groupMapper.toMemberResponse(ownerMember)));
        return r;
    }

    @Transactional
    public GroupResponse update(UUID id, GroupRequest request) {
        GroupEntity entity = getExisting(id);
        entity.setName(request.getName());
        log.info("Updated Group {}", id);
        GroupResponse r = groupMapper.toResponse(entity);
        r.setMembers(entity.getMembers().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .map(groupMapper::toMemberResponse)
                .toList());
        return r;
    }

    @Transactional
    public void delete(UUID id) {
        GroupEntity entity = getExisting(id);
        entity.setIsActive(false);
        log.info("Deactivated Group {}", id);
    }

    @Transactional
    public GroupInvitationResponse sendInvitation(UUID groupId, GroupInvitationRequest request, UUID invitedById) {
        GroupEntity group = getExisting(groupId);
        UUID invitedUserId = request.getUserId();

        requireOwner(groupId, invitedById);

        if (groupMemberRepository.existsByGroupIdAndUserIdAndIsActiveTrue(groupId, invitedUserId)) {
            throw AlreadyExistsException.of("User {0} is already a member of group {1}", invitedUserId, groupId);
        }

        // Cancel any existing pending invitation for this user+group before sending a new one
        invitationRepository.findByGroupIdAndInvitedUserIdAndStatus(groupId, invitedUserId, PENDING)
                .ifPresent(existing -> existing.setStatus(DECLINED));

        UserEntity invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow(() -> DataNotFoundException.of("User {0} not found", invitedUserId));
        UserEntity invitedBy = userRepository.findById(invitedById)
                .orElseThrow(() -> DataNotFoundException.of("User {0} not found", invitedById));

        GroupInvitationEntity invitation = GroupInvitationEntity.builder()
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(invitedBy)
                .memberRole(request.getMemberRole())
                .status(PENDING)
                .build();

        GroupInvitationEntity saved = invitationRepository.save(invitation);
        log.info("Sent invitation {} to user {} for group {}", saved.getId(), invitedUserId, groupId);
        return invitationMapper.toResponse(saved);
    }

    @Transactional
    public void acceptInvitation(UUID invitationId, UUID userId) {
        GroupInvitationEntity invitation = invitationRepository
                .findByIdAndInvitedUserIdAndIsActiveTrue(invitationId, userId)
                .orElseThrow(() -> DataNotFoundException.of("Invitation {0} not found", invitationId));

        if (!PENDING.equals(invitation.getStatus())) {
            throw AlreadyExistsException.of("Invitation {0} is no longer pending", invitationId);
        }

        invitation.setStatus(ACCEPTED);

        GroupMemberEntity member = GroupMemberEntity.builder()
                .group(invitation.getGroup())
                .user(invitation.getInvitedUser())
                .memberRole(invitation.getMemberRole())
                .build();
        groupMemberRepository.save(member);
        log.info("User {} accepted invitation {} to group {}", userId, invitationId, invitation.getGroup().getId());
    }

    @Transactional
    public void declineInvitation(UUID invitationId, UUID userId) {
        GroupInvitationEntity invitation = invitationRepository
                .findByIdAndInvitedUserIdAndIsActiveTrue(invitationId, userId)
                .orElseThrow(() -> DataNotFoundException.of("Invitation {0} not found", invitationId));

        if (!PENDING.equals(invitation.getStatus())) {
            throw AlreadyExistsException.of("Invitation {0} is no longer pending", invitationId);
        }

        invitation.setStatus(DECLINED);
        log.info("User {} declined invitation {} to group {}", userId, invitationId, invitation.getGroup().getId());
    }

    public List<GroupInvitationResponse> getGroupInvitations(UUID groupId, UUID requesterId) {
        requireOwner(groupId, requesterId);
        return invitationRepository.findByGroupIdAndStatusAndIsActiveTrue(groupId, PENDING)
                .stream()
                .map(invitationMapper::toResponse)
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID groupId, UUID invitationId, UUID requesterId) {
        requireOwner(groupId, requesterId);
        GroupInvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> DataNotFoundException.of("Invitation {0} not found", invitationId));
        if (!invitation.getGroup().getId().equals(groupId)) {
            throw DataNotFoundException.of("Invitation {0} does not belong to group {1}", invitationId, groupId);
        }
        if (!PENDING.equals(invitation.getStatus())) {
            throw AlreadyExistsException.of("Invitation {0} is no longer pending", invitationId);
        }
        invitation.setStatus(REVOKED);
        invitation.setIsActive(false);
        log.info("Revoked invitation {} from group {}", invitationId, groupId);
    }

    public List<GroupInvitationResponse> getMyInvitations(UUID userId) {
        return invitationRepository.findAllByInvitedUserIdAndStatusAndIsActiveTrue(userId, PENDING)
                .stream()
                .map(invitationMapper::toResponse)
                .toList();
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberId, UUID requesterId) {
        requireOwner(groupId, requesterId);

        GroupMemberEntity member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> DataNotFoundException.of("Group member {0} not found", memberId));
        if (!member.getGroup().getId().equals(groupId)) {
            throw DataNotFoundException.of("Member {0} does not belong to group {1}", memberId, groupId);
        }
        if (OWNER_ROLE.equals(member.getMemberRole())) {
            throw ForbiddenException.of("Cannot remove the group owner");
        }
        member.setIsActive(false);
        log.info("Removed member {} from group {}", memberId, groupId);
    }

    private void requireOwner(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndMemberRoleAndIsActiveTrue(groupId, userId, OWNER_ROLE)) {
            throw ForbiddenException.of("Only the group owner can perform this action");
        }
    }

    private GroupEntity getExisting(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of("Group {0} not found", id));
    }
}
