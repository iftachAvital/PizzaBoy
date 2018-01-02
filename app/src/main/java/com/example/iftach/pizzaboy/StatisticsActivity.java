package com.example.iftach.pizzaboy;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String TAG = getClass().getSimpleName();

    final String[] daysString = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    private Progress progress;

    private RelativeLayout relativeLayout;
    private ArrayList<Shift> shifts;
    private ArrayList<Delivery> deliveries;
    private FirebaseFirestore db;
    private WeekData weekData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_statistics);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progress = new Progress(this);
        relativeLayout = findViewById(R.id.statistics_relative_layout);
        db = FirebaseFirestore.getInstance();
        weekData = ((MyApplication) getApplication()).getWeekData();

        showBarChartOfDayVsTipPerHour();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_statistics_clear:
                clearLayout();
                return  true;
//            case R.id.action_statistics_cost_vs_tip:
//                clearLayout();
//                showScatterChartOfCostVsTip();
//                return true;
            case R.id.action_statistics_day_vs_average_tip:
                clearLayout();
                showBarChartOfDayVsAverageTip();
                return true;
//            case R.id.action_statistics_distance_vs_tip:
//                clearLayout();
//                showScatterChartOfDistanceVsTip();
//                return true;
            case R.id.action_statistics_heatmap:
                clearLayout();
                showHeatMap();
                return true;
            case R.id.action_statistics_day_vs_tip_per_hour:
                clearLayout();
                showBarChartOfDayVsTipPerHour();
                return true;
            case R.id.action_statistics_days_hour_vs_deliveries:
                clearLayout();
                showBarChartOfHourVsDeliveries();
                return true;
            case R.id.action_statistics_days_hour_vs_tip:
                clearLayout();
                showBarChartOfHourVsTip();
                return true;
            default:
                clearLayout();
                return true;
        }
    }

    private void clearLayout() {
        relativeLayout.removeAllViewsInLayout();
        setActionBarTitle("");
    }

    private void setActionBarTitle(String title) {
        android.support.v7.app.ActionBar ab = this.getSupportActionBar();
        if (ab != null) {
            ab.setTitle(title);
        }
    }

    private void showHeatMap() {
        setActionBarTitle(getString(R.string.action_statistics_heatmap));

        relativeLayout.setPadding(0,0,0,0);

        if (shifts == null) {
            progress.showProgress("Loading..", false);
            shifts = new ArrayList<>();
            deliveries = new ArrayList<>();
            db.collection(Constants.SHIFTS_COLLECTION)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot documentSnapshots) {
                            for (DocumentSnapshot ds : documentSnapshots) {
                                Shift shift = ds.toObject(Shift.class);
                                shift.setId(ds.getId());
                                shifts.add(shift);
                                deliveries.addAll(shift.getDeliveries());
                            }
                            progress.dismissProgress();
                            callMap();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progress.dismissProgress();
                            Log.e(TAG, "error loading deliveries", e);
                        }
                    });
        }
        else {
            callMap();
        }
    }

    private void callMap() {
        MapFragment mMapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.statistics_relative_layout, mMapFragment);
        fragmentTransaction.commit();

        mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        ArrayList<LatLng> latLngs = new ArrayList<>();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (Delivery delivery : deliveries) {
            if (delivery.getLocation() != null) {
                LatLng latLng = new LatLng(delivery.getLocation().getLatitude(),
                        delivery.getLocation().getLongitude());
                latLngs.add(latLng);
                builder.include(latLng);
            }
        }

        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(latLngs)
                .build();

        googleMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));

        LatLngBounds bounds = builder.build();
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

