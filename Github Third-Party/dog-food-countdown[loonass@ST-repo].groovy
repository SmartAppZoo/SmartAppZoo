/**
 *  
 */
definition(
	name: "Dog food countdown",
	namespace: "loonass",
	author: "Mike Harvey",
	description: "To be used with DTH to turn on an indicator after set number of hours",
	category: "Pets",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	pausable: true
)

preferences {
	section("Choose indicator") {
		input "indicator", "capability.switchLevel", title: "Where?"
	}
}

def installed() {
	initialize()
}

def updated() {
	log.debug "Updated ${settings}"
	unsubscribe() //TODO no longer subscribe like we used to - clean this up after all apps updated
	initialize()
}

def initialize() {
	subscribe(indicator, "switch.off", action)
}

def action(evt) {
	log.debug "Reset countDown"
	def currentState = indicator.currentState("counter")
	log.debug "countDown = ${currentState.value}"
	timer()
	schedule("59 59 23 ? * * *", resetOccurrences)
}

def timer() {
    runIn(3600,"countDown")
}

def countDown() {
	indicator.counterDown()
	def currentState = indicator.currentState("counter")
	log.debug "countDown = ${currentState.value}"
	if (currentState.value == "0") {log.debug "Stop countDown"} else {timer()}
}

def resetOccurrences(evt) {
	log.debug "Reset occurrences"
    indicator.occurrencesReset()
}