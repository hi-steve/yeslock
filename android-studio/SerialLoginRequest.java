package com.johnathanmah.yeslock;
// Importing the necessary apis, sdks, and libraries

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Johnathan Mah on 3/1/2017.
 */

// This Request page simply connects our Android Application to the online SQL database

public class SerialLoginRequest extends StringRequest {
    // USING LOGIN3.php
    private static final String SERIAL_REQUEST_URL = "https://eec136.000webhostapp.com/Login3.php";
    private Map<String, String> params;

    // Here we specify the parameters that will be passed to be compared with the SQL database info.
    public SerialLoginRequest(String serial_number, Response.Listener<String> listener) {
        // We are passing in a JSON object so we are using a POST command
        // We are checking 1 object with the database: serial_number
        super(Method.POST, SERIAL_REQUEST_URL, listener, null);
        // Hashmap is used to store data into a list
        params = new HashMap<>();
        params.put("serial_number", serial_number);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
