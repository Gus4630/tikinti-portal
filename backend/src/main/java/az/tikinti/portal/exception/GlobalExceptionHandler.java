package az.tikinti.portal.exception;

import static az.tikinti.portal.exception.model.constant.ErrorCode.DUPLICATE_INVOICE;
import static az.tikinti.portal.exception.model.constant.ErrorCode.INTERNAL_SERVER;
import static az.tikinti.portal.exception.model.constant.ErrorCode.VALIDATION_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import az.tikinti.portal.model.dto.record.ErrorResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(DuplicateInvoiceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInvoice(DuplicateInvoiceException ex) {
        Map<String, Object> params = Map.of("expenseId", ex.getExistingExpenseId());
        return ResponseEntity.status(CONFLICT)
                .body(build(DUPLICATE_INVOICE, params));
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DataNotFoundException ex) {
        return ResponseEntity.status(NOT_FOUND)
                .body(build(ex.getErrorCode(), null));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(AlreadyExistsException ex) {
        return ResponseEntity.status(CONFLICT)
                .body(build(ex.getErrorCode(), null));
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorResponse> handleCommon(CommonException ex) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(build(ex.getErrorCode(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> params = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            params.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(BAD_REQUEST)
                .body(build(VALIDATION_ERROR, params));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(build(INTERNAL_SERVER, null));
    }

    private ErrorResponse build(String code, Map<String, Object> params) {
        return ErrorResponse.builder()
                .errorCode(code)
                .errorMessage(resolveMessage(code))
                .params(params)
                .requestId(MDC.get("traceId"))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String resolveMessage(String code) {
        try {
            return messageSource.getMessage(code, null, Locale.getDefault());
        } catch (NoSuchMessageException e) {
            return code;
        }
    }
}
