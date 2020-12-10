package de.fhg.iais.roberta.robot.ble.matatabot;

import android.util.Log;

import java.util.ArrayList;

public class CommandProcess {
    private static final String TAG = "CommandProcess";
    private static ArrayList<Byte> _receivedCommand = new ArrayList<Byte>();
    private static int _lastFrameReservedData = -1;
    private static int _receivedCommandLength = -1;
    private static boolean _receivedCommandStart = false;

    public static byte[] firmwareVersion = new byte[3];
    public static String deviceName = "MatataBot";
    public static boolean motionForwardStepFlag = false;
    public static boolean motionBackwardStepFlag = false;
    public static boolean motionTurnLeftAngleFlag = false;
    public static boolean motionTurnRightAngleFlag = false;
    public static boolean motionMovingFlag = false;
    public static boolean motionWhirlFlag = false;
    public static boolean motionSingleWheelSpeedFlag = false;
    public static boolean motionAllWheelSpeedFlag = false;
    public static boolean motionStopMovingFlag = false;
    public static boolean doDanceFlag = false;
    public static boolean doActionFlag = false;
    public static boolean soundAltoFlag = false;
    public static boolean soundTrebleFlag = false;
    public static boolean soundMelodyFlag = false;
    public static boolean soundSongFlag = false;
    public static boolean eyeLedSingleSetFlag = false;
    public static boolean eyeLedAllOffFlag = false;
    public static boolean setNewProtocolFlag = false;
    public static boolean getVersionFlag = false;

    public interface BLECommand {
        public  static final byte CMD_CHECK_VERSION    =   0x01;
        public  static final byte CMD_MOVE_POS         =   0x10;
        public  static final byte CMD_MOVE_SPEED       =   0x11;
        public  static final byte CMD_DANCE            =   0x12;
        public  static final byte CMD_ACTION           =   0x13;
        public  static final byte CMD_PLAY_TONE        =   0x15;
        public  static final byte CMD_PLAY_MUSIC       =   0x16;
        public  static final byte CMD_EYE_LED          =   0x17;
        public  static final byte CMD_SET_NEW_PROTOCOL =   0x7e;
        public  static final byte CMD_HEARTBEAT        =   (byte)0x87;
        public  static final byte CMD_GENERAL_RSP      =   (byte)0x88;
    }

    public interface MovePosCommand {
        public  static final byte MOVE_FORWARD         =   0x01;
        public  static final byte MOVE_BACKWARD        =   0x02;
        public  static final byte MOVE_LEFT            =   0x03;
        public  static final byte MOVE_RIGHT           =   0x04;
    }

    public interface MoveSpeedCommand {
        public  static final byte MOTION_LEFT          =   0x01;
        public  static final byte MOTION_RIGHT         =   0x02;
        public  static final byte MOTION_BOTH          =   0x03;
    }

    public interface PlayMusicCommand {
        public  static final byte PLAY_UNTIL_DONE     =   0x01;
        public  static final byte PLAY_NORMAL         =   0x02;
        public  static final byte PLAY_LOOP           =   0x03;
        public  static final byte PLAY_STOP           =   0x04;
    }

    public static int crc16(ArrayList<Byte> bufferList, int crc_init) {
        int crc = crc_init & 0xffff;
        for (int i = 0; i < bufferList.size(); i++) {
            int tdata = (byte)bufferList.get(i);
            if(tdata < 0) {
                tdata = 256 + tdata;
            }
            crc = ((crc >> 8) | (crc << 8)) & 0xffff;
            crc ^= tdata & 0xffff;
            crc ^= ((crc & 0xff) >> 4) & 0xffff;
            crc ^= ((crc << 8) << 4) & 0xffff;
            crc ^= (((crc & 0xff) << 4) << 1) & 0xffff;
        }
        return crc;
    }

    public static ArrayList<Byte> packCommand(ArrayList<Byte> command_data) {
        ArrayList<Byte> command_array = new ArrayList<Byte>();
        int message_len = command_data.size() + 2;
        ArrayList<Byte> message_len_array = new ArrayList<Byte>();
        message_len_array.add((byte) (message_len & 0xff));
        int crc = crc16(message_len_array, 0xffff);
        crc = crc16(command_data, crc);
        command_array.add((byte) 0xfe);
        command_array.add((byte) (message_len & 0xff));
        for (byte b : command_data) {
            if (b == (byte) 0xfe) {
                command_array.add((byte) 0xfd);
                command_array.add((byte) 0xde);
            } else if (b == (byte) 0xfd) {
                command_array.add((byte) 0xfd);
                command_array.add((byte) 0xdd);
            } else {
                command_array.add((byte) b);
            }
        }
        if (((byte) (crc >> 8)) == (byte) 0xfe) {
            command_array.add((byte) 0xfd);
            command_array.add((byte) 0xde);
        } else if (((byte) (crc >> 8)) == (byte) 0xfd) {
            command_array.add((byte) 0xfd);
            command_array.add((byte) 0xdd);
        } else {
            command_array.add((byte) (crc >> 8));
        }
        if (((byte) (crc & 0xff)) == (byte) 0xfe) {
            command_array.add((byte) 0xfd);
            command_array.add((byte) 0xde);
        } else if (((byte) (crc & 0xff)) == (byte) 0xfd) {
            command_array.add((byte) 0xfd);
            command_array.add((byte) 0xdd);
        } else {
            command_array.add((byte) (crc & 0xff));
        }
        String log_string = "send: ";
        log_string = log_string + (DataProcess.byteList2hex(command_array));
        Log.d(TAG, "initialized" + log_string);
        return command_array;
    }

