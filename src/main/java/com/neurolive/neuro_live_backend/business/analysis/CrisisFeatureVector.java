package com.neurolive.neuro_live_backend.business.analysis;

public record CrisisFeatureVector(
        float bpmDelta,
        float spo2Drop,
        float errorRate,
        float dwellTime,
        float flightTime
) {

    public float[] toArray() {
        return new float[]{bpmDelta, spo2Drop, errorRate, dwellTime, flightTime};
    }
}
