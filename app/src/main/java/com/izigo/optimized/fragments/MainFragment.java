package com.izigo.optimized.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.LayoutMode;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.ModalDialog;
import com.afollestad.materialdialogs.bottomsheets.BottomSheet;
import com.afollestad.materialdialogs.list.DialogListExtKt;
import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.PhoneUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ThreadUtils.SimpleTask;
import com.blankj.utilcode.util.VibrateUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.izigo.optimized.App;
import com.izigo.optimized.R;
import com.izigo.optimized.dialogs.TransferDialogFragment;
import com.izigo.optimized.listeners.BaseErrorListener;
import com.izigo.optimized.models.Transaction;
import com.izigo.optimized.models.entities.Country;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.models.viewmodels.ResultItem;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.SPUtils;
import com.izigo.optimized.utils.Utils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnLongClickListener;
import com.mikepenz.itemanimators.SlideInOutLeftAnimator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java8.lang.Doubles;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class MainFragment extends Fragment {
    private static final String TAG = MainFragment.class.getSimpleName();
    private static final int MAX_APP_LAUNCH_COUNT = 10;
    private static SPUtils mSp;
    private Country mCountry;

    private ItemAdapter mItemAdapter;
    private FastAdapter<ResultItem> mFastAdapter;

    private double mMax;
    private double mMin;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @BindView(R.id.layout_optimize_bar)
    View mSbLayout;

    @BindView(R.id.til_amount)
    TextInputLayout mTilAmount;

    @BindView(R.id.button_optimize)
    AppCompatButton mBtnOptimize;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.no_content_hint)
    View mEmptyView;

    @NonNull
    public static MainFragment newInstance(@NonNull Country country) {
        final MainFragment fragment = new MainFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.KEY_FRAGMENT_PARCEL_COUNTRY, country);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        final Bundle args = requireArguments();
        mCountry = args.getParcelable(Constants.KEY_FRAGMENT_PARCEL_COUNTRY);

        if (mCountry == null)
            throw new NullPointerException(TAG.concat(": Provide a non-null 'Country' object"));

        if (mSp == null)
            mSp = SPUtils.getInstance(requireContext());

        mItemAdapter = new ItemAdapter<>();
        mFastAdapter = FastAdapter.with(mItemAdapter);
        mFastAdapter.withSelectOnLongClick(true);

        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        })
                .launch(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE});
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, root);
        initViews();
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mToolbar.setTitle(R.string.app_name);
        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
        Objects.requireNonNull(activity).setSupportActionBar(mToolbar);
        Objects.requireNonNull(activity.getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        tryShowRating();
    }

    @Override
    public void onResume() {
        super.onResume();
        mEmptyView.setVisibility(mItemAdapter.getAdapterItemCount() > 0 ? View.GONE : View.VISIBLE);
        if (mTilAmount.getEditText() != null) {
            mTilAmount.getEditText().setOnEditorActionListener((textView, actionId, keyEvent)
                    -> actionId == EditorInfo.IME_ACTION_DONE && mBtnOptimize.performClick());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        KeyboardUtils.hideSoftInput(requireActivity());

        final int id = item.getItemId();
        Fragment fragment = null;

        switch (id) {
            case R.id.action_settings:
                fragment = PreferenceFragment.newInstance(mCountry.getProviders());
                break;
            case R.id.action_prices:
                fragment = PricingFragment.newInstance((ArrayList<Provider>) mCountry.getProviders());
                break;
            case R.id.action_send_money:
                if (meetsPrerequisites(null)) {
                    final TransferDialogFragment bottomSheetFragment =
                            TransferDialogFragment.newInstance((ArrayList<Provider>) mCountry.getProviders(), null, 0);
                    bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
                }
                break;
            case R.id.action_show_balance:
                if (meetsPrerequisites(null) && isPermissionGranted(Manifest.permission.CALL_PHONE))
                    prepareForDialOrCall();
                else
                    requestCallPhonePermission(item);
                break;
        }
        if (fragment != null)
            Utils.openFragment(getParentFragmentManager(), fragment);

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.button_optimize)
    public void onButtonOptimizeClick() {
        KeyboardUtils.hideSoftInput(requireActivity());
        final Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.wiggle_anim);

        if (mTilAmount.getEditText() == null) return;

        if (mTilAmount.getEditText().getText().toString().isEmpty()) {
            mSbLayout.startAnimation(anim);
            mTilAmount.setError(getString(R.string.search_bar_input_text_error_empty_field));
            return;
        }

        if (StringUtils.isEmpty(mTilAmount.getEditText().getText())) {
            mSbLayout.startAnimation(anim);
            mTilAmount.setError(getString(R.string.search_bar_input_text_error_empty_field));
            return;
        }

        final double amount = Double.parseDouble(mTilAmount.getEditText().getText().toString());

        if (!Utils.isBetweenInclusive(amount, mMin, mMax)) {
            mSbLayout.startAnimation(anim);
            mTilAmount.setError(getString(R.string.search_bar_input_text_error_empty_field));
            return;
        }

        mTilAmount.setHelperText(Utils.getHelperText(requireContext(), mMin, mMax));
        final Set<String> providersSet = mSp.getStringSet(Constants.KEY_PREF_PROVIDERS, Collections.emptySet());

        if (providersSet.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.pref_category_providers_warning_empty_selection_short)
                    .setPositiveButton(R.string.action_settings, (dialog, i) -> {
                        dialog.dismiss();
                        final PreferenceFragment settings = PreferenceFragment.newInstance(mCountry.getProviders());
                        Utils.openFragment(getParentFragmentManager(), settings);
                    }).show();
            return;
        }

        new SimpleTask<List<ResultItem>>() {
            public List<ResultItem> doInBackground() {
                return computeOptimizations(amount, providersSet);
            }

            public void onSuccess(List<ResultItem> results) {
                mEmptyView.setVisibility(results.size() > 0 ? View.GONE : View.VISIBLE);
                mItemAdapter.clear();
                mItemAdapter.add(results);
            }

            public void onFail(Throwable t) {
                super.onFail(t);
                LogUtils.eTag(TAG, t);
            }
        }.run();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void updateCountryObject(Country country) {
        mCountry = country;
        EventBus.getDefault().removeAllStickyEvents();
        LogUtils.dTag(TAG, "New 'Country' object received: previous object updated.");
    }

    @NonNull
    private List<ResultItem> computeOptimizations(double amount, Set<String> set) {
        final List<Provider> workList = StreamSupport.stream(mCountry.getProviders())
                .filter(p -> set.contains(p.getId())).collect(Collectors.toList());

        final List<ResultItem> results = new ArrayList<>(workList.size());
        final int level = Integer.parseInt(mSp.getString(Constants.KEY_PREF_TRX_THRESHOLD, getString(R.string.list_transactions_threshold_default)));
        final int mode = Integer.parseInt(mSp.getString(Constants.KEY_PREF_SEARCH_MODE, getString(R.string.list_transactions_search_mode_default)));

        for (Provider provider : workList) {
            final ResultItem r = provider.getOptimizedTransactions(amount, level, mode,
                    Constants.VALUE_OPERATION_TYPE_WITHDRAWAL);
            if (r != null)
                results.add(r);
        }

        if (results.size() > 1) {
            if (SPUtils.getInstance(requireContext()).getString(Constants.KEY_PREF_FILTER_BY, getString(R.string.list_transactions_filter_default)).equals("1"))
                Collections.sort(results, Utils.trxSavingsOrderByDesc);
            else
                Collections.sort(results, trxFeesOrderByAsc);
        }

        return results;
    }

    private void initViews() {
        mEmptyView.setVisibility(View.VISIBLE);
        final double[] pricingListValues = Utils.getPricingListValues(mCountry.getProviders(), Constants.VALUE_OPERATION_TYPE_WITHDRAWAL);
        final Pair<Double, Double> minMax = Utils.computeMinAndMaxValues(pricingListValues);
        mMin = minMax.getValue0();
        mMax = minMax.getValue1();

        if (mTilAmount.getEditText() != null) {
            final int maxLength = String.valueOf((int) mMax).length();
            mTilAmount.getEditText().setFilters(new InputFilter[]{new LengthFilter(maxLength)});
            if (!App.NUMBER_FORMAT.contains("0"))
                mTilAmount.getEditText().setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }

        mTilAmount.setHelperText(Utils.getHelperText(requireContext(), mMin, mMax));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setItemAnimator(new SlideInOutLeftAnimator(mRecyclerView));
        mRecyclerView.getItemAnimator().setAddDuration(250);
        mRecyclerView.getItemAnimator().setRemoveDuration(250);

        //Assign adapter to recyclerview
        mRecyclerView.setAdapter(mFastAdapter);
        mFastAdapter.withOnPreLongClickListener(onPreLongClickListener);
    }

    final OnLongClickListener<ResultItem> onPreLongClickListener = (v, adapter, item, position) -> {
        VibrateUtils.vibrate(100);
        DialogListExtKt.listItems(new MaterialDialog(requireContext(), new BottomSheet(LayoutMode.WRAP_CONTENT)),
                R.array.list_result_item_actions, null, null, false, (dialog, integer, charSequence) -> {
                    switch (integer) {
                        //TODO
                        case 0:
                            if (meetsPrerequisites(item.getProvider())) {
                                final double amount = StreamSupport.stream(item.getTransactions())
                                        .map(Transaction::getTrxAmount).reduce(0d, Doubles::sum);

                                final TransferDialogFragment bottomSheetFragment =
                                        TransferDialogFragment.newInstance((ArrayList<Provider>) mCountry.getProviders(),
                                                item.getProvider().getMnc(), amount);

                                bottomSheetFragment.show(getParentFragmentManager(), bottomSheetFragment.getTag());
                            }
                            break;
                        case 1:
                            final String message = BuildSharedString(item.getProvider().getName(), item.getTransactions(), item.getSavings());
                            final Intent intent = IntentUtils.getShareTextIntent(message);
                            if (intent != null)
                                startActivity(Intent.createChooser(intent, null));
                            break;
                    }
                    return null;
                }).show();
        return item.getProvider().getMnc() != null;
    };

    private final Comparator<ResultItem> trxFeesOrderByAsc = (r1, r2) -> {
        final Double f1 = r1.getOptimizedFee();
        final Double f2 = r2.getOptimizedFee();
        return f1.compareTo(f2);
    };

    @NonNull
    @CheckResult
    private String BuildSharedString(@NonNull final String name, @NonNull final List<Transaction> transactions, final double savings) {
        final StringBuilder builder = new StringBuilder();
        final String message = getString(R.string.result_menu_share_string);
        final String trxCount = String.valueOf(transactions.size());
        final double amount = StreamSupport.stream(transactions).map(Transaction::getTrxAmount).reduce(0d, Doubles::sum);

        for (Iterator<Transaction> iterator = transactions.iterator(); iterator.hasNext(); ) {
            final Transaction tr = iterator.next();
            builder.append(String.format(App.NUMBER_FORMAT, tr.getTrxAmount()));
            if (iterator.hasNext())
                builder.append(", ");
        }
        return String.format(message, String.format(App.NUMBER_FORMAT, amount), App.APP_CURRENCY,
                trxCount, builder.toString(), String.format(App.NUMBER_FORMAT, savings), name);
    }

    @CheckResult
    public boolean meetsPrerequisites(final Provider provider) {
        if (PhoneUtils.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.rationale_gsm_device_required)
                    .setPositiveButton(android.R.string.ok, null).show();
            return false;
        }

        if (!Utils.hasActiveSim(requireContext())) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.rationale_sim_required_title)
                    .setMessage(R.string.rationale_sim_required)
                    .setPositiveButton(android.R.string.ok, null).show();
            return false;
        }

        if (provider != null && (provider.getMnc() == null || !Utils.isMncInDevice(requireContext(), provider.getMnc()))) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.rationale_sim_required_title)
                    .setMessage(String.format(getString(R.string.rationale_mnc_for_sim_not_found), provider.getId()))
                    .setPositiveButton(android.R.string.ok, null).show();
            return false;
        }

        return true;
    }

    private void requestCallPhonePermission(MenuItem item) {
        Dexter.withContext(requireContext()).withPermission(Manifest.permission.CALL_PHONE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        LogUtils.iTag(TAG, this, "-> Permission granted");
                        onOptionsItemSelected(item);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        LogUtils.eTag(TAG, this, "-> Permission denied");
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
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        showPermissionRationale(permissionToken);
                    }
                })
                .withErrorListener(new BaseErrorListener())
                .check();
    }

    private void prepareForDialOrCall() {
        /* We want to check user wallet balance
         * if we find more than one active SIM, we show a SingleSelect Dialog to the user
         * else we dial directly the corresponding number for the sole operator found
         * */
        final List<SubscriptionInfo> subscriptions = Utils.getActiveSubscriptions(requireContext());
        if (subscriptions == null) return;

        final List<String> items = new ArrayList<>(subscriptions.size());
        for (SubscriptionInfo s : subscriptions) items.add(s.getDisplayName().toString());
        final AtomicInteger index = new AtomicInteger();

        if (subscriptions.size() > 1) {
            DialogListExtKt
                    .listItems(new MaterialDialog(requireContext(), ModalDialog.INSTANCE), null, items, null,
                            false, (dialog, integer, charSequence) -> {
                                dialog.dismiss();
                                index.set(integer);
                                dialOrCall(subscriptions, index.get());
                                return null;
                            })
                    .negativeButton(android.R.string.cancel, null, null)
                    .show();
            return;
        }
        dialOrCall(subscriptions, index.get());
    }

    private void dialOrCall(@NonNull final List<SubscriptionInfo> subscriptions, final int index) {
        final boolean shouldCall = mSp.getBoolean(Constants.KEY_PREF_DIAL_BEHAVIOR, false);
        final Provider provider = StreamSupport.stream(mCountry.getProviders())
                .filter(op -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? op.getMnc() != null && op.getMnc().equals(subscriptions.get(index).getMncString())
                        : op.getMnc() != null && op.getMnc().equals(String.valueOf(subscriptions.get(index).getMnc()))
                ).findFirst().orElseThrow();

        final String ussd = Utils.getBalanceUri(provider);

        if (shouldCall)
            Utils.callPhone(requireContext(), ussd, subscriptions.get(index).getSimSlotIndex());
        else PhoneUtils.dial(ussd);
    }

    private void showPermissionRationale(PermissionToken token) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.rationale_grant_call_phone)
                .setNegativeButton(android.R.string.cancel, (dialog, i) -> {
                    dialog.dismiss();
                    token.cancelPermissionRequest();
                })
                .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                    dialog.dismiss();
                    token.continuePermissionRequest();
                })
                .setOnDismissListener(dialog -> token.cancelPermissionRequest())
                .show();
    }

    private void tryShowRating() {
        final AtomicInteger launchCount = new AtomicInteger(mSp.getInt(Constants.KEY_PREF_LAUNCH_COUNT, 0));
        final boolean showReview = launchCount.get() == MAX_APP_LAUNCH_COUNT;
        if (showReview) {
            final ReviewManager manager = ReviewManagerFactory.create(requireContext());
            final Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    final ReviewInfo reviewInfo = task.getResult();
                    final Task<Void> flow = manager.launchReviewFlow(requireActivity(), reviewInfo);
                    flow.addOnCompleteListener(t ->
                            mSp.setValue(Constants.KEY_PREF_LAUNCH_COUNT, launchCount.getAndIncrement()));
                }
            });
            return;
        }
        mSp.setValue(Constants.KEY_PREF_LAUNCH_COUNT, launchCount.getAndIncrement());
    }

    private boolean isPermissionGranted(String permission) {
        return ActivityCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}