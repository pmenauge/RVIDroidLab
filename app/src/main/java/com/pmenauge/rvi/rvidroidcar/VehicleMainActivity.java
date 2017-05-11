package com.pmenauge.rvi.rvidroidcar;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pmenauge.rvi.rvidroidlib.RviManager;
import com.pmenauge.rvi.rvidroidlib.RviManagerChangeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import static android.util.Log.d;
import static com.pmenauge.rvi.rvidroidlib.RviManager.RVI_STATE_CONNECTED;

public class VehicleMainActivity extends AppCompatActivity implements RviManagerChangeListener, VehicleRemoteControlChangeListener {

    private static final String TAG = "VehicleMainActivity";

    private EditText mRviConnectURLView = null;
    private EditText mReportingPeriodView = null;
    private EditText mRviServicesList = null;
    private TextView mRviConnectStateView = null;
    private TextView mReportingStateView = null;

    private static String [] DEFAULT_REPORTING_CHANNELS = {VehicleReportingControl.CHANNEL_VEHICLE_LOCATION, VehicleReportingControl.CHANNEL_VEHICLE_SPEED, VehicleReportingControl.CHANNEL_ODOMETER};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_main);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        RviManager.getInstance().setConnectivityManager(cm);
        RviManager.getInstance().mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        RviManager.getInstance().mWifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        RviManager.getInstance().addChangeListener(this);

        VehicleReportingManager.getInstance().init(this);

        VehicleRemoteControlManager.getInstance().init(this);
        VehicleRemoteControlManager.getInstance().addChangeListener(this);

        Button rviConnectButton = (Button)findViewById(R.id.rviConnectButton);
        rviConnectButton.setOnClickListener(mRviConnectButtonListener);

        Button rviDisconnectButton = (Button)findViewById(R.id.rviDisconnectButton);
        rviDisconnectButton.setOnClickListener(mRviDisconnectButtonListener);

        Button rviStartReportingButton = (Button)findViewById(R.id.reportingStart);
        rviStartReportingButton.setOnClickListener(mRviStartReportingButtonListener);

        Button rviStopReportingButton = (Button)findViewById(R.id.reportingStop);
        rviStopReportingButton.setOnClickListener(mRviStopReportingButtonListener);

        Button rviGetServicestButton = (Button)findViewById(R.id.getServicesButton);
        rviGetServicestButton.setOnClickListener(mRviGetServicesButtonListener);

        mRviConnectURLView = (EditText)findViewById(R.id.rviConnectURLValue);
        mRviConnectURLView.setText(VehicleConfig.getInstance().RVI_SERVICE_EDGE_URL);
        mReportingPeriodView = (EditText)findViewById(R.id.reportingPeriodValue);
        mReportingPeriodView.setText(""+ VehicleReportingManager.getInstance().getReportingPeriod());
        mRviConnectStateView = (TextView)findViewById(R.id.connectionStateValue);
        mRviConnectStateView.setText(RviManager.getInstance().getConnectionStateAsString());
        mReportingStateView = (TextView)findViewById(R.id.reportingStatusValue);
        refreshDisplayedReportingState();

        mRviServicesList = (EditText)findViewById(R.id.servicesListValue);
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
    }

    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart()");
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    private View.OnClickListener mRviConnectButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mRviConnectURLView == null || mRviConnectURLView.getText().toString().length() == 0) {
                Log.e(TAG,"mRviConnectButtonListener(): missing URL string");
            }
            else {
                String connectURL = mRviConnectURLView.getText().toString();
                Log.d(TAG,"mRviConnectButtonListener(): Connect to URL "+connectURL);
                try {
                    RviManager.getInstance().rviConnect(connectURL);
                    VehicleReportingManager.getInstance().registerRviServices();
                    VehicleRemoteControlManager.getInstance().registerRviServices();
                }
                catch (Exception e) {
                    Log.e(TAG,"mRviConnectButtonListener(): "+e);
                }
            }
        }
    };

    private View.OnClickListener mRviDisconnectButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            d(TAG,"mRviDisconnectButtonListener()");
            try {
                VehicleReportingManager.getInstance().unregisterRviServices();

                RviManager.getInstance().rviDisconnect();
                // Clear services list
                mRviServicesList.setText("");
            }
            catch (Exception e) {
                Log.e(TAG,"mRviDisconnectButtonListener(): "+e);
            }
        }
    };

    private View.OnClickListener mRviStartReportingButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mReportingPeriodView == null || mReportingPeriodView.getText().toString().length() == 0) {
                Log.d(TAG,"mRviStartReportingButtonListener(): missing reporting period duration, use default");
                mReportingPeriodView.setText(String.valueOf(VehicleConfig.getInstance().DEFAULT_REPORTING_PERIOD_IN_MS));
            }
            String reportingPeriodStr = mReportingPeriodView.getText().toString();
            Log.d(TAG,"mRviStartReportingButtonListener(): reporting period string = "+reportingPeriodStr);
            try {
                int reportingPeriodInMS = Integer.parseInt(reportingPeriodStr);

                VehicleReportingManager.getInstance().setReportingPeriod(reportingPeriodInMS);
                VehicleReportingManager.getInstance().subscribeChannels(DEFAULT_REPORTING_CHANNELS);
                refreshDisplayedReportingState();
            }
            catch (Exception e) {
                Log.d(TAG,"mRviStartReportingButtonListener(): bad format for reporting period duration, use default");
                mReportingPeriodView.setText(""+ VehicleReportingManager.getInstance().getReportingPeriod());
            }
        }
    };

    private View.OnClickListener mRviStopReportingButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
            d(TAG,"mRviStopReportingButtonListener()");
            try {
                VehicleReportingManager.getInstance().unsubscribeChannels(DEFAULT_REPORTING_CHANNELS);
                refreshDisplayedReportingState();
            }
            catch (Exception e) {
                Log.d(TAG,"mRviStopReportingButtonListener(): exception: "+e);
            }
        }
    };

    private View.OnClickListener mRviGetServicesButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            mRviServicesList.setText(""); // Clear
            if (RviManager.getInstance().getConnectionState() != RVI_STATE_CONNECTED) {
                Log.e(TAG,"mRviGetServicesButtonListener(): Not connected ("+RviManager.getInstance().getConnectionStateAsString()+"), please connect first");
            }
            else {
                try {
                    // TODO: Send get services request
                    RviManager.getInstance().rviRequestAvailableServices();
                }
                catch (Exception e) {
                    Log.e(TAG,"mRviGetServicesButtonListener(): "+e);
                }
            }
        }
    };

    private void refreshDisplayedReportingState() {
        mReportingStateView.setText(VehicleReportingManager.getInstance().isVehicleReportingOn()?"On":"Off");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_vehicle_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void rviConnectionStateChanged(RviManager source, final int newConnectionState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                d(TAG, "rviConnectionStateChanged()");
                mRviConnectStateView.setText(RviManager.getInstance().getConnectionStateAsString(newConnectionState));
            }
        });
    }

    public void rviServicesListChanged(RviManager source, final ArrayList newServicesList) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                d(TAG, "rviServicesListChanged()");
                StringBuffer strbuf = new StringBuffer();
                Collections.sort(newServicesList);
                Iterator it = newServicesList.iterator();
                while (it.hasNext()) {
                    strbuf.append((String) it.next());
                    if (it.hasNext()) strbuf.append('\n');
                }
                d(TAG, "mRviGetServicesButtonListener(): strbuf = " + strbuf);
                mRviServicesList.setText(strbuf.toString());
            }
        });
    }

    public void vehicleLocked(String lockId) {
        Log.d(TAG, "vehicleLocked(): TODO update UI to lock "+lockId);
    }

    public void vehicleUnlocked(String lockId) {
        Log.d(TAG, "vehicleUnlocked(): TODO update UI to unlock "+lockId);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
