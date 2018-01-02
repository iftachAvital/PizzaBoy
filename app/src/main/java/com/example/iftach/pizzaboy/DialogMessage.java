package com.example.iftach.pizzaboy;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by iftach on 12/12/16.
 */
public class DialogMessage {
    private AlertDialog userDialog;
    private Context context;
    private AlertDialog.Builder builder;

    public DialogMessage(Context context) {
        this(context, null);
    }

    public DialogMessage(Context context, String title) {
        this(context, title, null);
    }

    public DialogMessage(Context context, String title, String message) {
        this(context, title, message, true);
    }

    public DialogMessage(Context context, String title, String message, boolean cancelable) {
        this(context, title, message, cancelable, null, null);
    }

    public DialogMessage(Context context, String title, String message, boolean cancelable,
                         String positiveButtonName, DialogInterface.OnClickListener positiveOnClickListener) {
        this(context, title, message, cancelable, positiveButtonName, positiveOnClickListener, null, null);
    }

    public DialogMessage(Context context, String title, String message, boolean cancelable,
                         String positiveButtonName, DialogInterface.OnClickListener positiveOnClickListener,
                         String negativeButtonName, DialogInterface.OnClickListener negativeOnClickListener) {
        this.context = context;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(cancelable);
        builder.setPositiveButton(positiveButtonName, positiveOnClickListener);
        builder.setNegativeButton(negativeButtonName, negativeOnClickListener);
    }

    public void show() {
        userDialog = builder.create();
        userDialog.show();
    }

    public void dismiss() {
        try { userDialog.dismiss(); } catch (Exception e) { }
    }
}
