/**
 *  dim-with-me.app.groovy
 *  Dim With Me
 *
 *  Author: todd@wackford.net
 *  Date: 2013-11-12
 */
/**
 *  App Name:   Dim With Me
 *
 *  Author: 	Todd Wackford
 *		twack@wackware.net
 *  Date: 	2013-11-12
 *  Version: 	0.2
 *  
 *  Use this program with a virtual dimmer as the master for best results.
 *
 *  This app lets the user select from a list of dimmers to act as a triggering
 *  master for other dimmers or regular switches. Regular switches come on
 *  anytime the master dimmer is on or dimmer level is set to more than 0%.
 *  of the master dimmer.
 *

 */


// Automatically generated. Make future change here.
definition(
    name: "Dim With Me",
    namespace: "wackware",
    author: "todd@wackford.net",
    description: "Follows the dimmer level of another dimmer",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("When this...") { 
		input "masters", "capability.switchLevel", 
			multiple: false, 
			title: "Master Dimmer Switch...", 
			required: true
	}

	section("And these will follow with dimming level...") {
		input "slaves", "capability.switchLevel", 
			multiple: true, 
			title: "Slave Dimmer Switch(es)...", 
			required: true
	}
}

def installed()
{
	subscribe(masters, "switch.setLevel", switchSetLevelHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	subscribe(masters, "switch", switchSetLevelHandler)
	subscribe(masters, "switch.on", switchSetLevelHandler)	
}

def updated()
{
	unsubscribe()
	subscribe(masters, "switch.setLevel", switchSetLevelHandler)
	subscribe(masters, "switch.off", switchOffHandler)
	subscribe(masters, "switch", switchSetLevelHandler)
	subscribe(masters, "switch.on", switchSetLevelHandler)
	
	log.info "subscribed to all of switches events"
}

def switchSetLevelHandler(evt)
{	
	    if ((evt.value == "on") || (evt.value == "off" ))
			return

			def level = evt.value.toFloat()
			level = level.toInteger()
			log.info "In switchSetLevelHandler setting Level to: ${level}"
			slaves?.setLevel(level)
}

def switchOffHandler(evt) {
	

    	if (state.DWMTurnedOnLight) {
        log.info "The real switch was turned on by DWM so turning off now"
		log.info "switchoffHandler Event: ${evt.value}"
		slaves?.off()
        state.DWMTurnedOnLight = false
	}
        else {
        log.info "The switch was NOT turned on by DWM so NOT turning off now"
        }
}
def switchOnHandler(evt) {
       state.alreadyOn = slaves?.latestValue("switch").contains("ON")
       
       log.debug "Set the state value of alreadyOn (contains ON) to ${state.alreadyOn}"
	   
       if (!state.alreadyOn) {
		log.info "The switch was not already on SO running switchOnHandler Event Value: ${evt.value}"
   		def dimmerValue = masters.latestValue("level") //can be turned on by setting the level
		log.info "Turning ON (switchOnHandler) with dimmerValue ${dimmerValue}"
		slaves?.setLevel(dimmerValue)
        state.DWMTurnedOnLight = true
		//slaves?.on()
	  }
        else {
        log.info "The real switch was turned on manually so NOT turning ON now"
        state.DWMTurnedOnLight = false
        log.info "DWMTurnedOnLight = $state.DWMTurnedOnLight"
        }
}