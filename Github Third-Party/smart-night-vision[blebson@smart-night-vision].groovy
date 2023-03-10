/**
 *  Smart Night Vision
 *  Version 1.1.3
 *  Copyright 2016 BLebson
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
    name: "Smart Night Vision",
    namespace: "blebson",
    author: "Ben Lebson",
    description: "Ties camera night-vision to a switch or dimmer for more exact control.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png"
)

preferences {
	section("Choose one or more, when..."){		
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "dimmingLevel", "number", title: "Minimum Dimming Level, from 0-100 (only for dimmers)", range: "000..100", required: false		
	}
	section("Choose camera to use") {
		input "camera", "capability.imageCapture", description: "NOTE: Currently only compatable with D-Link Devices made by BLebson"		
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {	
	subscribe(mySwitch, "switch.on", onStateChanged)	
    subscribe(mySwitch, "switch.off", offStateChanged)
    subscribe(mySwitch, "level", dimStateChanged)
	subscribe(mySwitch, "switch", dimStateChanged)
}

def dimStateChanged(evt) {
	
	if (evt.value == "on")
	{
    	if(dimmingLevel == null)
        {
        	camera.nvOff()
        }
        else {        
		if (mySwitch.level >= dimmingLevel)
		{
			camera.nvOff()
		}
		else if (mySwitch.level < dimmingLevel)
		{
			camera.nvOn()
		}
    	}
	}
	else if (evt.value == "off" )
	{
    	camera.nvOn()        
	}
	else{
    	log.debug("Dimming Event. ${evt.value}")
		def level = evt.value                
		level = level.toInteger()
		if (level >= dimmingLevel)
		{
			camera.nvOff()
		}
		else if (level < dimmingLevel)
		{
			camera.nvOn()
		}
	}
}

def onStateChanged(evt) {

log.debug("State Changed to Off." )
camera.nvOff()

}

def offStateChanged(evt) {

log.debug("State Changed to On." )
camera.nvOn()

}
