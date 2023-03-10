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
 *  Author: Lcosta luisfcosta@live.com
 */
 
definition(
    name: "Hot Tub Control SmartApp",
    namespace: "luisfcosta",
    author: "luisfcosta@live.com",
    description: "Syncs 3 individual sensor/actuators with one individual device: Spa heater, Spa Jets (switches) and Spa temperature sensor.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/luisfocosta/smartthings/master/Hot%20tub/icons/Hot-Tub-Heating-On.png",
    iconX2Url: "https://raw.githubusercontent.com/luisfocosta/smartthings/master/Hot%20tub/icons/Hot-Tub-Heating-On.png"
)

preferences {
	section("Choose the switch/relay that controls the Hot Tub Jets"){
		input "jetsSwitchDevice", "capability.switch", title: "Hot Tub Jets switch?", required: true
	}
	section("Choose the switch/relay that controls the Hot Tub heater"){
		input "heaterSwitchDevice", "capability.switch", title: "Hot Tub Heater Switch?", required: true
	}
	section("Choose the temperature sensor"){
		input "tempSensorDevice", "capability.temperatureMeasurement", title: "Hot Tub temperature Sensor?", required: true
	}
	section("Choose the Virtual Hot Tub Device? "){
		input "virtualSpaDevice", "capability.thermostat", title: "Hot Tub Virtual device?", required: true
	}
}

def installed()
{
	subscribe(virtualSpaDevice, "jets", virtualJetsHandler)
    subscribe(virtualSpaDevice, "heater", virtualHeaterHandler)
    subscribe(virtualSpaDevice, "switch", virtualSwitchHandler)
	subscribe(tempSensorDevice, "temperature", tempHandler)
	subscribe(jetsSwitchDevice, "switch", JetsHandler)
	subscribe(heaterSwitchDevice, "switch", HeaterHandler)

	def rTemp = tempSensorDevice.currentValue("temperature")
	def vTemp = virtualSpaDevice.currentValue("temperature")
	def rHeater = heaterSwitchDevice.currentValue("switch")
	def vHeater = virtualSpaDevice.currentValue("heater")
    def rJets = jetsSwitchDevice.currentValue("switch")
	def vJets = virtualSpaDevice.currentValue("jets")
    def vswitch = virtualSpaDevice.currentValue("switch")
    
    if (vTemp == null) {
    	vTemp = 0
    }
    
    if (vHeater == null) {
    	vHeater = "NA"
    }
    
    if (vJets == null) {
    	vJets = "NA"
    }
    
    log.debug "installed - rTemp: $rTemp"
    log.debug "installed - vTemp: $vTemp"
    log.debug "installed - rHeater: $rHeater"
    log.debug "installed - vHeater: $vHeater"
    log.debug "installed - rJets: $rJets"
    log.debug "installed - vJets: $vJets"

	// sync them up: virtual same as real
	if (rTemp != vTemp){
    	log.debug "setting Spa device temp to $rTemp"
		virtualSpaDevice.setTemperature(rTemp)
	}

	if (rHeater != vHeater){
		if (rHeater == "on") {
        	log.debug "turning on virtual Spa heater"
			virtualSpaDevice.heaterOn()
	    } else {
        	log.debug "turning off virtual Spa heater"
    		virtualSpaDevice.heaterOff()
	    }
	}

	if (rJets != vJets){
		if (rJets == "on") {
			virtualSpaDevice.JetsOn()
            log.debug "turning on virtual Spa jets"
	    } else {
    		virtualSpaDevice.JetsOff()
            log.debug "turning off virtual Spa jets"
    	}
	}
}

