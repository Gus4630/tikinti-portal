package az.tikinti.portal.dao.entity.building;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.group.GroupEntity;
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
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Data
@Entity
@Audited
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "buildings")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class BuildingEntity extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "description")
    private String description;

    @Column(name = "floor_area_m2")
    private BigDecimal floorAreaM2;

    @Column(name = "budget_limit")
    private BigDecimal budgetLimit;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;
}
