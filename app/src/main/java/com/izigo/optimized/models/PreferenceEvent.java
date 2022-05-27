package com.izigo.optimized.models;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

public class PreferenceEvent {
    private final Object newValue;
    private final Preference preference;

    public PreferenceEvent(@NonNull final Preference preference, @NonNull Object newValue) {
        this.preference = preference;
        this.newValue = newValue;
    }

    public Preference getPreference() {
        return preference;
    }

    public Object getNewValue() {
        return newValue;
    }
}
