/**
 *  Thingspeak Wattage Logger for HEM
 *
 *  Copyright 2015 Brian Wilson
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
 *  For documentation and updates, please see: https://github.com/bdwilson/ThingSpeak-Energy-Logger
 *
 */
definition(
    name: "Thingspeak Energy Logger",
    namespace: "bdwilson",
    author: "Brian Wilson",
    description: "Thingspeak Logger",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Log devices...") {
        input "power", "capability.powerMeter", title: "Power", required: false, multiple: true
        // input "energy", "capability.energyMeter", title: "Energy", required: false, multiple: true
    }

    section ("ThinkSpeak channel id...") {
        input "channelId", "number", title: "Channel id"
    }

    section ("ThinkSpeak read key...") {
        input "readKey", "text", title: "Read key"
    }

    section ("ThinkSpeak write key...") {
        input "writeKey", "text", title: "Write key"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(power, "power", handlePowerEvent)
    //subscribe(energy, "energy", handleEnergyEvent)


    updateChannelInfo()
    log.debug state.fieldMap
}

def handleEnergyEvent(evt) {
	logField(evt,"energy") { it.toString() }
}

def handlePowerEvent(evt) {
    logField(evt,"power") { it.toString() }

}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    log.debug "Retrieving channel info for ${fieldMap}"
    return fieldMap
}

private updateChannelInfo() {
    log.debug "Retrieving channel info for ${channelId}"

    def url = "https://api.thingspeak.com/channels/${channelId}/feed.json?key=${readKey}&results=0"
    httpGet(url) {
        response ->
        if (response.status != 200 ) {
            log.debug "ThingSpeak data retrieval failed, status = ${response.status}"
        } else {
            state.channelInfo = response.data?.channel
        }
    }

    state.fieldMap = getFieldMap(state.channelInfo)
}

private logField(evt, field, Closure c) {
    def deviceName = evt.displayName.trim() + '.' + field
    def fieldNum = state.fieldMap[deviceName]

    if (!fieldNum) {
        log.debug "Device '${deviceName}' has no field"
        return
    }
    def value = c(evt.value)
    log.debug "Logging to channel ${channelId}, ${fieldNum}, value ${value}"

    def url = "https://api.thingspeak.com/update?key=${writeKey}&${fieldNum}=${value}"
    httpGet(url) { 
        response -> 
        if (response.status != 200 ) {
            log.debug "ThingSpeak logging failed, status = ${response.status}"
        }
    }
}
