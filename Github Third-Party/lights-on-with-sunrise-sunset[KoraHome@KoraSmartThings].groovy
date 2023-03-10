/**
 *  Lights on with Sunrise / Sunset
 *
 *  Copyright 2016 Kora Home
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
    name: "Lights on with Sunrise / Sunset",
    namespace: "KoraHome",
    author: "Adam Aiello",
    description: "Device control based on sunrise / sunset & presence.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@3x.png")


preferences {
	section("When someone isn't home after sunset...") {
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Turn on a light..."){
		input "switch1", "capability.switch", multiple: true
	}
	section("When returning home, turn light off after...") {
		input "minutes1", "number", title: "Minutes", required:true
	}
}

def installed() {
	subscribe(presence1, "presence", presenceHandler)
	subscribe(location, "sunset", presenceHandler)
    subscribe(location, "sunrise", presenceHandler)
}

def updated() {
	unsubscribe()
	subscribe(presence1, "presence", presenceHandler)
	subscribe(location, "sunset", presenceHandler)
    subscribe(location, "sunrise", presenceHandler)
}

def presenceHandler(evt){
	
	def now = new Date()
	def sunTime = getSunriseAndSunset()
	def sunsOut = null
	def current = presence1.currentValue("presence")

	if ((now > sunTime.sunrise) && (now < sunTime.sunset)){
		sunsOut = 1
	}
    	else {
    		sunsOut = 0 
    	}
    	
	def presenceValue = presence1.find{it.currentPresence == "not present"}

        
	if(presenceValue && (sunsOut == 0)) {
		switch1.on()
		log.debug "It's night time. Someone isn't home. Turning on lights."
	}
	else if(presenceValue && (sunsOut == 1)) {
    	log.debug "It's day time. Someone isn't home. Turning lights off."
        switch1.off()
	}
	else {
		runIn(60*minutes1, offHandler)
		log.debug "Just turn it off."
	}
}

def offHandler (){
	switch1.off()
}
