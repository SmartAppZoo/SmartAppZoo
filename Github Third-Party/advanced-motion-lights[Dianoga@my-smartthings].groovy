/**
*  Advanced Motion Lights
*
*  <TODO: Enter some description of your smart app project here>
*
*  Copyright undefined Brian Steere
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
definition(
	name: "Advanced Motion Lights",
	namespace: "dianoga",
	author: "Brian Steere",
	description: "Enter some description of your smart app project here",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

def getZoneCount() {
	return 10
}

preferences {
	page(name: "mainPage", install: true, uninstall: true)

	for (def i = 1; i <= zoneCount; i++) {
		page(name: "zonePage${i}")
	}
}

def zonePage1() { getZonePage(1) }
def zonePage2() { getZonePage(2) }
def zonePage3() { getZonePage(3) }
def zonePage4() { getZonePage(4) }
def zonePage5() { getZonePage(5) }
def zonePage6() { getZonePage(6) }
def zonePage7() { getZonePage(7) }
def zonePage8() { getZonePage(8) }
def zonePage9() { getZonePage(9) }
def zonePage10() { getZonePage(10) }

def mainPage() {
	dynamicPage(name: "mainPage") {
		def enabledZones = 0
		def availableZones = []

		section {
			for (def i = 1; i <= zoneCount; i++) {
				if (settings["enabled${i}"]) {
					enabledZones++;
					href "zonePage${i}", title: settings["name${i}"];
				} else {
					availableZones.push(i)
				}
			}

			if (enabledZones < zoneCount) {
				def firstAvailableZone = availableZones[0]
				href "zonePage${firstAvailableZone}", title: "Setup new zone"
			}
		}
	}
}

def getZonePage(zoneNum) {
	dynamicPage(name: "zonePage${zoneNum}") {
		section {
			input "enabled${zoneNum}", "bool", title: "Zone Enabled", submitOnChange: true
		}

		if (settings["enabled${zoneNum}"]) {
			section("Zone Settings") {
				input "name${zoneNum}", "text", title: "Name", defaultValue: "Zone ${zoneNum}", required: true

				input "motion${zoneNum}", "capability.motionSensor", title: "Motion Sensor(s)", multiple: true, required: true
				input "switch${zoneNum}", "capability.switch", title: "Light(s)", multiple: true, required: true
			}

			section("Advanced Settings") {
				input "offAfterMotion${zoneNum}", "bool", title: "Turn off when motion stops", submitOnChange: true
				if (settings["offAfterMotion${zoneNum}"]) {
					input "offDelay${zoneNum}", "number", title: "How long", hint: "Minutes", required: true
				}

				input "physicalOverride${zoneNum}", "bool", title: "Physical Override", hint: "Leave on when physically turned on", defaultValue: false
			}
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	state.zones = [:]
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	for (def i = 1; i <= zoneCount; i++) {
		if (settings["enabled${i}"]) {
			subscribe(settings["motion${i}"], "motion", motionHandler)
			subscribe(settings["switch${i}"], "switch", switchHandler)
		}

		state.zones[i] = state.zones[i] ? state.zones[i] : [
			zone: i,
			active: false,
			inactive: 0,
			physical: false
		]
	}
}

def motionHandler(evt) {
	log.trace "Motion: value: ${evt.value}, deviceId: ${evt.deviceId}"

	def zones = findDevice("motion", evt.deviceId)
	log.trace "Zones: $zones"

	if (!zones) {
		log.trace "Device not found"
		return
	}

	zones.each { zone ->
		if (evt.value == "active") {
			log.debug settings["name${zone}"] + ": Turning on"
			state.zones["${zone}"].active = true

			settings["switch${zone}"]?.on()
		}

		// If all sensors in this zone are inactive
		if (evt.value == "inactive" && !settings["motion${zone}"].any { it.currentMotion == "active" }) {
			state.zones["${zone}"].active = false
			state.zones["${zone}"].inactive = nowSeconds

			if (!settings["offAfterMotion${zone}"]) {
				return
			}

			runIn(60 * settings["offDelay${zone}"], switchOff, [overwrite: false, data: [zone: zone]])
			// runIn(5 * settings["offDelay${zone}"] + 30, switchOff, [overwrite: false, data: [zone: zone, doubleCheck: true]])
		}
	}
}

def switchHandler(evt) {
	log.trace "Switch: physical: ${evt.isPhysical()}, value: ${evt.value}, deviceId: ${evt.deviceId}"

	def zones = findDevice("switch", evt.deviceId)
	log.trace "Zones: $zones"

	if (!zones) {
		log.trace "Device not found"
		return
	}

	zones.each { zone ->
		state.zones["${zone}"].physical = evt.isPhysical()
	}
}

def switchOff(data) {
	def zone = data.zone

	log.debug settings["name${zone}"] + ": Off requested (doubleCheck: ${data.doubleCheck})"

	settings["switch${zone}"].each {
		if (shouldSwitchTurnOff(it)) {
			log.debug settings["name${zone}"] + ": ${it} turned off"
			it.off()
		} else {
			log.debug settings["name${zone}"] + ": ${it} left on"
		}
	}
}

def shouldSwitchTurnOff(device) {
	def zones = findDevice("switch", device.id);

	return zones.every { zone ->
		def zoneState = state.zones["${zone}"]

		// Check for physical Override
		if (settings["physicalOverride${zone}"] && zoneState.physical) {
			log.trace "Physical ${device.displayName}"
			return false;
		}

		// Check against other sensors in zone
		if (zoneState.active) {
			log.trace "Active ${device.displayName}"
			return false
		}

		// Check against delay
		def offDelaySeconds = settings["offDelay${zone}"] * 60
		if (nowSeconds < zoneState.inactive + offDelaySeconds - 5) {
			log.trace "Delay ${device.displayName}, $nowSeconds, ${zoneState.inactive + offDelaySeconds}"
			return false
		}

		return true
	}
}

def findDevice(type, deviceId) {
	def result = []

	for (def i = 1; i < zoneCount; i++) {
		if (settings["enabled${i}"]) {
			settings["${type}${i}"].each {
				if (it.id == deviceId) {
					result.push(i)
				}
			}
		}
	}

	return result
}

int getNowSeconds() {
	return now() / 1000
}
