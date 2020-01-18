package com.michal5111.fragmentator_server.exceptions;

public class YouTubeDlException extends Exception {
    public YouTubeDlException() {
    }

    public YouTubeDlException(String message) {
        super(message);
    }

    public YouTubeDlException(String message, Throwable cause) {
        super(message, cause);
    }

    public YouTubeDlException(Throwable cause) {
        super(cause);
    }

    public YouTubeDlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
