package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
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
public class SerialLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_login);

        // Setting up the layout parameter variables
        final EditText etSerial = (EditText) findViewById(R.id.etSerial);
        final Button bNext = (Button) findViewById(R.id.bNext);

        // Setting the ONCLICK Listener, where if pushed will begin validating data with database
        bNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String serial_number = etSerial.getText().toString();

                // Response received from the server
                // We get the data from our SQL and PHP server (requires cellphone WiFi)
                // we do a Try and Catch to see if there are exceptions
                // A success will allow us to "intent" into another screen
                // A failure will give you a pop up that the information you entered was invalid
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            if (success) { // SUCCESS means you entered a serial number that exists

                                Intent intent = new Intent(SerialLoginActivity.this, RegisterActivity.class);
                                SerialLoginActivity.this.startActivity(intent);
                            } else { // FAILURE means a Serial Number that does not exist was entered
                                AlertDialog.Builder builder = new AlertDialog.Builder(SerialLoginActivity.this);
                                builder.setMessage("Invalid or Unknown Serial #")
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
                SerialLoginRequest serialRequest = new SerialLoginRequest(serial_number, responseListener);
                RequestQueue queue = Volley.newRequestQueue(SerialLoginActivity.this);
                queue.add(serialRequest);
            }
        });
    }
}
