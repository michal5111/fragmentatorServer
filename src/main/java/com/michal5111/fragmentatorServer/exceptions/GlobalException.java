package com.michal5111.fragmentatorServer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GlobalException extends ResponseStatusException {

    public GlobalException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public GlobalException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }
}
