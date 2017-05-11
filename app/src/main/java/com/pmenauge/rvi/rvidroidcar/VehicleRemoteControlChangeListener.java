package com.pmenauge.rvi.rvidroidcar;

/**
 * Created by pmenauge on 03/05/17.
 */

public interface VehicleRemoteControlChangeListener {
    public void vehicleLocked(String lockId);
    public void vehicleUnlocked(String lockId);
}
