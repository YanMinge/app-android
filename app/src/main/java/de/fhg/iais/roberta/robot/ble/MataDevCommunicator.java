package de.fhg.iais.roberta.robot.ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.fhg.iais.roberta.main.ORLabActivity;
import de.fhg.iais.roberta.main.R;
import de.fhg.iais.roberta.main.Util;
import de.fhg.iais.roberta.robot.RobotCommunicator;

import static java.lang.Integer.parseInt;

public class MataDevCommunicator extends RobotCommunicator {
    private static final String TAG = MataDevCommunicator.class.getSimpleName();
    private static final int REQUEST_BT_ENABLE = 10;
    private static final int REQUEST_GPS_ENABLE = 11;
    private static final int REQUEST_LOC_PERMISSION = 12;
    private ORLabActivity orLabActivity;
    private WebView mainView;
    private BluetoothClient BleClient;
    List<Map<String,Object>> scannedDevices = new ArrayList<Map<String,Object>>();
    List<Map<String,Object>> connectedDevices = new ArrayList<Map<String,Object>>();

    public MataDevCommunicator(ORLabActivity orLabActivity, WebView mainView) {
        this.orLabActivity = orLabActivity;
        this.mainView = mainView;
        this.ROBOT = "matatabot";
        this.scannedDevices = scannedDevices;
        this.connectedDevices = connectedDevices;
        this.BleClient = new BluetoothClient(orLabActivity);
        Log.d(TAG, "initialized");
    }

    private boolean isBleMacInList(List<Map<String,Object>> list,String mac) {
        for (int i = 0; i < list.size(); i++) {
            final Map<String,Object> deviceMap = list.get(i);
            if (deviceMap.get("mac").equals(mac)) {
                return true;
            }
        }
        return false;
    }

