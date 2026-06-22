package az.tikinti.portal.dao.entity.category;

import az.tikinti.portal.dao.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "categories")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CategoryEntity extends BaseEntity {

    @Column(name = "item_code", nullable = false, unique = true, length = 40)
    private String itemCode;

    @Column(name = "level_1", nullable = false, length = 100)
    private String level1;

    @Column(name = "level_2", nullable = false, length = 100)
    private String level2;

    @Column(name = "level_3", nullable = false, length = 100)
    private String level3;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "display_order")
    private Integer displayOrder;
}
