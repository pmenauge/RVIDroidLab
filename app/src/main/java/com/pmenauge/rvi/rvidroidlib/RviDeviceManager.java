package com.pmenauge.rvi.rvidroidlib;

import com.googlecode.jsonrpc4j.JsonRpcParam;

import java.util.HashMap;

/**
 * Implements RVI DM interface
 */

public interface RviDeviceManager {
    public void provisionKey(@JsonRpcParam(value="keyId") String keyId, @JsonRpcParam(value="key") String key);
    public void provisionCertificate(@JsonRpcParam(value="certid") String certid, @JsonRpcParam(value="checksum") String checksum, @JsonRpcParam(value="certificate") String certificate);
    public void eraseCertificate(@JsonRpcParam(value="certid") String certid);
    public void clearCertificates();
    public void revokeCertificate(@JsonRpcParam(value="certid") String certid);
    public void readConfigurationVariable(@JsonRpcParam(value="variable") String [] variables);
    public void writeConfigurationVariable(@JsonRpcParam(value="variables") HashMap<String,String>[] variables);

}
