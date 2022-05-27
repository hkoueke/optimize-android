package com.izigo.optimized.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import androidx.annotation.CheckResult;
import androidx.annotation.FloatRange;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.blankj.utilcode.util.FragmentUtils;
import com.blankj.utilcode.util.IntentUtils;
import com.blankj.utilcode.util.LogUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.izigo.optimized.App;
import com.izigo.optimized.R;
import com.izigo.optimized.models.entities.Line;
import com.izigo.optimized.models.entities.Provider;
import com.izigo.optimized.models.entities.Service;
import com.izigo.optimized.models.viewmodels.ResultItem;

import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import java8.util.DoubleSummaryStatistics;
import java8.util.stream.Collectors;
import java8.util.stream.DoubleStreams;
import java8.util.stream.StreamSupport;

import static android.os.Build.VERSION.SDK_INT;

public final class Utils {
    private Utils() {
    }

    private static final String TAG = Utils.class.getSimpleName();

    @NonNull
    @CheckResult
    public static Pair<Double, Double> computeMinAndMaxValues(@NonNull final double... values) {
        final DoubleSummaryStatistics stats = DoubleStreams.of(values).summaryStatistics();
        return Pair.with(stats.getMin(), stats.getMax());
    }

    @CheckResult
    private static int getPricingLinesCount(@NonNull final List<Provider> providers, int operationType) {
        int lines = 0;
        for (int i = 0; i < providers.size(); i++) {
            lines += StreamSupport
                    .stream(providers.get(i).getOperations())
                    .filter(op -> op.getId() == operationType)
                    .findFirst()
                    .orElseThrow()
                    .getPricing()
                    .size();
        }
        return lines;
    }

    @CheckResult
    public static int getIndexForAmount(@NonNull List<Line> lines, @FloatRange(from = 0.0) double amount) {
        final int size = lines.size();
        for (int i = 0; i < size; i++) {
            final Line line = lines.get(i);
            final boolean lowerThanUpper = line.getLower() < line.getUpper();
            if (lowerThanUpper && amount >= line.getLower() && amount <= line.getUpper()) {
                return i;
            }
            if (!lowerThanUpper && amount <= line.getLower() && amount >= line.getUpper()) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @CheckResult
    public static double[] getPricingListValues(@NonNull final List<Provider> providers, int operationType) {
        int index = 0;
        final int size = getPricingLinesCount(providers, operationType);
        final double[] values = new double[size * 2];
        for (Provider p : providers) {
            for (Line line :
                    StreamSupport
                            .stream(p.getOperations())
                            .filter(op -> op.getId() == operationType)
                            .findFirst()
                            .orElseThrow()
                            .getPricing()) {
                values[index] = line.getLower();
                index++;
                values[index] = line.getUpper();
                index++;
            }
        }
        return values;
    }

    @NonNull
    @CheckResult
    public static String getProvidersSummary(@NonNull Context context, @NonNull List<Provider> providers) {
        int counter = 1;
        final StringBuilder builder = new StringBuilder();

        for (Provider p : providers) {
            if (counter > 2) {
                final int remain = (providers.size() - counter) + 1;
                builder.append(context.getResources().getQuantityString(R.plurals.providers_count_plurals, remain, remain));
                break;
            }

            builder.append(p.getId());
            if (providers.size() > counter)
                builder.append(", ");

            counter++;
        }
        return builder.toString();
    }

    @NonNull
    @CheckResult
    public static String getFormattedDateTime(@NonNull Context context, long timestamp) {
        final String date = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_YEAR);
        final String time = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
        return String.format(context.getString(R.string.preference_category_settings_pref_manual_update_summary), date, time);
    }

    @NonNull
    @CheckResult
    public static String getHelperText(@NonNull Context context, final double minimum, final double maximum) {
        final String minStr = String.format(Locale.getDefault(), App.NUMBER_FORMAT, minimum);
        final String maxStr = String.format(Locale.getDefault(), App.NUMBER_FORMAT, maximum);
        return String.format(context.getString(R.string.search_bar_input_helper_text__form_to), minStr, App.APP_CURRENCY, maxStr);
    }

    @NonNull
    @CheckResult
    public static TableRow buildTableRow(@NonNull Context context) {
        final TableRow tr = new TableRow(context);
        tr.setId(R.id.table_row);
        tr.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        tr.setDividerDrawable(ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_bright));
        tr.setGravity(Gravity.CENTER);
        tr.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return tr;
    }

    @NonNull
    @CheckResult
    public static LinearLayoutCompat buildLinearLayout(@NonNull Context context, int width, int height, int orientation) {
        final LinearLayoutCompat layout = new LinearLayoutCompat(context);
        layout.setLayoutParams(new LinearLayoutCompat.LayoutParams(width, height));
        layout.setOrientation(orientation);
        return layout;
    }

