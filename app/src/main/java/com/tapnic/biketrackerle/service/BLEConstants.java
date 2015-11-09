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
 */

package com.tapnic.biketrackerle.service;

import java.util.HashMap;
import java.util.UUID;

public class BLEConstants {
    private static HashMap<String, String> attributes = new HashMap<>();
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static final String CSC_SERVICE = "00001816-0000-1000-8000-00805f9b34fb";
    public static final String CSC_CHARACTERISTIC = "00002a5b-0000-1000-8000-00805f9b34fb";


    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG);
    public static final UUID CSC_SERVICE_UUID = UUID.fromString(CSC_SERVICE);
    public static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString(CSC_CHARACTERISTIC);

    static {
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(CSC_CHARACTERISTIC, "CSC Bike");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
