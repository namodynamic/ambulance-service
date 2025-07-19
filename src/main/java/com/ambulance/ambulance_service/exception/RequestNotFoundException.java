package com.ambulance.ambulance_service.exception;

public class RequestNotFoundException extends Exception {
    public RequestNotFoundException(String message) {
        super(message);
    }
}