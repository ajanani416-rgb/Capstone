package com.salon.management.exception;

import com.salon.management.dto.ErrorResponse;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Maps domain failures to the standard envelope — user-friendly messages,
 * field-level detail for 400s, and never a stack trace or secret. */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Method-security (@PreAuthorize) denials surface inside the
     * DispatcherServlet, ahead of Spring Security's own translation. Rethrow
     * so the entry point / access-denied handler answer 401/403 JSON instead
     * of this advice's 500 envelope. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void securityPassthrough(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() == null ? "Invalid value." : e.getDefaultMessage(),
                        (first, second) -> first));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Validation failed. Review the highlighted fields.", fields));
    }

    @ExceptionHandler(DuplicatePhoneException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicatePhoneException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> credentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnverifiedPhoneException.class)
    public ResponseEntity<ErrorResponse> unverified(UnverifiedPhoneException ex) {
        // 403 (not 401): the password was correct, only the OTP step remains.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> invalidOtp(InvalidOtpException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> otpExpired(OtpExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(SlotTakenException.class)
    public ResponseEntity<ErrorResponse> slotTaken(SlotTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> transition(IllegalStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    /** Service-layer 400/404s with a safe reason — keeps the envelope uniform
     * instead of Spring's default HTML/whitelabel body. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> fallback(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Something went wrong. Please retry."));
    }
}
