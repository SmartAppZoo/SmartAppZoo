/**
 *  Local Weather Station
 *
 *  Copyright 2018 Brian Steere
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
	name: "Local Weather Station",
	namespace: "dianoga",
	author: "Brian Steere",
	description: "Weather station using combined local and cloud data",
	category: "Green Living",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	singleInstance: true
)


preferences {
	section("Setup") {
		paragraph "Nothing to see here. Hit done and move along"
	}
}

mappings {
	path("/update") {
		action: [
			POST: "updateStation"
		]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}

def uninstalled() {
	getChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def initialize() {
	if (!state.accessToken) {
		createAccessToken()
	}

	def deviceId = "local-weather-station"

	def children = getChildDevices()

	if (children.size() == 0) {
		try {
			addChildDevice("Local Weather Station", deviceId, null, [name: "Device.${deviceId}", label: "Local Weather Station", completedSetup: true])
		} catch (Exception e) {
			log.error "Error creating device: ${e}"
		}
	}

	runEvery1Minute(poll)

	log.debug("URL: ${commandUrl}")
}

private String getCommandUrl() {
	return apiServerUrl("api/smartapps/installations/${app.id}/update?access_token=${state.accessToken}")
}

def poll() {
	def child = getChildDevices()[0]
	child?.poll()
}

def updateStation() {
	def data = request.JSON
	log.debug "Updating local weather station: ${data}"

	def child = getChildDevices()[0]

	child?.sendEvent(name: "temperature", value: fToPref(data.tempf) as Double, unit: getTemperatureScale())
	child?.sendEvent(name: "humidity", value: data.humidity, unit: "%")
	child?.sendEvent(name: "illuminance", value: calculateLux(data.solarradiation) as Integer, unit: "lux")

	child?.sendEvent(name: "wind", value: data.windspeedmph as Double, unit: "mph")
	child?.sendEvent(name: "windGust", value: data.windgustmph as Double, unit: "mph")
	child?.sendEvent(name: "dewPoint", value: fToPref(data.dewptf) as Double, unit: getTemperatureScale())
	child?.sendEvent(name: "feelsLike", value: fToPref(data.windchillf) as Double, unit: getTemperatureScale())
	child?.sendEvent(name: "windDirection", value: data.winddir as Integer)
	child?.sendEvent(name: "rainInches", value: data.rainin as Double, unit: "in")
	child?.sendEvent(name: "rainDailyInches", value: data.dailyrainin as Double, unit: "in")
	child?.sendEvent(name: "rainWeeklyInches", value: data.weeklyrainin as Double, unit: "in")
	child?.sendEvent(name: "rainMonthlyInches", value: data.monthlyrainin as Double, unit: "in")
	child?.sendEvent(name: "rainYearlyInches", value: data.yearlyrainin as Double, unit: "in")
	child?.sendEvent(name: "pressureInches", value: data.baromin as Double, unit: "in")
}

def fToPref(temp) {
	if(getTemperatureScale() == 'C') {
		return temp / 1.8 - 32
	} else {
		return temp
	}
}

def calculateLux(rad) {
	(rad as Double) * 126.7
}