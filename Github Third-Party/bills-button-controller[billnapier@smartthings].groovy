/**
 *	Button Dimmer Controller
 *
 *  Heavily Modified from SmartThings "Button Controller"
 *
 *  Author: Bill Napier <napier@pobox.com>
 *	Date: 2015-5-13
 */
definition(
    name: "Bill's Button Controller",
    namespace: "billnaper",
    author: "Bill Napier <napier@pobox.com>",
    description: "Controls the Aeon Labs Minimote to handle dimming of lights on held button",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: "configureButton1", uninstall: configured()) {
		section {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
		}
	}
}

def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a second button, set it up here",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a third button, you can do even more here",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a fouth button, you rule, and can set it up here",
		install: true, uninstall: true, getButtonSections(4))
}

def getButtonSections(buttonNumber) {
	return {
    section("Dimmers") {
      input "dimmers_${buttonNumber}", "capability.switchLevel", title: "Dimmer", multiple: true, required: false
  	}
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
  log.debug "initialize"
	subscribe(buttonDevice, "button", buttonEvent)
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)
}

def buttonConfigured(idx) {
	return settings["dimmers_$idx"]
}

def buttonEvent(evt) {
	def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
	def value = evt.value
	log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
	log.debug "button: $buttonNumber, value: $value"

	def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
	log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

	if(recentEvents.size <= 1){
		switch(buttonNumber) {
			case ~/.*1.*/:
				executeHandlers(1, value)
				break
			case ~/.*2.*/:
				executeHandlers(2, value)
				break
			case ~/.*3.*/:
				executeHandlers(3, value)
				break
			case ~/.*4.*/:
				executeHandlers(4, value)
				break
		}
	} else {
		log.debug "Found recent button press events for $buttonNumber with value $value"
	}
}

def executeHandlers(buttonNumber, value) {
	log.debug "executeHandlers: $buttonNumber - $value"

  def dimmers = find('dimmers', buttonNumber, value)
  if (dimmers != null) doIt(dimmers, value)
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def doIt(devices, value) {
	log.debug "setLevel1: $devices = ${devices*.currentValue('level')}"
	log.debug "setLevel2: $devices = ${devices*.currentValue('switch')}"
  if (value == "held") {
    devices.on()
    // 30 should be an input param
    devices.setLevel(30)
  } else if (value == "pushed") {
    if (devices*.currentValue('switch').contains('on')) {
      devices.setLevel(99)
      devices.off()
    } else if (devices*.currentValue('switch').contains('off')) {
      devices.on()
      devices.setLevel(99)
    }
  }
}
