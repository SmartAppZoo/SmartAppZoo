definition(
	name: "Thermostat",
	namespace: "ajcarpenter",
	author: "Andrew Carpenter",
	description: "Programmable thermostat with for devices that support thermostatMode + temperatureMeasurement",
	category: "Green Living",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	singleInstance: true
)

preferences {
	page(name: "page1", title: "Thermostat", install: true, uninstall: true) {
		section("Set these thermostats") {
			input "thermostat", "capability.thermostatMode", title: "Which?"
		}

		section("Using these temperature sources") {
			input "temperatureMeasurements", "capability.temperatureMeasurement", title: "Which?", multiple: true
		}
		
		section("Emergency heat") {
			input("emergencyHeatButtons", "capability.momentary", title: "Emergency heat buttons", multiple: true)
			input("emergencyHeatSetpoint", type: "decimal", title: "Emergency heat setpoint", defaultValue: 21)
			input("emergencyHeatMinutes", type: "number", title: "Emergency heat minutes", defaultValue: 30)
		}

		section("Configuration") {
			app(name: "days", appName: "Thermostat Days", namespace: "ajcarpenter", title: "Day Pattern", multiple: true)
			input("modes", "mode", title: "Only heat in these modes", multiple: true)
		}

		section( "Notifications" ) {
			input "sendPushMessage", "enum", title: "Send a push notification?", options:["Yes", "No"], required: false
		}
	}
}

def installed() {
	// subscribe to these events
	log.debug "Installed called with $settings"
	initialize()
}

def updated() {
	// we have had an update
	// remove everything and reinstall
	log.debug "Updated called with $settings"
	initialize()
}

def getTimeZone() {
	location.timeZone ?: TimeZone.getDefault()
}

def initialize() {
	unschedule()
	unsubscribe()

	subscribe(emergencyHeatButtons, "momentary.pushed", triggerEmergencyHeat)
	runEvery5Minutes(updateThermostatMode)
}

def triggerEmergencyHeat(event) {
	Calendar localCalendar = Calendar.getInstance()
	localCalendar.setTimeZone(getTimeZone())
	state.emergencyHeatStart = localCalendar.getTime()
	localCalendar.add(Calendar.MINUTE, emergencyHeatMinutes.toInteger())
	state.emergencyHeatEnd = localCalendar.getTime()
	updateThermostatMode()
}

def updateThermostatMode() {
	def inEmergencyHeat = state.emergencyHeatStart != null && timeOfDayIsBetween(state.emergencyHeatStart, state.emergencyHeatEnd, new Date(), getTimeZone())
	def heatingSetpoint = inEmergencyHeat ? new BigDecimal(emergencyHeatSetpoint) : getCurrentHeatingSetpoint();
	def temperature = getFusedTemperatureMeasurements();
	def inValidMode = modes.size() == 0 || location.mode in modes;
   
	def mode = temperature < heatingSetpoint && inValidMode  ? 'heat' : 'off';
	
	if (thermostat.currentThermostatMode != mode) {
		def debugString = "Thermostat: Temperature = ${ temperature }, Setpoint = ${ heatingSetpoint }. Current mode ${ thermostat.currentThermostatMode }. Setting mode to ${ mode }. In valid mode = ${ inValidMode }. In emergency heat = ${ inEmergencyHeat }"
		
		log.debug debugString
		thermostat.setThermostatMode(mode)
		
		if(sendPushMessage) {
			sendPush(debugString)
		}
	}
}

def getCurrentHeatingSetpoint() {
	def day = getActiveDay();
	if (day) {
		def time = day.getActiveTime();
		if (time) {
			return time.heatingSetpoint
		}
	}
}

def getActiveDay() {
	Calendar localCalendar = Calendar.getInstance()
	localCalendar.setTimeZone(getTimeZone())
	int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)
	
	def days = getChildApps().find { 
		currentDayOfWeek.toString() in it.days
	}
}

def getFusedTemperatureMeasurements() {
	def validTemperatureMeasurements = temperatureMeasurements.findAll {
		it.currentTemperature != null
	}
	
	def average = validTemperatureMeasurements.sum {
		it.currentTemperature
	} / validTemperatureMeasurements.size()
	
	return average
}