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
    name: "Sunset Light Schedule v2",
    namespace: "mihamil",
    author: "Michael Hamilton",
    description: "Turns lights on and off at a specific time if you are not home.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MakeItLookLike.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MakeItLookLike@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MakeItLookLike@2x.png")


preferences {

    //section ("Where are you?"){
    //	input "zipcode", "string", title: "Zip Code", required: true
    //}
    //section("Set these lights to this level"){
   // 	input "dimmers", "capability.switchLevel", title: "Lights", multiple: true, required: true
    //}
    section("Tturn these lights on"){
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
                                                                           "sunrise":"Sunrise", "sunset":"Sunset"]
                                                                           
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
                                                                           "sunrise":"Sunrise", "sunset":"Sunset"]
	input "toTimeDelay", "number", title: "+ minutes", required: false
                                                                           
        
     input "pushNotifications", "bool", title: "Enable Push Notifications"
     
    }
}

def installed() {
	sendNotificationEvent( "Installed with settings: ${settings}")

	initialize()
}

def updated() {
	sendNotificationEvent( "Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize() {
	 def tz = location.timeZone
     sendNotificationEvent("Timezone ${tz}")
    if(tz == null){
    	sendNotificationEvent("Location services is not enabled. UTC timezone used.")
    	tz = TimeZone.getTimeZone("UTC"); 
    }
    log.debug tz.lastRule
    
    
    def now = new Date()
    sendNotificationEvent("${now}")
    
	//Schedule the lights on check
    if(fromTime == "sunset"){
    	subscribe(location, fromTime, lightsOnDelay)
    }
    if(fromTime == "sunrise"){
    	subscribe(location, fromtime, lightsOnDelay)
    }
    if(fromTime != "sunrise" && fromTime != "sunset"){
		//Schedule to turn on lights
        log.debug tz
        def fdate_schedule = Date.parse('yyyy-MM-dd hh:mm:ss.SSS Z', new Date().format("yyyy-MM-dd ${fromTime} Z",location.timeZone))
        log.debug fdate_schedule
        sendNotificationEvent("From Schedule created with ${fdate_schedule}")
        schedule(fdate_schedule, lightsOnDelay)
        //log.debug "Lights On Scheduled"
    }
    
    
    //Schedule the lights off. Regardless of current state. If the toTime is hit, turn off the lights.
    if( toTime == "sunset"){
    	//log.debug "Sunset Subscription Created"
    	subscribe(location, toTime, lightsOffDelay)
    }
    if(toTime == sunrise){
    	//log.debug "Sunrise Subscription Created"
    	subscribe(location, toTime, lightsOffDelay)
    }
    if(toTime != "sunrise" && toTime != "sunset"){
		//Schedule to turn off lights    
        def tdate_schedule = Date.parse('yyyy-MM-dd hh:mm:ss.SSS Z', new Date().format("yyyy-MM-dd ${toTime} Z",location.timeZone))
        log.debug tdate_schedule
        sendNotificationEvent("To Schedule created with ${tdate_schedule}")
        schedule(tdate_schedule, lightsOffDelay)
        //log.debug "Lights Off Scheduled"
    }
    
     
    // Subscribe to presence changes
    subscribe(presenceDevices, "presence", presenceEventHandler)
 	sendNotificationEvent("Installation Complete")
}



// Lights on and off controller.
// Not much meaning now, but pulled out in case there needs to be some global logic later
def lightsOffDelay(){
	sendNotificationEvent("Lights off Delay scheduled for ${currentToDelay} minutes")
	def currentToDelay = findToTimeDelay()
    //log.debug("Turning lights off in $currentToDelay minutes")
	runIn(currentToDelay * 60, "lightsOff", [overwrite: false])
}
def lightsOff(){
	lights.each{ light -> 
    	light.off()
        log.debug "${light} Off"
    }
	
    log.debug "Lights Off"
    if(pushNotifications){
    	sendPush("Security lights have turned off")
    } else {
    	sendNotificationEvent("Security lights have turned off")
    }
}


def lightsOnDelay(){
	sendNotificationEvent("Lights On Scheduled for ${currentFromDelay} minutes.")
	def currentFromDelay = findFromTimeDelay()
    //log.debug("Turning lights on in $currentFromDelay minutes")
	runIn(currentFromDelay * 60, "checkScheduleAndTurnOnLights", [overwrite: false])
}
def lightsOn(){
	lights.each{ light -> 
    	light.on()
        log.debug "${light} On"
    }
    log.debug "Lights On"
    if(pushNotifications){
    	sendPush("Security lights have turned on")
    } else {
    	sendNotificationEvent("Security lights have turned off")
    }
}





//Sunrise and Sunset event handlers
//def sunsetHandler(evt){
	//log.debug "Sunset Event Fired"
    //if(fromTime == "Sunset"){
//    	lights.on()
//    }
//    if(toTime == "Sunset"){
//  /  	lights.off()
//    }
//}

//Presence Event Handlers

def presenceEventHandler(evt){
	sendNotificationEvent( "Presense Event Fired")
    
	if (evt.value == "not present") {
    	sendNotificationEvent( "Someone Left")
    	//Someone left, check if everyone is gone and turn lights on if necessary
       //log.debug "checking if everyone is away"
        if (everyoneIsAway()) {
        	
            runIn(findFalseAlarmThreshold() * 60, "checkScheduleAndTurnOnLights", [overwrite: false])
        }
    } else {
    	sendNotificationEvent("Someone Arrived")
        //log.debug "Someone arrived"
        runIn(findFalseAlarmThreshold() * 60, "lightsOff", [overwrite: false])
    }
}



def checkScheduleAndTurnOnLights(){
    if (everyoneIsAway()) {
    	//Everyone is away, Get a count of presense devices that have been away long enough to trigger.
        def threshold = 1000 * 60 * findFalseAlarmThreshold() - 1000
        def awayLongEnough = presenceDevices.findAll { person ->
        	//log.debug person
            def presenceState = person.currentState("presence")
            //log.debug presenseState
            def elapsed = now() - presenceState.rawDateCreated.time
            
            threshold = 0
            elapsed >= threshold
        }
        
        //log.debug "Found ${awayLongEnough.size()} out of ${presenceDevices.size()} person(s) who were away long enough"
        if (presenseDevices != null){
            if (awayLongEnough.size() == presenceDevices.size()) {
              // log.debug "Everyone has been away long enough"
               if(isInsideLightsOnSchedule()){
                    //log.debug "We are between the lights on schedule"
                    lightsOn()         
               }
            } else {
                //Someone has not been away long enough.
                //Reschedule this function for 1 minute from now.
                //log.debug "not everyone has been away long enough; doing nothing, checking again in 1 minute"
                runIn(60, "checkScheduleAndTurnOnLights", [overwrite: false])
            }
        } else {
        	if(isInsideLightsOnSchedule()){
                    //log.debug "We are between the lights on schedule"
                    lightsOn()         
               }
        }
        
    } else {
        //log.debug "not everyone is away; doing nothing"
    }
}



//Helpers
private findFalseAlarmThreshold(){
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 1
}

private findFromTimeDelay(){
    // In Groovy, the return statement is implied, and not required.
    // We check to see if the variable we set in the preferences
    // is defined and non-empty, and if it is, return it.  Otherwise,
    // return our default value of 10
    (fromTimeDelay != null && fromTimeDelay != "" && fromTimeDelay >= 0) ? fromTimeDelay : 0
}

private findToTimeDelay(){
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
    
    return result
}

def isInsideLightsOnSchedule(){
	//fromTime and toTime are both string values similar to 13:30:00.000 or "Sunrise" or "Sunset"
	def loft = fromTime
    def lott = toTime
    log.debug "LOFT: ${loft}    LOTT: ${lott}    NOW: ${new Date()}"

	def localSun = getSunriseAndSunset()
    
    if(fromTime == "sunrise"){
    	//Have also tried localSun.sunrise
    	//loft = location.currentValue("sunriseTime")
        loft = localSun.sunrise
    }
    if(fromTime == "sunset"){
    	//Have also tried localSun.sunset
    	//loft = location.currentValue("sunsetTime")
        loft = localSun.sunset
    }
    if(toTime == "sunrise"){
    	//Have also tried localSun.sunrise
    	//lott = location.currentValue("sunriseTime")
        lott = localSun.sunrise
    }
    if(toTime == "sunset"){
    	//Have also tried localSun.sunset
    	//lott = location.currentValue("sunsetTime")
        lott = localSun.sunset
    }
    def now = new Date()
    
	def between = true
    
    
    between = timeOfDayIsBetween(loft, lott, new Date(), location.timeZone)
    log.debug between
    sendNotificationEvent("In Schedule: ${between}")
    return between
    
    //if(between){
   // 	log.debug "Within lights on schedule, turn on"
   // 	lights.on()
    //} else {
    //	log.debug "Not within lights on schedule. Leaving light off"
    //}
	
}