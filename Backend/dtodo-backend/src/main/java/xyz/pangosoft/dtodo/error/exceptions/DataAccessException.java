package xyz.pangosoft.dtodo.error.exceptions;

public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message);
    }
}
