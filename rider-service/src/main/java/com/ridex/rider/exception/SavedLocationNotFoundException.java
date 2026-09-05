package com.ridex.rider.exception;


public class SavedLocationNotFoundException extends RuntimeException {

    public SavedLocationNotFoundException(String message) {
        super(message);
    }
}