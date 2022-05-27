package com.izigo.optimized;

import android.app.Application;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.izigo.optimized.models.migrations.OptimizeMigrations;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.SPUtils;

import java.io.FileNotFoundException;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class App extends Application {
    private static SPUtils mSp;

    public static int PHONE_NUMBER_LENGTH;
    public static String NUMBER_FORMAT;
    public static String APP_CURRENCY;

    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Realm.init(this);
        final RealmConfiguration config = new RealmConfiguration.Builder().schemaVersion(0) // Must be bumped when the schema changes
                .name(getString(R.string.app_name)) // Name of my database
                .migration(new OptimizeMigrations()) // Migration to run instead of throwing an exception
                .build();

        try {
            Realm.migrateRealm(config, new OptimizeMigrations());
        } catch (FileNotFoundException ignored) {
            // If the Realm file doesn't exist, just ignore.
        }

        if (mSp == null)
            mSp = SPUtils.getInstance(this);

        //final String defaultTheme = getString(R.string.list_theme_default);

        final boolean shownOnboarding = mSp.getBoolean(Constants.KEY_PREF_APP_SHOWN_ONBOARDING, false);
        if (!shownOnboarding && !mSp.getBoolean(Constants.KEY_PREF_APP_FIRST_START, true))
            mSp.setValue(Constants.KEY_PREF_APP_SHOWN_ONBOARDING, true); // Don't show onboarding if it is not first start

        /*final String theme = mSp.getString(Constants.KEY_PREF_THEME, defaultTheme);
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(theme));*/

        final FirebaseRemoteConfig remoteConfig = getRemoteConfig();
        final Task<Boolean> fetchTask = remoteConfig.fetchAndActivate();

        PHONE_NUMBER_LENGTH = Integer.parseInt(fetchTask.isSuccessful() || fetchTask.isComplete()
                ? remoteConfig.getString(Constants.KEY_PREF_APP_PHONE_NUMBER_lENGTH)
                : "9");

        NUMBER_FORMAT = fetchTask.isSuccessful() || fetchTask.isComplete()
                ? remoteConfig.getString(Constants.KEY_PREF_APP_NUMBER_FORMAT)
                : "%.0f";

        APP_CURRENCY = fetchTask.isSuccessful() || fetchTask.isComplete()
                ? remoteConfig.getString(Constants.KEY_PREF_APP_CURRENCY)
                : "XAF";
    }

    @NonNull
    @CheckResult
    private FirebaseRemoteConfig getRemoteConfig() {
        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build());
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        remoteConfig.ensureInitialized();
        return remoteConfig;
    }
}