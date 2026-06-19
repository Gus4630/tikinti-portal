package az.tikinti.portal.mapper.building;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.building.BuildingEntity;
import az.tikinti.portal.dao.entity.building.BuildingMediaEntity;
import az.tikinti.portal.model.dto.request.building.BuildingMediaRequest;
import az.tikinti.portal.model.dto.request.building.BuildingRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.building.BuildingMediaResponse;
import az.tikinti.portal.model.dto.response.building.BuildingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BuildingMapper {

    @Mapping(target = "group", ignore = true)
    BuildingEntity toEntity(BuildingRequest request);

    @Mapping(target = "media", ignore = true)
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    BuildingResponse toResponse(BuildingEntity entity);

    PageableResponse<BuildingResponse> toResponse(Page<BuildingEntity> page);

    @Mapping(target = "group", ignore = true)
    void updateEntityFromRequest(BuildingRequest request, @MappingTarget BuildingEntity entity);

    @Mapping(target = "building", ignore = true)
    BuildingMediaEntity toMediaEntity(BuildingMediaRequest request);

    BuildingMediaResponse toMediaResponse(BuildingMediaEntity entity);
}
