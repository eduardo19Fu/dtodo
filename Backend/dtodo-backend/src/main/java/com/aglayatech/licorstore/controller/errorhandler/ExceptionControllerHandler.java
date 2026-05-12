package com.aglayatech.licorstore.controller.errorhandler;

import com.aglayatech.licorstore.error.ErrorDTO;
import com.aglayatech.licorstore.error.exceptions.DataAccessException;
import com.aglayatech.licorstore.error.exceptions.DuplicateNotaCreditoException;
import com.aglayatech.licorstore.error.exceptions.MethodArgumentTypeMismatchException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.NumberFormatException;
import com.aglayatech.licorstore.error.exceptions.ParseException;
import com.aglayatech.licorstore.error.exceptions.ReportGenerationException;
import com.aglayatech.licorstore.error.exceptions.SQLException;

import com.aglayatech.licorstore.error.exceptions.SigningDocumentFelException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class ExceptionControllerHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {NoContentException.class})
    public ResponseEntity<ErrorDTO> noContentExceptionHandler(RuntimeException exception) {
        log.error("There is no content available", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.NO_CONTENT.value());
        errorDTO.setStatus(HttpStatus.NO_CONTENT);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.NO_CONTENT);
    }

    @ExceptionHandler(value = {NotFoundException.class})
    public ResponseEntity<ErrorDTO> notFoundExceptionHandler(RuntimeException exception) {
        log.error("The element was not found: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.NOT_FOUND.value());
        errorDTO.setStatus(HttpStatus.NOT_FOUND);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {DataAccessException.class})
    public ResponseEntity<ErrorDTO> dataAccessExceptionHandler(DataAccessException exception) {
        log.error("The data was not accessible for the application: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {SQLException.class})
    public ResponseEntity<ErrorDTO> sQLExceptionHandler(SQLException exception) {
        log.error("An SQLException has occurred: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {NumberFormatException.class})
    public ResponseEntity<ErrorDTO> numberFormatExceptionHandler(NumberFormatException exception) {
        log.error("A number format exception has happen", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.CONFLICT.value());
        errorDTO.setStatus(HttpStatus.CONFLICT);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorDTO> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException exception) {
        log.error("A bad request has happen", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.BAD_REQUEST.value());
        errorDTO.setStatus(HttpStatus.BAD_REQUEST);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = ParseException.class)
    public ResponseEntity<ErrorDTO> parseExceptionHandler(ParseException exception) {
        log.error("An error has ocurred trying to parse a value");
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = SigningDocumentFelException.class)
    public ResponseEntity<ErrorDTO> parseExceptionHandler(SigningDocumentFelException exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = ReportGenerationException.class)
    public ResponseEntity<Object> reportGenerationException(ReportGenerationException ex, WebRequest request) {
        log.error("An exception ocurred while generating the report in path: {}, Exception: {}", request.getContextPath(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al generar reporte".concat(ex.getMessage()));
    }

    @ExceptionHandler(value = {DuplicateNotaCreditoException.class})
    public ResponseEntity<ErrorDTO> duplicateNotaCreditoExceptionHandler(DuplicateNotaCreditoException exception) {
        log.error("Intento de crear una nota de credito duplicada: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.CONFLICT.value());
        errorDTO.setStatus(HttpStatus.CONFLICT);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<>(errorDTO, HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorDTO> handlerException(Exception exception) {
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
