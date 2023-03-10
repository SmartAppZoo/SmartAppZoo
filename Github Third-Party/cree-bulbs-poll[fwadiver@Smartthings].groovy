/**
 *  Cree Light Bulbs Power Outage Restoration
 *
 *  Author: fwadiver
 */
definition(
    name: "CREE Bulbs Poll",
    namespace: "fwadiver",
    author: "fwadiver",
    description: "Restore CREE Bulbs after power outage and perform usage poll",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Cree light Bulbs:") {
		input "CreeBulbs", "capability.switch", title: "Cree List?", multiple: true
	}
    section ("Specify the level of tracing to be done.  Defaults to none") {
    	input(name: "trclevel", type: "enum", title: "Trace Level", options: ["debug","trace","info","none"])
	}
}

def installed()
{
	DEBUG("Installed with settings: ${settings}")
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
    subscribe(CreeBulbs, "switch", switchHandler, [filterEvents: false])
	schedule("0 0/1 * * * ?",checkRestore)
    if (trclevel != "debug" && trclevel != "trace" && trclevel != "info"){
    	trclevel = "none"
    }
}

def switchHandler(evt) {
    def lightsState = [:]
    lightsState = state.lStates
    def allon = state.allon
    INFO("${evt.displayName} event = ${evt.value} at hub ${evt?.hub.id} allon = $allon")
    if (allon == 0){
    	if (evt.value == "on"){
        	lightsState[evt.deviceId] = now()
    		state.lStates = lightsState
    		}
    	else {
    		lightsState[evt.deviceId] = "off"
    	}
        DEBUG("Saved Light Status is $lightsState")
    }
    if (allon == 1 && evt.value == "off"){
    	lightsState[evt.deviceId] = "off"
    }
}

def checkRestore(evt) {
    TRACE("Checking Restore")  
    ResetBulbs()
    def lightsState = [:]
    lightsState = state.lStates
    def switchcnt = 0
    def oncnt = 0
    TRACE("Counting bulbs that are on")
    CreeBulbs?.each {
    	switchcnt = switchcnt+1
    	DEBUG("Switch id = ${it.id} : ${it.displayName} value ${it.currentSwitch}")
        if (it.currentSwitch == "on"){
        	oncnt = oncnt+1
        }
    }
    if (switchcnt > 3) {
    	switchcnt = switchcnt-1
    }
    if (switchcnt <= oncnt ) {
    	INFO("All lights on - turning them off")
        state.allon = 1
    	TurnOff()
    	}
    else {
    	state.allon = 0
    	pollBulbs()
    }
}

def TurnOff(evt) {
	def lightsState = [:]
    lightsState = state.lStates
    def timediff
	CreeBulbs.each {
    	if (lightsState[it.id] == "off"){
        	TRACE("Turn off ${it.displayName}")
        	it.off()
        	}
        else{
            def currtime = now()
            def timeon = lightsState[it.id]
            timediff = (currtime - timeon) / 1000
            DEBUG("CurrentTime = $currtime : OnTime = $timeon : TimeDef = $timediff seconds")
            if (timediff > 300){
        		TRACE("$it.displayName} remains on")
            	}
            else {
                TRACE("Turn off ${it.displayName}")
                it.off()
            }    
        }
    }
}

private pollBulbs(evt) {
	CreeBulbs.each {CreeBulb ->
		def hasPoll = CreeBulb.hasCommand("poll")
    	if (hasPoll) {
    		CreeBulb.poll()
        	TRACE("Poll ${CreeBulb.displayName}")
    	}
	}
}

private ResetBulbs(evt) {
	CreeBulbs.each {CreeBulb ->
		CreeBulb.refresh()
        TRACE("Refresh ${CreeBulb.displayName}")
	}
}

private InitStates() {
	TRACE("Initializing States")
   	def lightsState = [:]
    CreeBulbs?.each {
    	DEBUG("${it.displayName} current state is ${it.currentSwitch}")
        if (it.currentSwitch == "on") {
        	lightsState[it.id] = now()
            }
        else {
			lightsState[it.id] = it.currentSwitch
        }
    }
   	state.lStates = lightsState
    state.allon = 0
    DEBUG("Initialize at $state.lStateTime")
    DEBUG("$lightsState")
}

private def TRACE(message) {
    if (trclevel == "trace" || trclevel == "debug"){
    	log.trace message
    }
}

private def DEBUG(message) {
	if (trclevel == "debug" ){
		log.debug message
	}
}

private def INFO(message) {
		log.info message
}