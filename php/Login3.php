<?php
	// This php script is for the android screen where a user is instructed to enter a serial number. 
	// A valid serial number (checked with predefined set in database) will allow you to register an account.
	// A invalid serial number will result in an error. 

	// These are our connection parameters that are required to connect to the database.
    $con = mysqli_connect("localhost", "id981978_yeslock", "Ham8932414", "id981978_eec136");
    
	// We are only passing in one parameter, Serial Numbers, to compare with the databases predefined set of serial numbers. 
    $serial_number = $_POST["serial_number"];
    
	// We specify here that we are selecting values from the SERIAL_NUMBERS database to compare with the JSON object above.
    $statement = mysqli_prepare($con, "SELECT * FROM serial_numbers WHERE serial_number = ?");
    mysqli_stmt_bind_param($statement, "s", $serial_number);
    mysqli_stmt_execute($statement);
    
	// We store the value of the serial number into a variable
	// We bind the result to match the parameters of the database exactly!
    mysqli_stmt_store_result($statement);
    mysqli_stmt_bind_result($statement, $serial_number);
    
	// Response from the server
    $response = array();
    $response["success"] = false;  
    
	// If successfull, the input matches a serial number and a boolean true is passed
    while(mysqli_stmt_fetch($statement)){
        $response["success"] = true;  
        $response["serial_number"] = $serial_number;

    }
    
    echo json_encode($response);
?>