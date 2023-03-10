/**
 *  There Can Be Only One
 *
 *  Author: Justin Nale
 *  Based on Mirror Switch by Jason E
 */
 
definition(
    name: "There Can Be Only One",
    namespace: "JustinNale",
    author: "Justin Nale",
    description: "One switch on, all others off",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

 
  
 
 
 
 
preferences {
	section("When this switch is used...") {
		input "master", "capability.switch", title: "Where?"
	}
    
	section("Turn off all of these switches") {
		input "offSwitches", "device.onOffButtonTile", multiple: true, required: false
	}
}

def installed()
{
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
	unsubscribe()
	subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) {
	log.info evt.value
	def recentStates = master.statesSince("switch", new Date(now() - 6000))
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"

	if (evt.value == "on") {
		log.debug "detected on, turn off other light(s)"
		offSwitches?.off()
	} 
}

//private onSwitches() {
//	switches + onSwitches
//}

//private offSwitches() {
//	switches + offSwitches
//}

private lastTwoStatesWere(value, states) {
	// first get a list of the only the on and off events, so that wakeup events don't mess us up
	log.debug "UNFILTERED: [${states.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
	def onOff = states.findAll { it.isPhysical() || !it.type }
	log.debug "FILTERED:   [${onOff.collect{it.dateCreated + ':' + it.value}.join(', ')}]"
	onOff.size() > 1 && onOff[0].value == value && onOff[1].value == value
}