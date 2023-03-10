/**
 *  Twilio IVR API Access Application
 *
 *  Author: SmartThings
 */

preferences {
	section("Allow Twilio to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true
		input "locks", "capability.lock", title: "Which Locks?", multiple: true
        input "authorizedNumbers", "text", title: "Authorized Number?", multiple: true
   		input "feeder", "device.PetFeederShield", title: "Pet Feeder?"
        input "petName", "text", title: "Pets Name"
        input "areaCode","text", title: "Area Code", required: true
	}
}

mappings {
	path("/main") {
		action: [
			POST: "mainResponder",
            GET: "mainResponder"
		]
	}

	path("/main/response") {
		action: [
			POST: "mainResponseHandler",
            GET: "mainResponseHandler"
		]
	}
    
  	path("/switches/response") {
		action: [
			POST: "switchesResponseHandler",
            GET: "switchesResponseHandler"
		]
	}
    
    path("/locks/response") {
		action: [
			POST: "locksResponseHandler",
            GET: "locksResponseHandler"
		]
	}
    
    path("/modes/response") {
		action: [
			POST: "modesResponseHandler",
            GET: "modesResponseHandler"
		]
	}
    
    path("/sms") {
		action: [
			POST: "receiveSmsHandler",
            GET: "receiveSmsHandler"
		]
	}
}

def provisionSubAccount() {

	def successClosure = { response ->
 		 log.debug "Request was successful, $response"
	}
    
    log.debug "Provisioning Twilio Sub-Account"

	def friendlyName = "ST_${location.id}"
    
    def twilioSID = "ACc7d8967316a264470ad4b9e9c3a2dd09"
    def twilioAuthToken = "5e3da207e6ff945f655f2f8ced930e05"
   
   	def postBody="FriendlyName=$friendlyName"
    def url = "https://$twilioSID:$twilioAuthToken@api.twilio.com/2010-04-01/Accounts.json"
    
    log.debug "Url = $url"
    log.debug "Post Body=$postBody"
    
    def result = null
        
	httpPost(url, postBody) {response -> 
    	result = response
	}

	log.debug "SID=${result.data.sid}, Auth Token=${result.data.auth_token}"
    
    // Hang onto the information that we need about the account.
    
    state.twilioSID = result.data.sid
    state.twilioAuthToken = result.data.auth_token
    
    settings.twilioSID = result.data.sid
    
}

def provisionPhoneNumber() {
    
// Now provision a new number in the selected area code

    log.debug "Provisioning New Twilio Phone Number"
        
    def voiceUrl = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/main?access_token=${state.accessToken}".encodeAsURL()
    def smsUrl = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/sms?access_token=${state.accessToken}".encodeAsURL()
    
    def postBody =  "AreaCode=${settings.areaCode}&" +
    				"VoiceMethod=GET&" +
                    "VoiceUrl=${voiceUrl}&" +
                    "SmsMethod=POST&" +
                    "SmsUrl=${smsUrl}"
     
    def url = "https://${state.twilioSID}:${state.twilioAuthToken}@api.twilio.com/2010-04-01/Accounts/${state.twilioSID}/IncomingPhoneNumbers.json"
    
    log.debug "Post Body: ${postBody}"
   
    def result = null
       
	httpPost(url, postBody) {response -> 
    	result = response
	}

	log.debug "Response=${result.data}"
	log.debug "Phone Number=${result.data.phone_number}"
    log.debug "Phone SID=${result.data.sid}"
    
    // Store the Phone Number Information
    
    state.areaCode = settings.areaCode
    state.phoneNumber = result.data.phone_number
    state.phoneSID = result.data.sid
    
    // Notify the user. Best we have right now. This should be a) in feed, b) push notification, and c) as a read-only SETTING that the user can see
    
    sendPush ("New Twilio IVR Phone Number Assigned = ${state.phoneNumber}")

}

def removePhoneNumber() {
    
// Now DE-provision the current phone number

	if (state.phoneSID != "") {

    	log.debug "De-Provision Existing Twilio Phone Number"

   		def url = "https://${state.twilioSID}:${state.twilioAuthToken}@api.twilio.com/2010-04-01/Accounts/${state.twilioSID}/IncomingPhoneNumbers/${state.phoneSID}"
    
    	log.debug "Url = $url"
   
    	def result = null
        
		httpDelete(url) {response -> 
    		result = response
		}
        
        // Notify the user
        
        sendPush ("Twilio IVR Phone Number (${state.phoneNumber}) has been removed from your location.")
    
    	// Clear the Phone Number Information
    
    	state.phoneNumber = ""
    	state.phoneSID = ""
	}

}

def installed() {

	// Generate a new OAUTH access token
    createAccessToken()

	// provision a new sub-account
	provisionSubAccount()
    
    // Provision New Number
    
    provisionPhoneNumber()

}  

