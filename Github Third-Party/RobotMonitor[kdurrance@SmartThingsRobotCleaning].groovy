/**
 *
 *  Robot Vacuum Monitor
 *
 *  Place a SmartThings Multi Sensor inside your robot vacuum cleaner to track movement.
 *  Create a new Mode called "Robot Cleaning" or similar, and disable motion sensors in this Mode
 *  Once robot is inactive for the defined period the Mode switches back to the desired Mode
 *
 *
 *  Author: Karl Durrance
 *  Git repo: https://github.com/kdurrance/SmartThingsRobotCleaning
 *
 */
definition(
    name: "Robot Vacuum Monitor",
    namespace: "kdurrance",
    author: "Karl Durrance",
    description: "Changes mode based on accelerometer sensor on SmartThings Multi Sensor",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot@2x.png"
)

preferences {
	section("When this sensor detects movement"){
		input "accelerationSensor", "capability.accelerationSensor", title: "Select a SmartThings Multi Sensor"
	}

	section("Change to this mode when Robot is cleaning") {
		input "runningMode", "mode", title: "Change to Mode when cleaning?"
	}
    
	section("Change to this mode when Robot is finished") {
		input "finishedMode", "mode", title: "Change to Mode when finished?"
	}
}

def installed() {
	subscribe(accelerationSensor, "acceleration.active", RobotCleaningHandler)
}

def updated() {
	unsubscribe()
	subscribe(accelerationSensor, "acceleration.active", RobotCleaningHandler)
}

def RobotCleaningHandler(evt) {
	// Change mode to Robot Active Mode

	log.debug "RobotCleaningHandler, location.mode = $location.mode, runningMode = $runningMode, location.modes = $location.modes"
	
	if (location.mode != runningMode) {
		if (location.modes?.find{it.name == runningMode}) {
			setLocationMode(runningMode)
            		sendNotificationEvent("Robot has begun cleaning. I changed mode to '${runningMode}' as you requested.")
			log.debug "Robot Vacuum Monitor has changed the mode to '${runningMode}'"
		}
		else {
			log.warn "Robot Vacuum Monitor tried to change to undefined mode '${runningMode}'"
		}
	}
    
    // execute finished handler a couple of minutes after inactivity
    runIn(2 * 60, RobotCleaningFinished)
}

def RobotCleaningFinished() {
	// Change mode to Robot InActive Mode

	log.debug "RobotCleaningFinished, location.mode = $location.mode, finishedMode = $finishedMode, location.modes = $location.modes"
	
	if (location.mode != finishedMode) {
		if (location.modes?.find{it.name == finishedMode}) {
			setLocationMode(finishedMode)
            		sendNotificationEvent("Robot has finished cleaning. I changed mode to '${finishedMode}' as you requested.")
			log.debug "Robot Vacuum Monitor has changed the mode to '${finishedMode}'"
		}
		else {
			log.warn "Robot Vacuum Monitor tried to change to undefined mode '${finishedMode}'"
		}
	}
}
