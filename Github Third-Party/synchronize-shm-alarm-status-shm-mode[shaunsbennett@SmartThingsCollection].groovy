/**
 *  Synchronize SHM Alarm Status SHM Mode
 *
 *  Copyright 2020 Shaun S Bennett
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
    name: "Synchronize SHM Alarm Status SHM Mode",
    namespace: "shaunsbennett",
    author: "Shaun S Bennett",
    description: "Set Smart Home Monitor (SHM) status and synchronize with SHM mode",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/alarm/alarm/alarm.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/alarm/alarm/alarm@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/alarm/alarm/alarm@2x.png")


preferences {
      section("Select your button devices (optional)") {
            input "buttonDeviceAway", "capability.button", title: "SHM Away, Mode Away", multiple: false, required: false
            input "buttonDeviceStay", "capability.button", title: "SHM Stay, Mode Night", multiple: false, required: false
            input "buttonDeviceDisarm", "capability.button", title: "SHM Disarm, Mode Home", multiple: false, required: false
      }
      section("Options") {
            input name: "logTrace", type: "bool", title: "Log Trace Messages", required: false
 			input name: "buttonDuplicateSeconds", type: "number", title: "Duplicate Button Seconds", description: "Ignore duplicate button push within seconds", range: "1..10", defaultValue: 5, required: true
      }
}

def installed() {
	log.debug "installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

	if(settings.buttonDuplicateSeconds == null) settings.buttonDuplicateSeconds = 4

	subscribe(buttonDeviceAway, "button", buttonEvent)
	subscribe(buttonDeviceStay, "button", buttonEvent)
	subscribe(buttonDeviceDisarm, "button", buttonEvent)
    
	subscribe(location, "alarmSystemStatus", alarmSystemStatusEvent)
	subscribe(location, "mode", modeEvent)
}

// ***** button *****

def buttonEvent(evt){
    if(logTrace) log.trace "buttonEvent: ${evt.name} = ${evt.value} (${evt.data})"
	
    def buttonDevice = evt.device
	def value = getButtonValue(buttonDevice.id)
    
    def recentEvents = buttonDevice.eventsSince(new Date(now() - (buttonDuplicateSeconds * 1000))).findAll{it.value == evt.value && it.data == evt.data}
    if(logTrace) log.trace "found ${recentEvents.size()?:0} events in past ${buttonDuplicateSeconds} seconds"

    if(recentEvents.size <= 1){
        def buttonNumber = (evt.data =~ /.*:(.+)}.*/)[ 0 ][ 1 ]
        buttonNumber = (buttonNumber) ? buttonNumber.replace('"','').toInteger() : 0
        executeButtonHandler(buttonNumber, value)
    } else {
        log.debug "found recent button ${evt.value}, ignoring event"
    }	
}

def executeButtonHandler(buttonNumber, value) {
	if(logTrace) log.trace "executeButtonHandler: ${buttonNumber} (${value})"
    
    executeAlarmSystemStatusHandler(value)
}

def getButtonValue(deviceId) {
    def value
    if(deviceId == buttonDeviceAway?.id) { value = "away" }
    else if(deviceId == buttonDeviceStay?.id) { value = "stay" }
    else if(deviceId == buttonDeviceDisarm?.id) { value = "disarm" }
    else { value = "unknown" }
    return value
}

// ***** alarmSystemStatusEvent *****

def alarmSystemStatusEvent(evt) {
    if(logTrace) log.trace "alarmSystemStatusEvent: ${evt.name} = ${evt.value} (${evt.data})"
    
    executeMode(evt.value)
}

def executeAlarmSystemStatusHandler(value) {
    value = fixValue(value,"alarm")	
	if(location.currentState("alarmSystemStatus")?.value != value) {
        if(["away","stay","off"].contains(value)) {
        
            if(logTrace) log.trace "executeAlarmSystemStatusHandler: ${value}"   
            sendLocationEvent(name: "alarmSystemStatus", value: value)
            
        } else {
            log.warn "tried to change to undefined alarmSystemStatus: ${value}"    
        }        
    }
}

// ***** mode *****

def modeEvent(evt) {
    if(logTrace) log.trace "modeEvent: ${evt.name} = ${evt.value} (${evt.data})"
    
    executeAlarmSystemStatusHandler(evt.value)    
}

def executeMode(value) {
    value = fixValue(value,"mode")	
    if (location.mode != value) {
        if (location.modes?.find{it.name == value}) {
        
   			if(logTrace) log.trace "executeMode: ${value}"
            setLocationMode(value)
            
        }  else {
            log.warn "tried to change to undefined mode: ${value}"
        }
    }
}

// ***** helpers *****

def fixValue(value,section) {
	if(section == "mode"){
        if(value == "off" || value == "disarm") { value = "home" }
        else if(value == "stay") { value = "night" }
        value = value?.capitalize()    
    }
	else if(section == "alarm"){
        value = value?.toLowerCase()
        if(value == "disarm" || value == "home") { value = "off" }
        else if(value == "night") { value = "stay" }  
    }
	return value
}



