package com.pmenauge.rvi.rvidroidlib;

import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_CONNECT;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_DISCONNECT;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_GET_AVAILABLE_SERVICES;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_GET_NODE_SERVICE_PREFIX;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_MESSAGE;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_REGISTER_SERVICE;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RviAsyncTask.RVI_CMD_UNREGISTER_SERVICE;

/**
 * Created by pmenauge on 21/03/17.
 */

public class RviManager {
    private static final String TAG = "RviManager";

    private static final RviManager ourInstance = new RviManager();

    public static RviManager getInstance() {
        return ourInstance;
    }

    public static final int RVI_STATE_DISCONNECTED = 0;
    public static final int RVI_STATE_CONNECTED = 1;

    private int mRviConnectionState = RVI_STATE_DISCONNECTED;

    private JsonRpcHttpClient mServiceEdge = null;
    private ArrayList mLocalServiceNames = new ArrayList();
    private ArrayList mServicesRepo = new ArrayList();
    private String mNodeServiceNamePrefix = null;

    private List mChangeListeners = new ArrayList<RviManagerChangeListener>();

    private Integer mLastAsyncTaskReturnCode = 0;
    private String mLastRegisteredServiceName = null;

    private String mIpAddress="127.0.0.1";
    //private String mIpAddress="192.168.50.12";
    public ConnectivityManager mConnectivityMgr;
    public WifiManager mWifiMgr;
    public TelephonyManager mTelephonyMgr;

    private int mRviCallbackPortURL = 54123;
    private int mRviCallbackPortServerSocket = 54125;
    private String mRviCallbackUrl = "http://"+ mIpAddress+":"+mRviCallbackPortURL+"/";
    private RviCallbackImpl mRviCallback;
    private RviDeviceManagerImpl mRviDeviceManager;
    private String mRviDeviceManagerServiceName = "/dm";
    private static final String [] mRviDeviceManagerMethods = {"key_provision", "cert_provision", "cert_erase", "cert_clear", "cert_revoke", "var_read", "var_write"};

    private RviManager() {
        // TODO: if Android emulated device, mRviCallbackPortServerSocket and mRviCallbackPortURL should be different for ADB port forwarding. Same otherwise.

        mRviCallback = new RviCallbackImpl(mRviCallbackPortServerSocket);

        mRviDeviceManager = new RviDeviceManagerImpl();
    }

    public void setConnectivityManager(ConnectivityManager cm) {
        mConnectivityMgr = cm;
    }

    class RviAsyncTask extends AsyncTask<Object, Void, Integer> {

        private static final String TAG = "RviAsyncTask";

        public static final String RVI_CMD_CONNECT = "connect";
        public static final String RVI_CMD_DISCONNECT = "disconnect";
        public static final String RVI_CMD_REGISTER_SERVICE = "register_service";
        public static final String RVI_CMD_UNREGISTER_SERVICE = "unregister_service";
        public static final String RVI_CMD_GET_NODE_SERVICE_PREFIX = "get_node_service_prefix";
        public static final String RVI_CMD_GET_AVAILABLE_SERVICES = "get_available_services";
        public static final String RVI_CMD_MESSAGE = "message";

        public String mCmd = null;

        public RviAsyncTask(String rviCmd) {
            mCmd = rviCmd;
        }

