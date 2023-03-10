/**
 *	Leviton VRCS Scene Controller 
 *
 *	Author: Iskender Eguz
 *	Date: 2016-11-26
 *  
 *  Special thanks to:
 *    - Brian Dahlem, for ZWave Scene Control and Button Controller, that serves as the basis of this code
 *    - Statusbits.com, Dim and Dimmer (2014-12-21), that had a framework for defining scenes
 * 
 *  Note: You need to have Brian Dahlem's device handler for Leviton installed before using this application.  
 * 
 */

definition(
    name: "Leviton VRCS Scene Controller",
    namespace: "ieguz",
    author: "Iskender Eguz",
    description: "Control light scenes including dimmers with buttons like Leviton VRCS4",
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
	page(name: "configureDimLevels1")
	page(name: "configureDimLevels2")
	page(name: "configureDimLevels3")
	page(name: "configureDimLevels4")
    page(name: "pageAbout")
}

def selectButton() {

    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
        return pageAbout()
    }
    
	dynamicPage(name: "selectButton", title: "First, select your zwave controller", nextPage: "configureButton1", uninstall: configured()) {
		section {
			input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
		}
        section ("If there is a built in switch, what device is it?") {
        	input "relayDevice", "capability.switch", title: "Built in switch", multiple: false, required: false
            input "relayAssociate", "bool", title: "Use switch 1 to control relay?"
        }
		
		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
			
			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}

 		if (state.installed) {
      		section {
        		href(name: "toconfigureButton1", page: "configureButton1", title: "Configure Button 1")
        		href(name: "toconfigureButton2", page: "configureButton2", title: "Configure Button 2")
        		href(name: "toconfigureButton3", page: "configureButton3", title: "Configure Button 3")
        		href(name: "toconfigureButton4", page: "configureButton4", title: "Configure Button 4")
      		}	
        }

      	section {
        	label(title: "Label this SmartApp", required: false, defaultValue: "VRCS-ES Controller")
      		href(name: "topageAbout", page: "pageAbout", title: "About This Smart App...")
      	}  

	}
}

def configureButton1() {
	if (relayAssociate == true) {
    	dynamicPage(name: "configureButton1", title: "The first button will control ${relayDevice.displayName}",
			nextPage: "configureButton2", uninstall: configured(), null)
    }
    else {
		dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button...",
			nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
    }
}

def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a second button, what do you want it to do?",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a third button, what do you want it to do?",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a fouth button, what do you want it to do?",
		install: true, uninstall: true, getButtonSections(4))
}

def configureDimLevels1() {
	dynamicPage(name: "configureDimLevels1", title: "For the first button scene, select switch on/off or light level (0-99).",
		nextPage: "configureButton1", uninstall: configured(), getDimmingLevels(1))
}

def configureDimLevels2() {
	dynamicPage(name: "configureDimLevels2", title: "For the second button scene, select switch on/off or light level (0-99).",
		nextPage: "configureButton2", uninstall: configured(), getDimmingLevels(2))
}

def configureDimLevels3() {
	dynamicPage(name: "configureDimLevels3", title: "For the third button scene, select switch on/off or light level (0-99).",
		nextPage: "configureButton3", uninstall: configured(), getDimmingLevels(3))
}

def configureDimLevels4() {
	dynamicPage(name: "configureDimLevels4", title: "For the fourth button scene, select switch on/off or light level (0-99).",
		nextPage: "configureButton4", uninstall: configured(), getDimmingLevels(4))
}