def uninstalled() {

	// Revoke Access Token
    revokeAccessToken()
    
    // De-Provision Phone Number
    removePhoneNumber()
    
    // De-Provision Account? We could re-use the same sub-account SID later ... TODO
    
}

def updated() {

    log.debug "state.twilioSID = ${state.twilioSID}"
    log.debug "state.twilioAuthToken = ${state.twilioAuthToken}"
    
    // Handle Area Code Change with New Number Provisioning and Removal of Old Number
    
    if (state.areaCode != settings.areaCode) {
    
    	// Delete Old Phone Number
        removePhoneNumber()
    
    	// Provision New Number
    	provisionPhoneNumber()
    
    }

}

def mainResponder() {

    log.debug "Access Token = ${params.access_token}"
    state.oauth_token = params.access_token

    log.debug "Build the TwiML response ..."
    log.debug "URI=$request.forwardURI"
    
// Here is the basic XML response we need to get back to Twilio...
// <?xml version="1.0" encoding="UTF-8"?>
// <Response>
//    <Gather action="/api/smartapps/installations/<appInstanceUUID>/main/response" numDigits="1">
//        <Say>Hi. Thanks for calling your Smart Things Twilio App.</Say>
//        <Say>To control switches, press 1.</Say>
//        <Say>To control locks, press 2.</Say>
//        <Say>To change modes, press 3.</Say>
//        <Say>To end this call, press 9.</Say>
//    </Gather>
//    <!-- If customer doesn't input anything, prompt and try again. -->
//    <Say>Sorry, I didn't get your response.</Say>
//    <Redirect>/api/smartapps/installations/<appInstanceUUID>/main</Redirect>
// </Response>
    
    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    log.debug "authorized: $authorizedNumbers"
    
    // see if the From number is authorized.
    if (authorizedNumbers.contains(params.From)) {
       
       log.debug "Found $params.From in list of authorized phone numbers"
    
    def gather = document.createElement "Gather"
    gather.setAttribute("action","https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main/response?access_token=${state.oauth_token}")
    gather.setAttribute("numDigits","1")
    
    def say = document.createElement "Say"
    say.setAttribute("voice", "man")
    say.appendChild(document.createTextNode("Hello. This is your Smart Things Twilio Application."))
    gather.appendChild say
    
    
    def say1 = document.createElement "Say"
    say1.setAttribute("voice", "man")
    say1.appendChild(document.createTextNode("To control switches, press 1."))
    gather.appendChild say1
    
    def say2 = document.createElement "Say"
    say2.setAttribute("voice", "man")
    say2.appendChild(document.createTextNode("To control door locks, press 2."))
    gather.appendChild say2
    
    def say3 = document.createElement "Say"
    say3.setAttribute("voice", "man")
    say3.appendChild(document.createTextNode("To change modes, press 3."))
    gather.appendChild say3
    
    def say4 = document.createElement "Say"
    say4.setAttribute("voice", "man")
    say4.appendChild(document.createTextNode("To feed $settings.petName, press 4."))
    gather.appendChild say4
    
    def say9 = document.createElement "Say"
    say9.setAttribute("voice", "man")
    say9.appendChild(document.createTextNode("To end this call, press 9."))
    gather.appendChild say9
    
    response.appendChild gather
    
    def sayDontUnderstand = document.createElement "Say"
    sayDontUnderstand.setAttribute("voice", "man")
    sayDontUnderstand.appendChild(document.createTextNode("Sorry, I didn't understand your response."))
    response.appendChild sayDontUnderstand
    
    def redirect = document.createElement "Redirect"
    redirect.appendChild(document.createTextNode(request.forwardURI))
    response.appendChild redirect
    
    }
    else { // unauthorized number calling
    
    	def sayUnauthorized = document.createElement "Say"
    	sayUnauthorized.setAttribute("voice", "man")
    	sayUnauthorized.appendChild(document.createTextNode("You are calling from an unauthorized number."))
    	response.appendChild sayUnauthorized
        
        def hangup = document.createElement "Hangup"
    	response.appendChild hangup 
    
    }
    
    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document
    
}

