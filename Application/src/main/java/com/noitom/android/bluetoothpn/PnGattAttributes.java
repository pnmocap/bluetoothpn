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

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class PnGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String IMU_DATA_TRANS = "00005A01-3846-5A8B-0000-F3A4C5D6E49F".toLowerCase();
    public static String IMU_DATA_SPEED = "00005A03-3846-5A8B-0000-F3A4C5D6E49F".toLowerCase();

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public final static UUID UUID_IMU_DATA_TRANS =
            UUID.fromString(PnGattAttributes.IMU_DATA_TRANS);

    static {
        // Services.
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00005A00-3846-5A8B-0000-F3A4C5D6E49F".toLowerCase(), "PNLAB Data Service");
        // Characteristics.
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(IMU_DATA_TRANS, "IMU Data Trans");
        attributes.put("00005A02-3846-5A8B-0000-F3A4C5D6E49F".toLowerCase(), "IMU Data Mode");
        attributes.put("00005A03-3846-5A8B-0000-F3A4C5D6E49F".toLowerCase(), "IMU Data Speed");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
