/**
 *  Light Bulb Events
 *
 *  Author: fwadiver
 */
definition(
    name: "My Light Bulb Events",
    namespace: "fwadiver",
    author: "fwadiver",
    description: "Determine light bulb events supported",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Light Bulb being tested:") {
		input "TestBulbs", "capability.switch", title: "Bulb List?", multiple: true
	}
}

def installed()
{
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated()
{
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	InitStates()
    subscribe(TestBulbs, "switch", switchHandler, [filterEvents: false])
	schedule("0 0/1 * * * ?",checkRestore)
    
}
def switchHandler(evt) {
	log.info "${evt.displayName} ${evt.value} at hub ${evt?.hub.id}"
	if (evt.physical) {
		log.trace "Event is physical"
	}
	else {
		log.trace "Event is digital"
	}
    def lightsState = [:]
    lightsState = state.lStates
    lightsState[evt.deviceId] = evt.value
    log.info "$lightsState"
    state.lStates = lightsState
}

def checkRestore(evt) {
    log.debug "Checking Restore"  
    TestBulbs?.each {
    	log.debug "Switch id = ${it.id} : ${it.displayName} value ${it.currentSwitch}"
/*        if (it.currentSwitch == "on"){
        	log.debug "Turning on ${it.displayName}"
        	it.on()
            }
        else {
        	log.debug "Turning off ${it.displayName}"
        	it.off()
        } */
   	} 
    pollBulbs()
}

private pollBulbs(evt) {

TestBulbs.each {TestBulb ->
	def hasPoll = TestBulb.hasCommand("poll")
    if (hasPoll) {
    	TestBulb.poll()
        log.debug "Poll ${TestBulb.displayName}"
    	}
    else {
    	TestBulb.refresh()
        log.debug "Refresh ${TestBulb.displayName}"
    	}
	}
}

private InitStates() {
	log.debug "Initializing States"
   	def lightsState = [:]
    TestBulbs?.each {
    	log.debug "${it.displayName} current state is ${it.currentSwitch}"
		lightsState[it.id] = it.currentSwitch
    }
   	state.lStates = lightsState
    log.debug "$lightsState"
}
