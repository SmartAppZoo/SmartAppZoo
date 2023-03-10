/*
 *  EPIC! Scene Director
 *  Version: 1.0
 *  Written by Ben Koehler (@triphius)
 * 
 *  https://github.com/triphius/smartthings/blob/master/epic-scene-director-v1.groovy
 *
 *  Last Updated: June 7, 2015
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
 *  Based on Rob Landry's (@rob_a_landry) Hue/Bulb Scenes (v1.0.2)
 *  https://github.com/roblandry/Hue-Bulb-Scenes/blob/master/hue-bulb-scenes.groovy
 *  
 *  Designed for use with my EPIC! LIFX Bulb Device Type, which is based on Nicolas Cerveaux's (@zzarbi) LIFX 2.0 Device Type.
 *
 *  EPIC! LIFX Bulb		https://github.com/triphius/smartthings/blob/master/epic-lifx-bulb.groovy
 *  LIFX 2.0 Device Type 	https://github.com/zzarbi/smartthings/blob/master/device/lifx/lifx-bulb-2.groovy
 *  LIFX 2.0 Connect App	https://github.com/zzarbi/smartthings/blob/master/app/lifx/lifx-connect2.groovy
 */
 
definition(
	name: "EPIC! Scene Director",
	namespace: "triphius",
	author: "Ben Koehler",
	description: "Trigger scenes that include LIFX bulbs, Hue bulbs and Switch devices.",
	category: "SmartThings Labs",
	iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png"
)

preferences {
	
	page(name: "mainPage", title: "", install: true, uninstall: true)
	
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		
		section {
			
			input "starting", "time", title: "Starting", required: false
			
			input "ending", "time", title: "Ending", required: false
			
		}
		
	}
	
}

def mainPage() {
	
	dynamicPage(name: "mainPage") {

		// Introduction //
		
		section() {
			
			paragraph image: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment1-icn@2x.png", "The EPIC! Scene Director will allow you to trigger scenes that include LIFX bulbs, Hue bulbs and Switch devices."
		
		}


		// Triggers //
		
		def anythingSet = anythingSet()

		if (anythingSet) {
			section("Select Triggers..."){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
				ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
				ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
				ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
				ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
				ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
				ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false
			}
		}
		
		section(anythingSet ? "Select Additional Triggers..." : "Select Triggers...", hideable: anythingSet, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
			ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
			ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
			ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
			ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
			ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false
		}


		// LIFX Bulbs //
		section() {

			paragraph title: "LIFX Bulbs", image: "https://d21buns5ku92am.cloudfront.net/40204/logo/small-1385905812.png", "Configure scene changes for LIFX bulbs here."
			
			input "bulbsLifxSelection", "capability.colorControl", title: "Select LIFX Bulbs", required: false, multiple: true
			
			input "bulbsLifxColor", "enum", title: "Set Color", required: false, multiple: false, options: [
				"Red":"Red",
				"Orange":"Orange",
				"Yellow":"Yellow",
				"Lime":"Lime",
				"Green":"Green",
				"Turquoise":"Turquoise",
				"Cyan":"Cyan",
				"Blue":"Blue",
				"Purple":"Purple",
				"HotPink":"Hot Pink"
			]
			
			input "bulbsLifxWhite", "enum", title: "Set White Temperature", required: false, multiple: false, options: [
				"2700K":"Warm (2700K)",
				"3000K":"Warm (3000K)",
				"3500K":"Neutral (3500K)",
				"4100K":"Cool (4100K)",
				"5500K":"Daylight (5500K)",
				"6500K":"Daylight (6500K)"
			]

			input "bulbsLifxBrightness", "enum", title: "Set Brightness", required: false, options: [
				"5":"5%",
				"10":"10%",
				"15":"15%",
				"20":"20%",
				"25":"25%",
				"30":"30%",
				"35":"35%",
				"40":"40%",
				"45":"45%",
				"50":"50%",
				"55":"55%",
				"60":"60%",
				"65":"65%",
				"70":"70%",
				"75":"75%",
				"80":"80%",
				"85":"85%",
				"90":"90%",
				"95":"95%",
				"100":"100%"
			]

			input "bulbsLifxPowerState", "enum", title: "Set Power State", required: false, options: [
				"on":"Turn On",
				"off":"Turn Off",
			]
			
			input "bulbsLifxTransTime", "number", title: "Set Transition Time (in seconds)", required: true, defaultValue: "1"
			
		}


		// Additional Options //
		section("Additional Options", hideable: true, hidden: true) {
			
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: [
				"Monday",
				"Tuesday",
				"Wednesday",
				"Thursday",
				"Friday",
				"Saturday",
				"Sunday"
			]
			
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
			
		}
		
		
		// Name and Mode //
		section([mobileOnly:true]) {
			
			label title: "Assign a name", required: false
			
			mode title: "Set for specific mode(s)", required: false
			
		}
		
	}
	
}

