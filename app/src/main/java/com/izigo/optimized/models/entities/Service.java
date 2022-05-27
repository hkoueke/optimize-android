package com.izigo.optimized.models.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.PropertyName;

@Keep
public class Service implements Parcelable {

    private int id;
    private String name;
    private String ussd;

    public Service() {
    }

    @PropertyName("id")
    public int getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(@IntRange(from = 0, to = 20) int id) {
        this.id = id;
    }

    @PropertyName("name")
    @Nullable
    public String getName() {
        return name;
    }

    @PropertyName("name")
    public void setName(@Nullable String name) {
        this.name = name;
    }

    @PropertyName("service_ussd")
    @Nullable
    public String getUssd() {
        return ussd;
    }

    @PropertyName("service_ussd")
    public void setUssd(@Nullable String ussd) {
        this.ussd = ussd;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeString(this.ussd);
    }

    protected Service(@NonNull Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.ussd = in.readString();
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        @NonNull
        public Service createFromParcel(@NonNull Parcel source) {
            return new Service(source);
        }

        @Override
        @NonNull
        public Service[] newArray(int size) {
            return new Service[size];
        }
    };
}
