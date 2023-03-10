/**
 *  ERV Timed Operation
 *
 *  Copyright 2016 Toby Jennings
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
    name: "ERV Timed Operation",
    namespace: "tcjennings",
    author: "Toby Jennings",
    description: "Reacts to the ERV Ventilation routine and schedules a task 20/40/60-minutes in future to turn the ERV back off.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
) {
	appSetting "apikey"
	appSetting "latitude"
	appSetting "longitude"
}

preferences {
	section("ERV Switches") {
		input "erv", "capability.switch", 
		title: "Which ERV On/Off switch?", multiple: false
		
		input "hilo", "capability.switch",
		title: "Which ERV Low/High switch?", multiple:false
	}
	
	section("Routine Timer Duration") {
		input "minutes", "number", required: true, title: "How Long (minutes)?"
	}

	section("Periodic Timer Duration") {
		input "pminutes", "number", required: true, title: "How Long (minutes < 60)?"
	}
    
	section("Outdoor Conditions") {
		input "outdoor", "capability.relativeHumidityMeasurement",
			required: true, title: "Which Outdoor temp/humidity sensor?"
		
		input "dewpoint", "number", required: false,
			title: "Dew Point Threshold (default 70)?"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	getWebForecast()
	runEvery1Hour(periodicVentilation)
	subscribe(location, "routineExecuted", routineChanged)
	subscribe(outdoor, "temperature", outdoorHandler)
	subscribe(outdoor, "humidity", outdoorHandler)
}

def routineChanged(evt) {
	if (evt.displayName == "ERV Ventilation") {
		log.debug "${evt.descriptionText}"
		def operationTime = minutes * 60 //n minutes * 60 seconds
		runIn(operationTime, turnOffERV)
	}
}

def turnOffERV() {
	log.debug "Turning off ERV switches."
	if (erv.currentSwitch == "on") { erv.off() }
    if (hilo.currentSwitch == "on") { hilo.off() }
    return true
}

def lowSpeedERV() {
	log.debug "Putting ERV in low speed mode."
	if (erv.currentSwitch == "off") { erv.on() }
    if (hilo.currentSwitch == "on") { hilo.off() }
    return true
}

def highSpeedERV() {
	log.debug "Putting ERV in high speed mode."
	if (erv.currentSwitch == "off") { erv.on() }
    if (hilo.currentSwitch == "off") { hilo.on() }
    return true
}

def periodicVentilation() {

	def DP = getCurrentDewPoint()
	// Check outdoor Dew Point, and if it is above threshold, don't turn on the ERV.
	if ( DP < getDewPointThreshold() ) {
        lowSpeedERV()
		runIn(60 * findRunTime(), turnOffERV)
	} else {
		log.debug("Dew Point is too high for ventilation.")
        //sendPush("Dew Point is ${DP}, not ventilating this hour.")
		if (erv.currentSwitch == "on" ) { turnOffERV() }
		return false
	}
}

def outdoorHandler(evt) {
	//Calculate dew point
    def T = outdoor.currentValue("temperature")
    def RH = outdoor.currentValue("humidity")
    def DP = T - ( 9/25 * (100-RH) )
    log.debug("Current outdoor Dew Point is $DP")
    
    state.dewpoint = DP
    state.outtemp = T
    state.outrh = RH
    state.lastForecast = now() / 1000
    state.forecastSource = "Sensor"
}

def getCurrentDewPoint() {
	def lastForecast = state.lastForecast
	def staleThreshold = ( now() / 1000 ) - ( 60 * 45 )
	def DP
	if ( lastForecast < staleThreshold ) {
    	//sendPush("Application state is stale.")
		DP = getWebForecast()
	} else {
		DP = state.dewpoint
	}
	
	DP
}

def getDewPointThreshold() {
	(dewpoint != null && dewpoint !="") ? dewpoint : 70
}

//Validate runtime in minutes
private findRunTime() {
	if (pminutes > 60) {
		return 60
    } else if (pminutes < 1 ) {
    	return 1 
    } else {
    	return pminutes
    }
}

def getWebForecast() {
	def forecastURL = "https://api.forecast.io/forecast/${appSettings.apikey}/${appSettings.latitude},${appSettings.longitude}"
	def responseTime
	def responseDewPoint
	def responseTemp
	def responseRH
	
	log.debug "Checking Forecast.io weather."
    //sendPush "Making an API call to Forecast.io."

	httpGet(forecastURL) { response ->
		if (response.data) {
			responseTime = response.data.currently.time.intValue()
			responseDewPoint = response.data.currently.dewPoint.floatValue()
			responseTemp = response.data.currently.temperature.floatValue()
			responseRH = response.data.currently.humidity.floatValue()
			
			state.lastForecast = responseTime
			state.forecastSource = "Web"
			
			state.dewpoint = responseDewPoint
			state.outtemp = responseTemp
			state.outrh = responseRH
		} else {
			responseDewPoint = 100
			
			state.forecastSource = "Unavailable"
			state.dewpoint = responseDewPoint
			
			log.debug "HttpGet Response data unsuccessful"
            sendPush("API call to Forecast.io didn't return data.")
		}
	}
	responseDewPoint
}

