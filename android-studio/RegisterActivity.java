package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
/**
 * Created by Johnathan Mah on 2/20/2017.
 */
// The RegisterActivity is the page where users can create an account and store the information onto a SQL database
// We require the a user to enter their name, username, password, guest password, and serial number
// This connects to SQL database using PHP to INSERT the info
// If a username already exists you will get a popup asking to choose a new one
// If a serial number is associated with an account already, a popup stating this will appear

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Defining the layout parameter variables
        final EditText etid = (EditText) findViewById(R.id.etid);
        final EditText etName = (EditText) findViewById(R.id.etName);
        final EditText etUsername = (EditText) findViewById(R.id.etUsername);
        final EditText etPassword = (EditText) findViewById(R.id.etPassword);
        final EditText etGuestpw = (EditText) findViewById(R.id.etGuestpw);
        final Button bRegister = (Button) findViewById(R.id.bRegister);

        // Setting the ONCLICK Listener, where if pushed will begin validating data with database
        bRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = etName.getText().toString();
                final String username = etUsername.getText().toString();
                final String serial = etid.getText().toString();
                final String password = etPassword.getText().toString();
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
                            // We are passing in JSON objects through PHP
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            if (success) { // SUCCESS means your account has successfully been created!
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                RegisterActivity.this.startActivity(intent);
                            } else { // Failure means information already exists or is invalid
                                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
                                builder.setMessage("Register Failed: Username or Serial # already taken.")
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
                // Adds service we want to a queue
                RegisterRequest registerRequest = new RegisterRequest(name, username, password, serial, guestpw, responseListener);
                RequestQueue queue = Volley.newRequestQueue(RegisterActivity.this);
                queue.add(registerRequest);
            }
        });
    }
}