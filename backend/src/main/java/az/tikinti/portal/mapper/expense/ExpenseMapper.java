package az.tikinti.portal.mapper.expense;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseMediaEntity;
import az.tikinti.portal.model.dto.request.expense.ExpenseMediaRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseMediaResponse;
import az.tikinti.portal.model.dto.response.expense.ExpenseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseMapper {

    @Mapping(target = "buildingId", source = "building.id")
    @Mapping(target = "buildingName", source = "building.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryItemCode", source = "category.itemCode")
    @Mapping(target = "categoryItemName", source = "category.itemName")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "media", ignore = true)
    ExpenseResponse toResponse(ExpenseEntity entity);

    PageableResponse<ExpenseResponse> toResponse(Page<ExpenseEntity> page);

    @Mapping(target = "expense", ignore = true)
    ExpenseMediaEntity toMediaEntity(ExpenseMediaRequest request);

    ExpenseMediaResponse toMediaResponse(ExpenseMediaEntity entity);
}
