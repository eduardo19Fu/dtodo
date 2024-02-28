package com.aglayatech.licorstore.controller.errorhandler;

import com.aglayatech.licorstore.error.ErrorDTO;
import com.aglayatech.licorstore.error.exceptions.BadRequestException;
import com.aglayatech.licorstore.error.exceptions.DataAccessException;
import com.aglayatech.licorstore.error.exceptions.NoContentException;
import com.aglayatech.licorstore.error.exceptions.NotFoundException;
import com.aglayatech.licorstore.error.exceptions.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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
        log.error("The element was not found", exception);
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
        log.error("The data was not accessible for the application", exception);
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
        log.error("An SQLException has occurred", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = {BadRequestException.class})
    public ResponseEntity<ErrorDTO> badRequestExceptionHandler(BadRequestException exception) {
        log.error("A bad request has happen", exception);
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.BAD_REQUEST.value());
        errorDTO.setStatus(HttpStatus.BAD_REQUEST);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<ErrorDTO>(errorDTO, HttpStatus.BAD_REQUEST);
    }
}
