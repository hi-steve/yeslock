package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries
import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by Johnathan Mah on 3/1/2017.
 */
// The GuestLoginActivity is the page where Guests have the ability to login
// We require the owner's unique user_id, owner name, and owner's guest password
// This connects to SQL database using PHP to get the info
// If a guest inputs incorrect data they will be denied access.

public class GuestLoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_login);

        // Defining the layout parameter variables
        final EditText etUserID = (EditText) findViewById(R.id.etUserID);
        final EditText etGuestpw = (EditText) findViewById(R.id.etGuestpw);
        final EditText etOwner = (EditText) findViewById(R.id.etOwner);
        final Button bGuestLogin = (Button) findViewById(R.id.bGuestLogin);
        final Button bRequestInfo = (Button) findViewById(R.id.bRequestInfo);

        // Setting the ONCLICK Listener, where if pushed will begin validating data with database
        bGuestLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String UserID = etUserID.getText().toString();
                final String name = etOwner.getText().toString();
                final String guestpw = etGuestpw.getText().toString();

                // Response received from the server
                // We get the data from our SQL and PHP server (requires cellphone WiFi)
                // we do a Try and Catch to see if there are exceptions
                // A success will allow us to "intent" into another screen
                // A failure will give you a pop up that the information you entered was invalid
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // We are passing JSON objects with PHP
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");

                            if (success) {
                                // SUCCESS MOVE ONTO THE NEXT ACTIVITY
                                Intent intent = new Intent(GuestLoginActivity.this, MainActivity.class);
                                GuestLoginActivity.this.startActivity(intent);
                            } else { // FAILURE = POPUP with message that authorization was not granted
                                AlertDialog.Builder builder = new AlertDialog.Builder(GuestLoginActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }

                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                // We use the volley service to pass information
                // Adds service we want into a queue
                GuestLoginRequest loginRequest = new GuestLoginRequest(UserID, name, guestpw, responseListener);
                RequestQueue queue = Volley.newRequestQueue(GuestLoginActivity.this);
                queue.add(loginRequest);
            }
        });

        // This section of code is for the Request Info Button
        // We set an onclick listener for the button trigger then have the intent be a "send"
        // We can preset the message we want in the application (email, messenger, etc.)
        bRequestInfo.setOnClickListener(new View.OnClickListener()  {
            @Override
                public void onClick(View v){
                    Intent myIntent = new Intent(Intent.ACTION_SEND);
                    myIntent.setType("text/plain");
                    String shareBody = "Hey! Sending this from YES LOCK! What's your Guest Login Information?";
                    String shareSub = "YES LOCK: Guest Login Request";
                    myIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
                    myIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(myIntent, "Request Using"));
            }
        });



    }
}




