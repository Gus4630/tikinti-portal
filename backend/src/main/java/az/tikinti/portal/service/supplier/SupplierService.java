package az.tikinti.portal.service.supplier;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.SUPPLIER_NOT_FOUND_MESSAGE;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import az.tikinti.portal.dao.repository.supplier.SupplierRepository;
import az.tikinti.portal.dao.specification.SupplierSpecification;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.supplier.SupplierMapper;
import az.tikinti.portal.model.dto.request.supplier.SupplierFilterRequest;
import az.tikinti.portal.model.dto.request.supplier.SupplierRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.supplier.SupplierResponse;
import az.tikinti.portal.util.PageUtil;
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
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    public PageableResponse<SupplierResponse> search(SupplierFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);
        var spec = SupplierSpecification.getSpecification(request);
        return supplierMapper.toResponse(supplierRepository.findAll(spec, pageable));
    }

    public SupplierResponse getById(UUID id) {
        return supplierMapper.toResponse(getExistingEntity(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        var entity = supplierMapper.toEntity(request);
        var saved = supplierRepository.save(entity);
        log.info("Successfully CREATED Supplier with ID - {}", saved.getId());
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        var entity = getExistingEntity(id);
        supplierMapper.updateEntityFromRequest(request, entity);
        log.info("Successfully UPDATED Supplier with ID - {}", id);
        return supplierMapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        var entity = getExistingEntity(id);
        entity.setIsActive(false);
        log.info("Successfully DEACTIVATED Supplier with ID - {}", id);
    }

    public SupplierEntity getExistingEntity(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of(SUPPLIER_NOT_FOUND_MESSAGE,
                        BaseEntity.Fields.id, id));
    }
}
