package com.pmenauge.rvi.rvidroidcar;

import com.googlecode.jsonrpc4j.JsonRpcParam;

/**
 * Created by pmenauge on 03/04/17.
 */

public interface VehicleReportingControl {

    public static final String CHANNEL_VEHICLE_SPEED = "speed";
    public static final String CHANNEL_ENGINE_RPM = "rpm";
    public static final String CHANNEL_ENGINE_COOLANT_TEMP = "ctemp";
    public static final String CHANNEL_MASS_AIR_FLOW = "maf";
    public static final String CHANNEL_AIR_INTAKE_TEMP = "ait";
    public static final String CHANNEL_ENGINE_LOAD = "eload";
    public static final String CHANNEL_THROTTLE_POSITION = "tpos";
    public static final String CHANNEL_BATTERY_VOLTAGE = "bvolt";
    public static final String CHANNEL_AMBIENT_AIR_TEMP = "atemp";
    public static final String CHANNEL_ENGINE_OIL_TEMP = "otemp";
    public static final String CHANNEL_ENGINE_RUNNING_TIME = "etime";
    public static final String CHANNEL_VEHICLE_LOCATION = "location";
    public static final String CHANNEL_ODOMETER = "odometer";
    public static final String [] ALL_CHANNELS = {CHANNEL_VEHICLE_SPEED, CHANNEL_ENGINE_RPM, CHANNEL_ENGINE_COOLANT_TEMP, CHANNEL_MASS_AIR_FLOW, CHANNEL_AIR_INTAKE_TEMP, CHANNEL_ENGINE_LOAD, CHANNEL_THROTTLE_POSITION, CHANNEL_BATTERY_VOLTAGE, CHANNEL_AMBIENT_AIR_TEMP, CHANNEL_ENGINE_OIL_TEMP, CHANNEL_ENGINE_RUNNING_TIME, CHANNEL_VEHICLE_LOCATION, CHANNEL_ODOMETER};

    public void subscribe(@JsonRpcParam(value="channels") String [] channels, @JsonRpcParam(value="reporting_interval") Integer reportingIntervalMs);
    public void unsubscribe(@JsonRpcParam(value="channels") String [] channels);
}
