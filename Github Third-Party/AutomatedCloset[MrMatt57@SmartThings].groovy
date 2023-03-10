/**
 *  Closet Door
 *
 *  Copyright 2015 Matthew Walker
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
    name: "Automated Closet",
    namespace: "",
    author: "Matthew Walker",
    description: "Automated Closet",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section("When these sensor are activated...") {
    input name: "contactSensor", type: "capability.contactSensor", title: "Contact Sensor", multiple: true
    input name: "knockSensor", type: "capability.accelerationSensor", title: "Movement Sensor", required: false, multiple: true
  }
  
  section("Turn this on...") {
    input "switchDevice", "capability.switch", title: "Switch?", required: false, multiple: true
    input name: "turnOff", type: "bool", title: "Turn off when Closed?"
    input "allowance", "number", title: "Leave on for Minutes? (0 Forever)"
  }
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
    if(knockSensor) {
      subscribe(knockSensor, "acceleration.active", doorKnockHandler)
    }
	subscribe(contactSensor, "contact", sensorHandler)
}

def sensorHandler(evt){
  log.debug "$evt.value"
  if(evt.value == "open") {
    turnOnSwitch()
  }
  else if (evt.value == "closed" && turnOff) {
    turnOffSwitch()
  }
}

def doorKnockHandler(evt) {
  if(contactSensor.latestValue("contact") == "closed"){
    return // door is closed
  }
  turnOnSwitch()
}

def turnOnSwitch() {
	switchDevice?.on()
	if(allowance && allowance > 0) {
    	unschedule() // reset timers
		def delay = allowance * 60
		log.debug "Turning off in ${allowance} minutes (${delay}seconds)"
		runIn(delay, turnOffSwitch)
    }
}

def turnOffSwitch() {
	switchDevice?.off()
    unschedule() // reset timers
}
