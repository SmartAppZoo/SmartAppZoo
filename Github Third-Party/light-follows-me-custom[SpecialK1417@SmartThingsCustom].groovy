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
 *  Light Follows Me Plus Sunrise
 *  this was adopted from Light Follows Me from SmartThings
 *  I added two new features. First, the option to ignore when not dark based on sunset and sunrise
 *  There is also an option to specify any number of lights to override the motion
 *    this is used when there is a light nearby that when on you don't want the motion to activate
 *    I needed this because sometimes I manually turn on one light and then I don't want the
 *    light that is on the auto sensor to also come on. This could bue used to disable
 *    all your lights on a motion sensor by tying them to a light in another room too such as a closet
 *    finally I added an optional boolean to use or not use the disable upon no motion option
 *
 *  Author: Ken Washington
 */

definition(
    name: "Light Follows Me Custom",
    namespace: "SpecialK1417",
    author: "Ken Washington",
    description: "Turn your lights on when motion is detected with various new options.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Turn on when there's movement..."){
		input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("Turn on/off these light(s)..."){
		input "switches", "capability.switch", multiple: true
	}
	section("Auto off options..."){
        input "nomotion", "boolean", title: "Turn off when motion stops"
		input "minutes1", "number", title: "Wait this many minutes (1 - 60)?", required: false
	}
    section("More options...") {
        input "usesunset", "boolean", title: "Active only after sunset and before sunrise?"
		input "switches2", "capability.switch", title: "Ignore motion if these are on", multiple: true, required: false
    }
}

def installed() {
    state.isdark = true
    initialize()
}

def updated() {
	unsubscribe()
    initialize()
}

def initialize() {
	subscribe(motion1, "motion", motionHandler)
    state.isdark = true
    if (usesunset) {
        def srss = getSunriseAndSunset()
        if ( now() < srss.sunset.getTime() ) {
            state.isdark = false
        }
    	subscribe(location, "sunrise", sunriseHandler)
	    subscribe(location, "sunset", sunsetHandler)
        log.debug "Sunset / Sunrise monitoring enabled. Currently isdark = $state.isdark"
    }
    
    // fix minutes1 if something crazy specified
    if (nomotion && minutes1 > 60) {
    	minutes1 = 60
    }
}

def motionHandler(evt) {
	log.debug "$evt.name: $evt.value"

    def ignoremotion = false

    // check if the override light is on - if so, log info and do nothing
    def overrideswitches = switches2.currentSwitch
    if (overrideswitches) {
        def onsw = overrideswitches.findAll {
            switchVal -> switchVal == "on" ? true : false
        }
        if (onsw.size() == 1) {
            log.debug "A switch is on that you specified to override so motion ignored"
            ignoremotion = true
        }
        else if (onsw.size() > 0) {
            log.debug "${onsw.size()} switches are on that you specified to override so motion ignored"
            ignoremotion = true
        }
    }

    // check if sunrise override
    if ( usesunset && isdark==false) {
        ignoremotion = true
        log.debug "sunrise has occured and sunset has not happened yet so motion ignored"
    }

	// handle case when it is dark or the option isnt used
    if ( !ignoremotion ) {
    	if (evt.value == "active") {
    		log.debug "turning on lights due to motion"
    		switches.on()
    	} else if (evt.value == "inactive") {

            // check settings
            if (nomotion && minutes1==0) {
	            log.debug "turning off lights due to no motion immediately"
            	switches.off()
            }
            else if (nomotion && minutes1) {
	            log.debug "turning off lights due to no motion in $minutes1 minutes"
        		runIn(minutes1 * 60, scheduleCheck, [overwrite: false])
            } else {
            	log.debug "motion stopped but lights not turned off because feature not activated"
            }
    	}
    }
}

def scheduleCheck() {
	log.debug "schedule check"
	def motionState = motion1.currentState("motion")
    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.rawDateCreated.time
    	def threshold = 1000 * 60 * minutes1 - 1000
    	if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning lights off"
            switches.off()
    	} else {
        	log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
    	log.debug "Motion is active, do nothing and wait for inactive"
    }
}


def sunriseHandler(evt) {
    state.isdark = false
	log.debug "Sunrise happened"
}

def sunsetHandler(evt) {
    state.isdark = true
	log.debug "Sunset happened"
}