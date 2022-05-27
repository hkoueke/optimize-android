package com.izigo.optimized.dialogs;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.CheckResult;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.ModalDialog;
import com.afollestad.materialdialogs.checkbox.DialogCheckboxExtKt;
import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ArrayUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.PhoneUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.izigo.optimized.App;
import com.izigo.optimized.R;
import com.izigo.optimized.listeners.BaseTextWatcher;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.models.viewmodels.ResultItem;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.SPUtils;
import com.izigo.optimized.utils.Utils;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberType;
import io.michaelrocks.libphonenumber.android.Phonenumber.PhoneNumber;
import java8.util.stream.StreamSupport;

import static android.os.Build.VERSION.SDK_INT;


public class TransferDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = TransferDialogFragment.class.getSimpleName();
    private static final int REQUEST_CODE = 1;
    @BindView(R.id.button_transfer)
    MaterialButton mBtnTransfer;
    @BindView(R.id.til_account_number)
    TextInputLayout mTilAccountNumber;
    @BindView(R.id.til_amount_to_send)
    TextInputLayout mTilAmountToSend;
    @BindView(R.id.material_toggle_group)
    MaterialButtonToggleGroup mToggleGroup;
    @BindView(R.id.checkbox_withdrawal_fee)
    AppCompatCheckBox mCheckBoxFee;
    private String mMnc;
    private double mAmountToSend, mMin, mMax;
    private List<Provider> mProviders;
    private final MaterialButtonToggleGroup.OnButtonCheckedListener buttonCheckedListener = new MaterialButtonToggleGroup.OnButtonCheckedListener() {
        @Override
        public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
            /* 1- Watch Toggle Group for selection changes
             * 2- If user changes selected operator, compute helper text accordingly
             * 3- If user changes selected operator, check if amount is still in range
             * 4- Validate fields ob Background thread
             */
            if (isChecked) {
                final EditText editText = mTilAmountToSend.getEditText();
                final MaterialButton button = group.findViewById(checkedId);
                final String text = getString(R.string.transfer_funds_action_transfer_hint);
                mBtnTransfer.setText(String.format(text, button.getText()));

                mTilAmountToSend.setHelperText(getHelperText(checkedId));

                if (editText != null && !editText.getText().toString().isEmpty()) {
                    final double amount = Double.parseDouble(editText.getText().toString());
                    if (!Utils.isBetweenInclusive(amount, mMin, mMax) && mTilAmountToSend.getError() != null) {
                        mTilAmountToSend.setError(getString(R.string.search_bar_input_text_error_empty_field));
                    }
                }
                //Validate Fields
                new Handler(Looper.getMainLooper()).postDelayed(() -> validateFields(), 10);
            }
        }
    };
    private SPUtils mSp;

    @NonNull
    public static TransferDialogFragment newInstance(@NonNull ArrayList<Provider> providers,
                                                     final String mnc,
                                                     @FloatRange(from = 0f) final double amount) {
        final TransferDialogFragment fragment = new TransferDialogFragment();
        final Bundle bundle = new Bundle();
        bundle.putDouble(Constants.KEY_FRAGMENT_PARCEL_TRANSFER_AMOUNT, amount);

        if (mnc != null && !mnc.isEmpty())
            bundle.putString(Constants.KEY_FRAGMENT_PARCEL_TRANSFER_MNC, mnc);

        bundle.putParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS, providers);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = requireArguments();
        mAmountToSend = args.getDouble(Constants.KEY_FRAGMENT_PARCEL_TRANSFER_AMOUNT);
        mMnc = args.getString(Constants.KEY_FRAGMENT_PARCEL_TRANSFER_MNC);
        mProviders = args.getParcelableArrayList(Constants.KEY_FRAGMENT_PARCEL_PROVIDERS);

        if (mSp == null)
            mSp = SPUtils.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.layout_transfer_money, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        //If we do not hold the permission to read Phone State we should dismiss this Fragment
        //TODO: change this to show notification early
        if (!PermissionUtils.isGranted(PermissionConstants.PHONE))
            dismissAllowingStateLoss();

        //Initialize Button Group
        final List<SubscriptionInfo> subscriptions = Utils.getActiveSubscriptions(requireContext());
        if (subscriptions != null && mToggleGroup.getChildCount() == 0)
            new Handler(Looper.getMainLooper()).post(() -> initializeToggleGroup(subscriptions));
    }

    @Override
    public void onResume() {
        super.onResume();
        mToggleGroup.addOnButtonCheckedListener(buttonCheckedListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        mToggleGroup.clearOnButtonCheckedListeners();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            //Send our list to phone number selector
            if (data == null) return;
            final List<String> numbers = Utils.getPhoneNumber(requireContext(), data);
            if (!numbers.isEmpty())
                selectNumberToDisplay(numbers);
        }
    }

    private void initializeToggleGroup(@NonNull final List<SubscriptionInfo> subscriptions) {
        /*Initialize SIM selector Group
         * 1- Get Button layout we will use
         * 2- Set Button as default if a valid MNC was provided
         */
        if (mMnc != null) {
            final MaterialButton button = (MaterialButton) getLayoutInflater().inflate(R.layout.layout_button_outlined, mToggleGroup, false);
            final SubscriptionInfo info = StreamSupport.stream(subscriptions)
                    .filter(inf -> SDK_INT >= Build.VERSION_CODES.Q
                            ? inf.getMncString().equals(mMnc) : inf.getMnc() == Integer.parseInt(mMnc))
                    .findFirst().orElseThrow();

            button.setText(info.getDisplayName());
            button.setTag(info);
            mToggleGroup.addView(button);
            mToggleGroup.check(button.getId());
            return;
        }

        /* Initialize SIM selector Group
         * 3- If no MNC is provided, we fetch a list of Active SIMS
         * 4- We set the fetched list to the Chip group by order of SIM Slot found
         * 5- If we have more than one active SIM, select the first
         */
        for (SubscriptionInfo info : subscriptions) {
            final MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.layout_button_outlined, mToggleGroup, false);
            button.setText(info.getDisplayName());
            button.setTag(info);
            mToggleGroup.addView(button);
        }

        mToggleGroup.check(mToggleGroup.getChildAt(0).getId());
    }

    private void initializeViews() {
        /*Initialize Transfer button*/
        mBtnTransfer.setText(R.string.transfer_funds_action_transfer_hint);
        mBtnTransfer.setEnabled(false);

        //Initialize Amount field
        mTilAmountToSend.setEnabled(!(mAmountToSend > 0f));
        Objects.requireNonNull(mTilAmountToSend.getEditText()).setText(mAmountToSend > 0f
                ? String.format(App.NUMBER_FORMAT, mAmountToSend)
                : null);

        //Initialize Account Number Field
        if (mTilAccountNumber.getEditText() != null) {
            mTilAccountNumber.getEditText().setFilters(new InputFilter[]{new LengthFilter(App.PHONE_NUMBER_LENGTH)});
            mTilAccountNumber.setCounterMaxLength(App.PHONE_NUMBER_LENGTH);

            //Watch AccountNumber field for changes
            mTilAccountNumber.getEditText().addTextChangedListener(new BaseTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    super.afterTextChanged(editable);

                    if (editable.length() > 2) {
                        try {
                            final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.createInstance(requireContext());
                            final PhoneNumber phoneNumber = phoneNumberUtil.parse(editable, "CM");

                            mTilAccountNumber.setError(!phoneNumberUtil.isValidNumber(phoneNumber) ||
                                    !phoneNumberUtil.getNumberType(phoneNumber).equals(PhoneNumberType.MOBILE)
                                    ? getString(R.string.transfer_funds_input_number_invalid)
                                    : null);

                        } catch (NumberParseException e) {
                            LogUtils.eTag(TAG, e);
                        }
                    }

                    //useful when user clears account number field
                    if (editable.length() == 0) mTilAccountNumber.setError(null);
                    if (editable.length() > 0 && editable.length() <= 2)
                        mTilAccountNumber.setError(getString(R.string.transfer_funds_input_number_invalid));

                    //Validate field
                    new Handler(Looper.getMainLooper()).postDelayed(() -> validateFields(), 10);
                }
            });
        }

        //Initialize Amount Field
        if (mTilAmountToSend.getEditText() != null) {
            mTilAmountToSend.getEditText().addTextChangedListener(new BaseTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    super.afterTextChanged(editable);

                    //useful when user clears account number field
                    if (editable.length() != 0) {
                        //Set Error text if amount not in range
                        final double amount = Double.parseDouble(editable.toString());
                        if (!Utils.isBetweenInclusive(amount, mMin, mMax))
                            mTilAmountToSend.setError(getString(R.string.search_bar_input_text_error_empty_field));
                        else
                            mTilAmountToSend.setError(null);
                    }
                    //useful when user clears account number field
                    if (editable.length() == 0) mTilAmountToSend.setError(null);

                    //Validate Fields
                    new Handler(Looper.getMainLooper()).postDelayed(() -> validateFields(), 10);
                }
            });
        }
    }

    @OnClick(R.id.button_transfer)
    public void onButtonTransferClicked() {
        // All field were validated correctly. we can try money transfer
        dismiss();

        final MaterialButton button = mToggleGroup.findViewById(mToggleGroup.getCheckedButtonId());
        final SubscriptionInfo info = (SubscriptionInfo) button.getTag();
        final boolean shouldCall = mSp.getBoolean(Constants.KEY_PREF_DIAL_BEHAVIOR, false);

        new ThreadUtils.SimpleTask<String>() {
            @Override
            public String doInBackground() {
                final Provider provider = StreamSupport.stream(mProviders)
                        .filter(op -> SDK_INT >= Build.VERSION_CODES.Q
                                ? info.getMncString().equals(op.getMnc())
                                : info.getMnc() == Integer.parseInt(op.getMnc()))
                        .findFirst().orElseThrow();
                return getUriForDialOrCall(provider);
            }

            @Override
            public void onSuccess(String uri) {
                //Show Dialog for user confirmation
                if (shouldCall) {
                    Utils.callPhone(requireContext(), uri, info.getSimSlotIndex());
                    return;
                }
                PhoneUtils.dial(uri);
            }

            @Override
            public void onFail(Throwable t) {
                super.onFail(t);
                LogUtils.eTag(TAG, t);
            }
        }.run();
    }

    @OnClick(R.id.chip_get_contacts)
    public void onButtonGetContactsClicked() {
        PermissionUtils
                .permission(Manifest.permission.READ_CONTACTS)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> granted) {
                        LogUtils.dTag(TAG, granted);
                        final Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                        startActivityForResult(intent, REQUEST_CODE);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                        LogUtils.eTag(TAG, denied, deniedForever);
                        if (!deniedForever.isEmpty()) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.rationale_grant_read_contacts)
                                    .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss())
                                    .setPositiveButton(R.string.action_settings, (dialog, i) -> {
                                        dialog.dismiss();
                                        AppUtils.launchAppDetailsSettings();
                                    })
                                    .show();
                        }
                    }
                })
                .rationale((activity, shouldRequest) ->
                        new AlertDialog.Builder(requireContext())
                        .setMessage(R.string.rationale_grant_read_contacts)
                        .setNegativeButton(android.R.string.cancel, (dialog, i) -> shouldRequest.again(false))
                        .setPositiveButton(android.R.string.ok, (dialog, i) -> shouldRequest.again(true))
                        .setOnDismissListener(dialog -> shouldRequest.again(false))
                        .show())
                .request();
    }

    private void selectNumberToDisplay(@NonNull final List<String> numbers) {
        final AtomicInteger selectedIndex = new AtomicInteger(0);
        if (mTilAccountNumber.getEditText() == null) return;

        if (numbers.size() == 1) {
            mTilAccountNumber.getEditText().setText(numbers.get(selectedIndex.get()));
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.transfer_funds_dialog_select_number)
                .setSingleChoiceItems(numbers.toArray(new String[0]), 0, (dialogInterface, i)
                        -> selectedIndex.set(i))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    //Put selected phone number in Edittext
                    mTilAccountNumber.getEditText().setText(numbers.get(selectedIndex.get()));
                    /*Show alert dialog to remind user his responsibility to always check if
                     * the phone number is correct and is from the correct network operator
                     */
                    if (!mSp.getBoolean(Constants.KEY_PREF_SHOW_CONTACTS_WARNING, false)) {
                        DialogCheckboxExtKt.checkBoxPrompt(new MaterialDialog(requireContext(), ModalDialog.INSTANCE),
                                R.string.transfer_funds_check_phone_number_understood, null, false, aBoolean -> {
                                    mSp.setValue(Constants.KEY_PREF_SHOW_CONTACTS_WARNING, aBoolean);
                                    return null;
                                })
                                .positiveButton(android.R.string.ok, null, null)
                                .message(R.string.transfer_funds_check_phone_number, null, null)
                                .show();
                    }
                })
                .show();
    }

    @NonNull
    @CheckResult
    private String getHelperText(final int checkedId) {
        // Get selected Operator, then get Maximum transferable amount and set it as helper text
        final MaterialButton button = mToggleGroup.findViewById(checkedId);
        final SubscriptionInfo info = (SubscriptionInfo) button.getTag();
        final Provider provider = StreamSupport.stream(mProviders)
                .filter(op -> SDK_INT >= Build.VERSION_CODES.Q
                        ? info.getMncString().equals(op.getMnc())
                        : info.getMnc() == Integer.parseInt(op.getMnc()))
                .findFirst().orElseThrow();

        final double[] pricingListValues = Utils.getPricingListValues(ArrayUtils.asUnmodifiableList(provider),
                Constants.VALUE_OPERATION_TYPE_TRANSFER_INTRA);
        final Pair<Double, Double> minMax = Utils.computeMinAndMaxValues(pricingListValues);
        mMin = minMax.getValue0();
        mMax = minMax.getValue1();
        return Utils.getHelperText(requireContext(), mMin, mMax);
    }

    private void validateFields() {
        boolean isValid = true;
        if (mTilAmountToSend.getError() != null || mTilAccountNumber.getError() != null)
            isValid = false;
        else if (mTilAccountNumber.getEditText() == null || mTilAmountToSend.getEditText() == null)
            isValid = false;
        else if (mTilAccountNumber.getEditText().getText().toString().isEmpty()
                || mTilAmountToSend.getEditText().getText().toString().isEmpty())
            isValid = false;
        mBtnTransfer.setEnabled(isValid);
    }

    @NonNull
    @CheckResult
    private String getUriForDialOrCall(@NonNull Provider provider) {
        //Prepare data for Dial or Call
        final String phoneNumber = Objects.requireNonNull(mTilAccountNumber.getEditText()).getText().toString();
        final int level = Integer.parseInt(mSp.getString(Constants.KEY_PREF_TRX_THRESHOLD,
                getString(R.string.list_transactions_threshold_default)));

        final int mode = Integer.parseInt(mSp.getString(Constants.KEY_PREF_SEARCH_MODE,
                getString(R.string.list_transactions_search_mode_default)));

        // Sum up Amount and withdrawal fee if applicable
        double amount = Double.parseDouble(Objects.requireNonNull(mTilAmountToSend.getEditText()).getText().toString());
        final ResultItem result = provider.getOptimizedTransactions(amount, level, mode, Constants.VALUE_OPERATION_TYPE_WITHDRAWAL);
        final double fee = mCheckBoxFee.isChecked() ? result.getOptimizedFee() : 0;
        amount += fee;
        return Utils.getTransferUri(provider, phoneNumber, String.format(App.NUMBER_FORMAT, amount));
    }
}