// Configure each button actions
def getButtonSections(buttonNumber) {
	return {
    	section(title: "Light Scene:", hidden: hideSection(buttonNumber, "scene"), hideable: true) {
			input "lights_${buttonNumber}_scene", "capability.switch", title: "Select Lights for Scene:", multiple: true, required: false
			input "lights_${buttonNumber}_scene_toggle", "enum", title: "Toggle lights off on 2nd press?", defaultValue: "Yes", required: false, options: ["Yes", "No"]
       		href(name: "toconfigureDimLevels${buttonNumber}", page: "configureDimLevels${buttonNumber}", title: "Configure Dimming Levels")
		}      
		section(title: "Turn on these...", hidden: hideSection(buttonNumber, "on"), hideable: true) {
			input "lights_${buttonNumber}_on", "capability.switch", title: "Switches:", multiple: true, required: false
		}      
    	section(title: "Turn off these...", hidden: hideSection(buttonNumber, "off"), hideable: true) {
			input "lights_${buttonNumber}_off", "capability.switch", title: "Switches:", multiple: true, required: false
		}
		section(title: "Toggle these...", hidden: hideSection(buttonNumber, "toggle"), hideable: true) {
			input "lights_${buttonNumber}_toggle", "capability.switch", title: "Switches:", multiple: true, required: false
		}
    	section(title: "Sonos:", hidden: hideSonosSection(buttonNumber), hideable: true) {
			input "sonos_${buttonNumber}_toggle", "capability.musicPlayer", title: "Music players to toggle:", multiple: true, required: false
			input "sonos_${buttonNumber}_on", "capability.musicPlayer", title: "Music players to turn on:", multiple: true, required: false
			input "sonos_${buttonNumber}_off", "capability.musicPlayer", title: "Music players to turn off:", multiple: true, required: false
		}
		section(title: "Locks:", hidden: hideLocksSection(buttonNumber), hideable: true) {		
			input "locks_${buttonNumber}_toggle", "capability.lock", title: "Locks to toggle:", multiple: true, required: false
			input "locks_${buttonNumber}_unlock", "capability.lock", title: "Locks to unlock:", multiple: true, required: false
			input "locks_${buttonNumber}_lock", "capability.lock", title: "Locks to lock:", multiple: true, required: false
		}

		section("Modes") {
			input "mode_${buttonNumber}_on", "mode", title: "Activate these modes:", required: false
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			section("Hello Home Actions") {
				log.trace phrases
				input "phrase_${buttonNumber}_on", "enum", title: "Activate these phrases:", required: false, options: phrases
			}
		}
	}
}

// Get scene details if/when needed
def getDimmingLevels(buttonNumber) {
   	def scenelights = find('lights', buttonNumber, "scene")
	if (scenelights != null) {
	    return {
			section {
				scenelights?.each() {
					if (it.capabilities.name.contains("Switch Level"))  {
						input "lights_${buttonNumber}_scenesetting_${it.id}", "number", title:"${it.displayName} (0 to 99 Dim Level)", required:true
                    } else {
                        input "lights_${buttonNumber}_scenesetting_${it.id}", "enum", title:"${it.displayName} (On / Off)", metadata:[values: ["on", "off"]], required: true
                    }
				} 
			}
		}
	}
	section {
		href(name: "toconfigureButton${buttonNumber}", page: "configureButton${buttonNumber}", title: "Back to button ${buttonNumber} configuration...")
	}
}



def installed() {
	initialize()
    state.installed = true
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	
	subscribe(buttonDevice, "button", buttonEvent)
    
    if (relayDevice) {
        log.debug "Associating ${relayDevice.deviceNetworkId}"
        if (relayAssociate == true) {
            buttonDevice.associateLoad(relayDevice.deviceNetworkId)
        }
        else {
            buttonDevice.associateLoad(0)
        }
    }
    
    state.last_active_scene = "N/A"

}

def configured() {
	return  buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)
}

def buttonConfigured(idx) {
	return settings["lights_$idx_toggle"] ||
		settings["locks_$idx_toggle"] ||
		settings["sonos_$idx_toggle"] ||
        settings["lights_$idx_on"] ||
		settings["sonos_$idx_on"] ||
        settings["lights_$idx_scene"] ||
		settings["lights_$idx_off"] ||
		settings["sonos_$idx_off"] ||
        settings["locks_$idx_lock"] ||
        settings["locks_$idx_unlock"] ||
		settings["mode_$idx_on"] ||
        settings["phrase_$idx_on"]
}

