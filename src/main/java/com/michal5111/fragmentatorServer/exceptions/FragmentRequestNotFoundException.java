package com.michal5111.fragmentatorServer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FragmentRequestNotFoundException extends Exception {
    public FragmentRequestNotFoundException(String message) {
        super(message);
    }
}
