
definition(
	name: "Mode Change Thermostats",
	namespace: "drandyhaas",
	author: "Andy Haas",
	description: "Change the thermostat temperatures on a mode change",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

    preferences {
	section("Choose thermostat (s)") {
		input "thermostatgroup1", "capability.thermostat", title: "Thermostats in group 1", description: "Thermostats in group 1", required: true, multiple: true
		input "thermostatgroup2", "capability.thermostat", title: "Thermostats in group 2", description: "Thermostats in group 2", required: false, multiple: true
		input "thermostatgroup3", "capability.thermostat", title: "Thermostats in group 3", description: "Thermostats in group 3", required: false, multiple: true
		input "thermostatgroup4", "capability.thermostat", title: "Thermostats in group 4", description: "Thermostats in group 4", required: false, multiple: true
	}

    section("Set mode temperatures") {
        input "opHeatSet1", "decimal", title: "When Heating 1", description: "Heating temperature 1 for mode", required: true
        input "opCoolSet1", "decimal", title: "When Cooling 1", description: "Cooling temperature 1 for mode", required: true
        input "opHeatSet2", "decimal", title: "When Heating 2", description: "Heating temperature 2 for mode", required: false
        input "opCoolSet2", "decimal", title: "When Cooling 2", description: "Cooling temperature 2 for mode", required: false
        input "opHeatSet3", "decimal", title: "When Heating 3", description: "Heating temperature 3 for mode", required: false
        input "opCoolSet3", "decimal", title: "When Cooling 3", description: "Cooling temperature 3 for mode", required: false
        input "opHeatSet4", "decimal", title: "When Heating 4", description: "Heating temperature 4 for mode", required: false
        input "opCoolSet4", "decimal", title: "When Cooling 4", description: "Cooling temperature 4 for mode", required: false
    }
}

def installed()
{
	subscribeToEvents()
}

def updated()
{
    unsubscribe()
    subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(location, modeChangeHandler)
}

// Handle mode changes, reinitialize the current temperature and timers after a mode change
def modeChangeHandler(evt) {
    if (thermostatgroup1){
    def thermosize1 = thermostatgroup1.size()
    log.debug "thermostatgroup1 size is $thermosize1 "
    if (thermosize1>0){
      thermostatgroup1.setHeatingSetpoint(opHeatSet1)
      thermostatgroup1.setCoolingSetpoint(opCoolSet1)
      log.info "Set $thermostatgroup1 Heat $opHeatSet1°, Cool $opCoolSet1° on $evt.value mode"
      sendNotificationEvent("Set $thermostatgroup1 Heat $opHeatSet1°, Cool $opCoolSet1° on $evt.value mode")
    }
    }
    if (thermostatgroup2){
    def thermosize2 = thermostatgroup2.size()
    log.debug "thermostatgroup2 size is $thermosize2 "
    if (thermosize2>0){
      thermostatgroup2.setHeatingSetpoint(opHeatSet2)
      thermostatgroup2.setCoolingSetpoint(opCoolSet2)
      log.info "Set $thermostatgroup2 Heat $opHeatSet2°, Cool $opCoolSet2° on $evt.value mode"
      sendNotificationEvent("Set $thermostatgroup2 Heat $opHeatSet2°, Cool $opCoolSet2° on $evt.value mode")
    }
    }
    if (thermostatgroup3){
    def thermosize3 = thermostatgroup3.size()
    log.debug "thermostatgroup3 size is $thermosize3 "
    if (thermosize3>0){
      thermostatgroup3.setHeatingSetpoint(opHeatSet3)
      thermostatgroup3.setCoolingSetpoint(opCoolSet3)
      log.info "Set $thermostatgroup3 Heat $opHeatSet3°, Cool $opCoolSet3° on $evt.value mode"
      sendNotificationEvent("Set $thermostatgroup3 Heat $opHeatSet3°, Cool $opCoolSet3° on $evt.value mode")
    }
    }
    if (thermostatgroup4){
    def thermosize4 = thermostatgroup4.size()
    log.debug "thermostatgroup4 size is $thermosize4 "
    if (thermosize4>0){
      thermostatgroup4.setHeatingSetpoint(opHeatSet4)
      thermostatgroup4.setCoolingSetpoint(opCoolSet4)
      log.info "Set $thermostatgroup4 Heat $opHeatSet4°, Cool $opCoolSet4° on $evt.value mode"
      sendNotificationEvent("Set $thermostatgroup4 Heat $opHeatSet4°, Cool $opCoolSet4° on $evt.value mode")
    }
    }
}
