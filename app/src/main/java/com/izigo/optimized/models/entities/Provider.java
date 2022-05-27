package com.izigo.optimized.models.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;
import com.izigo.optimized.App;
import com.izigo.optimized.models.Transaction;
import com.izigo.optimized.models.viewmodels.ResultItem;
import com.izigo.optimized.utils.Constants;
import com.izigo.optimized.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java8.lang.Doubles;
import java8.util.Objects;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

@IgnoreExtraProperties
@Keep
public class Provider implements Parcelable {
    private String id;
    private String name;
    private String mnc;
    private boolean isActive;
    private List<Operation> operations;
    private String iconUri;
    private String pricingUri;
    private String walletUssdCode;
    private String walletUssdTag;
    private List<Service> services;

    public Provider() {
        super();
    }

    @PropertyName("id")
    @NonNull
    public String getId() {
        return this.id;
    }

    @PropertyName("id")
    public void setId(@NonNull String id) {
        this.id = id;
    }

    @PropertyName("name")
    @NonNull
    public String getName() {
        return this.name;
    }

    @PropertyName("name")
    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    @PropertyName("mnc")
    public String getMnc() {
        return mnc;
    }

    @PropertyName("mnc")
    public void setMnc(String mnc) {
        this.mnc = mnc;
    }

    @PropertyName("active")
    public boolean isActive() {
        return this.isActive;
    }

    @PropertyName("active")
    public void setActive(boolean active) {
        this.isActive = active;
    }

