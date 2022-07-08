package com.noitom.android.bluetoothpn;

import android.util.Log;

public class Quaternion {
    private final static String TAG = Quaternion.class.getSimpleName();

    public float x;
    public float y;
    public float z;
    public float w;

    public Quaternion(){

    }

    public Quaternion(float x, float y, float z, float w){
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    static void vectorRotation(float[] vector, Quaternion q){
        Quaternion qv = new Quaternion(vector[0], vector[1], vector[2], 0);
        //四元数旋转公式 q0*qv*(q0逆）
        qv = Quaternion.multiplication(Quaternion.multiplication(q, qv), q.inverse());
        vector[0] = qv.x;
        vector[1] = qv.y;
        vector[2] = qv.z;

    }

    public EulerAngles toEulerAngles(){
        // roll (x-axis rotation)
        return new EulerAngles(this.x, this.y, this.z, this.w);
    }

    static Quaternion multiplication(Quaternion q0, Quaternion q1){
        Quaternion ret = new Quaternion();
        ret.w = q0.w * q1.w - q0.x * q1.x - q0.y * q1.y - q0.z * q1.z;
        ret.x = q0.w * q1.x + q0.x * q1.w + q0.y * q1.z - q0.z * q1.y;
        ret.y = q0.w * q1.y + q0.y * q1.w + q0.z * q1.x - q0.x * q1.z;
        ret.z = q0.w * q1.z + q0.z * q1.w + q0.x * q1.y - q0.y * q1.x;
        return ret;
    }

    public Quaternion inverse(){
        Quaternion ret;
        ret = this;
        ret.x *= -1;
        ret.y *= -1;
        ret.z *= -1;
        return ret;
    }

    public void displayAngle(){
        float scale = (float)(180/Math.PI);

        double angle = Math.acos(w) * scale * 2;
        Log.i(TAG, "QAGM:QA:"+angle);
    }
}
