package com.michal5111.fragmentator_server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LineNotFoundException extends Exception {
    public LineNotFoundException(String message) {
        super(message);
    }
}