private anythingSet() {
	for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","arrivalPresence","departurePresence","smoke","water","button1","triggerModes","timeOfDay"]) {
		if (settings[name]) {
			return true
		}
	}
	return false
}

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}

	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}

def eventHandler(evt) {
	log.trace "eventHandler($evt.name: $evt.value)"
	if (allOk) {
		log.trace "allOk"
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					takeAction(evt)
				}
			}
			else {
				takeAction(evt)
			}
		}
		else {
			log.debug "Not taking action because it was already taken today."
		}
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}

def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	eventHandler(null)
}

def appTouchHandler(evt) {
	takeAction(evt)
}

private takeAction(evt) {

	if (frequency) {
		state[frequencyKey(evt)] = now()
	}


	// LIFX Actions //
	
	def lifxHue = -1
	def lifxSaturation = 100
	def lifxKelvin = -1
	def lifxPowerState = -1
	def lifxBrightness = 100
	def lifxDuration = bulbsLifxTransTime

	switch(bulbsLifxColor) {
		case "Red":
			lifxHue = 100
			break;			
		case "Orange":
			lifxHue = 10.5
			break;
		case "Yellow":
			lifxHue = 15
			break;
		case "Lime":
			lifxHue = 20
			break;
		case "Green":
			lifxHue = 36
			break;
		case "Turquoise":
			lifxHue = 42
			break;
		case "Cyan":
			lifxHue = 50
			break;
		case "Blue":
			lifxHue = 67
			break;
		case "Purple":
			lifxHue = 77
			break;
		case "HotPink":
			lifxHue = 87
			break;		
	}
		
	switch(bulbsLifxWhite) {
		case "2700K":
			lifxKelvin = 2700
			break;			
		case "3000K":
			lifxKelvin = 3000
			break;
		case "3500K":
			lifxKelvin = 3500
			break;
		case "4100K":
			lifxKelvin = 4100
			break;
		case "5500K":
			lifxKelvin = 5500
			break;
		case "6500K":
			lifxKelvin = 6500
			break;	
	}
	
	switch(bulbsLifxBrightness) {
		case "5":
			lifxBrightness = 5
			break;
		case "10":
			lifxBrightness = 10
			break;
		case "15":
			lifxBrightness = 15
			break;
		case "20":
			lifxBrightness = 20
			break;
		case "25":
			lifxBrightness = 25
			break;
		case "30":
			lifxBrightness = 30
			break;
		case "35":
			lifxBrightness = 35
			break;
		case "40":
			lifxBrightness = 40
			break;
		case "45":
			lifxBrightness = 45
			break;
		case "50":
			lifxBrightness = 50
			break;
		case "55":
			lifxBrightness = 55
			break;
		case "60":
			lifxBrightness = 60
			break;
		case "65":
			lifxBrightness = 65
			break;
		case "70":
			lifxBrightness = 70
			break;
		case "75":
			lifxBrightness = 75
			break;
		case "80":
			lifxBrightness = 80
			break;
		case "85":
			lifxBrightness = 85
			break;
		case "90":
			lifxBrightness = 90
			break;
		case "95":
			lifxBrightness = 95
			break;
		case "100":
			lifxBrightness = 100
			break;
	}
	
	switch(bulbsLifxPowerState) {
		case "on":
			lifxPowerState = 1
			break;			
		case "off":
			lifxPowerState = 0
			break;
	}	

	state.previous = [:]

	bulbsLifxSelection.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation"),
			"kelvin": it.currentValue("kelvin")
		]
    }

	log.debug "Current LIFX Values = $state.previous"

	def newLifxValue = [hue: lifxHue, saturation: lifxSaturation, kelvin: lifxKelvin, level: lifxBrightness as Integer, duration: lifxDuration as Integer]

	log.debug "New LIFX Values = $newLifxValue"

	bulbsLifxSelection.each {
	
		if (lifxHue != -1) {
			it.setAdjustedColor(newLifxValue)
		}
		
		if (lifxKelvin != -1) {
			it.setAdjustedWhite(newLifxValue)
		}
		
		if (lifxPowerState == 1) {
			it.on()
		}
		
		if (lifxPowerState == 0) {
			it.off()
		}
	
	}
    
}

private frequencyKey(evt) {
	"lastActionTimeStamp"
}

private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}

private oncePerDayOk(Long lastTime) {
	def result = true
	if (oncePerDay) {
		result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
		log.trace "oncePerDayOk = $result"
	}
	result
}

private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