        protected Integer doInBackground(Object... arg0) {
            Integer rc = new Integer(0);

            Log.d(TAG, "doInBackground(): cmd="+mCmd);

            if (mCmd.equals(RVI_CMD_CONNECT)) {
                Log.d(TAG, "doInBackground(): Create SSL connection");
                String connectUrl = (String) arg0[0];

                try {
                    mServiceEdge = new JsonRpcHttpClient(new URL(connectUrl));

                    Log.d(TAG, "doInBackground(): Start rvi callback socket and thread for incoming requests");
                    mRviCallback.start();

                    try {
                        for (int i = 0 ; i < mRviDeviceManagerMethods.length ; i++) {
                            String serviceName = mRviDeviceManagerServiceName + "/" + mRviDeviceManagerMethods[i];
                            Log.d(TAG, "doInBackground(): Register Device Management service "+serviceName+" with RVI core");
                            _rviRegisterService(serviceName, mRviDeviceManager, RviDeviceManager.class);
                        }
                    }
                    catch (Exception eDm) {
                        Log.e(TAG, "doInBackground(): exception when registering Device Management service: "+eDm);
                    }

                    Log.d(TAG, "doInBackground(): Successfully connected to remote Service Edge");
                    setConnectionState(RVI_STATE_CONNECTED);
                } catch (Throwable e) {
                    e.printStackTrace();
                    rc = new Integer(-1);
                }
            }
            else if (mCmd.equals(RVI_CMD_DISCONNECT)) {
                Iterator it = mLocalServiceNames.iterator();
                while (it.hasNext()) {
                    try {
                        String serviceName = (String) it.next();
                        Log.d(TAG, "doInBackground(): send unregister_service request for "+serviceName);
                        _rviUnregisterService(serviceName);
                        Log.d(TAG, "doInBackground(): Remove service from repository: " + serviceName);
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                        rc = new Integer(-1);
                    }
                }
                mLocalServiceNames.clear();

                Log.d(TAG,"doInBackground(): Stop callback service");
                mRviCallback.stop();

                Log.d(TAG, "doInBackground(): Close connection");
                mServiceEdge = null;
                setConnectionState(RVI_STATE_DISCONNECTED);
                Log.d(TAG,"doInBackground(): Connection released, pending closure by GC");
            }
            else if (mCmd.equals(RVI_CMD_REGISTER_SERVICE)) {
                String serviceName = (String) arg0[0];
                Object serviceCallback = arg0[1];
                Class serviceCallbackInterface = (Class) arg0[2];
                try {
                    _rviRegisterService(serviceName, serviceCallback, serviceCallbackInterface);
                }
                catch (Exception e) {
                    Log.e(TAG, "doInBackground(): exception when registering service: "+e);
                    rc = new Integer(-2);
                }
            }
            else if (mCmd.equals(RVI_CMD_UNREGISTER_SERVICE)) {
                try {
                    String serviceName = (String) arg0[0];
                    rc = _rviUnregisterService(serviceName);
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    rc = new Integer(-1);
                }
            }
            else if (mCmd.equals(RVI_CMD_GET_NODE_SERVICE_PREFIX)) {
                try {
                    Map<String, Object> rviReqParams = new HashMap<String, Object>();
                    rviReqParams.put("full", new Boolean(true));
                    java.util.LinkedHashMap rspRes = mServiceEdge.invoke(RVI_CMD_GET_NODE_SERVICE_PREFIX, rviReqParams, java.util.LinkedHashMap.class);
                    Log.d(TAG, "doInBackground(): invoke() response class name = " + rspRes.getClass());
                    Log.d(TAG, "doInBackground(): invoke() response = " + rspRes);
                    rc = (Integer) rspRes.get("status");
                    if (rc >= 0) {
                        Log.d(TAG, "doInBackground(): ");
                        mNodeServiceNamePrefix = (String) rspRes.get("prefix");
                    }
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    rc = new Integer(-1);
                }
            }
            else if (mCmd.equals(RVI_CMD_GET_AVAILABLE_SERVICES)) {
                try {
                    Map<String, Object> rviReqParams = new HashMap<String, Object>();
                    // Add dummy param as empty list is not supported by RVI core
                    rviReqParams.put("dummy", new Boolean(true));
                    java.util.LinkedHashMap rspRes = mServiceEdge.invoke(RVI_CMD_GET_AVAILABLE_SERVICES, rviReqParams, java.util.LinkedHashMap.class);
                    Log.d(TAG, "doInBackground(): invoke() response class name = " + rspRes.getClass());
                    Log.d(TAG, "doInBackground(): invoke() response = " + rspRes);
                    rc = (Integer) rspRes.get("status");
                    if (rc >= 0) {
                        Log.d(TAG, "doInBackground(): Parse response");
                        mServicesRepo = (ArrayList) rspRes.get("services");
                        notifyServicesListChanged();
                    }
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    rc = new Integer(-1);
                }
            }
            else if (mCmd.equals(RVI_CMD_MESSAGE)) {
                String callingService = (String) arg0[0];
                String targetService = (String) arg0[1];
                HashMap params = (HashMap) arg0[2];
                try {
                    Map<String, Object> rviReqParams = new HashMap<String, Object>();
                    rviReqParams.put("calling_service", callingService);
                    rviReqParams.put("service_name", targetService);
                    rviReqParams.put("timeout", 1402091332);
                    rviReqParams.put("parameters", params);
                    java.util.LinkedHashMap rspRes = mServiceEdge.invoke(RVI_CMD_MESSAGE, rviReqParams, java.util.LinkedHashMap.class);
                    Log.d(TAG, "doInBackground(): invoke() response class name = " + rspRes.getClass());
                    Log.d(TAG, "doInBackground(): invoke() response = " + rspRes);
                    rc = (Integer) rspRes.get("status");
                    if (rc >= 0) {
                        Log.d(TAG, "doInBackground(): Parse response: TODO");
                    }
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    rc = new Integer(-1);
                }

            }
            else {
                Log.e(TAG, "doInBackground(): Unrecognized RVI command: " + mCmd);
            }

            return rc;
        }

