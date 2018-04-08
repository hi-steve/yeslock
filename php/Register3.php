<?php
	// This php script is used for the registration page of the android application.
	// We ask users to enter in their name, a desired username, a password, serial number, and guest password. 
	// The data they enter in here will be sent to the SQL server to be stored in real time. 
	// Errors will popup if a username is already taken or if a serial number is already associated with an account. 

	// These are our connection parameters that are required to connect to the database.
    $con = mysqli_connect("localhost", "id981978_yeslock", "Ham8932414", "id981978_eec136");
    
	// We are taking the following 5 parameters and posting them to the SQL database
    $name = $_POST["name"];
    $username = $_POST["username"];
    $password = $_POST["password"];
    $serial = $_POST["serial"];
    $guestpw = $_POST["guestpw"];
    
	// This function registers the user account onto the SQL database
	function registerUser() {
        global $con, $name, $username, $password, $serial, $guestpw;
		
		// We are taking in the data above and INSERTING them into the SQL database
        $statement = mysqli_prepare($con, "INSERT INTO user_id (name, username, password, serial, guestpw) VALUES (?, ?, ?, ?, ?)");
        mysqli_stmt_bind_param($statement, "sssss", $name, $username, $password, $serial, $guestpw);
        mysqli_stmt_execute($statement);
        mysqli_stmt_close($statement);     
    }
    
	// This function checks to see if a username is already taken
	function usernameAvailable() {
        global $con, $username;
		
		// Checking data from the user_id database
        $statement = mysqli_prepare($con, "SELECT * FROM user_id WHERE username = ?"); 
        mysqli_stmt_bind_param($statement, "s", $username);
        mysqli_stmt_execute($statement);
        mysqli_stmt_store_result($statement);
        $count = mysqli_stmt_num_rows($statement);
        mysqli_stmt_close($statement); 
				
		// Return true if the count increments (count only increments if the JSON object already seems to exist in the database)
        if ($count < 1){
            return true; 
        }else {
            return false; 
        }
    }
	
	// This function checks to see if a serial number is already associated with an account
	function idAvailable() {
        global $con, $serial;
		
		// Checking data from the user_id database
        $statement = mysqli_prepare($con, "SELECT * FROM user_id WHERE serial = ?"); 
        mysqli_stmt_bind_param($statement, "s", $serial);
        mysqli_stmt_execute($statement);
        mysqli_stmt_store_result($statement);
        $count = mysqli_stmt_num_rows($statement);
        mysqli_stmt_close($statement); 
		
		// Return true if the count increments (count only increments if the JSON object already seems to exist in the database)
        if ($count < 1){
            return true; 
        }else {
            return false; 
        }
    }
    
	// Response from the server
    $response = array();
    $response["success"] = false;  
    if (usernameAvailable() AND idAvailable()){
        registerUser();
        $response["success"] = true;  
    }
    
    echo json_encode($response);
?>