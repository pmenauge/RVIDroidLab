package com.pmenauge.rvi.rvidroidlib;

import android.util.Log;

import com.googlecode.jsonrpc4j.JsonRpcParam;

import java.util.HashMap;

/**
 * Created by pmenauge on 20/04/17.
 */

public class RviDeviceManagerImpl implements RviDeviceManager {
    private static final String TAG = "RviDeviceManagerImpl";

    public void provisionKey(@JsonRpcParam(value="keyId") String keyId, @JsonRpcParam(value="key") String key) {
        Log.d(TAG, "provisionKey() - TODO store into secure storage");
    }

    public void provisionCertificate(@JsonRpcParam(value="certid") String certid, @JsonRpcParam(value="checksum") String checksum, @JsonRpcParam(value="certificate") String certificate) {
        Log.d(TAG, "provisionCertificate() - TODO store into secure storage");
    }

    public void eraseCertificate(@JsonRpcParam(value="certid") String certid) {
        Log.d(TAG, "eraseCertificate() - TODO erase from secure storage");
    }

    public void clearCertificates() {
        Log.d(TAG, "clearCertificates() - TODO erase from secure storage");
    }

    public void revokeCertificate(@JsonRpcParam(value="certid") String certid) {
        Log.d(TAG, "revokeCertificate() - TODO erase from secure storage");
    }

    public void readConfigurationVariable(@JsonRpcParam(value="variable") String [] variables) {
        Log.d(TAG, "readConfigurationVariable() - TODO read from secure Management Information Base");
    }

    public void writeConfigurationVariable(@JsonRpcParam(value="variables") HashMap<String,String> [] variables) {
        Log.d(TAG, "writeConfigurationVariable() - TODO store into secure Management Information Base");
    }
}