    @NonNull
    @CheckResult
    public static AppCompatTextView buildTextView(@NonNull Context context, @IdRes int id) {
        final AppCompatTextView tv = new AppCompatTextView(context);
        tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(tv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        final int[] sizeValues = context.getResources().getIntArray(R.array.autosize_text_sizes_int);
        TextViewCompat.setAutoSizeTextTypeUniformWithPresetSizes(tv, sizeValues, TypedValue.COMPLEX_UNIT_SP);
        tv.setId(id);
        tv.setSingleLine();
        return tv;
    }

    @CheckResult
    public static boolean isBetweenInclusive(final double amount, final double lower, final double upper) {
        if ((lower < upper) && (amount >= lower && amount <= upper))
            return true;
        else
            return (lower > upper) && amount <= lower && amount >= upper;
    }

    public static final Comparator<ResultItem> trxSavingsOrderByDesc = (r1, r2) -> {
        final Double s1 = r1.getSavings();
        final Double s2 = r2.getSavings();
        final Integer c1 = r1.getTransactions().size();
        final Integer c2 = r2.getTransactions().size();

        if (s1.equals(s2)) return c1.compareTo(c2);
        return s2.compareTo(s1);
    };

    @NonNull
    @CheckResult
    public static BottomSheetDialog getBottomSheetDialog(@NonNull Context context) {
        final BottomSheetDialog bsd = new BottomSheetDialog(context);
        bsd.setCanceledOnTouchOutside(true);
        bsd.setDismissWithAnimation(true);
        return bsd;
    }

    public static void openFragment(@NonNull FragmentManager fm, @NonNull Fragment frag) {
        FragmentUtils.replace(fm, frag, R.id.container, true,
                R.anim.slide_in_right, R.anim.slide_out_left,
                android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @NonNull
    @CheckResult
    public static ArrayList<String> getPhoneNumber(@NonNull Context context, @NonNull Intent data) {
        final ArrayList<String> phoneNumbers = new ArrayList<>();
        final Uri uri = data.getData();

        if (uri == null)
            return phoneNumbers;

        final Cursor cursor = context.getContentResolver().query(uri, null,
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // get the contact's information
                final String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                final int hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                // get the user's phone number(s)
                if (hasPhone > 0) {
                    final Cursor cp = context.getContentResolver()
                            .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);

                    if (cp != null && cp.moveToFirst()) {
                        do {
                            String phone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            try {
                                phone = getNationalNumber(context, phone);
                                if (!phoneNumbers.contains(phone)) {
                                    phoneNumbers.add(phone);
                                }
                            } catch (NumberParseException e) {
                                LogUtils.eTag(TAG, e);
                            }
                        } while (cp.moveToNext());
                        cp.close();
                    }
                }
            } while (cursor.moveToNext());

            // clean up cursor
            cursor.close();
        }
        return phoneNumbers;
    }

    private static final String[] PROJECTION = {
            ContactsContract.Data.DATA1, ContactsContract.Data.DATA2, ContactsContract.Data.DATA3, ContactsContract.Data.DATA4, ContactsContract.Data.DATA5, ContactsContract.Data.DATA6, ContactsContract.Data.DATA7,
            ContactsContract.Data.DATA8, ContactsContract.Data.DATA9, ContactsContract.Data.DATA10, ContactsContract.Data.DATA11, ContactsContract.Data.DATA12, ContactsContract.Data.DATA13, ContactsContract.Data.DATA14, ContactsContract.Data.DATA15
    };

    @NonNull
    @CheckResult
    private static String getNationalNumber(@NonNull final Context context, @NonNull final String number) throws NumberParseException {
        return String.valueOf(PhoneNumberUtil.createInstance(context).parseAndKeepRawInput(number, "CM").getNationalNumber());
    }

    @CheckResult
    public static boolean hasActiveSim(@NonNull final Context context) {
        final List<SubscriptionInfo> list = getActiveSubscriptions(context);
        return list != null && StreamSupport.stream(list)
                .filter(inf -> inf.getCountryIso().equals("cm"))
                .collect(Collectors.toList()).size() > 0;
    }

    @CheckResult
    public static boolean isMncInDevice(@NonNull final Context context, @NonNull String mnc) {
        final List<SubscriptionInfo> list = getActiveSubscriptions(context);
        if (list == null) return false;
        return StreamSupport.stream(list)
                .filter(inf -> SDK_INT >= Build.VERSION_CODES.Q ? inf.getMncString().equals(mnc) : inf.getMnc() == Integer.parseInt(mnc))
                .findFirst().orElse(null) != null;
    }

    @Nullable
    @CheckResult
    public static List<SubscriptionInfo> getActiveSubscriptions(@NonNull final Context context) {
        final SubscriptionManager manager = ActivityCompat.getSystemService(context, SubscriptionManager.class);
        if (manager == null || ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            return null;

        final List<SubscriptionInfo> list = manager.getActiveSubscriptionInfoList();
        if (list == null) return null;
        else return StreamSupport.stream(manager.getActiveSubscriptionInfoList())
                .filter(inf -> inf.getCountryIso().equals("cm")).collect(Collectors.toUnmodifiableList());
    }

    @NonNull
    @CheckResult
    public static String getBalanceUri(@NonNull final Provider provider) {
        final StringBuilder sb = new StringBuilder();
        assert provider.getServices() != null;
        final Service service = StreamSupport.stream(provider.getServices())
                .filter(s -> s.getId() == Constants.VALUE_SERVICE_BALANCE).findFirst().orElseThrow();
        return Uri.encode(sb.append(provider.getWalletUssdTag()).append(provider.getWalletUssdCode())
                .append("*").append(service.getUssd()).append("#").toString());
    }

    @NonNull
    @CheckResult
    public static String getTransferUri(@NonNull final Provider provider, @NonNull String phoneNumber,
                                        @NonNull String amount) {
        final StringBuilder sb = new StringBuilder();
        assert provider.getServices() != null;
        final Service service = StreamSupport.stream(provider.getServices())
                .filter(s -> s.getId() == Constants.VALUE_SERVICE_TRANSFER).findFirst().orElseThrow();

        return Uri.encode(sb.append(provider.getWalletUssdTag()).append(provider.getWalletUssdCode()).append("*")
                .append(service.getUssd()).append("*").append(phoneNumber).append("*").append(amount)
                .append("#").toString());
    }

    public static void callPhone(@NonNull final Context context, @NonNull String number, final int slot) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            final Intent intent = IntentUtils.getCallIntent(number);
            intent.putExtra("com.android.phone.extra.slot", slot);
            context.startActivity(intent);
        }
    }
}