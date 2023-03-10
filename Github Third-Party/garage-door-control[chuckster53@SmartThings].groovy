/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or  agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Garage Door Control 
 *
 *  Author: Chuck Wrobel
 *
 *  Date: 2015-10-12
 */
definition(
    name: "Garage Door Control",
    namespace: "Home",
    author: "Chuck Wrobel",
    description: "Auto Close Garage Door and get notices when open too long",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
    	section("The Door") {
            input "doorSensor", "capability.contactSensor", title: "Which Sensor? ", required:true
           	input "switches", "capability.switch", multiple: true
            input "doorName", "text", title: "Name", defaultValue:"Garage Door", required:false
        }
    /* Presence */
    	section("Arrival / Departure"){
        	input "presenceArrive", "capability.presenceSensor", title: "Open when these people arrive:", multiple:true, required: false
        	input "presenceDepart", "capability.presenceSensor", title: "Close when these people leave:", multiple:true, required: false
        }
    
   /* Night Settings */
        section ("Night Settings") {
            //input "closeSunset", "enum", title: "Close after sunset", required: false, metadata: [values: ["Yes","No"]]
            input "sunsetOff", "bool", title: "Close after sunset", required: false
        }
	    section ("Sunset offset (optional)...") {
		    input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		    input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	    }
	    section ("Zip code (optional, defaults to location coordinates)...") {
		    input "zipCode", "text", required: false
	    }
        section ("If opened after dark, close after...") {
            input "closeAfter", "number", title: "Minutes", required: false
        }
        
    /* Notifications */
	    section( "Notifications" ) {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneNumber", "phone", title: "Send a text message?", required: false
        }
        section ("Open Too Long") {
        	paragraph "If left open longer than..."
            input "notifyLeftOpen", "number", title: "Minutes", required: false
            paragraph "Repeat until closed..."
            input "notifyFrequency", "number", title: "Minutes", required: false
            paragraph "A maximum of..."
            input "notifyMax", "number", title: "Times", required: false
            paragraph "Leave empty for unlimited notifications"
        }

}

def getDoorName() { settings.doorName ?: "Garage Door" }

def installed() {
	initialize()
    log.debug "Installed with settings: ${settings}"
}

def updated() {
    log.debug "Updated with settings:  ${settings}"
	unsubscribe()
	//unschedule handled in astroCheck method
	initialize()
}

def initialize() {
	subscribe(location, "position", locationPositionChange)
	// subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
    /**
   	log.debug "${getDoorName()} is ${settings.doorSensor.contactState?.value}"
    **/
	subscribe(presenceArrive, "presence", presenceHandler)
	subscribe(presenceDepart, "presence", presenceHandler)
	subscribe(doorSensor, "contact", contactHandler)
	subscribe(app, appTouchHandler)

   	state.openTime = 0
    state.openNotifyCount = 0

    /* check door state for left open */
    if (settings.notifyLeftOpen && settings.doorSensor.contactState?.value == "open"){
    	scheduleDoorCheck()
    }
    
	astroCheck()
}

/* Events */
def appTouchHandler(evt){
	log.debug "appTouchHandler: ${evt}"
	if (doorSensor.contactState.value == "open"){ close() }
    else { open() }
}

def contactHandler(evt) {
	log.debug "Contact is in ${evt.value} state"
    
    if(evt.value == "open" && notify.contains("Opening")) {
        def openTime = "${evt.date}"
        log.debug "$openTime"
    	def msg = "${getDoorName()} opened $openTime"
        log.debug "$msg"
        sendPush("$msg")
    }
    if(evt.value == "closed" && notify.contains("Closing")){
        def msg = "${getDoorName()} closed"
        log.debug "$msg"
        sendPush("$msg")
    }
    
    
    // if (closeSunset == true && evt.value == "open" && closeAfter && isItNight()){
     if (evt.value == "open") { 
        def openTime = "${evt.date}"
        if (closeAfter && isItNight()){
            log.debug "closeAfter: $closeAfter minutes"
            //schedule this to run after the input delay
            //because it is night time
            runIn(closeAfter * 60, closeWhenDark)
        }
        else {
             if (notifyLeftOpen && evt.value == "open"){
    	         scheduleDoorCheck()
             }
       }
    }
    
    
    if (evt.value == "closed"){
            log.debug "close()"
    	state.openTime = 0
        state.openNotifyCount = 0
    }
}

def presenceHandler(evt) {
	log.debug "${evt.displayName} has left the ${location}"

	if (evt.value == "not present" && doorSensor.contactState.value == "open") {
    	for (person in presenceDepart) {
            if (person.toString() == evt.displayName){
				close()
                if (notify.contains("Closing")){
                	def msg = "Closing ${getDoorName()} due to the departure of ${evt.displayName}"
                    log.debug "$msg"
                    sendPush("$msg")
                }
                break
            }
        }
	}
    
    if (evt.value == "present" && doorSensor.contactState.value == "closed") {
    	for (person in presenceArrive) {
            if (person.toString() == evt.displayName){
				open()
                if (notify.contains("Opening")){
                	def msg = "Opening ${getDoorName()} due to the arriaval of ${evt.displayName}"
                    log.debug "$msg"
                    sendPush("$msg")
                }
                break
            }
        }
	}
}

