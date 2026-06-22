package az.tikinti.portal.service.building;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.BUILDING_NOT_FOUND_MESSAGE;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.building.BuildingEntity;
import az.tikinti.portal.dao.entity.building.BuildingMediaEntity;
import az.tikinti.portal.dao.entity.group.GroupEntity;
import az.tikinti.portal.dao.repository.building.BuildingMediaRepository;
import az.tikinti.portal.dao.repository.building.BuildingRepository;
import az.tikinti.portal.dao.repository.expense.ExpenseRepository;
import az.tikinti.portal.dao.repository.group.GroupMemberRepository;
import az.tikinti.portal.dao.repository.group.GroupRepository;
import az.tikinti.portal.dao.specification.BuildingSpecification;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.exception.ForbiddenException;
import az.tikinti.portal.mapper.building.BuildingMapper;
import az.tikinti.portal.model.dto.request.building.BuildingFilterRequest;
import az.tikinti.portal.model.dto.request.building.BuildingMediaRequest;
import az.tikinti.portal.model.dto.request.building.BuildingRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.building.BuildingMediaResponse;
import az.tikinti.portal.model.dto.response.building.BuildingResponse;
import az.tikinti.portal.model.enums.ExpenseStatus;
import az.tikinti.portal.util.PageUtil;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingMediaRepository buildingMediaRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final BuildingMapper buildingMapper;

    public PageableResponse<BuildingResponse> search(BuildingFilterRequest request, UUID userId) {
        var allowedGroupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        Pageable pageable = PageUtil.createPageable(request);
        var spec = BuildingSpecification.getSpecification(request, allowedGroupIds);
        var page = buildingRepository.findAll(spec, pageable);
        var response = buildingMapper.toResponse(page);
        response.getContent().forEach(b -> {
            b.setMedia(getMediaForBuilding(b.getId()));
            b.setTotalSpent(expenseRepository.sumAmountBaseCurrencyByBuilding(b.getId(), ExpenseStatus.APPROVED));
        });
        return response;
    }

    public BuildingResponse getById(UUID id) {
        var response = buildingMapper.toResponse(getExistingEntity(id));
        response.setMedia(getMediaForBuilding(id));
        response.setTotalSpent(expenseRepository.sumAmountBaseCurrencyByBuilding(id, ExpenseStatus.APPROVED));
        return response;
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        var entity = buildingMapper.toEntity(request);
        entity.setGroup(resolveGroup(request.getGroupId()));
        var saved = buildingRepository.save(entity);
        if (request.getMedia() != null) {
            saveMedia(saved, request.getMedia());
        }
        log.info("Successfully CREATED Building with ID - {}", saved.getId());
        var response = buildingMapper.toResponse(saved);
        response.setMedia(getMediaForBuilding(saved.getId()));
        return response;
    }

    @Transactional
    public BuildingResponse update(UUID id, BuildingRequest request) {
        var entity = getExistingEntity(id);
        buildingMapper.updateEntityFromRequest(request, entity);
        entity.setGroup(resolveGroup(request.getGroupId()));
        log.info("Successfully UPDATED Building with ID - {}", id);
        var response = buildingMapper.toResponse(entity);
        response.setMedia(getMediaForBuilding(id));
        return response;
    }

    @Transactional
    public BuildingMediaResponse addMedia(UUID buildingId, BuildingMediaRequest request) {
        var building = getExistingEntity(buildingId);
        var media = buildingMapper.toMediaEntity(request);
        media.setBuilding(building);
        var saved = buildingMediaRepository.save(media);
        log.info("Added media {} to Building {}", saved.getId(), buildingId);
        return buildingMapper.toMediaResponse(saved);
    }

    @Transactional
    public void removeMedia(UUID buildingId, UUID mediaId) {
        var media = buildingMediaRepository.findById(mediaId)
                .filter(m -> m.getBuilding().getId().equals(buildingId))
                .orElseThrow(() -> DataNotFoundException.of("Media {0} not found on building {1}", mediaId, buildingId));
        media.setIsActive(false);
        log.info("Removed media {} from Building {}", mediaId, buildingId);
    }

    @Transactional
    public void delete(UUID id) {
        var entity = getExistingEntity(id);
        entity.setIsActive(false);
        log.info("Successfully DEACTIVATED (soft-deleted) Building with ID - {}", id);
    }

    public BuildingEntity getExistingEntity(UUID id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of(BUILDING_NOT_FOUND_MESSAGE,
                        BaseEntity.Fields.id, id));
    }

    public void verifyBuildingAccess(UUID buildingId, UUID userId) {
        var allowedGroupIds = groupMemberRepository.findGroupIdsByUserId(userId);
        var building = getExistingEntity(buildingId);
        if (!allowedGroupIds.contains(building.getGroup().getId())) {
            throw ForbiddenException.of("Access denied to building {0}", buildingId);
        }
    }

    private List<BuildingMediaResponse> getMediaForBuilding(UUID buildingId) {
        return buildingMediaRepository
                .findAllByBuildingIdAndIsActiveTrueOrderByDisplayOrderAsc(buildingId)
                .stream().map(buildingMapper::toMediaResponse).toList();
    }

    private GroupEntity resolveGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> DataNotFoundException.of("Group {0} not found", groupId));
    }

    private void saveMedia(BuildingEntity building, List<BuildingMediaRequest> mediaRequests) {
        mediaRequests.forEach(req -> {
            var media = buildingMapper.toMediaEntity(req);
            media.setBuilding(building);
            buildingMediaRepository.save(media);
        });
    }
}
