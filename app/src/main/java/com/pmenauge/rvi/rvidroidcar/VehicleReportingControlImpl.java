package com.pmenauge.rvi.rvidroidcar;

import android.util.Log;

import com.googlecode.jsonrpc4j.JsonRpcParam;

//import javax.servlet.Servlet;

/**
 * Created by pmenauge on 03/04/17.
 */

public class VehicleReportingControlImpl implements VehicleReportingControl {

    private static final String TAG = "VehiReportingCtrlImpl";

    public VehicleReportingControlImpl() {

    }

    public void subscribe(@JsonRpcParam(value="channels") String [] channels, @JsonRpcParam(value="reporting_interval") Integer reportingIntervalMs) {
        Log.d(TAG, "subscribe(): ##########################################################");
        StringBuffer buf = new StringBuffer();
        for (int i = 0 ; i < channels.length ; i++) {
            buf.append(channels[i]);
            if (i != channels.length-1) buf.append(", ");
        }
        Log.d(TAG, "subscribe(): "+channels.length+" channels="+buf.toString()+" ; interval="+reportingIntervalMs);
        VehicleReportingManager.getInstance().setReportingPeriod(reportingIntervalMs);
        VehicleReportingManager.getInstance().subscribeChannels(channels);
        Log.d(TAG, "subscribe(): ##########################################################");
    }

    public void unsubscribe(@JsonRpcParam(value="channels") String [] channels){
        Log.d(TAG, "unsubscribe(): ##########################################################");
        StringBuffer buf = new StringBuffer();
        for (int i = 0 ; i < channels.length ; i++) {
            buf.append(channels[i]);
            if (i != channels.length-1) buf.append(", ");
        }
        Log.d(TAG, "unsubscribe(): "+channels.length+" channels="+buf.toString());
        VehicleReportingManager.getInstance().unsubscribeChannels(channels);
        Log.d(TAG, "unsubscribe(): ##########################################################");
    }


}
