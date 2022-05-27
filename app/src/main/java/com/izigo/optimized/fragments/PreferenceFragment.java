package com.izigo.optimized.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.FragmentUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.izigo.optimized.BuildConfig;
import com.izigo.optimized.R;
import com.izigo.optimized.listeners.BaseErrorListener;
import com.izigo.optimized.models.PreferenceEvent;
import com.izigo.optimized.models.entities.Country;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.SPUtils;
import com.izigo.optimized.utils.Utils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class PreferenceFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener,
        OnPreferenceChangeListener {

    private static final String TAG = PreferenceFragment.class.getSimpleName();
    private static SPUtils mSp;
    private List<Provider> mProviders;
    private boolean mInternetAvailable = false;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @NonNull
    public static PreferenceFragment newInstance(@NonNull List<Provider> providers) {
        PreferenceFragment fragment = new PreferenceFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS,
                (ArrayList<? extends Parcelable>) providers);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (mSp == null)
            mSp = SPUtils.getInstance(requireContext());

        final Bundle args = requireArguments();
        mProviders = args.getParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS);

        if (mProviders == null)
            throw new NullPointerException(TAG.concat(": Provide a non-null ArrayList<Provider>"));

        new Handler(Looper.getMainLooper()).post(this::initPreferences);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mToolbar.setTitle(R.string.action_settings);
        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mToolbar);
        Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(requireContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentUtils.remove(this);
        }
        FragmentUtils.pop(getParentFragmentManager(), true);
        return super.onOptionsItemSelected(item);
    }

    private final OnPreferenceClickListener aboutClickListener = preference -> {
        Intent intent = null;

        if (preference.getKey().equals(Constants.KEY_PREF_ABOUT_SHARE)) {
            final String sharedText = TextUtils.concat(getString(R.string.preference_category_about_share_desc), " ",
                    getString(R.string.app_playstore_link)).toString();

            intent = IntentUtils.getShareTextIntent(sharedText);
        } else if (preference.getKey().equals(Constants.KEY_PREF_ABOUT_MAILTO_DEVS)) {
            intent = IntentUtils.getShareTextIntent(null);
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"hervekoueke-tnsrms@yahoo.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.preference_category_about_mail_subject));
        }

        if (intent != null)
            startActivity(Intent.createChooser(intent, null));

        return true;
    };

    private final OnPreferenceClickListener manualUpdClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            NetworkUtils.isAvailableAsync(aBoolean -> mInternetAvailable = aBoolean);

            if (!NetworkUtils.isConnected()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.preference_category_settings_pref_manual_update_title)
                        .setMessage(R.string.error_network_disconnected_check_settings)
                        .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                            dialog.dismiss();
                            new Handler(Looper.getMainLooper()).post(NetworkUtils::openWirelessSettings);
                        }).show();
                return false;
            } else if (!mInternetAvailable) {
                SnackbarUtils
                        .with(mToolbar)
                        .setMessage(getString(R.string.error_network_unavailable))
                        .setDuration(SnackbarUtils.LENGTH_LONG).show();
                return false;
            } else {
                SnackbarUtils
                        .with(mToolbar)
                        .setDuration(SnackbarUtils.LENGTH_LONG)
                        .setMessage(getString(R.string.preference_category_settings_pref_manual_update_updating))
                        .show();

                FirebaseDatabase.getInstance().getReference().child(Constants.KEY_FIREBASE_ROOT_NODE)
                        .child(mSp.getString(Constants.KEY_PREF_COUNTRY_ISO, ""))
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                PreferenceFragment.mSp.setValue(Constants.KEY_PREF_UPDATE_TIMESTAMP, System.currentTimeMillis());
                                preference.setSummary(Utils.getFormattedDateTime(requireContext(), System.currentTimeMillis()));
                                final Country country = snapshot.getValue(Country.class);
                                if (country != null) {
                                    EventBus.getDefault().postSticky(country);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                preference.setSummary(R.string.preference_category_settings_pref_manual_update_failed);
                                ToastUtils.showShort(R.string.preference_category_settings_pref_manual_update_failed);
                            }
                        });
                return true;
            }
        }
    };

    private void initPreferences() {
        final SwitchPreferenceCompat autoUpdate = findPreference(Constants.KEY_PREF_AUTO_UPDATE);
        final MultiSelectListPreference providers = findPreference(Constants.KEY_PREF_PROVIDERS);
        final Preference manualUpdate = findPreference(Constants.KEY_PREF_MANUAL_UPDATE);
        final Preference version = findPreference(Constants.KEY_PREF_ABOUT_VERSION);
        final SwitchPreferenceCompat dialogBehavior = findPreference(Constants.KEY_PREF_DIAL_BEHAVIOR);

        providers.setEnabled(false);
        manualUpdate.setEnabled(!autoUpdate.isChecked());
        manualUpdate.setOnPreferenceClickListener(manualUpdClickListener);
        autoUpdate.setOnPreferenceChangeListener(this);
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            mSp.setValue(Constants.KEY_PREF_DIAL_BEHAVIOR, false);
        }
        dialogBehavior.setOnPreferenceChangeListener(this);

        final long datetime = mSp.getLong(Constants.KEY_PREF_UPDATE_TIMESTAMP, System.currentTimeMillis());
        manualUpdate.setSummary(Utils.getFormattedDateTime(requireContext(), datetime));

        findPreference(Constants.KEY_PREF_ABOUT_SHARE).setOnPreferenceClickListener(aboutClickListener);
        findPreference(Constants.KEY_PREF_ABOUT_MAILTO_DEVS).setOnPreferenceClickListener(aboutClickListener);

        final int size = mProviders.size();
        final String[] entries = new String[size];
        final String[] entryValues = new String[size];

        for (int i = 0; i < size; i++) {
            entries[i] = mProviders.get(i).getName();
            entryValues[i] = mProviders.get(i).getId();
        }

        if (entries.length > 0) {
            providers.setEntries(entries);
            providers.setEntryValues(entryValues);
            final Set<String> valuesInPrefs = mSp.getStringSet(Constants.KEY_PREF_PROVIDERS, Collections.emptySet());
            final List<Provider> innerJoin = StreamSupport
                    .stream(mProviders)
                    .filter(p -> valuesInPrefs.contains(p.getId()))
                    .collect(Collectors.toUnmodifiableList());

            if (valuesInPrefs.isEmpty())
                providers.setSummary(getString(R.string.preference_category_settings_pref_providers_none_selected));
            else
                providers.setSummary(Utils.getProvidersSummary(requireContext(), innerJoin));

            providers.setOnPreferenceChangeListener(this);
            providers.setEnabled(true);
        }
        version.setSummary(BuildConfig.VERSION_NAME);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @NonNull String key) {
        if (key.equals(Constants.KEY_PREF_AUTO_UPDATE)) {
            findPreference(Constants.KEY_PREF_MANUAL_UPDATE)
                    .setEnabled(!sharedPreferences.getBoolean(Constants.KEY_PREF_AUTO_UPDATE, true));
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference.getKey().equals(Constants.KEY_PREF_PROVIDERS)) {
            Set<String> selected = (Set) newValue;
            if (selected == null) return false;

            if (selected.isEmpty()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.error_dialog_select_at_least_one_provider)
                        .setPositiveButton(android.R.string.ok, null).show();
                return false;
            }
            preference.setSummary(Utils.getProvidersSummary(requireContext(),
                    StreamSupport.stream(mProviders).filter(p -> selected.contains(p.getId()))
                            .collect(Collectors.toUnmodifiableList())
                    )
            );
        } else if (preference.getKey().equals(Constants.KEY_PREF_DIAL_BEHAVIOR)) {
            final boolean activated = (boolean) newValue;
            if (activated) {
                //If permission is not yet granted, ask
                Dexter.withContext(requireContext()).withPermission(Manifest.permission.CALL_PHONE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                ((SwitchPreferenceCompat) preference).setChecked(false);
                                if (response.isPermanentlyDenied()) {
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle(R.string.app_name)
                                            .setMessage(R.string.rationale_grant_call_phone)
                                            .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss())
                                            .setPositiveButton(R.string.action_settings, (dialog, i) -> {
                                                dialog.dismiss();
                                                AppUtils.launchAppDetailsSettings();
                                            }).show();
                                }
                                LogUtils.eTag(TAG, response);
                            }

                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {
                                ((SwitchPreferenceCompat) preference).setChecked(true);
                                LogUtils.iTag(TAG, response);
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest,
                                                                           PermissionToken permissionToken) {
                                new AlertDialog.Builder(requireContext())
                                        .setMessage(R.string.rationale_grant_call_phone)
                                        .setNegativeButton(android.R.string.cancel, (dialog, i) -> {
                                            dialog.dismiss();
                                            permissionToken.cancelPermissionRequest();
                                        })
                                        .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                                            dialog.dismiss();
                                            permissionToken.continuePermissionRequest();
                                        })
                                        .setOnDismissListener(dialog -> permissionToken.cancelPermissionRequest())
                                        .show();
                            }
                        })
                        .withErrorListener(new BaseErrorListener())
                        .check();
            }
        }

        EventBus.getDefault().post(new PreferenceEvent(preference, newValue));
        LogUtils.dTag(TAG, "Preference object posted");
        return true;
    }
}
