definition(
    name: "Refresh Weather Station",
    namespace: "st.kmugh",
    author: "kmugh",
    description: "Updates Virtual Weather Station devices every hour.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Weather/weather9-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Weather/weather9-icn@2x.png"
)

preferences {
	section("Choose Weather Station")
    {
		input "weatherDevices", "device.virtualWeatherStation"
	}
    
    section("Input Update Interval (minutes)")
    {
    	input "updateInterval", "number"
    }
}

def installed() {
	// log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	// log.debug "Updated with settings: ${settings}"

	unschedule()
	initialize()
}

def initialize() {
    scheduledEvent()
}

def scheduledEvent() {
	state.lastRun = new Date().toSystemFormat()
	// log.trace "scheduledEvent() @ $state.lastRun"
    runIn(updateInterval * 60, scheduledEvent, [overwrite: false])
	weatherDevices.refresh()
}