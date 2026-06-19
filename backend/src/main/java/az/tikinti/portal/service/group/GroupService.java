package az.tikinti.portal.service.group;

import az.tikinti.portal.dao.entity.auth.UserEntity;
import az.tikinti.portal.dao.entity.group.GroupEntity;
import az.tikinti.portal.dao.entity.group.GroupMemberEntity;
import az.tikinti.portal.dao.repository.auth.UserRepository;
import az.tikinti.portal.dao.repository.group.GroupMemberRepository;
import az.tikinti.portal.dao.repository.group.GroupRepository;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.group.GroupMapper;
import az.tikinti.portal.model.dto.request.group.AddGroupMemberRequest;
import az.tikinti.portal.model.dto.request.group.GroupRequest;
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

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    public List<GroupResponse> findAll() {
        return groupRepository.findAll().stream()
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
    public GroupResponse create(GroupRequest request) {
        GroupEntity entity = GroupEntity.builder()
                .name(request.getName())
                .build();
        GroupEntity saved = groupRepository.save(entity);
        log.info("Created Group {}", saved.getId());
        return groupMapper.toResponse(saved);
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
    public GroupMemberResponse addMember(UUID groupId, AddGroupMemberRequest request) {
        GroupEntity group = getExisting(groupId);
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, request.getUserId())) {
            return groupMemberRepository.findByGroupIdAndUserId(groupId, request.getUserId())
                    .map(groupMapper::toMemberResponse)
                    .orElseThrow();
        }
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> DataNotFoundException.of("User {0} not found", request.getUserId()));
        GroupMemberEntity member = GroupMemberEntity.builder()
                .group(group)
                .user(user)
                .memberRole(request.getMemberRole())
                .build();
        GroupMemberEntity saved = groupMemberRepository.save(member);
        log.info("Added user {} to group {}", request.getUserId(), groupId);
        return groupMapper.toMemberResponse(saved);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID memberId) {
        GroupMemberEntity member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> DataNotFoundException.of("Group member {0} not found", memberId));
        if (!member.getGroup().getId().equals(groupId)) {
            throw DataNotFoundException.of("Member {0} does not belong to group {1}", memberId, groupId);
        }
        member.setIsActive(false);
        log.info("Removed member {} from group {}", memberId, groupId);
    }

    private GroupEntity getExisting(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of("Group {0} not found", id));
    }
}
