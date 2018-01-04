package com.example.iftach.pizzaboy;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MonthActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private final String TAG = getClass().getSimpleName();

    private Progress progress;
    private FirebaseFirestore db;
    private String userUid;
    private ArrayList<Shift> shifts;
    private Shift shift;

    private TextView textTotalTips;
    private TextView textAverageTip;
    private TextView textTotalHours;
    private TextView textAveragePerHour;

    private MenuItem startShiftItem;
    private MenuItem resumeShiftItem;

    private float averageTip;
    private float averagePerHour;
    private boolean adapterLongClickItemFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userUid = getIntent().getStringExtra(Constants.USER_UID_EXTRA);
        progress = new Progress(this);
        db = FirebaseFirestore.getInstance();
        adapterLongClickItemFlag = false;

        initUI();

        shift = Shift.loadFromPref(this);

        if (shift != null) {
            resumeShift();
        }
        else {
            loadShifts();
        }
    }

    @Override
    protected void onStop() {
        progress.dismissProgress();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);

        switch (requestCode) {
            case Constants.RC_IN_SHIFT:
                if (data != null) shift = data.getParcelableExtra(Constants.SHIFT_EXTRA);

                switch (resultCode) {
                    case RESULT_OK:
                        updateShiftItem();
                        loadShifts();
                        break;
                    case RESULT_CANCELED:
                        updateShiftItem();
                        if (shifts == null) loadShifts();
                }
                break;
            case Constants.RC_SHOW_SHIFT:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_month, menu);
        startShiftItem = menu.findItem(R.id.action_month_start_shift);
        resumeShiftItem = menu.findItem(R.id.action_month_resume_shift);
        updateShiftItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_month_sign_out:
                signOut();
                return true;
            case R.id.action_month_settings:
                return true;
            case R.id.action_month_change_date:
                getShiftsByDatePiker();
                return true;
            case R.id.action_month_start_shift:
                startShift();
                return true;
            case R.id.action_month_resume_shift:
                resumeShift();
                return true;
            case R.id.action_month_statistics:
                startStatisticsActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "sign out success");

                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MonthActivity.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(Constants.SHIFT, null);
                        editor.apply();

                        Toast.makeText(MonthActivity.this, "Sign Out", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MonthActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "sign out failed");
                        e.printStackTrace();

                        Toast.makeText(MonthActivity.this, "Error sign out", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadShifts() {
        Calendar calendar = Calendar.getInstance();
        loadShifts(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
    }

    private void loadShifts(final int year, final int month) {
        progress.showProgress("Loading Shifts", false);
        db.collection(Constants.SHIFTS_COLLECTION)
                .whereEqualTo("userUid", userUid)
                .whereGreaterThanOrEqualTo("shiftStart", getCalenderOfBeginningOfMonth(year, month).getTimeInMillis())
                .whereLessThanOrEqualTo("shiftStart", getCalenderOfEndOfMonth(year, month).getTimeInMillis())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        progress.dismissProgress();

                        shifts = new ArrayList<>();
                        for (DocumentSnapshot ds : documentSnapshots) {
                            Shift shift = ds.toObject(Shift.class);
                            shift.setId(ds.getId());
                            shifts.add(shift);
                        }

                        updateTitleByDate(year, month);
                        updateUI();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progress.dismissProgress();

                        Log.w(TAG, "get shifts failed");
                        e.printStackTrace();

                        Toast.makeText(MonthActivity.this,
                                "Error loading shifts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startShift() {
        shift = new Shift();
        shift.setShiftStart(System.currentTimeMillis());
        shift.setUserUid(userUid);
        shift.saveToPref(this);
        resumeShift();
    }

    private void resumeShift() {
        startShiftActivity(Constants.RC_IN_SHIFT, shift);
    }

    private void showShift(Shift shift) {
        startShiftActivity(Constants.RC_SHOW_SHIFT, shift);
    }

    private void startShiftActivity(int requestCode, Shift shift) {
        Intent intent = new Intent(MonthActivity.this, ShiftActivity.class);
        intent.putExtra(Constants.REQUEST_CODE, requestCode);
        intent.putExtra(Constants.SHIFT_EXTRA, shift);
        startActivityForResult(intent, requestCode);
    }

    private void startStatisticsActivity() {
        Intent intent = new Intent(this, StatisticsActivity.class);
        startActivity(intent);
    }

    private void getShiftsByDatePiker() {
        MonthYearPickerDialog pd = new MonthYearPickerDialog();
        pd.setListener(this);
        pd.show(getFragmentManager(), "MonthYearPickerDialog");
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
        Log.d(TAG, "onDateSet: " + year + " " + month + " " + day);
        loadShifts(year, month-1);
    }

    private void updateShiftItem() {
        if (startShiftItem != null && resumeShiftItem != null) {
            boolean inShift = shift != null;
            startShiftItem.setVisible(!inShift);
            resumeShiftItem.setVisible(inShift);
        }
    }

    private void setTitleMonth(Calendar calendar) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(new SimpleDateFormat("MMMM yyyy", new Locale("en")).
                    format(calendar.getTime()));
        }
    }

    private void deleteShift(final Shift shiftToDelete) {
        db.collection(Constants.SHIFTS_COLLECTION)
                .document(shiftToDelete.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "success delete shift id: " + shiftToDelete.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "failure delete shift id: " + shiftToDelete.getId());
                        e.printStackTrace();
                    }
                });

        loadShifts();
    }

    public static class MonthYearPickerDialog extends DialogFragment {

        private DatePickerDialog.OnDateSetListener listener;

        public void setListener(DatePickerDialog.OnDateSetListener listener) {
            this.listener = listener;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            Calendar cal = Calendar.getInstance();

            View dialog = inflater.inflate(R.layout.date_picker_dialog, null);
            final NumberPicker monthPicker = dialog.findViewById(R.id.picker_month);
            final NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

            monthPicker.setMinValue(1);
            monthPicker.setMaxValue(12);
            monthPicker.setValue(cal.get(Calendar.MONTH) + 1);

            int year = cal.get(Calendar.YEAR);
            yearPicker.setMinValue(2017);
            yearPicker.setMaxValue(year);
            yearPicker.setValue(year);

            builder.setView(dialog)
                    // Add action buttons
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            listener.onDateSet(null, yearPicker.getValue(), monthPicker.getValue(), 0);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MonthYearPickerDialog.this.getDialog().cancel();
                        }
                    });

            return builder.create();
        }
    }

    private class ShiftAdapter extends ArrayAdapter<Shift> {

        private float maxTip;
        private float minTip;
        private float minTipHour;
        private float maxTipHour;

        ShiftAdapter(Context context, ArrayList<Shift> shiftArrayList) {
            super(context, R.layout.shift_item, shiftArrayList);
            calculateVars(shiftArrayList);
        }

        private void calculateVars(ArrayList<Shift> shiftArrayList) {
            if (shiftArrayList.size() > 0) {
                minTip = shiftArrayList.get(0).getAverageTip();
                maxTip = shiftArrayList.get(0).getAverageTip();

                maxTipHour = shiftArrayList.get(0).calculateTipPerHour();
                minTipHour = shiftArrayList.get(0).calculateTipPerHour();

                for (Shift shift : shiftArrayList) {
                    if (shift.getAverageTip() > maxTip)
                        maxTip = shift.getAverageTip();
                    if (shift.getAverageTip() < minTip)
                        minTip = shift.getAverageTip();
                    if (shift.calculateTipPerHour() > maxTipHour)
                        maxTipHour = shift.calculateTipPerHour();
                    if (shift.calculateTipPerHour() < minTipHour)
                        minTipHour = shift.calculateTipPerHour();
                }
            }
        }

        private float biggestDifference(float a, float b, float center) {
            float aAbsDistance = Math.abs(a - center);
            float bAbsDistance = Math.abs(b - center);
            return Math.max(aAbsDistance, bAbsDistance);
        }

        private int handleColorTextByMinMax(float min, float max, float x, float average) {
            final double factor = 0.8;

            float maxDistance = biggestDifference(min, max, average);
            float maxFactor = average + maxDistance;
            float minFactor = average - maxDistance;

            float percent = (x - minFactor) / (maxFactor - minFactor);

            int r = (int) (factor * (1-percent) * (float)255);
            int g = (int) (factor * percent * (float)255);
            int b = 0;

            return Color.rgb(r,g,b);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Shift shift = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.shift_item, parent, false);
            }

            if (shift != null) {
                TextView textDayText = convertView.findViewById(R.id.shift_day_text);
                TextView textDayNumber = convertView.findViewById(R.id.shift_day_number);
                TextView textTip = convertView.findViewById(R.id.shift_tip);
                TextView textTipPerHour = convertView.findViewById(R.id.shift_tip_per_hour);
                TextView textCost = convertView.findViewById(R.id.shift_cost);
                TextView textAverageTip = convertView.findViewById(R.id.shift_average_tip);
                TextView textTime = convertView.findViewById(R.id.shift_time);

                final Calendar shiftStartCalendar = Calendar.getInstance();
                Calendar shiftEndCalendar = Calendar.getInstance();
                shiftStartCalendar.setTimeInMillis(shift.getShiftStart());
                shiftEndCalendar.setTimeInMillis(shift.getShiftEnd());
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE", new Locale("en"));

                textDayText.setText(dateFormat.format(shiftStartCalendar.getTime()));
                textDayNumber.setText(String.valueOf(shiftStartCalendar.get(Calendar.DAY_OF_MONTH)));

                int hours = (int) TimeUnit.MILLISECONDS.toHours(
                        shiftEndCalendar.getTimeInMillis() - shiftStartCalendar.getTimeInMillis());
                int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(
                        shiftEndCalendar.getTimeInMillis() - shiftStartCalendar.getTimeInMillis()) - hours * 60;

                textTipPerHour.setText(String.format(Constants.EN_LOCAL, "%.2f", shift.calculateTipPerHour()));

                String timeStr = hours + ":" + String.format(Constants.EN_LOCAL, "%02d", minutes);

                textTime.setText(timeStr);
                textCost.setText(String.valueOf(shift.getTotalCost()));
                textTip.setText(String.valueOf(shift.getTotalTips()));
                textAverageTip.setText(String.format(Constants.EN_LOCAL, "%.1f", shift.getAverageTip()));

                textAverageTip.setTextColor(handleColorTextByMinMax(
                        minTip, maxTip, shift.getAverageTip(), averageTip));
                textTipPerHour.setTextColor(handleColorTextByMinMax(
                        minTipHour, maxTipHour, shift.calculateTipPerHour(), averagePerHour));
            }
            return convertView;
        }
    }

    private Calendar getCalenderOfBeginningOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private Calendar getCalenderOfEndOfMonth(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar;
    }

    private void updateTitleByDate(int year, int month) {
        setTitleMonth(getCalenderOfBeginningOfMonth(year, month));
    }

    private void updateUI() {
        int totalShifts = 0;
        int totalTip = 0;
        float totalTipAverage = 0;
        long totalMillis = 0;

        averageTip = 0;
        this.averagePerHour = 0;

        if (shifts.size() > 0) {
            for (Shift shift : shifts) {
                totalShifts += 1;
                totalTip += shift.getTotalTips();
                totalTipAverage += shift.getAverageTip();
                totalMillis += shift.getShiftEnd() - shift.getShiftStart();
            }

            averageTip = totalTipAverage/(float)totalShifts;
            this.averagePerHour = (float) totalTip / ((float) totalMillis / 1000 / 60 / 60);
        }

        int totalHours = (int) TimeUnit.MILLISECONDS.toHours(totalMillis);
        int totalMinutes = (int) (TimeUnit.MILLISECONDS.toMinutes(totalMillis) - totalHours*60);

        String totalHourStr = totalHours + ":" + String.format(new Locale("en"), "%02d", totalMinutes);

        textTotalTips.setText(String.valueOf(totalTip));
        textAveragePerHour.setText(String.format(new Locale("en"), "%.2f", averagePerHour));
        textAverageTip.setText(String.format(new Locale("en"), "%.1f", averageTip));
        textTotalHours.setText(totalHourStr);

        Log.i(TAG, "averageTip tip hour: " + this.averagePerHour);

        Collections.sort(shifts);

        ShiftAdapter shiftAdapter = new ShiftAdapter(getApplicationContext(), shifts);
        ListView listView = findViewById(R.id.main_list_view);
        listView.setAdapter(shiftAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "OnItemClickListener: " + i + " " + l);
                if (!adapterLongClickItemFlag) {
                    Shift shift = (Shift) adapterView.getItemAtPosition(i);
                    showShift(shift);
                }
                adapterLongClickItemFlag = false;
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                Log.d(TAG, "OnItemLongClickListener: " + i + " " + l);
                adapterLongClickItemFlag = true;
                final Shift shift = (Shift) adapterView.getItemAtPosition(i);
                new AlertDialog.Builder(MonthActivity.this)
                        .setTitle("Delete Shift")
                        .setMessage("Do you want to delete: " + new SimpleDateFormat(
                                "dd/MM/yyyy", Constants.EN_LOCAL).format(new Date(shift.getShiftStart())) + "?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                deleteShift(shift);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                return false;
            }
        });
    }

    private void initUI() {
        textTotalTips = findViewById(R.id.main_total_tips);
        textAverageTip = findViewById(R.id.main_average_tip);
        textTotalHours = findViewById(R.id.main_total_hours);
        textAveragePerHour = findViewById(R.id.main_average_per_hour);
    }
}