def mainResponseHandler() {

    state.oauth_token = params.access_token
    
    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    def digits = params.Digits
    log.debug "Digits = $digits"

    if (digits == "1") { // Build the switch specific IVR sub-menu
    	log.debug "User pressed 1."
        
        def gather = document.createElement "Gather"
    	gather.setAttribute("action","https://graph.api.smartthings.com/api/smartapps/installations/$app.id/switches/response?access_token=${state.oauth_token}")
    	gather.setAttribute("numDigits","1") 
    
    	def say1 = document.createElement "Say"
    	say1.setAttribute("voice", "man")
    	say1.appendChild(document.createTextNode("To turn the switches on, press 1."))
    	gather.appendChild say1
    
    	def say2 = document.createElement "Say"
    	say2.setAttribute("voice", "man")
    	say2.appendChild(document.createTextNode("To turn the switches off, press 2."))
    	gather.appendChild say2
        
        def say9 = document.createElement "Say"
    	say9.setAttribute("voice", "man")
    	say9.appendChild(document.createTextNode("To return to the main menu, press 9."))
    	gather.appendChild say9
        
        response.appendChild gather
        
    }
    else if (digits == "2") { // Build the lock specific IVR sub-menu
		log.debug "User pressed 2."
        
        def gather = document.createElement "Gather"
    	gather.setAttribute("action","https://graph.api.smartthings.com/api/smartapps/installations/$app.id/locks/response?access_token=${state.oauth_token}")
    	gather.setAttribute("numDigits","1") 
    
    	def say1 = document.createElement "Say"
    	say1.setAttribute("voice", "man")
    	say1.appendChild(document.createTextNode("To lock all doors, press 1."))
    	gather.appendChild say1
    
    	def say2 = document.createElement "Say"
    	say2.setAttribute("voice", "man")
    	say2.appendChild(document.createTextNode("To unlock all doors, press 2."))
    	gather.appendChild say2
        
        def say9 = document.createElement "Say"
    	say9.setAttribute("voice", "man")
    	say9.appendChild(document.createTextNode("To return to the main menu, press 9."))
    	gather.appendChild say9
        
        response.appendChild gather
        
    }
    else if (digits == "3") { // Build the mode specific IVR sub-menu
    
		log.debug "User pressed 3."
        
        def gather = document.createElement "Gather"
    	gather.setAttribute("action","https://graph.api.smartthings.com/api/smartapps/installations/$app.id/modes/response?access_token=${state.oauth_token}")
    	gather.setAttribute("numDigits","1") 
        
        // tell them what mode they are currently in
                
        def currentMode = document.createElement "Say"
    	currentMode.setAttribute("voice", "man")
    	currentMode.appendChild(document.createTextNode("This location is currently in ${location.mode} mode."))
    	gather.appendChild currentMode
        
        def modeNumber = 1
        
        for (theMode in location.modes) {
        
            def sayModeOption = document.createElement "Say"
    		sayModeOption.setAttribute("voice", "man")
    		sayModeOption.appendChild(document.createTextNode("To enter ${theMode.name} mode, press $modeNumber."))
    		gather.appendChild sayModeOption
            modeNumber++
        
        }
        
        def say9 = document.createElement "Say"
    	say9.setAttribute("voice", "man")
    	say9.appendChild(document.createTextNode("To return to the main menu, press 9."))
    	gather.appendChild say9
        
        response.appendChild gather
        
    }
    else if (digits == "4") { // Feed the Dog Now
    
    	if (settings.feeder) { // if pet feeder configured
        
        	feeder.feed()
            
            def feedResponse = document.createElement "Say"
    		feedResponse.setAttribute("voice", "man")
    		feedResponse.appendChild(document.createTextNode("${settings.petName} has been fed."))
    		response.appendChild feedResponse
            
        }
        else {
                    
            def feedResponse = document.createElement "Say"
    		feedResponse.setAttribute("voice", "man")
    		feedResponse.appendChild(document.createTextNode("No pet feeder is currently configured."))
    		response.appendChild feedResponse
        
        }
        
        def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect    
        
    }
    else if (digits == "9") { // return locks list as IVR menu
		log.debug "User pressed 9."
        
        def say = document.createElement "Say"
        say.setAttribute("voice", "man")
        say.appendChild(document.createTextNode("Thank you for calling. Goodbye."))
        response.appendChild say
        
        def hangup = document.createElement "Hangup"
    	response.appendChild hangup      
        
    }
    else {  // unknown response error

    }
    
    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document

}

def switchesResponseHandler() {

    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    def digits = params.Digits
    log.debug "Digits = $digits"

    if (digits == "1") { // turn switches on
    	log.debug "User pressed 1."
        
        switches.on()
        
        def say = document.createElement "Say"
        say.setAttribute("voice", "man")
        say.appendChild(document.createTextNode("The switches are now on."))
        response.appendChild say
        
        def pause = document.createElement "Pause"
   		pause.appendChild(document.createTextNode("2"))
    	response.appendChild pause
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect   
        
    }
    else if (digits == "2") { // turn switches off
		log.debug "User pressed 2."
        
        switches.off()
        
        def say = document.createElement "Say"
        say.setAttribute("voice", "man")
        say.appendChild(document.createTextNode("The switches are now off."))
        response.appendChild say
        
        def pause = document.createElement "Pause"
   		pause.appendChild(document.createTextNode("2"))
    	response.appendChild pause
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect   
        
    }
    else if (digits == "9") { // return to main menu
		log.debug "User pressed 9."
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect    
        
    }
    else {  // unknown response error

    }
    
    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document

}