//    private void showScatterChartOfDistanceVsTip() {
//        setActionBarTitle("Distance vs Tip");
//
//        relativeLayout.setPadding(8,8,8,8);
//
//        CombinedChart chart = new CombinedChart(this);
//
//        chart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT));
//
//        chart.getLegend().setEnabled(false);
//        chart.getAxisRight().setEnabled(false);
//        chart.setDescription(null);
//        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
//        chart.getAxisLeft().setDrawGridLines(false);
//        chart.getXAxis().setDrawGridLines(false);
//
//        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
//                CombinedChart.DrawOrder.SCATTER, CombinedChart.DrawOrder.LINE
//        });
//
//        relativeLayout.addView(chart);
//
//        List<Entry> scatterEntries = new ArrayList<>();
//        SimpleRegression simpleRegression = new SimpleRegression(true);
//
//        int minDistance = deliveries.get(0).getCost();
//        int maxDistance = deliveries.get(0).getCost();
//
//        Collections.sort(deliveries, new Comparator<Delivery>() {
//            @Override
//            public int compare(Delivery d1, Delivery d2) {
//                return d1.getPizzaDistance() - d2.getPizzaDistance();
//            }
//        });
//
//        for (Delivery delivery : deliveries) {
//            if (delivery.getPizzaDistance() > 0) {
//                scatterEntries.add(new Entry(delivery.getPizzaDistance(), delivery.getTip()));
//                simpleRegression.addData(delivery.getPizzaDistance(), delivery.getTip());
//
//                if (delivery.getPizzaDistance() > maxDistance)
//                    maxDistance = delivery.getPizzaDistance();
//                if (delivery.getPizzaDistance() < minDistance)
//                    minDistance = delivery.getPizzaDistance();
//            }
//        }
//
//        Log.d(TAG, "slope = " + simpleRegression.getSlope());
//        Log.d(TAG, "intercept = " + simpleRegression.getIntercept());
//
//        List<Entry> lineEntries = new ArrayList<>();
//        lineEntries.add(new Entry(minDistance, (float) simpleRegression.predict(minDistance)));
//        lineEntries.add(new Entry(maxDistance, (float) simpleRegression.predict(maxDistance)));
//
//        ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "Deliveries");
//        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
//        scatterDataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
//        scatterDataSet.setScatterShapeSize(12f);
//        ScatterData scatterData = new ScatterData(scatterDataSet);
//
//        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Linear Regression");
//        lineDataSet.setDrawCircles(false);
//        lineDataSet.setDrawValues(false);
//        lineDataSet.setLineWidth(1.8f);
//        lineDataSet.setColor(ColorTemplate.LIBERTY_COLORS[2]);
//
//        LineData lineData = new LineData(lineDataSet);
//        lineData.setHighlightEnabled(false);
//
//        CombinedData combinedData = new CombinedData();
//        combinedData.setData(scatterData);
//        combinedData.setData(lineData);
//
//        chart.setData(combinedData);
//        chart.invalidate();
//    }

