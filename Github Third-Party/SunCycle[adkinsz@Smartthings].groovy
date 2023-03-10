/**
 *  SunCycle
 *
 *  Copyright 2016 Zachary
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
    name: "SunCycle",
    namespace: "adkinsz",
    author: "Zachary",
    description: "Control RGBW lights to roughly map to the color of natural light.",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Color Changing Lights (RGBW)") {
	}
    section("Select Bulbs to control color of and temperature (RGBW)") {
    	input "bulbs", "capability.colorControl", title: "Which Color Changing Bulbs?", multiple:true, required: false
	}
    section("Switch to control application.") {
    	input "appSwitch", "capability.switch", title: "(optional)"
    }
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(bulbs, "switch.on", onPowered)
    subscribe(appSwitch, "switch.on", onActivate)
}

// TODO: implement event handlers
def onPowered(evt) {
	log.debug "Monitored Switch Powered"
    updateBulbs()
}

def onActivate(evt) {
	log.debug "Control Switch Activated: Resuming Color Control."
    updateBulbs()
}

def updateBulbs(){
	def onSwitches = bulbs.findAll { switchVal -> switchVal.currentValue("switch") == "on"}
    log.info "Active Switches: ${onSwitches.size()}"
    log.info "Active: ${onSwitches}"
    
    if(appSwitch) {
    	if(appSwitch.currentValue("switch") != "on"){
        	log.debug "Control Switch in Off Position. Leaving bulb states alone."
            return
        }
    }
    
    if(onSwitches){
    	SetColor(onSwitches)    
        //if active bulbs are found rerun in the future to keep light colors in check.
        log.info "Scheduling Color Check in 5 Minutes"
        runEvery5Minutes(updateBulbs)
    }
}

def SetColor(devices){
	def cal = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"))
    def time = cal.get(Calendar.HOUR_OF_DAY)
    log.info "Current Time: ${cal}"
	if(5 <= time && time < 7){
    	log.info "Setting Mode: Early Morning"
        devices.each{
        	it.setHue(30/360)
            it.setSaturation(100)
            it.setLevel(85)
        }
    }
    else if(7 <= time && time < 11){
    	log.info "Setting Mode: Morning"
  		devices.each{it.setColorTemperature(3200)}
    }
    else if(11 <= time && time < 15){
    	log.info "Setting Mode: High Noon"
    	devices.each{it.setColorTemperature(6000)}
    }
    else if(15 <= time && time < 17){
    	log.info "Setting Mode: Afternoon"
        devices.each{it.setColorTemperature(3000)}
    }
    else if(17 <= time && time < 20){
    	log.info "Setting Mode: Evening"
        devices.each{
        	it.setHue(34/360)
            it.setSaturation(74)
            it.setLevel(65)
        }
    }
    else if(20 <= time || time < 5){
    	log.info "Setting Mode: Night"
    	devices.each{
        	it.setHue(353/360)
            it.setSaturation(90)
            it.setLevel(30)
        }
    }
    else 
    	log.error "Unrecognized Time: ${time}"
}
