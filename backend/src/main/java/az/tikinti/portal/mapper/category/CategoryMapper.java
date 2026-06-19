package az.tikinti.portal.mapper.category;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.model.dto.request.category.CategoryRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.category.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    CategoryEntity toEntity(CategoryRequest request);

    CategoryResponse toResponse(CategoryEntity entity);

    PageableResponse<CategoryResponse> toResponse(Page<CategoryEntity> page);

    void updateEntityFromRequest(CategoryRequest request, @MappingTarget CategoryEntity entity);
}
