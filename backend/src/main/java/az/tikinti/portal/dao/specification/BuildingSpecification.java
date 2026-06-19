package az.tikinti.portal.dao.specification;

import static az.tikinti.portal.dao.specification.BaseSpecification.equalPredicate;
import static az.tikinti.portal.dao.specification.BaseSpecification.likePredicate;

import az.tikinti.portal.dao.entity.building.BuildingEntity;
import az.tikinti.portal.model.dto.request.building.BuildingFilterRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuildingSpecification {

    public static Specification<BuildingEntity> getSpecification(BuildingFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            likePredicate(request.getName(), root.get("name"), cb).ifPresent(predicates::add);
            equalPredicate(request.getIsActive(), root.get("isActive"), cb).ifPresent(predicates::add);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
