/**
 *  AV Input - Automatically set an AV input based on a mode or a switch
 *   
 */

definition(
	name: "AV Input",
	namespace: "KristopherKubicki",
	author: "kristopher@acm.org",
	description: "Set an AV input based on a mode, motion or a switch",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote@2x.png"
)

preferences {
	section("Control these AV Receivers...") {
        //  Ideally, I would specify capability.avTuner instead
		input "receivers", "capability.musicPlayer", title: "Which Receivers?", multiple:true, required: true
	}
    section("Whenever this switch is activated") {
		input "switches", "capability.switch", title: "Which switches?", multiple:true, required: false
	}
    section("Or whenever this motion is activated") {
		input "motions", "capability.motionSensor", title: "Which motions?", multiple:true, required: false
	}
    section("Or when switching to these modes") {
		input "smodes", "mode", title: "Which mode?", multiple:true, required: false
	}
    section("With this input...") {
		input(name: "inputChan", type: "text", title: "Which channel?", required: false)
        input(name: "level", type: "text", title: "What volume level?", required: false)
	}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

private def initialize() {
	log.debug("initialize() with settings: ${settings}")
	subscribe(switches, "switch.on", receiverHandler)
    subscribe(motions, "motion.active", receiverHandler)
    
	subscribe(location, "mode", modeHandler)
}

// If we detect the Hub moving into a sleep mode, also activate the handler
def modeHandler(evt) {
	log.debug "changing mode $evt"
    for(smode in smodes) { 
    	if(location.mode == smode) { 
    		receiverHandler(evt)
    	}
    }
}

def receiverHandler(evt) {
	receivers?.inputSelect(inputChan)
    receivers?.setLevel(level)
    receivers?.refresh()
}