def updated() {
	subscribe(virtualSpaDevice, "jets", virtualJetsHandler)
    subscribe(virtualSpaDevice, "heater", virtualHeaterHandler)
    subscribe(virtualSpaDevice, "switch", virtualSwitchHandler)

	subscribe(tempSensorDevice, "temperature", tempHandler)
	subscribe(jetsSwitchDevice, "switch", JetsHandler)
	subscribe(heaterSwitchDevice, "switch", HeaterHandler)

	def rTemp = tempSensorDevice.currentValue("temperature")
	def vTemp = virtualSpaDevice.currentValue("temperature")
	def rHeater = heaterSwitchDevice.currentValue("switch")
	def vHeater = virtualSpaDevice.currentValue("heater")
    def rJets = jetsSwitchDevice.currentValue("switch")
	def vJets = virtualSpaDevice.currentValue("jets")
    def vswitch = virtualSpaDevice.currentValue("switch")
 
     if (vTemp == null) {
    	vTemp = 0
    }
    
    if (vHeater == null) {
    	vHeater = "NA"
    }
    
    if (vJets == null) {
    	vJets = "NA"
    }
    
    log.debug "updated - rTemp: $rTemp"
    log.debug "updated - vTemp: $vTemp"
    log.debug "updated - rHeater: $rHeater"
    log.debug "updated - vHeater: $vHeater"
    log.debug "updated - rJets: $rJets"
    log.debug "updated - vJets: $vJets"

	// sync them up if need be set virtual same as actual
	if (rTemp != vTemp){
		virtualSpaDevice.setTemperature(realTemp)
	}

	if (rHeater != vHeater){
		if (rHeater == "on") {
			virtualSpaDevice.heaterOn()
            log.debug "updated - turning on virtual Spa heater"
	    } else {
    		virtualSpaDevice.heaterOff()
            log.debug "updated - turning off virtual Spa heater"
	    }
	}

	if (rJets != vJets){
		if (rJets == "on") {
            log.debug "updated - turning on virtual Spa jets"
            virtualSpaDevice.JetsOn()
	    } else {
            log.debug "updated - turning off virtual Spa jets"
			virtualSpaDevice.JetsOff()
    	}
	}
}

def virtualJetsHandler(evt) {
    //Responds to events coming from the jets switch on the virtual device: update real jets switch
    def rJets = jetsSwitchDevice.currentValue("switch")
    log.debug "Received event on the virtual jets device: $evt.value. Real jets are $rJets."
   	if("on" == evt.value){
    	if (rJets != "on"){
        	log.debug "turning on real Spa jets"
        	jetsSwitchDevice.on()
      	}
    } else if("off" == evt.value){
    	if (rJets != "off"){
        	log.debug "turning off real Spa jets"
       		jetsSwitchDevice.off()
      	}
   	}
}

def JetsHandler(evt) {
    //Responds to events coming from the jets switch on the physical (real) device: update virtual Spa device
    def vJets = virtualSpaDevice.currentValue("jets")
    log.debug "Received event on the real jets device: $evt.value. Virtual jets are $vJets."
   	if("on" == evt.value){
    	if (vJets != "on"){
        	log.debug "turning on virtual Spa jets"
        	virtualSpaDevice.JetsOn()
      	}
    } else if("off" == evt.value){
    	if (vJets != "off"){
        	log.debug "turning off virtual Spa jets"
       		virtualSpaDevice.JetsOff()
      	}
   	}
}

def virtualHeaterHandler(evt) {
    //Responds to events coming from the heater switch on the virtual device: update real heater switch accordingly
    def rJets = heaterSwitchDevice.currentValue("switch")
    log.debug "Received event on the virtual heater device: $evt.value. Real heater is $rJets."
   	if("on" == evt.value){
    	if (rJets != "on"){
        	log.debug "turning on real Spa heater"
        	heaterSwitchDevice.on()
      	}
    } else if("off" == evt.value){
    	if (rJets != "off"){
        	log.debug "turning off real Spa heater"
       		heaterSwitchDevice.off()
      	}
   	}
}

def HeaterHandler(evt) {
    //Responds to events coming from the heater switch on the physical (real) device: update virtual Spa device accordingly
    def vHeater = virtualSpaDevice.currentValue("heater")
    log.debug "Received event on the real heater device: $evt.value. Virtual heater is $vJets."
   	if("on" == evt.value){
    	if (vHeater != "on"){
        	log.debug "turning on virtual Spa heater"
        	virtualSpaDevice.heaterOn()
      	}
    } else if("off" == evt.value){
    	if (vHeater != "off"){
        	log.debug "turning off virtual Spa heater"
       		virtualSpaDevice.heaterOff()
      	}
   	}
}

def tempHandler(evt) {
    //Responds to events coming from the temp sensor (real) device: update virtual Spa device accordingly
    def rTemp = virtualSpaDevice.currentState("temperature")?.integerValue
    log.debug "Received event on the real temp sensor: $evt.value. Virtual temp is $rTemp."
   	if(rTemp != evt.value){
    	log.debug "setting virtual Spa temperature to $rTemp"
       	virtualSpaDevice.setTemperature(evt.value)
   	}
}