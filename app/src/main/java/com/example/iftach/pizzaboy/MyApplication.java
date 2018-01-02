package com.example.iftach.pizzaboy;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Created by iftach on 19/06/17.
 */

public class MyApplication extends Application{
    private final String TAG = getClass().getSimpleName();

    private FirebaseFirestore db;
    private WeekData weekData;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        loadWeekData();
    }

    private void loadWeekData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String uuid = firebaseUser.getUid();
            db.collection(Constants.USERS_COLLENTION)
                    .document(uuid)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot snapshot) {
                            try {
                                weekData = new WeekData(snapshot);
                                weekData.saveToPref(MyApplication.this);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    public WeekData getWeekData() {
        return weekData;
    }
}