    public static boolean checkCRC() {
        ArrayList<Byte> crc_data_temp = new ArrayList<Byte>();
        for (int i = 1; i < _receivedCommandLength; i++) {
            crc_data_temp.add(_receivedCommand.get(i));
        }
        Log.d(TAG, "crc_data_temp:" + DataProcess.byteList2hex(crc_data_temp));
        int crc_calculation = crc16(crc_data_temp, 0xffff);
        int crc_received = (_receivedCommand.get(_receivedCommandLength) << 8) & 0xff00 | (_receivedCommand.get(_receivedCommandLength + 1) & 0xff);
        Log.d(TAG, "crc_calculation:" + crc_calculation);
        Log.d(TAG, "crc_received:" + crc_received);
        return crc_calculation == crc_received;
    }

    public static void parseCommand() {
        if (!checkCRC()) {
            Log.d(TAG, "checkCRC false!");
            _receivedCommand.clear();
            _receivedCommandLength = 0;
            _receivedCommandStart = false;
        }
        Log.d(TAG, "_receivedCommand:" + DataProcess.byteList2hex(_receivedCommand));
        switch((byte)_receivedCommand.get(2)) {
            case BLECommand.CMD_SET_NEW_PROTOCOL: {
                Log.d(TAG, "set new protocol response!");
                if (_receivedCommand.get(3) == 0x01) {
                    deviceName = "MatataBot";
                    if (_receivedCommand.get(4) == 0x01) {
                        Log.d(TAG, "MatataBot not supoort new protocol");
                    }
                } else if (_receivedCommand.get(3) == 0x02){
                    if (_receivedCommand.get(5) == 0x01) {
                        Log.d(TAG, "MatataCon not supoort new protocol");
                    }
                    deviceName = "MatataCon";
                }
                setNewProtocolFlag = false;
                break;
            }
            case BLECommand.CMD_GENERAL_RSP: {
                Log.d(TAG, "get general response!");
                break;
            }
            case BLECommand.CMD_CHECK_VERSION: {
                Log.d(TAG, "CMD_CHECK_VERSION");
                firmwareVersion[0] = _receivedCommand.get(4);
                firmwareVersion[1] = _receivedCommand.get(5);
                firmwareVersion[2] = _receivedCommand.get(6);
                String version = Integer.toString(firmwareVersion[0] & 0xff) + '.' + Integer.toString(firmwareVersion[1] & 0xff) + '.' + Integer.toString(firmwareVersion[2] & 0xff);
                Log.d(TAG, "version:" + version);
                getVersionFlag = false;
                break;
            }
            case BLECommand.CMD_HEARTBEAT: {
                if (_receivedCommand.get(3) == (byte)0x02) {
                    deviceName = "MatataCon";
                } else if (deviceName.equals("MatataCon")) {
                    deviceName = "MatataAll";
                }
                Log.d(TAG, "CMD_HEARTBEAT:" + deviceName);
                break;
            }
            default: {
                break;
            }
        }
        _receivedCommand.clear();
        _receivedCommandLength = 0;
        _receivedCommandStart = false;
    }

    public static void depackCommand(ArrayList<Byte> command_data) {
        ArrayList<Byte> command_data_temp = new ArrayList<Byte>();
        if (_lastFrameReservedData != -1) {
            command_data_temp.add((byte) (_lastFrameReservedData & 0xff));
            _lastFrameReservedData = -1;
        }
        command_data_temp.addAll(command_data);
        for (int i = 0; i < command_data_temp.size(); i++) {
            if ((command_data_temp.get(i) == (byte) 0xfe) && (!_receivedCommandStart)) {
                _receivedCommand.add((byte) 0xfe);
                _receivedCommandStart = true;
            } else if (_receivedCommandStart) {
                if (command_data_temp.get(i) == (byte) 0xfd) {
                    if (i == command_data_temp.size() - 1) {
                        _lastFrameReservedData = 0xfd;
                        continue;
                    } else if (command_data_temp.get(i + 1) == (byte) 0xdd) {
                        _receivedCommand.add((byte) 0xfd);
                        i++;
                        continue;
                    } else if (command_data_temp.get(i + 1) == (byte) 0xde) {
                        _receivedCommand.add((byte) 0xfe);
                        i++;
                        continue;
                    }
                    _receivedCommand.add((byte) 0xfd);
                } else {
                    _receivedCommand.add(command_data_temp.get(i));
                }
                if (_receivedCommand.size() > 1) {
                    _receivedCommandLength = _receivedCommand.get(1) & 0xff;
                    if(_receivedCommand.size() == _receivedCommandLength + 2) {
                        parseCommand();
                    }
                }
            }
        }
    }
}