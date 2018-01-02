package com.example.iftach.pizzaboy;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftStatisticsActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private WeekData weekData;
    private Shift shift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_statistics);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        weekData = ((MyApplication) getApplication()).getWeekData();
        shift = getIntent().getParcelableExtra(Constants.SHIFT_EXTRA);
        if (shift != null) intiCharts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return true;
        }
    }

    private void intiCharts() {
        float groupSpace = 0.2f;
        float barSpace = 0f;
        float barWidth = 0.4f;

        ArrayList<Delivery> deliveries = shift.getDeliveries();

        long shiftEnd = getEndShiftInMillis(shift.getShiftStart());

        LineChart lineChart = findViewById(R.id.shift_line_chart);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setSpaceTop(30f);
        lineChart.getLegend().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawAxisLine(true);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setAxisMaximum(shiftEnd + 30 * 60 * 1000);
        lineChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return new SimpleDateFormat("HH:mm", Constants.EN_LOCAL).format(new Date((long) value));
            }
        });

        BarChart barChart = findViewById(R.id.shift_bar_chart);
        barChart.getAxisLeft().setSpaceTop(30f);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisRight().setEnabled(false);
        barChart.setDescription(null);
        barChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (value > 23) value -= 24;
                int amPm = 0;

                if (value > 12) {
                    amPm = 1;
                    value -= 12;
                }

                return String.format(Constants.EN_LOCAL, "%d%s", (int) value, amPm == 1 ? "p" : "a");
            }
        });

        ArrayList<BarEntry> currentBarEntries = new ArrayList<>();
        ArrayList<BarEntry> historyBarEntries = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        DayData dayData = new DayData();
        int startHour;
        int endHour;
        int day;

        calendar.setTimeInMillis(shift.getShiftStart());
        day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        startHour = calendar.get(Calendar.HOUR_OF_DAY);
        if (shift.getShiftEnd() > 0) calendar.setTimeInMillis(shift.getShiftEnd());
        else calendar.setTimeInMillis(getEndShiftInMillis(shift.getShiftStart()));
        endHour = calendar.get(Calendar.HOUR_OF_DAY);
        if (endHour < startHour) endHour+= 24;

        for (Delivery delivery : deliveries) {
            calendar.setTimeInMillis(delivery.getArrivalTime());
            dayData.addValue(calendar.get(Calendar.HOUR_OF_DAY), 1, delivery.getTip());
        }

        for (int i = startHour; i < endHour; i++) {
            int hour = i > 23 ? i - 24 : i;
            currentBarEntries.add(new BarEntry(i, dayData.getHourData(hour).getTipsValue()));
            historyBarEntries.add(new BarEntry(i, weekData.getDayData(day).getHourData(hour).getAverageTip()));
        }

        BarDataSet currentBarDataSet = new BarDataSet(currentBarEntries, "Current");
        currentBarDataSet.setColor(Color.parseColor("#FF80AB"));
        currentBarDataSet.setDrawValues(false);

        BarDataSet historyBarDataSet = new BarDataSet(historyBarEntries, "History");
        historyBarDataSet.setColor(Color.parseColor("#2196F3"));
        historyBarDataSet.setDrawValues(false);

        BarData barData = new BarData(currentBarDataSet, historyBarDataSet);
        barData.setBarWidth(barWidth);
        barChart.setData(barData);
        barChart.groupBars(startHour - 0.5f, groupSpace, barSpace);
        barChart.setFitBars(true);
        barChart.invalidate();


        lineChart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        Collections.sort(deliveries);
        int tips = 0;
        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry(shift.getShiftStart(), tips));

        for (Delivery delivery : deliveries) {
            tips += delivery.getTip();
            entries.add(new Entry(delivery.getArrivalTime(), tips));
        }

        entries.add(new Entry(shift.getShiftEnd() > 0 ? shift.getShiftEnd() : System.currentTimeMillis(), tips));
        LineDataSet lineDataSet = new LineDataSet(entries, "Tips");
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setLineWidth(3.0f);
        lineDataSet.setColor(Color.parseColor("#00BCD4"));

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

//        float shiftAverage = (float) tips / ((float)(shiftEnd - shiftStart));
//        float predictEnd = shiftAverage * (getEndShiftInMillis(shiftStart) - shiftStart);
//        Log.d(TAG, "shift average: " + shiftAverage + " predict end: " + predictEnd);


//        List<Entry> predictEntries = new ArrayList<>();
//        predictEntries.add(new Entry(lineDataSet.getXMax(), lineDataSet.getYMax()));
////        predictEntries.add(new Entry(getEndShiftInMillis(shiftStart), predictEnd));
//
//        LineDataSet predictionDataSet = new LineDataSet(predictEntries, "Prediction");
//        predictionDataSet.setDrawCircles(false);
//        predictionDataSet.setLineWidth(3.0f);
//        predictionDataSet.setColor(Color.parseColor("#757575"));
//        predictionDataSet.enableDashedLine(10f, 10f, 0);
//
    }


    private long getEndShiftInMillis(long shiftStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(shiftStart);
        int hour = 3;

        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.THURSDAY:
                hour = 4;
                break;
            case Calendar.FRIDAY:
                hour = 6;
                break;
        }

        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, hour);

        return calendar.getTimeInMillis();
    }

    private String histogramHourFormat(long value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        return String.format(Constants.EN_LOCAL, "%d%s", calendar.get(Calendar.HOUR),
                calendar.get(Calendar.AM_PM) == 1 ? "p" : "a");
    }
}
