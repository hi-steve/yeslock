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
public class LoginRequest extends StringRequest {
    // USING LOGIN.PHP
    private static final String LOGIN_REQUEST_URL = "https://eec136.000webhostapp.com/Login.php";
    private Map<String, String> params;

    // Here we specify the parameters that will be passed to be compared with the SQL database info.
    public LoginRequest(String username, String password, Response.Listener<String> listener) {
        // We are passing in a JSON object so we are using a POST command
        // We are checking 2 objects with the database: username and password
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        // Hashmap is used to store data into a list
        params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}
