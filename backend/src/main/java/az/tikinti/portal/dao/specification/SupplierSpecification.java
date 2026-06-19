package az.tikinti.portal.dao.specification;

import static az.tikinti.portal.dao.specification.BaseSpecification.equalPredicate;
import static az.tikinti.portal.dao.specification.BaseSpecification.likePredicate;

import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import az.tikinti.portal.model.dto.request.supplier.SupplierFilterRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SupplierSpecification {

    public static Specification<SupplierEntity> getSpecification(SupplierFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            likePredicate(request.getName(), root.get("name"), cb).ifPresent(predicates::add);
            equalPredicate(request.getSupplierType(), root.get("supplierType"), cb).ifPresent(predicates::add);
            equalPredicate(request.getIsActive(), root.get("isActive"), cb).ifPresent(predicates::add);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
