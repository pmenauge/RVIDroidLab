package com.pmenauge.rvi.rvidroidcar;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.pmenauge.rvi.rvidroidlib.RviManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import static android.content.Context.LOCATION_SERVICE;
import static com.pmenauge.rvi.rvidroidcar.VehicleConfig.LOCATION_REFRESH_DISTANCE;
import static com.pmenauge.rvi.rvidroidcar.VehicleConfig.LOCATION_REFRESH_TIME;

/**
 * Created by pmenauge on 03/05/17.
 */

public class VehicleReportingManager {
    private static final String TAG = "VehicleReportingManager";

    private static final VehicleReportingManager ourInstance = new VehicleReportingManager();

    public static VehicleReportingManager getInstance() {
        return ourInstance;
    }

    private VehicleReportingManager() {
    }

    private HashSet<String> mSupportedChannels;
    private HashSet<String> mActiveChannels;

    private VehicleReportingControlImpl mVehicleReportingControlImpl;

    private VehicleReportingThread mReportingThread;
    private int mReportingPeriodInMS;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private SimpleDateFormat mTimestampFormatter;

    private int mHasGpsPermission = PackageManager.PERMISSION_DENIED;
    //private ArrayList mPositionsStillToReport = new ArrayList();
    private LocationManager mLocationManager = null;
    private Location mLastPosition = null;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            if (isBetterLocation(location, mLastPosition)) {
                Log.d(TAG, "onLocationChanged() got new position better than previous one");
                mLastPosition = location;
            }
            else {
                Log.d(TAG, "onLocationChanged() got new position but same as the one previously reported, ignore it");
            }
        }
        public void onProviderDisabled(final String provider) {
            Log.d(TAG, "onProviderDisabled() provider="+provider);

        }
        public void onProviderEnabled(final String provider) {
            Log.d(TAG, "onProviderEnabled() provider="+provider);

        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged() provider="+provider+", status="+status+", extras="+extras);

        }
    };


    public void init(Activity activity) {
        mTimestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        mReportingThread = new VehicleReportingThread("VehicleReporting");

        mSupportedChannels = new HashSet<String>();
        mActiveChannels = new HashSet<String>();

        // Define supported channels
        mSupportedChannels.add(VehicleReportingControl.CHANNEL_VEHICLE_LOCATION);
        mSupportedChannels.add(VehicleReportingControl.CHANNEL_VEHICLE_SPEED);
        mSupportedChannels.add(VehicleReportingControl.CHANNEL_ODOMETER);

        mLocationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
        mHasGpsPermission = activity.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (mHasGpsPermission == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME,
                    LOCATION_REFRESH_DISTANCE, mLocationListener);
        }
        else {
            Log.e(TAG, "onCreate() Permission ACCESS_FINE_LOCATION has not been granted, cannot access to GPS");
        }
        setReportingPeriod(VehicleConfig.getInstance().DEFAULT_REPORTING_PERIOD_IN_MS);

        mReportingThread.start();

        mVehicleReportingControlImpl = new VehicleReportingControlImpl();
    }


    void subscribeChannels(String [] channels) {
        Log.d(TAG, "subscribeChannels(): subscribe to "+channels.length+" channels");
        for (int i = 0 ; i < channels.length ; i++) {
            mActiveChannels.add(channels[i]);
        }
        mReportingThread.resumeThread();
    }

    void unsubscribeChannels(String [] channels) {
        Log.d(TAG,"unsubscribeChannels(): unsubscribe to "+channels.length+" channels");
        for (int i = 0 ; i < channels.length ; i++) {
            mActiveChannels.remove(channels[i]);
        }
    }

    boolean isVehicleReportingOn() { return (mActiveChannels.size() > 0); }

    int getReportingPeriod() { return mReportingPeriodInMS; }

    void setReportingPeriod(int reportingPeriodInMS) {
        mReportingPeriodInMS = reportingPeriodInMS;
    }

    void registerRviServices() throws Exception {
        Log.d(TAG, "registerRviServices(): Register service " + VehicleConfig.getInstance().VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME);
        RviManager.getInstance().rviRegisterService(VehicleConfig.getInstance().VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME, mVehicleReportingControlImpl, VehicleReportingControl.class);
        Log.d(TAG, "registerRviServices(): Register service " + VehicleConfig.getInstance().VEHICLE_REPORTING_UNSUBSCRIBE_SERVICE_NAME);
        RviManager.getInstance().rviRegisterService(VehicleConfig.getInstance().VEHICLE_REPORTING_UNSUBSCRIBE_SERVICE_NAME, mVehicleReportingControlImpl, VehicleReportingControl.class);
    }

    void unregisterRviServices() throws Exception {
        Log.d(TAG, "unregisterRviServices(): Unregister service " + VehicleConfig.getInstance().VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME);
        RviManager.getInstance().rviUnregisterService(VehicleConfig.getInstance().VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME);
        Log.d(TAG, "unregisterRviServices(): Unregister service " + VehicleConfig.getInstance().VEHICLE_REPORTING_UNSUBSCRIBE_SERVICE_NAME);
        RviManager.getInstance().rviUnregisterService(VehicleConfig.getInstance().VEHICLE_REPORTING_UNSUBSCRIBE_SERVICE_NAME);
    }

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    class VehicleReportingThread extends Thread {

        public VehicleReportingThread(String name) {
            super(name);
        }

        public void run() {
            while (true) {
                try {
                    if (mActiveChannels.size() > 0) {
                        Log.d(TAG, "VehicleReportingThread(): Send report to cloud for "+mActiveChannels.size()+" channels");
                        Iterator<String> activechannelIterator = mActiveChannels.iterator();
                        int index = 0;
                        ArrayList<HashMap> dataArray = new ArrayList<HashMap>();
                        while (activechannelIterator.hasNext()) {
                            String channel = activechannelIterator.next();
                            if (channel.equals(VehicleReportingControl.CHANNEL_VEHICLE_LOCATION)) {
                                if (mLastPosition == null) {
                                    Log.d(TAG, "VehicleReportingThread(): Current position not available, cannot report location");
                                }
                                else {
                                    dataArray.add(index, new HashMap());
                                    dataArray.get(index).put("channel", "location");
                                    HashMap valueMap = new HashMap();
                                    valueMap.put("lat", String.valueOf(mLastPosition.getLatitude()));
                                    valueMap.put("lon", String.valueOf(mLastPosition.getLongitude()));
                                    valueMap.put("alt", String.valueOf(mLastPosition.getAltitude()));
                                    dataArray.get(index).put("value", valueMap);
                                    index++;
                                }
                            }
                            else if (channel.equals(VehicleReportingControl.CHANNEL_VEHICLE_SPEED)) {
                                if (mLastPosition == null) {
                                    Log.d(TAG, "VehicleReportingThread(): Current position not available, cannot report speed");
                                }
                                else {
                                    dataArray.add(index, new HashMap());
                                    dataArray.get(index).put("channel", "speed");
                                    dataArray.get(index).put("value", String.valueOf(mLastPosition.getSpeed()));
                                    index++;
                                }
                            }
                            else if (channel.equals(VehicleReportingControl.CHANNEL_VEHICLE_SPEED)) {
                                dataArray.add(index, new HashMap());
                                dataArray.get(index).put("channel", "odometer");
                                // TODO: not yet implemented, send dummy value to keep compatibility with current rvi_backend implementation
                                dataArray.get(index).put("value", "5");
                                index++;
                            }
                            else {
                                Log.d(TAG, "VehicleReportingThread(): channel "+channel+" is not yet implemented");
                            }
                        }
                        try {
                            HashMap params = new HashMap();
                            String nowAsTimestamp = mTimestampFormatter.format(new Date());
                            params.put("timestamp", nowAsTimestamp);
                            params.put("vin", VehicleConfig.getInstance().VIN);
                            params.put("data", dataArray);

                            RviManager.getInstance().rviInvoke(VehicleConfig.getInstance().VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME, VehicleConfig.getInstance().REMOTE_VEHICLE_REPORTING_SERVICE_NAME, params);
                        } catch (Exception eRPC) {
                            Log.e(TAG, "VehicleReportingThread(): " + eRPC);
                        }
                        Log.d(TAG, "VehicleReportingThread(): Suspend for " + mReportingPeriodInMS + " milliseconds");
                        sleep(mReportingPeriodInMS);
                    } else {
                        // Wait for start request from UI button
                        Log.d(TAG, "VehicleReportingThread(): Go to sleep");
                        synchronized (this) {
                            while (mActiveChannels.size() == 0) {
                                wait();
                            }
                        }
                        Log.d(TAG, "VehicleReportingThread(): Resumed from sleep");
                    }
                }
                catch (Exception e) {
                    Log.d(TAG, "VehicleReportingThread(): Thread interrupted: "+e);
                }
            }
        }

        synchronized void resumeThread() {
            notify();
        }
    }
}
