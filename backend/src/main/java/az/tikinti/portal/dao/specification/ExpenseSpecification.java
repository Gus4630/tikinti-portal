package az.tikinti.portal.dao.specification;

import static az.tikinti.portal.dao.specification.BaseSpecification.equalPredicate;
import static az.tikinti.portal.dao.specification.BaseSpecification.rangePredicate;

import az.tikinti.portal.dao.entity.expense.ExpenseEntity;
import az.tikinti.portal.model.dto.request.expense.ExpenseFilterRequest;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExpenseSpecification {

    public static Specification<ExpenseEntity> getSpecification(ExpenseFilterRequest request, Collection<UUID> allowedBuildingIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (allowedBuildingIds.isEmpty()) {
                predicates.add(cb.disjunction());
            } else if (request.getBuildingId() != null && allowedBuildingIds.contains(request.getBuildingId())) {
                equalPredicate(request.getBuildingId(), root.get("building").get("id"), cb).ifPresent(predicates::add);
            } else {
                predicates.add(root.get("building").get("id").in(allowedBuildingIds));
            }
            equalPredicate(request.getCategoryId(), root.get("category").get("id"), cb)
                    .ifPresent(predicates::add);
            equalPredicate(request.getSupplierId(), root.get("supplier").get("id"), cb)
                    .ifPresent(predicates::add);
            equalPredicate(request.getStatus(), root.get("status"), cb)
                    .ifPresent(predicates::add);
            equalPredicate(request.getCurrency(), root.get("currency"), cb)
                    .ifPresent(predicates::add);
            rangePredicate(request.getDateFrom(), ">=", root.get("expenseDate"), cb)
                    .ifPresent(predicates::add);
            rangePredicate(request.getDateTo(), "<=", root.get("expenseDate"), cb)
                    .ifPresent(predicates::add);
            equalPredicate(request.getIsActive(), root.get("isActive"), cb)
                    .ifPresent(predicates::add);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
