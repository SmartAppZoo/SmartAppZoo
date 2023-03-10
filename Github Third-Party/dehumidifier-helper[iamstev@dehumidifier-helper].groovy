/**
 *  Fan Control
 *
 *  Copyright 2016 Steven Smith
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
    name: "Dehumidifier Helper",
    namespace: "iamstev",
    author: "Steven Smith",
    description: "Run fans to help cirulate air when a dehumidifer is running. Also runs the fans for 15 minutes every 3 hours.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/a.stev.link/smartthings/wind.png",
    iconX2Url: "https://s3.amazonaws.com/a.stev.link/smartthings/wind@2x.png",
    iconX3Url: "https://s3.amazonaws.com/a.stev.link/smartthings/wind@2x.png")



preferences {
	page(name: "pageSettings", title: "Settings", install: true, uninstall: true){
    	section("Devices"){
       		input(name:"dehumid", type: "capability.powerMeter", title: "Dehumidifier", multiple: false, required: true)
            input(name:"fans", type: "capability.switch", title: "Fans", multiple: true, required: true)
            input(name: "threshold", type: "number", title: "Wattage", required: true)
        }
        section("Help"){
        	paragraph "Run a fan while the dehumidifier is running. Also runs the fans for 15 minutes every 3 hours."

        }
        section(){
        	label(title: "Name this app", defaultValue: "Dehumidifier Helper", required: true)
            mode(title: "Set for specific mode(s)", required: false)
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(dehumid, "power", meter_handler)
    state.interval_fan = false
	runEvery3Hours(do_fans_on)
}

def do_fans_on(){
	log.debug "Interval fan turned on."
    fans.on()
    state.interval_fan = false
    runIn(900, do_fans_off)
}

def do_fans_off(){
	log.debug "Interval fan turned off."
	fans.off()
    state.interval_fan = false
}



def meter_handler(evt) {
	if(state.interval_fan){
    	log.debug "Dehumidifier fan event skipped becuase interval fan already running."
    }else{
        def meterValue = evt.value as double
        def thresholdValue = threshold as int
        if(meterValue > thresholdValue){
        	log.debug "Dehumidifier fan turned on."
            fans.on()
        }else{
        	log.debug "Dehumidifier fan turned off."
            fans.off()
        }
	}
}