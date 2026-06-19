package az.tikinti.portal.dao.entity.expense;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.building.BuildingEntity;
import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.dao.entity.supplier.SupplierEntity;
import az.tikinti.portal.model.enums.ExpenseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

@Data
@Entity
@Audited
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "expenses")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ExpenseEntity extends BaseEntity {

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingEntity building;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private SupplierEntity supplier;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    @Column(name = "amount_base_currency", nullable = false)
    private BigDecimal amountBaseCurrency;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
}
