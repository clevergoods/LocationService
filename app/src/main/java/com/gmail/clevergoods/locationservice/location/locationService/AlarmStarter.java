/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmail.clevergoods.locationservice.location.locationService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gmail.clevergoods.PrefHelper;
import com.gmail.clevergoods.WLog;
import com.gmail.clevergoods.locationservice.location.locationUtils.LocationUtils;

public class AlarmStarter extends BroadcastReceiver {


    private static final String LOG_TAG = "AlarmStarter";

    public static long restartInterval = 60;

    @Override
    public void onReceive(Context context, Intent intent) {

            WLog.d(LOG_TAG, "AlarmStarter onReceive do singleLocationRequest");
            LocationUtils.Companion.getInstance(context).singleLocationRequest();
        try {
            startLocationAlarm(context);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startLocationAlarm(Context ctx) {
        restartInterval = PrefHelper.Companion.getTrackMode();

        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent2 = new Intent(ctx, AlarmStarter.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, 1545, intent2, 0);
        alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + restartInterval * 1000, alarmIntent);
        WLog.d(LOG_TAG, "ALARM_SERVICE started");
    }

    public static void stopLocationAlarm(Context ctx) {
        Intent intent = new Intent(ctx, AlarmStarter.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx.getApplicationContext(), 1545, intent, 0);
        AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(pendingIntent);
        WLog.d(LOG_TAG, "ALARM_SERVICE stopped");
    }
}
