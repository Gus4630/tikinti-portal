package az.tikinti.portal.exception;

public class NonRetryableOcrException extends RuntimeException {

    public NonRetryableOcrException(String message) {
        super(message);
    }

    public NonRetryableOcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
