/********

todo:
1) DONE 12/11/2015: move thermostat to parent
2) DONE 12/20/2015: figure out how to provide default name of child smartapp
3) DONE 12/20/2015: Figure out how to hide child apps from mobile app - don't publish the child app, just the parent
4) DONE 12/31/2015: Convert from runOnce to schedule() using crontab syntax - hoping this will solve re-scheduling problem.  It will also
   obviate the need for computing the next date based on day of week
*/

definition(
	name: "New Brighter Thermostat Control",
	namespace: "trentfoley64",
	author: "A. Trent Foley, Sr.",
	description: "Child app for Brighter Thermostat Controls.",
	category: "My Apps",
	parent: "trentfoley64:Brighter Thermostat Controls",
	iconUrl: "http://www.trentfoley.com/ST/icons/thermostat.png",
	iconX2Url: "http://www.trentfoley.com/ST/icons/thermostat@2x.png",
	iconX3Url: "http://www.trentfoley.com/ST/icons/thermostat@3x.png",
)

preferences {
	page name: "schedulePage", title: "Brighter Thermostat Control", install: false, uninstall: true, nextPage: "namePage"
	page name: "namePage", title: "Brighter Thermostat Control", install: true, uninstall: true
}

def schedulePage() {
	dynamicPage(name: "schedulePage") {
		// Let user pick set points
		section("To these set points") {
			input "heatSetpoint", "decimal", title: "for Heating", default:70
			input "coolSetpoint", "decimal", title: "for Cooling", default:80
		}
		// Let user pick which days of week
		section("for Days of Week") {
			input "daysOfWeekList", "enum", title: "Which days?", required: true, multiple: true,
				options: ['Monday','Tueday','Wednesday','Thursday','Friday','Saturday','Sunday']
		}
		// Let user specify Time of day
		section("Time of day") {
			input "timeOfDay", "time", title: "At this time of day"
		}
		// Let user specify presence rules
		section( "Presences") {
			input "anyMustBePresent", "capability.presenceSensor", title: "At least one must be present", multiple: true, required: false
			input "allMustBePresent", "capability.presenceSensor", title: "All must be present", multiple: true, required: false
			input "anyMustBeAbsent", "capability.presenceSensor", title: "At least one must be absent", multiple: true, required: false
			input "allMustBeAbsent", "capability.presenceSensor", title: "All must be absent", multiple: true, required: false
		}
		// Let user specify notification recipients
		section( "Notifications" ) {
			input "sendPushMessage", "enum", title: "Send a push notification?", options:["Yes", "No"], required: false, default: "No"
			input "sendSMSNumber", "phone", title: "Send a text message to this number:", required: false
		}
	}
}

def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        app.updateLabel(defaultLabel())
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def installed() {
	log.debug "${app.label}: Installed with $settings"
	initialize()
}

def updated() {
	log.debug "${app.label}: Updated with $settings"
	initialize()
}

def initialize() {
	// build crontab from timeOfDay and daysOfWeekList
    def crontab=buildCronTab()
    // debug messages:
    def msg="${app.label}: schedule $crontab"
    log.debug msg
    sendNotificationEvent msg
    // schedule the thermostat control based on crontab
    schedule(crontab,runThermostatControl)
}

def buildCronTab() {
	// make date object from timeOfDay
	def cronDate=timeToday(timeOfDay,location.timeZone)
    // extract minute and hour from time, adjusted for timeZone
    def cronMinute=cronDate.format("mm",location.timeZone)
    def cronHour=cronDate.format("HH",location.timeZone)
    // build list of days of weeks separated by comma with no spaces
    def cronDayOfWeekList=""
    def cronComma=""
    for(def i=0;i<daysOfWeekList.size();i++) {
    	cronDayOfWeekList=cronDayOfWeekList+cronComma+daysOfWeekList[i][0..2].toUpperCase()
        cronComma=","
    }
    // crontab expects:  <hour> <minute> <hour> <dayOfMonth> <month> <daysOfWeek> [<year>]
    return "0 $cronMinute $cronHour ? * $cronDayOfWeekList"
}

