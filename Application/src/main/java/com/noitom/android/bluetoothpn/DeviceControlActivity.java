/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * This project is based android open source project(https://github.com/android/connectivity-samples/tree/main/BluetoothLeGatt)
 * Copyright © 2015-2022 Noitom Ltd.
 */

package com.noitom.android.bluetoothpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Toast.makeText(DeviceControlActivity.this, "Device Connected", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "To Discover Services");
                mBluetoothLeService.discoverServices();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
                Toast.makeText(DeviceControlActivity.this, "Device DisConnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                handleData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_UUID), intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_MTU_CHANGED.equals(action)){
                Toast.makeText(DeviceControlActivity.this, "Mtu is set to 50", Toast.LENGTH_SHORT).show();
//                mBluetoothLeService.discoverServices();
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);

                        showCharacteristicDetail(characteristic);
                        return true;
                    }
                    return false;
                }
    };

    private void showCharacteristicDetail(BluetoothGattCharacteristic characteristic){
        final Dialog characteristicDialog = new Dialog(this);
        final BluetoothGattCharacteristic finalChar = characteristic;

        LayoutInflater factory = LayoutInflater.from(this);
        View characteristicView = factory.inflate(R.layout.characteristic_detail, null);
        Button btnRead = characteristicView.findViewById(R.id.read_characteristic);
        Button btnNotification = characteristicView.findViewById(R.id.notify_characteristic);
        Button btnWrite = characteristicView.findViewById(R.id.write_characteristic);
        final EditText writeValue = characteristicView.findViewById(R.id.characteristic_write_value);
        TextView characteristicUuid = characteristicView.findViewById(R.id.characteristic_uuid);
        TextView characteristicProperties = characteristicView.findViewById(R.id.characteristic_properties);
        Button btnClose = characteristicView.findViewById(R.id.btn_close);

        final int charProperties = characteristic.getProperties();
        ArrayList<String> alProperties = new ArrayList<>();
        if((charProperties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0 ){
            alProperties.add("BROADCAST");
        }
        if((charProperties & BluetoothGattCharacteristic.PROPERTY_READ) > 0 ){
            alProperties.add("READ");
            btnRead.setEnabled(true);
        }else{
            btnRead.setEnabled(false);
        }
        if((charProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 ){
            alProperties.add("WRITE");
            btnWrite.setEnabled(true);
            writeValue.setEnabled(true);
        }else{
            btnWrite.setEnabled(false);
            writeValue.setEnabled(false);
        }
        if((charProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 ){
            alProperties.add("NOTIFY");
            btnNotification.setEnabled(true);
        }else {
            btnNotification.setEnabled(false);
        }
        if((charProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0 ){
            alProperties.add("INDICATE");
        }
        String strProp = String.join(",", alProperties);
        characteristicProperties.setText(strProp);
        characteristicUuid.setText(characteristic.getUuid().toString());

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(finalChar);
                characteristicDialog.dismiss();
            }
        });

        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                } else {
                    mNotifyCharacteristic = finalChar;
                    mBluetoothLeService.setCharacteristicNotification(
                            finalChar, true);
                }
                characteristicDialog.dismiss();
            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strWV = writeValue.getText().toString();
                if(strWV!=null && !strWV.isEmpty()){
                    Byte b = Byte.parseByte(strWV);
                    byte[] v = new byte[]{b};
                    mBluetoothLeService.writeCharacteristic(finalChar,v,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    characteristicDialog.dismiss();
                }else{
                    Toast.makeText(DeviceControlActivity.this,"write value is empty",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                characteristicDialog.dismiss();
            }
        });

        characteristicDialog.setTitle(R.string.characteristic_title);
        characteristicDialog.setContentView(characteristicView);
        characteristicDialog.show();
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_mtu).setVisible(true);
            menu.findItem(R.id.menu_view).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_mtu).setVisible(false);
            menu.findItem(R.id.menu_view).setVisible(false);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case R.id.menu_mtu:
                showMtuDiaglog();
                return true;
            case R.id.menu_view:
                showDeviceView();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMtuDiaglog(){
        final EditText etMtu = new EditText(this);
        etMtu.setInputType(InputType.TYPE_CLASS_NUMBER);
        etMtu.setHint("MTU value: <23 - 517>");
        etMtu.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("MTU").setIcon(android.R.drawable.ic_dialog_info).setView(etMtu)
                .setNegativeButton("Cancel", null);
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onClick(DialogInterface dialog, int which) {
                String strMtu = etMtu.getText().toString();
                if(strMtu!=null && !strMtu.isEmpty())
                {
                    int mtu = Integer.parseInt(strMtu);
                    mBluetoothLeService.requestMTU(mtu);
                }
                else
                {
                    Toast.makeText(DeviceControlActivity.this,"Empty MTU", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    private void showDeviceView(){
        final Intent intent = new Intent(this, DeviceViewActivity.class);
        intent.putExtra(EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private void handleData(String charaUuid, byte[] value){
//        Log.i(TAG, "uuid="+charaUuid+", value="+value);

        if(PnGattAttributes.IMU_DATA_TRANS.equals(charaUuid)){
            ImuQagm imuQagm = ImuQagm.fromBytes(value);
            String strImuQagm = imuQagm==null? "null" : imuQagm.toString();
//            Log.i(TAG, "QAGM:"+ strImuQagm);
            displayData(strImuQagm);
        } else if(PnGattAttributes.IMU_DATA_SPEED.equals(charaUuid)){
            int freq = ImuQagm.bytesToShort(value[0], value[1]);
            displayData(String.valueOf(freq));
        } else{
            final byte[] data = value;
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                Log.i(TAG, "broadcastUpdate data len="+data.length+ ", data="+stringBuilder.toString());
                displayData(stringBuilder.toString());
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, PnGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);

                // get Characteristic properties and display on name
                int charProperties = gattCharacteristic.getProperties();
                StringBuilder sbProper = new StringBuilder();
                if((charProperties & BluetoothGattCharacteristic.PROPERTY_BROADCAST) > 0 ){
                    sbProper.append("B");
                }
                if((charProperties & BluetoothGattCharacteristic.PROPERTY_READ) > 0 ){
                    sbProper.append("R");
                }
                if((charProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0 ){
                    sbProper.append("W");
                }
                if((charProperties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 ){
                    sbProper.append("N");
                }
                if((charProperties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0 ){
                    sbProper.append("I");
                }
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                String strCharName = PnGattAttributes.lookup(uuid, unknownCharaString) + "|" + String.valueOf(charProperties) + "|"+sbProper.toString();

                currentCharaData.put(LIST_NAME, strCharName);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_MTU_CHANGED);
        return intentFilter;
    }
}
