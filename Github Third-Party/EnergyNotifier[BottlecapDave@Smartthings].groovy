/**

    Energy Notifier
    *
    Notifies the user via a push notification when a plug has been over a configurable threshold for a configurable amount of time 
    *
    */
    definition(
        name: "Energy Notifier",
        namespace: "bottlecap",
        author: "David Kendall",
        description: "Notify when when consumption is above a certain threshold for a given period of time",
        category: "Green Living",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
    )

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
		input(name: "threshold", type: "number", title: "Reports above...", required: true, description: "in watts.")
	}
    section("After time has passed...") {
        input(name: "hourspassed", type: "number", title: "Hours", required: true, defaultValue: 0)
		input(name: "minutespassed", type: "number", title: "Minutes", required: true,  defaultValue: 0)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	state.lastvalue = Integer.MAX_VALUE
    state.isSwitchOffScheduled = false

	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
  def meterValue = evt.value as double
  def thresholdValue = threshold as int
  log.debug "${meter} reported energy consumption of ${meterValue}. Last value is ${state.lastvalue}"

  if (meterValue >= thresholdValue) {
      if (state.isSwitchOffScheduled == false) {
          state.isSwitchOffScheduled = true
          log.debug "${meter} reported energy consumption above ${threshold}. Scheduling to notify."
          runIn((hourspassed * 60 * 60) + (minutespassed * 60), switchOff)
      }
  }
  else {
      state.isSwitchOffScheduled = false
  }

  state.lastvalue = meterValue
}

def switchOff() {
    def thresholdValue = threshold as int
	if (state.lastvalue >= thresholdValue) {
        log.debug "${meter} reported energy consumption above ${threshold} after ${hourspassed} hours ${minutespassed} minutes. Notifying parties."
        sendPush("${meter} has been on for more than ${hourspassed} hour(s) ${minutespassed} minute(s)")
    }
    
    state.isSwitchOffScheduled = false
}
