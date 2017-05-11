package com.pmenauge.rvi.rvidroidcar;

import android.util.Log;

import com.googlecode.jsonrpc4j.JsonRpcParam;

/**
 * Created by pmenauge on 20/04/17.
 */

public class VehicleRemoteControlImpl implements VehicleRemoteControl {
    private static final String TAG = "VehicleRemoteControlImpl";

    public boolean isSupportedLockId(String lockId) {
        if (lockId != null)
            for (int i = 0; i < VehicleRemoteControl.ALL_LOCK_IDS.length ; i++)
                if (lockId.equals(VehicleRemoteControl.ALL_LOCK_IDS[i]))
                    return true;
        return false;
    }

    public void lock(@JsonRpcParam(value="action") String action, @JsonRpcParam(value="locks") String [] locks) {
        Log.d(TAG, "lock() callback: action="+action);
        if (locks==null) {
            Log.e(TAG, "lock() callback: no lock ids provided");
        }
        else {
            StringBuffer buf = new StringBuffer();
            for (int i=0; i < locks.length; i++) {
                buf.append(locks[i]);
                if (i != locks.length-1) buf.append(", ");
            }
            Log.d(TAG, "lock() callback: locks="+buf.toString());
        }
        // TODO UI & Model changes
        for (int i=0; i < locks.length; i++) {
            if (isSupportedLockId(locks[i])) {
                if (action.equals(VehicleRemoteControl.ACTION_LOCK)) {
                    VehicleRemoteControlManager.getInstance().lock(locks[i]);
                }
                else if (action.equals(VehicleRemoteControl.ACTION_UNLOCK)) {
                    VehicleRemoteControlManager.getInstance().unlock(locks[i]);
                }
                else {
                    Log.d(TAG, "lock() unrecognized action: "+action);
                }
            }
            else {
                Log.e(TAG, "lock() callback: unrecognized lock id "+locks[i]);
            }
        }
    }
}
