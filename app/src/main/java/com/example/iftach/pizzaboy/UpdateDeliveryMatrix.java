package com.example.iftach.pizzaboy;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by iftach on 28/12/17.
 */

public class UpdateDeliveryMatrix extends AsyncTask<Delivery, Void, Delivery> {

    private final String TAG = getClass().getSimpleName();
    private final String apiKey;

    private Listener listener;

    UpdateDeliveryMatrix(Listener listener, String apiKey) {
        this.listener = listener;
        this.apiKey = apiKey;
    }

    @Override
    protected Delivery doInBackground(Delivery... deliveries) {
        Delivery delivery = deliveries[0];

        while (true) {
            try {
                URL url = new URL(initURL(delivery));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append('\n');
                }

                JSONObject jsonObject = new JSONObject(stringBuilder.toString());

                if (jsonObject.getString("status").equals("OK")) {
                    JSONArray rows = jsonObject.getJSONArray("rows");

                    JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");

                    int distance = elements.getJSONObject(0).getJSONObject("distance").getInt("value");
                    int duration = elements.getJSONObject(0).getJSONObject("duration").getInt("value");

                    Log.d(TAG, "distance=" + distance + " duration=" + duration);

                    delivery.setPizzaDistance(distance);
                    delivery.setPizzaDuration(duration);

                    return delivery;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e1) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPostExecute(Delivery delivery) {
        if (listener != null) {
            if (delivery != null) listener.onSuccess(delivery);
        }
    }

    private String initURL(Delivery delivery) {
        return String.format(
                Constants.EN_LOCAL,
                "https://maps.googleapis.com/maps/api/distancematrix/json?" +
                        "origins=%f,%f&destinations=%f,%f&mode=driving&language=en&key=%s",
                Constants.PIZZA_LATITUDE, Constants.PIZZA_LONGITUDE,
                delivery.getLocation().getLatitude(), delivery.getLocation().getLongitude(),
                apiKey);
    }

    interface Listener {
        void onSuccess(Delivery delivery);
    }
}
