package com.pmenauge.rvi.rvidroidlib;

import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.StreamServer;

import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.HashMap;

import static android.content.ContentValues.TAG;

/**
 * Created by pmenauge on 19/04/17.
 */

public class RviCallbackImpl implements RviCallback {

    private static final String TAG = "RviCallbackImpl";

    private JsonRpcServer mJsonRpcServer;
    private StreamServer mStreamServer;
    private ServerSocket mServerSocket;
    private int mStreamServerPort;
    private int mMaxThreads = 3;
    private HashMap<String, Object> mServiceCallbacks;

    public RviCallbackImpl(int port) {
        // handler
        mJsonRpcServer = new JsonRpcServer(new RviObjectMapper(), this, RviCallback.class);
        mStreamServerPort = port;
        mServiceCallbacks = new HashMap<String, Object>();
        try {
            mServerSocket = new ServerSocket(mStreamServerPort);
            mStreamServer = new StreamServer(mJsonRpcServer, mMaxThreads, mServerSocket);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to initialize callback server");
        }
    }

    public void addServiceCallback(String serviceName, Object serviceCallback, Class serviceCallbackInterface) {
        Log.d(TAG, "addServiceCallback(): add service callback for "+serviceName);
        RviServiceCallback cb = new RviServiceCallback(serviceName, serviceCallback, serviceCallbackInterface);
        mServiceCallbacks.put(serviceName, cb);
    }

    public void removeServiceCallback(String serviceName) {
        Log.d(TAG, "removeServiceCallback(): remove service callback for "+serviceName);
        mServiceCallbacks.remove(serviceName);
    }

    public void start() {
        if (mStreamServer == null) {
            Log.d(TAG, "start(): callback server not initialized");
        }
        if (mStreamServer.isStarted()) {
            Log.d(TAG, "start(): callback server already started");
        }
        else {
            Log.d(TAG, "start(): start callback server");
            mStreamServer.start();
        }
    }

