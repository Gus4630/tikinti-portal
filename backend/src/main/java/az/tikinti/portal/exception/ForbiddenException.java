package az.tikinti.portal.exception;

import static az.tikinti.portal.exception.model.constant.ErrorCode.FORBIDDEN;

import java.text.MessageFormat;

public class ForbiddenException extends CommonException {

    public ForbiddenException(String message) {
        super(FORBIDDEN, message);
    }

    public static ForbiddenException of(String message, Object... args) {
        return new ForbiddenException(MessageFormat.format(message, args));
    }
}
