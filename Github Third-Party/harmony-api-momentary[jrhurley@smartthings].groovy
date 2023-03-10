/**
 *  Harmony API - Momentary 
 *  Controls devices via Harmony API
 *  Responds to the pushed event of a momentary button
 *
 *  Version 1.1
 *   - 1.0 Initial version
 *   - 1.1 Updated to use Harmony API server device rather than hardwiring server IP and port
 *
 *  Copyright 2017 Jonathon Hurley
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
    name: "Harmony API - Momentary",
    namespace: "jrhurley",
    author: "Jonathon Hurley",
    description: "Control devices through a Harmony API server triggered by the pushed event of a momentary switch",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
	page(name: "page-basic", title: "Basic settings", nextPage: "page-on", uninstall: true) {
		section("When this momentary switch is activated") {
        	input "theSwitch", "capability.momentary", required: true, title: ""
        }
        section("Send commands to this Harmony API server") {
        	input "theServer", "device.harmonyAPI-DeviceHandler", required: true, title: ""
        }
        section("") {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
    	}
    }
    
    page(name: "page-on", title: "Button pushed", install:true) {
    	section() {
        	paragraph "Enter the commands to be sent to harmonyapi when the button is pushed.  They will be sent in the listed order."
        	paragraph "Format: hub-slug/device-slug/command-slug"
        	input "commandOn1", "text", required: true, title: "Command 1"
            input "commandOn2", "text", required: false, title: "Command 2"
            input "commandOn3", "text", required: false, title: "Command 3"
            input "commandOn4", "text", required: false, title: "Command 4"
            input "commandOn5", "text", required: false, title: "Command 5"
    	}
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(theButton, "momentary.pushed", buttonPressHandler)
}

// Event handlers

def buttonPressHandler(evt) {    
	log.debug("Button press handler")
    
    loopThroughCommands([commandOn1, commandOn2, commandOn3, commandOn4, commandOn5])
    
	log.debug("End of handler.")
}

def loopThroughCommands(commands) {
	commands.each { command ->
    	if (command == null || command == "") {
        	return
        }
        
    	theServer.sendCommand(command)
    }
}





////////////////////////////////////////
/*
def parseCommand(command) {
	return command.tokenize("/")
}

def harmonyCommand(cmd) {
	// Settings for harmonyapi server
	def harmonyapiServer = "10.0.1.29"
	def harmonyapiPort = 8282
    
    def parsedCommand = parseCommand(cmd)
    log.debug("Parsed command: $parsedCommand")
    
    def hub = parsedCommand[0]
    def device = parsedCommand[1]
    def command = parsedCommand[2]
    
    if (hub == "" || hub == null || device == "" || device == null || command == "" || command == null) {
    	log.debug("Error in command")
        return
    }
    
    try {
    	//sendHubCommand(new physicalgraph.device.HubAction("""GET /hubs HTTP/1.1\r\nHOST: 10.0.1.23:8282\r\n\r\n""", physicalgraph.device.Protocol.LAN))//, null, [callback: calledBackHandler]))//, "0A000117:205A"))
    	sendHubCommand(new physicalgraph.device.HubAction("""POST /hubs/$hub/devices/$device/commands/$command HTTP/1.1\r\nHOST: $harmonyapiServer:$harmonyapiPort\r\nContent-Length:0\r\n\r\n""", physicalgraph.device.Protocol.LAN, null, [callback: callbackHandler]))
        log.debug("Hub command sent")
	}
	catch (Exception e) {
		log.debug("Exception $e when trying sendHubCommand")
    }
}

void callbackHandler(physicalgraph.device.HubResponse hubResponse) {
	log.debug("callbackHandler $hubResponse.status -- $hubResponse.headers")
}
*/