/**
 *  Eco Mode
 *
 *  Author: MW
 *
 */
preferences {
	section("Turn off these:"){
		input "switch1", "capability.switch", multiple: true
	}
	section("When we are away:") {
        input "modeAway", "mode", title: "Away Mode?", required: false
    }
    section("At a certain time:") {
    	input "runTime", "time", title: "Time?", required: false
    }
}

def installed()
{
    initialize()
}

def updated()
{
	unsubscribe()
    initialize()
}

def initialize() {
	subscribe(app)
    if(modeAway) {
    	subscribe(location, modeChanged)
    }
    if(runTime) {
    	schedule(runTime, scheduledTime)
    }
}

def appTouch(evt) {
	log.info evt.value
    allOff()
}

def modeChanged(evt) {
	if (location.mode == modeAway) {
		log.info evt.value
    	allOff()
    }
}

def scheduledTime() {
	allOff()
}


def allOff() {
	switch1?.off()
}
