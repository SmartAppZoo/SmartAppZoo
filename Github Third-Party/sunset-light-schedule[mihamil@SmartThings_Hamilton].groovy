/**
 *  Sunset Light Schedule
 *
 *  Copyright 2017 Michael Hamilton
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
    name: "Sunset Light Schedule",
    namespace: "mihamil",
    author: "Michael Hamilton",
    description: "Turns lights on and off at a specific time if you are not home.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

    //section ("Where are you?"){
    //	input "zipcode", "string", title: "Zip Code", required: true
    //}
    section("Turn on these lights"){
    	input "lights", "capability.switch", title: "Lights", multiple: true, required: true
    }
    section("only when these devices are not present") {
    	input "presenceDevices", "capability.presenceSensor", title: "Presence Devices", multiple: true, required: false
        input "falseAlarmThreshold", "number", title: "Delay (1 minute minimum)", required: true
    }
    section("between these times"){
    	input "fromTime", "enum", title: "From", required: true, options: [ "00:00:00.000": "12:00 AM", "00:30:00.000": "12:30 AM", 
                                                                             "01:00:00.000": "1:00 AM" , "01:30:00.000": "1:30 AM" , 
                                                                             "02:00:00.000": "2:00 AM" , "02:30:00.000": "2:30 AM" , 
                                                                             "03:00:00.000": "3:00 AM" , "03:30:00.000": "3:30 AM" , 
                                                                             "04:00:00.000": "4:00 AM" , "04:30:00.000": "4:30 AM" , 
                                                                             "05:00:00.000": "5:00 AM" , "05:30:00.000": "5:30 AM" , 
                                                                             "06:00:00.000": "6:00 AM" , "06:30:00.000": "6:30 AM" , 
                                                                             "07:00:00.000": "7:00 AM" , "07:30:00.000": "7:30 AM" , 
                                                                             "08:00:00.000": "8:00 AM" , "08:30:00.000": "8:30 AM" , 
                                                                             "09:00:00.000": "9:00 AM" , "09:30:00.000": "9:30 AM" , 
                                                                             "10:00:00.000": "10:00 AM", "10:30:00.000": "10:30 AM", 
                                                                             "11:00:00.000": "11:00 AM", "11:30:00.000": "11:30 AM",
                                                                             "12:00:00.000": "12:00 PM", "12:30:00.000": "12:30 PM", 
                                                                             "13:00:00.000": "1:00 PM" , "13:30:00.000": "1:30 PM" , 
                                                                             "14:00:00.000": "2:00 PM" , "14:30:00.000": "2:30 PM" , 
                                                                             "15:00:00.000": "3:00 PM" , "15:30:00.000": "3:30 PM" , 
                                                                             "16:00:00.000": "4:00 PM" , "16:30:00.000": "4:30 PM" , 
                                                                             "17:00:00.000": "5:00 PM" , "17:30:00.000": "5:30 PM" , 
                                                                             "18:00:00.000": "6:00 PM" , "18:30:00.000": "6:30 PM" , 
                                                                             "19:00:00.000": "7:00 PM" , "19:30:00.000": "7:30 PM" , 
                                                                             "20:00:00.000": "8:00 PM" , "20:30:00.000": "8:30 PM" , 
                                                                             "21:00:00.000": "9:00 PM" , "21:30:00.000": "9:30 PM" , 
                                                                             "22:00:00.000": "10:00 PM", "22:30:00.000": "10:30 PM", 
                                                                             "23:00:00.000": "11:00 PM", "23:30:00.000": "11:30 PM",
                                                                           "Sunrise":"Sunrise", "Sunset":"Sunset"]
                                                                           
		input "fromTimeDelay", "number", title: "+ minutes", required: false
                                                                           
         input "toTime", "enum", title: "To", required: true, options: [ "00:00:00.000": "12:00 AM", "00:30:00.000": "12:30 AM", 
                                                                         "01:00:00.000": "1:00 AM" , "01:30:00.000": "1:30 AM" , 
                                                                         "02:00:00.000": "2:00 AM" , "02:30:00.000": "2:30 AM" , 
                                                                         "03:00:00.000": "3:00 AM" , "03:30:00.000": "3:30 AM" , 
                                                                         "04:00:00.000": "4:00 AM" , "04:30:00.000": "4:30 AM" , 
                                                                         "05:00:00.000": "5:00 AM" , "05:30:00.000": "5:30 AM" , 
                                                                         "06:00:00.000": "6:00 AM" , "06:30:00.000": "6:30 AM" , 
                                                                         "07:00:00.000": "7:00 AM" , "07:30:00.000": "7:30 AM" , 
                                                                         "08:00:00.000": "8:00 AM" , "08:30:00.000": "8:30 AM" , 
                                                                         "09:00:00.000": "9:00 AM" , "09:30:00.000": "9:30 AM" , 
                                                                         "10:00:00.000": "10:00 AM", "10:30:00.000": "10:30 AM", 
                                                                         "11:00:00.000": "11:00 AM", "11:30:00.000": "11:30 AM",
                                                                         "12:00:00.000": "12:00 PM", "12:30:00.000": "12:30 PM", 
                                                                         "13:00:00.000": "1:00 PM" , "13:30:00.000": "1:30 PM" , 
                                                                         "14:00:00.000": "2:00 PM" , "14:30:00.000": "2:30 PM" , 
                                                                         "15:00:00.000": "3:00 PM" , "15:30:00.000": "3:30 PM" , 
                                                                         "16:00:00.000": "4:00 PM" , "16:30:00.000": "4:30 PM" , 
                                                                         "17:00:00.000": "5:00 PM" , "17:30:00.000": "5:30 PM" , 
                                                                         "18:00:00.000": "6:00 PM" , "18:30:00.000": "6:30 PM" , 
                                                                         "19:00:00.000": "7:00 PM" , "19:30:00.000": "7:30 PM" , 
                                                                         "20:00:00.000": "8:00 PM" , "20:30:00.000": "8:30 PM" , 
                                                                         "21:00:00.000": "9:00 PM" , "21:30:00.000": "9:30 PM" , 
                                                                         "22:00:00.000": "10:00 PM", "22:30:00.000": "10:30 PM", 
                                                                         "23:00:00.000": "11:00 PM", "23:30:00.000": "11:30 PM",
                                                                           "Sunrise":"Sunrise", "Sunset":"Sunset"]
	input "toTimeDelay", "number", title: "+ minutes", required: false
                                                                           
        
     input "pushNotifications", "bool", title: "Enable Push Notifications"
     
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

	//Schedule the lights on check
    if(fromTime == "Sunet"){
    	subscribe(location, "sunset", lightsOnDelay)
    }
    if(fromTime == "Sunrise"){
    	subscribe(location, "sunrise", lightsOnDelay)
    }
    if(fromTime != "Sunrise" && fromTime != "Sunset"){
		//Schedule to turn on lights
        schedule("2017-1-1T" + fromTime + "-0600", lightsOnDelay)
        log.debug "Lights On Scheduled"
    }
    
    
    //Schedule the lights off. Regardless of current state. If the toTime is hit, turn off the lights.
    if( toTime == "Sunset"){
    	log.debug "Sunset Subscription Created"
    	subscribe(location, "sunset", lightsOffDelay)
    }
    if(toTime == "Sunrise"){
    	log.debug "Sunrise Subscription Created"
    	subscribe(location, "sunrise", lightsOffDelay)
    }
    if(toTime != "Sunrise" && toTime != "Sunset"){
		//Schedule to turn off lights    
        schedule("2017-1-1T" + toTime + "-0600", lightsOffDelay)
        log.debug "Lights Off Scheduled"
    }
    
     
    // Subscribe to presence changes
    subscribe(presenceDevices, "presence", presenceEventHandler)
 
}



// Lights on and off controller.
// Not much meaning now, but pulled out in case there needs to be some global logic later
def lightsOff() {
	lights.off()
    if(pushNotifications){
    	sendPush("Smart lights have turned off")
    }
}

def lightsOn() {
	lights.on()
    if(pushNotifications){
    	sendPush("Smart lights have turned on")
    }
}

def lightsOffDelay(){
	def currentToDelay = findToTimeDelay()
    log.debug("Turning lights off in $currentToDelay minutes")
	runIn(currentToDelay * 60, "lightsOff", [overwrite: false])
}
def lightsOnDelay(){
	def currentFromDelay = findFromTimeDelay()
    log.debug("Turning lights on in $currentFromDelay minutes")
	runIn(currentFromDelay * 60, "smartTurnLightsOn", [overwrite: false])
}


//Sunrise and Sunset event handlers
def sunsetHandler(evt){
	log.debug "Sunset Event Fired"
    if(fromTime == "Sunset"){
    	lights.on()
    }
    if(toTime == "Sunset"){
    	lights.off()
    }
}

//Presence Event Handlers

def presenceEventHandler(evt){
	log.debug "Presence Event Fired"
    
	if (evt.value == "not present") {
    	//Someone left, check if everyone is gone and turn lights on if necessary
       log.debug "checking if everyone is away"
        if (everyoneIsAway()) {
            log.debug "starting sequence"
            runIn(findFalseAlarmThreshold() * 60, "smartTurnLightsOn", [overwrite: false])
        }
    } else {
        log.debug "Someone arrived"
        runIn(findFalseAlarmThreshold() * 60, "lightsOff", [overwrite: false])
    }
}

def isInsideLightsOnSchedule(){
	//fromTime and toTime are both string values similar to 13:30:00.000 or "Sunrise" or "Sunset"
	def loft = fromTime
    def lott = toTime

	def localSun = getSunriseAndSunset()
    
    log.debug location.currentValue("sunriseTime")
    if(fromTime == "Sunrise"){
    	//Have also tried localSun.sunrise
    	//loft = location.currentValue("sunriseTime")
        loft = localSun.sunrise
    }
    if(fromTime == "Sunset"){
    	//Have also tried localSun.sunset
    	//loft = location.currentValue("sunsetTime")
        loft = localSun.sunset
    }
    if(toTime == "Sunrise"){
    	//Have also tried localSun.sunrise
    	//lott = location.currentValue("sunriseTime")
        lott = localSun.sunrise
    }
    if(toTime == "Sunset"){
    	//Have also tried localSun.sunset
    	//lott = location.currentValue("sunsetTime")
        lott = localSun.sunset
    }
    def now = new Date()
    log.debug "LOFT: $loft, LOTT: $lott, DATE: $now"
	def between = true
    between = timeOfDayIsBetween(loft, lott, new Date(), location.timeZone)
    
    return between
    
    //if(between){
   // 	log.debug "Within lights on schedule, turn on"
   // 	lights.on()
    //} else {
    //	log.debug "Not within lights on schedule. Leaving light off"
    //}
	
}

def smartTurnLightsOn() {
    if (everyoneIsAway()) {
        def threshold = 1000 * 60 * findFalseAlarmThreshold() - 1000
        def awayLongEnough = presenceDevices.findAll { person ->
            def presenceState = person.currentState("presence")
            def elapsed = now() - presenceState.rawDateCreated.time
            elapsed >= threshold
        }
        log.debug "Found ${awayLongEnough.size()} out of ${presenceDevices.size()} person(s) who were away long enough"
        if (awayLongEnough.size() == presenceDevices.size()) {
           log.debug "Everyone has been away long enough"
           if(isInsideLightsOnSchedule()){
           		log.debug "We are betweent he lights on schedule"
               	lightsOn()         
           }
        } else {
            log.debug "not everyone has been away long enough; doing nothing"
        }
    } else {
        log.debug "not everyone is away; doing nothing"
    }
}


private findFalseAlarmThreshold() {
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 1
}

private findFromTimeDelay() {
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (fromTimeDelay != null && fromTimeDelay != "" && fromTimeDelay >= 0) ? fromTimeDelay : 0
}

private findToTimeDelay() {
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (toTimeDelay != null && toTimeDelay != "" && toTimeDelay >= 0) ? toTimeDelay : 0
}

private everyoneIsAway() {
    def result = true
    // iterate over our people variable that we defined
    // in the preferences method
    for (person in presenceDevices) {
        if (person.currentPresence == "present") {
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
}