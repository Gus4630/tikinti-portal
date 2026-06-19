package az.tikinti.portal.exception;

import static az.tikinti.portal.exception.model.constant.ErrorCode.ALREADY_EXIST;

import java.text.MessageFormat;

public class AlreadyExistsException extends CommonException {

    public AlreadyExistsException(String message) {
        super(ALREADY_EXIST, message);
    }

    public static AlreadyExistsException of(String message, Object... args) {
        return new AlreadyExistsException(MessageFormat.format(message, args));
    }
}
