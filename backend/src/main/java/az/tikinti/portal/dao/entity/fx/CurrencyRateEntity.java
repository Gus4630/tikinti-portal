package az.tikinti.portal.dao.entity.fx;

import az.tikinti.portal.dao.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "currency_rates")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CurrencyRateEntity extends BaseEntity {

    @Column(name = "exchange_date", nullable = false)
    private LocalDate exchangeDate;

    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;

    @Column(name = "currency_name", nullable = false)
    private String currencyName;

    @Column(name = "nominal", nullable = false)
    private String nominal;

    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;
}
