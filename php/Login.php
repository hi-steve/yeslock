<?php
	// This php script is for the android screen for a Owner Login. 
	// A Owner is required to enter a username and password.  
	// Successful input of the information will allow authorization into the main screen of the application.

    // These are our connection parameters that are required to connect to the database.
	$con = mysqli_connect("localhost", "id981978_yeslock", "Ham8932414", "id981978_eec136");
    
	// We are entering two things to compare with the database, username and password, which are passed as JSON objects
    $username = $_POST["username"];
    $password = $_POST["password"];
    
	// We specify which database we want to SELECT data from to check with the JSON object from Android (ss stands for string string)
    $statement = mysqli_prepare($con, "SELECT * FROM user_id WHERE username = ? AND password = ?");
    mysqli_stmt_bind_param($statement, "ss", $username, $password);
    mysqli_stmt_execute($statement);
    
	// We bind the result to match the parameters of the database exactly!
    mysqli_stmt_store_result($statement);
    mysqli_stmt_bind_result($statement, $user_id, $name, $username, $password, $serial, $guestpw);
    
	// Response from the server
    $response = array();
    $response["success"] = false;  
    
	// If true we get the values from the SQL database, and pass a boolean true
    while(mysqli_stmt_fetch($statement)){
        $response["success"] = true;  
        $response["name"] = $name;
        $response["username"] = $username;
		$response["password"] = $password;
        $response["serial"] = $serial;
        $response["guestpw"] = $guestpw;
    }
    
    echo json_encode($response);
?>