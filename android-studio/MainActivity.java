package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

// The MainActivity page is where the user connects to bluetooth and PSoC 4 BLE
// Upon connection, users have the options to unlock/lock their bike lock,
// turn on/off the alarm buzzer, and check for their bike location
// All bluetooth functionalites are accomplished here by calling methods of PSoCYesBikeService

public class MainActivity extends AppCompatActivity {
    Intent intent = getIntent();
    // TAG is used for informational messages
    private final static String TAG = MainActivity.class.getSimpleName();

    // Variables to access objects from the layout such as buttons and switches
    private static Button start_button;
    private static Button search_button;
    private static Button connect_button;
    private static Button discover_button;
    private static Button disconnect_button;
    private static Switch lock_switch;
    private static Button alarm_on;
    private static Button alarm_off;
    private static Button bLocation;


    // Variables to manage BLE connection
    private static boolean mConnectState;
    private static boolean mServiceConnected;
    private static PSoCYesBikeService mPSoCYesBikeService;

    private static final int REQUEST_ENABLE_BLE = 1;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Keep track of whether Warning Notifications are on or off
    private static boolean WarningNotifyState = false;

    // Variables to manage Warning and Alarm notifications
    private NotificationManager mNotificationManager;
    private int notifyIDWarning = 1;
    private int notifyIDAlarm = 2;
    private int numMessagesW = 0;
    private int numMessagesA = 0;
    private Context context;

    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the PSoCYesBikeService is connected.
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mPSoCYesBikeService = ((PSoCYesBikeService.LocalBinder) service).getService();
            mServiceConnected = true;
            mPSoCYesBikeService.initialize();
        }

        /**
         * This is called when the PSoCYesBikeService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mPSoCYesBikeService = null;
        }
    };


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * This is called when the main activity is first created
     *
     * @param savedInstanceState is any state saved from prior creations of this activity
     */
    @TargetApi(Build.VERSION_CODES.M) // This is required for Android 6.0 (Marshmallow) to work
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Set up variables for accessing buttons and switches
        start_button = (Button) findViewById(R.id.start_button);
        search_button = (Button) findViewById(R.id.search_button);
        connect_button = (Button) findViewById(R.id.connect_button);
        discover_button = (Button) findViewById(R.id.discoverSvc_button);
        disconnect_button = (Button) findViewById(R.id.disconnect_button);
        lock_switch = (Switch) findViewById(R.id.lock_switch);
        alarm_on = (Button) findViewById(R.id.alarm_on);
        alarm_off = (Button) findViewById(R.id.alarm_off);
        bLocation = (Button) findViewById(R.id.bLocation);

        // Initialize service and connection state variable
        mServiceConnected = false;
        mConnectState = false;

        // This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)

         /* This will be called when the Lock switch is toggled */
        lock_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                BluetoothGattCharacteristic mWarningChar = mPSoCYesBikeService.getWarningCharacteristic();

                // Writes to Lock Characteristic to tell PSoC to either unlock/lock the lock
                // and enables/disables Warning Notifications which begins/ends alarm scanning
                if (!lock_switch.isChecked()) {
                    mPSoCYesBikeService.writeLockCharacteristic(0);
                    mPSoCYesBikeService.setWarningNotification(mWarningChar, false);
                    Toast.makeText(getApplicationContext(), "Alarm scanning off!", Toast.LENGTH_LONG).show();
                } else {
                    mPSoCYesBikeService.writeLockCharacteristic(1);
                    mPSoCYesBikeService.setWarningNotification(mWarningChar, true);
                    Toast.makeText(getApplicationContext(), "Alarm scanning engaged!", Toast.LENGTH_LONG).show();
                }

            }

        });

        /* This will be called when the Alarm On button is pressed */
        alarm_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If the lock is locked, writes to Alarm Characteristic to tell PSoC to turn on the alarm
                // and disables the Lock switch button from being toggled
                if(mPSoCYesBikeService.getLockSwitchState()==1)
                {
                    mPSoCYesBikeService.writeAlarmCharacteristic(1);
                    lock_switch.setEnabled(false);
                }

            }
        });

        /* This will be called when the Alarm Off button is pressed */
        alarm_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If the lock is locked, writes to Alarm Characteristic to tell PSoC to turn off the alarm
                // and resets alarm scanning by re-enabling the Lock switch and unlocking the lock
                if(mPSoCYesBikeService.getLockSwitchState()==1)
                {
                    mPSoCYesBikeService.writeAlarmCharacteristic(0);
                    lock_switch.setEnabled(true);
                    lock_switch.setChecked(false);
                }

            }
        });

        /* This will be called when the Check Location button is pressed */
        bLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Brings user to activity ExtraActivity
                Intent locationIntent = new Intent(MainActivity.this, ExtraActivity.class);
                MainActivity.this.startActivity(locationIntent);
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }

    // This method requests for user's location and is required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } // End of section for Android 6.0 (Marshmallow)

    /* This is called to register the broadcast receiver, and specifies the messages the main activity
       looks for from PSoCYesBikeService
    */
    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(PSoCYesBikeService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(PSoCYesBikeService.ACTION_CONNECTED);
        filter.addAction(PSoCYesBikeService.ACTION_DISCONNECTED);
        filter.addAction(PSoCYesBikeService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(PSoCYesBikeService.ACTION_DATA_RECEIVED);
        filter.addAction(PSoCYesBikeService.TACTICAL_PRESSED);
        registerReceiver(mBleUpdateReceiver, filter);
    }

    /* This is called when user's bluetooth is disabled and requests to turn it on */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* This is called when the phone is idle while the user is still on this activity */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    /* This is called when leaving this activity */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mPSoCYesBikeService.close();
        unbindService(mServiceConnection);
        mPSoCYesBikeService = null;
        mServiceConnected = false;
    }

    /**
     * This method handles the start bluetooth button
     *
     * @param view the view object
     */
    public void startBluetooth(View view) {

        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(this, PSoCYesBikeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Disable the start button and turn on the search  button
        start_button.setEnabled(false);
        search_button.setEnabled(true);
        Log.d(TAG, "Bluetooth is Enabled");
    }

    /**
     * This method handles the Search for Device button
     *
     * @param view the view object
     */
    public void searchBluetooth(View view) {
        if(mServiceConnected) {
            mPSoCYesBikeService.scan();
        }

        /* After this we wait for the scan callback to detect that a device has been found */
        /* The callback broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Connect to Device button
     *
     * @param view the view object
     */
    public void connectBluetooth(View view) {
        mPSoCYesBikeService.connect();

        /* After this we wait for the gatt callback to report the device is connected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Discover Services and Characteristics button
     *
     * @param view the view object
     */
    public void discoverServices(View view) {
        /* This will discover both services and characteristics */
        mPSoCYesBikeService.discoverServices();

        /* After this we wait for the gatt callback to report the services and characteristics */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * This method handles the Disconnect button
     *
     * @param view the view object
     */
    public void Disconnect(View view) {
        mPSoCYesBikeService.disconnect();

        /* After this we wait for the gatt callback to report the device is disconnected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }


    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {

                // Called when scanning for our nearby bluetooth device...
                case PSoCYesBikeService.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    //search_button.setEnabled(false);
                    connect_button.setEnabled(true);

                    Log.d(TAG, "Scan");
                    Toast.makeText(getApplicationContext(), "Bluetooth scanning...", Toast.LENGTH_SHORT).show();
                    break;

                // Called when bluetooth connection is established...
                case PSoCYesBikeService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Warning notifications */
                    if (!mConnectState) {
                        // Disable the connect button, enable the discover services and disconnect button
                        connect_button.setEnabled(false);
                        discover_button.setEnabled(true);
                        disconnect_button.setEnabled(true);
                        mConnectState = true;
                        Log.d(TAG, "Connected");
                        Toast.makeText(getApplicationContext(), "Bluetooth connected!", Toast.LENGTH_SHORT).show();

                    }
                    break;

                // Called when bluetooth has been disconnected...
                case PSoCYesBikeService.ACTION_DISCONNECTED:
                    // Disable the disconnect, discover svc, discover char button, and enable the search button
                    start_button.setEnabled(true);
                    discover_button.setEnabled(false);
                    disconnect_button.setEnabled(false);
                    //search_button.setEnabled(true);

                    // Turn off and disable the Lock switch
                    lock_switch.setChecked(false);
                    lock_switch.setEnabled(false);
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;

                // Called when finished discovering for services and characteristics...
                case PSoCYesBikeService.ACTION_SERVICES_DISCOVERED:
                    // Disable the discover services button
                    discover_button.setEnabled(false);
                    // Enable the LED and CapSense switches
                    lock_switch.setEnabled(true);

                    Log.d(TAG, "Services Discovered");
                    break;

                // Called when PSoC sends a notification
                case PSoCYesBikeService.ACTION_DATA_RECEIVED:

                    int warningState = mPSoCYesBikeService.getWarningState();
                    // warningState = 1 displays a warning notification
                    // warningState = 2 displays an alarm notification

                    if (warningState==1)
                    {
                        Intent notification_intent = new Intent(getApplicationContext(), MainActivity.class);
                        notification_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), notifyIDWarning, notification_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                        builder
                                .setSmallIcon(R.drawable.bike_icon)
                                .setOngoing(true)                           // Can't cancel by swipe
                                .setAutoCancel(true)                        // Only cancel when pressed
                                .setContentIntent(pendingIntent)
                                .setContentTitle("YES BIKE")
                                .setContentText("WARNING: Your bike is about to get taken.")
                                .setPriority(Notification.PRIORITY_MAX)     // Heads-up notification
                                .setDefaults(Notification.DEFAULT_VIBRATE)  // Heads-up notification
                                .setLights(Color.YELLOW, 1000, 2500)
                                .setNumber(++numMessagesW);

                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        mNotificationManager.notify(notifyIDWarning, notification);
                    }
                    else if (warningState==2)
                    {
                        Intent notification_intent = new Intent(getApplicationContext(), MainActivity.class);
                        notification_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), notifyIDAlarm, notification_intent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                        builder
                                .setSmallIcon(R.drawable.bike_icon)
                                .setOngoing(true)                           // Can't cancel by swipe
                                .setAutoCancel(true)                        // Only cancel when pressed
                                .setContentIntent(pendingIntent)
                                .setContentTitle("YES BIKE")
                                .setContentText("ALARM: Your bike is getting stolen!")
                                .setPriority(Notification.PRIORITY_MAX)     // Heads-up notification
                                .setDefaults(Notification.DEFAULT_VIBRATE)  // Heads-up notification
                                .setLights(Color.YELLOW, 1000, 2500)
                                .setNumber(++numMessagesA);

                        Notification notification = builder.build();
                        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        mNotificationManager.notify(notifyIDAlarm, notification);
                    }

                    //Log.d(TAG, "Debugging: ACTION DATA RECEIVED");
                    break;

                default:
                    break;
            }
        }
    };

    // Clears the number count of notifications whenever the user clicks the notification
    protected void onNewIntent(final Intent intent) {
        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            // do nothing
        } else {
            // user clicked on the notification
            numMessagesW = 0;
            numMessagesA = 0;
        }
    }
}