//    private void showScatterChartOfCostVsTip() {
//        setActionBarTitle("Cost vs Tip");
//
//        relativeLayout.setPadding(8,8,8,8);
//
//        CombinedChart chart = new CombinedChart(this);
//        chart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT));
//
//        chart.getLegend().setEnabled(false);
//        chart.getAxisRight().setEnabled(false);
//        chart.setDescription(null);
//        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
//        chart.getAxisLeft().setDrawGridLines(false);
//        chart.getXAxis().setDrawGridLines(false);
//
//        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
//                CombinedChart.DrawOrder.SCATTER, CombinedChart.DrawOrder.LINE
//        });
//
//        relativeLayout.addView(chart);
//
//        List<Entry> scatterEntries = new ArrayList<>();
//        SimpleRegression simpleRegression = new SimpleRegression(true);
//
//        Collections.sort(deliveries, new Comparator<Delivery>() {
//            @Override
//            public int compare(Delivery d1, Delivery d2) {
//                return d1.getCost() - d2.getCost();
//            }
//        });
//
//        int minCost = deliveries.get(0).getCost();
//        int maxCost = deliveries.get(0).getCost();
//
//        for (Delivery delivery : deliveries) {
//            if (delivery.getCost() > 0) {
//                scatterEntries.add(new Entry(delivery.getCost(), delivery.getTip()));
//                simpleRegression.addData(delivery.getCost(), delivery.getTip());
//            }
//            if (delivery.getCost() > maxCost)
//                maxCost = delivery.getCost();
//            if (delivery.getCost() < minCost)
//                minCost = delivery.getCost();
//        }
//
//        Log.d(TAG, "slope = " + simpleRegression.getSlope());
//        Log.d(TAG, "intercept = " + simpleRegression.getIntercept());
//
//        List<Entry> lineEntries = new ArrayList<>();
//        lineEntries.add(new Entry(minCost, (float) simpleRegression.predict(minCost)));
//        lineEntries.add(new Entry(maxCost, (float) simpleRegression.predict(maxCost)));
//
//        ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "Deliveries");
//        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
//        scatterDataSet.setColor(ColorTemplate.COLORFUL_COLORS[1]);
//        scatterDataSet.setScatterShapeSize(12f);
//        ScatterData scatterData = new ScatterData(scatterDataSet);
//
//        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Linear Regression");
//        lineDataSet.setDrawCircles(false);
//        lineDataSet.setDrawValues(false);
//        lineDataSet.setLineWidth(1.8f);
//        lineDataSet.setColor(ColorTemplate.LIBERTY_COLORS[2]);
//
//        LineData lineData = new LineData(lineDataSet);
//        lineData.setHighlightEnabled(false);
//
//        CombinedData combinedData = new CombinedData();
//        combinedData.setData(scatterData);
//        combinedData.setData(lineData);
//
//        chart.setData(combinedData);
//        chart.invalidate();
//    }

    private void showBarChartOfDayVsTipPerHour() {
        setActionBarTitle(getString(R.string.action_statistics_day_vs_tip_per_hour));

        relativeLayout.setPadding(8,8,8,8);

        BarChart chart = new BarChart(this);
        BarDataSet barDataSet;

        relativeLayout.addView(chart);

        chart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        chart.getAxisLeft().setSpaceTop(30f);
        chart.getAxisLeft().setSpaceBottom(30f);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDescription(null);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return daysString[(int) value];
            }
        };

        chart.getXAxis().setValueFormatter(formatter);

        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            float averageTipPerHour =
                    ((float) weekData.getDayData(i).getTips() / (float) weekData.getDayData(i).getShiftsNum()) /
                            ((float) weekData.getDayData(i).getMinutes() / 60 / (float) weekData.getDayData(i).getShiftsNum());
            entries.add(new BarEntry(i, averageTipPerHour));
        }

        barDataSet = new BarDataSet(entries, "Average Tip");
        barDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        BarData data = new BarData(barDataSet);
        chart.setData(data);
        chart.setFitBars(true);
        chart.invalidate();
    }

    private void showBarChartOfDayVsAverageTip() {
        setActionBarTitle(getString(R.string.action_statistics_day_vs_average_tip));

        relativeLayout.setPadding(8,8,8,8);

        BarChart chart = new BarChart(this);
        BarDataSet barDataSet;

        relativeLayout.addView(chart);

        chart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        chart.getAxisLeft().setSpaceTop(30f);
        chart.getAxisLeft().setSpaceBottom(30f);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDescription(null);

        IAxisValueFormatter formatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return daysString[(int) value];
            }
        };

        chart.getXAxis().setValueFormatter(formatter);

        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            float averageTip = (float) weekData.getDayData(i).getTips() / (float) weekData.getDayData(i).getDeliveriesNum();
            entries.add(new BarEntry(i, averageTip));
        }

        barDataSet = new BarDataSet(entries, "Average Tip");
        barDataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);

        BarData data = new BarData(barDataSet);
        chart.setData(data);
        chart.setFitBars(true);
        chart.invalidate();
    }

    private void showBarChartChoseDay(final ArrayList<BarData> barData, String title) {
        setActionBarTitle(title);

        relativeLayout.setPadding(8,8,8,8);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final BarChart barChart = new BarChart(this);
        Spinner spinner = new Spinner(this);

        ArrayList<String> spinnerArray = new ArrayList<>(Arrays.asList(daysString));

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, spinnerArray);

        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                barChart.setData(barData.get(position));
                barChart.invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        linearLayout.addView(spinner);

        RelativeLayout relativeLayout1 = new RelativeLayout(this);
        relativeLayout1.addView(barChart);
        linearLayout.addView(relativeLayout1);

        relativeLayout.addView(linearLayout);

        barChart.setLayoutParams(new RelativeLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getLegend().setEnabled(false);
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
        barChart.setData(barData.get(0));
        barChart.setFitBars(true);
        barChart.invalidate();
    }

    private void showBarChartOfHourVsDeliveries() {
        ArrayList<BarData> barData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            barData.add(weekData.toNumData(i));
        }
        showBarChartChoseDay(barData, getString(R.string.action_statistics_day_vs_deliveries_per_hour));
    }

    private void showBarChartOfHourVsTip() {
        ArrayList<BarData> barData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            barData.add(weekData.toTipsData(i));
        }
        showBarChartChoseDay(barData, getString(R.string.action_statistics_day_vs_tips_per_hour));
    }
}
