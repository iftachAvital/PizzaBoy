package com.example.iftach.pizzaboy;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String TAG = getClass().getSimpleName();

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Intent intent = getIntent();

        if (intent.getIntExtra(Constants.MAP_COMMAND, 0) == Constants.RC_SHOW_DELIVERIES) {
            ArrayList<MarkerOptions> markerOptions =
                    intent.getParcelableArrayListExtra(Constants.MARKERS_LIST_EXTRA);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (MarkerOptions markerOption : markerOptions) {
                mMap.addMarker(markerOption);
                builder.include(markerOption.getPosition());
            }

            if (markerOptions.size() == 1) {
                mMap.moveCamera(CameraUpdateFactory.newLatLng(markerOptions.get(0).getPosition()));
                mMap.moveCamera(CameraUpdateFactory.zoomTo((float) 16.5));
            }
            else {
                LatLngBounds bounds = builder.build();
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        }
    }
}
