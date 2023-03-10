/**
 *  Verisure
 *
 *  Copyright 2017 Martin Carlsson
 *  Based upon the work by Anders Sveen (https://github.com/anderssv/smartthings-code)
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
 *  CHANGE LOG
 *  - 0.1   - 
 *
 *
 * Version: 0.1
 *
 */
definition(
        name: "Verisure",
        namespace: "orecus",
        author: "Martin Carlsson",
        description: "Lets you trigger automations whenever your Verisure alarm changes state or temperature changes.",
        category: "Safety & Security",
        iconUrl: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg",
        iconX2Url: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg",
        iconX3Url: "https://pbs.twimg.com/profile_images/448742746266677248/8RSgcRVz.jpeg") {
}


preferences {
    page(name: "setupPage")
}

def setupPage() {
    dynamicPage(name: "setupPage", title: "Configure Verisure", install: true, uninstall: true) {

        section("Authentication") {
            input "username", "text", title: "Username"
            input "password", "password", title: "Password"
        }

        def actions = location.helloHome?.getPhrases()*.label
        actions.sort()

        section("Action when disarmed") {
            input "unarmedAction", "enum", title: "Action for unarmed", options: actions, required: false
            input "armedAction", "enum", title: "Action for armed", options: actions, required: false
            input "armedHomeAction", "enum", title: "Action for armed home", options: actions, required: false
        }

        section("Errors and logging") {
            input "logUrl", "text", title: "Splunk URL to log to", required: false
            input "logToken", "text", title: "Splunk Authorization Token", required: false
        }
    }
}

def installed() {
    debug("[verisure.installed]")
    initialize()
}

def updated() {
    debug("[verisure.updated]")
    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    debug("[verisure.uninstalling]")
    removeChildDevices(getChildDevices())
}

def initialize() {
    state.app_version = "0.1"
    try {
        debug("[verisure.initialize] Verifying Credentials")
        updateAlarmState()
        debug("[verisure.scheduling]")
        schedule("0 0/1 * * * ?", checkPeriodically)
    } catch (e) {
        error("[verisure.initialize] Could not initialize app", e)
    }
}

def getAlarmState() {
    debug("[verisure.alarmState] Retrieving cached alarm state")
    return state.previousAlarmState
}

def checkPeriodically() {
    debug("[verisure.checkPeriodically] Periodic check from timer")
    try {
        updateAlarmState()
    } catch (Exception e) {
        error("[verisure.checkPeriodically] Error updating alarm state", e)
    }
}

def updateAlarmState() {
    def baseUrl = "https://mypages.verisure.com"
    def loginUrl = baseUrl + "/j_spring_security_check?locale=en_GB"

    def alarmState = null
    def climateState = null

    def childDevices = getChildDevices()

    def sessionCookie = login(loginUrl)
    alarmState = getAlarmState(baseUrl, sessionCookie)
    climateState = getClimateState(baseUrl, sessionCookie)

    debug("[childDevices.found] " + childDevices)
    if (state.previousAlarmState == null) { state.previousAlarmState = alarmState }

    //Add or Update Sensors
    climateState.each {climateDevice ->
        def existingDevice = getChildDevice(climateDevice.id)
        if(!existingDevice) {
            addChildDevice(app.namespace, "Verisure Sensor", climateDevice.id, null, [label: climateDevice.location, timestamp: climateDevice.timestamp, humidity: climateDevice.humidity, type: climateDevice.type, temperature: climateDevice.temperature])
            debug("[climateDevice.created] " + climateDevice)
        } else {           
            if (climateDevice.humidity != "") {
                debug("[climateDevice.updated] " + climateDevice.location + " | Humidity: " + climateDevice.humidity.substring(0,4) + " | Temperature: " + climateDevice.temperature.substring(0,4))
                existingDevice.sendEvent(name: "humidity", value: climateDevice.humidity.substring(0,4))
            } else {
                debug("[climateDevice.updated] " + climateDevice.location + " | Humidity: " + "0" + " | Temperature: " + climateDevice.temperature.substring(0,4))
                existingDevice.sendEvent(name: "humidity", value: "0")
            }

            existingDevice.sendEvent(name: "timestamp", value: climateDevice.timestamp)
            existingDevice.sendEvent(name: "type", value: climateDevice.type)
            existingDevice.sendEvent(name: "temperature", value: climateDevice.temperature.substring(0,4))
        }
    }

    //Add & update main alarm

    def alarmDevice = getChildDevice('verisure-alarm')

    if(!alarmDevice) {
        debug("[alarmDevice.created] " + alarmDevice)
        addChildDevice(app.namespace, "Verisure Alarm", "verisure-alarm", null, [status: alarmState.status, loggedBy: alarmState.name, loggedWhen: alarmState.date])  
    } else {
        debug("[alarmDevice.updated] " + alarmDevice + " | Status: " + alarmState.status + " | LoggedBy: " + alarmState.name + " | LoggedWhen: " + alarmState.date)
        alarmDevice.sendEvent(name: "status", value: alarmState.status)
        alarmDevice.sendEvent(name: "loggedBy", value: alarmState.name)
        alarmDevice.sendEvent(name: "loggedWhen", value: alarmState.date)
    }

    if (alarmState.status != state.previousAlarmState.status) {
        debug("[verisure.updateAlarmState] State changed, execution actions")
        state.previousAlarmState = alarmState
        triggerActions(alarmState.status)
    }
}

