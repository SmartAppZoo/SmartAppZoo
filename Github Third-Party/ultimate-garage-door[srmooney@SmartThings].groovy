/**
 *  Ultimate Garage Door
 *
 *  Copyright 2015 Sean Mooney
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
    name: "Ultimate Garage Door",
    namespace: "srmooney",
    author: "Sean Mooney",
    description: "The ultimate garage door app",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/srmooney/SmartThings/master/icons/garage.png",
    iconX2Url: "https://raw.githubusercontent.com/srmooney/SmartThings/master/icons/garage.png",
    iconX3Url: "https://raw.githubusercontent.com/srmooney/SmartThings/master/icons/garage.png")

//import groovy.time.TimeDuration 
//import groovy.time.TimeCategory

preferences {
	page(name: "page1", title: "", uninstall:true, install:true) {
    	section("The Door") {
            input "doorSensor", "capability.contactSensor", title: "Which Sensor?", required:true
            input "doorSwitch", "capability.momentary", title: "Which Relay?", required:true
            input "doorName", "text", title: "Name", defaultValue:"Garage Door", required:false
        }
        
        section("Settings") {
			href "page2", title:"Arrival / Departure"
            href "page3", title:"Night Settings"
            href "page4", title:"Notifications"
		}        
    }
    
    /* Presence */
    page(name: "page2", title: "", uninstall:false, install:false) {
    	section("Arrival / Departure"){
        	input "presenceArrive", "capability.presenceSensor", title: "Open when these people arrive:", multiple:true, required: false
        	input "presenceDepart", "capability.presenceSensor", title: "Close when these people leave:", multiple:true, required: false
        }
    }
    
    /* Night Time */
    page(name: "page3", title: "", uninstall:false, install:false) {
        section ("Night Settings") {
            input "closeSunset", "bool", title: "Close after sunset", required: false
        }
        section ("Sunset offset (optional)...", hideable:true, hidden:true) {
            input "sunsetOffsetValue", "number", title: "Minutes", required: false
            input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
        }
        section ("If opened after dark, close after...") {
            input "closeAfter", "number", title: "Minutes", required: false
        }
    }
    
    /* Notification */
    page(name: "page4", title: "", uninstall:false, install:true) {
        section () {
            input "notify", "enum", title: "Notify when...", required: false, metadata: [values: ["Opening","Closing"]], multiple: true
        }
        section () {
        	paragraph "If left open longer than..."
            input "notifyLeftOpen", "number", title: "Minutes", required: false
            paragraph "Repeat until closed..."
            input "notifyFrequency", "number", title: "Minutes", required: false
            paragraph "A maximum of..."
            input "notifyMax", "number", title: "Times", required: false
            paragraph "Leave empty for unlimited notifications"
        }
    } 
}

def getDoorName() { settings.doorName ?: "Garage Door" }

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "${getDoorName()} is ${settings.doorSensor.contactState?.value}"
	subscribe(presenceArrive, "presence", presenceHandler)
	subscribe(presenceDepart, "presence", presenceHandler)
	subscribe(doorSensor, "contact", contactHandler)
    subscribe(location, "sunsetTime", sunsetTimeHandler)
    subscribe(location, "sunriseTime", sunriseTimeHandler)
	subscribe(app, appTouchHandler)

	state.openTime = 0
    state.openNotifyCount = 0
    state.closeAttempts = 0

	log.debug "state: $state"
    
    /* Check door state for left open */
    if (settings.notifyLeftOpen && settings.doorSensor.contactState?.value == "open"){
    	scheduleDoorCheck()
    }


    /* If sunset is still to come, schedule closeAfterSunset */
    if (settings.closeSunset) {
        def sunsetTime = getSunriseAndSunset().sunset.time + getSunsetOffsetValue()
        log.debug "sunset today: ${new Date(sunsetTime)} (${formatLocalDate(sunsetTime)} local time)"
        if (sunsetTime > now()) {
            log.debug "schedule close on $sunsetTime, ${new Date(sunsetTime)}"
            runOnce(new Date(sunsetTime), closeAfterSunset)
        }
    }

}



/************************************************
 * Events
 ************************************************/
def appTouchHandler(evt){
	log.debug "appTouchHandler: ${evt}"
	if (doorSensor.contactState.value == "open"){ close() }
    else { open() }
}

