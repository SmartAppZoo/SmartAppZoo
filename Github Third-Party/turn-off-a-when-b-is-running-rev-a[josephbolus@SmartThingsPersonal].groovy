/**
 *  Turn off A when B is running Rev A
 *
 *  Copyright 2015 Keith Croshaw
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
    name: "Turn off A when B is running Rev A",
    namespace: "keithcroshaw",
    author: "Keith Croshaw",
    description: "Turn off the washing machine when the CatGenie is running.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

  	section ("Disable?") {
    	input "disableLogic", "bool", title: "Disable Logic?"
  	}
    section ("When this device is drawing power") {
    	input "meter", "capability.powerMeter", multiple: false, required: true
        input "DeviceRunning", "number", title: "Device running when power is above (W)", description: "8", required: true
    }
    section("Turn off these switches..."){
		input "switches", "capability.switch", multiple: true
	}

}

def installed() {

	initialize()
}

def updated() {
	
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(meter, "power", eventHandler)
}

def eventHandler(evt) {

	if (!disableLogic) {   
    
    	def latestPower = meter.currentValue("power")
    	//log.debug "Power: ${latestPower}W"
    
    	if (latestPower >= DeviceRunning){
    		state.deviceInStandby = 0
    	} else {
    		state.deviceInStandby = 1
    	}
    	//log.debug "state.deviceInStandby: ${state.deviceInStandby}"
            
    if (state.deviceInStandby==0){
    	switches?.off()
        log.debug "ST Turned off the ${switches}"
        
    } else {
    	
        switches?.on()
        log.debug "ST Turned on the ${switches}"
      
    }
  }
}