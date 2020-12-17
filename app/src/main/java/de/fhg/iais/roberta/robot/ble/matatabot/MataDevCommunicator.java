package de.fhg.iais.roberta.robot.ble.matatabot;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.webkit.WebView;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Code;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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
    private static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_NOTIFY = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String MATATABOT_LATEST_FIRMWARE_VERSION = "2.2.14";
    private static final String MATATACON_LATEST_FIRMWARE_VERSION = "1.1.0";

    public MataDevCommunicator(ORLabActivity orLabActivity, WebView mainView) {
        this.orLabActivity = orLabActivity;
        this.mainView = mainView;
        this.ROBOT = "matatabot";
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
    @RequiresApi(api = Build.VERSION_CODES.M)
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
        if (scannedDevices.size() != 0) {
            scannedDevices.clear();
        }
        if (this.BleClient != null) {
            this.BleClient.stopSearch();
        }
        Log.d(TAG, "stop scanning");
    }

    private void bleHalWrite(int device_id, ArrayList<Byte> List){
        if (BleClient == null) {
            Log.d(TAG, "bleHalWrite BleClient null!");
            return;
        }
        if (connectedDevices.size() == 0) {
            Log.d(TAG, "bleHalWrite connectedDevices size 0!");
            return;
        }
        if (List.size() == 0) {
            Log.d(TAG, "bleHalWrite List size 0!");
            return;
        }
        if (device_id > connectedDevices.size()) {
            Log.d(TAG, "bleHalWrite device_id less then connectedDevices size!");
            return;
        }
        final Map<String,Object> deviceMapTemp = connectedDevices.get(device_id);
        Log.d(TAG, "deviceMapTemp:" + deviceMapTemp);
        byte[] byteData = new byte[List.size()];
        for (int i = 0; i < List.size(); i++) {
            byteData[i] = List.get(i);
        }
        BleClient.writeNoRsp((String)deviceMapTemp.get("mac"), UUID.fromString(UUID_SERVICE), UUID.fromString(UUID_WRITE), byteData, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == Code.REQUEST_SUCCESS) {
                    Log.d(TAG, "Yanminge === writeNoRsp onResponse");
                }
            }
        });
    }

    private void bleSetNewProtocol(int device_id) {
        CommandProcess.setNewProtocolFlag = true;
        bleHalWrite(device_id, CommandPack.setNewProtocol());
        final int[] count = {0};
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                    if (count[0] > 4000) {
                        Log.d(TAG, "setNewProtocol timeout!");
                        CommandProcess.setNewProtocolFlag = false;
                        timer.cancel();
                    } else if (!CommandProcess.setNewProtocolFlag) {
                        Log.d(TAG, "setNewProtocol success!");
                        bleCheckVersion(device_id);
                        timer.cancel();
                    }
                    count[0] += 10;
                }
            }, 10, 10);
    }

    private void bleCheckVersion(int device_id) {
        CommandProcess.getVersionFlag = true;
        bleHalWrite(device_id, CommandPack.checkVersion());
        final int[] count = {0};
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "checkVersion timeout!");
                    CommandProcess.getVersionFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.getVersionFlag) {
                    String[] version_array;
                    if (CommandProcess.deviceName.equals("MatataBot")) {
                        version_array = MATATABOT_LATEST_FIRMWARE_VERSION.split("\\.");
                    } else {
                        version_array = MATATACON_LATEST_FIRMWARE_VERSION.split("\\.");
                    }
                    int latest_version = (((Integer.parseInt(version_array[0]) & 0xff) << 16) + ((Integer.parseInt(version_array[1])  & 0xff) << 8) | ((Integer.parseInt(version_array[2]))  & 0xff));
                    int current_version = ((int)(CommandProcess.firmwareVersion[0] & 0xff) << 16) + ((int)(CommandProcess.firmwareVersion[1] & 0xff) << 8) + ((int)(CommandProcess.firmwareVersion[2] & 0xff));
                    Log.d(TAG, "latest_version:" + latest_version + ",current_version:" + current_version);
                    if (latest_version > current_version) {
                        //show version waring
                        Log.d(TAG, "the current version need upgrade");
                        close();
                    }
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionForwardStep (int device_id, String steps) {
        final int[] count = {0};
        int step = 1;
        if (steps.equals("STEP1")) {
            step = 1;
        } else if (steps.equals("STEP2")) {
            step = 2;
        } else if (steps.equals("STEP3")) {
            step = 3;
        } else if (steps.equals("STEP4")) {
            step = 4;
        } else if (steps.equals("STEP5")) {
            step = 5;
        } else if (steps.equals("STEP6")) {
            step = 6;
        }
        final int timeout = 2000 * step;
        final int[] movePosCount = {step};
        CommandProcess.motionForwardStepFlag = true;
        bleHalWrite(device_id, CommandPack.motionForwardStep());
        movePosCount[0] = movePosCount[0] - 1;
        final Map<String,Object> deviceMapTemp = connectedDevices.get(device_id);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > timeout) {
                    CommandProcess.motionForwardStepFlag = false;
                    reportStateChanged("commandResponse", "motionForwardStep", (String)deviceMapTemp.get("mac"), "brickname", "timeOut");
                    Log.d(TAG, "motionForwardStep timeout!");
                    timer.cancel();
                } else if (!CommandProcess.motionForwardStepFlag) {
                    if (movePosCount[0] > 0) {
                        bleHalWrite(device_id  , CommandPack.motionForwardStep());
                        movePosCount[0] = movePosCount[0] - 1;
                        CommandProcess.motionForwardStepFlag = true;
                    } else {
                        reportStateChanged("commandResponse", "motionForwardStep", (String)deviceMapTemp.get("mac"), "brickname", "done");
                        Log.d(TAG, "motionForwardStep done");
                        timer.cancel();
                    }
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionBackwardStep (int device_id, int step) {
        final int[] count = {0};
        final int timeout = 2000 * step;
        final int[] movePosCount = {step};
        CommandProcess.motionBackwardStepFlag = true;
        bleHalWrite(device_id, CommandPack.motionBackwardStep());
        movePosCount[0] = movePosCount[0] - 1;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > timeout) {
                    Log.d(TAG, "motionBackwardStep timeout!");
                    CommandProcess.motionBackwardStepFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionBackwardStepFlag) {
                    if (movePosCount[0] > 0) {
                        CommandProcess.motionBackwardStepFlag = true;
                        bleHalWrite(device_id  , CommandPack.motionBackwardStep());
                        movePosCount[0] = movePosCount[0] - 1;
                    } else {
                        timer.cancel();
                    }
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionTurnLeftAngle (int device_id, int moveAngle) {
        final int[] count = {0};
        CommandProcess.motionTurnLeftAngleFlag = true;
        bleHalWrite(device_id, CommandPack.motionTurnLeftAngle(moveAngle));
        Timer timer = new Timer();
        int timeout = 15 * Math.abs(moveAngle);
        if (timeout < 1000) {
            timeout = 1000;
        } else if (timeout > 2700) {
            timeout = 2700;
        }
        int finalTimeout = timeout;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > finalTimeout) {
                    Log.d(TAG, "motionTurnLeftAngle timeout!");
                    CommandProcess.motionTurnLeftAngleFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionTurnLeftAngleFlag) {
                    Log.d(TAG, "motionTurnLeftAngle success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionTurnRightAngle (int device_id, int moveAngle) {
        final int[] count = {0};
        CommandProcess.motionTurnRightAngleFlag = true;
        bleHalWrite(device_id, CommandPack.motionTurnRightAngle(moveAngle));
        Timer timer = new Timer();
        int timeout = 15 * Math.abs(moveAngle);
        if (timeout < 1000) {
            timeout = 1000;
        } else if (timeout > 2700) {
            timeout = 2700;
        }
        int finalTimeout = timeout;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > finalTimeout) {
                    Log.d(TAG, "motionTurnRightAngle timeout!");
                    CommandProcess.motionTurnRightAngleFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionTurnRightAngleFlag) {
                    Log.d(TAG, "motionTurnRightAngle success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionMoving (int device_id, int movePos) {
        final int[] count = {0};
        CommandProcess.motionMovingFlag = true;
        int movePosTemp = movePos;
        if (movePosTemp < -1000) {
            movePosTemp = -1000;
        } else if (movePosTemp > 1000) {
            movePosTemp = 1000;
        }
        bleHalWrite(device_id, CommandPack.motionMoving(movePosTemp));
        Timer timer = new Timer();
        int timeout = 15 * Math.abs(movePosTemp);
        if (timeout < 1000) {
            timeout = 1000;
        }
        int finalTimeout = timeout;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > finalTimeout) {
                    Log.d(TAG, "motionMoving timeout!");
                    CommandProcess.motionMovingFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionMovingFlag) {
                    Log.d(TAG, "motionMoving success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionWhirl (int device_id, int moveAngle) {
        final int[] count = {0};
        CommandProcess.motionWhirlFlag = true;
        int moveAngleTemp = moveAngle;
        if (moveAngleTemp < -360) {
            moveAngleTemp = -360;
        } else if (moveAngleTemp > 360) {
            moveAngleTemp = 360;
        }
        bleHalWrite(device_id, CommandPack.motionWhirl(moveAngleTemp));
        Timer timer = new Timer();
        int timeout = 15 * Math.abs(moveAngleTemp);
        if (timeout < 1000) {
            timeout = 1000;
        }
        int finalTimeout = timeout;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > finalTimeout) {
                    Log.d(TAG, "motionWhirl timeout!");
                    CommandProcess.motionWhirlFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionWhirlFlag) {
                    Log.d(TAG, "motionWhirl success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionSingleWheelSpeed (int device_id, int wheelChannel, int Speed) {
        final int[] count = {0};
        CommandProcess.motionSingleWheelSpeedFlag = true;
        int SpeedTemp = Speed;
        if (SpeedTemp < -245) {
            SpeedTemp = -245;
        } else if (SpeedTemp > 245) {
            SpeedTemp = 245;
        }
        bleHalWrite(device_id, CommandPack.motionSingleWheelSpeed(wheelChannel, SpeedTemp));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "motionSingleWheelSpeed timeout!");
                    CommandProcess.motionSingleWheelSpeedFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionSingleWheelSpeedFlag) {
                    Log.d(TAG, "motionSingleWheelSpeed success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionAllWheelSpeed (int device_id, int leftSpeed, int rightSpeed) {
        final int[] count = {0};
        CommandProcess.motionAllWheelSpeedFlag = true;
        int leftSpeedTemp = leftSpeed;
        int rightSpeedTemp = rightSpeed;
        if (leftSpeedTemp < -17) {
            leftSpeedTemp = -17;
        } else if (leftSpeedTemp > 17) {
            leftSpeedTemp = 17;
        }
        if (rightSpeedTemp < -17) {
            rightSpeedTemp = -17;
        } else if (rightSpeedTemp > 17) {
            rightSpeedTemp = 17;
        }
        int leftSpeedValue = (int)Math.round(144.0 * leftSpeedTemp / 9.7);
        int rightSpeedValue = (int)Math.round(144.0 * rightSpeedTemp / 9.7);
        bleHalWrite(device_id, CommandPack.motionAllWheelSpeed(leftSpeedValue, rightSpeedValue));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "motionAllWheelSpeed timeout!");
                    CommandProcess.motionAllWheelSpeedFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionAllWheelSpeedFlag) {
                    Log.d(TAG, "motionAllWheelSpeed success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleMotionStopMoving (int device_id, int channel) {
        final int[] count = {0};
        CommandProcess.motionStopMovingFlag = true;
        bleHalWrite(device_id, CommandPack.motionStopMoving(channel));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "motionStopMoving timeout!");
                    CommandProcess.motionStopMovingFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.motionStopMovingFlag) {
                    Log.d(TAG, "motionStopMoving success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleDoDance (int device_id, int danceIndex) {
        final int[] count = {0};
        CommandProcess.doDanceFlag = true;
        bleHalWrite(device_id, CommandPack.doDance(danceIndex));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "doDance timeout!");
                    CommandProcess.doDanceFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.doDanceFlag) {
                    Log.d(TAG, "doDance success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleDoAction (int device_id, int actionIndex) {
        final int[] count = {0};
        CommandProcess.doActionFlag = true;
        bleHalWrite(device_id, CommandPack.doAction(actionIndex));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 1000) {
                    Log.d(TAG, "doAction timeout!");
                    CommandProcess.doActionFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.doActionFlag) {
                    Log.d(TAG, "doAction success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleSoundAlto (int device_id, int toneFreq, int beatTime) {
        final int[] count = {0};
        CommandProcess.soundAltoFlag = true;
        bleHalWrite(device_id, CommandPack.soundAlto(toneFreq, beatTime));
        Timer timer = new Timer();
        int timeout = beatTime * 500 + 100;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > timeout) {
                    Log.d(TAG, "soundAlto timeout!");
                    CommandProcess.soundAltoFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.soundAltoFlag) {
                    Log.d(TAG, "soundAlto success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleSoundTreble (int device_id, int toneFreq, int beatTime) {
        final int[] count = {0};
        CommandProcess.soundTrebleFlag = true;
        bleHalWrite(device_id, CommandPack.soundTreble(toneFreq, beatTime));
        Timer timer = new Timer();
        int timeout = beatTime * 500 + 100;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > timeout) {
                    Log.d(TAG, "soundTreble timeout!");
                    CommandProcess.soundTrebleFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.soundTrebleFlag) {
                    Log.d(TAG, "soundTreble success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleSoundMelody (int device_id, int melodyIndex) {
        final int[] count = {0};
        CommandProcess.soundMelodyFlag = true;
        bleHalWrite(device_id, CommandPack.soundMelody(melodyIndex));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 10000) {
                    Log.d(TAG, "soundMelody timeout!");
                    CommandProcess.soundMelodyFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.soundMelodyFlag) {
                    Log.d(TAG, "soundMelody success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleSoundSong (int device_id, int songIndex) {
        final int[] count = {0};
        CommandProcess.soundSongFlag = true;
        bleHalWrite(device_id, CommandPack.soundSong(songIndex));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 20000) {
                    Log.d(TAG, "soundSong timeout!");
                    CommandProcess.soundSongFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.soundSongFlag) {
                    Log.d(TAG, "soundSong success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleEyeLedSingleSet (int device_id, int ledIndex, int redValue, int greenValue, int blueValue) {
        final int[] count = {0};
        CommandProcess.eyeLedSingleSetFlag = true;
        bleHalWrite(device_id, CommandPack.eyeLedSingleSet(ledIndex, redValue, greenValue, blueValue));
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 2000) {
                    Log.d(TAG, "eyeLedSingleSet timeout!");
                    CommandProcess.eyeLedSingleSetFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.eyeLedSingleSetFlag) {
                    Log.d(TAG, "eyeLedSingleSet success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
    }

    private void bleEyeLedAllOff (int device_id) {
        final int[] count = {0};
        CommandProcess.eyeLedAllOffFlag = true;
        bleHalWrite(device_id, CommandPack.eyeLedAllOff());
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 2000) {
                    Log.d(TAG, "eyeLedAllOff timeout!");
                    CommandProcess.eyeLedAllOffFlag = false;
                    timer.cancel();
                } else if (!CommandProcess.eyeLedAllOffFlag) {
                    Log.d(TAG, "eyeLedAllOff success!");
                    timer.cancel();
                }
                count[0] += 5;
            }
        }, 2, 5);
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
                        BleClient.notify((String)deviceMapTemp.get("mac"),  UUID.fromString(UUID_SERVICE), UUID.fromString(UUID_NOTIFY), new BleNotifyResponse() {
                            @Override
                            public void onNotify(UUID service, UUID character, byte[] value) {
                                ArrayList<Byte> valueList = new ArrayList<Byte>();
                                for(byte bdt : value) {
                                    valueList.add(bdt);
                                }
                                CommandProcess.depackCommand(valueList);
                            }
                            @Override
                            public void onResponse(int code) {
                                if (code == Code.REQUEST_SUCCESS) {
                                    Log.d(TAG, "Yanminge === onNotify onResponse");
                                }
                            }
                        });
                        bleSetNewProtocol(0);
                    }
                }
            } else if (status == Constants.STATUS_DISCONNECTED) {
                Log.d(TAG, "STATUS_DISCONNECTED device:" + mac);
                for (int i = 0; i < connectedDevices.size(); i++) {
                    final Map<String,Object> deviceMapTemp = connectedDevices.get(i);
                    if (deviceMapTemp.get("mac").equals(mac)) {
                        connectedDevices.remove(i);
                        reportStateChanged("connect", "disconnected", (String)deviceMapTemp.get("mac"));
                        try {
                            BleClient.unregisterConnectStatusListener((String)deviceMapTemp.get("mac"), mBleConnectStatusListener);
                            BleClient.unnotify((String)deviceMapTemp.get("mac"),  UUID.fromString(UUID_SERVICE), UUID.fromString(UUID_NOTIFY), new BleUnnotifyResponse() {
                                @Override
                                public void onResponse(int code) {
                                    if (code == Code.REQUEST_SUCCESS) {
                                        Log.d(TAG, "Yanminge === unnotify onResponse");
                                    }
                                }
                            });
                        }catch (Exception E) {

                        }
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
                                switch (msg.getString("action")) {
                                    case "motionForwardStep":
                                        bleMotionForwardStep(0, msg.getString("step"));
                                        break;
                                    default:
                                        break;
                                }
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
        }
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
                Log.d(TAG, newMsg.toString());
                this.mainView.post(new Runnable() {
                    @Override
                    public void run() {
                        mainView.loadUrl("javascript:webviewController.appToJsInterface('" + newMsg.toString() + "')");
                    }
                });
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
