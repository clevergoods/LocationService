package com.gmail.clevergoods.locationservice.location;

import android.content.Context;
import android.content.Intent;

import com.gmail.clevergoods.DBHelper;
import com.gmail.clevergoods.PrefHelper;
import com.gmail.clevergoods.Utils;
import com.gmail.clevergoods.WLog;
import com.gmail.clevergoods.locationservice.location.locationService.LocationService;

public class LocationServiceApi {
    private static String LOG_TAG = "LocationServiceApi";

    public static void stopLocationServiceAndClearBase(Context ctx) {
        stopLocationService(ctx);
        DBHelper.instance(ctx).clearLocationTable();
    }

    public static void stopLocationService(Context ctx) {
        if (Utils.Companion.isServiceRunning(LocationService.class, ctx)) {
            Intent stopIntent = new Intent(ctx, LocationService.class);
            ctx.stopService(stopIntent);
        }
    }

    public static void startLocationService(Context ctx) {
        if (!Utils.Companion.isServiceRunning(LocationService.class, ctx)) {
            startGeoService(ctx);
        }
    }

    private static void startGeoService(Context ctx) {
        if (Utils.Companion.isLocationServiceAllowed(ctx)) {
            DBHelper.instance(ctx).createLocationTable();
            Intent intent = new Intent(ctx, LocationService.class);
            intent.putExtra("trackInterval", PrefHelper.Companion.getTrackMode());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startService(intent);
            WLog.i(LOG_TAG, "START LocationService");
        }
    }

    public static int getTrackingMode() {
        return PrefHelper.Companion.getTrackMode();
    }

    public static void setTrackingMode(Context ctx, int trackInterval) {
        int current = PrefHelper.Companion.getTrackMode();
        if (current != trackInterval && Utils.Companion.isLocationServiceAllowed(ctx)) {
            PrefHelper.Companion.setTrackMode(trackInterval);
            startGeoService(ctx);
            WLog.i(LOG_TAG, "setTrackingMode  new trackInterval=" + trackInterval);
        }
    }
}