        protected void onProgressUpdate(Void... progress) {
            Log.d(TAG, "onProgressUpdate():");
        }

        protected void onPostExecute(Integer result) {
            Log.d(TAG, "onPostExecute(): result="+String.valueOf(result));
        }
    }

    protected String _rviRegisterService(String localServiceName, Object serviceCallback, Class serviceCallbackInterface) throws Exception {
        try {
            Map<String, Object> rviReqParams = new HashMap<String, Object>();
            //JSONObject rviReqParams = new JSONObject();
            //rviReqParams.put("name", "/android_client");
            rviReqParams.put("service", localServiceName);
            //rviReqParams.put("address", serviceCallbackUrl);
            rviReqParams.put("network_address", mRviCallbackUrl);
            Log.d(TAG, "_rviRegisterService(): local service=" + localServiceName + ", network address="+mRviCallbackUrl);

            java.util.LinkedHashMap rspRes = mServiceEdge.invoke(RVI_CMD_REGISTER_SERVICE, rviReqParams, java.util.LinkedHashMap.class);
            Log.d(TAG, "_rviRegisterService(): invoke() response class name = " + rspRes.getClass());
            Log.d(TAG, "_rviRegisterService(): invoke() response = " + rspRes);
            Integer rc = (Integer) rspRes.get("status");
            if (rc == 0) {
                mLastRegisteredServiceName = localServiceName;
                String networkServiceName = (String) rspRes.get("service");
                Log.d(TAG, "_rviRegisterService(): Service successfully registered with name " + networkServiceName);
                mLocalServiceNames.add(mLastRegisteredServiceName);
                mRviCallback.addServiceCallback(localServiceName, serviceCallback, serviceCallbackInterface);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return mLastRegisteredServiceName;
    }

    protected Integer _rviUnregisterService(String serviceName) throws Throwable {
        // TODO stop callback server

        Map<String, Object> rviReqParams = new HashMap<String, Object>();
        rviReqParams.put("service", serviceName);

        java.util.LinkedHashMap rspRes = mServiceEdge.invoke(RVI_CMD_UNREGISTER_SERVICE, rviReqParams, java.util.LinkedHashMap.class);
        Log.d(TAG, "doInBackground(): invoke() response class name = " + rspRes.getClass());
        Log.d(TAG, "doInBackground(): invoke() response = " + rspRes);
        Integer rc = (Integer) rspRes.get("status");
        if (rc == 0) {
            Log.d(TAG, "doInBackground(): Remove service from repository: " + serviceName);
            mLocalServiceNames.remove(serviceName);
            mRviCallback.removeServiceCallback(serviceName);
        }
        return rc;
    }

    public void rviConnect(String connectUrl) throws Exception {
        if (mRviConnectionState == RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviConnect(): Already connected, please disconnect first");
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_CONNECT).execute(connectUrl);
            Log.d(TAG, "rviConnect(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviConnect(): async task completed: "+status);
        }
    }

    public void rviDisconnect() throws Exception {
        if (mServiceEdge == null) {
            Log.d(TAG, "rviDisconnect(): no existing connection");
        }
        else if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviDisconnect(): not in CONNECTED state: " + getConnectionStateAsString());
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_DISCONNECT).execute();
            Log.d(TAG, "rviDisconnect(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviDisconnect(): async task completed: "+status);
        }
    }

    public void rviRegisterService(String serviceName, Object serviceCallback, Class serviceCallbackInterface) throws Exception {
        if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviRegisterService(): Not connected, please connect first");
        } else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_REGISTER_SERVICE).execute(serviceName, serviceCallback, serviceCallbackInterface);
            //new Integer(mRviCallbackPort), mRviCallback.getStreamServer());
            Log.d(TAG, "rviRegisterService(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviRegisterService(): async task completed: "+status);
        }
    }

    public void rviUnregisterService(String serviceName) throws Exception {
        if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviUnregisterService(): Not connected, please connect first");
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_UNREGISTER_SERVICE).execute(serviceName);
            Log.d(TAG, "rviUnregisterService(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviUnregisterService(): async task completed: "+status);
        }
    }

    public void rviRequestNodeServiceNamePrefix() throws Exception {
        if (mServiceEdge == null) {
            Log.d(TAG, "rviRequestNodeServiceNamePrefix(): no existing connection");
        }
        else if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviRequestNodeServiceNamePrefix(): not in CONNECTED state: " + getConnectionStateAsString());
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_GET_NODE_SERVICE_PREFIX).execute();
            Log.d(TAG, "rviRequestNodeServiceNamePrefix(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviRequestNodeServiceNamePrefix(): async task completed: "+status);
        }
    }

    public void rviInvoke(String callingService, String targetService, HashMap params) throws Exception {
        if (mServiceEdge == null) {
            Log.d(TAG, "rviInvoke(): no existing connection");
        }
        else if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviInvoke(): not in CONNECTED state: " + getConnectionStateAsString());
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_MESSAGE).execute(callingService, targetService, params);
            Log.d(TAG, "rviInvoke(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviInvoke(): async task completed: "+status);
        }
    }

    public void rviRequestAvailableServices() throws Exception {
        if (mServiceEdge == null) {
            Log.d(TAG, "rviRequestAvailableServices(): no existing connection");
        }
        else if (mRviConnectionState != RVI_STATE_CONNECTED) {
            Log.d(TAG, "rviRequestAvailableServices(): not in CONNECTED state: " + getConnectionStateAsString());
        }
        else {
            RviAsyncTask task = (RviAsyncTask) new RviAsyncTask(RVI_CMD_GET_AVAILABLE_SERVICES).execute();
            Log.d(TAG, "rviRequestAvailableServices(): wait for async task to complete");
            Integer status = task.get();
            Log.d(TAG, "rviRequestAvailableServices(): async task completed: "+status);
        }
    }

    public void addChangeListener(RviManagerChangeListener listener) {
        mChangeListeners.add(listener);
    }

    public void removeChangeListener(RviManagerChangeListener listener) {
        mChangeListeners.remove(listener);
    }

    private void notifyConnectionStateChanged() {
        Iterator it = mChangeListeners.iterator();
        while (it.hasNext()) {
            RviManagerChangeListener listener = (RviManagerChangeListener) it.next();
            listener.rviConnectionStateChanged(this, mRviConnectionState);
        }
    }

    private void notifyServicesListChanged() {
        Iterator it = mChangeListeners.iterator();
        while (it.hasNext()) {
            RviManagerChangeListener listener = (RviManagerChangeListener) it.next();
            listener.rviServicesListChanged(this, mServicesRepo);
        }
    }

    public JsonRpcHttpClient getRviServiceEdge() {
        return mServiceEdge;
    }

    public String getNodeServiceNamePrefix() { return mNodeServiceNamePrefix; }

    public String getConnectionStateAsString() {
        return getConnectionStateAsString(mRviConnectionState);
    }

    public String getConnectionStateAsString(int state) {
        switch (state) {
            case RVI_STATE_DISCONNECTED: return "RVI_STATE_DISCONNECTED";
            case RVI_STATE_CONNECTED: return "RVI_STATE_CONNECTED";
            default: return "RVI_STATE_UNRECOGNIZED";
        }
    }

    public int getConnectionState() {
        return mRviConnectionState;
    }

    private void setConnectionState(int connectionState) {
        mRviConnectionState = connectionState;
        notifyConnectionStateChanged();
    }

}

