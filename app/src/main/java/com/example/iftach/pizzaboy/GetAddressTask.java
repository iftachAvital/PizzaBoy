package com.example.iftach.pizzaboy;

import android.location.Location;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iftach on 28/12/17.
 */

class GetAddressTask extends AsyncTask<Location, Integer,  Map<String, String>> {

    private final String TAG = getClass().getSimpleName();
    private final String apiKey;

    private Listener listener = null;
    private Exception exception = null;

    GetAddressTask(String apiKey, Listener listener) {
        this.apiKey = apiKey;
        this.listener = listener;
    }

    @Override
    protected  Map<String, String> doInBackground(Location... locations) {
        Location location = locations[0];
        HttpURLConnection urlConnection = null;
        Map<String, String> map = new HashMap<>();

        map.put("street_number", "");
        map.put("route", "");
        map.put("locality", "");

        try {
            URL url = new URL(initURL(location));
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }

            JSONObject jsonObject = new JSONObject(stringBuilder.toString());

            if (jsonObject.getString("status").equals("OK")) {
                JSONArray results = jsonObject.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    JSONArray addressComponents = results.getJSONObject(i).getJSONArray("address_components");
                    for (int j = 0; j < addressComponents.length(); j++) {
                        JSONArray types = addressComponents.getJSONObject(j).getJSONArray("types");
                        for (int k = 0; k < types.length(); k++) {
                            if (map.containsKey(types.get(k).toString())) {
                                map.put(types.get(k).toString(), addressComponents.getJSONObject(j).getString("long_name"));
                            }
                        }
                    }
                }

                Log.d(TAG, map.toString());
                return map;
            }

        } catch (IOException | JSONException e) {
            exception = e;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Map<String, String> map) {
        if (listener != null) {
            if (map != null) {
                listener.onSuccess(map);
            }
            else if (exception != null) {
                listener.onFailure(exception);
            }
        }

    }

    private String initURL(Location location) {
        return String.format(Constants.EN_LOCAL,
                "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "language=iw&result_type=street_address&latlng=%f,%f&key=%s",
                location.getLatitude(), location.getLongitude(), apiKey);
    }

    interface Listener {
        void onSuccess(Map<String, String> map);
        void onFailure(Exception e);
    }
}
