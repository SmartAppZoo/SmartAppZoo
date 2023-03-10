/**
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
 *  Evohome Automation
 *
 *  Author: TomM
 *
 *  Date: 2016-04-05
 */
definition(
	name: "EvoHome Automation",
	namespace: "TomM",
	author: "TomM",
	description: "Automates Evohome	.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Thermostat to control") {
		input "thermo", "capability.thermostat", title: "Which Thermostat?", submitOnChange: true
	}
    if (thermo) {
            // Do something here to list each SmartThings mode (e.g. Home,
            // Away, Night etc.) and for each allow selection of the available
            // modes for the thermostat selected above
            //section {
            //    input(name: "dimmerLevel", type: "number", title: "Level to dim lights to...", required: true)
            //}
    }        
    section (title: "More options", hideable: true) {
        input "pushbool", "bool", title: "Send a push notification?"
        input "days", "enum", title: "Set for specific day(s) of the week", multiple: true, required: false,
            options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    }
}

def installed()
{   
	subscribe(location, "mode", modeChangeHandler)
}

def updated()
{
	unsubscribe()
	subscribe(location, "mode", modeChangeHandler)
}

def logHandler(evt) {
	log.debug evt.value
}

def modeChangeHandler(evt) {
	log.debug evt.value
	def thermoSensorStateold = thermo.currentThermostatMode
	log.debug "Old mode is $thermoSensorStateold"
    log.debug "Days selected $days"
    def temp = DaysOK
    log.debug "Days ok? $temp"
    //if (DaysOK){  
    	switch (evt.value) {
    	case "Away":
        	thermo?.setThermostatMode("Economy")
        	log.debug "Setting to Eco as AWAY"
        	notifications("economy",thermoSensorStateold)
        	break
    	case "Home":
        	thermo?.setThermostatMode("Auto")
        	log.debug "Setting to Auto as HOME"
        	notifications("auto",thermoSensorStateold)
        	break
    	case "Holiday":
        	thermo?.setThermostatMode("Off")
        	log.debug "Setting to Off as HOLIDAY"
        	notifications("off",thermoSensorStateold)
        	break
    	default:
        	thermo?.setThermostatMode("Auto")
        	log.debug "Setting to Auto as Default"
        	notifications("auto",thermoSensorStateold)
    	}
   	//}
    def thermoSensorStatenew = thermo.currentThermostatMode
	log.debug "New mode is $thermoSensorStatenew"
}

def notifications(evomode,evomodeold) {
	log.debug "Push boolean is $pushbool"
	log.debug "Old mode -$evomodeold- New mode -$evomode-"
    if(evomode != evomodeold) {
    	if (pushbool == true) {
    		log.debug "Push sent"
        	sendPush("Evohome set to $evomode")
        	}
    	else {
        	log.debug "Notification sent"
       		sendNotificationEvent("Evohome set to $evomode")
        	}
    	}	
 }
 
private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("Europe/London"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
    log.debug "daysOk = $result"
	result
}
