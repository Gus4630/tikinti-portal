package az.tikinti.portal.dao.specification;

import static az.tikinti.portal.dao.specification.BaseSpecification.equalPredicate;
import static az.tikinti.portal.dao.specification.BaseSpecification.inPredicate;
import static az.tikinti.portal.dao.specification.BaseSpecification.likePredicate;

import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.model.dto.request.category.CategoryFilterRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategorySpecification {

    public static Specification<CategoryEntity> getSpecification(CategoryFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            inPredicate(request.getLevel1s(), root.get("level1")).ifPresent(predicates::add);
            inPredicate(request.getLevel2s(), root.get("level2")).ifPresent(predicates::add);
            likePredicate(request.getItemName(), root.get("itemName"), cb).ifPresent(predicates::add);
            likePredicate(request.getItemCode(), root.get("itemCode"), cb).ifPresent(predicates::add);
            equalPredicate(request.getIsActive(), root.get("isActive"), cb).ifPresent(predicates::add);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
