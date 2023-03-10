/** 
	Button Controller EXTREME
  	Copyright 2016  Author: SmartThings, butchered by SkyJedi
       
   Control devices using the buttons of an Remotec ZRC-90 Scene Master
   
  Change Log
  2016-11-11 Initial Release
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at: www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.Button Controller Plus
 	
 
*/
  
definition(
    name: "Button Controller EXTREME",
    namespace: "SkyJedi",
    author: "SkyJedi",
    description: "Control devices with buttons using Remotec ZRC-90 Scene Master",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote@2x.png"
    )

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")
	page(name: "configureButton5")
	page(name: "configureButton6")
    page(name: "configureButton7")
	page(name: "configureButton8")
    
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, choose your button hardware ...", nextPage: "configureButton1", uninstall: configured()) {
        
        section {
			input "buttonDevice", "capability.button", title: "Select button 'thing' device...", multiple: false, required: true
            //input "numbButtons", "number", title: "Number of Buttons", description: "Number of buttons your remote has..", required: true
		}
		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
        
        section() {
        	label title: "Assign a name:", required: false
        } 
    }
}

def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the FIRST button... ",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))          
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a SECOND button, set it up here or 'Next'",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}
def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a THIRD button, set it up here or 'Next'",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a FOURTH button, set it up here or 'Next'",
		nextPage: "configureButton5", uninstall: configured(), getButtonSections(4))
}
def configureButton5() {
	dynamicPage(name: "configureButton5", title: "If you have a FIFTH button, set it up here or 'Next'",
		nextPage: "configureButton6", uninstall: configured(), getButtonSections(5))
}
def configureButton6() {
	dynamicPage(name: "configureButton6", title: "If you have a SIXTH button, set it up here or 'Next'",
		nextPage: "configureButton7", uninstall: configured(), getButtonSections(6))
}
def configureButton7() {
	dynamicPage(name: "configureButton7", title: "If you have a SEVENTH button, set it up here or 'Next'",
		nextPage: "configureButton8", uninstall: configured(), getButtonSections(7))
}
def configureButton8() {
	dynamicPage(name: "configureButton8", title: "If you have a EIGHTH button, set it up here or 'Done'",
		install: true, uninstall: true, getButtonSections(8))
}

