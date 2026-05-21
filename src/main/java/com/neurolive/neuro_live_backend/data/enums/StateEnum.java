package com.neurolive.neuro_live_backend.data.enums;

// Enum que describe el estado en el ciclo de una crisis.
public enum StateEnum {
    NORMAL,
    RISK_ELEVATED,
    ACTIVE_CRISIS;

    // Orden numerico para comparar severidades sin duplicar switch en cada clase.
    public int severity() {
        return switch (this) {
            case NORMAL -> 0;
            case RISK_ELEVATED -> 1;
            case ACTIVE_CRISIS -> 2;
        };
    }
}
