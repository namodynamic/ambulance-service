package com.ambulance.ambulance_service.exception;

public class NoAvailableAmbulanceException extends Exception {
    public NoAvailableAmbulanceException(String message) {
        super(message);
    }
}