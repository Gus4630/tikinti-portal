package az.tikinti.portal.dao.entity.supplier;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.model.enums.SupplierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

@Data
@Entity
@Audited
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "suppliers")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SupplierEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false, length = 20)
    private SupplierType supplierType;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "total_advanced_paid", nullable = false)
    private BigDecimal totalAdvancedPaid;

    @Column(name = "retainage_percentage", nullable = false)
    private BigDecimal retainagePercentage;

    @Column(name = "retainage_held_amount", nullable = false)
    private BigDecimal retainageHeldAmount;
}
