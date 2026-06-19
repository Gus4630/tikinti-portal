package az.tikinti.portal.mapper.group;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.group.GroupEntity;
import az.tikinti.portal.dao.entity.group.GroupMemberEntity;
import az.tikinti.portal.model.dto.response.group.GroupMemberResponse;
import az.tikinti.portal.model.dto.response.group.GroupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GroupMapper {

    @Mapping(target = "members", ignore = true)
    GroupResponse toResponse(GroupEntity entity);

    @Mapping(target = "memberId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    GroupMemberResponse toMemberResponse(GroupMemberEntity entity);
}
