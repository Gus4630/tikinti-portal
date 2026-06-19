package az.tikinti.portal.exception.model.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorMessage {

    public static final String DATA_NOT_FOUND_MESSAGE = "Data not found with {0} - {1}";
    public static final String ALREADY_EXISTS_MESSAGE = "Resource already exists with {0}: {1}";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "Unexpected internal server error";

    public static final String BUILDING_NOT_FOUND_MESSAGE = "Building not found with {0} - {1}";
    public static final String CATEGORY_NOT_FOUND_MESSAGE = "Category not found with {0} - {1}";
    public static final String SUPPLIER_NOT_FOUND_MESSAGE = "Supplier not found with {0} - {1}";
    public static final String EXPENSE_NOT_FOUND_MESSAGE = "Expense not found with {0} - {1}";
    public static final String USER_NOT_FOUND_MESSAGE = "User not found with {0} - {1}";
    public static final String CURRENCY_NOT_FOUND_MESSAGE = "Currency not found with {0} - {1}";

    public static final String DUPLICATE_INVOICE_MESSAGE =
            "Invoice already exists. Existing expense id: {0}";
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";
    public static final String REFRESH_TOKEN_INVALID_MESSAGE = "Refresh token is invalid or expired";
}
