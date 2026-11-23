package com.dsu.roomapi;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException e, HttpServletRequest request) {
        var fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldMessage(f.getField(), f.getDefaultMessage()))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "request has invalid fields", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "request body is missing or is not valid JSON", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> wrongType(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "'" + e.getName() + "' has the wrong type", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException e, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized(AuthenticationException e, HttpServletRequest request) {
        String message = "login required";
        if (e instanceof OAuth2AuthenticationException) {
            message = "token is invalid or expired";
        } else if (e instanceof BadCredentialsException) {
            message = "wrong username or password";
        }
        return respond(HttpStatus.UNAUTHORIZED, message, request, List.of());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forbidden(ForbiddenException e, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, e.getMessage(), request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "admin role required", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse spring && spring.getStatusCode().is4xxClientError()) {
            var status = HttpStatus.valueOf(spring.getStatusCode().value());
            var body = ApiError.of(status, spring.getBody().getDetail(), request.getRequestURI(), List.of());
            return ResponseEntity.status(status).headers(spring.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON).body(body);
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected server error", request, List.of());
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, String message,
                                             HttpServletRequest request, List<ApiError.FieldMessage> fields) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ApiError.of(status, message, request.getRequestURI(), fields));
    }
}
