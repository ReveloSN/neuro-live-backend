package com.neurolive.neuro_live_backend.data.enums;

// Enum que clasifica el tipo de intervencion aplicada en una crisis.
public enum TypeEnum {
    NO_INTERVENTION,
    CALM_MODE,
    LIGHT,
    AUDIO,
    UI,
    BREATHING,
    @Deprecated LIGHTING_CONTROL,
    @Deprecated AUDITORY_REGULATION,
    @Deprecated UI_REDUCTION,
    @Deprecated GUIDED_BREATHING;

    // Devuelve un unico tipo de intervencion para que comandos, eventos y reportes hablen el mismo lenguaje.
    public TypeEnum canonical() {
        return switch (this) {
            case LIGHT, LIGHTING_CONTROL -> LIGHT;
            case AUDIO, AUDITORY_REGULATION -> AUDIO;
            case UI, UI_REDUCTION -> UI;
            case BREATHING, GUIDED_BREATHING -> BREATHING;
            case NO_INTERVENTION -> NO_INTERVENTION;
            case CALM_MODE -> CALM_MODE;
        };
    }

    public boolean sameFamily(TypeEnum other) {
        return other != null && canonical() == other.canonical();
    }

    public boolean isLight() {
        return canonical() == LIGHT;
    }

    public boolean isAudio() {
        return canonical() == AUDIO;
    }

    public boolean isUi() {
        return canonical() == UI;
    }

    public boolean isBreathing() {
        return canonical() == BREATHING;
    }
}
