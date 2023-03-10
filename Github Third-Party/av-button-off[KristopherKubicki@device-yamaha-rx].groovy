/**
 *  AV Input - Automatically set an AV input based on a mode or a switch
 *   
 */

definition(
	name: "AV Button Off",
	namespace: "KristopherKubicki",
	author: "kristopher@acm.org",
	description: "Set an AV input when a switch is set to off",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MiscHacking/remote@2x.png"
)

preferences {
	section("Control these AV Receivers...") {
        //  Ideally, I would specify capability.avTuner instead
		input "receivers", "capability.musicPlayer", title: "Which Receivers?", multiple:true, required: true
	}
    section("Whenever this switch is turned off") {
		input "switches", "capability.switch", title: "Which switches?", multiple:true, required: false
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
	subscribe(switches, "switch.off", receiverHandler)
}


def receiverHandler(evt) {
	receivers?.inputSelect(inputChan)
    receivers?.refresh()
}
