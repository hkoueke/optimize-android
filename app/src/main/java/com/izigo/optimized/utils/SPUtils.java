package com.izigo.optimized.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Set;

public class SPUtils {
    private static SPUtils sInstance;
    private final Editor editor;
    private final SharedPreferences pref;

    private SPUtils(@NonNull Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = pref.edit();
    }

    public static synchronized SPUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SPUtils(context.getApplicationContext());
        }
        return sInstance;
    }

    public void setValue(@NonNull String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public void setValue(@NonNull String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public void setValue(@NonNull String key, double value) {
        setValue(key, Double.toString(value));
    }

    public void setValue(@NonNull String key, long value) {
        editor.putLong(key, value);
        editor.apply();
    }

    public void setValue(@NonNull String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void setValue(@NonNull String key, Set<String> value) {
        editor.putStringSet(key, value);
        editor.apply();
    }

    public String getString(@NonNull String key, String defaultValue) {
        return pref.getString(key, defaultValue);
    }

    public int getInt(@NonNull String key, int defaultValue) {
        return pref.getInt(key, defaultValue);
    }

    public long getLong(@NonNull String key, long defaultValue) {
        return pref.getLong(key, defaultValue);
    }

    public boolean getBoolean(@NonNull String keyFlag, boolean defaultValue) {
        return pref.getBoolean(keyFlag, defaultValue);
    }

    public Set<String> getStringSet(@NonNull String keyFlag, @Nullable Set<String> defaultValue) {
        return pref.getStringSet(keyFlag, defaultValue);
    }
}
