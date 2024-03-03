package com.aglayatech.licorstore.error.exceptions;

public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message);
    }
}