def defaultLabel() {
    def msg=""
    if (anyMustBePresent) {
    	msg=msg?"$msg, and ":"" + "any of ${anyMustBePresent} are present"
	}
    if (allMustBePresent) {
    	msg=msg?"$msg, and ":"" + "all of ${allMustBePresent} are present"
    }
    if (anyMustBeAbsent) {
    	msg=msg?"$msg, and ":"" + "any of ${anyMustBeAbsent} are absent"
    }
    if (allMustBeAbsent) {
    	msg=msg?"$msg, and ":"" + "all of ${allMustBeAbsent} are absent"
    }
	"Set ${parent.thermostats} to ${heatSetpoint}/${coolSetpoint} at " +
    timeOfDay.format("HH:mm z", location.timeZone) + " on ($daysOfWeekList)" + msg?" when $msg":""
}

def runThermostatControl() {
	// trying to debug why events aren't rescheduled
	def msg="${app.label}: runThermostatControl at " + new Date(now()).format("EEE MMM dd yyyy HH:mm z", location.timeZone)
    log.debug msg
    sendNotificationEvent msg
 	// Check presences
 	def passedChecks=checkPresences()
	// If we have hit the conditions to execute this then lets do it
	if (passedChecks) {
		msg="${parent.thermostats} heat setpoint to '${heatSetpoint}' and cool setpoint to '${coolSetpoint}'"
		log.debug "${app.label}: $msg"
        sendNotificationEvent "${app.label}: $msg"
        // do the actual thermostat change
		parent.thermostats.setHeatingSetpoint(heatSetpoint)
		parent.thermostats.setCoolingSetpoint(coolSetpoint)
        parent.thermostats.setThermostatMode("auto")
        parent.thermostats.setThermostatFanMode("auto")
        // send any push / notification messages
		sendMessage msg
	}
}

private checkPresences() {
	def msg=""
	// If defined, check anyMustBePresent
	if (anyMustBePresent) {
		// If anyMustBePresent does not contain anyone present, do not change thermostats
		if (!anyMustBePresent.currentValue('presence').contains('present')) {
			msg="${app.label}: change cancelled due to all of ${anyMustBePresent} being absent."
			log.debug msg
        	sendNotificationEvent msg
			return false
		}
	}
	// If defined, check allMustBePresent
	if (allMustBePresent) {
		// If allMustBePresent contains anyone not present, do not change thermostats
		if (allMustBePresent.currentValue('presence').contains('not present')) {
			msg="${app.label}: cancelled due to one of ${allMustBePresent} being absent."
			log.debug msg
        	sendNotificationEvent msg
			return false
		}
	}
	// If defined, check anyMustBeAbsent
	if (anyMustBeAbsent) {
		// If anyMustBeAbsent does not contain anyone not present, do not change thermostats
		if (!anyMustBeAbsent.currentValue('presence').contains('not present')) {
			msg="${app.label}: cancelled due to all of ${anyMustBeAbsent} being present."
			log.debug msg
        	sendNotificationEvent msg
			return false
		}
	}
	// If defined, check allMustBeAbsent
	if (allMustBeAbsent) {
		// If allMustBeAbsent contains anyone present, do not change thermostats
		if (allMustBeAbsent.currentValue('presence').contains('present')) {
			msg="${app.label}: cancelled due to one of ${allMustBeAbsent} being present."
			log.debug msg
        	sendNotificationEvent msg
			return false
		}
	}
    // If we've gotten to here, all checks have passed
	true
} 

private sendMessage(msg) {
	// If user specified sending a push message, do so
	if (sendPushMessage == "Yes") {
		sendPush(msg)
	}
    // If user supplied a phone number, send an SMS
	if (sendSMSNumber) {
		sendSms(sendSMSNumber,msg)
	}
}