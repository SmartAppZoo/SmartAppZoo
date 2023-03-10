 /**
 *  Turn It On When I'm Here Between Sunset/Sunrise
 *  Borrowed code from: Sunrise/Sunset by dianoga7@3dgo.net
 *	Borrowed code from: Scott Jones & Barry & ChrisB
 *
 *  Author: Darc Ranger
 *  Date: 17 Jan 2014
 * 
 *  Modified by: David A
 *  Date: 31 Aug 2015
 *
 *  Turn on a light when I'm Here [REQUIRED]. Optionally turn it off a set 
 *  number of minutes later.  Only turns on between sunset and sunrise.
 */
 

// Automatically generated. Make future change here.
definition(
    name: "On When I Arrive After Dark",
    namespace: "dadlersan",
    author: "Darc Ranger",
    description: "Turn on a light when someone arrives between sunset and sunrise. Optionally turn it off a set number of minutes later.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather4-icn@3x.png")

preferences {
	section("When someone arrives..."){
		input "presence1", "capability.presenceSensor", title: "Who?", required: true, multiple: true
	}
	section("Turn on this light(s)..."){
		input "switch1", "capability.switch", multiple: true
	}
    	section("Turn off after how many minutes?") {
		input "minutes", "number", title: "Enter 0 to not auto-off"
	
    	}
	section ("Sunrise offset (optional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
	section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
		input "zipCode", "text", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		//input "phone", "phone", title: "Send a Text Message?", required: false
	}
}

def installed()
{
	initSettings()
}

def updated()
{
	//unschedule()
	unsubscribe()
	initSettings()
}

def initSettings()
{
	subscribe(presence1, "presence", presenceHandler)
	if (state.modeStartTime == null) {
		state.modeStartTime = 0
	}
}

def scheduleSwitchOff(durationMinutes)
{
	if (durationMinutes == 0) {
    	log.debug "leaving on indefinitely"
	} else {
        switch1?.off(delay: durationMinutes * 60000)
        log.debug "Switching off in $durationMinutes minutes"
	}
}

def presenceHandler(evt)
{
	astroCheck()  // Moved from initSettings() because rise/set times weren't updating

	def t1 = now()
	def resultQ = t1 < state.riseTime || t1 >= state.setTime
	log.debug "presenceHandler $evt.name: $evt.value"
	def current = presence1.currentValue("presence")
    
	log.debug current
	def presenceValue = presence1.find{it.currentPresence == "present"}
	log.debug "presenceValue = $presenceValue and $presence1"
 	log.debug "Rise: $state.riseTime and Set: $state.setTime and Now: $t1 and Dark: $resultQ"
    
	if ((presenceValue) && (resultQ == true)) {
        switch1.on()
		log.debug "Someone's home! ...$presenceValue and Lighting Activated"
		if (sendPushMessage == "Yes") {sendPush("$presence1 $location - Lighting Activated")}
		scheduleSwitchOff(minutes)
	}
	else if ((presenceValue) && (resultQ == false)) {
		log.debug "Someone's home, but's it not dark! Lighting not activated"
		if (sendPushMessage == "Yes") {sendPush("$presence1 $location - Lighting NOT Activated")}
	}
}

def astroCheck() {

	//def t0 = now()
	def modeStartTime = new Date(state.modeStartTime)

	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	state.riseTime = s.sunrise.time
	state.setTime = s.sunset.time
	log.debug "rise: ${new Date(state.riseTime)}($state.riseTime), set: ${new Date(state.setTime)}($state.setTime)"
}

//private enabled() {
//	def result

//		def t0 = now()
//		result = t0 < state.riseTime || t0 > state.setTime
        //switch1.on()
//	}


private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
