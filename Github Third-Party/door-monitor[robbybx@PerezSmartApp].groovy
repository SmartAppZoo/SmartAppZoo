/**
 *  Door Monitor
 *  Based off Siren Beep, Credit: https://raw.githubusercontent.com/KristopherKubicki/smartapp-beep/master/smartapp-beep.groovy
 */
definition(
    name: "Door Monitor",
    namespace: "ajesseperez",
    author: "ajesse.perez@gmail.com",
    description: "Quickly Pulse a Siren When a Door is opened based on if your security system is armed.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan@2x.png")

preferences {
	section("Sirens"){
        input "sirens", "capability.alarm", title: "Which Siren?", required: false, multiple: true
    }
    section("Virtual Switch"){
        input "ExtContacts", "capability.contactSensor", title: "Which External Contacts?", required: false, multiple: true
    }
    section("Virtual Switch"){
        input "IntContacts", "capability.contactSensor", title: "Which Internal Contacts?", required: false, multiple: true
    }
}

def installed() {
   log.debug "Installed with settings: ${settings}"
   initialized()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialized()
}

def initialized() {
    subscribe(IntContacts, "contact", IntContactHandler)
    subscribe(ExtContacts, "contact", ExtContactHandler)
    subscribe(sirens, "alarm", sirenHandler)
    subscribe(location, "alarmSystemStatus", alarmStatusHandler)
    state.siren = false
    state.alarm = "off"
}

def alarmStatusHandler(evt) {
    log.debug "alarm status changed to: ${evt.value}"
    if ("away" == evt.value) {
    	state.alarm = "away"
    	log.debug "Alarm Set to Away"
    }
    else if ("stay" == evt.value) {
    	state.alarm = "stay"
        log.debug "Alarm Set to Stay"
    }
    else if ("off" == evt.value) {
    	state.alarm = "off"
        log.debug "Alarm Set to Off"
    }
}

def sirenHandler(evt) {
	log.debug "Siren Event"
    
    if("both" == evt.value) {
        log.debug "Alarm Both Active"
        state.siren = true
        log.debug "Alarm.Active = True"
	}
    else if ("siren" == evt.value) {
        log.debug "Alarm Siren Active"
        state.siren = true
        log.debug "Alarm.Active = True"
	}
    else if ("strobe" == evt.value) {
        log.debug "Alarm Strobe Active"
        state.siren = false
        log.debug "Alarm.Active = false"
    }
	else if ("off" == evt.value) {
        log.debug "Alarm Off"
        state.siren = false
        log.debug "Alarm.Active = False"
    }
}

def IntContactHandler(evt) {
	log.debug "Int Contact Check"
	if (state.alarm == "off"){
		if("open" == evt.value) {
			log.debug "Internal Door Open, Beep Siren"
			BeepSiren()
		}
	}
	else if (state.alarm == "stay") {
		if (state.siren == false) {
			if("open" == evt.value) {
				log.debug "Internal Door Open, Beep Siren"
				BeepSiren()
			}
		}
	}
}

def ExtContactHandler(evt) {
	log.debug "Ext Contact Check"
	if (state.alarm == "off") {
		log.debug "Siren not Active and Alarm is Off: Good to Beep"
		if("open" == evt.value) {
			log.debug "External Door Open, Beep Siren"
			BeepSiren()
		}
	}
}
	
def BeepSiren() {
    Short duration = 0
    sirens?.chime(duration)
}
