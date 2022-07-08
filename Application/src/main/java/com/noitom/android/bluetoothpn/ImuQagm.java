package com.noitom.android.bluetoothpn;

import android.util.Log;

import java.util.Arrays;

public class ImuQagm {
    private final static String TAG = ImuQagm.class.getSimpleName();
    private final static int BYTE_LEN = 28;
    private final static int Q_MULTIPLE = 32768;
    private final static float A_AD = 0.244f;
    private final static float G_AD = 0.07f;
    private final static float M_AD = 0.001f;


    int id;
    int frameNo;
    float[] q; //x,y,z,w
    float[] acc; //x,y,z
    float[] gyro; //x,y,z
    float[] magn; //x,y,z

    @Override
    public String toString() {
        return "ImuQagm{" +
                "id=" + id +
                ", frameNo=" + String.format("%02X ", frameNo) +
                ", q=" + Arrays.toString(q) +
                ", acc=" + Arrays.toString(acc) +
                ", gyro=" + Arrays.toString(gyro) +
                ", magn=" + Arrays.toString(magn) +
                '}';
    }

    public static ImuQagm fromBytes(byte[] value){
        if(value== null || value.length != BYTE_LEN){
            Log.w(TAG, "fromBytes length invalid");
            return null;
        }
        ImuQagm imuQagm = new ImuQagm();
        imuQagm.id = Byte.toUnsignedInt(value[0]);
        imuQagm.frameNo = Byte.toUnsignedInt(value[1]);
        Quaternion q = new Quaternion(bytesToShort(value[4], value[5])/(float)Q_MULTIPLE, bytesToShort(value[6], value[7])/(float)Q_MULTIPLE,
                bytesToShort(value[8], value[9])/(float)Q_MULTIPLE, bytesToShort(value[2], value[3])/(float)Q_MULTIPLE);
        imuQagm.q = new float[]{q.x,q.y,q.z,q.w};
        imuQagm.acc = ADConvert(bytesToShort(value[10], value[11]), bytesToShort(value[12], value[13]), bytesToShort(value[14], value[15]), A_AD);
        imuQagm.gyro = ADConvert(bytesToShort(value[16], value[17]), bytesToShort(value[18], value[19]), bytesToShort(value[20], value[21]), G_AD);
        imuQagm.magn = ADConvert(bytesToShort(value[22], value[23]), bytesToShort(value[24], value[25]), bytesToShort(value[26], value[27]), M_AD);
        return imuQagm;
    }

    // parse 2 bytes to a short. little endian
    public static short bytesToShort(byte b1, byte b2){
        short s = (short) ((b2 & 0xFF) << 8 | (b1 & 0xFF));
        return s;
    }

    public static float[] ADConvert(short x, short y, short z, float adNum){
        return new float[] {x * adNum, y * adNum, z * adNum};
    }

}
