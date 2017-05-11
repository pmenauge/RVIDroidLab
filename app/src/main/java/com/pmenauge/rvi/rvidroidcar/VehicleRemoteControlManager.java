package com.pmenauge.rvi.rvidroidcar;

import android.app.Activity;
import android.util.Log;

import com.pmenauge.rvi.rvidroidlib.RviManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pmenauge on 03/05/17.
 */

public class VehicleRemoteControlManager {
    private static final String TAG = "VehicleRemoteControlManager";

    private static final VehicleRemoteControlManager ourInstance = new VehicleRemoteControlManager();

    public static final String LOCKED_STATE = "LOCKED";
    public static final String UNLOCKED_STATE = "UNLOCKED";

    public static VehicleRemoteControlManager getInstance() {
        return ourInstance;
    }

    private VehicleRemoteControlImpl mVehicleRemoteControlCallback;
    private HashMap<String,String> mVehicleLockUnlockStates;
    private ArrayList mVehicleRemoteControlEventsListeners = new ArrayList();

    private VehicleRemoteControlManager() {
    }

    public void init(Activity activity) {
        mVehicleRemoteControlCallback = new VehicleRemoteControlImpl();
        mVehicleLockUnlockStates = new HashMap<String,String>();
        for (int i = 0; i < VehicleRemoteControl.ALL_LOCK_IDS.length ; i++) {
            mVehicleLockUnlockStates.put(VehicleRemoteControl.ALL_LOCK_IDS[i], VehicleConfig.getInstance().DEFAULT_VEHICLE_LOCK_UNLOCK_STATE);
        }
    }

    public String getVehicleLockUnlockState(String lockId) { return mVehicleLockUnlockStates.get(lockId); }

    public void lock(String lockId) {
        String lockState = mVehicleLockUnlockStates.get(lockId);
        if (lockState == null) {
            Log.d(TAG, "lock(): lock id "+lockId+" not found");
        }
        else if (lockState.equals(LOCKED_STATE)) {
            Log.d(TAG, "lock(): lock id "+lockId+" is already locked");
        }
        else {
            Log.d(TAG, "lock(): lock "+lockId+" and notify listeners");
            mVehicleLockUnlockStates.put(lockId, LOCKED_STATE);
            for (int i = 0; i < mVehicleRemoteControlEventsListeners.size() ; i++) {
                ((VehicleRemoteControlChangeListener) mVehicleRemoteControlEventsListeners.get(i)).vehicleLocked(lockId);
            }
        }
    }

    public void unlock(String lockId) {
        String lockState = mVehicleLockUnlockStates.get(lockId);
        if (lockState == null) {
            Log.d(TAG, "unlock(): lock id "+lockId+" not found");
        }
        else if (lockState.equals(UNLOCKED_STATE)) {
            Log.d(TAG, "unlock(): lock id "+lockId+" is already unlocked");
        }
        else {
            Log.d(TAG, "unlock(): unlock "+lockId+" and notify listeners");
            mVehicleLockUnlockStates.put(lockId, UNLOCKED_STATE);
            for (int i = 0; i < mVehicleRemoteControlEventsListeners.size() ; i++) {
                ((VehicleRemoteControlChangeListener) mVehicleRemoteControlEventsListeners.get(i)).vehicleUnlocked(lockId);
            }
        }
    }

    public HashMap<String, String> getAllLocksStates() {
        return (HashMap<String, String>) mVehicleLockUnlockStates.clone();
    }

    public void addChangeListener(VehicleRemoteControlChangeListener listener) {
        mVehicleRemoteControlEventsListeners.add(listener);
    }

    public void removeChangeListener(VehicleRemoteControlChangeListener listener) {
        mVehicleRemoteControlEventsListeners.remove(listener);
    }

    public void registerRviServices() throws Exception {
        Log.d(TAG, "registerRviServices(): Register service " + VehicleConfig.getInstance().VEHICLE_REMOTE_CONTROL_LOCK_UNLOCK_SERVICE_NAME);
        RviManager.getInstance().rviRegisterService(VehicleConfig.getInstance().VEHICLE_REMOTE_CONTROL_LOCK_UNLOCK_SERVICE_NAME, mVehicleRemoteControlCallback, VehicleRemoteControl.class);
    }

    public void unregisterRviServices() throws Exception {
        Log.d(TAG, "unregisterRviServices(): Unregister service " + VehicleConfig.getInstance().VEHICLE_REMOTE_CONTROL_LOCK_UNLOCK_SERVICE_NAME);
        RviManager.getInstance().rviUnregisterService(VehicleConfig.getInstance().VEHICLE_REMOTE_CONTROL_LOCK_UNLOCK_SERVICE_NAME);
    }


}
