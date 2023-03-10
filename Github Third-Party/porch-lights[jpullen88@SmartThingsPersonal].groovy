/**
 *  Porch Lights
 *
 *  Copyright 2017 Jason Pullen
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
    name: "Porch Lights",
    namespace: "jpullen88",
    author: "Jason Pullen",
    description: "Setup porch lights so they come on automatically but will only auto turn off under certain circumstances.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Lighting"){
    	input "objLight", "capability.switch", title:"Select the lights you want controlled.", required:true, multiple:true
    	input "objMinutes", "number", title:"How long, in minutes, to keep the lights on.", required:true
    }
    section("User Presence"){
    	input "objUser", "capability.presenceSensor", title:"Select the presence sensors or phones that will trigger the event.", required:true, multiple:true
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
    subscribe(objUser, "presence", handlerCheckPresence)
}

//Handlers

def handlerCheckPresence(evt) {
	def sunriseTime = getSunriseAndSunset().sunrise.format("yyyy-MM-dd HH:mm:ss", location.timeZone)    	
    def sunsetTime = getSunriseAndSunset().sunset.format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def currentTime = new Date(((long)now())).format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    
    if (currentTime < sunriseTime && currentTime < sunsetTime) {
    	log.debug "0001 to sunrise  // Night"
        if (evt.value == "present") {
        	objLight.on()
            runIn(objMinutes*60, handlerLightsOff)
        }
    } else if (currentTime > sunriseTime && currentTime < sunsetTime) {    		
    	log.debug "sunrise to sunset // Day"
        objLight.off()
    } else if (currentTime > sunriseTime && currentTime > sunsetTime) {    		
    	log.debug "sunset to 2359 // Night"
        if (evt.value == "present") {
        	log.debug "turning on..."
            objLight.on()
            runIn(objMinutes*60, handlerLightsOff)
        }
    }
}
def handlerLightsOff() {
	objLight.off()
}