/*
 * Master Outlet
 *
 * Author: jschlackman (james@schlackman.org)
 * Version: 1.0
 * Date: 2019-04-10
 *
 * Turn switches on and off according to whether a selected device is drawing power above its idle level.
 *
 * Latest version: https://github.com/jschlackman/MasterOutlet
 *
 */

definition(
	name: "Master Outlet",
	namespace: "jschlackman",
	author: "james@schlackman.org",
	description: "Turn switches on and off according to whether a selected device is drawing power above its idle level.",
	category: "Convenience",
	iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png",
	iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png"
)

preferences {
	section {
    	input(name: "about", type: "paragraph", element: "paragraph", required: false, title: "Master Outlet 1.0", description: "By James Schlackman <james@schlackman.org>\n\nSelect a Master device below. When that device is drawing power above a certain level, the selected switches will be turned on. When power usage falls below that level, the switches will be turned off.")
    }
	section {
		input(name: "meter", type: "capability.powerMeter", title: "Power meter to use as Master", required: true, multiple: false, description: null)
		input(name: "threshold", type: "number", title: "Master device on/off power level", required: true, description: "Enter power in W.")
	}
	section {
		input(name: "switches", type: "capability.switch", title: "Control these Switches", required: true, multiple: true, description: null)
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
	state.MasterOn == null
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
	def meterValue = evt.value as double
	def thresholdValue = threshold as int
	if (meterValue > thresholdValue) {
		// Only send on commands if the Master device was previously off
        if (!(state.MasterOn == "on")) {
        	log.debug "${meter} reported energy consumption at or above ${threshold}. Turning on switches."
			switches.on()
            state.MasterOn = "on"
        }
        else {
           	log.debug "${meter} reported energy consumption at or above ${threshold}, but device was already on when previously reported."
        }
	}
	else {
		// Only send on commands if the Master device was previously on
        if (!(state.MasterOn == "off")) {
			log.debug "${meter} reported energy consumption below ${threshold}. Turning off switches."
			switches.off()
            state.MasterOn = "off"
        }
        else {
           	log.debug "${meter} reported energy consumption below ${threshold}, but device was already on when previously reported."
        }
	}
}