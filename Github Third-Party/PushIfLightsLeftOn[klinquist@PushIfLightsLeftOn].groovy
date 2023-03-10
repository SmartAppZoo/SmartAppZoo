/**
 *  Push If Lights Left On
 *
 *  Author: kris@linquist.net
 *  Date: 2014-03-25
 */
preferences {
	section("When one of these tags leaves..."){
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Monitor these lights..."){
		input "switches", "capability.switch", multiple: true
	}
   
}

def installed()
{
	subscribe(presence1, "presence", presenceHandler)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presenceHandler)
}


def leftLightsOn() {
	def result = false
    log.debug "Checking lights."
	for (it in switches) {
    	log.debug "checking a light."
		if (it.currentValue("switch") == "on") {
            result = it.displayName
            log.debug "You left the $it.displayName on."
			break
		}
	}
	return result
}


def presenceHandler(evt)
{
	log.debug "presenceHandler $evt.name: $evt.value"
	def current = presence1.currentValue("presence")
	log.debug current
	def presenceValue = presence1.find{it.currentPresence == "present"}
	log.debug presenceValue
	if(!presenceValue){
    	log.debug "You left."
        def light = leftLightsOn()
		if (light) {
        	sendPush("$light light was left on.")
		}
	}

}
