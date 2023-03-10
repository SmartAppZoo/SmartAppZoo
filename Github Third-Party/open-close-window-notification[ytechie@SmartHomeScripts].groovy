definition(
    name: "Open/Close Window Notification",
    namespace: "ytechie",
    author: "Jason Young",
    description: "Sends a notification when the windows should be opened/closed in order to match the optimal temperature.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Temperature Sensors") {
        input "mainFloorTempSensor", "capability.temperatureMeasurement", required: true
        input "outdoorTempSensor", "capability.temperatureMeasurement", required: true
	}
    section("Parameters") {
    	input "targetTemp", "number", required: true, title: "Target Temperature"
    }
    /*
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to")
    }
    */
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.open = false

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(mainFloorTempSensor, "temperature", tempChange)
    subscribe(outdoorTempSensor, "temperature", tempChange)    
}

def tempChange(evt) {
  log.debug "Main floor temp: " + mainFloorTempSensor.latestValue("temperature")
  log.debug "Outdoor temp: " + outdoorTempSensor.latestValue("temperature")
  
  def outdoorTemp = outdoorTempSensor.latestValue("temperature")
  def indoorTemp = mainFloorTempSensor.latestValue("temperature")
  
	//Todo: take into account target temp
	def shouldBeOpen = outdoorTemp < indoorTemp;
  
	if(state.open && !shouldBeOpen) {
    	sendPush("Close the Windows")
    	state.open = false
	}
    if(!state.open && shouldBeOpen) {
    	sendPush("Open the Windows")
    	state.open = true
	}
  
	log.debug "Windows open: " + state.open
}