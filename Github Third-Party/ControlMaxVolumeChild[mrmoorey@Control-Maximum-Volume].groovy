/**
 *  Control Maximum Volume Child
 *
 *  Copyright 2018 Alan Moore
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
	name: "Control Maximum Volume Child",
	namespace: "mrmoorey",
	author: "Alan Moore",
	description: "Control the maximum volume of a speaker (Sonos, Bose, etc). This is the child SmartApp allowing multiple automations",
	category: "My Apps",
	iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics16-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics16-icn@2x.png"
)

preferences {
	page(name: "mainPage", title: "Speaker to Control", install: true, uninstall: true)
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		section("Select Speaker") {
			input "player", "capability.musicPlayer", title: "Speaker", required: true
		}
		section("Condition"){
			input "maxvolume", "number", title: "When volume is more than...", description: "0-100%", required: true
			input "volume", "number", title: "Set the volume to...", description: "0-100%", required: true
		}
		section("More options", hideable: true, hidden: true) {
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			if (settings.modes) {
				input "modes", "mode", title: "Only when mode is", multiple: true, required: false
			}
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)"
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	app.updateLabel(defaultLabel())    
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
   	app.updateLabel(defaultLabel())
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	log.trace "subscribeToEvents()"
	subscribe(player, "level", eventHandler, [filterEvents: false])
}

def eventHandler(evt) {
	def currentLevel = player.currentState("level").value
	if (allOk) {
		def lastTime = state[frequencyKey(evt)]
		if (oncePerDayOk(lastTime)) {
			if (frequency) {
				if (lastTime == null || now() - lastTime >= frequency * 60000) {
					if(currentLevel.toInteger() > maxvolume.toInteger()) {
						log.debug "Changing to $volume%. $currentLevel% > $maxvolume%"
						takeAction(evt)
					}
					else
						log.debug "Nothing to do. $currentLevel% < $maxvolume%"
				}
				else {
					log.debug "Not taking action because $frequency minutes have not elapsed since last action"
				}
			}
			else {
				if(currentLevel.toInteger() > maxvolume.toInteger()) {
					log.debug "Changing to $volume%. $currentLevel% > $maxvolume%"
					takeAction(evt)
				}
				else
					log.debug "Nothing to do. $currentLevel% < $maxvolume%"
			}
		}
		else {
			log.debug "Not taking action because it was already taken today"
		}
	}
}

private takeAction(evt) {
	log.debug "takeAction()"
	def options = [:]
	if (volume) {
		player.setLevel(volume as Integer)
		options.delay = 1000
	}

	if (frequency) {
		state.lastActionTimeStamp = now()
	}
}

private frequencyKey(evt) {
	//evt.deviceId ?: evt.value
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
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

// A method that will set the default label of the automation.
// It uses the selected speakers and volume parameters
def defaultLabel() {
	def playerLabel = player.displayName

	"Set $playerLabel to $volume% if greater than $maxvolume%"
}
