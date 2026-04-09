package com.neurolive.neuro_live_backend.data.exception;

public class CrisisNotFoundException extends RuntimeException {

    public CrisisNotFoundException(Long crisisId) {
        super("Crisis event not found with id " + crisisId);
    }
}
