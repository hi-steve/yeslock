<?php
	// This php script is for the android screen for a Guest Login. 
	// A guest is required to enter a bike owner's unique user ID #, name, and guest password. 
	// Successful input of the owners information will allow authorization into the main screen of the application.


    // These are our connection parameters that are required to connect to the database.
	$con = mysqli_connect("localhost", "id981978_yeslock", "Ham8932414", "id981978_eec136");
    
	// The UserID, owner name, and guestpw are the three parameters we are entering to compare to the database. They are passed as JSON objects. 
    $user_id = $_POST["UserID"];
    $name = $_POST["name"];
    $guestpw = $_POST["guestpw"];
    
	// We specify which database we want to SELECT data from to check with the JSON object from Android (sss stands for string string string)
    $statement = mysqli_prepare($con, "SELECT * FROM user_id WHERE user_id = ? AND name = ? AND guestpw = ?");
    mysqli_stmt_bind_param($statement, "sss", $user_id, $name, $guestpw);
    mysqli_stmt_execute($statement);
    
	// We bind the result to match the parameters of the database exactly!
    mysqli_stmt_store_result($statement);
    mysqli_stmt_bind_result($statement, $user_id, $name, $username, $password, $id, $guestpw);
    
	// Response from the server
    $response = array();
    $response["success"] = false;  
    
	// If true we get the values from the SQL database, and pass a boolean true 
    while(mysqli_stmt_fetch($statement)){
        $response["success"] = true;  
        $response["name"] = $name;
        $response["username"] = $username;
		$response["password"] = $password;
        $response["id"] = $id;
        $response["guestpw"] = $guestpw;
    }
    
    echo json_encode($response);
?>