package az.tikinti.portal.service.category;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.CATEGORY_NOT_FOUND_MESSAGE;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.dao.repository.category.CategoryRepository;
import az.tikinti.portal.dao.specification.CategorySpecification;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.category.CategoryMapper;
import az.tikinti.portal.model.dto.request.category.CategoryFilterRequest;
import az.tikinti.portal.model.dto.request.category.CategoryRequest;
import az.tikinti.portal.model.dto.response.PageableResponse;
import az.tikinti.portal.model.dto.response.category.CategoryResponse;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public PageableResponse<CategoryResponse> search(CategoryFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);
        var spec = CategorySpecification.getSpecification(request);
        return categoryMapper.toResponse(categoryRepository.findAll(spec, pageable));
    }

    public CategoryResponse getById(UUID id) {
        return categoryMapper.toResponse(getExistingEntity(id));
    }

    public List<CategoryResponse> getAllActive() {
        return categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        var entity = categoryMapper.toEntity(request);
        var saved = categoryRepository.save(entity);
        log.info("Successfully CREATED Category with ID - {}", saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        var entity = getExistingEntity(id);
        categoryMapper.updateEntityFromRequest(request, entity);
        log.info("Successfully UPDATED Category with ID - {}", id);
        return categoryMapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        var entity = getExistingEntity(id);
        entity.setIsActive(false);
        log.info("Successfully DEACTIVATED Category with ID - {}", id);
    }

    public CategoryEntity getExistingEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> DataNotFoundException.of(CATEGORY_NOT_FOUND_MESSAGE,
                        BaseEntity.Fields.id, id));
    }
}
