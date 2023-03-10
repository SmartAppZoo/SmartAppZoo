/**
 *  ZWave Test
 *
 *  Author: mwwalker@gmail.com
 *  Date: 2014-01-12
 */
preferences { 
	section("Test") {
		input "testSwitch", "capability.switch", title: "Switch Device?", multiple: true
        input "thermostat", "capability.thermostat", title: "Thermostat?", multiple: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

    schedule("0 * * * * ?", initialize) 
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    //unschedule()
    schedule("0 * * * * ?", initialize) 
}

def initialize() {
	refreshDevice()
    runIn(900, refreshDevice)
    runIn(1800, refreshDevice)
    runIn(2700, refreshDevice)
}

def refreshDevice() {
	testSwitch.refresh()
    thermostat.poll()
}