    public void stop() {
        if (mStreamServer == null) {
            Log.d(TAG, "stop(): callback server not initialized");
        }
        if (!mStreamServer.isStarted()) {
            Log.d(TAG, "stop(): callback server already stopped");
        }
        else {
            try {
                Log.d(TAG, "stop(): stop callback server");
                mStreamServer.stop();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public StreamServer getStreamServer() {
        return mStreamServer;
    }

    public int getPort() {
        return mStreamServerPort;
    }

    public static Object parseJsonRpcParamValue(String jsonStr) throws Exception {
        Object paramValue = null;
        try {
            Log.d(TAG, "parseJsonRpcParamValue(): Parse "+jsonStr+" with readTree()");
            JsonNode paramValueNode = new ObjectMapper().readTree(jsonStr);
            JsonNodeType paramValueType = paramValueNode.getNodeType();
            if (paramValueNode.isArray()) {
                Log.d(TAG, "parseJsonRpcParamValue(): isArray = true");
                int arraySize = paramValueNode.size();
                Object [] paramValueArray = null;
                if (arraySize == 0) {
                    Log.d(TAG, "parseJsonRpcParamValue(): empty array");
                }
                else {
                    for (int i = 0 ; i < arraySize ; i++) {
                        JsonNode arrayCell = paramValueNode.get(i);
                        JsonNodeType arrayCellType = arrayCell.getNodeType();
                        Log.d(TAG, "parseJsonRpcParamValue(): type of array cell: "+arrayCellType);
                        if (arrayCell.isArray()) {
                            Log.d(TAG, "parseJsonRpcParamValue(): array of array, not yet implemented");
                        }
                        else if (arrayCell.isBoolean()) {
                            if (paramValueArray == null) paramValueArray = new Boolean[arraySize];
                            paramValueArray[i] = arrayCell.booleanValue();
                            Log.d(TAG, "parseJsonRpcParamValue(): boolean value: "+paramValueArray[i]);
                        }
                        else if (arrayCell.isNumber()) {
                            if (paramValueArray == null) paramValueArray = new Number[arraySize];
                            paramValueArray[i] = arrayCell.numberValue();
                            Log.d(TAG, "parseJsonRpcParamValue(): number value: "+paramValueArray[i]+", type: "+arrayCell.numberType());
                        }
                        else if (arrayCell.isNull()) {
                            Log.d(TAG, "parseJsonRpcParamValue(): null value");
                        }
                        else if (arrayCell.isTextual()) {
                            if (paramValueArray == null) paramValueArray = new String[arraySize];
                            paramValueArray[i] = arrayCell.textValue();
                            Log.d(TAG, "parseJsonRpcParamValue(): String value: "+paramValueArray[i]);
                        }
                        else if (arrayCell.isBinary()) {
                            if (paramValueArray == null) paramValueArray = new Byte[arraySize];
                            paramValueArray[i] = arrayCell.binaryValue();
                            Log.d(TAG, "parseJsonRpcParamValue(): boolean value: "+paramValueArray[i]);
                        }
                        else if (arrayCell.isObject()) {
                            Log.d(TAG, "parseJsonRpcParamValue(): isObject = true, not yet implemented");
                        }
                        else if (arrayCell.isMissingNode()) {
                            Log.d(TAG, "parseJsonRpcParamValue(): isMissingNode = true, not yet implemented");
                        }
                        else {
                            Log.d(TAG, "parseJsonRpcParamValue(): unexpected value type: "+arrayCellType);
                        }
                    }
                }
                for (int k=0; k < paramValueArray.length ; k++) {
                    Log.d(TAG, "parseJsonRpcParamValue(): array cell["+k+"] = "+paramValueArray[k].getClass().getName());
                }
                paramValue = paramValueArray;
                Object [] paramValueArray2 = (Object[]) paramValue;
                for (int k=0; k < paramValueArray2.length ; k++) {
                    Log.d(TAG, "parseJsonRpcParamValue(): array2 cell["+k+"] = "+paramValueArray2[k].getClass().getName());
                }
            }
            else if (paramValueNode.isBoolean()) {
                paramValue = paramValueNode.booleanValue();
                Log.d(TAG, "parseJsonRpcParamValue(): boolean value: "+paramValue);
            }
            else if (paramValueNode.isNumber()) {
                paramValue = paramValueNode.numberValue();
                Log.d(TAG, "parseJsonRpcParamValue(): number value: "+paramValue+", type: "+paramValueNode.numberType());
            }
            else if (paramValueNode.isNull()) {
                Log.d(TAG, "parseJsonRpcParamValue(): null value");
            }
            else if (paramValueNode.isTextual()) {
                paramValue = paramValueNode.textValue();
                Log.d(TAG, "parseJsonRpcParamValue(): String value: "+paramValue);
            }
            else if (paramValueNode.isBinary()) {
                paramValue = paramValueNode.binaryValue();
                Log.d(TAG, "parseJsonRpcParamValue(): boolean value: "+paramValue);
            }
            else if (paramValueNode.isObject()) {
                Log.d(TAG, "parseJsonRpcParamValue(): isObject = true, not yet implemented");
            }
            else if (paramValueNode.isMissingNode()) {
                Log.d(TAG, "parseJsonRpcParamValue(): isMissingNode = true, not yet implemented");
            }
            else {
                Log.d(TAG, "parseJsonRpcParamValue(): unexpected value type: "+paramValueType);
            }
        }
        catch (Exception e) {
            Log.d(TAG, "parseJsonRpcParamValue(): error while parsing parameter value");
            e.printStackTrace();
            throw e;
        }
        return paramValue;
    }


    class RviServiceCallback {
        String serviceName;
        Object callbackObj;
        Class callbackInterface;
        Method lastRequestCbMethod;
        Object [] lastRequesCbMethodtArgs;

        RviServiceCallback(String serviceName, Object callbackObj, Class callbackInterface) {
            this.serviceName = serviceName;
            this.callbackObj = callbackObj;
            this.callbackInterface = callbackInterface;
        }
    }

    RviServiceCallback getServiceCallbackByNames(String serviceName, HashMap<String, Object> params) {
        if (serviceName == null) {
            Log.e(TAG, "getServiceCallbackByName(): service name is null");
            return null;
        }

        // Search callback object and interface as previously registered
        RviServiceCallback svcCb = (RviServiceCallback) mServiceCallbacks.get(serviceName);
        if (svcCb == null) {
            if (params == null || !params.containsKey("service")) {
                Log.e(TAG, "getServiceCallbackByNames(): service name not provided and not available in parameters. Cannot find callback object");
            }
            else {
                serviceName = (String) params.get("service");
                svcCb = (RviServiceCallback) mServiceCallbacks.get(serviceName);
                if (svcCb == null) {
                    Log.e(TAG, "getServiceCallbackByNames(): service name " + serviceName + " provided in parameters but no registered callback found");
                }
            }
        }
        return svcCb;
    }

    void searchCallbackMethodAndPrepareArgs(RviServiceCallback svcCb, String serviceMethodName, HashMap<String, Object> params) {
        Class callbackClass = svcCb.callbackObj.getClass();
        //Class callbackClass = callbackInterface;
        Method[] methods = callbackClass.getMethods();
        boolean foundCallbackMethod = false;
        for (int methodIndex = 0; (methodIndex < methods.length && !foundCallbackMethod); methodIndex++) {
            svcCb.lastRequestCbMethod = methods[methodIndex];
            String methodName = svcCb.lastRequestCbMethod.getName();
            if (methodName.equals(serviceMethodName)) {
                int methodParamNb = svcCb.lastRequestCbMethod.getGenericParameterTypes().length;
                Log.d(TAG, "message(): found method with right name, params nb = " + methodParamNb);
                if (methodParamNb != params.size()) {
                    Log.d(TAG, "message(): different parameter numbers, ignore method");
                    svcCb.lastRequestCbMethod = null;
                } else {
                    // Found method on the callback object with proper name and number of parameters
                    // Now, compare parameter names
                    java.lang.annotation.Annotation[][] paramAnnotations = svcCb.lastRequestCbMethod.getParameterAnnotations();
                    Class[] paramTypes = svcCb.lastRequestCbMethod.getParameterTypes();
                    boolean parameterIsMissing = false, parameterValueParsingError = false;
                    svcCb.lastRequesCbMethodtArgs = new Object[methodParamNb];
                    for (int i = 0; i < paramAnnotations.length; i++) {
                        for (int j = 0; j < paramAnnotations[i].length; j++) {
                            //Log.d(TAG, "message(): parameter annotation [" + i + "][" + j + "] = " + paramAnnotations[i][j]);
                            Class annotationType = paramAnnotations[i][j].annotationType();
                            //Log.d(TAG, "message(): parameter annotation type [" + i + "][" + j + "] = " + annotationType);
                            // Get param name from annotations ane retreive value from "params" object
                            if (paramAnnotations[i][j] instanceof JsonRpcParam) {
                                JsonRpcParam rpcParam = (JsonRpcParam) paramAnnotations[i][j];
                                String paramName = rpcParam.value();
                                if (!params.containsKey(paramName)) {
                                    Log.d(TAG, "message(): missing value for parameter " + paramName + ", look for another method");
                                    parameterIsMissing = true;
                                    break;
                                } else {
                                    try {
                                        Object paramValue = params.get(paramName);
                                        Class paramClass = paramTypes[i];
                                        String strObj = "";
                                        //Log.d(TAG, "message(): parameter type name is " + paramClass.getName());
                                        svcCb.lastRequesCbMethodtArgs[i] = parseJsonRpcParamValue((String) paramValue);
                                        Log.d(TAG, "message(): found parameter with right name, assign value " + svcCb.lastRequesCbMethodtArgs[i] + " to parameter [" + i + "]");
                                    } catch (Exception e) {
                                        Log.e(TAG, "message(): parsing error for parameter " + paramName);
                                        parameterValueParsingError = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (parameterIsMissing || parameterValueParsingError) {
                        svcCb.lastRequesCbMethodtArgs = null;
                        svcCb.lastRequestCbMethod = null;
                    }
                    // Found method with right name & parameters nb, no parsing errors, consider this is the right one to call
                    foundCallbackMethod = true;
                }
            }
        }
    }

    public void message(@JsonRpcParam(value="service_name") String service, @JsonRpcParam(value="parameters") HashMap<String, Object> params) {
        Log.d(TAG, "message(): service="+service+", params="+params.toString());
        RviServiceCallback svcCb = getServiceCallbackByNames(service, params);

        if (svcCb != null) {
            // Found callback object, look for proper method, check parameters and prepare arguments values for calling invoke() method
            String serviceMethodName = service.substring(service.lastIndexOf('/') + 1);
            int serviceParamNb = params.size();
            Log.d(TAG, "message(): search for method \"" + serviceMethodName + "\" with "+serviceParamNb+" parameters");
            searchCallbackMethodAndPrepareArgs(svcCb, serviceMethodName, params);
            if (svcCb.lastRequestCbMethod == null || svcCb.lastRequesCbMethodtArgs == null) {
                // Error, cannot serve the request
                Log.e(TAG, "message(): Could not find any matching method on callback objects, discard request");
            }
            else {
                try {
                    Log.d(TAG, "message(): invoke callback method");
                    Object result = svcCb.lastRequestCbMethod.invoke(svcCb.callbackObj, svcCb.lastRequesCbMethodtArgs);
                    Log.d(TAG, "message(): received result="+result);
                }
                catch (Exception e) {
                    Log.e(TAG, "message(): exception during invoke of callback method");
                    e.printStackTrace();
                }
            }
        }
    }

}
