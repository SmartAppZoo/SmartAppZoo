/**
 *  Double Tap for Hue
 *
 *  Author: John Gorsica
 */
definition(
    name: "Double Tap for Hue",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Change lights to a certain color when a switch is double tapped on.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("When this switch is double-tapped...") {
		input "master", "capability.switch", title: "Where?"
	}
	section("Set these hue bulbs...") {
		input "hues", "capability.colorControl", multiple: true, required: true
        input "hue", "number", title: "Hue?"
        input "saturation", "number", title: "Saturation?"
        input "level", "number", title: "Level?"
	}
}

def installed()
{
	log.trace "installed()"
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
	log.trace "updated()"
	unsubscribe()
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	log.info "switchHandler($evt.name: $evt.value, type: '${evt.event.type}', isPhysical: ${evt.isPhysical()} (${evt.event.isPhysical()}), isDigital: ${evt.isDigital()})"
	log.info evt.event.encodeAsJSON()
    
	// use Event rather than DeviceState because we may be changing DeviceState to only store changed values
    def recentStates = master.statesSince("switch", new Date(now() - 8000))
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

	if (evt.isPhysical()) {
		if (evt.value == "on" && recentStates?.size()>1) {
			log.debug "detected two taps, set colors."
			setColors()
        }
	}
	else {
		log.trace "Skipping digital on/off event"
	}
}

def setColors() {
	def newValue = [hue: hue, saturation: saturation, level: level]
    log.debug "new value = $newValue"
    hues*.setColor(newValue)
}
