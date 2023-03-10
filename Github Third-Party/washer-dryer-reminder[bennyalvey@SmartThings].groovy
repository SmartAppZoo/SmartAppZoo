/*
 * Turn on a switch when a washing machine has been stopped for a certain period of time
 *
 * This app makes use of the runIn() method to turn off the switch the specified period of time
 * after the vibration stops.
 *
 */
preferences {

	section("When this washing machine...") {
		input "sensor1", "capability.accelerationSensor"
	}
	section("Has stopped for this number of minutes...") {
		input "timePeriod", "decimal", title: "Minutes"
	}
	section("Turn on a switch ...") {
		input "switch1", "capability.switch"
	}
}

def installed()
{
	subscribe(sensor1, "acceleration", accelerationHandler)
}

def updated()
{
	unsubscribe()
	subscribe(sensor1, "acceleration", accelerationHandler)
}

def accelerationHandler(evt)
{
	// this method will be called whenever virbration starts or stops
	log.trace "$evt.name: $evt.value"

	if (evt.value == "active") {
		// If vibration is active, unschedule the turning on of the switch
		unschedule("turnOn")
	} else if (evt.value == "inactive") {
		// If vibration is inactive, start the timer
		runIn(timePeriod * 60, turnOn)
	}
}

def turnOn()
{
	// turn on the switch (and/or whatever else you want to do when the washer stops)
	switch1.on()
	unschedule("turnOn") // This is a work-around to a bug in runIn()
}