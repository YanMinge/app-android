package de.fhg.iais.roberta.robot.ble.matatabot;

import java.util.ArrayList;

public class DataProcess {
    private static final String TAG = "DataProcess";
    public static String bytes2hex(byte[] bytesData) {
        if (bytesData == null) {
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        String tmp = null;
        for (byte b : bytesData) {
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            hexString.append(tmp);
        }
        return hexString.toString();
    }

    public static String byteList2hex(ArrayList<Byte> List) {
        if (List.size() == 0) {
            return null;
        }
        StringBuilder hexString = new StringBuilder();
        String tmp = null;
        for (int i = 0; i < List.size(); i++) {
            tmp = Integer.toHexString(0xFF & (int)List.get(i));
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            hexString.append(tmp);
        }
        return hexString.toString();
    }
}
