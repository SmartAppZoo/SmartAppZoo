/**
 *  Poller
 *
 *  Author: Carlo Innocenti
 *  Date: 2014-04-02
 */

// Automatically generated. Make future change here.
definition(
    name: "Poller",
    namespace: "",
    author: "Carlo Innocenti",
    description: "Poller",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
    section("Settings... ") {
		input "devices", "capability.polling", title: "Devices", multiple: true, required: true
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
	unschedule()
	//schedule("0 0/${interval} * * * ?", poll)
    //runEvery5Minutes(poll)
    runIn(180, poll)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
    subscribe(app, appTouch)
}

def pollerEvent(evt) {
	log.debug "[PollerEvent]"
    if (state.keepAliveLatest && now() - state.keepAliveLatest > 270000) {
    	log.error "Waking up timer"
    	poll()
    }
}
def poll() {
	log.info "Polling"
    runIn(180, poll)
    state.keepAliveLatest = now()
	devices.poll()
}

def appTouch(evt)
{
	log.debug "appTouch: $evt, $settings"
	poll()
}
