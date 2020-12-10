package de.fhg.iais.roberta.robot.ble.matatabot;

import java.util.ArrayList;

public class CommandPack {
    private static final String TAG = "CommandPack";
    public static ArrayList<Byte> setNewProtocol() {
        ArrayList<Byte> setNewProtocolDataTemp = new ArrayList<Byte>();
        setNewProtocolDataTemp.add((byte)CommandProcess.BLECommand.CMD_SET_NEW_PROTOCOL);
        setNewProtocolDataTemp.add((byte)0x02);
        setNewProtocolDataTemp.add((byte)0x02);
        setNewProtocolDataTemp.add((byte)0x00);
        setNewProtocolDataTemp.add((byte)0x00);
        ArrayList<Byte> setNewProtocolData;
        setNewProtocolData = CommandProcess.packCommand(setNewProtocolDataTemp);
        return setNewProtocolData;
    }

    public static ArrayList<Byte> checkVersion() {
        ArrayList<Byte> checkVersionDataTemp = new ArrayList<Byte>();
        checkVersionDataTemp.add((byte)CommandProcess.BLECommand.CMD_CHECK_VERSION);
        checkVersionDataTemp.add((byte)0x01);
        ArrayList<Byte> checkVersionData;
        checkVersionData = CommandProcess.packCommand(checkVersionDataTemp);
        return checkVersionData;
    }

    public static ArrayList<Byte> motionForwardStep() {
        ArrayList<Byte> motionForwardStepDataTemp = new ArrayList<Byte>();

        motionForwardStepDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        motionForwardStepDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_FORWARD);
        // 100mm move pos
        motionForwardStepDataTemp.add((byte)0x00);
        motionForwardStepDataTemp.add((byte)0x64);

