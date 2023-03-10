definition(
    name: "SmartThings Coffee Switch",
    namespace: "smartthings",
    author: "Curtis Carney",
    description: "Dumb Coffee Maker Turned Smart",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)


preferences {
	section("When this virtual switch is turned on..."){
		input "theSwitch", "capability.switch", title: "What virtual switch?", required: true, multiple: false
	}
	section("Check that this water sensor detects water...") {
		input "alarm", "capability.waterSensor", title: "What water sensor?", required: true, multiple: false
	}
	section("Before turning on this coffee maker (actual switch)...") {
		input "coffee", "capability.switch", title: "What switch/outlet is the coffee maker connected to?", required: true, multiple: false
	}
}

def installed() {
	subscribe(theSwitch, "switch.On", onHandler)
    subscribe(alarm, "water.dry", waterDryHandler)
}

def updated() {
	unsubscribe()
	subscribe(theSwitch, "switch.On", onHandler)
    subscribe(alarm, "water.dry", waterDryHandler)
}

def onHandler(evt) {
    checkWater()
}
    
def checkWater(evt) {
	def sensor = alarm.latestValue("water")
    
    if (sensor == "wet") {
        sendPush("Your coffee is brewing!")
   		coffee.on()
    } else {
        sendPush("You forgot to add water dummy! Cancelling brew.")
    	theSwitch.off()
    }
}
    
def waterDryHandler(evt) {
	def switchValue = theSwitch.latestValue("switch")
    if (switchValue == "on") {
        theSwitch.off()
        sendPush("Your coffee is ready!")
    }
}