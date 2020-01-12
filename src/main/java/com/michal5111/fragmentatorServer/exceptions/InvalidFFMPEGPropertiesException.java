package com.michal5111.fragmentatorServer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFFMPEGPropertiesException extends Exception {
    public InvalidFFMPEGPropertiesException(String message) {
        super(message);
    }
}
