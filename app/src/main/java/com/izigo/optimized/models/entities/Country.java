package com.izigo.optimized.models.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.blankj.utilcode.util.CollectionUtils;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.List;

import java8.util.Objects;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

@IgnoreExtraProperties
@Keep
public class Country implements Parcelable {

    private String iso;
    private String name;
    private List<Provider> providers;

    public Country() {
    }

    @PropertyName("iso_code")
    @NonNull
    public String getIso() {
        return iso;
    }

    @PropertyName("iso_code")
    public void setIso(@NonNull String iso) {
        this.iso = iso;
    }

    @PropertyName("name")
    @NonNull
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(@NonNull String name) {
        this.name = name;
    }

    @PropertyName("providers")
    @NonNull
    public List<Provider> getProviders() {
        return StreamSupport.stream(providers).filter(Objects::nonNull).filter(Provider::isActive)
                .collect(Collectors.toList());
    }

    @PropertyName("providers")
    public void setProviders(@NonNull List<Provider> providers) {
        CollectionUtils.filter(providers, Objects::nonNull);
        this.providers = providers;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(this.iso);
        dest.writeString(this.name);
        dest.writeTypedList(this.providers);
    }

    protected Country(@NonNull Parcel in) {
        this.iso = in.readString();
        this.name = in.readString();
        this.providers = in.createTypedArrayList(Provider.CREATOR);
    }

    public static final Creator<Country> CREATOR = new Creator<Country>() {
        @Override
        public Country createFromParcel(@NonNull Parcel source) {
            return new Country(source);
        }

        @Override
        public Country[] newArray(int size) {
            return new Country[size];
        }
    };
}