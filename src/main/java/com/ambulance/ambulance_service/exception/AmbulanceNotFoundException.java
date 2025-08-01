package com.ambulance.ambulance_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an ambulance with a given ID is not found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class AmbulanceNotFoundException extends RuntimeException {

    public AmbulanceNotFoundException() {
        super();
    }

    public AmbulanceNotFoundException(String message) {
        super(message);
    }

    public AmbulanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbulanceNotFoundException(Throwable cause) {
        super(cause);
    }
}