    //@RequiresApi(api = Build.VERSION_CODES.M)
    private void startScan() {
        Log.d(TAG, "startScan");
        // check if Bluetooth adapter is available
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //BluetoothDevice BluetoothDevice = new BluetoothDevice();
        if (bluetoothAdapter == null) {
            reportScanError("no Bluetooth adapter available");
            Util.showAlert(this.orLabActivity, R.string.blue_adapter_missing);
            return;
        }
        // check if Bluetooth LE is available
        if (!this.orLabActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            reportScanError("no Bluetooth LE available");
            Util.showAlert(this.orLabActivity, R.string.blue_adapter_missing);
            return;
        }
        // check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Util.enableBluetooth(orLabActivity, REQUEST_BT_ENABLE);
            return;
        }
        // check bluetooth version
        int bluetoothVersion = parseInt(Build.VERSION.SDK);
        switch (bluetoothVersion) {
            case Build.VERSION_CODES.BASE:
            case Build.VERSION_CODES.BASE_1_1:
            case Build.VERSION_CODES.CUPCAKE:
            case Build.VERSION_CODES.DONUT:
                reportScanError("wrong Bluetooth version");
                Util.showAlert(this.orLabActivity, R.string.msg_blue_old_version);
                return;
            default:
                break;
        }
        if (orLabActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Util.checkForLocationPermission(orLabActivity, REQUEST_LOC_PERMISSION);
        }
        // for Bluetooth LE, we need at least "Battery Saving" permission at location mode
        final LocationManager manager = (LocationManager) this.orLabActivity.getSystemService(Context.LOCATION_SERVICE);
        if (manager != null) {
            if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Util.enableLocationService(this.orLabActivity, REQUEST_GPS_ENABLE);
                return;
            }
        }else{
            Log.e(TAG, "no location manager");
            Util.showAlert(this.orLabActivity,R.string.location_problem);
            reportScanError("no location service");
            return;
        }
        try {
            SearchRequest request = new SearchRequest.Builder()
                    .searchBluetoothLeDevice(5000, 6)
                    .build();
            this.BleClient.search(request, new SearchResponse() {
                @Override
                public void onSearchStarted() {
                    scannedDevices.clear();
                    Log.d("TAG", "onSearchStarted");
                }
                @Override
                public void onDeviceFounded(SearchResult device) {
                    if ((device.getName() != null) && (device.getName().contains("Mata")) && (!isBleMacInList(scannedDevices, device.getAddress()))) {
                        Map<String,Object> deviceMap = new HashMap<String,Object>();
                        if ((device.rssi > -95) && (device.rssi != 0)) {
                            deviceMap.put("name", device.getName());
                            deviceMap.put("rssi" , device.rssi);
                            deviceMap.put("mac" , device.getAddress());
                            scannedDevices.add(deviceMap);
                            String ScanMessage = device.getName() + "--" + device.getAddress() + "(" + device.rssi + "db)";
                            Log.d("TAG", ScanMessage);
                            reportStateChanged("scan", "appeared", (String)device.getAddress(), "brickname", ScanMessage);
                        }
                    }
                }
                @Override
                public void onSearchStopped() {
                    scannedDevices.clear();
                    Log.d("TAG", "onSearchStopped");
                }
                @Override
                public void onSearchCanceled() {
                    scannedDevices.clear();
                    Log.d("TAG", "onSearchCanceled");
                }
            });
        } catch (IllegalStateException e) {
            reportScanError( e.getMessage());
            Util.showAlert(this.orLabActivity, e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScan() {
        scannedDevices.clear();
        this.BleClient.stopSearch();
        Log.d(TAG, "stop scanning");
    }

    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == Constants.STATUS_CONNECTED) {
                Log.d(TAG, "STATUS_CONNECTED device:" + mac);
                for (int i = 0; i < scannedDevices.size(); i++) {
                    final Map<String,Object> deviceMapTemp = scannedDevices.get(i);
                    if (deviceMapTemp.get("mac").equals(mac)) {
                        Map<String,Object> deviceMap = new HashMap<String,Object>();
                        deviceMap.put("name", deviceMapTemp.get("name"));
                        deviceMap.put("rssi" , deviceMapTemp.get("rssi"));
                        deviceMap.put("mac" , deviceMapTemp.get("mac"));
                        connectedDevices.add(deviceMap);
                        reportStateChanged("connect", "connected", (String)deviceMapTemp.get("mac"), "brickname", (String)deviceMapTemp.get("name"));
                        break;
                    }
                }
            } else if (status == Constants.STATUS_DISCONNECTED) {
                Log.d(TAG, "STATUS_DISCONNECTED device:" + mac);
                for (int i = 0; i < connectedDevices.size(); i++) {
                    final Map<String,Object> deviceMapTemp = connectedDevices.get(i);
                    if (deviceMapTemp.get("mac").equals(mac)) {
                        connectedDevices.remove(i);
                        reportStateChanged("connect", "disconnected", (String)deviceMapTemp.get("mac"));
                        break;
                    }
                }
            }
        }
    };

    private void connect(String deviceID) {
        String deviceMac = deviceID;
        Log.d(TAG, "connect deviceID:" + deviceMac);
        for (int i = 0; i < connectedDevices.size(); i++) {
            final Map<String,Object> deviceMapTemp = connectedDevices.get(i);
            if (deviceMapTemp.get("mac").equals(deviceMac)) {
                Map<String,Object> deviceMap = new HashMap<String,Object>();
                Log.d(TAG, "the device is connected!");
                return;
            }
        }
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)
                .setConnectTimeout(20000)
                .setServiceDiscoverRetry(3)
                .setServiceDiscoverTimeout(10000)
                .build();
        this.BleClient.registerConnectStatusListener(deviceMac, mBleConnectStatusListener);
        this.BleClient.connect(deviceMac, options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile data) {
                switch (code) {
                    case Constants.REQUEST_SUCCESS:
                        Log.d(TAG, "REQUEST_SUCCESS");
                        break;
                    case Constants.REQUEST_FAILED:
                        reportStateChanged("connect", "failed", deviceMac);
                        Log.d(TAG, "REQUEST_FAILED");
                        break;
                    case Constants.REQUEST_CANCELED:
                        Log.d(TAG, "REQUEST_CANCELED");
                        break;
                    case Constants.REQUEST_TIMEDOUT:
                        Log.d(TAG, "REQUEST_TIMEDOUT");
                        break;
                    default:
                        reportStateChanged("connect", "failed", deviceMac);
                        Log.d(TAG, "ERROR:" + code);
                        break;
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void jsToRobot(JSONObject msg) {
        Log.d(TAG, "jsToRobot: " + msg.toString());
        try {
            String target = msg.getString("target");
            String type = msg.getString("type");
            if (target.equals(ROBOT)) {
                switch (type) {
                    case "startScan":
                        this.startScan();
                        break;
                    case "stopScan":
                        this.stopScan();
                        break;
                    case "connect":
                        this.connect(msg.getString("robot"));
                        break;
                    case "disconnect":
                        //TODO check if we need more information from webview
                    case "command":
                        switch (msg.getString("actuator")) {
                            case "motor":
                                break;
                            case "piezo":
                                break;
                            case "light":
                                break;
                            default:
                                throw new NullPointerException();
                        }
                        break;
                    default:
                        Log.e(TAG, "Not supported msg: " + msg);
                        break;
                }
            }
        } catch (final JSONException e) {
            // ignore invalid messages
            Log.e(TAG, "Json parsing error: " + e.getMessage() + " processing: " + msg);
        } catch (final NullPointerException e) {
            Log.e(TAG, "Command parsing error: " + e.getMessage() + " processing: " + msg);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void close() {
        this.scannedDevices.clear();
        for (int i = 0; i < connectedDevices.size(); i++) {
            final Map<String,Object> deviceMapTemp = connectedDevices.get(i);
            this.BleClient.disconnect((String)deviceMapTemp.get("mac"));
            this.BleClient.unregisterConnectStatusListener((String)deviceMapTemp.get("mac"), mBleConnectStatusListener);
        }
        this.connectedDevices.clear();
    }

    public void didUpdateDeviceInfo() {
        //TODO check if and what we want to report from deviceInfo
        //reportStateChanged("connect", "connected", legoDevice.getDeviceId(), "brickname", legoDevice.getName());
    }

    public void didChangeNameFrom(String s, String s1) {
        // not supported in Open Roberta
    }

    public void didChangeButtonState(boolean b) {
        // TODO check if we should support this button in the Open Roberta Lab (works reliable)
        //reportStateChanged("update", String.valueOf(b), legoDevice.getDeviceId(), "sensor", "button");
    }

    public void didUpdateBatteryLevel(int i) {
        //reportStateChanged("update", Integer.toString(i), legoDevice.getDeviceId(), "sensor", "batterylevel");
    }
    public void didUpdateLowVoltageState(boolean b) {
        // not supported in Open Roberta
    }

    public void didAddService() {
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void handleActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_BT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    startScan();
                } else {
                    reportScanError("Bluetooth adapter not enabled");
                }
                break;
            case REQUEST_GPS_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    startScan();
                } else {
                    reportScanError("no location service allowed");
                }
                break;
            default:
                // ignore this request
                break;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOC_PERMISSION: {
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        startScan();
                        return;
                    }
                }
                reportScanError("no location service allowed");
            }
            default:
        }
    }

    public void didRemoveService() {
    }

    public void didFailToAddServiceWithError() {
    }

    public void onDeviceDidAppear() {
    }

    public void onDeviceDidDisappear() {
    }

    public void onWillStartConnectingToDevice() {

    }

    public void onDidStartInterrogatingDevice() {

    }

    public void onDidFinishInterrogatingDevice() {

    }

    public void didUpdateValueData(byte[] bytes, byte[] bytes1) {

    }

    public void didUpdateInputFormat() {

    }

    /**
     * Report to the webview new state information.
     *
     * @param strg, min. type, state and brickid in this order are reqired (3 arguments). All next arguments have to appear in pairs, key <-> value
     */
    public void reportStateChanged(String type, String state, String brickid, String... strg) {
        try {
            if (type != null && state != null && brickid != null) {
                JSONObject newMsg = new JSONObject();
                newMsg.put("target", ROBOT);
                newMsg.put("type", type);
                newMsg.put("state", state);
                newMsg.put("brickid", brickid);
                if (strg != null) {
                    for (int i = 0; i < strg.length; i += 2) {
                        newMsg.put(strg[i], strg[i + 1]);
                    }
                }
                mainView.loadUrl("javascript:webviewController.appToJsInterface('" + newMsg.toString() + "')");
                Log.d(TAG, newMsg.toString());
            } else {
                throw new IllegalArgumentException("Min. 3 parameters required + additional parameters in pairs!");
            }
        } catch (JSONException | IllegalArgumentException | IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage() + "caused by: " + type + state + brickid + strg);
        }
    }

    private void reportScanError(String msg) {
        try {
            final JSONObject newMsg = new JSONObject();
            newMsg.put("target", ROBOT);
            newMsg.put("type", "scan");
            newMsg.put("state", "error");
            newMsg.put("message", msg);
            this.mainView.post(new Runnable() {
                @Override
                public void run() {
                    mainView.loadUrl("javascript:webviewController.appToJsInterface('" + newMsg.toString() + "')");

                }
            });
            Log.e(TAG, newMsg.toString());
        } catch (JSONException | IllegalArgumentException e) {
            Log.e(TAG, e.getMessage() + "caused by: scan " + msg);
        }
    }

    private class MotionListener{
        MataDevCommunicator MataDevCommunicator;

        MotionListener(MataDevCommunicator MataDevCommunicator) {
            this.MataDevCommunicator = MataDevCommunicator;
        }

        public void didUpdateDistance(float v, float v1) {
            //reportStateChanged("update", Float.toString(v1), motionSensor.getDevice().getDeviceId(), "sensor", motionSensor.getServiceName(), "id", Integer.toString(motionSensor.getDefaultInputFormat().getConnectId()));
        }

        public void didUpdateCount(int i) {
            // not supported in Open Roberta
        }

        public void didUpdateValueData(byte[] bytes, byte[] bytes1) {
            // not supported in Open Roberta
        }

        public void didUpdateInputFormat() {
            // not supported in Open Roberta
        }
    }
}
