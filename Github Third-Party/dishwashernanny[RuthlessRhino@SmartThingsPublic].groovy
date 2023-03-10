/**
 *  dishWasherGranny
 *
 *  Copyright 2015 mmaxwell
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
    name: "dishWasherNanny",
    namespace: "MikeMaxwell",
    author: "mmaxwell",
    description: "Just another first world problem solved.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
        
    section("Dishwasher temperature sensor"){
		input "dishTemp", "capability.temperatureMeasurement",title: "Which?", multiple: false, required: true
	}
    //temp units dynamic page???
	section("Running temperature threshold F"){
		input "runningThreshold", "enum", title: "Degrees" , options:["90","100","105","110","115","120"]
	}
    section("Complete temperature threshold F (must be less than running..."){
		input "doneThreshold", "enum", title: "Degrees" , options:["80","90","100","105","110"]
	}
	section("Dishwasher door contact"){
		input "dishContact", "capability.contactSensor",title: "Which?", multiple: false, required: true
	}
    section("Indicator switch panel"){
    	input "indicator", "capability.switch", title: "Which?", multiple: false, required: true
    }
	
}

def installed() {
	//log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	state.map = [running: false, done: false, manReset: true, tempReset: true]
    
    subscribe(dishTemp,"temperature",tempHandler)
    subscribe(dishContact,"contact",contactHandler)
    subscribe(indicator, "switch", indicatorHandler)
}
def tempHandler(evt){
	def m = state.map
	log.info "temp:${evt.integerValue} map:${m}"
    if (evt.integerValue >= runningThreshold.toInteger()) {
    	//cantidate for cleaned state change
        if (!m.done && m.manReset && m.tempReset) {
        	//yup, washer is running...
            //?? blink
            m.running = true
        }
    } else if (evt.integerValue < doneThreshold.toInteger()) {
    	//if running, then done
        if (m.running) {
        	m.done = true
            m.tempReset = true
            m.running = false
            //reset blinker
            indicator.flash("blink")
            indicator.on()
        }
    	//cantidate for tempReset state change
    } else {
    	//
    }
    if (m.running){
      	indicator.flash("5minute")
    }
    
}
def contactHandler(evt){
	def m = state.map
	log.info "contact:${evt.value} map:${m}"
	if (indicator.currentValue("switch") == "on"  && m.done) {
    	indicator.strobe()
    }
}
def indicatorHandler(evt){
	def m = state.map
	log.info "indicator:${evt.value} map:${m}"
    if (evt.value == "off") {
    	m.manReset = true
        m.done = false
        m.tempReset = true
        m.running = false
    }
    
    
	
}