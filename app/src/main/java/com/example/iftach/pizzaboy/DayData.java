package com.example.iftach.pizzaboy;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by iftach on 01/01/18.
 */

class DayData implements Parcelable {

    private ArrayList<HourData> hours;
    private long tips;
    private long shiftsNum;
    private long deliveriesNum;
    private long minutes;

    DayData() {
        hours = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) hours.add(new HourData());
    }

    DayData(Map map) throws IllegalAccessException {
        tips = (long) map.get("tips");
        shiftsNum = (long) map.get("shiftsNum");
        deliveriesNum = (long) map.get("deliveriesNum");
        minutes = (long) map.get("minutes");
        Map hoursMap = (Map) map.get("hours");
        hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(new HourData((Map) hoursMap.get(String.valueOf(i))));
        }
    }

    public ArrayList<HourData> getHours() {
        return hours;
    }

    public void setHours(ArrayList<HourData> hours) {
        this.hours = hours;
    }

    public long getTips() {
        return tips;
    }

    public void setTips(long tips) {
        this.tips = tips;
    }

    public long getShiftsNum() {
        return shiftsNum;
    }

    public void setShiftsNum(long shiftsNum) {
        this.shiftsNum = shiftsNum;
    }

    public long getDeliveriesNum() {
        return deliveriesNum;
    }

    public void setDeliveriesNum(long deliveriesNum) {
        this.deliveriesNum = deliveriesNum;
    }

    public long getMinutes() {
        return minutes;
    }

    public void setMinutes(long minutes) {
        this.minutes = minutes;
    }

    void addValue(int hour, int value, int tips) {
        hours.get(hour).addValue(value, tips);
    }

    HourData getHourData(int hour) {return hours.get(hour);}

    BarDataSet toNumDataSet() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();

        for (int i = 11; i < 29; i++) {
            barEntries.add(new BarEntry(i, hours.get(i > 23 ? i-24 : i).getAverageNum()));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Deliveries");
        barDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        return barDataSet;
    }

    BarDataSet toTipsDataSet() {
        ArrayList<BarEntry> barEntries = new ArrayList<>();

        for (int i = 11; i < 29; i++) {
            barEntries.add(new BarEntry(i, hours.get(i > 23 ? i-24 : i).getAverageTip()));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Tips");
        barDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        return barDataSet;
    }

    protected DayData(Parcel in) {
        tips = in.readLong();
        shiftsNum = in.readLong();
        deliveriesNum = in.readLong();
        minutes = in.readLong();
        if (in.readByte() == 0x01) {
            hours = new ArrayList<HourData>();
            in.readList(hours, HourData.class.getClassLoader());
        } else {
            hours = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(tips);
        dest.writeLong(shiftsNum);
        dest.writeLong(deliveriesNum);
        dest.writeLong(minutes);
        if (hours == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(hours);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DayData> CREATOR = new Parcelable.Creator<DayData>() {
        @Override
        public DayData createFromParcel(Parcel in) {
            return new DayData(in);
        }

        @Override
        public DayData[] newArray(int size) {
            return new DayData[size];
        }
    };
}
