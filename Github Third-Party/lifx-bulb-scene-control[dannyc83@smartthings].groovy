/**
 *  LIFX Bulb Scene Control
 *  Version: 1.0
 *  Written by Ben Koehler (@triphius)
 * 
 *  https://github.com/triphius/smartthings/blob/master/lifx-bulb-scene-control.groovy
 *
 *  Last Updated: May 31, 2015
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
 *  Designed for use with Nicolas Cerveaux's (@zzarbi) LIFX 2.0 Device Type and Connect App
 *  https://github.com/zzarbi/smartthings/blob/master/app/lifx/lifx-connect2.groovy
 *  https://github.com/zzarbi/smartthings/blob/master/device/lifx/lifx-bulb-2.groovy
 *
 */
 
definition(
	name: "LIFX Bulb Scene Control",
	namespace: "triphius",
	author: "Ben Koehler",
	description: "Sets the color, brightness level and state of your LIFX bulbs.",
	category: "SmartThings Labs",
	iconUrl: "https://d21buns5ku92am.cloudfront.net/40204/logo/small-1385905812.png",
	iconX2Url: "https://d21buns5ku92am.cloudfront.net/40204/logo/small-1385905812.png"
)

preferences {
	page(name: "mainPage", title: "Sets the color, brightness level and state of your LIFX bulbs.", install: true, uninstall: true)
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
	dynamicPage(name: "mainPage") {
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
		section("Color Settings for LIFX Bulbs...") {
			input "bulbsColor", "capability.colorControl", title: "Which LIFX Bulbs?", required:false, multiple:true
			input "color", "enum", title: "LIFX Color?", required: false, multiple:false, options: [
				"WhiteWarm":"White (Warm)",
				"WhiteNeutral":"White (Neutral)",
				"WhiteCool":"White (Cool)",
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
			input "lightLevel", "enum", title: "Light Level?", required: false, options: [10:"10%",20:"20%",30:"30%",40:"40%",50:"50%",60:"60%",70:"70%",80:"80%",90:"90%",100:"100%"]
		}
        
		// Turn On //
		section("Turn on these LIFX Bulbs...") {
			input "bulbsOn", "capability.switch", title: "Which Bulbs?", required:false, multiple:true
		}        

		// Turn Off //
		section("Turn off these LIFX Bulbs...") {
			input "bulbsOff", "capability.switch", title: "Which Bulbs?", required:false, multiple:true
		}

		// More Options //
		section("More options", hideable: true, hidden: true) {
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
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

	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "WhiteWarm":
			hueColor = 9.72
			saturation = 10
			break;
		case "WhiteNeutral":
			hueColor = 42.5
			saturation = 4
			break;
		case "WhiteCool":
			hueColor = 57.77
			saturation = 17
			break;
		case "Red":
			hueColor = 100
			break;			
		case "Orange":
			hueColor = 10.5
			break;
		case "Yellow":
			hueColor = 15
			break;
		case "Lime":
			hueColor = 20
			break;
		case "Green":
			hueColor = 36
			break;
		case "Turquoise":
			hueColor = 42
			break;
		case "Cyan":
			hueColor = 50
			break;
		case "Blue":
			hueColor = 67
			break;
		case "Purple":
			hueColor = 77
			break;
		case "HotPink":
			hueColor = 87
			break;		
	}

	state.previous = [:]

	bulbsColor.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}

	bulbsOn.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch")
		]
	}

	bulbsOff.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch")
        ]
    }

	log.debug "current values = $state.previous"

	def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]

	log.debug "new hue value = $newValue"

	bulbsColor*.setAdjustedColor(newValue)
	bulbsOn*.on()
	bulbsOff*.off()
    
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
