package com.michal5111.fragmentator_server.exceptions;

public class YouTubeDlPropertiesException extends Throwable {
    public YouTubeDlPropertiesException() {
    }

    public YouTubeDlPropertiesException(String message) {
        super(message);
    }

    public YouTubeDlPropertiesException(String message, Throwable cause) {
        super(message, cause);
    }

    public YouTubeDlPropertiesException(Throwable cause) {
        super(cause);
    }

    public YouTubeDlPropertiesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