def getButtonSections(buttonNumber) {
	return {
 //       log.debug "buttonNumber($buttonNumber)"
 
		section("Lights to Toggle") {
			input "lights_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lights_${buttonNumber+8}_pushed", "capability.switch", title: "Double-tapped", multiple: true, required: false
            input "lights_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Dimmers to Toggle") {
			input "lightsDT_${buttonNumber}_pushed", "capability.switchLevel", title: "Pushed", multiple: true, required: false
			input "lightsDTVal_${buttonNumber}_pushed", "number", title: "Dim Level", required: false, description: "0 to 99"
			input "lightsDT_${buttonNumber+8}_pushed", "capability.switchLevel", title: "Double-tapped", multiple: true, required: false
			input "lightsDTVal_${buttonNumber+8}_pushed", "number", title: "Dim Level", required: false, description: "0 to 99"
            input "lightsDT_${buttonNumber}_held", "capability.switchLevel", title: "Held", multiple: true, required: false
			input "lightsDTVal_${buttonNumber}_held", "number", title: "Dim Level", required: false, description: "0 to 99"
		}
		section("Locks") {
			input "locks_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "locks_${buttonNumber+8}_pushed", "capability.lock", title: "Double-tapped", multiple: true, required: false
            input "locks_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
		}
        section("Lights to Turn On") {
			input "lightOn_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightOn_${buttonNumber+8}_pushed", "capability.switch", title: "Double-tapped", multiple: true, required: false
            input "lightOn_${buttonNumber}_held", "capability.switch", title: "held", multiple: true, required: false

		}
		section("Lights to Turn Off") {
			input "lightOff_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightOff_${buttonNumber+8}_pushed", "capability.switch", title: "Double-tapped", multiple: true, required: false
           	input "lightOff_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Modes") {
			input "mode_${buttonNumber}_pushed", "mode", title: "Pushed", required: false
			input "mode_${buttonNumber+8}_pushed", "mode", title: "Double-tapped", required: false
            input "mode_${buttonNumber}_held", "mode", title: "Held", required: false
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			section("Hello Home Actions") {
				log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "Pushed", required: false, options: phrases
				input "phrase_${buttonNumber+8}_pushed", "enum", title: "Double-tapped", required: false, options: phrases
				input "phrase_${buttonNumber}_held", "enum", title: "Held", required: false, options: phrases
			}
		}
		section("Sirens") {
			input "sirens_${buttonNumber}_pushed","capability.alarm" ,title: "Pushed", multiple: true, required: false
			input "sirens_${buttonNumber+8}_pushed", "capability.alarm", title: "Double-tapped", multiple: true, required: false
			input "sirens_${buttonNumber}_held","capability.alarm" ,title: "Held", multiple: true, required: false

		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(buttonDevice, "button", buttonEvent)
    subscribe(virtB_1_pushed,"momentary.pushed",fakebutton1Event)
    subscribe(virtB_2_pushed,"momentary.pushed",fakebutton2Event)
    subscribe(virtB_3_pushed,"momentary.pushed",fakebutton3Event)
    subscribe(virtB_4_pushed,"momentary.pushed",fakebutton4Event)
    subscribe(virtB_5_pushed,"momentary.pushed",fakebutton5Event)
    subscribe(virtB_6_pushed,"momentary.pushed",fakebutton6Event)
    subscribe(virtB_7_pushed,"momentary.pushed",fakebutton7Event)
    subscribe(virtB_8_pushed,"momentary.pushed",fakebutton8Event)
    subscribe(virtB_9_pushed,"momentary.pushed",fakebutton9Event)
    subscribe(virtB_10_pushed,"momentary.pushed",fakebutton10Event)
    subscribe(virtB_11_pushed,"momentary.pushed",fakebutton11Event)
    subscribe(virtB_12_pushed,"momentary.pushed",fakebutton12Event)
    subscribe(virtB_13_pushed,"momentary.pushed",fakebutton13Event)
    subscribe(virtB_14_pushed,"momentary.pushed",fakebutton14Event)
    subscribe(virtB_15_pushed,"momentary.pushed",fakebutton15Event)
    subscribe(virtB_16_pushed,"momentary.pushed",fakebutton16Event)
    subscribe(virtB_1_held,"momentary.pushed",fakebutton1hEvent)
    subscribe(virtB_2_held,"momentary.pushed",fakebutton2hEvent)
    subscribe(virtB_3_held,"momentary.pushed",fakebutton3hEvent)
    subscribe(virtB_4_held,"momentary.pushed",fakebutton4hEvent)
    subscribe(virtB_5_held,"momentary.pushed",fakebutton5hEvent)
    subscribe(virtB_6_held,"momentary.pushed",fakebutton6hEvent)
	subscribe(virtB_7_held,"momentary.pushed",fakebutton7hEvent)
    subscribe(virtB_8_held,"momentary.pushed",fakebutton8hEvent)
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)|| buttonConfigured(5)|| buttonConfigured(6)|| buttonConfigured(7)|| buttonConfigured(8)
}

def buttonConfigured(idx) {
	return settings["lights_$idx_pushed"] ||
    	settings["lightsDT_$idx_pushed"] ||
        settings["lightsDTVal_$idx_pushed"] ||
        settings["lightOn_$idx_pushed"] ||
    	settings["lightOff_$idx_pushed"] ||
		settings["locks_$idx_pushed"] ||
		settings["speaker_$idx_pushed"] ||
		settings["mode_$idx_pushed"] ||
        settings["notifications_$idx_pushed"] ||
        settings["sirens_$idx_pushed"]
}

def fakebutton1Event(evt) {
    executeHandlers(1, "pushed")
}

def fakebutton2Event(evt) {
    executeHandlers(2, "pushed")
}

def fakebutton3Event(evt) {
    executeHandlers(3, "pushed")
}

def fakebutton4Event(evt) {
    executeHandlers(4, "pushed")
}

def fakebutton5Event(evt) {
    executeHandlers(5, "pushed")
}

def fakebutton6Event(evt) {
    executeHandlers(6, "pushed")
}

def fakebutton7Event(evt) {
    executeHandlers(7, "pushed")
}

def fakebutton8Event(evt) {
    executeHandlers(8, "pushed")
}

def fakebutton9Event(evt) {
    executeHandlers(9, "pushed")
}

def fakebutton10Event(evt) {
    executeHandlers(10, "pushed")
}

def fakebutton11Event(evt) {
    executeHandlers(11, "pushed")
}

def fakebutton12Event(evt) {
    executeHandlers(12, "pushed")
}

def fakebutton13Event(evt) {
    executeHandlers(13, "pushed")
}

def fakebutton14Event(evt) {
    executeHandlers(14, "pushed")
}

def fakebutton15Event(evt) {
    executeHandlers(15, "pushed")
}

def fakebutton16Event(evt) {
    executeHandlers(16, "pushed")
}

def fakebutton1hEvent(evt) {
    executeHandlers(1, "held")
}

def fakebutton2hEvent(evt) {
    executeHandlers(2, "held")
}

def fakebutton3hEvent(evt) {
    executeHandlers(3, "held")
}

def fakebutton4hEvent(evt) {
    executeHandlers(4, "held")
}

def fakebutton5hEvent(evt) {
    executeHandlers(5, "held")
}

def fakebutton6hEvent(evt) {
    executeHandlers(6, "held")
}

def fakebutton7hEvent(evt) {
    executeHandlers(7, "held")
}

def fakebutton8hEvent(evt) {
    executeHandlers(8, "held")
}

def buttonEvent(evt){
	if(allOk) {
		def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
		def value = evt.value
		log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"

		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

		if(recentEvents.size <= 1){
			switch(buttonNumber) {
				case ~/."buttonNumber":1./:
					executeHandlers(1, value)
					break
				case ~/."buttonNumber":2./:
					executeHandlers(2, value)
					break
				case ~/."buttonNumber":3./:
					executeHandlers(3, value)
					break
				case ~/."buttonNumber":4./:
					executeHandlers(4, value)
					break
				case ~/."buttonNumber":5./:
					executeHandlers(5, value)
					break
				case ~/."buttonNumber":6./:
					executeHandlers(6, value)
					break
				case ~/."buttonNumber":7./:
					executeHandlers(7, value)
					break
				case ~/."buttonNumber":8./:
					executeHandlers(8, value)
					break
				case ~/."buttonNumber":9./:
					executeHandlers(9, value)
					break
				case ~/."buttonNumber":10./:
					executeHandlers(10, value)
					break
				case ~/."buttonNumber":11./:
					executeHandlers(11, value)
					break
				case ~/."buttonNumber":12./:
					executeHandlers(12, value)
					break
				case ~/."buttonNumber":13./:
					executeHandlers(13, value)
					break
				case ~/."buttonNumber":14./:
					executeHandlers(14, value)
					break
				case ~/."buttonNumber":15./:
					executeHandlers(15, value)
					break
				case ~/."buttonNumber":16./:
					executeHandlers(16, value)
					break
			}
		} else {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
}

def executeHandlers(buttonNumber, value) {
	log.debug "executeHandlers: $buttonNumber - $value"

	def lights = find('lights', buttonNumber, value)
	if (lights) toggle(lights)

	def lightsDT = find('lightsDT', buttonNumber, value)
	def dimTVal = find('lightsDTVal', buttonNumber, value)
	if (lightsDT) dimToggle(lightsDT, dimTVal)
    
    def lights1 = find('lightOn', buttonNumber, value)
	if (lights1) turnOn(lights1)

	def lights2 = find('lightOff', buttonNumber, value)
	if (lights2) turnOff(lights2)

	def locks = find('locks', buttonNumber, value)
	if (locks) toggle(locks)

	def mode = find('mode', buttonNumber, value)
	if (mode) changeMode(mode)

	def phrase = find('phrase', buttonNumber, value)
	if (phrase) location.helloHome.execute(phrase)

	def sirens = find('sirens', buttonNumber, value)
	if (sirens) toggle(sirens)
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def findMsg(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def turnDim(devices, level) {
	log.debug "turnDim: $devices = ${devices*.currentSwitch}"

	devices.setLevel(level)
}

def toggle(devices) {
	log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

	if (devices*.currentValue('switch').contains('on')) {
		devices.off()
	}
	else if (devices*.currentValue('switch').contains('off')) {
		devices.on()
	}
	else if (devices*.currentValue('lock').contains('locked')) {
		devices.unlock()
	}
	else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
	else {
		devices.on()
	}
}

def turnOn(devices) {
	log.debug "turnOn: $devices = ${devices*.currentSwitch}"

	devices.on()
}

def turnOff(devices) {
	log.debug "turnOff: $devices = ${devices*.currentSwitch}"

	devices.off()
}
def dimToggle(devices, dimLevel) {
	log.debug "dimToggle: $devices = ${devices*.currentValue('switch')}"

	if (devices*.currentValue('switch').contains('on')) devices.off()
	else devices.setLevel(dimLevel)
}


def changeMode(mode) {
	log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

	if (location.mode != mode && location.modes?.find { it.name == mode }) {
		setLocationMode(mode)
	}
}

// execution filter methods
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

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}