package az.tikinti.portal.dao.entity.building;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.category.CategoryEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "building_category_budgets")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BuildingCategoryBudgetEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingEntity building;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "budget_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
