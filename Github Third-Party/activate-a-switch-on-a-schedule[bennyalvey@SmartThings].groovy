/**
 *  Activate A Switch On A Schedule 
 *
 *  Author: SmartThings
 */
preferences {
	section("Turn on/off a switch...") {
		input "switch1", "capability.switch"
	}
	section("At what time?") {
		input "cron1", "cron", title: "When?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(cron1, "scheduleCheck")
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	schedule(cron1, "scheduleCheck")
}

def scheduleCheck()
{
	log.debug "scheduledCheck: $settings"
	def latestValue = switch1.currentSwitch

    if(latestValue == "off") {
		log.debug "Switch is currently off, switching on"
		switch1.on()
    } else if(latestValue == "on") {
		log.debug "Switch is currently on, switching off"
		switch1.off()
    } else {
		log.debug "Latest value is not on or off, by default turn switch off. Value: $latestValue"
		switch1.off()
    }
}