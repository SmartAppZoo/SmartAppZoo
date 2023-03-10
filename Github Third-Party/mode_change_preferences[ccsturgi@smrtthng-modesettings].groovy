/**
 *  Mode Changes, Settings Change 
 *
 *  Author: Chris Sturgis
 *  Date: 6/21/14
 *
 *  Triggers changes to nest home/away mode based on a mode setting. Will also trigger switches.
 */

definition(
    name:        "Mode Changes, Settings Change",
    namespace:   "",
    author:      "Chris Sturgis",
    description: "Sets up preferences for a certain mode. This includeds individual nest home/away settings",
    category:    "Green Living",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("When changing to this mode") {
    input ("newMode", "mode")
  }
  section("Set these thermostats to Home/Away mode") {
    	input "thermostats",
        	"capability.thermostat",
        	multiple: true,
        	title: "Thermostat(s)?"
		input "thermode","enum",
        	title: "Home or Away?",
            metadata: [ values: [
				       'Home',
				       'Away']]
	}
    section("Turn these switches On or Off") {
    	input"onSwitches", "capability.switch",
        	title: "Switches to Turn On",
            required: false,
            multiple: true
        input "offSwitches", "capability.switch",
        	title: "Switches to Turn Off",
            required: false,
            multiple: true
       }
  }
def installed() {
	log.debug "Installed with settings: ${settings}"
    log.debug "Current mode: $location.mode"
    subscribe(location ,changeMode)
	subscribe(app, changeMode)
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
    log.debug "Current mode: $location.mode"
	unsubscribe()
    subscribe(location ,changeMode)
	subscribe(app, changeMode)
}
def changeMode(evt) {
  log.debug "Mode changed to $evt.value"
  if(evt.value == newMode) {
    homeAway()
  }

  else {
  	if(evt.value != newMode)
    log.debug("No changes")
  }
}
def homeAway() {
	if(thermode == "Home"){
    	thermostats?.present()
        changeSwitches()
        log.debug "${thermostats} set to Home"
        }
    else {
    	if(thermode == "Away"){
        thermostats?.away()
        changeSwitches()
        log.debug "${thermostats} set to Away" }
    	}
    }
def changeSwitches() {
	onSwitches.on()
    offSwitches.off()
    log.debug "${onSwitches} turned On, ${offSwitches} turned Off"
    }
