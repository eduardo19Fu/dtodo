package xyz.pangosoft.dtodo.controller.errorhandler;

import xyz.pangosoft.dtodo.error.ErrorDTO;
import xyz.pangosoft.dtodo.error.exceptions.DataAccessException;
import xyz.pangosoft.dtodo.error.exceptions.BadRequestException;
import xyz.pangosoft.dtodo.error.exceptions.DuplicateCorrelativoException;
import xyz.pangosoft.dtodo.error.exceptions.DuplicateNotaCreditoException;
import xyz.pangosoft.dtodo.error.exceptions.InvalidPasswordException;
import xyz.pangosoft.dtodo.error.exceptions.MethodArgumentTypeMismatchException;
import xyz.pangosoft.dtodo.error.exceptions.NoContentException;
import xyz.pangosoft.dtodo.error.exceptions.NotFoundException;
import xyz.pangosoft.dtodo.error.exceptions.NumberFormatException;
import xyz.pangosoft.dtodo.error.exceptions.ParseException;
import xyz.pangosoft.dtodo.error.exceptions.ReportGenerationException;
import xyz.pangosoft.dtodo.error.exceptions.SQLException;

import xyz.pangosoft.dtodo.error.exceptions.SigningDocumentFelException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class ExceptionControllerHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Los datos enviados no son válidos.");
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(message);
        errorDTO.setCode(HttpStatus.BAD_REQUEST.value());
        errorDTO.setStatus(HttpStatus.BAD_REQUEST);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<>(errorDTO, HttpStatus.BAD_REQUEST);
    }

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

    @ExceptionHandler(value = {BadRequestException.class})
    public ResponseEntity<ErrorDTO> badRequestExceptionHandler(BadRequestException exception) {
        log.warn("Petición rechazada por datos inválidos: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.BAD_REQUEST.value());
        errorDTO.setStatus(HttpStatus.BAD_REQUEST);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<>(errorDTO, HttpStatus.BAD_REQUEST);
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

    @ExceptionHandler(value = {DuplicateCorrelativoException.class})
    public ResponseEntity<ErrorDTO> duplicateCorrelativoExceptionHandler(DuplicateCorrelativoException exception) {
        log.error("Intento de crear un correlativo duplicado: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCause(exception.getCause());
        errorDTO.setCode(HttpStatus.CONFLICT.value());
        errorDTO.setStatus(HttpStatus.CONFLICT);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<>(errorDTO, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = {InvalidPasswordException.class})
    public ResponseEntity<ErrorDTO> invalidPasswordExceptionHandler(InvalidPasswordException exception) {
        log.warn("Autorización rechazada por contraseña incorrecta: {}", exception.getMessage());
        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setMessage(exception.getMessage());
        errorDTO.setCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
        errorDTO.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        errorDTO.setInstant(Instant.now());
        return new ResponseEntity<>(errorDTO, HttpStatus.UNPROCESSABLE_ENTITY);
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
