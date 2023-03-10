/**
 *  Virtual Device Linker Child App
 *
 *  Copyright 2017 makutaku
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
    name: "Virtual Device Linker Child App",
    namespace: "makutaku",
    author: "makutaku",
    description: "Virtual Device Linker Child App",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


 preferences {
 
 	def physicalSensorTypes = [
                "contactSensor":"Open/Closed Sensor",
                "motionSensor":"Motion Sensor",
                "switch": "Switch",
                "presenceSensor": "Presence Sensor",
                "moistureSensor": "Moisture Sensor"]
 
  	def virtualSensorTypes = [
                "contactSensor":"Open/Closed Sensor",
                "motionSensor":"Motion Sensor",
                "switch": "Switch",
                "moistureSensor": "Moisture Sensor"]
                
    page(name: "sensorTypePage", title: "Select sensor types", nextPage: "deviceAndActionsPage", uninstall: true) {
        section {
            input(name: "physicalSensorType", type: "enum", title: "Which sensor type is the physical device?", required: true, multiple: false, options: physicalSensorTypes)

            input(name: "virtualSensorType", type: "enum", title: "Which sensor type is the virtual device?", required: true, multiple: false, options: virtualSensorTypes)
            
        }
    }

    page(name: "deviceAndActionsPage", title: "Select devices and actions", install: true, uninstall: true)
}

def deviceAndActionsPage() {
    dynamicPage(name: "deviceAndActionsPage") {
        section {
            input(name: "physicalSensors", type: "capability.${physicalSensorType}", title: "If the ${physicalSensorType} device", required: true, multiple: true, submitOnChange: true)
            input(name: "physicalSensorAction", type: "enum", title: "is", required: true, multiple: false, options: attributeValues(physicalSensorType))
        }
        section {
            input(name: "virtualSensors", type: "capability.${virtualSensorType}", title: "Perform on ${virtualSensorType}", required: true, multiple: true)
            input(name: "virtualSensorAction", type: "enum", title: "action", required: true, multiple: false, options: actionValues(virtualSensorType))
            input(name: "delay", type: "number", title: "Delay in seconds", required: false, multiple: false, defaultValue: 0)
         }
         section {
            input(name: "logicalOperation", type: "enum", title: "Logical Operation", required: false, multiple: false, defaultValue: "every",
            	options: ["every":"All", "any":"Any"])
            
         }
    }
}

// A method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
def defaultLabel() {
    def physicalSensorsText = (physicalSensors.size() > 1) ? 
    	"When ${logicalOperation} ${physicalSensors.displayName} are ${physicalSensorAction}" : 
        "When ${physicalSensors.displayName} is ${physicalSensorAction}"

	return "${physicalSensorsText}, perform ${virtualSensorAction} on ${virtualSensors.displayName}"
}

private attributeValues(attributeName) {

    def sensorEvents = [
        "switch": ["on","off"],
        "contactSensor": ["open","closed"],
        "motionSensor":["active","inactive"],
        "presenceSensor":["present","not present"],
        "moistureSensor":["wet","dry"]]
        
    return sensorEvents.containsKey(attributeName) ? sensorEvents[attributeName] : ["UNDEFINED"]
}

private actionValues(attributeName) {

    def sensorActions = [
        "switch": ["on","off"],
        "contactSensor": ["open","close"],
        "motionSensor":["active","inactive"],
        "presenceSensor":["arrived","left"],
        "moistureSensor":["wet","dry"]]
        
    return sensorActions.containsKey(attributeName) ? sensorActions[attributeName] : ["UNDEFINED"]
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
    
    if (physicalSensors)	{
    	//def eventType = (physicalSensorType - "Sensor") + "." + physicalSensorAction
        def eventType = (physicalSensorType - "Sensor")
    	log.debug "Subscribing to physical device event ${eventType} ..."
    	subscribe(physicalSensors, eventType, physicalSensorEventHandler)
	}

    if (virtualSensors)	{
    	def eventType = (virtualSensorType - "Sensor") + "." + virtualSensorAction
    	log.debug "Subscribing to virtual device event ${eventType} ..."
    	subscribe(virtualSensors, eventType, virtualSensorEventHandler)
	}
}

def	physicalSensorEventHandler(evt)	{
	log.debug "Handled event ${evt.name} with value ${evt.value} from physical device ${evt.displayName}"  
    
    log.debug "unscheduling action"
    unschedule(invokeAction)
    log.debug "unscheduled action"
    
    if (physicalSensors."${logicalOperation}"{sensor -> sensor.currentValue(evt.name) == physicalSensorAction}) {
    	log.debug "Scheduling action"
    	runIn(delay, invokeAction)
    }
}

def invokeAction() {
    log.debug "Invoking action ${virtualSensorAction} on virtual device ${virtualSensors.displayName}"
    virtualSensors."${virtualSensorAction}"()
    log.debug "Invoked action ${virtualSensorAction} on virtual device ${virtualSensors.displayName}"
}

def	virtualSensorEventHandler(evt)	{
	log.debug "Handled event ${evt.name} with value ${evt.value} from virtual device ${evt.displayName} "    
}


/*
// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            physicalDevicesInputs()
            actionInputs()
        }
        timeInputs()
    }
}

// page for allowing the user to give the automation a custom name
def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

// inputs to select the lights
def physicalDevicesInputs() {
    input "physicalSensors", "capability.motionSensor", title: "Which virtual device do you want to control?", multiple: true, submitOnChange: true
}

// inputs to control what to do with the lights (turn on, turn on and set color, turn on
// and set level)
def actionInputs() {
    if (lights) {
        input "action", "enum", title: "What do you want to do?", options: actionOptions(), required: true, submitOnChange: true
        if (action == "color") {
            input "color", "enum", title: "Color", required: true, multiple:false, options: [
                ["Soft White":"Soft White - Default"],
                ["White":"White - Concentrate"],
                ["Daylight":"Daylight - Energize"],
                ["Warm White":"Warm White - Relax"],
                "Red","Green","Blue","Yellow","Orange","Purple","Pink"]

        }
    }
}

// utility method to get a map of available actions for the selected switches
def actionMap() {
    def map = [on: "Turn On", off: "Turn Off"]
    if (lights.find{it.hasCommand('setLevel')} != null) {
        map.level = "Turn On & Set Level"
    }
    if (lights.find{it.hasCommand('setColor')} != null) {
        map.color = "Turn On & Set Color"
    }
    map
}

// utility method to collect the action map entries into maps for the input
def actionOptions() {
    actionMap().collect{[(it.key): it.value]}
}

// inputs for selecting on and off time
def timeInputs() {
    if (settings.action) {
        section {
            input "turnOnTime", "time", title: "Time to turn lights on", required: true
            input "turnOffTime", "time", title: "Time to turn lights off", required: true
        }
    }
}

// the handler method that turns the lights on and sets level and color if specified
def turnOnHandler() {
    // switch on the selected action
    switch(action) {
        case "level":
            lights.each {
                // check to ensure the switch does have the setLevel command
                if (it.hasCommand('setLevel')) {
                    log.debug("Not So Smart Lighting: $it.displayName setLevel($level)")
                    it.setLevel(level as Integer)
                }
                it.on()
            }
            break
        case "on":
            log.debug "on()"
            lights.on()
            break
        case "color":
            setColor()
            break
        }
}

// set the color and level as specified, if the user selected to set color.
def setColor() {

    def hueColor = 0
    def saturation = 100

    switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
        case "Daylight":
            hueColor = 53
            saturation = 91
            break;
        case "Soft White":
            hueColor = 23
            saturation = 56
            break;
        case "Warm White":
            hueColor = 20
            saturation = 80
            break;
        case "Blue":
            hueColor = 70
            break;
        case "Green":
            hueColor = 39
            break;
        case "Yellow":
            hueColor = 25
            break;
        case "Orange":
            hueColor = 10
            break;
        case "Purple":
            hueColor = 75
            break;
        case "Pink":
            hueColor = 83
            break;
        case "Red":
            hueColor = 100
            break;
    }

    def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]
    log.debug "color = $value"

    lights.each {
        if (it.hasCommand('setColor')) {
            log.debug "$it.displayName, setColor($value)"
            it.setColor(value)
        } else if (it.hasCommand('setLevel')) {
            log.debug "$it.displayName, setLevel($value)"
            it.setLevel(level as Integer ?: 100)
        } else {
            log.debug "$it.displayName, on()"
            it.on()
        }
    }
}

*/