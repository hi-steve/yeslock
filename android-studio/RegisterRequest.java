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

public class RegisterRequest extends StringRequest {
    // USING REGISTER3.php
    private static final String REGISTER_REQUEST_URL = "https://eec136.000webhostapp.com/Register3.php";
    private Map<String, String> params;

    // Here we specify the parameters that will be passed to be compared with the SQL database info.
    public RegisterRequest(String name, String username, String password, String serial, String guestpw, Response.Listener<String> listener) {
        // We are passing in a JSON object so we are using a POST command
        // We are checking 5 objects with the database: username, password, serial, guestpw, and name
        super(Method.POST, REGISTER_REQUEST_URL, listener, null);
        // Hashmap is used to store data into a list
        params = new HashMap<>();
        params.put("name", name);
        params.put("username", username);
        params.put("password", password);
        params.put("serial", serial);
        params.put("guestpw", guestpw);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
