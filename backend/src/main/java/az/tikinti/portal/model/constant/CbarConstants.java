package az.tikinti.portal.model.constant;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CbarConstants {

    public static final String BASE_CURRENCY = "AZN";
    public static final String FOREIGN_CURRENCY_TYPE = "Xarici valyutalar";
    public static final DateTimeFormatter CBAR_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static final int EXCHANGE_RATE_SCALE = 10;
    public static final int FINAL_AMOUNT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
}
