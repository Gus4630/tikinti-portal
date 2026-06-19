package az.tikinti.portal.model.dto.record;

import java.time.LocalDateTime;

public record ErrorResponse(String errorCode, String message, Object details, LocalDateTime timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(String code, String message, Object details) {
        return new ErrorResponse(code, message, details, LocalDateTime.now());
    }
}
