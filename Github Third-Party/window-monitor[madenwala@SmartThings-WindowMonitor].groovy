/**
 *  Window Monitor
 *
 *  Copyright 2019 Mohammed Adenwala
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
    name: "Window Monitor",
    namespace: "madenwala",
    author: "Mohammed Adenwala",
    description: "Hour weather forecasts to detect upcoming precipitation and remind you to close any open windows.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/madenwala/SmartThings-WindowMonitor/master/icons/Icon.png",
    iconX2Url: "https://raw.githubusercontent.com/madenwala/SmartThings-WindowMonitor/master/icons/Icon@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/madenwala/SmartThings-WindowMonitor/master/icons/Icon@3x.png")

def APP_NAME = "WindowMonitor"

preferences {
    section("AccuWeather") {
    	paragraph "This smart app requires you to have your own developer account from AccuWeather's developer website. The free limited-trial account should provide you plenty of credits per day to run this app."
        href(
             title: "AccuWeather API Key",
             style: "external",
             url: "https://developer.accuweather.com/user/me/apps",
             description: "Register with AccuWeather to create an API key")             
    	input "accuWeatherApiKey", "text", title: "AccuWeather API Key", required: true, defaultValue: "IDAqoGCKyaIPlMgvr4dGjIos8uOTLqqA"
    }
    section("Settings") {
        input "zipCode", "text", title: "Zip code", required: true
        input "sensors", "capability.contactSensor", title: "Windows to monitor", multiple: true, required: true
    }
    section("Device Options") {
    	paragraph "Enable device notifications when rain is detected while windows are open"
    	input "enableNotifications", "boolean", title: "Send push notifications", defaultValue: true
    }
    section("Audio Notifications") {
    	paragraph "Select any speaker device for audio notifications when rain is detected while windows are open"
        input "audioSpeakers", "capability.audioNotification", title: "Audio Devices", multiple: true, required: false
        input "alexaSpeakers", "device.echoSpeaksDevice", title: "Alexa Devices", multiple: true, required: false, hideWhenEmpty: true
    }
}

def installed() {
	log.debug APP_NAME + ":Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug APP_NAME + ": Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	state.APP_NAME = "WindowMonitor"
    state.LAST_RUN = new Date()
    state.LOCATION_KEY = null
    subscribe(app, appHandler)
    subscribe(sensors, "contact.open", eventHandler)
    state.LOCATION_KEY = getLocationKey()
    log.warn state.APP_NAME + ": LOCATION_KEY for ZipCode ${zipCode}: ${state.LOCATION_KEY}"
    runEvery1Hour(refreshData)
}

def appHandler(evt) {
	log.debug state.APP_NAME + ": App Event ${evt.value} received"
    refreshData(true)
}

def eventHandler(evt) {
	log.debug APP_NAME + ": Sensor opened: ${evt}"
    refreshData(false)
}

def refreshData() {
	refreshData(false)
}

def refreshData(isAppHandler){
	try {
        log.info state.APP_NAME + ": Refresh data..."
        def openSenors = getOpenSensors()
        
        if(state.LOCATION_KEY == null) {
    		state.LOCATION_KEY = getLocationKey()
    		log.warn state.APP_NAME + ": LOCATION_KEY for ZipCode ${zipCode}: ${state.LOCATION_KEY}"
            if(state.LOCATION_KEY == null) {
            	notifications("Could not check weather because location couldn't be determined from the zip code.")
                return
            }
        }

        if(openSensors != null && openSensors.isEmpty() == false) {
            log.debug state.APP_NAME + ": ${openSensors} are open"
            def data = getData()
            def message = getMessage(data, isAppHandler)
            notifications(message)
        } else {
            log.debug state.APP_NAME + ": All contact sensors are closed."
            if(isAppHandler) {
            	notifications("All windows and doors are closed.")
            }
        }
    } catch(ex) {
    	log.error state.APP_NAME + ": Could not refreshData: " + ex
    }
}

def getOpenSensors() {
	def openSensors = null
    openSensors = sensors.findAll {
        it.currentContact == "open"
    }
    return openSensors
}

def getLocationKey() {
	try{
        def jsonUrl = "http://dataservice.accuweather.com/locations/v1/postalcodes/search?q=${zipCode}&apikey=${accuWeatherApiKey}"
        log.debug state.APP_NAME + ": Refresh Zip from ${jsonUrl}"

        def params = [
            uri: jsonUrl,
            contentType: 'application/json'
        ]

        httpGet(params) { resp ->
            if(resp.status == 200){
                // get the data from the response body
                log.debug state.APP_NAME + ": Data: ${resp.data}"
                return resp.data[0].Key;
            } else {
                // get the status code of the response
                log.debug state.APP_NAME + ": Status Code: ${resp.status}"
                return null;
            }  
        }
    } catch(e) {
        if (e.equals("groovyx.net.http.ResponseParseException: Unauthorized")) {
            log.error "User unauthorized, requesting new token"
        }
        else {
            log.error "Something went wrong with the AccuWeather API call $e"
        }
        return null;
    }
}

def getData() {
	try{
        def jsonUrl = "https://dataservice.accuweather.com/forecasts/v1/hourly/12hour/${state.LOCATION_KEY}?apikey=${accuWeatherApiKey}"
        log.debug state.APP_NAME + ": Refresh Data from ${jsonUrl}"

        def params = [
            uri: jsonUrl,
            contentType: 'application/json'
        ]

        httpGet(params) { resp ->
            if(resp.status == 200){
                // get the data from the response body
                log.debug state.APP_NAME + ": Data: ${resp.data[0]}"
                return resp.data;
            } else {
                // get the status code of the response
                log.debug state.APP_NAME + ": Status Code: ${resp.status}"
                return null;
            }  
        }
    } catch(e) {
        if (e.equals("groovyx.net.http.ResponseParseException: Unauthorized")) {
            log.error "User unauthorized, requesting new token"
        }
        else {
            log.error "Something went wrong with the AccuWeather API call $e"
        }
        return null;
    }
}

def getMessage(data, isAppHandler) {
	try {
        def message = null
        if(data == null || data.length == 0){
            message = "I can't tell if it's going to precipitation because I could not access AccuWeather data."
            log.error message    
        } else if(data.size() >= 1 && (data[0].HasPrecipitation || data[0].PrecipitationProbability > 0)) {
            message = "There is a ${data[0].PrecipitationProbability}% chance of precipitation within the hour. Consider keeping the windows closed."
        } else if(data.size() >= 2 && (data[1].HasPrecipitation || data[1].PrecipitationProbability > 0)) {
            message = "There is a ${data[1].PrecipitationProbability}% chance of precipitation after an hour."
        } else if(isAppHandler) {
            message = "No precipitation in the forecast. You're welcome to open windows or doors."
        } else {
            log.info "No precipitation in the current forecast."
        }
        log.debug "Message: " + message;
        return message;
    }
    catch(ex) {
    	log.error state.APP_NAME + ": Could run getMessage: " + ex
        return null
    }
}

def notifications(message) {
    log.info state.APP_NAME + ": Audio Speak: " + message;
    if(message) {
    	try {
            audioSpeakers*.playTextAndRestore(message)
            alexaSpeakers*.playAnnouncement(message)
        } catch(ex) {
    		log.error state.APP_NAME + ": Could play audio on speakers: " + ex
        }        
		if(sendPush && enableNotifications == 'true')
    		sendPush(message)
    }
}