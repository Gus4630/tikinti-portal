package az.tikinti.portal.exception;

import static az.tikinti.portal.exception.model.constant.ErrorCode.DUPLICATE_INVOICE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.DUPLICATE_INVOICE_MESSAGE;

import java.text.MessageFormat;
import java.util.UUID;
import lombok.Getter;

@Getter
public class DuplicateInvoiceException extends CommonException {

    private final UUID existingExpenseId;

    public DuplicateInvoiceException(UUID existingExpenseId) {
        super(DUPLICATE_INVOICE, MessageFormat.format(DUPLICATE_INVOICE_MESSAGE, existingExpenseId));
        this.existingExpenseId = existingExpenseId;
    }
}
