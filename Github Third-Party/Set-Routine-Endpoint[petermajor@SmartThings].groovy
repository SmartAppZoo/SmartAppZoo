definition(
    name: "Routines endpoint",
    namespace: "petermajor",
    author: "Peter Major",
    description: "External endpoint for setting current routine. It will also send a push notification of the change to my phone.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
}

mappings {
  path("/routine/") {
    action: [
      POST: "setRoutine"
    ]
  }
}

def installed() {}
    
def updated() {}

def setRoutine() {
    log.debug "setRoutine() executed"

    def routineName = params.routineName
	log.debug "params: routineName = ${params.routineName}"

	location.helloHome?.execute(routineName)
    
    sendPush("${routineName}")
    
    return [status: "ok"];
}