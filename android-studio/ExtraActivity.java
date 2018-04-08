package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.net.Uri;
/**
 * Created by Johnathan Mah on 3/9/2017.
 */
// The ExtraActivity page is the page where we include extra features for our application
// Currently this page simply extends a button where you can get real time updates on Location
// We display Latitude and Longtitude on a scroll box that updates every 5-10 seconds.
// Additionally, we include a button that connects to a website where you can insert a series of
// Latitude and Longitudes, to mark a map of where you have traveled

public class ExtraActivity extends AppCompatActivity {
// Defining global variables
    private Button b;
    private TextView t;
    private LocationManager locationManager;
    private LocationListener listener;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_extra);

        // Here we assign t, b to the layout parameters
        t = (TextView) findViewById(R.id.textView);
        b = (Button) findViewById(R.id.button);

        // Defining the Location Manager service for Latitude and Longitude
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Create a listener
        // Button push should enable the listener and begin searching for Latitude and Longitude
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                t.append("\n " + location.getLatitude() + "," + location.getLongitude());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            // Here we check for the Location source
            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        configure_button();
    }

    // We check for permissions, and if we are granted access we call the configure_button function
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                    configure_button();
                break;
            default:
                break;
        }
    }

    // This function is the bulk of checking permissions and actually pinging for Lat and Long data
    void configure_button(){
        // Check for permissions (this part is lengthy, but it's only checking for permissions)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        // This will not execute IF permissions are not allowed, because in the line above there is a return statement.
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection MissingPermission
                locationManager.requestLocationUpdates("gps", 5000, 0, listener);
            }
        });
    }

    // This is where we have the button to open up the browser to enter Lat and Long information
    // We call an intent to a browser screen, with a predefined URL
    public void browser(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.darrinward.com/lat-long"));
        startActivity(browserIntent);
    }


}


