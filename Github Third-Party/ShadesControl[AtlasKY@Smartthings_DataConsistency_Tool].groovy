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
 *  Based on Big Turn ON
 *
 *  Author: Luis Pinto
 */

definition(
	name: "Shades Controller",
	namespace: "smartthings",
	author: "Luis Pinto",
	description: "Control your shades with buttons.",
	category: "Convenience",
	iconUrl: "http://www.ezex.co.kr/img/st/window_close.png",
	iconX2Url: "http://www.ezex.co.kr/img/st/window_close.png"
)

preferences {
	section("Control Close Buttons...") {
		input "switchesOpen", "capability.button", multiple: true, title: "Open Buttons", required: true
		input "switchesClose", "capability.button", multiple: true, title: "Close Buttons", required: false
		input "switchesPause", "capability.button", multiple: true, title: "Pause Buttons", required: false
		input "dimmers", "capability.switchLevel", multiple: true, title: "Dimmers", required: false
		input "shades", "capability.switchLevel", multiple: true, title: "Shades"
		input "invertControl", "bool", title: "Invert controls"
	}

	section("Timers...") {
		input "theTimeOpen", "time", title: "Time to execute Open", required: false
		input "theTimeHalf", "time", title: "Time to execute 50%", required: false
		input "theTimeClose", "time", title: "Time to execute Close", required: false
	}
	
	section("Automation...") {
		input "sensor", "capability.temperatureMeasurement", title: "Sensor", required: false
		input "threshold", "number", title: "Temperature threshold", required: false
		input "desiredTemperature", "number", title: "Desired Temperature", required: false
		input "theTemperatureControlStart", "time", title: "Time to Start Automation", required: false
		input "theTemperatureControlEnd", "time", title: "Time to End Automation", required: false
		input "returnToOpen", "boolean", title: "Return To open position after automation", required: false
		input "automationEnabled", "boolean", title: "Enable automation", required: false
	}
	
}

def installed()
{
	subscribe(switchesClose, "button", buttonEventClose)
	subscribe(switchesOpen, "button", buttonEventOpen)
	subscribe(switchesPause, "button", buttonEventPause)
	subscribe(dimmers, "switch.setLevel", dimmersEvent)
	subscribe(dimmers, "switch", dimmersEvent)
	subscribe(dimmers, "switch.on", dimmersEvent)
	subscribe(dimmers, "switch.off", dimmersEvent)
	subscribe(shades, "windowShade", windowShadeEvent)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		setTemperature (sensor.currentTemperature)
	}
	
	if (theTimeOpen != null && theTimeOpen != "")
		schedule(theTimeOpen, "handlerSchOpen")
	if (theTimeHalf != null && theTimeHalf != "")
		schedule(theTimeHalf, "handlerSchHalf")
	if (theTimeClose != null && theTimeClose != "")
		schedule(theTimeClose, "handlerSchClose")

	if (automationEnabled == "true") {
		if (theTemperatureControlStart != null && theTemperatureControlStart != "")
			schedule(theTemperatureControlStart, "automationBeginFunction")
		if (theTemperatureControlEnd != null && theTemperatureControlEnd != "")
			schedule(theTemperatureControlEnd, "automationEndFunction")
	}
	state.defaultPosition = 0
	log.info "Storing current position to return after automation, value is ${state.defaultPosition}"

		
}


def updated()
{
	unsubscribe()
	unschedule()
	installed()
}

def temperatureHandler(evt)
{
	setTemperature (evt.doubleValue)
}

def automationBeginFunction () {
	if (automationEnabled == "true") {
		state.defaultPosition = getLevel()
		log.info "Storing current position to return after automation, value is ${state.defaultPosition}"
		return
	}

}

def automationEndFunction () {
	if (automationEnabled == "true") {
		log.info "End of automation returning to open position, value ${state.defaultPosition}"
		shades.setLevel(state.defaultPosition)
	}

}

