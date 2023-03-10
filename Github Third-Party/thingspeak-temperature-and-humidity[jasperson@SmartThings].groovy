/**
 *  ThingSpeak Temperature and Humidity
 *
 *  Copyright 2017 J.R. Jasperson
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
    name: "ThingSpeak Temperature and Humidity",
    namespace: "jasperson",
    author: "J.R. Jasperson",
    description: "ThingSpeak integration to track and visualize temperature and humidity",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn@3x.png")


// Presented to user on app installation/update for configuration
preferences {
    section("Devices") {
        input "tempDev", "capability.temperatureMeasurement", title: "Temperature", required:true, multiple: false
        input "RHDev", "capability.relativeHumidityMeasurement", title: "Humidity", required:true, multiple: false
    }

    section ("ThingSpeak Channel ID") {
        input "channelID", "number", title: "Channel ID"
    }

    section ("ThingSpeak Write Key") {
        input "channelKey", "text", title: "Channel Key"
    }
}

// Invoked on app install
def installed() {
    initialize()
}

// Invoked on app update/save
def updated() {
    unsubscribe()
    initialize()
}

// Invoked by installed() and updated()
def initialize() {
	schedule("0 0 0/3 1/1 * ? *", handleSchedule) // Every 3 hours
    updateChannelInfo()
}

// Invoked via the schedule() set in initialize()
def handleSchedule(){
    def tempCurrent = tempDev.currentValue("temperature")
    def tempField = state.fieldMap["temperature"]
    def RHCurrent = RHDev.currentValue("humidity")
    def RHField = state.fieldMap["humidity"]
    
    def url = "https://api.thingspeak.com/update?api_key=${channelKey}&${tempField}=${tempCurrent}&${RHField}=${RHCurrent}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            send("ThingSpeak logging failed, status = ${response.status}")
        }
    }
}

// Invoked by updateChannelInfo()
private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    return fieldMap
}

// Invoked by initialize()
private updateChannelInfo() {
    send("Retrieving channel info for ${channelID}")

    def url = "https://api.thingspeak.com/channels/${channelID}/feeds.json?key=${channelKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            send("ThingSpeak data retrieval failed, status = ${response.status}")
        } else {
            state.channelInfo = response.data?.channel
        }
    }
    state.fieldMap = getFieldMap(state.channelInfo)
}

private send(msg){
	// log levels: [trace, debug, info, warn, error, fatal]
	sendNotificationEvent(msg)			// sendNotificationEvent() displays a message in Hello, Home, but does not send a push notification or SMS message.
    log.debug("${app.label}: ${msg}")
}