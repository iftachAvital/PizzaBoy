package com.example.iftach.pizzaboy;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class NewDeliveryActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private FusedLocationProviderClient flc;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private EditText editTotal;
    private EditText editTip;
    private EditText editCost;
    private EditText editAddress;
    private EditText editHouseNumber;
    private EditText editCity;
    private TextView textAccuracy;
    private TextView textArrivalTime;

    private Calendar arrivalCalendar;
    private Location lastLocation;

    private boolean updateAddressFlag;
    private int addressCounter;

    private ArrayList<Map<String, String>> addressMaps;
    private Shift shift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_delivery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        flc = LocationServices.getFusedLocationProviderClient(this);
        shift = getIntent().getParcelableExtra(Constants.SHIFT_EXTRA);

        setResult(RESULT_CANCELED);

        createLocationRequest();
        createLocationCallback();
        setupUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        arrivalCalendar = Calendar.getInstance();
        resetUI();
        startLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocation();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_delivery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG, "Home pressed");
                finish();
                return true;
            case R.id.action_refresh_location:
                startLocation();
                return true;
            case R.id.action_stop_location:
                rewindAddress();
                return true;
            case R.id.action_get_location_by_address:
                stopLocation();
                startPlaceAutoComplete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case Constants.RC_PLACE_AUTOCOMPLETE:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    Location locationFromAutocomplete = new Location("Autocomplete");
                    locationFromAutocomplete.setLatitude(place.getLatLng().latitude);
                    locationFromAutocomplete.setLongitude(place.getLatLng().longitude);
                    Log.d(TAG, "Getting address from locationFromAutocomplete");
                    updateAddressFlag = true;
                    handleNewLocation(locationFromAutocomplete);
                }
                else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);
                    Log.e(TAG, status.getStatusMessage());
                }
                break;
            case Constants.RC_LOCATION:
                startLocation();
                break;
        }
    }

    private void saveDelivery() {
        Delivery delivery = new Delivery();
        delivery.setArrivalTime(arrivalCalendar.getTimeInMillis());
        delivery.setAccuracy(lastLocation.getAccuracy());
        String addressStr = editAddress.getText().toString() + " " + editHouseNumber.getText().toString();
        delivery.setAddress(addressStr);
        delivery.setCity(editCity.getText().toString());
        delivery.setCost(Integer.parseInt(editCost.getText().toString()));
        delivery.setTip(Integer.parseInt(editTip.getText().toString()));
        delivery.setLocation(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));

        shift.addDelivery(delivery);
        shift.saveToPref(this);

        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.SHIFT_EXTRA, shift);
        setResult(RESULT_OK, resultIntent);

        Intent i = new Intent();
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    private void rewindAddress() {
        if (updateAddressFlag) {
            stopLocation();
        }
        else if (addressMaps.size() > 1 && addressCounter > 1) {
            Map<String, String> map = addressMaps.get(addressCounter-2);
            putAddressInUI(map);
            addressCounter--;
        }
        else {
            Toast.makeText(this, "no more previous addresses", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetUI() {
        textArrivalTime.setText(get24FormatTime(arrivalCalendar));
        editCost.setText("0");
        editTip.setText("");
        editTotal.setText("");
        editAddress.setText("");
        editCity.setText("");
        editHouseNumber.setText("");
        textAccuracy.setText("");
    }

    private void startPlaceAutoComplete() {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(this);
            startActivityForResult(intent, Constants.RC_PLACE_AUTOCOMPLETE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.w(TAG, "error starting place auto complete", e);
        }
    }

    private void createLocationRequest() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                handleNewLocation(locationResult.getLastLocation());
            }
        };
    }

    private void createLocationCallback() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void stopLocation() {
        updateAddressFlag = false;
        flc.removeLocationUpdates(locationCallback);
    }

    private void startLocation() {
        if (!updateAddressFlag) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.RC_LOCATION);
                return;
            }

            updateAddressFlag = true;
            addressCounter = 0;
            addressMaps = new ArrayList<>();

            flc.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        else {
            Log.d(TAG, "already started location");
        }
    }

    private void putAddressInUI(Map<String, String> map) {
        editAddress.setText(map.get("route"));
        editHouseNumber.setText(map.get("street_number"));
        editCity.setText(map.get("locality"));
    }

    private void handleNewLocation(Location location) {
        lastLocation = location;
        Log.d(TAG, "get new location: " + lastLocation.toString());
        if (updateAddressFlag) {
            new GetAddressTask(getString(R.string.google_maps_key), new GetAddressTask.Listener() {
                @Override
                public void onSuccess(Map<String, String> map) {
                    if (updateAddressFlag) {
                        putAddressInUI(map);
                        addressMaps.add(map);
                        addressCounter++;
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(NewDeliveryActivity.this, "Error getting address",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "error getting address");
                    e.printStackTrace();
                }
            }).execute(location);
        }
    }

    private String get24FormatTime(Calendar calendar) {
        return (new SimpleDateFormat("HH:mm", Constants.EN_LOCAL)).format(calendar.getTime());
    }

    private void getTimeByPicker() {
        int hour = arrivalCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = arrivalCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        arrivalCalendar.set(Calendar.MINUTE, minute);
                        arrivalCalendar.set(Calendar.HOUR_OF_DAY, hour);

                        Log.d(TAG, get24FormatTime(arrivalCalendar));

                        textArrivalTime.setText(get24FormatTime(arrivalCalendar));
                    }
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void setupUI() {
        editCost = findViewById(R.id.new_cost);
        editTip = findViewById(R.id.new_tip);
        editTotal = findViewById(R.id.new_total);
        editAddress = findViewById(R.id.new_address);
        editHouseNumber = findViewById(R.id.new_house_number);
        editCity = findViewById(R.id.new_city);
        textAccuracy = findViewById(R.id.new_accuracy);
        textArrivalTime = findViewById(R.id.new_arrival_time);

        textArrivalTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTimeByPicker();
            }
        });

        View.OnFocusChangeListener onFocusChangeListenerStopLocation = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    stopLocation();
            }
        };

        editAddress.setOnFocusChangeListener(onFocusChangeListenerStopLocation);
        editCity.setOnFocusChangeListener(onFocusChangeListenerStopLocation);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocation();
            }
        };

        editAddress.setOnClickListener(clickListener);
        editCity.setOnClickListener(clickListener);

        editHouseNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocation();
                editHouseNumber.setText("");
            }
        });

        editHouseNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    stopLocation();
                    editHouseNumber.setText("");
                }
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty() && !editCost.getText().toString().isEmpty() && !editTotal.getText().toString().isEmpty()) {
                    editTip.setText(String.valueOf(Integer.parseInt(editTotal.getText().toString()) - Integer.parseInt(editCost.getText().toString())));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ((EditText) v).setText("");
                }
            }
        };

        editCost.addTextChangedListener(watcher);
        editTotal.addTextChangedListener(watcher);

        editTip.setOnFocusChangeListener(focusChangeListener);
        editCost.setOnFocusChangeListener(focusChangeListener);
        editTotal.setOnFocusChangeListener(focusChangeListener);

        findViewById(R.id.new_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(editTip.getText()) && !TextUtils.isEmpty(editCost.getText()) &&
                        !TextUtils.isEmpty(editTotal.getText())) {
                    stopLocation();
                    saveDelivery();
                }
            }
        });
    }
}
