package com.pmenauge.rvi.rvidroidcar;

/**
 * Created by pmenauge on 03/05/17.
 */

public class VehicleConfig {
    private static final VehicleConfig ourInstance = new VehicleConfig();

    public static VehicleConfig getInstance() {
        return ourInstance;
    }

    private VehicleConfig() {
    }

    // Generic config data, cross services
    String VIN = "15232532623621";
    String RVI_DOMAIN_PREFIX = "genivi.org";
    String RVI_SERVICE_EDGE_URL = "http://192.168.50.31:9001";

    // Config data for position reporting service
    int DEFAULT_REPORTING_PERIOD_IN_MS = 5000;
    String VEHICLE_REPORTING_SUBSCRIBE_SERVICE_NAME = "/logging/subscribe";
    String VEHICLE_REPORTING_UNSUBSCRIBE_SERVICE_NAME = "/logging/unsubscribe";
    String REMOTE_VEHICLE_REPORTING_SERVICE_NAME = RVI_DOMAIN_PREFIX +"/backend/logging/report";
    static final float LOCATION_REFRESH_DISTANCE = 1;
    static final long LOCATION_REFRESH_TIME = 100;

    // Config data for lock/unlock service
    String VEHICLE_REMOTE_CONTROL_LOCK_UNLOCK_SERVICE_NAME = "/control/lock";
    String DEFAULT_VEHICLE_LOCK_UNLOCK_STATE = VehicleRemoteControlManager.LOCKED_STATE;
}
