package com.example.iheaven.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.iheaven.util.NotificationHelper;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String body  = intent.getStringExtra("body");

        if (title != null && body != null) {
            NotificationHelper.showLocalNotification(
                    context, title, body);
        }
    }
}
