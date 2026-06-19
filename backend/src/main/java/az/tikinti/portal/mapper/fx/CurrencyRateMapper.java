package az.tikinti.portal.mapper.fx;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import az.tikinti.portal.client.cbar.model.CbarCurrency;
import az.tikinti.portal.dao.entity.fx.CurrencyRateEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrencyRateMapper {

    @Mapping(target = "exchangeDate", source = "exchangeDate")
    @Mapping(target = "currencyCode", source = "currency.code")
    @Mapping(target = "currencyName", source = "currency.name")
    @Mapping(target = "nominal", source = "currency.nominal")
    @Mapping(target = "exchangeRate", expression = "java(new java.math.BigDecimal(currency.getValue().replace(\",\", \".\")))")
    CurrencyRateEntity toEntity(CbarCurrency currency, LocalDate exchangeDate);

    default List<CurrencyRateEntity> toEntityList(List<CbarCurrency> currencies, LocalDate exchangeDate) {
        return currencies.stream().map(c -> toEntity(c, exchangeDate)).toList();
    }
}
