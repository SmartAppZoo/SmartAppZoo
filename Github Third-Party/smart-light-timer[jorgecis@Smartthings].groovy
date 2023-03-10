/**
 *  Smart Timer
 */

definition(
    name: "Smart Light Timer",
    namespace:   "jorgeci",
    author:      "jorgecis@gmail.com",
    description: "Turns off a switch in X minutes, when mode change to Night",
    category: "Convenience",
    iconUrl: "http://upload.wikimedia.org/wikipedia/commons/6/6a/Light_bulb_icon_tips.svg",
    iconX2Url: "http://upload.wikimedia.org/wikipedia/commons/6/6a/Light_bulb_icon_tips.svg")

preferences {
	section("And off after no more triggers after..."){
		input "minutes1", "number", title: "Minutes?", defaultValue: "5"
	}
	section("Turn on/off light(s)..."){
		input "switches", "capability.switch", multiple: true, title: "Select Lights"
	}
}

def installed()
{
	subscribe(location, changeMode)
}

def changeMode(evt) {
	log.debug "Mode: " + location.mode
	if("Night" == location.mode) {
		runIn(minutes1 * 60, switchoff)
	    log.debug "Trigger in: " + minutes1
	}
}

def updated()
{
	unsubscribe()
    subscribe(location, changeMode)
}

def switchoff() 
{
	switches.off()
}