package az.tikinti.portal.exception.model.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorCode {

    public static final String DATA_NOT_FOUND             = "0001";
    public static final String ALREADY_EXIST              = "0002";
    public static final String DUPLICATE_INVOICE          = "0003";
    public static final String BAD_REQUEST                = "0004";
    public static final String CONFLICT                   = "0005";
    public static final String FORBIDDEN                  = "0006";
    public static final String UNAUTHORIZED               = "0007";
    public static final String INTERNAL_SERVER            = "0008";
    public static final String VALIDATION_ERROR           = "0009";
    public static final String UNIQUE_ERROR_CODE          = "0010";
    public static final String BUILDING_NOT_FOUND         = "0011";
    public static final String CATEGORY_NOT_FOUND         = "0012";
    public static final String SUPPLIER_NOT_FOUND         = "0013";
    public static final String EXPENSE_NOT_FOUND          = "0014";
    public static final String USER_NOT_FOUND             = "0015";
    public static final String CURRENCY_NOT_FOUND         = "0016";
    public static final String INVALID_CREDENTIALS        = "0017";
    public static final String REFRESH_TOKEN_INVALID      = "0018";
    public static final String OCR_RETRIGGER_NOT_ALLOWED  = "0019";
    public static final String OCR_NO_FILE                = "0020";
}