def buttonEvent(evt){
	log.debug "buttonEvent"
	if(allOk) {
		def buttonNumber = evt.value // why doesn't jsonData work? always returning [:]
		log.debug "buttonEvent: $evt.name - ($evt.data)"
		log.debug "button: $buttonNumber"
	
		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"
        
		if(recentEvents.size <= 1){
			switch(buttonNumber) {
				case ~/.*1.*/:
					executeHandlers(1)
					break
				case ~/.*2.*/:
					executeHandlers(2)
                    break
				case ~/.*3.*/:
					executeHandlers(3)
					break
				case ~/.*4.*/:
					executeHandlers(4)
					break
			}
		} else {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
    else {
    	log.debug "NotOK"
    }
}


def executeHandlers(buttonNumber) {
	log.debug "executeHandlers: $buttonNumber"

	def lights = find('lights', buttonNumber, "scene")
	if (lights != null) setscene(lights, buttonNumber)

	lights = find('lights', buttonNumber, "toggle")
	if (lights != null) toggle(lights)

	lights = find('lights', buttonNumber, "on")
	if (lights != null) flip(lights, "on")

	lights = find('lights', buttonNumber, "off")
	if (lights != null) flip(lights, "off")

	def locks = find('locks', buttonNumber, "toggle")
	if (locks != null) toggle(locks)

	locks = find('locks', buttonNumber, "unlock")
	if (locks != null) flip(locks, "unlock")

	locks = find('locks', buttonNumber, "lock")
	if (locks != null) flip(locks, "lock")

	def sonos = find('sonos', buttonNumber, "toggle")
	if (sonos != null) toggle(sonos)

	sonos = find('sonos', buttonNumber, "on")
	if (sonos != null) flip(sonos, "on")

	sonos = find('sonos', buttonNumber, "off")
	if (sonos != null) flip(sonos, "off")
    
	def mode = find('mode', buttonNumber, "on")
	if (mode != null) changeMode(mode)

	def phrase = find('phrase', buttonNumber, "on")
	if (phrase != null) location.helloHome.execute(phrase)
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def flip(devices, newState) {
	log.debug "flip: $devices = ${devices*.currentValue('switch')}"
	if (newState == "off") {
		devices.off()
	}
	else if (newState == "on") {
		devices.on()
	}
	else if (newState == "unlock") {
		devices.unlock()
	}
	else if (newState == "lock") {
		devices.lock()
	}
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
	else if (devices*.currentValue('lock').contains('unlocked')) {
		devices.lock()
	}
	else {
		devices.on()
	}
}

def setscene(devices, buttonNumber) {
	log.debug "setting scene: $devices = ${devices*.currentValue('switch')}"
	def toggle_scene = settings["lights_${buttonNumber}_scene_toggle"]
	def already_active_scene = state.last_active_scene
	// log.debug "setting scene: toggle flag is... $toggle_scene"
	// log.debug "setting scene: aleady active scene is... $already_active_scene"
	
	if ((toggle_scene == "Yes") && (already_active_scene == buttonNumber)) {
		log.debug "setting scene: Turning off all lights for toggle"
        state.last_active_scene = "N/A"
		devices.off()
    } else {
        state.last_active_scene = buttonNumber
		devices?.each {
	    	def value = settings["lights_${buttonNumber}_scenesetting_${it.id}"]
		    log.debug "setting scene: ${it.name} set for $value"
			switch (value) {
    			case "on":
        			it.on()
        		break
    			case "off":
        			it.off()
        		break
    			default:
					// value = value.toInteger()
        			if (value > 99) value = 99
					it.setLevel(value)
				}
			}
	}
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

private hideSection(buttonNumber, action) {
	(find("lights", buttonNumber, action) || find("locks", buttonNumber, action) || find("sonos", buttonNumber, action)) ? false : true
}

private hideLocksSection(buttonNumber) {
	(find("locks", buttonNumber, "lock") || find("locks", buttonNumber, "unlock") || find("locks", buttonNumber, "toggle")) ? false : true
}

private hideSonosSection(buttonNumber) {
	(find("sonos", buttonNumber, "on") || find("sonos", buttonNumber, "off") || find("sonos", buttonNumber, "toggle")) ? false : true
}

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private integer(String s) {
	return Integer.parseInt(s)
}



// Show "About" page
private def pageAbout() {

    def textAbout =
        "This smart app allows you to define and trigger light scenes using Leviton VRCS4 like buttons. " +
        "Light scenes for each button can be defined with dimmers set to different levels."

    def pageProperties = [
        name        : "pageAbout",
        title       : "About",
        nextPage    : "selectButton",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
            paragraph "${textVersion()}\n${textCopyright()}"
        }
        section("License") {
            paragraph textLicense()
        }
    }
}


private def textVersion() {
    def text = "Version 1.0.0"
}

private def textCopyright() {
    def text = "Copyright (c) 2016"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}