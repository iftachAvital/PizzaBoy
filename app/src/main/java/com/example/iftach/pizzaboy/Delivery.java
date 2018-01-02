package com.example.iftach.pizzaboy;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by iftach on 29/11/17.
 */

public class Delivery implements Comparable<Delivery>, Parcelable {
    private int pizzaDistance;
    private int pizzaDuration;
    private int cost;
    private int tip;
    private int drivingDistance;
    private long shiftStart;
    private long arrivalTime;
    private long startDrivingTime;
    private float accuracy;
    private int drivingDuration;
    private String id;
    private String address;
    private String city;
    private GeoPoint location;
    private GeoPoint startDrivingLocation;

    Delivery() {
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    long getShiftStart() {
        return shiftStart;
    }

    void setShiftStart(long shiftStart) {
        this.shiftStart = shiftStart;
    }

    long getArrivalTime() {
        return arrivalTime;
    }

    void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    int getPizzaDistance() {
        return pizzaDistance;
    }

    void setPizzaDistance(int pizzaDistance) {
        this.pizzaDistance = pizzaDistance;
    }

    int getPizzaDuration() {
        return pizzaDuration;
    }

    void setPizzaDuration(int pizzaDuration) {
        this.pizzaDuration = pizzaDuration;
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

    String getCity() {
        return city;
    }

    void setCity(String city) {
        this.city = city;
    }

    GeoPoint getLocation() {
        return location;
    }

    void setLocation(GeoPoint location) {
        this.location = location;
    }

    int getCost() {
        return cost;
    }

    void setCost(int cost) {
        this.cost = cost;
    }

    int getTip() {
        return tip;
    }

    void setTip(int tip) {
        this.tip = tip;
    }

    float getAccuracy() {
        return accuracy;
    }

    void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    GeoPoint getStartDrivingLocation() {
        return startDrivingLocation;
    }

    void setStartDrivingLocation(GeoPoint startDrivingLocation) {
        this.startDrivingLocation = startDrivingLocation;
    }

    long getStartDrivingTime() {
        return startDrivingTime;
    }

    void setStartDrivingTime(long startDrivingTime) {
        this.startDrivingTime = startDrivingTime;
    }

    int getDrivingDistance() {
        return drivingDistance;
    }

    void setDrivingDistance(int drivingDistance) {
        this.drivingDistance = drivingDistance;
    }

    int getDrivingDuration() {
        return drivingDuration;
    }

    void setDrivingDuration(int drivingDuration) {
        this.drivingDuration = drivingDuration;
    }

    boolean inTimeInterval(long startTime, long endTime) {
        return this.arrivalTime >= startTime && this.arrivalTime <= endTime;
    }

    public int compareTo(@NonNull Delivery other) {
        return (int) (this.getArrivalTime() - other.getArrivalTime());
    }

    protected Delivery(Parcel in) {
        pizzaDistance = in.readInt();
        pizzaDuration = in.readInt();
        cost = in.readInt();
        tip = in.readInt();
        drivingDistance = in.readInt();
        shiftStart = in.readLong();
        arrivalTime = in.readLong();
        startDrivingTime = in.readLong();
        accuracy = in.readFloat();
        drivingDuration = in.readInt();
        id = in.readString();
        address = in.readString();
        city = in.readString();
        if (in.readByte() == 0x01) {
            location = new GeoPoint(in.readDouble(), in.readDouble());
        }
        else {
            location = null;
        }
        if (in.readByte() == 0x01) {
            startDrivingLocation = new GeoPoint(in.readDouble(), in.readDouble());
        }
        else {
            startDrivingLocation = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pizzaDistance);
        dest.writeInt(pizzaDuration);
        dest.writeInt(cost);
        dest.writeInt(tip);
        dest.writeInt(drivingDistance);
        dest.writeLong(shiftStart);
        dest.writeLong(arrivalTime);
        dest.writeLong(startDrivingTime);
        dest.writeFloat(accuracy);
        dest.writeInt(drivingDuration);
        dest.writeString(id);
        dest.writeString(address);
        dest.writeString(city);
        if (location == null) {
            dest.writeByte((byte) (0x00));
        }
        else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(location.getLatitude());
            dest.writeDouble(location.getLongitude());
        }
        if (startDrivingLocation == null) {
            dest.writeByte((byte) (0x00));
        }
        else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(startDrivingLocation.getLatitude());
            dest.writeDouble(startDrivingLocation.getLongitude());
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Delivery> CREATOR = new Parcelable.Creator<Delivery>() {
        @Override
        public Delivery createFromParcel(Parcel in) {
            return new Delivery(in);
        }

        @Override
        public Delivery[] newArray(int size) {
            return new Delivery[size];
        }
    };
}
