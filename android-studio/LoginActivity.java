package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by Johnathan Mah on 2/20/2017.
 */
// The LoginActivity is the page where owners have the ability to login
// We require the owner's username and password
// This connects to SQL database using PHP to get the info
// If an owner inputs incorrect data they will be denied access.

// We also create buttons that act as intents that go to other activity pages such as Registration and Guest Login

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Defining the layout parameter variables
        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final Button bLogin = (Button) findViewById(R.id.bSignIn);
        final Button bRegister = (Button) findViewById(R.id.bRegister);
        final Button bGuest = (Button) findViewById(R.id.bGuest);

        // Setting the ONCLICK Listener to go to the Registration Page (Serial First)
        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent serialIntent = new Intent(LoginActivity.this, SerialLoginActivity.class);
                LoginActivity.this.startActivity(serialIntent);
            }
        });

        // Setting the ONCLICK Listener to go to the Guest Login Page
        bGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent guestIntent = new Intent(LoginActivity.this, GuestLoginActivity.class);
                LoginActivity.this.startActivity(guestIntent);
            }
        });

        // Setting the ONCLICK Listener, where if pushed will begin validating data with database
        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String username = etUsername.getText().toString();
                final String password = etPassword.getText().toString();

                // Response received from the server
                // We get the data from our SQL and PHP server (requires cellphone WiFi)
                // we do a Try and Catch to see if there are exceptions
                // A success will allow us to "intent" into another screen
                // A failure will give you a pop up that the information you entered was invalid
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // We are passing JSON Objects through PHP
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");

                            // SUCCESS means you can move onto the next step of the Registration Process
                            if (success) {
                                String name = jsonResponse.getString("name");
                                String serial = jsonResponse.getString("serial");

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("serial", serial);
                                intent.putExtra("username", username);
                                LoginActivity.this.startActivity(intent);
                            } else { // Failure will result in a popup message saying invalid entry
                                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                // We use the volley service to pass information
                // Adds service we want into a queue
                LoginRequest loginRequest = new LoginRequest(username, password, responseListener);
                RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
                queue.add(loginRequest);
            }
        });
    }
}
