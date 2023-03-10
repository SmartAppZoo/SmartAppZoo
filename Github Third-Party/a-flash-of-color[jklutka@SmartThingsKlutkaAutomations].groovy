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
 *  A Flash of Color
 *
 *	Modified By: Justin Klutka
 *	Date: 2016-06-05
 *
 */
definition(
    name: "A Flash of Color",
    namespace: "klutka",
    author: "Justin Klutka",
    description: "Flashes a set of lights in response to motion, an open/close event, or a switch.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather3-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather3-icn@2x.png"
)

preferences {
	section("When any of the following devices trigger..."){
		input "motion", "capability.motionSensor", title: "Motion Sensor?", required: false
		input "contact", "capability.contactSensor", title: "Contact Sensor?", required: false
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Sensor?", required: false
		input "mySwitch", "capability.switch", title: "Switch?", required: false
		input "myPresence", "capability.presenceSensor", title: "Presence Sensor?", required: false
	}
	section("Then flash..."){
		input "switches", "capability.switch", title: "These lights", multiple: true
        input "lightColor", "enum", title: "This color (default is no change)", required: false, options:
          [["Red": "Red"], ["Orange": "Orange"], ["Yellow": "Yellow"], ["Green": "Green"], ["Blue": "Blue"], ["Purple": "Purple"]]
       	input "lightLevel", "number", title: "This light level (default is 100%)", required: false, range: "0..100"
	}
	section("How many seconds would you like the color to flash? (optional)..."){
		input "onFor", "number", title: "On for (default is indefinite)", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	subscribe()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe()
}

def subscribe() {
	if (contact) {
		subscribe(contact, "contact.open", colorFlashHandler)
	}
	if (acceleration) {
		subscribe(acceleration, "acceleration.active", colorFlashHandler)
	}
	if (motion) {
		subscribe(motion, "motion.active", colorFlashHandler)
	}
	if (mySwitch) {
		subscribe(mySwitch, "switch.on", colorFlashHandler)
	}
	if (myPresence) {
		subscribe(myPresence, "presence", presenceHandler)
	}
}

def colorFlashHandler(evt) {
	log.trace "Flash Handler $evt.value"
	colorFlashLights()    
    log.debug "Setting up delay statement for ${onFor}."
    //runIn(onFor,restoreDefaultColor)
}

def presenceHandler(evt) {
	log.debug "presence $evt.value"
	if (evt.value == "present") {
		colorFlashHandler(evt)
	} 
    /* FUTURE: If future versions want to handle arrival and departures
    else if (evt.value == "not present") {
		colorFlashLights()
	}
    */
}

def restoreDefaultColor() {
    
    log.debug "Restoring Default Colors"
    
    switches.each {    	         	        
        it.setColor(null)        
        it.setColorTemperature(2703)        
    }
}

private getBulbColor(colorValue)
{
		def newColorValue 
        def lightLevel = lightLevel ?: 100
                
        switch (colorValue) {
        	case "Red":
            	return newColorValue = [hue: 100, saturation: 100, level: lightLevel as Integer]
            case "Orange":
            	return newColorValue = [hue: 10, saturation: 100, level: lightLevel as Integer]
            case "Yellow":
            	return newColorValue = [hue: 60, saturation: 100, level: lightLevel as Integer]
            case "Green":
            	return newColorValue = [hue: 39, saturation: 100, level: lightLevel as Integer]
            case "Blue":
            	return newColorValue = [hue: 70, saturation: 100, level: lightLevel as Integer]
            case "Purple":
            	return newColorValue = [hue: 75, saturation: 100, level: lightLevel as Integer]
            default:
            	log.debug "getBulbColor: Took the default branch of the switch statement parameter was: ${colorValue}"
            	return null
        }        		
}

private colorFlashLights() {
	     	    
    log.debug "Processing Color Change: ${lightColor}"

    //switches.each { 
    //    it.setColor(getBulbColor(lightColor))
    //}
    
    restoreDefaultColor()
         
    //NOTE: Attempting to pass the instruction twice to catch lights that inconsistently turn
    //TODO: Update to self check light hue values and address just those
    //switches.each { 
   //     it.setColor(getBulbColor(lightColor))              
    //}        
    
    //if ( onFor > 0 ) {
    //	runIn(onFor, restoreDefaultColor)
    //}
}

