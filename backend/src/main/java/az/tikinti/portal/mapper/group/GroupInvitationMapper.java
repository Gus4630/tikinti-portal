package az.tikinti.portal.mapper.group;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.group.GroupInvitationEntity;
import az.tikinti.portal.model.dto.response.group.GroupInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupInvitationMapper {

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "invitedUserId", source = "invitedUser.id")
    @Mapping(target = "invitedUserUsername", source = "invitedUser.username")
    @Mapping(target = "invitedUserFullName", source = "invitedUser.fullName")
    @Mapping(target = "invitedById", source = "invitedBy.id")
    @Mapping(target = "invitedByUsername", source = "invitedBy.username")
    @Mapping(target = "invitedByFullName", source = "invitedBy.fullName")
    GroupInvitationResponse toResponse(GroupInvitationEntity entity);
}
