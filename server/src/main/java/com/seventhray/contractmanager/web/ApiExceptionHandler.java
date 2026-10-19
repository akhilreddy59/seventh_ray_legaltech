package com.seventhray.contractmanager.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ContractNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<Map<String, String>> invalidTransition(InvalidStatusTransitionException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidUploadException.class, MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException validation && validation.getBindingResult().getFieldError() != null
                ? validation.getBindingResult().getFieldError().getDefaultMessage()
                : ex.getMessage();
        return response(HttpStatus.BAD_REQUEST, message == null ? "The request is invalid." : message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadTooLarge() {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 10 MB limit.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> invalidMultipart() {
        return response(HttpStatus.BAD_REQUEST, "Upload a PDF or DOCX file using the file field.");
    }

    @ExceptionHandler({StorageException.class, DataAccessException.class})
    public ResponseEntity<Map<String, String>> dependencyUnavailable(Exception ex) {
        log.error("Contract Manager dependency failure", ex);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "The database or document storage is temporarily unavailable.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception ex) {
        log.error("Unhandled Contract Manager API error", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred.");
    }

    private static ResponseEntity<Map<String, String>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
