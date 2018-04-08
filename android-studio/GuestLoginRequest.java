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
public class GuestLoginRequest extends StringRequest {
    // USING LOGIN2.PHP
    private static final String GUEST_LOGIN_REQUEST_URL = "https://eec136.000webhostapp.com/Login2.php";
    private Map<String, String> params;

    // Here we specify the parameters that will be passed to be compared with the SQL database info.
    public GuestLoginRequest(String UserID, String name, String guestpw, Response.Listener<String> listener) {
        // We are passing in a JSON object so we are using a POST command
        // We are checking 3 objects with the database: UserID, name, and guestpw
        super(Method.POST, GUEST_LOGIN_REQUEST_URL, listener, null);
        // Hashmap is used for storing the data into a list
        params = new HashMap<>();
        params.put("UserID", UserID);
        params.put("name", name);
        params.put("guestpw", guestpw);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}