    @PropertyName("operations")
    @NonNull
    public List<Operation> getOperations() {
        return StreamSupport.stream(this.operations).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @PropertyName("operations")
    public void setOperations(@NonNull List<Operation> operations) {
        StreamSupport.stream(operations).filter(Objects::nonNull);
        this.operations = operations;
    }

    @PropertyName("icon_uri")
    @NonNull
    public String getIconUri() {
        return this.iconUri;
    }

    @PropertyName("icon_uri")
    public void setIconUri(@NonNull String iconUri) {
        this.iconUri = iconUri;
    }

    @PropertyName("pricing_uri")
    @NonNull
    public String getPricingUri() {
        return this.pricingUri;
    }

    @PropertyName("pricing_uri")
    public void setPricingUri(@NonNull String pricingUri) {
        this.pricingUri = pricingUri;
    }

    @PropertyName("wallet_ussd_code")
    public void setWalletUssdCode(@NonNull String code) {
        this.walletUssdCode = code;
    }

    @PropertyName("wallet_ussd_code")
    @NonNull
    public String getWalletUssdCode() {
        return this.walletUssdCode;
    }

    @PropertyName("wallet_ussd_tag")
    public void setWalletUssdTag(@NonNull String tag) {
        this.walletUssdTag = tag;
    }

    @PropertyName("wallet_ussd_tag")
    @NonNull
    public String getWalletUssdTag() {
        return this.walletUssdTag;
    }

    @PropertyName("wallet_services")
    @Nullable
    public List<Service> getServices() {
        return services;
    }

    @PropertyName("wallet_services")
    public void setServices(@Nullable List<Service> services) {
        this.services = services;
    }

    public ResultItem getOptimizedTransactions(double initialAmt, int threshold, int searchMode, int operationType) {
        final double initialFee;
        final List<Line> pricing = StreamSupport.stream(this.operations)
                .filter(op -> op.getId() == operationType)
                .findFirst()
                .orElseThrow()
                .getPricing();

        final int index = Utils.getIndexForAmount(pricing, initialAmt);
        if (index < 0)
            return null;

        initialFee = pricing.get(index).getFee() < 1 ? pricing.get(index).getFee() * initialAmt : pricing.get(index).getFee();

        final List<ResultItem> results = getOptimizationsByInterval(index, initialAmt, initialFee, threshold, operationType);
        results.addAll(getOptimizations(index, initialAmt, initialFee, threshold, operationType));

        if (!results.isEmpty())
            return filterResults(results, searchMode);

        return new ResultItem(this, App.NUMBER_FORMAT, App.APP_CURRENCY)
                .withInitialAmount(initialAmt)
                .withInitialFee(initialFee).withTransactions(Collections.emptyList());
    }

    @NonNull
    private List<ResultItem> getOptimizationsByInterval(int index, double initialAmt, double initialFee,
                                                        int threshold, int operationType) {
        //Transaction List
        List<Transaction> transactions = new ArrayList<>();

        //final result list
        final List<ResultItem> results = new ArrayList<>();

        //step counter
        int step = 0;

        for (int i = index; i >= 1; i--) {
            final int subLevel = i - 1;
            double remain = initialAmt;
            double fees = 0;
            step++;

            for (int j = subLevel; j >= 0; j--) {
                //no remain to work wih? quit
                if (remain <= 0)
                    break;

                //Get current line
                final Line line = StreamSupport.stream(operations)
                        .filter(op -> op.getId() == operationType)
                        .findFirst()
                        .orElseThrow()
                        .getPricing()
                        .get(j);

                if (j != subLevel && !Utils.isBetweenInclusive(remain, line.getLower(), line.getUpper()))
                    continue;

                final double currentUpper = line.getUpper() - line.getWeight();
                final double min = Math.min(remain, currentUpper);
                final double fee = line.getFee() < 1 ? Math.min(remain, currentUpper) * line.getFee() : line.getFee();

                transactions.add(new Transaction(min, fee));
                fees += fee;
                remain -= min;

                if (remain >= currentUpper || Utils.isBetweenInclusive(remain, line.getLower(), line.getUpper()))
                    j++;
            }

            // are collected transactions worth to be kept as results?
            final double sumFee = StreamSupport.stream(transactions).map(Transaction::getTrxFee).reduce(0d, Doubles::sum);
            final double amt = StreamSupport.stream(transactions).map(Transaction::getTrxAmount).reduce(0d, Doubles::sum);

            if (fees < initialAmt && fees <= initialAmt && initialFee - sumFee > 0 && amt == initialAmt) {
                final ResultItem resultItem = new ResultItem(this, App.NUMBER_FORMAT, App.APP_CURRENCY)
                        .withInitialAmount(initialAmt).withInitialFee(initialFee).withTransactions(transactions);

                results.add(resultItem);
            }

            if (threshold > 0 && step == threshold)
                break;

            transactions = new ArrayList<>();
        }
        return results;
    }

    @NonNull
    private List<ResultItem> getOptimizations(int index, double initialAmt, double initialFee, int threshold, int operationType) {
        //Transaction List
        List<Transaction> transactions = new ArrayList<>();

        //final result list
        final List<ResultItem> results = new ArrayList<>();

        //step counter
        int step = 0;

        for (int i = index; i >= 1; i--) {
            final int subLevel = i - 1;
            double remain = initialAmt;
            double fees = 0;
            step++;

            for (int j = subLevel; j >= 0; j--) {
                //no remain to work with? quit
                if (remain <= 0)
                    break;

                //Get current line
                final Line line = StreamSupport.stream(operations)
                        .filter(op -> op.getId() == operationType).findFirst().orElseThrow().getPricing().get(j);

                final double currentUpper = line.getUpper() - line.getWeight();

                if (j > 0 && remain - currentUpper <= 0)
                    continue;

                final double min = Math.min(remain, currentUpper);
                final double fee = line.getFee() < 1 ? min * line.getFee() : line.getFee();
                transactions.add(new Transaction(min, fee));
                fees += fee;
                remain -= min;

                if (remain >= line.getUpper())
                    j++;
            }

            final double sumFee = StreamSupport.stream(transactions).map(Transaction::getTrxFee).reduce(0d, Doubles::sum);
            final double amt = StreamSupport.stream(transactions).map(Transaction::getTrxAmount).reduce(0d, Doubles::sum);

            if (fees < initialAmt && fees <= initialAmt && initialFee - sumFee > 0 && amt == initialAmt) {
                final ResultItem resultItem = new ResultItem(this, App.NUMBER_FORMAT, App.APP_CURRENCY)
                        .withInitialAmount(initialAmt).withInitialFee(initialFee).withTransactions(transactions);

                results.add(resultItem);
            }

            if (threshold > 0 && step == threshold)
                break;

            transactions = new ArrayList<>();
        }

        return results;
    }

    @NonNull
    private ResultItem filterResults(@NonNull final List<ResultItem> unsortedList, int mode) {
        switch (mode) {
            case Constants.VALUE_PREF_FILTER_PREFER_SAVINGS:
                Collections.sort(unsortedList, Utils.trxSavingsOrderByDesc);
                break;
            case Constants.VALUE_PREF_FILTER_PREFER_TRANSACTIONS_COUNT:
                Collections.sort(unsortedList, trxCountOrderByAsc);
                break;
        }
        return unsortedList.get(0);
    }

    private final Comparator<ResultItem> trxCountOrderByAsc = (r1, r2) -> {
        final Integer c1 = r1.getTransactions().size();
        final Integer c2 = r2.getTransactions().size();
        return c1.compareTo(c2);
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.name);
        dest.writeByte(this.isActive ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.operations);
        dest.writeString(this.iconUri);
        dest.writeString(this.pricingUri);
        dest.writeString(this.walletUssdCode);
        dest.writeString(this.walletUssdTag);
        dest.writeTypedList(this.services);
    }

    protected Provider(@NonNull Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.isActive = in.readByte() != 0;
        this.operations = in.createTypedArrayList(Operation.CREATOR);
        this.iconUri = in.readString();
        this.pricingUri = in.readString();
        this.walletUssdCode = in.readString();
        this.walletUssdTag = in.readString();
        this.services = in.createTypedArrayList(Service.CREATOR);
    }

    public static final Creator<Provider> CREATOR = new Creator<Provider>() {
        @Override
        public Provider createFromParcel(@NonNull Parcel source) {
            return new Provider(source);
        }

        @Override
        @NonNull
        public Provider[] newArray(int size) {
            return new Provider[size];
        }
    };
}