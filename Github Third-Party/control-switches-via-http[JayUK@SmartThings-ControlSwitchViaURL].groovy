/*
//// Installation instructions ////
Login at https://graph.api.smartthings.com/
Navigate: My SmartApps > New SmartApp > From Code (tab)
Paste contents of rest-api.groovy
Click: Create, Publish > For Me
Navigate: App Settings > OAuth
Click: Enable OAuth in Smart App (make sure Redirect URI, Display Name and Display Link are blank)
Note the Client ID (i.e. abc123) and Client Secret (i.e. def321)

//// Get Access Token via OAuth ////
Replace the client_id value and open this URL in a browser: https://graph.api.smartthings.com/oauth/confirm_access?response_type=code&scope=app&redirect_uri=http%3A%2F%2Flocalhost&client_id=abc123
Login and Click "Authorize".
You will be redirected to http://localhost; note the code query value (i.e. aD4kF5 in http://localhost/?code=aD4kF5)
Run curl -v -H "Content-Type: application/x-www-form-urlencoded" -X POST --data 'grant_type=authorization_code&code=aD4kF5&client_id=abc123&client_secret=def321&redirect_uri=http%3A%2F%2Flocalhost' https://graph.api.smartthings.com/oauth/token
Note access_token in response (i.e. xyz123). This will be the access/bearer token used to authenticate to the REST API.
Run curl -v -H "Authorization: Bearer xyz123" https://graph.api.smartthings.com/api/smartapps/endpoints
Note the uri value (i.e. https://graph1.smartthings.com/api/smartapps/installations/123987), this will be the Endpoint URL used to access the REST API.

//// Usage instructions ////
The switch name must use %20 instead of a " " when using curl!!!!

list switches: curl -v -H "Authorization: Bearer f732a8dc-3a04-478c-99c1-422c85ea1e11" https://graph-eu01-euwest1.api.smartthings.com:443/api/smartapps/installations/fd9efc2f-0ffc-41eb-aaf1-742eb0564125/switches
turn on switch: curl -H "Authorization: Bearer f732a8dc-3a04-478c-99c1-422c85ea1e11" -X POST https://graph-eu01-euwest1.api.smartthings.com:443/api/smartapps/installations/fd9efc2f-0ffc-41eb-aaf1-742eb0564125/switches/UPS%20Fault/on
turn off switch: curl -H "Authorization: Bearer f732a8dc-3a04-478c-99c1-422c85ea1e11" -X POST https://graph-eu01-euwest1.api.smartthings.com:443/api/smartapps/installations/fd9efc2f-0ffc-41eb-aaf1-742eb0564125/switches/UPS%20Fault/off
toggle switch: curl -H "Authorization: Bearer f732a8dc-3a04-478c-99c1-422c85ea1e11" -X POST https://graph-eu01-euwest1.api.smartthings.com:443/api/smartapps/installations/fd9efc2f-0ffc-41eb-aaf1-742eb0564125/switches/UPS%20Fault/toggle
*/

definition(
    name: "Control switches via HTTP",
    namespace: "JayUK",
    author: "JayUK",
    description: "Allow switches to be controlled via HTTP requests",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Control switches via HTTP", displayLink: "http://localhost:4567"])

preferences {
	section ("Allow external service to control these things...") {
    input "switches", "capability.switch", multiple: true, required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {

}

mappings {
	path("/switches") {
    	action: [
      	GET: "listSwitches"
    	]
  	}
  	path("/switches/:switchName/:command") {
    	action: [
      	PUT: "updateSwitches",
        POST: "updateSwitches"
    	]
  	}
}

def listSwitches() {

	def resp = []
 
 	switches.each {
        resp << [displayNameLabel: it.displayName, name: it.name, value: it.currentValue("switch")]
    }
    return resp
}

def updateSwitches() {
    
    def command = params.command
    def switchName = params.switchName

    log.debug "Issued command $command for switch $switchName"
    
	def theSwitch = switches.find {it.displayName == "${switchName}"}
    log.debug "Selected switch ${theSwitch}"
    
    def switchCurrent = theSwitch.currentValue("switch")
    log.debug "Current value: ${switchCurrent}"
    
    switch(command) {
       	case "on":
			theSwitch.on()
	        break
	    case "off":
	        theSwitch.off()
	        break
	    case "toggle":
	     	if(switchCurrent == "on")
	            {theSwitch.off()}
	        else
	            {theSwitch.on()}
    	    break
	    default:
	        httpError(400, "$command is not a valid command for the specified switch")
	}
}