def presenceHandler(evt) {
	//log.debug "${evt.displayName} has left the ${location}"

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

def contactHandler(evt) {
	log.debug "Contact is in ${evt.value} state"
    
    if(evt.value == "open" && notify.contains("Opening")) {
    	def msg = "${getDoorName()} opened"
        log.debug "$msg"
        sendPush("$msg")
    }

    if(evt.value == "closed" && notify.contains("Closing")){
        def msg = "${getDoorName()} closed"
        log.debug "$msg"
        sendPush("$msg")
    }
    
    if (evt.value == "open" && closeAfter && isItNight()){
        runIn(closeAfter * 60, closeWhenDark)
    }
    
    if (notifyLeftOpen && evt.value == "open"){
    	scheduleDoorCheck()
    }
    
    if (evt.value == "close"){
    	state.openTime = 0
        state.openNotifyCount = 0
    }
}

def sunsetTimeHandler(evt) {
	def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", evt.value)
	def sunsetTomorrow = sunsetTime.time + getSunsetOffsetValue()
    sendPush("Next Sunset: ${new Date(sunsetTomorrow)} (${formatLocalDate(sunsetTomorrow)} local time)")
    if (settings.closeSunset) {        
        log.debug "schedule close for $sunsetTomorrow, ${new Date(sunsetTomorrow)}"
        runOnce(new Date(sunsetTomorrow), closeAfterSunset)
    }
}

def sunriseTimeHandler(evt){    
}

/************************************************
 * Methods
 ************************************************/

def formatLocalDate(epoch){
	new Date(epoch).format('M-d-yyyy hh:mm:ss a',location.timeZone)
}

def formatSeconds(seconds){
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

def getSunsetOffsetValue() {
    if (settings.sunsetOffsetValue && settings.sunsetOffsetDir == 'Before') { (settings.sunsetOffsetValue * 60 * 1000) * -1 }
    else if (settings.sunsetOffsetValue) { (settings.sunsetOffsetValue * 60 * 1000) }
    else { 0 }
}

def isItNight(){
    def sunrise = getSunriseAndSunset().sunrise.time
    def sunset = getSunriseAndSunset().sunset.time + getSunsetOffsetValue()

    log.debug "sunrise: $sunrise"
    log.debug "sunset (original): ${getSunriseAndSunset().sunset.time}"
    log.debug "sunset: $sunset"
    log.debug "now: ${now()}"

    if(sunrise > now() || sunset < now()){
        log.debug "isItNight: Yes"
        return true
    }
    else {
        log.debug "isItNight: No"
        return false
    }
  }

def scheduleDoorCheck() {
	def delay = (notifyLeftOpen * 60)
    state.openTime = delay
    log.debug "Schedule door check in $delay secs"
    runIn(delay, doorOpenTooLong, [overwrite: false])
}

def doorOpenTooLong(){
	log.debug "doorOpenTooLong(): $state.openTime secs"
    if (doorSensor.latestValue("contact") == "open") {
    	state.openNotifyCount = state.openNotifyCount + 1
        
        if (notifyMax && state.openNotifyCount == notifyMax){
        	sendPush("Last reminder! ${getDoorName()} left open for ${formatSeconds(state.openTime)}")
        }        
        else{
        	sendPush("${getDoorName()} left open for ${formatSeconds(state.openTime)}")
        }
        
        if (notifyFrequency && (!notifyMax || (notifyMax && state.openNotifyCount < notifyMax))) {
        	def delay = notifyFrequency * 60
        	state.openTime = state.openTime + delay
        	runIn(delay, doorOpenTooLong, [overwrite: false])
        }
	}
}

def ensureClosed(){
    if (doorSensor.contactState.value == "open") {
        if (state.closeAttempts < 3){
            state.closeAttempts = state.closeAttempts + 1
            close()
        }
        else {
            sendPush("${getDoorName()} not closed, attempted to close 3 times.")
        }
    }
    else {
        state.closeAttempts = 0
    }
}

def close(){
	log.debug "Closing ${getDoorName()}"
    doorSwitch.push()
    runIn(60, ensureClosed)
}

def open(){
	log.debug "Opening ${getDoorName()}"
    doorSwitch.push()
}

def closeWhenDark(){
	if (doorSensor.contactState.value == "open") {
    	sendPush("Closing ${getDoorName()}, open for $closeAfter mins after sunset")
        close()
    }
}

def closeAfterSunset(){
    if (closeSunset == true && doorSensor.contactState.value == "open") {
        sendPush("Closing ${getDoorName()} ${ settings.sunsetOffsetValue && settings.sunsetOffsetDir ? settings.sunsetOffsetDir.toLowerCase() : "at"} sunset")
        close()
    }
}
