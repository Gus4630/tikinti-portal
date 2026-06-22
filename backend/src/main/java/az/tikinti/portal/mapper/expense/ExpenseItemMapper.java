package az.tikinti.portal.mapper.expense;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.dao.entity.expense.ExpenseItemEntity;
import az.tikinti.portal.model.dto.record.OcrExtractionResult;
import az.tikinti.portal.model.dto.request.expense.ExpenseItemRequest;
import az.tikinti.portal.model.dto.response.expense.ExpenseItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseItemMapper {

    @Mapping(target = "expense", source = "expense")
    @Mapping(target = "description", source = "lineItem.description")
    @Mapping(target = "quantity", source = "lineItem.quantity")
    @Mapping(target = "unitPrice", source = "lineItem.unitPrice")
    @Mapping(target = "totalPrice",
            expression = "java(lineItem.unitPrice().multiply(java.math.BigDecimal.valueOf(lineItem.quantity())))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ExpenseItemEntity toEntity(OcrExtractionResult.LineItem lineItem, ExpenseEntity expense);

    @Mapping(target = "expense", source = "expense")
    @Mapping(target = "totalPrice",
            expression = "java(request.getUnitPrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity())))")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ExpenseItemEntity toEntity(ExpenseItemRequest request, ExpenseEntity expense);

    ExpenseItemResponse toResponse(ExpenseItemEntity entity);
}
