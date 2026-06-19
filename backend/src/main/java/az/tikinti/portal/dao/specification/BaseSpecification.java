package az.tikinti.portal.dao.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaseSpecification {

    public static <T> Optional<Predicate> equalPredicate(T value, Path<T> path, CriteriaBuilder cb) {
        return Optional.ofNullable(value).map(v -> cb.equal(path, v));
    }

    public static Optional<Predicate> likePredicate(String value, Path<String> path, CriteriaBuilder cb) {
        return Optional.ofNullable(value)
                .filter(v -> !v.isBlank())
                .map(v -> cb.like(cb.lower(path), "%" + v.toLowerCase() + "%"));
    }

    public static <T extends Comparable<T>> Optional<Predicate> rangePredicate(
            T value, String condition, Path<T> path, CriteriaBuilder cb) {
        if (value == null) return Optional.empty();
        return Optional.of(switch (Optional.ofNullable(condition).orElse("=")) {
            case ">" -> cb.greaterThan(path, value);
            case ">=" -> cb.greaterThanOrEqualTo(path, value);
            case "<" -> cb.lessThan(path, value);
            case "<=" -> cb.lessThanOrEqualTo(path, value);
            default -> cb.equal(path, value);
        });
    }

    public static <T> Optional<Predicate> inPredicate(Collection<T> values, Path<T> path) {
        if (values == null || values.isEmpty()) return Optional.empty();
        return Optional.of(path.in(values));
    }
}
