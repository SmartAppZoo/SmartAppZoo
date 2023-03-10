/**
 *  ecobeeEcho
 *
 *  Copyright 2015 Dan Lewandowski
 *  Based largely upon Yves Racine's ecobeeChangeMode (Thank you!)
 
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
	name: "ecobeeEcho",
	namespace: "tomagoes",
	author: "tomagoes",
	description:
	"Change the mode manually (by pressing the app's play button) and/or automatically at the ecobee thermostat(s)",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)

preferences {


	page(name: "selectThermostats", title: "Thermostats", install: false , uninstall: true, nextPage: "selectProgram") {
		section("About") {
			paragraph "ecobeeFollowSTmode, the smartapp that sets your ecobee thermostat to Heat/Cool/Off" + 
                		" based on SmartThings mode."
			paragraph "Version 1.0\n\n" +
				"If you like this app, please support the developer via PayPal:\n\7apbt7@gmail.com\n\n" +
				"Copyright©2015 Dan Lewandowski" + "This SmartApp and code is based largely upon yvesracine's ecobeeChangeMode.groovy" + 
				"Please check out his work at https://github.com/yracine/device-type.myecobee and support him via" +
                "PayPal:\n\nyracine@yahoo.com\n\n"
			href url: "http://github.com/tomagoes", style: "embedded", required: false, title: "More information...",
			description: "http://github.com/tomagoes"
		}
		section("Change the following ecobee thermostat(s)...") {
			input "thermostats", "device.myEcobeeDevice", title: "Which thermostat(s)", multiple: true
		}
	}
	page(name: "selectProgram", title: "Select Options", content: "selectProgram")
	page(name: "Notifications", title: "Notifications Options", install: true, uninstall: true) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
        section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
	}
}


def selectProgram() {
	state.lastMode = location.mode
    def prevMode = state.lastMode
    def ecobeePrograms = thermostats[0].currentThermostatMode.toString().minus('[').minus(']').tokenize(',')
	log.debug "programs: $ecobeePrograms"
	def enumModes=[]
	location.modes.each {
		enumModes << it.name
	}    

	return dynamicPage(name: "selectProgram", title: "Select Options", install: false, uninstall: true, nextPage:
			"Notifications") {
		section("Choose Heat Switch"){
        	input "switchHeat", "capability.switch", options: enumSwitches, multiple: false, required: true
        }
        section("Select ST Heat mode") {
			input "newModeH", "enum", options: enumModes, multiple:false, required: false
		}
        section("Choose Cool Switch"){
        	input "switchCool", "capability.switch", options: enumSwitches, multiple: false, required: true
        }
        section("Select ST Cool mode") {
			input "newModeC", "enum", options: enumModes, multiple:false, required: false
		}
        section("Choose Off Switch"){
        	input "switchOff", "capability.switch", options: enumSwitches, multiple: false, required: true
		}
        section("Select ST off mode") {
			input "newModeO", "enum", options: enumModes, multiple:false, required: false
		}

        
	}
}


def installed() {
	subscribe(location, changeMode)
	subscribe(app, changeMode)
}

def updated() {
	unsubscribe()
	subscribe(location, changeMode)
	subscribe(app, changeMode)
}



def changeMode(evt) {
	def message
	state.lastMode = location.mode
/*    message = "Current Mode is $locationMode and last is $state.lastMode" */
    send(message)
	Boolean foundMode=false 
    def curMode = location.currentMode
	if (curMode == newModeC){
        switchCool.on()
        switchHeat.off()
        switchOff.on()  
        thermostats?.setThermostatMode('cool')
    message = "ecobeeEcho>ST mode set to Cool.."
    send(message)
	message = "ecobeeEcho>setting the thermostat(s) to Cool.."
	send(message)
    }
    if (curMode == newModeH){
        switchCool.off()
        swtichHeat.on()
        switchOff.on()
        thermostats?.setThermostatMode('heat')
    message = "ecobeeEcho>ST mode set to Heat.."
    send(message)
	message = "ecobeeEcho>setting the thermostat(s) to Heat.."
	send(message)
    }
    if (curMode == newModeO){        
    	switchCool.off()
        switchHeat.off()
        switchOff.off()
        thermostats?.setThermostatMode('off')
    message = "ecobeeEcho>ST mode set to Air off.."
    send(message)
	message = "ecobeeEcho>setting the thermostat(s) to off.."
	send(message)
    }
	
    state.lastMode
    message = "ecobeeEcho>changing ST mode back to $state.lastMode"
    send(message)
}


private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)
	}
	if (phone) {
		log.debug("sending text message")
		sendSms(phone, msg)
	}

	log.debug msg
}
