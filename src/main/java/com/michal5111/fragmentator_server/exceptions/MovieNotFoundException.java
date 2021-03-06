package com.michal5111.fragmentator_server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class MovieNotFoundException extends Exception {
    public MovieNotFoundException(String message) {
        super(message);
    }
}
