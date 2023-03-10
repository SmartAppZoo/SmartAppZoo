/**
 *  Light > Dark
 *  Version 1.0.8 11/26/15
 *
 *  1.0.1 Added a verify so they event has to trip trice in a row to do the action.
 *  1.0.2 Added custom icon
 *  1.0.3 Revision to interface for better flow
 *  1.0.4 Added dimmer switches/levels, reorganized interface and added time restrictions options
 *  1.0.5 Fixed an inconsistent time limitiation code issue
 *  1.0.6 Added an About screen
 *  1.0.7 Code optimization
 *  1.0.8 Code fix to compensate for ST syntax change
 *
 *
 *	Using code from SmartThings Light Up The Night App and the Sunrise/Sunset app
 *  Copyright 2015 Michael Struck
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  Turn specific switched on when it gets dark. Can also turn off specific switches when it becomes light again based on brightness from a connected light sensor.
 *
 */
definition(
    name: "Light > Dark",
    namespace: "MichaelStruck",
    author: "Michael Struck",
    description: "Turn specific switches on when it gets dark and optionally changes the mode. Will also turn off specific switches and optionally change modes when it becomes light again based on brightness from a connected light sensor.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Light-Dark/LockDark.png",
    iconX2Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Light-Dark/LockDark@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/Light-Dark/LockDark@2x.png"
    )

preferences {
	page name: "getPref"
}

def getPref() {
    dynamicPage(name: "getPref", install:true, uninstall: true) {
        section("Monitor the luminosity and set brightness thresholds...") {
			input "lightSensor", "capability.illuminanceMeasurement", title: "Light Sensor"
			input "luxOn", "number", title: "Lower lux threshold (default=100)", required: false, defaultValue: 100
            input "luxOff", "number", title: "Upper lux threshold", required: false
        }
		section("Turn on lights/set mode when brightness below lower lux threshold...") {
			input "lightsOn", "capability.switch", multiple: true, title: "Lights/Switches", required: false
			input "dimmersOn","capability.switchLevel", multiple: true, required: false, title: "Dimmers"
            input "dimmerLevelOn", "enum", multiple:false, required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], title: "Turn on dimmers to this level (100% default)"
        	input "onMode", "mode", title: "Change mode to?", required: false
		}
    	section("Optionally, turn off lights/set mode when brightness above upper lux threshold...") {
			input "lightsOff", "capability.switch", multiple: true, title: "Lights/Switches", required: false
			input "dimmersOff","capability.switchLevel", multiple: true, title: "Dimmers", required: false
            input "dimmerLevelOff", "enum", multiple:false, required: false, options: [[0:"Off"],[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], title: "Turn off dimmers to this level (0% default)"
        	input "offMode", "mode", title: "Change mode to?", required: false
        }
        section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false, defaultValue: "Light > Dark")
		    href "timeIntervalInput", title: "Set for specific times", description: getTimeLabel(timeStart, timeEnd), state: greyedOutTime(timeStart, timeEnd)
            mode title: "Set for specific mode(s)", required: false
            href "pageAbout", title: "About ${textAppName()}", description: "Tap to get application version, license and instructions"
        }
	}
}

page(name: "pageAbout", title: "About ${textAppName()}") {
        section {
            paragraph "${textVersion()}\n${textCopyright()}\n\n${textLicense()}\n"
        }
        section("Instructions") {
            paragraph textHelp()
        }
}

//------------------------------------------------
def installed() {
	log.debug "Installed with settings: ${settings}"
    init()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    unsubscribe()
	init()
}

def init(){
 	state.dimLevelOn = dimmerLevelOn ? dimmerLevelOn as Integer : 100
	state.dimLevelOff = dimmerLevelOff ? dimmerLevelOff as Integer : 0
    state.lumOn = luxOn ? luxOn : 100
    state.lumOff = luxOff ? luxOff : state.lumOn
	state.lastStatus = lightSensor.latestValue("illuminance") < state.lumOn ? "on" : "off"
    
    subscribe(lightSensor, "illuminance", illuminanceHandler)
}

//Handlers
def illuminanceHandler(evt) {
  
    if (getTimeOk()) {
        if (state.lastStatus == "off" && evt.integerValue < state.lumOn) {
           	lightsOn?.on()
            dimmersOn?.setLevel(state.dimLevelOn)
        	state.lastStatus = "on"
 	        changeMode(onMode)
        } 
		if (state.lastStatus  == "on" && evt.integerValue > state.lumOff) {
           	lightsOff?.off()
           	dimmersOff?.setLevel(state.dimLevelOff)
        	state.lastStatus = "off"
            changeMode(offMode)
		} 
	}
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
		}
		else {
			log.debug "Unable to change to undefined mode '${newMode}'"
		}
	}
}

//Common Methods

def getTimeLabel(start, end){
	def timeLabel = "Tap to set"
	
    if(start && end){
    	timeLabel = "Between" + " " + hhmm(start) + " "  + "and" + " " +  hhmm(end)
    }
    else if (start) {
		timeLabel = "Start at" + " " + hhmm(start)
    }
    else if(end){
    timeLabel = "End at" + hhmm(end)
    }
	timeLabel	
}

def greyedOutTime(start, end){
	def result = start || end ? "complete" : ""
    result
}

private hhmm(time){
	new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", time).format("h:mm a", timeZone(time))
}

private getTimeOk() {
	def result = true
	if (timeStart && timeEnd) {
		def currTime = now()
		def start = timeToday(timeStart).time
		def stop = timeToday(timeEnd).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	else if (timeStart){
    	result = currTime >= start
    }
    else if (timeEnd){
    	result = currTime <= stop
    }
    result
}

page(name: "timeIntervalInput", title: "Run only during a certain time", refreshAfterSelection:true) {
		section {
			input "timeStart", "time", title: "Starting", required: false, refreshAfterSelection:true
			input "timeEnd", "time", title: "Ending", required: false, refreshAfterSelection:true
		}
}

//Version/Copyright/Information/Help

private def textAppName() {
	def text = "Light > Dark"
}	

private def textVersion() {
    def text = "Version 1.0.8 (11/26/2015)"
}

private def textCopyright() {
    def text = "Copyright © 2015 Michael Struck"
}

private def textLicense() {
    def text =
		"Licensed under the Apache License, Version 2.0 (the 'License');"+
		"you may not use this file except in compliance with the License."+
		"You may obtain a copy of the License at"+
		"\n\n"+
		"    http://www.apache.org/licenses/LICENSE-2.0"+
		"\n\n"+
		"Unless required by applicable law or agreed to in writing, software"+
		"distributed under the License is distributed on an 'AS IS' BASIS,"+
		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied."+
		"See the License for the specific language governing permissions and"+
		"limitations under the License."
}

private def textHelp() {
	def text =
    	"Similar to a night light, you can choose what lights or dimmers go on and then, separately, " +
        "which lights or dimmers go off based on the lux setting of a light sensor. In addition, when the lux thresholds are " +
        "met, different modes can be set." 
}
