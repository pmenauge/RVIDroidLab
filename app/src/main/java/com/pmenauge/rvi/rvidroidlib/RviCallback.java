package com.pmenauge.rvi.rvidroidlib;

import com.googlecode.jsonrpc4j.JsonRpcParam;

import java.util.HashMap;

/**
 * Created by pmenauge on 19/04/17.
 */

public interface RviCallback {
//    public void message(String service, HashMap<String, Object> params);
    public void message(@JsonRpcParam(value="service_name") String service, @JsonRpcParam(value="parameters") HashMap<String, Object> params);
}
