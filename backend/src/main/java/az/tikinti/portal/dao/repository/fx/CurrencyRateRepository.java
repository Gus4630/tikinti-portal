package az.tikinti.portal.dao.repository.fx;

import az.tikinti.portal.dao.entity.fx.CurrencyRateEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRateEntity, UUID> {

    Optional<CurrencyRateEntity> findByExchangeDateAndCurrencyCode(LocalDate exchangeDate, String currencyCode);

    boolean existsByExchangeDate(LocalDate exchangeDate);

    List<CurrencyRateEntity> findAllByExchangeDate(LocalDate exchangeDate);

    @Query(value = "SELECT pg_advisory_xact_lock(:namespace, :dateKey)", nativeQuery = true)
    void acquireCurrencyRateDateLock(@Param("namespace") int namespace, @Param("dateKey") int dateKey);
}