        ArrayList<Byte> motionForwardStepData;
        motionForwardStepData = CommandProcess.packCommand(motionForwardStepDataTemp);
        return motionForwardStepData;
    }

    public static ArrayList<Byte> motionBackwardStep() {
        ArrayList<Byte> motionBackwardStepDataTemp = new ArrayList<Byte>();

        motionBackwardStepDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        motionBackwardStepDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_BACKWARD);
        // 100mm move pos
        motionBackwardStepDataTemp.add((byte)0x00);
        motionBackwardStepDataTemp.add((byte)0x64);

        ArrayList<Byte> motionBackwardStepData;
        motionBackwardStepData = CommandProcess.packCommand(motionBackwardStepDataTemp);
        return motionBackwardStepData;
    }

    public static ArrayList<Byte> motionTurnLeftAngle(int moveAngle) {
        ArrayList<Byte> motionTurnLeftAngleDataTemp = new ArrayList<Byte>();

        motionTurnLeftAngleDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        motionTurnLeftAngleDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_LEFT);
        motionTurnLeftAngleDataTemp.add((byte)0x00);
        motionTurnLeftAngleDataTemp.add((byte)(moveAngle & 0xff));

        ArrayList<Byte> motionTurnLeftAngleData;
        motionTurnLeftAngleData = CommandProcess.packCommand(motionTurnLeftAngleDataTemp);
        return motionTurnLeftAngleData;
    }

    public static ArrayList<Byte> motionTurnRightAngle(int moveAngle) {
        ArrayList<Byte> motionTurnRightAngleDataTemp = new ArrayList<Byte>();

        motionTurnRightAngleDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        motionTurnRightAngleDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_RIGHT);
        motionTurnRightAngleDataTemp.add((byte)0x00);
        motionTurnRightAngleDataTemp.add((byte)(moveAngle & 0xff));

        ArrayList<Byte> motionTurnRightAngleData;
        motionTurnRightAngleData = CommandProcess.packCommand(motionTurnRightAngleDataTemp);
        return motionTurnRightAngleData;
    }

    public static ArrayList<Byte> motionMoving(int movePos) {
        ArrayList<Byte> motionMovingDataTemp = new ArrayList<Byte>();

        motionMovingDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        if (movePos >= 0) {
            motionMovingDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_FORWARD);
        } else {
            motionMovingDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_BACKWARD);
        }
        motionMovingDataTemp.add((byte)((Math.abs(movePos) & 0xff00) >> 8));
        motionMovingDataTemp.add((byte)(Math.abs(movePos) & 0xff));

        ArrayList<Byte> motionMovingData;
        motionMovingData = CommandProcess.packCommand(motionMovingDataTemp);
        return motionMovingData;
    }

    public static ArrayList<Byte> motionWhirl(int moveAngle) {
        ArrayList<Byte> motionWhirlDataTemp = new ArrayList<Byte>();

        motionWhirlDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_POS);
        if (moveAngle >= 0) {
            motionWhirlDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_RIGHT);
        } else {
            motionWhirlDataTemp.add((byte)CommandProcess.MovePosCommand.MOVE_LEFT);
        }
        motionWhirlDataTemp.add((byte)((Math.abs(moveAngle) & 0xff00) >> 8));
        motionWhirlDataTemp.add((byte)(Math.abs(moveAngle) & 0xff));

        ArrayList<Byte> motionWhirlData;
        motionWhirlData = CommandProcess.packCommand(motionWhirlDataTemp);
        return motionWhirlData;
    }

    public static ArrayList<Byte> motionSingleWheelSpeed(int wheelChannel, int Speed) {
        ArrayList<Byte> motionSingleWheelSpeedDataTemp = new ArrayList<Byte>();

        motionSingleWheelSpeedDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_SPEED);
        if (wheelChannel == CommandProcess.MoveSpeedCommand.MOTION_LEFT) {
            motionSingleWheelSpeedDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_LEFT);
        } else {
            motionSingleWheelSpeedDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_RIGHT);
        }
        if (Speed >= 0) {
            motionSingleWheelSpeedDataTemp.add((byte)0x01);
        } else {
            motionSingleWheelSpeedDataTemp.add((byte)0x02);
        }
        motionSingleWheelSpeedDataTemp.add((byte)((Math.abs(Speed) & 0xff00) >> 8));
        motionSingleWheelSpeedDataTemp.add((byte)(Math.abs(Speed) & 0xff));

        ArrayList<Byte> motionSingleWheelSpeedData;
        motionSingleWheelSpeedData = CommandProcess.packCommand(motionSingleWheelSpeedDataTemp);
        return motionSingleWheelSpeedData;
    }

    public static ArrayList<Byte> motionAllWheelSpeed(int leftSpeed, int rightSpeed) {
        ArrayList<Byte> motionAllWheelSpeedDataTemp = new ArrayList<Byte>();

        motionAllWheelSpeedDataTemp.add((byte)CommandProcess.BLECommand.CMD_MOVE_SPEED);
        motionAllWheelSpeedDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_BOTH);
        if (leftSpeed >= 0) {
            motionAllWheelSpeedDataTemp.add((byte)0x01);
        } else {
            motionAllWheelSpeedDataTemp.add((byte)0x02);
        }
        motionAllWheelSpeedDataTemp.add((byte)((Math.abs(leftSpeed) & 0xff00) >> 8));
        motionAllWheelSpeedDataTemp.add((byte)(Math.abs(leftSpeed) & 0xff));
        if (rightSpeed >= 0) {
            motionAllWheelSpeedDataTemp.add((byte)0x01);
        } else {
            motionAllWheelSpeedDataTemp.add((byte)0x02);
        }
        motionAllWheelSpeedDataTemp.add((byte)((Math.abs(rightSpeed) & 0xff00) >> 8));
        motionAllWheelSpeedDataTemp.add((byte)(Math.abs(rightSpeed) & 0xff));

        ArrayList<Byte> motionAllWheelSpeedData;
        motionAllWheelSpeedData = CommandProcess.packCommand(motionAllWheelSpeedDataTemp);
        return motionAllWheelSpeedData;
    }

    public static ArrayList<Byte> motionStopMoving(int wheelChannel) {
        ArrayList<Byte> motionStopMovingDataTemp = new ArrayList<Byte>();

        motionStopMovingDataTemp.add(CommandProcess.BLECommand.CMD_MOVE_SPEED);
        if (wheelChannel == 0x01) {
            motionStopMovingDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_LEFT);
            motionStopMovingDataTemp.add((byte)0x01);
            motionStopMovingDataTemp.add((byte)0x00);
            motionStopMovingDataTemp.add((byte)0x00);
        } else if (wheelChannel == 0x02) {
            motionStopMovingDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_RIGHT);
            motionStopMovingDataTemp.add((byte)0x01);
            motionStopMovingDataTemp.add((byte)0x00);
            motionStopMovingDataTemp.add((byte)0x00);
        } else if (wheelChannel == 0x03) {
            motionStopMovingDataTemp.add((byte)CommandProcess.MoveSpeedCommand.MOTION_BOTH);
            motionStopMovingDataTemp.add((byte)0x01);
            motionStopMovingDataTemp.add((byte)0x00);
            motionStopMovingDataTemp.add((byte)0x00);
            motionStopMovingDataTemp.add((byte)0x01);
            motionStopMovingDataTemp.add((byte)0x00);
            motionStopMovingDataTemp.add((byte)0x00);
        }

        ArrayList<Byte> motionStopMovingData;
        motionStopMovingData = CommandProcess.packCommand(motionStopMovingDataTemp);
        return motionStopMovingData;
    }

    public static ArrayList<Byte> doDance(int danceIndex) {
        ArrayList<Byte> doDanceDataTemp = new ArrayList<Byte>();

        doDanceDataTemp.add(CommandProcess.BLECommand.CMD_DANCE);
        doDanceDataTemp.add((byte)(danceIndex & 0xff));

        ArrayList<Byte> doDanceData;
        doDanceData = CommandProcess.packCommand(doDanceDataTemp);
        return doDanceData;
    }

    public static ArrayList<Byte> doAction(int actionIndex) {
        ArrayList<Byte> doActionDataTemp = new ArrayList<Byte>();

        doActionDataTemp.add(CommandProcess.BLECommand.CMD_ACTION);
        doActionDataTemp.add((byte)(actionIndex & 0xff));

        ArrayList<Byte> doActionData;
        doActionData = CommandProcess.packCommand(doActionDataTemp);
        return doActionData;
    }

    public static ArrayList<Byte> soundAlto(int toneFreq, int beatTime) {
        ArrayList<Byte> soundAltoDataTemp = new ArrayList<Byte>();

        soundAltoDataTemp.add(CommandProcess.BLECommand.CMD_PLAY_TONE);
        soundAltoDataTemp.add((byte)((toneFreq & 0xff00) >> 8));
        soundAltoDataTemp.add((byte)(toneFreq & 0x00ff));
        soundAltoDataTemp.add((byte)(((beatTime * 500) & 0xff00) >> 8));
        soundAltoDataTemp.add((byte)((beatTime * 500) & 0xff));

        ArrayList<Byte> soundAltoData;
        soundAltoData = CommandProcess.packCommand(soundAltoDataTemp);
        return soundAltoData;
    }

    public static ArrayList<Byte> soundTreble(int toneFreq, int beatTime) {
        ArrayList<Byte> soundTrebleDataTemp = new ArrayList<Byte>();

        soundTrebleDataTemp.add(CommandProcess.BLECommand.CMD_PLAY_TONE);
        soundTrebleDataTemp.add((byte)((toneFreq & 0xff00) >> 8));
        soundTrebleDataTemp.add((byte)(toneFreq & 0x00ff));
        soundTrebleDataTemp.add((byte)(((beatTime * 500) & 0xff00) >> 8));
        soundTrebleDataTemp.add((byte)((beatTime * 500) & 0xff));

        ArrayList<Byte> soundTrebleData;
        soundTrebleData = CommandProcess.packCommand(soundTrebleDataTemp);
        return soundTrebleData;
    }

    public static ArrayList<Byte> soundMelody(int melodyIndex) {
        ArrayList<Byte> soundMelodyDataTemp = new ArrayList<Byte>();

        soundMelodyDataTemp.add(CommandProcess.BLECommand.CMD_PLAY_MUSIC);
        soundMelodyDataTemp.add(CommandProcess.PlayMusicCommand.PLAY_UNTIL_DONE);
        soundMelodyDataTemp.add((byte)(melodyIndex & 0xff));

        ArrayList<Byte> soundMelodyData;
        soundMelodyData = CommandProcess.packCommand(soundMelodyDataTemp);
        return soundMelodyData;
    }

    public static ArrayList<Byte> soundSong(int songIndex) {
        ArrayList<Byte> soundSongDataTemp = new ArrayList<Byte>();

        soundSongDataTemp.add(CommandProcess.BLECommand.CMD_PLAY_MUSIC);
        soundSongDataTemp.add(CommandProcess.PlayMusicCommand.PLAY_UNTIL_DONE);
        soundSongDataTemp.add((byte)(songIndex & 0xff));

        ArrayList<Byte> soundSongData;
        soundSongData = CommandProcess.packCommand(soundSongDataTemp);
        return soundSongData;
    }

    public static ArrayList<Byte> eyeLedSingleSet(int ledIndex, int redValue, int greenValue, int blueValue) {
        ArrayList<Byte> eyeLedSingleSetDataTemp = new ArrayList<Byte>();

        eyeLedSingleSetDataTemp.add(CommandProcess.BLECommand.CMD_EYE_LED);
        eyeLedSingleSetDataTemp.add((byte)(ledIndex & 0xff));
        eyeLedSingleSetDataTemp.add((byte)(redValue & 0xff));
        eyeLedSingleSetDataTemp.add((byte)(greenValue & 0xff));
        eyeLedSingleSetDataTemp.add((byte)(blueValue & 0xff));

        ArrayList<Byte> eyeLedSingleSetData;
        eyeLedSingleSetData = CommandProcess.packCommand(eyeLedSingleSetDataTemp);
        return eyeLedSingleSetData;
    }


    public static ArrayList<Byte> eyeLedAllOff() {
        ArrayList<Byte> eyeLedAllOffDataTemp = new ArrayList<Byte>();

        eyeLedAllOffDataTemp.add(CommandProcess.BLECommand.CMD_EYE_LED);
        eyeLedAllOffDataTemp.add((byte)0x03);
        eyeLedAllOffDataTemp.add((byte)0x00);
        eyeLedAllOffDataTemp.add((byte)0x00);
        eyeLedAllOffDataTemp.add((byte)0x00);

        ArrayList<Byte> eyeLedAllOffData;
        eyeLedAllOffData = CommandProcess.packCommand(eyeLedAllOffDataTemp);
        return eyeLedAllOffData;
    }
}
