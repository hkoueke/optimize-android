package com.izigo.optimized;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.blankj.utilcode.util.FragmentUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.SnackbarUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.izigo.optimized.fragments.MainFragment;
import com.izigo.optimized.models.PreferenceEvent;
import com.izigo.optimized.models.entities.Country;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.SPUtils;
import com.izigo.optimized.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mIsFirstStart;
    private boolean mKeepSynced;
    private static FirebaseDatabase mDatabase;
    private DatabaseReference mCountryRef;
    private static SPUtils mSp;

    @BindView(R.id.btnSync)
    MaterialButton mBtnSync;

    @BindView(R.id.layout_no_connectivity)
    View mNoConnectivityLayout;

    @BindView(R.id.progress)
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        if (mSp == null)
            mSp = SPUtils.getInstance(this);

        if (mDatabase == null)
            mDatabase = FirebaseDatabase.getInstance();

        mIsFirstStart = mSp.getBoolean(Constants.KEY_PREF_APP_FIRST_START, true);
        mCountryRef = mDatabase.getReference().child(Constants.KEY_FIREBASE_ROOT_NODE).child("cm");
        mKeepSynced = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

        if (mIsFirstStart) {
            mNoConnectivityLayout.setVisibility(View.VISIBLE);
            mBtnSync.setVisibility(View.VISIBLE);
            mBtnSync.setOnClickListener(btnSyncListener);

            if (!NetworkUtils.isConnected()) {
                mBtnSync.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_translate));
            }
        }
        final int freq = Integer.parseInt(mSp.getString(Constants.KEY_PREF_FREQUENCY, getString(R.string.list_update_freq_default)));
        if (mSp.getBoolean(Constants.KEY_PREF_AUTO_UPDATE, getResources().getBoolean(R.bool.auto_update_enabled)) && freq == -1)
            mKeepSynced = true;

        mCountryRef.keepSynced(mKeepSynced);
        if (NetworkUtils.isConnected()) {
            if (mIsFirstStart)
                startAnimations();
        }
        mCountryRef.addValueEventListener(valueEventListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);

        if (mCountryRef != null)
            mCountryRef.removeEventListener(valueEventListener);
    }

    private final OnClickListener btnSyncListener = v -> {
        if (NetworkUtils.isConnected()) {
            if (mIsFirstStart)
                startAnimations();

            mCountryRef.addValueEventListener(MainActivity.this.valueEventListener);
            return;
        }

        mBtnSync.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.error_network_disconnected_check_settings)
                .setPositiveButton(R.string.action_settings, (dialog, i) -> {
                    dialog.dismiss();
                    new Handler().postDelayed(NetworkUtils::openWirelessSettings, 500);
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mBtnSync.setVisibility(View.VISIBLE);
                    }
                }).show();
    };

    private final ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (mIsFirstStart) {
                mNoConnectivityLayout.setVisibility(View.GONE);
                clearAllAnimations();
            }

            final Country country = snapshot.getValue(Country.class);
            if (country == null) {
                LogUtils.eTag(TAG, "No object available for key ".concat(Objects.requireNonNull(mCountryRef.getKey())));
                return;
            }

            final long downloadedCount = country.getProviders().size();

            if (downloadedCount > mSp.getLong(Constants.KEY_PREF_ACTIVE_PROVIDERS_COUNT, 0)) {
                mSp.setValue(Constants.KEY_PREF_ACTIVE_PROVIDERS_COUNT, downloadedCount);
                final String notificationText = getString(R.string.notifcation_new_providers);
                SnackbarUtils.with(mNoConnectivityLayout).setMessage(notificationText).setDuration(SnackbarUtils.LENGTH_LONG).show();
            }

            if (mIsFirstStart)
                mSp.setValue(Constants.KEY_PREF_APP_FIRST_START, false);

            EventBus.getDefault().postSticky(country);
            if (NetworkUtils.isConnected() && mKeepSynced) {
                mSp.setValue(Constants.KEY_PREF_UPDATE_TIMESTAMP, System.currentTimeMillis());
            }

            final Fragment main = FragmentUtils.findFragment(getSupportFragmentManager(), MainFragment.class);
            if (main == null) {
                FragmentUtils.add(getSupportFragmentManager(), MainFragment.newInstance(country),
                        R.id.container, false, true);
                return;
            }
            FragmentUtils.show(main);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            LogUtils.eTag(TAG, "Operation failed with message ".concat(error.getMessage()));
            if (mIsFirstStart)
                clearAllAnimations();
        }
    };

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void eventReceiverPrefs(@NonNull PreferenceEvent pe) {
        LogUtils.dTag(TAG, "'PreferenceEvent' object received from 'PreferenceFragment'");
        final Preference pref = pe.getPreference();
        final Object value = pe.getNewValue();

        if (pref.getKey().equals(Constants.KEY_PREF_AUTO_UPDATE)) {
            final int updateFreq = Integer.parseInt(mSp.getString(Constants.KEY_PREF_FREQUENCY,
                    getString(R.string.list_update_freq_default)));
            mKeepSynced = (Boolean) value && updateFreq == -1;
        }
        if (pref.getKey().equals(Constants.KEY_PREF_FREQUENCY)) {
            final int newFreq = Integer.parseInt(String.valueOf(value));
            if (!mSp.getBoolean(Constants.KEY_PREF_AUTO_UPDATE, getResources().getBoolean(R.bool.auto_update_enabled)) || newFreq != -1) {
                mKeepSynced = false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (FragmentUtils.getAllFragments(getSupportFragmentManager()).isEmpty())
            finish();
    }

    private void clearAllAnimations() {
        mProgressBar.setVisibility(View.GONE);
        mProgressBar.clearAnimation();
        mBtnSync.setVisibility(View.VISIBLE);
    }

    private void startAnimations() {
        mBtnSync.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        ViewCompat.animate(mProgressBar);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, @NonNull Preference pref) {
        // Instantiate the new Fragment
        Fragment fragment = null;
        if (pref.getKey().equals("settings_wallets")) {
            final Bundle args = pref.getExtras();
            /*args.putInt(Constants.KEY_FRAGMENT_PARCEL_TOOLBAR_TITLE, R.string.preference_category_settings_wallets);*/

            fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                    getClassLoader(),
                    pref.getFragment());

            fragment.setArguments(args);
            fragment.setTargetFragment(caller, 0);
        }

        if (fragment == null) return false;
        // Replace the existing Fragment with the new Fragment
        Utils.openFragment(getSupportFragmentManager(), fragment);
        return true;
    }
}
