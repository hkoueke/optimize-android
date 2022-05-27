package com.izigo.optimized.models.entities;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.FloatRange;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.Objects;

@IgnoreExtraProperties
@Keep
public class Line implements Parcelable {
    private double fee;
    private double lower;
    private double upper;
    private double weight;
    private static final String TAG = Line.class.getSimpleName();

    public Line() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(fee, lower, upper, weight);
    }

    @PropertyName("from")
    public double getLower() {
        return this.lower;
    }

    @PropertyName("from")
    public void setLower(@FloatRange(from = 0) double lower) {
        this.lower = lower;
    }

    @PropertyName("to")
    public double getUpper() {
        return this.upper;
    }

    @PropertyName("to")
    public void setUpper(@FloatRange(from = 0, fromInclusive = false) double upper) {
        if (upper <= this.lower)
            throw new IllegalArgumentException(TAG.concat(": Upper value must be greater than lower value"));

        this.upper = upper;
    }

    @PropertyName("fee")
    public double getFee() {
        return this.fee;
    }

    @PropertyName("fee")
    public void setFee(double fee) {
        this.fee = fee;
    }

    @PropertyName("weight")
    public double getWeight() {
        return this.weight;
    }

    @PropertyName("weight")
    public void setWeight(@FloatRange(from = 0) double weight) {
        this.weight = weight;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(this.fee);
        dest.writeDouble(this.lower);
        dest.writeDouble(this.upper);
        dest.writeDouble(this.weight);
    }

    protected Line(@NonNull Parcel in) {
        this.fee = in.readDouble();
        this.lower = in.readDouble();
        this.upper = in.readDouble();
        this.weight = in.readDouble();
    }

    public static final Creator<Line> CREATOR = new Creator<Line>() {
        @Override
        public Line createFromParcel(@NonNull Parcel source) {
            return new Line(source);
        }

        @Override
        public Line[] newArray(int size) {
            return new Line[size];
        }
    };
}