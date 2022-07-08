/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This class is based android open source project(https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/os/RotationVectorDemo.java)
 * Copyright © 2015-2022 Noitom Ltd.
 */

package com.noitom.android.bluetoothpn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MenuItem;

public class DeviceViewActivity extends Activity {
    private final static String TAG = DeviceViewActivity.class.getSimpleName();

    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;

    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS);

        // Create our Preview view and set it as the content of our Activity
        mRenderer = new MyRenderer();
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(mRenderer);
        setContentView(mGLSurfaceView);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();

        mRenderer.start();
        mGLSurfaceView.onResume();
    }
    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();

        mRenderer.stop();
        mGLSurfaceView.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class MyRenderer extends BroadcastReceiver implements GLSurfaceView.Renderer {
        private Cube mCube;
        private final float[] mRotationMatrix = new float[16];
        public MyRenderer() {
            mCube = new Cube();
            // initialize the rotation matrix to identity
            mRotationMatrix[ 0] = 1;
            mRotationMatrix[ 4] = 1;
            mRotationMatrix[ 8] = 1;
            mRotationMatrix[12] = 1;
        }
        public void start() {
            // enable our sensor when the activity is resumed
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
            registerReceiver(this, intentFilter);
        }
        public void stop() {
            unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                handleData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_UUID), intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }

        private void handleData(String charaUuid, byte[] value){
            if(PnGattAttributes.IMU_DATA_TRANS.equals(charaUuid)){
                ImuQagm imuQagm = ImuQagm.fromBytes(value);
                String strImuQagm = imuQagm==null? "null" : imuQagm.toString();
//                Log.i(TAG, "QAGM:QQ1:"+ strImuQagm);
                Quaternion q = new Quaternion(imuQagm.q[0], imuQagm.q[1], imuQagm.q[2], imuQagm.q[3]);
                //convert the x,y,z axis direction (x to left, y to inside, z to down)
                //to standard xyz. (x to right, y to up, z to outside)
                Quaternion qUpY = new Quaternion(0.70710678f, 0f, 0f, 0.70710678f);
                Quaternion qRightX = new Quaternion(0, -1f, 0f, 0f);
                Quaternion q1 = Quaternion.multiplication(qUpY,q);
                Quaternion q2 = Quaternion.multiplication(qRightX,q1);
//                Log.i(TAG, "QAGM:QQ2:"+ q2.x+","+q2.y+","+q2.z+","+q2.w);
                float[] retQ = new float[]{q2.x, q2.y, q2.z, -q2.w}; //rotate sensor instead of camera
//                q.displayAngle();
//                EulerAngles ea = new EulerAngles(imuQagm.q[0], imuQagm.q[1], imuQagm.q[2], imuQagm.q[3]);
//                Log.i(TAG, "QAGM:EA:"+ ea.toString());
                SensorManager.getRotationMatrixFromVector(mRotationMatrix , retQ);
            }
        }

        public void onDrawFrame(GL10 gl) {
            // clear screen
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            // set-up modelview matrix
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3.0f);
            gl.glMultMatrixf(mRotationMatrix, 0);
            // draw our object
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            mCube.draw(gl);
        }
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // set view-port
            gl.glViewport(0, 0, width, height);
            // set projection matrix
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
        }
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // dither is enabled by default, we don't need it
            gl.glDisable(GL10.GL_DITHER);
            // clear screen in white
            gl.glClearColor(1,1,1,1);
        }
        class Cube {
            // initialize our cube
            private FloatBuffer mVertexBuffer;
            private FloatBuffer mColorBuffer;
            private ByteBuffer  mIndexBuffer;
            public Cube() {
//                final float vertices[] = { //1.0*1.0*1.0 立方体
//                        -0.5f, -0.5f, -0.5f,		 0.5f, -0.5f, -0.5f,
//                        0.5f,  0.5f, -0.5f,	    -0.5f,  0.5f, -0.5f,
//                        -0.5f, -0.5f,  0.5f,      0.5f, -0.5f,  0.5f,
//                        0.5f,  0.5f,  0.5f,     -0.5f,  0.5f,  0.5f,
//                };
                final float vertices[] = { //1.2*0.8*0.4 长方体
                        -0.4f, -0.6f, -0.2f,		 0.4f, -0.6f, -0.2f,
                        0.4f,  0.6f, -0.2f,	    -0.4f,  0.6f, -0.2f,
                        -0.4f, -0.6f,  0.2f,      0.4f, -0.6f,  0.2f,
                        0.4f,  0.6f,  0.2f,     -0.4f,  0.6f,  0.2f,
                };
                final float colors[] = {
                        0,  0,  0,  1,  1,  0,  0,  1,
                        1,  1,  0,  1,  0,  1,  0,  1,
                        0,  0,  1,  1,  1,  0,  1,  1,
                        1,  1,  1,  1,  0,  1,  1,  1,
                };
                final byte indices[] = {
                        0, 4, 5,    0, 5, 1,
                        1, 5, 6,    1, 6, 2,
                        2, 6, 7,    2, 7, 3,
                        3, 7, 4,    3, 4, 0,
                        4, 7, 6,    4, 6, 5,
                        3, 0, 1,    3, 1, 2
                };
                ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
                vbb.order(ByteOrder.nativeOrder());
                mVertexBuffer = vbb.asFloatBuffer();
                mVertexBuffer.put(vertices);
                mVertexBuffer.position(0);
                ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
                cbb.order(ByteOrder.nativeOrder());
                mColorBuffer = cbb.asFloatBuffer();
                mColorBuffer.put(colors);
                mColorBuffer.position(0);
                mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
                mIndexBuffer.put(indices);
                mIndexBuffer.position(0);
            }
            public void draw(GL10 gl) {
                gl.glEnable(GL10.GL_CULL_FACE);
                gl.glFrontFace(GL10.GL_CW);
                gl.glShadeModel(GL10.GL_SMOOTH);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
                gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
            }
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}