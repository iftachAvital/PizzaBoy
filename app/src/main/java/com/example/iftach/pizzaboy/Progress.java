package com.example.iftach.pizzaboy;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by iftach on 20/11/17.
 */

public class Progress {
    private ProgressDialog progress;

    public Progress(Context context) {
        progress = new ProgressDialog(context);
    }

    public void showProgress(String message, boolean cancelable) {
        progress.setCancelable(cancelable);
        progress.setMessage(message);
        progress.show();
    }

    public void dismissProgress() {
        progress.dismiss();
    }
}
