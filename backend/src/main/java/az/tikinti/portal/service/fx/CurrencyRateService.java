package az.tikinti.portal.service.fx;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.CURRENCY_NOT_FOUND_MESSAGE;
import static az.tikinti.portal.model.constant.CbarConstants.BASE_CURRENCY;
import static az.tikinti.portal.model.constant.CbarConstants.CBAR_DATE_FORMATTER;
import static az.tikinti.portal.model.constant.CbarConstants.DEFAULT_ROUNDING_MODE;
import static az.tikinti.portal.model.constant.CbarConstants.EXCHANGE_RATE_SCALE;
import static az.tikinti.portal.model.constant.CbarConstants.FINAL_AMOUNT_SCALE;
import static az.tikinti.portal.model.constant.CbarConstants.FOREIGN_CURRENCY_TYPE;

import az.tikinti.portal.client.cbar.CbarClient;
import az.tikinti.portal.client.cbar.model.CbarCurrencyResponse;
import az.tikinti.portal.client.cbar.model.CbarCurrencyType;
import az.tikinti.portal.dao.entity.fx.CurrencyRateEntity;
import az.tikinti.portal.dao.repository.fx.CurrencyRateRepository;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.mapper.fx.CurrencyRateMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyRateService {

    private static final int CURRENCY_RATE_LOCK_NAMESPACE = 901201;

    private final CbarClient cbarClient;
    private final CurrencyRateRepository repository;
    private final CurrencyRateMapper currencyRateMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * Convert sourceAmount from sourceCurrency to targetCurrency on the given date.
     * Returns BigDecimal[]{convertedAmount, exchangeRate}.
     */
    public BigDecimal[] exchangeCurrency(String sourceCurrency, String targetCurrency,
                                         BigDecimal sourceAmount, LocalDate exchangeDate) {
        ensureRatesLoaded(exchangeDate, sourceCurrency, targetCurrency);

        BigDecimal fromRate = getRate(exchangeDate, sourceCurrency);
        BigDecimal toRate = getRate(exchangeDate, targetCurrency);

        BigDecimal exchangeRate = fromRate.divide(toRate, EXCHANGE_RATE_SCALE, DEFAULT_ROUNDING_MODE);
        BigDecimal convertedAmount = sourceAmount.multiply(exchangeRate)
                .setScale(FINAL_AMOUNT_SCALE, DEFAULT_ROUNDING_MODE);

        return new BigDecimal[]{convertedAmount, exchangeRate};
    }

    private void ensureRatesLoaded(LocalDate date, String from, String to) {
        if (isBase(from) && isBase(to)) return;
        if (repository.existsByExchangeDate(date)) return;
        fetchAndSaveCurrencyRates(date);
    }

    private BigDecimal getRate(LocalDate date, String currencyCode) {
        String code = currencyCode.toUpperCase(Locale.ROOT);
        if (BASE_CURRENCY.equals(code)) return BigDecimal.ONE;
        return repository.findByExchangeDateAndCurrencyCode(date, code)
                .map(CurrencyRateEntity::getExchangeRate)
                .orElseThrow(() -> DataNotFoundException.of(CURRENCY_NOT_FOUND_MESSAGE, "currencyCode", code));
    }

    public List<CurrencyRateEntity> fetchAndSaveCurrencyRates(LocalDate date) {
        if (repository.existsByExchangeDate(date)) {
            return repository.findAllByExchangeDate(date);
        }
        String cbarDate = date.format(CBAR_DATE_FORMATTER);
        log.info("Fetching CBAR currency rates for date: {}", cbarDate);

        CbarCurrencyResponse response = cbarClient.getCurrencies(cbarDate);
        CbarCurrencyType foreignCurrencies = response.getCurrencyTypes().stream()
                .filter(v -> FOREIGN_CURRENCY_TYPE.equalsIgnoreCase(v.getType()))
                .findFirst()
                .orElseThrow(() -> DataNotFoundException.of(CURRENCY_NOT_FOUND_MESSAGE, "date", date));

        List<CurrencyRateEntity> rates = currencyRateMapper.toEntityList(foreignCurrencies.getCurrencies(), date);
        return Objects.requireNonNull(
                transactionTemplate.execute(status -> saveRatesWithLock(date, cbarDate, rates)));
    }

    private List<CurrencyRateEntity> saveRatesWithLock(LocalDate date, String cbarDate,
                                                        List<CurrencyRateEntity> rates) {
        int dateKey = Math.toIntExact(date.toEpochDay());
        repository.acquireCurrencyRateDateLock(CURRENCY_RATE_LOCK_NAMESPACE, dateKey);

        if (repository.existsByExchangeDate(date)) {
            log.info("Rates for {} already saved by another request", cbarDate);
            return repository.findAllByExchangeDate(date);
        }
        List<CurrencyRateEntity> saved = repository.saveAll(rates);
        log.info("Saved {} currency rates for {}", saved.size(), cbarDate);
        return saved;
    }

    private boolean isBase(String currencyCode) {
        return BASE_CURRENCY.equals(currencyCode.toUpperCase(Locale.ROOT));
    }
}