/*
    private String getIpAddress() {
        if (mConnectivityMgr == null) {
            Log.e(TAG, "getIpAddress(): Initialisation issue, no connectivity manager assigned");
            mIpAddress = null;
        }
        else if (mIpAddress == null) {
            // TODO: register for connection changes
            NetworkInfo net = mConnectivityMgr.getActiveNetworkInfo();
            if (null == net) {
                Log.e(TAG, "getIpAddress(): No active network, cannot determine IP address");
            }
            else if (!net.isConnectedOrConnecting()) {
                Log.e(TAG, "getIpAddress(): No internet connection, cannot determine IP address");
            }
            else {
                Log.d(TAG, "getIpAddress(): Network type="+net.getTypeName());
                if (net.getTypeName().equalsIgnoreCase("WIFI")) {
                    int ipAddressNum = mWifiMgr.getDhcpInfo().ipAddress;
                    mIpAddress = String.format("%d.%d.%d.%d", (ipAddressNum & 0xff),(ipAddressNum >> 8 & 0xff),(ipAddressNum >> 16 & 0xff),(ipAddressNum >> 24 & 0xff));
                    Log.d(TAG, "getIpAddress(): Retreive WIFI DHCP address: "+ipAddressNum+" = "+mIpAddress);
                }
                if (net.getTypeName().equalsIgnoreCase("MOBILE")) {
                    try {
                        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                        for (NetworkInterface intf : interfaces) {
                            List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                            for (InetAddress addr : addrs) {
                                Log.d(TAG, "getIpAddress(): if="+intf.getDisplayName()+", addr="+addr.getHostAddress().toUpperCase()+", isLoopback="+addr.isLoopbackAddress());
                                if (!addr.isLoopbackAddress()) {
                                    mIpAddress = addr.getHostAddress().toUpperCase();
                                }
                            }
                        }
                    } catch (Exception ex) { ex.printStackTrace();} // for now eat exceptions
                }
                else {

                }
            }
        }
        return mIpAddress;
    }

*/