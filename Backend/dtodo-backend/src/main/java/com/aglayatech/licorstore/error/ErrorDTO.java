package com.aglayatech.licorstore.error;
import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDTO {
    private HttpStatus status;
    private int code;
    private String message;
    private Throwable cause;
    private Instant instant;
    private String path;
}

