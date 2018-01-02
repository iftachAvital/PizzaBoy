package com.example.iftach.pizzaboy;

import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by iftach on 01/01/18.
 */

class HourData implements Parcelable {
    private int tips_value;
    private int deliveries_value;
    private int num;

    HourData() {
        this.tips_value = 0;
        this.deliveries_value = 0;
        this.num = 0;
    }

    HourData(Map map) throws IllegalAccessException {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (map.containsKey(field.getName())) {
                field.setInt(this, ((Long) map.get(field.getName())).intValue());
            }
        }
    }

    void addValue(int num, int tips) {
        this.tips_value += tips;
        this.deliveries_value += num;
        this.num++;
    }

    int getTipsValue() {return tips_value;}

    int getDeliveriesValue() {return deliveries_value;}

    float getAverageNum() {
        return num == 0 ? 0 : (float) deliveries_value / (float) num;
    }

    float getAverageTip() {
        return num == 0 ? 0 : (float) tips_value / (float) num;
    }

    protected HourData(Parcel in) {
        tips_value = in.readInt();
        deliveries_value = in.readInt();
        num = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(tips_value);
        dest.writeInt(deliveries_value);
        dest.writeInt(num);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<HourData> CREATOR = new Parcelable.Creator<HourData>() {
        @Override
        public HourData createFromParcel(Parcel in) {
            return new HourData(in);
        }

        @Override
        public HourData[] newArray(int size) {
            return new HourData[size];
        }
    };
}