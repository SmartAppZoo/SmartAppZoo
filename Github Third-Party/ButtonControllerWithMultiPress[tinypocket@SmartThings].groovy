/**
 *	Button Controller
 *
 *	Author: SmartThings
 *	Date: 2014-5-21
 */
definition(
    name: "Button Controller With Multitap",
    namespace: "nektarios",
    author: "Nektarios",
    description: "Control devices with buttons like the Aeon Labs Minimote, Securifi Key Fob remote.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")

	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: "configureButton1", uninstall: configured()) {
		section {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
		}

		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
	}
}

def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a second button, set it up here",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a third button, you can do even more here",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a fouth button, you rule, and can set it up here",
		install: true, uninstall: true, getButtonSections(4))
}

def getButtonSections(buttonNumber) {
	return {
		section("Lights") {
			input "lights_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lights_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
            input "lights_${buttonNumber}_multiple", "capability.switch", title: "Multiple taps", multiple: true, required: false
		}
		section("Lights - off") {
			input "lightsoff_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightsoff_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
			input "lightsoff_${buttonNumber}_multiple", "capability.switch", title: "Mutliple taps", multiple: true, required: false
		}
		section("Lights - on") {
			input "lightson_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightson_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
            input "lightson_${buttonNumber}_multiple", "capability.switch", title: "Multiple taps", multiple: true, required: false
		}
		section("Locks") {
			input "locks_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "locks_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
            input "locks_${buttonNumber}_multiple", "capability.lock", title: "Multiple taps", multiple: true, required: false
		}
		section("Locks - Lock") {
			input "lockslock_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "lockslock_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
            input "lockslock_${buttonNumber}_multiple", "capability.lock", title: "Multiple taps", multiple: true, required: false
		}
		section("Locks - Unlock") {
			input "locksunlock_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "locksunlock_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
            input "locksunlock_${buttonNumber}_multiple", "capability.lock", title: "Multiple taps", multiple: true, required: false
		}        
		section("Sonos") {
			input "sonos_${buttonNumber}_pushed", "capability.musicPlayer", title: "Pushed", multiple: true, required: false
			input "sonos_${buttonNumber}_held", "capability.musicPlayer", title: "Held", multiple: true, required: false
            input "sonos_${buttonNumber}_multiple", "capability.musicPlayer", title: "Multiple taps", multiple: true, required: false
		}
		section("Modes") {
			input "mode_${buttonNumber}_pushed", "mode", title: "Pushed", required: false
			input "mode_${buttonNumber}_held", "mode", title: "Held", required: false
            input "mode_${buttonNumber}_multiple", "mode", title: "Multiple taps", required: false
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			section("Hello Home Actions") {
				log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "Pushed", required: false, options: phrases
				input "phrase_${buttonNumber}_held", "enum", title: "Held", required: false, options: phrases
                input "phrase_${buttonNumber}_multiple", "mode", title: "Multiple taps", required: false
			}
		}
        section("Sirens") {
            input "sirens_${buttonNumber}_pushed","capability.alarm" ,title: "Pushed", multiple: true, required: false
            input "sirens_${buttonNumber}_held", "capability.alarm", title: "Held", multiple: true, required: false
            input "sirens_${buttonNumber}_multiple", "mode", title: "Multiple taps", required: false
        }

		section("Custom Message") {
			input "textMessage_${buttonNumber}", "text", title: "Message", required: false
		}

        section("Push Notifications") {
            input "notifications_${buttonNumber}_pushed","bool" ,title: "Pushed", required: false, defaultValue: false
            input "notifications_${buttonNumber}_held", "bool", title: "Held", required: false, defaultValue: false
            input "notification_${buttonNumber}_multiple", "mode", title: "Multiple taps", required: false
        }

        section("Sms Notifications") {
            input "phone_${buttonNumber}_pushed","phone" ,title: "Pushed", required: false
            input "phone_${buttonNumber}_held", "phone", title: "Held", required: false
            input "phone_${buttonNumber}_multiple", "mode", title: "Multiple taps", required: false
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
	subscribe(buttonDevice, "button", buttonEvent, [filterEvents: false])
    
    state.buttonEvent = [:]
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)
}

def buttonConfigured(idx) {
	return settings["lights_$idx_pushed"] ||
		settings["locks_$idx_pushed"] ||
		settings["sonos_$idx_pushed"] ||
		settings["mode_$idx_pushed"] ||
        settings["notifications_$idx_pushed"] ||
        settings["sirens_$idx_pushed"] ||
        settings["notifications_$idx_pushed"]   ||
        settings["phone_$idx_pushed"]
}

def buttonEvent(evt){
	if(allOk) {
		def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
		def value = evt.value
		log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"

		//def recentEvents = buttonDevice.eventsSince(new Date(evt.date.getTime() - 2000)).findAll{it.value == evt.value && it.data == evt.data}
		//log.debug "Found ${recentEvents.size()?:0} events in past 2 seconds"

		def btn=0;

		//can't just read the button number for some reason so need to do the wierd switch statement
		switch(buttonNumber) {
			case ~/.*1.*/:
				btn = 1
                break
			case ~/.*2.*/:
				btn = 2
				break
			case ~/.*3.*/:
				btn = 3
				break
			case ~/.*4.*/:
				btn = 4
				break
		}

		//store these in state to use runIn
        state.buttonEvent["${btn}_event.value"] = evt.value 
        state.buttonEvent["${btn}_event.data"] = evt.data 
        state.buttonEvent["${btn}_event.datemilliseconds"] = evt.date.getTime() 
        //runIn(1, "buttonHandler${btn}")
        
        //comment out runIn for now
        buttonHandler(btn)


//		if(recentEvents.size >= 2){
//			switch(buttonNumber) {
//				case ~/.*1.*/:
//					//executeHandlers(1, value)
//                  state.buttonEvent["1_event.value"] = evt.value 
//                    state.buttonEvent["1_event.data"] = evt.data 
//                    runIn(2, "buttonHandler1")
//					break
//				case ~/.*2.*/:
//					executeHandlers(2, value)
//					break
//				case ~/.*3.*/:
//					executeHandlers(3, value)
//					break
//				case ~/.*4.*/:
//					executeHandlers(4, value)
//					break
//			}
//		} else {
//			log.debug "Found recent button press events for $buttonNumber with value $value"
//		}
	}
}

def buttonHandler1() {
	buttonHandler(1)
}
def buttonHandler2() {
	buttonHandler(2)
} 
def buttonHandler3() {
	buttonHandler(3)
}
def buttonHandler4() {
	buttonHandler(4)
}


def buttonHandler(buttonNumber) {
	def eventValue = state.buttonEvent["${buttonNumber}_event.value"]
    def eventData = state.buttonEvent["${buttonNumber}_event.data"]
    def eventDateMilliseconds = state.buttonEvent["${buttonNumber}_event.datemilliseconds"]

eventDateMilliseconds=now()

	def recentEvents = buttonDevice.eventsSince(new Date(eventDateMilliseconds - 1000), [all:true, max: 5]).findAll{it.value == eventValue && it.data == eventData}
	log.debug "(Delayed check) Button ${buttonNumber} check: found ${recentEvents.size()?:0} events in past 2 seconds"

	if(recentEvents.size == 1){
    	executeHandlers(buttonNumber, eventValue)
    }
    else if(recentEvents.size >= 2){
    	executeHandlers(buttonNumber, "multiple")
    } else {
        log.debug "(Delayed check) Found recent button press events for $buttonNumber with value $value"
    }

    		
}


def executeHandlers(buttonNumber, value) {
	log.debug "executeHandlers: $buttonNumber - $value"

	def lights = find('lights', buttonNumber, value)
	if (lights != null) toggle(lights)

	def lightson = find('lightson', buttonNumber, value)
	if (lightson != null) on(lightson)
    
    def lightsoff = find('lightsoff', buttonNumber, value)
	if (lightsoff != null) off(lightsoff)

	def locks = find('locks', buttonNumber, value)
	if (locks != null) toggle(locks)

	def lockson = find('lockslock', buttonNumber, value)
	if (lockson != null) on(lockson)
    
    def locksoff = find('locksunlock', buttonNumber, value)
	if (locksoff != null) off(locksoff)

	def sonos = find('sonos', buttonNumber, value)
	if (sonos != null) toggle(sonos)

	def mode = find('mode', buttonNumber, value)
	if (mode != null) changeMode(mode)

	def phrase = find('phrase', buttonNumber, value)
	if (phrase != null) location.helloHome.execute(phrase)

	def textMessage = findMsg('textMessage', buttonNumber)

	def notifications = find('notifications', buttonNumber, value)
	if (notifications?.toBoolean()) sendPush(textMessage ?: "Button $buttonNumber was pressed" )

	def phone = find('phone', buttonNumber, value)
	if (phone != null) sendSms(phone, textMessage ?:"Button $buttonNumber was pressed")

    def sirens = find('sirens', buttonNumber, value)
    if (sirens != null) toggle(sirens)
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def findMsg(type, buttonNumber) {
	def preferenceName = type + "_" + buttonNumber
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def toggle(devices) {
	log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

	if (devices*.currentValue('lock').contains('locked')) {
		devices.unlock()
	}
	else if (devices*.currentValue('switch').contains('on')) {
		devices.off()
	}
	else if (devices*.currentValue('switch').contains('off')) {
		devices.on()
	}
	else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
	else {
		devices.on()
	}
}

def on(devices) {
	log.debug "on: $devices = ${devices.findAll{it.supportedCommands.findAll{it && it.name == 'lock'}.size()>0}.size()>0}"

	if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'lock'}.size()>0}.size()>0) {
    	devices.lock()
    }
    else if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'siren'}.size()>0}.size()>0) {
    	devices.siren()
    }
    else if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'on'}.size()>0}.size()>0) {
    	devices.on()
    }
	/*if (devices*.currentValue('lock')) {
		devices.lock()
	}
	else if (devices*.currentValue('switch')) {
		devices.on()
	}
	else if (devices*.currentValue('alarm')) {
        devices.siren()
    }
	else {
		devices.on()
	}*/
}

def off(devices) {
	log.debug "off: $devices = ${devices.findAll{it.supportedCommands.findAll{it && it.name == 'unlock'}.size()>0}.size()>0}"

	if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'unlock'}.size()>0}.size()>0) {
    	devices.unlock()
    }
    else if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'siren'}.size()>0}.size()>0) {
    	devices.siren()
    }
    else if (devices.findAll{it.supportedCommands.findAll{it && it.name == 'off'}.size()>0}.size()>0) {
    	devices.off()
    }

	/*
	if (devices*.currentValue('lock')) {
		devices.unlock()
	}
	else if (devices*.currentValue('switch')) {
		devices.off()
	}
	else if (devices*.currentValue('alarm')) {
        devices.siren()
    }
	else {
		devices.off()
	}*/
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