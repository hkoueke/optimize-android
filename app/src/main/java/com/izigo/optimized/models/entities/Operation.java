package com.izigo.optimized.models.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

@IgnoreExtraProperties
@Keep
public class Operation implements Parcelable, Comparable<Operation> {
    private int id;
    private String name;
    private List<Line> pricing;

    public Operation() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, pricing);
    }

    @PropertyName("id")
    public int getId() {
        return this.id;
    }

    @PropertyName("id")
    public void setId(int id) {
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

    @PropertyName("pricing")
    @NonNull
    public List<Line> getPricing() {
        final List<Line> list = this.pricing;
        if (list == null) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(list).filter(java8.util.Objects::nonNull).collect(Collectors.toUnmodifiableList());
    }

    @PropertyName("pricing")
    public void setPricing(@NonNull List<Line> pricing) {
        StreamSupport.stream(pricing).filter(java8.util.Objects::nonNull);
        this.pricing = pricing;
    }

    @Override
    public int compareTo(@NonNull Operation operation) {
        return Integer.compare(this.id, operation.id);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeTypedList(this.pricing);
    }

    protected Operation(@NonNull Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.pricing = in.createTypedArrayList(Line.CREATOR);
    }

    public static final Creator<Operation> CREATOR = new Creator<Operation>() {
        @Override
        public Operation createFromParcel(@NonNull Parcel source) {
            return new Operation(source);
        }

        @Override
        public Operation[] newArray(int size) {
            return new Operation[size];
        }
    };
}