def setTemperature(currentTemp) {
	log.info "automation is $automationEnabled and temperature is " + currentTemp

	if (automationEnabled == "false") {
		log.info "Automation not enabled, nothing to do"
		return
	}

	if (theTemperatureControlStart && theTemperatureControlEnd) {
		if(!timeOfDayIsBetween(theTemperatureControlStart, theTemperatureControlEnd, (new Date()), location.timeZone))
		{
			log.info "Out of time, nothing to do"
			return
		}
	}

	if (sensor && desiredTemperature) {
		def difference = currentTemp - desiredTemperature
		def percentage = 0;

		if (difference > threshold)
			difference = threshold
		else if (difference < 0)
			difference = 0
//            difference = -1*threshold

		log.info "difference is " + difference

		if (difference > 0) {
//            percentage = 50 + difference * 100 / (2*threshold)
			percentage = difference * 100 / threshold
			shades.setLevel(percentage)
			log.info "move curtains to " + percentage
		} else {
			log.info "Temperature is bellow desired, nothing to do"
		}

//        shades.setLevel(percentage)

	}
}

def windowShadeEvent (evt) {
	log.debug "windowShadeEvent Event: ${evt.value}"
	state.isWorking = evt.value == "opening" || evt.value == "closing"
}

def dimmersEvent(evt) {
	log.info "switchSetLevelHandler Event: ${level}"
	if (evt.value == "on") {
		shades.close()
		return
	}
	if (evt.value == "off" ){
		shades.open()
		return
	}
	def level = evt.value.toFloat()
	level = level.toInteger()
	shades.setLevel(level)
}

def handlerSchOpen(evt) {
	if (automationEnabled == "true")
		shades.open()
}

def handlerSchHalf(evt) {
	if (automationEnabled == "true")
		shades.setLevel(50)
}

def handlerSchClose(evt) {
	if (automationEnabled == "true")
		shades.close()
}

def buttonEventPause(evt) {
	log.debug "Pausing Shades: $evt"
	shades.pause();
}

def buttonEventClose(evt) {
	log.debug "Closing Shades"

	if (isWorking()) {
		shades.pause()
	}
	else if ((evt.value == "held" && !invertControl) || (evt.value == "pushed" && invertControl)) {
		log.debug "button was held"
		shades.close()
	  } else if ((evt.value == "pushed" && !invertControl) || (evt.value == "held" && invertControl)) {
		log.debug "button was pushed"
		if (getLevel() <100 && getLevel() >=75)
			shades.close()
		else if (getLevel() <75 && getLevel() >=50)
			shades.setLevel(75)
		else if (getLevel() <50 && getLevel() >=25)
			shades.setLevel(50)
		else if (getLevel() <25 && getLevel() >=0)
			shades.setLevel(25)
	  }

}

def buttonEventOpen(evt) {
	log.debug "Opening shades:"

	   if (isWorking()) {
		shades.pause()
	}
	else if (switchesClose == null) {
		if (getLevel() < 50)
			shades.close()
		else
			shades.open()
	}
	else if ((evt.value == "held" && !invertControl) || (evt.value == "pushed" && invertControl)) {
		log.debug "button was held"
		shades.open()
	  } else if ((evt.value == "pushed" && !invertControl) || (evt.value == "held" && invertControl)) {
		if (getLevel() <=100 && getLevel() >75)
			shades.setLevel(75)
		else if (getLevel() <=75 && getLevel() >50)
			shades.setLevel(50)
		else if (getLevel() <=50 && getLevel() >25)
			shades.setLevel(25)
		else if (getLevel() <=25 && getLevel() >0)
			shades.open()
	}
}

def isWorking(){
	return state.isWorking
/*	def isworking
	isworking = shades[0].currentState("windowShade").value == "opening" || shades[0].currentState("windowShade").value == "closing"
	log.debug "Is working: " + isworking
	return isworking
*/
}

def getLevel(){
	return shades[0].currentValue("level")
}
