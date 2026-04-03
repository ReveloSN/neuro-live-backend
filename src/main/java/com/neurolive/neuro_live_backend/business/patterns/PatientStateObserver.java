package com.neurolive.neuro_live_backend.business.patterns;

public interface PatientStateObserver {

    void onPatientStateChanged(PatientStateUpdate update);
}