def locksResponseHandler() {

    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    def digits = params.Digits
    log.debug "Digits = $digits"

    if (digits == "1") { // lock all doors
    	log.debug "User pressed 1."
        
        locks.lock()
        
        def say = document.createElement "Say"
        say.setAttribute("voice", "man")
        say.appendChild(document.createTextNode("The doors are now locked."))
        response.appendChild say
        
        def pause = document.createElement "Pause"
   		pause.appendChild(document.createTextNode("2"))
    	response.appendChild pause
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect    
        
    }
    else if (digits == "2") { // unlock all doors
		log.debug "User pressed 2."
        
        locks.unlock()
        
        def say = document.createElement "Say"
        say.setAttribute("voice", "man")
        say.appendChild(document.createTextNode("The doors are now unlocked."))
        response.appendChild say
        
        def pause = document.createElement "Pause"
   		pause.appendChild(document.createTextNode("2"))
    	response.appendChild pause
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect   
        
    }
    else if (digits == "9") { // return to main menu
		log.debug "User pressed 9."
        
    	def redirect = document.createElement "Redirect"
    	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
    	response.appendChild redirect       
        
    }
    else {  // unknown response error

    }
    
    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document

}

def modesResponseHandler() {

    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    def digits = params.Digits
    log.debug "Digits = $digits"
    
    // figure out which mode the user wants based on the value they entered
    
    def int selectedDigit = digits.toInteger()
    
    log.debug "Modes size: ${location.modes.size}"
    
    if (selectedDigit > 0 && selectedDigit <= location.modes.size) {
    
    	def selectedMode = location.modes[selectedDigit-1]
        setLocationMode(selectedMode)
        
        def newMode = document.createElement "Say"
        newMode.setAttribute("voice", "man")
        newMode.appendChild(document.createTextNode("The location mode has been changed to $selectedMode."))
        response.appendChild newMode
        
    }
    
    // return to main menu        
   	def redirect = document.createElement "Redirect"
   	redirect.appendChild(document.createTextNode("https://graph.api.smartthings.com/api/smartapps/installations/$app.id/main?access_token=${state.oauth_token}"))
   	response.appendChild redirect 

    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document

}

def receiveSmsHandler() {

    log.debug "From: ${params.From}"
    log.debug "Message: ${params.Body}"
    
    def document = newXmlDocument()
    def response = document.createElement "Response"
    
    // Tokenize and then process the SMS message
    
    def commandString = params.Body.toLowerCase()
    
    def tokens = commandString.tokenize(' ')
    def returnMessage = ""
    
    log.debug "Number of tokens = ${tokens.size}"
    
    if (tokens.size > 1) { // big enough
    
    	log.debug "Command: ${tokens[0]}.${tokens[1]}()"
    
    	switch (tokens[0]) {
        
        	case "switches":
    			switches."${tokens[1]}"()
                returnMessage = "Command: switches.${tokens[1]} Executed Successfully."
                break;
                
            case "locks":
    			locks."${tokens[1]}"()
                returnMessage = "Command: locks.${tokens[1]} Executed Successfully."
                break;
                
            case "mode":
               	// TODO: change mode
                def selectedMode = location.modes.find{it.name.equalsIgnoreCase(tokens[1])}
                if (selectedMode) {
                   setLocationMode (selectedMode)
                }
                returnMessage = "Command: Mode change to ${tokens[1]} Executed Successfully."
        		break; 
   
        
            default:
            	returnMessage = "Unknown Command: ${params.Body}."
                
         }
        
    }
    else if (tokens.size == 1) {
    
        
    	log.debug "Command: ${tokens[0]}.${tokens[1]}()"
    
    	switch (tokens[0]) {
                          
            case "feed":
               	// TODO: feed pet
                returnMessage = "Command: mode.${tokens[1]} Executed Successfully."
                if (settings.feeder) {
                   feeder.feed()
                   returnMessage = "Command: feed Executed Successfully."
                }
        		break;  
                
            case "?":
               	// change mode
                returnMessage = "Available Commands:\n\nswitches <on/off>\nlocks <lock/unlock>\nmode <mode name>\nFeed ${settings.petsName}"
        		break; 
        
        }
        
    }
    
    def Sms = document.createElement "Sms"
    Sms.setAttribute("voice", "man")
    Sms.appendChild(document.createTextNode(returnMessage))
    response.appendChild Sms
    
        
    document.appendChild response
    
    render status: 200, contentType: 'application/xml', data: document

}
