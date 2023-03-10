definition(
    name: "Intelligent Front Door Lighting",
    namespace: "jimmyfortinx",
    author: "Jimmy Fortin",
    description: "Adjust the ligthing based on multiple conditions like sunset, sunrise, time and more.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png"
)

preferences {
    section ("Control these lights") {
        input "lights", "capability.switch", multiple: true, required: true
    }

	section ("At morning") {
		input "morningOpenTime", "time", title: "What time to open ligths?", required: true
	}

    section ("At night") {
		input "nightCloseTime", "time", title: "What time to close ligths?", required: true
	}

    section ("Consider night when all devices are turned off") {
        input "devices", "capability.switch", multiple: true
        input "devicesCloseTime", "time", title: "What time to close ligths?"
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(location, "position", onLocationChange)

    schedule(morningOpenTime, "onMorning")
    schedule(nightCloseTime, "onNight")

    if (devices) {
        subscribe(devices, "switch.off", onDeviceTurnOff)
    }

	registerSunEvents()
    controlLights()
}

def onLocationChange(event) {
    log.trace "onLocationChange"

    controlLights(event)
}

def onSunrise(event) {
    log.trace "onSunrise"

	registerSunEvents()
    controlLights(event)
}

def onSunset(event) {
    log.trace "onSunset"

	registerSunEvents()
    controlLights(event)
}

def onMorning(event) {
    log.trace "onMorning"

    controlLights(event)
}

def onNight(event) {
    log.trace "onNight"

    controlLights(event)
}

def onDeviceTurnOff(event) {
    log.trace "onDeviceTurnOff"

    def openedDevices = filterSwitches(devices, true)

    if (openedDevices.size() == 0) {
        def nowTime = now()
        def closeTime = devicesCloseTime ? devicesCloseTime : nightCloseTime

        if (nowTime >= timeToday(closeTime).time) {
            turnOffLightsIfNeeded("As you requested, I closed front door ligths when all devices are closed.")
        }
    }
}

def filterSwitches(switchs, isOn) {
	def filter = isOn ? { value -> value == "on" } : { value -> value != "on" }

	return switchs.currentSwitch.findAll(filter)
}

def turnOnLightsIfNeeded(message) {
	def closedLights = filterSwitches(lights, false)

    if (closedLights.size() > 0) {
        lights.on();
        sendNotificationEvent(message)
    }
}

def turnOffLightsIfNeeded(message) {
    def openedLights = filterSwitches(lights, true)

    if (openedLights.size() > 0) {
        lights.off();
        sendNotificationEvent(message)
    }
}

def registerSunEvents () {
	def sunDetails = getSunriseAndSunset()

	def now = new Date()
	def riseTime = sunDetails.sunrise
	def setTime = sunDetails.sunset

	if (state.riseTime != riseTime.time) {
		unschedule("onSunrise")

		if(riseTime.before(now)) {
			riseTime = riseTime.next()
		}

		state.riseTime = riseTime.time

		log.info "scheduling sunrise handler for $riseTime"
		schedule(riseTime, onSunrise)
	}
    
    if (state.setTime != setTime.time) {
		unschedule("onSunset")

	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, onSunset)
	}
}

def controlLights(event) {
    def sunDetails = getSunriseAndSunset()

	def eventName = null
    
    if (event) {
    	eventName = event.name
    }

    def nowTime = now()
	def riseTime = sunDetails.sunrise
	def setTime = sunDetails.sunset
    
    if (nowTime < riseTime.time) {
        if (nowTime >= timeToday(morningOpenTime).time) {
            turnOnLightsIfNeeded("As you requested, I opened front door ligths at morning.")
        }
    } else if (nowTime < setTime.time) {
        turnOffLightsIfNeeded("As you requested, I closed front door ligths at sunrise.")
    } else if (nowTime < timeToday(nightCloseTime).time) {
        turnOnLightsIfNeeded("As you requested, I opened front door ligths at sunset.")
    } else {
        turnOffLightsIfNeeded("As you requested, I closed front door ligths at night.")
    }
}