package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// The PSoCYesBikeService service finds our device's unique UUID and handles
// read, write, and notify actions between the App and PSoC 4 BLE device

/**
 * Service for managing the BLE data connection with the GATT database.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP) // This is required to allow us to use the lollipop and later scan APIs
public class PSoCYesBikeService extends Service {

    private final static String TAG = PSoCYesBikeService.class.getSimpleName();

    // Bluetooth objects that we need to interact with
    private static BluetoothManager mBluetoothManager;
    private static BluetoothAdapter mBluetoothAdapter;
    private static BluetoothLeScanner mLEScanner;
    private static BluetoothDevice mLeDevice;
    private static BluetoothGatt mBluetoothGatt;

    // Bluetooth characteristics and descriptors that we need to read/write/notify
    private static BluetoothGattCharacteristic mLockCharacterisitc;
    private static BluetoothGattCharacteristic mWarningCharacteristic;
    private static BluetoothGattDescriptor mWarningCccd;
    private static BluetoothGattCharacteristic mAlarmCharacteristic;

    // UUIDs for the service and characteristics that the custom YesBike service uses
    // Currently the UUIDs are hard-coded because we only used one device
    // In the future, we would appropriately store the UUIDs of all users' devices into a
    // database and assign them to variables accordingly
    private final static String baseUUID =                   "00000000-0000-1000-8000-00805f9b34f";
    private final static String yesbikeServiceUUID =          baseUUID + "0";
    public  final static String lockCharacteristicUUID =      baseUUID + "1";
    public  final static String warningCharacteristicUUID =   baseUUID + "3";
    private final static String WarnCccdUUID =               "00002903-0000-1000-8000-00805f9b34fb";
    private final static String alarmCharacteristicUUID =     baseUUID + "4";

    // Variables to keep track of the Lock switch, Warning, and Alarm state
    private static int mLockSwitchState = 0;
    private static int mWarningState = 0;
    private static int mAlarmState = 0;

    // Actions used during broadcasts to the main activity
    public final static String ACTION_BLESCAN_CALLBACK =
            "com.cypress.academy.ble101.ACTION_BLESCAN_CALLBACK";
    public final static String ACTION_CONNECTED =
            "com.cypress.academy.ble101.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.cypress.academy.ble101.ACTION_DISCONNECTED";
    public final static String ACTION_SERVICES_DISCOVERED =
            "com.cypress.academy.ble101.ACTION_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_RECEIVED =
            "com.cypress.academy.ble101.ACTION_DATA_RECEIVED";
    public final static String TACTICAL_PRESSED =
            "com.cypress.academy.ble101.TACTICAL_PRESSED";

    public PSoCYesBikeService() {
    }

    public class LocalBinder extends Binder {
        PSoCYesBikeService getService() {
            return PSoCYesBikeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // The BLE close method is called when we unbind the service to free up the resources.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Scans for BLE devices that support the service we are looking for.
     */
    public void scan() {
        /* Scan for devices and look for the one with the service that we want */
        UUID   yesbikeService =       UUID.fromString(yesbikeServiceUUID);
        UUID[] yesbikeServiceArray = {yesbikeService};

        // Use old scan method for versions older than lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            mBluetoothAdapter.startLeScan(yesbikeServiceArray, mLeScanCallback);
        } else { // New BLE scanning introduced in LOLLIPOP
            ScanSettings settings;
            List<ScanFilter> filters;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
            // We will scan just for the CAR's UUID
            ParcelUuid PUuid = new ParcelUuid(yesbikeService);
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(PUuid).build();
            filters.add(filter);
            mLEScanner.startScan(filters, settings, mScanCallback);
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect() {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = mLeDevice.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        return true;
    }

    /**
     * Runs service discovery on the connected device.
     */
    public void discoverServices() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.discoverServices();
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Sends a broadcast to the listener in the main activity.
     *
     * @param action The type of action that occurred.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Implements the callback for when scanning for devices has found a device with
     * the service we are looking for.
     *
     * This is the callback for BLE scanning on versions prior to Lollipop
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mLeDevice = device;
                    //noinspection deprecation
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); // Stop scanning after the first device is found
                    broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
                }
            };

    /**
     * Implements the callback for when scanning for devices has found a device with
     * the service we are looking for.
     *
     * This is the callback for BLE scanning for LOLLIPOP and later
     */
    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDevice = result.getDevice();
            mLEScanner.stopScan(mScanCallback); // Stop scanning after the first device is found
            broadcastUpdate(ACTION_BLESCAN_CALLBACK); // Tell the main activity that a device has been found
        }
    };


    /**
     * Implements callback methods for GATT events that the app cares about. For example,
     * connection change and services discovered.
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastUpdate(ACTION_CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        /**
         * This is called when a service discovery has completed.
         *
         * It gets the characteristics we are interested in and then
         * broadcasts an update to the main activity.
         *
         * @param gatt The GATT database object
         * @param status Status of whether the write was successful.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            /* Get just the service that we are looking for */
            BluetoothGattService mService = gatt.getService(UUID.fromString(yesbikeServiceUUID));

            /* Get characteristics and CCCDs from our desired service */
            mLockCharacterisitc = mService.getCharacteristic(UUID.fromString(lockCharacteristicUUID));
            mWarningCharacteristic = mService.getCharacteristic(UUID.fromString(warningCharacteristicUUID));
            mWarningCccd = mWarningCharacteristic.getDescriptor(UUID.fromString(WarnCccdUUID));
            mAlarmCharacteristic = mService.getCharacteristic(UUID.fromString(alarmCharacteristicUUID));

            // Broadcast that service/characteristic/descriptor discovery is done
            broadcastUpdate(ACTION_SERVICES_DISCOVERED);
        }

        /**
         * This is called when a read completes
         *
         * @param gatt the GATT database object
         * @param characteristic the GATT characteristic that was read
         * @param status the status of the transaction
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            Log.d("TAG", "Debugging: Characteristic Reading...");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("TAG", "Debugging: Characteristic Reading Success!");

                String uuid = characteristic.getUuid().toString();

                // Currently the client does not read any values from the device
                // but we keep this portion of the code in case it is necessary
                // for future implementations

                /*if(uuid.equals(alarmCharacteristicUUID)) {
                    final byte[] data = characteristic.getValue();
                    mAlarmState = data[0];
                }*/

                // Notify the main activity that new data is available
                broadcastUpdate(ACTION_DATA_RECEIVED);
            }
        }

        /**
         * This is called when a characteristic with notify set changes.
         * It broadcasts an update to the main activity with the changed data.
         *
         * @param gatt The GATT database object
         * @param characteristic The characteristic that was changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            String uuid = characteristic.getUuid().toString();


            // Checks to see if Warning Characteristic has been changed
            if(uuid.equals(warningCharacteristicUUID)) {
                mWarningState = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                Log.d("TAG", "Debugging: " + mWarningState);
            }

            // Notify the main activity that new data is available
            broadcastUpdate(ACTION_DATA_RECEIVED);
        };

        // End of GATT event callback methods
    };

    /* This is called when Warning Notifications are enabled for alarm scanning */
    public void setWarningNotification(BluetoothGattCharacteristic characteristic,
                                       boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (warningCharacteristicUUID.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = mWarningCccd;
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /* This method is used to read the state of the Lock from the device */
    public void readLockCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(mLockCharacterisitc);
    }

    /* This method is used to read the Warning State from the device */
    public void readWarningCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(mWarningCharacteristic);
    }

    /**
     * This method is used to lock or unlock the lock, and when the
     * lock is locked, a flag is sent to allow updates for notifications
     *
     * @param value Locks (1) or unlocks (0) the lock
     */
    public void writeLockCharacteristic(int value) {
        byte[] byteVal = new byte[2];
        if (value == 1) {
            byteVal[0] = (byte) (1);        // Lock
            byteVal[1] = (byte) (2);        // Allow updates for notifications
        } else {
            byteVal[0] = (byte) (0);        // Unlock
            byteVal[1] = (byte) (7);        // Buffer byte
        }
        Log.i(TAG, "LED " + value);
        mLockSwitchState = value;
        mLockCharacterisitc.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mLockCharacterisitc);
    }

    /**
     * This method is used to turn the alarm on or off
     *
     * @param value Turns the alarm on (1) or off (0)
     */
    public void writeAlarmCharacteristic(int value) {
        byte[] byteVal = new byte[1];
        if (value == 1) {
            byteVal[0] = (byte) (1);        // Alarm on
        }
        else if (value == 2) {
            byteVal[0] = (byte) (2);        // Allow updates for notifications
        }
        else {
            byteVal[0] = (byte) (0);        // Alarm off
        }
        Log.i(TAG, "Alarm " + value);
        mAlarmState = value;
        mAlarmCharacteristic.setValue(byteVal);
        mBluetoothGatt.writeCharacteristic(mAlarmCharacteristic);
    }

    /* Get functions to return current states and characteristics */

    public int getLockSwitchState() {
        return mLockSwitchState;
    }

    public int getWarningState() {
        return mWarningState;
    }

    public int getAlarmState() { return mAlarmState; }

    public static BluetoothGattCharacteristic getWarningCharacteristic() {
        return mWarningCharacteristic;
    }

    public static BluetoothGattCharacteristic getmAlarmCharacteristic() {
        return mAlarmCharacteristic;
    }

}