def triggerActions(alarmState) {
    if (alarmState == "armed" && armedAction) {
        executeAction(armedAction)
    } else if (alarmState == "unarmed" && unarmedAction) {
        executeAction(unarmedAction)
    } else if (alarmState == "armedhome" && armedHomeAction) {
        executeAction(armedHomeAction)
    }
}

def executeAction(action) {
    debug("[verisure.executeAction] Executing action ${action}")
    location.helloHome?.execute(action)
}


def login(loginUrl) {
    def params = [
            uri               : loginUrl,
            requestContentType: "application/x-www-form-urlencoded",
            body              : [
                    j_username: username,
                    j_password: password
            ]
    ]

    httpPost(params) { response ->
        if (response.status != 200) {
            throw new IllegalStateException("Could not authenticate. Got response code ${response.status} . Is the username and password correct?")
        }

        def cookieHeader = response.headers.'Set-Cookie'
        if (cookieHeader == null) {
            throw new RuntimeException("Could not get session cookie! ${response.status} - ${response.data}")
        }

        return cookieHeader.split(";")[0]
    }
}

def getAlarmState(baseUrl, sessionCookie) {
    def alarmParams = [
            uri    : baseUrl + "/remotecontrol",
            headers: [
                    Cookie: sessionCookie
            ]
    ]

    return httpGet(alarmParams) { response ->
        //debug("[Alarm] Response from Verisure: " + response.data)
        return response.data.findAll { it."type" == "ARM_STATE" }[0]
    }
}

def getClimateState(baseUrl, sessionCookie) {
    def alarmParams = [
            uri    : baseUrl + "/overview/climatedevice",
            headers: [
                    Cookie: sessionCookie
            ]
    ]

    return httpGet(alarmParams) { response ->
        //debug("[Climate] Response from Verisure: " + response.data)
        return response.data
    }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private error(text, e) {
    log.error(text, e)
    if (logUrl) {
        httpLog("error", text, e)
    }
}

private debug(text) {
    log.debug(text)
    if (logUrl) {
        httpLog("debug", text, null)
    }
}

private httpLog(level, text, e) {
    def message = text
    if (e) {
        message = message + "\n" + e
    }

    def time = new Date()

    def json_body = [
            time : time.getTime(),
            host : location.id + ".smartthings.com",
            event: [
                    time       : time.format("E MMM dd HH:mm:ss.SSS z yyyy"),
                    smartapp_id: app.id,
                    location_id: location.id,
                    namespace  : app.namespace,
                    app_name   : app.name,
                    app_version: state.app_version,
                    level      : level,
                    message    : message
            ]
    ]

    def json_params = [
            uri    : logUrl,
            headers: [
                    'Authorization': "Splunk ${logToken}"
            ],
            body   : json_body
    ]

    try {
        httpPostJson(json_params)
    } catch (logError) {
        log.warn("Could not log to remote http", logError)
    }
}