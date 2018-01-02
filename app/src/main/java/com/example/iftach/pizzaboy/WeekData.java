package com.example.iftach.pizzaboy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by iftach on 31/12/17.
 */
class WeekData implements Parcelable {

    private ArrayList<DayData> days;

        WeekData(DocumentSnapshot snapshot) throws IllegalAccessException {
        days = new ArrayList<>();
        Map weekMap = (Map<String, Object>) snapshot.get("week_data");
        Map daysMap = (Map) weekMap.get("days");
        for (int i = 0; i < 7; i++) {
            days.add(new DayData((Map) daysMap.get(String.valueOf(i))));
        }
    }

//    WeekData(ArrayList<Shift> shifts) {
//        days = new ArrayList<>(7);
//        for (int i = 0; i < 7; i++) days.add(new DayData());
//
//        Calendar calendar = Calendar.getInstance();
//
//        for (Shift shift : shifts) {
//            int deliveriesInHour[] = new int[24];
//            int tipsInHour[] = new int[24];
//
//            calendar.setTimeInMillis(shift.getShiftStart());
//            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
//            int shiftStartHour = calendar.get(Calendar.HOUR_OF_DAY);
//            calendar.setTimeInMillis(shift.getShiftEnd());
//            int shiftEndHour = calendar.get(Calendar.HOUR_OF_DAY);
//
//            for (Delivery delivery : shift.getDeliveries()) {
//                calendar.setTimeInMillis(delivery.getArrivalTime());
//                int hour = calendar.get(Calendar.HOUR_OF_DAY);
//                deliveriesInHour[hour]++;
//                tipsInHour[hour] += delivery.getTip();
//            }
//
//            boolean normal = shiftEndHour > shiftStartHour;
//
//            for (int i = 0; i < 24; i++) {
//                boolean inHours = i >= shiftStartHour && i <= shiftEndHour;
//                if ((normal && inHours) || (!normal && !inHours)) {
//                    addValue(day, i, deliveriesInHour[i], tipsInHour[i]);
//                }
//            }
//        }
//    }

    private void addValue(int day, int hour, int value, int tips) {
        days.get(day).addValue(hour, value, tips);
    }

    void saveToPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = gson.toJson(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.WEEK_DATA, json);
        editor.apply();
    }

    static WeekData loadFromPref(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Gson gson = new Gson();
        String json = preferences.getString(Constants.WEEK_DATA, null);

        if (json != null) {
            return gson.fromJson(json, WeekData.class);
        }
        else {
            return null;
        }
    }

    DayData getDayData(int day) {return days.get(day);}

    BarData toNumData(int day) {
        BarDataSet barDataSets = days.get(day).toNumDataSet();
        return new BarData(barDataSets);
    }

    BarData toTipsData(int day) {
        BarDataSet barDataSets = days.get(day).toTipsDataSet();
        return new BarData(barDataSets);
    }

    WeekData(Parcel in) {
        if (in.readByte() == 0x01) {
            days = new ArrayList<DayData>();
            in.readList(days, DayData.class.getClassLoader());
        } else {
            days = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (days == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(days);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<WeekData> CREATOR = new Parcelable.Creator<WeekData>() {
        @Override
        public WeekData createFromParcel(Parcel in) {
            return new WeekData(in);
        }

        @Override
        public WeekData[] newArray(int size) {
            return new WeekData[size];
        }
    };
}