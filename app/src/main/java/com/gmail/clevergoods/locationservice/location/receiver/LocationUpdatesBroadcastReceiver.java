package com.gmail.clevergoods.locationservice.location.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.msk.socmoncore.utils.WifiUtils;

import java.util.HashMap;
import java.util.List;


public class LocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    static final String LOG_TAG = "LocationUpdatesBroadcastReceiver";

    static public String ACTION_PROCESS_UPDATES = "com.gmail.clevergoods.LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES";
    static public String ACTION_JUST_UPDATE = "com.gmail.clevergoods.LocationUpdatesBroadcastReceiver.ACTION_JUST_UPDATE";
    static public String LOG_LOCATION_SERVICE = "com.gmail.clevergoods.LocationUpdatesBroadcastReceiver.LOG_LOCATION_SERVICE";
    static public String HIGH_ACC_ACTION_PROCESS_UPDATES = "com.gmail.clevergoods.LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES";
    private Context context;

    @Override
    public void onReceive(final Context mContext, final Intent intent) {
        context = mContext;
        processLocation(intent);
    }

    private void processLocation(final Intent intent) {
        try {
            if (intent != null) {
                final String action = intent.getAction();
                boolean sendImmediately = false;
                if(intent.hasExtra("com.gmail.clevergoods.SEND_IMMEDIATELY")){
                    sendImmediately = intent.getBooleanExtra("com.gmail.clevergoods.SEND_IMMEDIATELY", false);
                }
                if (ACTION_PROCESS_UPDATES.equals(action)) {
                    int priority = intent.getIntExtra("priority", PRIORITY_BALANCED_POWER_ACCURACY);
                    LocationResult result = LocationResult.extractResult(intent);
                    if (result != null) {
                        List<Location> locations = result.getLocations();
                        WLog.d(LOG_TAG,"ProcessLocation Получил Locations size="+locations.size());
                        HashMap<Long, Location> mLocations = new HashMap<>();
                        for (Location loc : locations) {
                            if(loc != null) {
                                Bundle extras = loc.getExtras();
                                if (extras == null) extras = new Bundle();
                                extras.putInt("priority", priority);
                                mLocations.put(loc.getTime(), loc);
                            }
                        }
                        for (Long key : mLocations.keySet()) {
                            Location loc = mLocations.get(key);
                            sendLocation(loc, sendImmediately);
                        }
                    }
                }
                if(LOG_LOCATION_SERVICE.equals(action)){
                    String message = intent.getStringExtra("message");
                    WLog.d(LOG_TAG, message);
                }
            }
        } catch (Exception e) {
            WLog.e(LOG_TAG, e);
        }
    }

    private void sendLocation(Location loc, boolean sendImmediately) {
        WLLocation wlLocation = new WLLocation(loc);
        wlLocation.setSendImmediately(sendImmediately);
        new WifiUtils(context).wifiScan(wlLocation, true);
    }
}
