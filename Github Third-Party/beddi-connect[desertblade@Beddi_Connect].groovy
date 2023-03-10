/**
 *  Beddi Connect
 *
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: textAppName(),
    namespace: "desertblade",
    author: "Ben W",
    description: "Basic Beddi Integrations",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    //startPage
    page(name: "pageMain")
        
    page(name:"pageBeddiSettings")
    
    
    }
    
 def pageMain() {
      dynamicPage(name: "pageMain", title: none, uninstall: false, install: true){
   
      section("Devices") {
        input "switch1", "capability.switch", title: "Switch 1", multiple: false, required: false
        input "switch2", "capability.switch", title: "Switch 2", multiple: false, required: false
        input "switch3", "capability.switch", title: "Switch 3", multiple: false, required: false
        input "switch4", "capability.switch", title: "Switch 4", multiple: true, required: false
        }
      
      section("Options") {
			href ("pageBeddiSettings", title: "Beddi Settings", description: "Tap to get Beddi HTTP settings or to reset the access token",image: "")
        	label(title: "Label this SmartApp", required: false, defaultValue: "")
        }

	}
}

// Since the SmartApp doesn't have any dependencies when it's installed or updated,
// we don't need to worry about those states.
def installed() {}
def updated() {}


def pageBeddiSettings(){
    dynamicPage(name: "pageBeddiSettings", title: none, uninstall: false){
        section { paragraph "Settings" }	
        section ("Other Values/Variables"){

  
        	if (!state.accessToken) OAuthToken()
            if (!state.accessToken) paragraph "**You must enable OAuth via the IDE to setup this app**"
            else href url:"${getApiServerUrl()}/api/smartapps/installations/${app.id}/setup?access_token=${state.accessToken}", style:"embedded", required:false, title:"Beddi HTTP Requests", description: none
      }	
        section ("Advanced") { 
            href "pageConfirmation", title: "Revoke/Reset Access Token", description: "Tap to confirm this action",
            	image: ""
        }
    }
}


// This block defines an endpoint, and which functions will fire depending on which type
// of HTTP request you send
mappings {
    // The path is appended to the endpoint to make requests
    path("/setup") { action: [GET: "displayData"] }
    path("/switchOne/:command"){
    	action: [
        	PUT: "switchOneCommand"
        ]
    
    }
        path("/switchTwo/:command"){
    	action: [
        	PUT: "switchTwoCommand"
        ]
    
    }    
        path("/switchThree/:command"){
    	action: [
        	PUT: "switchThreeCommand"
        ]
    
    }
    path("/switchFour/:command"){
    	action: [
        	PUT: "switchFourCommand"
        ]
    
    }
      path("/switchTwoThree/:command"){
    	action: [
        	PUT: "switchTwoThreeCommand"
        ]
    
    }
}

void switchOneCommand() {
    def command = params.command
    switchCommand(switch1, command)
}

void switchTwoCommand() {
    def command = params.command
    switchCommand(switch2, command)
}

void switchTwoThreeCommand() {
    def command = params.command
    switchCommand(switch2, command)
    switchCommand(switch3, command)
}

void switchThreeCommand() {
    def command = params.command
    switchCommand(switch3, command)
}

void switchFourCommand() {
    def command = params.command
    switchCommand(switch4, command)
}

void switchCommand(theSwitch,command) {
    // use the built-in request object to get the command parameter
    //def command = params.command

    // all switches have the comand
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "on":
            theSwitch.on()
            break
        case "off":
            theSwitch.off()
        case "toggle":
        	if (theSwitch.currentValue("switch") == "off") {
            	theSwitch.on()
            } else {
            	theSwitch.off()
             }
             break
       	case "set":
           		theSwitch.setLevel(50)
          break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }

}

def OAuthToken(){
	try {
        createAccessToken()
		log.debug "Creating new Access Token"
	} catch (e) { log.error "Access Token not defined. OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth." }
}

// Callback functions
def listSwitches() {

    def resp = []
    switches.each {
        resp << [id: it.id, name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}



private device(it, type) {
	it ? [id: it.id, label: it.label, type: type, currentState: it.currentState("${type}").value] : null
    //it ? [it.currentState("${type}")] : null
}

def setupData(){
	log.info "Set up web page located at : ${getApiServerUrl()}/api/smartapps/installations/${app.id}/setup?access_token=${state.accessToken}"
	def result = """
    <b> Use PUT as http reqeust type!</b> </br>
     <b>You can change "toggle" to On/Off/Set</b>
    				<br><hr><br>
                    <i><b>Switch 1</b></i>
                    <br><br>
                    ${getApiServerUrl()}/api/smartapps/installations/${app.id}/switchOne/toggle?access_token=${state.accessToken}
                    <br><br><hr></div>
                    
                    <i><b>Switch 2</b></i>
                    <br><br>
                    ${getApiServerUrl()}/api/smartapps/installations/${app.id}/switchTwo/toggle?access_token=${state.accessToken}
                    <br><br><hr></div>
   
                    <i><b>Switch 3</b></i>
                    <br><br>
                    ${getApiServerUrl()}/api/smartapps/installations/${app.id}/switchThree/toggle?access_token=${state.accessToken}
                    <br><br><hr></div>
                    
                    <i><b>Switch 4 -- Multiple switches</b></i>
                    <br><b>Off</b><br>
                    ${getApiServerUrl()}/api/smartapps/installations/${app.id}/switchFour/off?access_token=${state.accessToken}
                    <br><br><hr></div>
                    
                    
                 """
}

def displayData(){
	render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8" /><meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"/></head><body style="margin: 0;">${setupData()}</body></html>"""
}


private def textAppName() { def text = "Beddi Connect" }