def scheduleDoorCheck() {
    log.debug "scheduleDoorCheck"
	def delay = (notifyLeftOpen * 60)
    state.openTime = delay
    log.debug "Schedule door check in $delay secs"
    runIn(delay, doorOpenTooLong, [overwrite: false])
}

def doorOpenTooLong(){
	log.debug "doorOpenTooLong(): $state.openTime secs"
    if (doorSensor.latestValue("contact") == "open") {
        state.openNotifyCount = state.openNotifyCount + 1
        
	log.debug "notifyMax: $notifyMax"
        if (notifyMax && state.openNotifyCount == notifyMax){
	log.debug "Max hit"
        	sendPush("Last reminder! ${getDoorName()} left open for ${formatSeconds(state.openTime)}")
                if ( phoneNumber ) {
       				log.debug( "sending text message" )
        			sendSms( phoneNumber, "Last reminder! ${getDoorName()} left open for ${formatSeconds(state.openTime)}")
               }
        }        
        else{
        	sendPush("${getDoorName()} left open for ${formatSeconds(state.openTime)}")
            if ( phoneNumber ) {
       				log.debug( "sending text message" )
        			sendSms( phoneNumber,  "${getDoorName()} left open for ${formatSeconds(state.openTime)}")
               }
        }
	log.debug "notifyFrequency: $notifyFrequency"
        
        if (notifyFrequency && (!notifyMax || (notifyMax && state.openNotifyCount < notifyMax))) {
        	def delay = notifyFrequency * 60
        	state.openTime = state.openTime + delay
        	runIn(delay, doorOpenTooLong, [overwrite: false])
        }
	}
}

def close(){
	log.debug "Close ${getDoorName()}"
    if ( phoneNumber ) {
       		log.debug( "sending text message" )
        	sendSms( phoneNumber, "Closing ${getDoorName()}")
    }
    /* with RM10 relay you turn on and then off to close but
     * first call off just in case the switch was left in on mode
     */
    switches?.off()
    log.debug( "Switch OFF" )
	switches?.on()
    log.debug( "Switch ON" )
    switches?.off()
    log.debug( "Switch OFF" )
    /* doorSwitch.push() */
}

def open(){
	log.debug "Open ${getDoorName()}"
    /* with RM10 relay you turn on and then off to close but
     * first call off just in case the switch was left in on mode
     */
    switches?.off()
	switches?.on()
    switches?.off()
    /* doorSwitch.push() */
}

def closeWhenDark(){
	log.debug "closeWhenDark"
	if (doorSensor.contactState.value == "open") {
    	sendPush("Closing ${getDoorName()}, open for $closeAfter minutes at Night")
        if ( phoneNumber ) {
            sendSms( phoneNumber, "WARNING! ${getDoorName()} closing in 2 minutes")
       }
       /* close() */
       runIn(120, close)
    }
}


def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	// def riseTime = s.sunrise
	def setTime = s.sunset
	// log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"

	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
}

def sunsetHandler() {
	log.info "Executing sunset handler"
	if ( (sunsetOff)  && (doorSensor.contactState.value == "open")) {
    	sendPush("Closing ${getDoorName()} because it is after sunset")
        if ( phoneNumber ) {
            sendSms( phoneNumber, "FINAL COUNTDOWN! ${getDoorName()} closing in 2 minutes")
        }
        runIn(120, close)
    }
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phoneNumber) {
            log.debug("sending text message")
            sendSms(phoneNumber, msg)
        }
    }

	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

private isItNight(){
        log.debug "sunrise ${getSunriseAndSunset().sunrise.time}"
        log.debug "sunset ${getSunriseAndSunset().sunset.time}"
        log.debug "now: ${now()}"
           if(getSunriseAndSunset().sunrise.time > now() || 
                getSunriseAndSunset().sunset.time < now()){
                log.debug "isItNight: Yes"
            	return true
            }
            else {
                log.debug "isItNight: No"
            	return false
            }
      }
      
      /* Methods */
def formatSeconds(seconds){
    log.debug "formatSeconds"
	if (seconds < 60) { return "$seconds seconds" }
    def minutes = (seconds / 60)
    if (minutes == 1) { return "$minutes minute" }
    if (minutes > 0 && minutes < 59) { return "$minutes minutes" }
    def hours = (minutes / 60)
    if (hours == 1) { return "$hours hour" }
    if (hours > 0 && hours < 24) { return "$hours hours" }
    def days = (hours / 24)
    if (days == 1) { return "$days day" }
    if (days > 1) { return "$days days" }
}