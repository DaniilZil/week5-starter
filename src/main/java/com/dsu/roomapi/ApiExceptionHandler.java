package com.dsu.roomapi;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidFields(MethodArgumentNotValidException e, HttpServletRequest request)
    {
        List<ApiError.FieldMessage> fields = e.getBindingResult().getFieldErrors().stream().map(f -> new ApiError.FieldMessage(f.getField(), f.getDefaultMessage())).toList();
        return respond(HttpStatus.BAD_REQUEST,"request has invalid fields", request, fields);
    }
    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    public ResponseEntity<ApiError> notFound(ChangeSetPersister.NotFoundException e, HttpServletRequest request)
    {
        return respond(HttpStatus.NOT_FOUND,e.getMessage(), request, List.of());
    }
}
