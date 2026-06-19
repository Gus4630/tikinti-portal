package az.tikinti.portal.mapper.supplier;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import az.tikinti.portal.model.dto.request.supplier.SupplierRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.supplier.SupplierResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

    @Mapping(target = "totalAdvancedPaid", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "retainagePercentage", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "retainageHeldAmount", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    SupplierEntity toEntity(SupplierRequest request);

    SupplierResponse toResponse(SupplierEntity entity);

    PageableResponse<SupplierResponse> toResponse(Page<SupplierEntity> page);

    void updateEntityFromRequest(SupplierRequest request, @MappingTarget SupplierEntity entity);
}
