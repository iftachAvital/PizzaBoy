package com.example.iftach.pizzaboy;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
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
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class ShiftActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private Shift shift;

    private TextView textTip;
    private TextView textCost;
    private TextView textDeliveriesNumber;
    private TextView textAverageTip;

    private int command;

    private int tips;
    private int costs;
    private int totalDeliveries;
    private float averageTip;

    private boolean adapterLongClickItemFlag;
    private boolean boundFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapterLongClickItemFlag = false;
        command = getIntent().getIntExtra(Constants.REQUEST_CODE, -1);
        shift = getIntent().getParcelableExtra(Constants.SHIFT_EXTRA);

        updateResult();
        initUI();
        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shift, menu);
        menu.findItem(R.id.action_shift_end_shift).setVisible(command != Constants.RC_SHOW_SHIFT);
        menu.findItem(R.id.action_shift_new_delivery).setVisible(command != Constants.RC_SHOW_SHIFT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_shift_new_delivery:
                startNewDelivery();
                return true;
            case android.R.id.home:
                Log.d(TAG, "Home pressed");
                finish();
                return true;
            case R.id.action_shift_settings:
                return true;
            case R.id.action_shift_end_shift:
                saveShift();
                return true;
            case R.id.action_shift_map:
                showDeliveriesOnMap();
                return true;
            case R.id.action_shift_statistics:
                startShiftStatistics();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RC_NEW_DELIVERY) {
            if (resultCode == RESULT_OK) {
                shift = data.getParcelableExtra(Constants.SHIFT_EXTRA);
                updateResult();
                updateUI();
            }
        }
    }

    private void updateResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.SHIFT_EXTRA, shift);
        setResult(RESULT_CANCELED, resultIntent);
    }

    private void startShiftStatistics() {
        Intent intent = new Intent(this, ShiftStatisticsActivity.class);
        intent.putExtra(Constants.SHIFT_EXTRA, shift);
        startActivity(intent);
    }

    private void startNewDelivery() {
        Intent intent = new Intent(this, NewDeliveryActivity.class);
        intent.putExtra(Constants.SHIFT_EXTRA, shift);
        startActivityForResult(intent, Constants.RC_NEW_DELIVERY);
    }

    private void showDeliveriesOnMap() {
        showDeliveriesOnMap(shift.getDeliveries());
    }

    private void showDeliveryOnMap(Delivery delivery) {
        ArrayList<Delivery> deliveries = new ArrayList<>(1);
        deliveries.add(delivery);
        showDeliveriesOnMap(deliveries);
    }

    private void showDeliveriesOnMap(ArrayList<Delivery> deliveries) {
        Intent intent = new Intent(ShiftActivity.this, MapsActivity.class);
        intent.putExtra(Constants.MAP_COMMAND, Constants.RC_SHOW_DELIVERIES);

        ArrayList<MarkerOptions> markerOptions = new ArrayList<>(deliveries.size());

        for (Delivery delivery : deliveries) {
            MarkerOptions marker = new MarkerOptions();
            marker.position(new LatLng(delivery.getLocation().getLatitude(),
                    delivery.getLocation().getLongitude()));
            String address = delivery.getAddress() + ", " + delivery.getCity();
            marker.title(address);
            marker.snippet(String.valueOf(delivery.getTip()));
            markerOptions.add(marker);
        }

        intent.putExtra(Constants.MARKERS_LIST_EXTRA, markerOptions);
        startActivity(intent);
    }

    private class DeliveryAdapter extends ArrayAdapter<Delivery> {
        DeliveryAdapter(Context context, ArrayList<Delivery> deliveries) {
            super(context, R.layout.delivery_item, deliveries);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Delivery delivery = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.delivery_item, parent, false);
            }

            if (delivery != null) {

                TextView time = convertView.findViewById(R.id.delivery_time);
                TextView street = convertView.findViewById(R.id.delivery_street);
                TextView city = convertView.findViewById(R.id.delivery_city);
                TextView cost = convertView.findViewById(R.id.delivery_cost);
                TextView tip = convertView.findViewById(R.id.delivery_tip);

                Date deliveryDate = new Date(delivery.getArrivalTime());
                String deliveryTime = new SimpleDateFormat("HH:mm", Constants.EN_LOCAL).format(deliveryDate);

                time.setText(deliveryTime);
                street.setText(delivery.getAddress());
                city.setText(delivery.getCity());

                if (delivery.getCost() != 0) {
                    cost.setText(String.valueOf(delivery.getCost()));
                } else {
                    cost.setText(getString(R.string.credit));
                }

                tip.setText(String.valueOf(delivery.getTip()));
            }

            return convertView;
        }
    }

    private void saveShift() {
        int totalDistance = 0;
        int totalDuration = 0;

        for (Delivery delivery : shift.getDeliveries()) {
            totalDistance += delivery.getPizzaDistance();
            totalDuration += delivery.getPizzaDuration();
        }

        shift.setShiftEnd(System.currentTimeMillis());
        shift.setTotalCost(costs);
        shift.setTotalTips(tips);
        shift.setTotalPizzaDistance(totalDistance);

        if (totalDeliveries > 0) {
            shift.setAverageTip(averageTip);
            shift.setAveragePizzaDistance((float) totalDistance / (float) totalDeliveries);
            shift.setAveragePizzaDuration((float) totalDuration / (float) totalDeliveries);
        }

        FirebaseFirestore.getInstance().collection(Constants.SHIFTS_COLLECTION)
                .add(shift)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference dr) {
                        Log.d(TAG, "success save shift id: " + dr.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "error save shift");
                        e.printStackTrace();
                    }
                });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ShiftActivity.this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.SHIFT, null);
        editor.apply();

        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.SHIFT_EXTRA, (Parcelable) null);
        setResult(RESULT_OK);
        finish();
    }

    private void deleteDelivery(final Delivery delivery) {
        shift.removeDelivery(delivery);
        shift.saveToPref(this);
        updateResult();
        updateUI();
    }

    private void updateUI() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(shift.getShiftStart());
        String title = new SimpleDateFormat("dd/MM/yyyy", Constants.EN_LOCAL).format(calendar.getTime());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(title);

        ArrayList<Delivery> deliveries = shift.getDeliveries();

        tips = 0;
        costs = 0;

        totalDeliveries = deliveries.size();

        for (int i=0; i < deliveries.size(); i++) {
            tips += deliveries.get(i).getTip();
            costs += deliveries.get(i).getCost();
        }

        if (totalDeliveries > 0) {
            averageTip = (float)tips / (float)totalDeliveries;
        }
        else {
            averageTip = 0;
        }

        textCost.setText(String.valueOf(costs));
        textTip.setText(String.valueOf(tips));
        textDeliveriesNumber.setText(String.valueOf(deliveries.size()));
        textAverageTip.setText(String.valueOf(String.format(new Locale("en"), "%.1f", averageTip)));

        Collections.sort(deliveries, Collections.<Delivery>reverseOrder());

        DeliveryAdapter deliveryAdapter = new DeliveryAdapter(getApplicationContext(), deliveries);
        ListView listView = findViewById(R.id.delivery_list_view);
        listView.setAdapter(deliveryAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "OnItemClickListener: adapterLongClickItemFlag=" + adapterLongClickItemFlag
                        + " " + i + " " + l);

                if (!adapterLongClickItemFlag) {
                    showDeliveryOnMap((Delivery) adapterView.getItemAtPosition(i));
                }
                adapterLongClickItemFlag = false;
            }
        });

        if (command == Constants.RC_IN_SHIFT) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    Log.d(TAG, "OnItemLongClickListener: " + i + " " + l);
                    adapterLongClickItemFlag = true;

                    final Delivery delivery = (Delivery) adapterView.getItemAtPosition(i);

                    new AlertDialog.Builder(ShiftActivity.this)
                            .setTitle("Delete Delivery")
                            .setMessage("Do you want to delete: " + delivery.getAddress() + "?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    deleteDelivery(delivery);
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
    }

    private void initUI() {
        textCost = findViewById(R.id.shift_cost);
        textTip = findViewById(R.id.shift_tip);
        textDeliveriesNumber = findViewById(R.id.shift_delivries_number);
        textAverageTip = findViewById(R.id.shift_average_tip);
    }
}
