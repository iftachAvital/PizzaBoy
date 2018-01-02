package com.example.iftach.pizzaboy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by iftach on 18/07/17.
 */
class Shift implements Parcelable, Comparable<Shift>{
    private int totalTips;
    private int totalCost;
    private int totalPizzaDistance;
    private int totalPizzaDuration;
    private long shiftEnd;
    private long shiftStart;
    private float averagePizzaDistance;
    private float averagePizzaDuration;
    private float averageTip;
    private String id;
    private String userUid;
    private ArrayList<Delivery> deliveries;

    Shift() {
        deliveries = new ArrayList<>();
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getUserUid() {
        return userUid;
    }

    void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    long getShiftStart() {
        return shiftStart;
    }

    void setShiftStart(long shiftStart) {
        this.shiftStart = shiftStart;
    }

    long getShiftEnd() {
        return shiftEnd;
    }

    void setShiftEnd(long shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    int getTotalTips() {
        return totalTips;
    }

    void setTotalTips(int totalTips) {
        this.totalTips = totalTips;
    }

    int getTotalCost() {
        return totalCost;
    }

    void setTotalCost(int totalCost) {
        this.totalCost = totalCost;
    }

    int getTotalPizzaDistance() {
        return totalPizzaDistance;
    }

    void setTotalPizzaDistance(int totalPizzaDistance) {
        this.totalPizzaDistance = totalPizzaDistance;
    }

    float getAverageTip() {
        return averageTip;
    }

    void setAverageTip(float averageTip) {
        this.averageTip = averageTip;
    }

    float getAveragePizzaDistance() {
        return averagePizzaDistance;
    }

    void setAveragePizzaDistance(float averagePizzaDistance) {
        this.averagePizzaDistance = averagePizzaDistance;
    }

    void setAveragePizzaDuration(float averagePizzaDuration) {
        this.averagePizzaDuration = averagePizzaDuration;
    }

    void setTotalPizzaDuration(int totalPizzaDuration) {
        this.totalPizzaDuration = totalPizzaDuration;
    }

    float getAveragePizzaDuration() {
        return averagePizzaDuration;
    }

    int getTotalPizzaDuration() {
        return totalPizzaDuration;
    }

    ArrayList<Delivery> getDeliveries() {
        return deliveries;
    }

    void setDeliveries(ArrayList<Delivery> deliveries) {
        this.deliveries = deliveries;
    }

    void addDelivery(Delivery delivery) {
        deliveries.add(delivery);
    }

    void removeDelivery(Delivery delivery) {
        deliveries.remove(delivery);
    }

    void updateDelivery(Delivery updatedDelivery) {
        for (Delivery delivery : deliveries) {
            if (delivery.getArrivalTime() == updatedDelivery.getArrivalTime()) {
                deliveries.set(deliveries.indexOf(delivery), updatedDelivery);
            }
        }
    }

    float calculateTipPerHour() {
        return (float) totalTips / ((float) (shiftEnd-shiftStart) / 1000 / 60 / 60);
    }

    protected Shift(Parcel in) {
        totalTips = in.readInt();
        totalCost = in.readInt();
        totalPizzaDistance = in.readInt();
        totalPizzaDuration = in.readInt();
        shiftEnd = in.readLong();
        shiftStart = in.readLong();
        averagePizzaDuration = in.readFloat();
        averageTip = in.readFloat();
        averagePizzaDistance = in.readFloat();
        id = in.readString();
        userUid = in.readString();
        if (in.readByte() == 0x01) {
            deliveries = new ArrayList<Delivery>();
            in.readList(deliveries, Delivery.class.getClassLoader());
        } else {
            deliveries = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(totalTips);
        dest.writeInt(totalCost);
        dest.writeInt(totalPizzaDistance);
        dest.writeInt(totalPizzaDuration);
        dest.writeLong(shiftEnd);
        dest.writeLong(shiftStart);
        dest.writeFloat(averagePizzaDuration);
        dest.writeFloat(averageTip);
        dest.writeFloat(averagePizzaDistance);
        dest.writeString(id);
        dest.writeString(userUid);
        if (deliveries == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(deliveries);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Shift> CREATOR = new Parcelable.Creator<Shift>() {
        @Override
        public Shift createFromParcel(Parcel in) {
            return new Shift(in);
        }

        @Override
        public Shift[] newArray(int size) {
            return new Shift[size];
        }
    };

    @Override
    public int compareTo(@NonNull Shift o) {
        return (int) (this.getShiftStart() - o.getShiftStart());
    }

    void saveToPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = gson.toJson(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.SHIFT, json);
        editor.apply();
    }

    static Shift loadFromPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Gson gson = new Gson();
        String json = preferences.getString(Constants.SHIFT, null);

        if (json != null) {
            return gson.fromJson(json, Shift.class);
        }
        else {
            return null;
        }
    }
}
