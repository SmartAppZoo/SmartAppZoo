/**
 *  Blue Iris Control - Cameras
 *  (Child app.  Parent app is: "Blue Iris Control")
 *
 *  Created by Belgarion (programmer_dave@yahoo.com)
 *
 *  Github: https://github.com/dguindon/mySmartThings/tree/master/smartapps/belgarion/blue-iris-control-cameras.src
 *
 *  PARENT APP CAN BE FOUND ON GITHUB: https://github.com/dguindon/mySmartThings/tree/master/smartapps/belgarion/blue-iris-control.src
 *
 *
 *  Based on work by:
 *  flyjmz at https://community.smartthings.com/t/release-blue-iris-fusion-integrate-smartthings-and-blue-iris/54226
 *  Tony Gutierrez in "Blue Iris Profile Integration"
 *  jpark40 at https://community.smartthings.com/t/blue-iris-profile-trigger/17522/76
 *  luma at https://community.smartthings.com/t/blue-iris-camera-trigger-from-smart-things/25147/9
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
 *  Release History:
 *    2017-04-27: v1.0.0 = Initial release.
 *
 * TODO:
 */

definition(
    name: "Blue Iris Control - Cameras",
    namespace: "belgarion",
    author: "programmer_dave@yahoo.com",
    parent: "belgarion:Blue Iris Control",
    description: "### Do not install! ###  This is the Blue Iris software Child SmartApp and will be called during setup within the 'Blue Iris Control' parent SmartApp.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo.png",
    iconX2Url: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo@3x.png")

preferences {
    page(name: "mainPage", title: "", install: true, uninstall: true)
    page(name: "certainTime")
}

def mainPage() {
    return dynamicPage(name: "mainPage", title: "") {
        section("Blue Iris Camera Name"){
            paragraph "Use the Blue Iris short name for the camera (Note: it is cAsE-sEnSiTiVe)."
            input "biCamera", "text", title: "Camera Name", required: true
        }
        section("Select 'Trigger' events"){
            input "myMotion", "capability.motionSensor", title: "Motion Sensors Active", required: false, multiple: true
            input "myContact", "capability.contactSensor", title: "Contact Sensors Opening", required: false, multiple: true
            input "mySwitch", "capability.switch", title: "Switches Turning On", required: false, multiple: true
            input "myAlarm", "capability.alarm", title: "Alarm Activated", required: false, multiple: true
            paragraph "Note: Only the Active/Open/On events will send a Trigger.  Motion stopping, Contacts closing, and Switches turning off will not send a trigger."
        }
        section("Notifications") {
            paragraph "You can choose to receive notifications for this trigger.  Message delivery matches your settings in the parent 'Blue Iris Control' app.  You can also choose to receive status notifications within the SmartThings Notifications tab (errors will always be displayed there)."
            def receiveAlerts = false
            input "receiveAlerts", "bool", title: "Receive Push/SMS Alerts?"
            def receiveNotifications = false
            input "receiveNotifications", "bool", title: "Receive Notifications?"
        }
        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        section("") {
            input "customTitle", "text", title: "Assign a Name", required: true
        }
    }
}

def certainTime() {
    dynamicPage(name:"certainTime",title: "Only during a certain time", uninstall: false) {
        section() {
            input "startingX", "enum", title: "Starting at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
            if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false
            else {
                if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
            }
        }

        section() {
            input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
            if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false
            else {
                if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
            }
        }
    }
}

def installed() {
    if (parent.loggingOn) log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
    app.updateLabel("${customTitle}")
}

def updated() {
    if (parent.loggingOn) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribeToEvents()
    app.updateLabel("${customTitle}")
}

def subscribeToEvents() {
    subscribe(myMotion, "motion.active", eventHandlerBinary)
    subscribe(myContact, "contact.open", eventHandlerBinary)
    subscribe(mySwitch, "switch.on", eventHandlerBinary)
    subscribe(myAlarm, "alarm.strobe", eventHandlerBinary)
    subscribe(myAlarm, "alarm.siren", eventHandlerBinary)
    subscribe(myAlarm, "alarm.both", eventHandlerBinary)
}

def eventHandlerBinary(evt) {
    if (parent.loggingOn) log.debug "Processed event '${evt.name}' from device '${evt.displayName}' with value '${evt.value}' and data '${evt.data}'"
    if (allOk) {
        if (parent.loggingOn) log.debug "Event occured within the desired timing conditions, triggering..."
        if (!receiveAlerts && receiveNotifications) sendNotificationEvent("'${evt.displayName}' is '${evt.value}', Blue Iris Control is triggering camera '${biCamera}'")
        if (receiveAlerts) parent.send("'${evt.displayName}' is '${evt.value}', Blue Iris Control is triggering camera '${biCamera}'")
        if (parent.localOnly) localTrigger()
        else externalTrigger()
    } else if (parent.loggingOn) log.debug "Event did not occur within the desired timing conditions, NOT triggering"
}

def localTrigger() {
    if (parent.loggingOn) log.debug "Running localTrigger..."
    def biHost = "${parent.host}:${parent.port}"
    def biRawCommand = "/admin?camera=${biCamera}&trigger&user=${parent.username}&pw=${parent.password}"
    if (parent.loggingOn) log.debug "Sending GET to URL '$biHost/$biRawCommand'"
    def httpMethod = "GET"
    def httpRequest = [
        method:     httpMethod,
        path:       biRawCommand,
        headers:    [
                    HOST:       biHost,
                    Accept:     "*/*",
                    ]
        ]
    def hubAction = new physicalgraph.device.HubAction(httpRequest)
    sendHubCommand(hubAction)
    //todo - add error notification (but need a way to check for error first!)
}

def externalTrigger() {
    if (parent.loggingOn) log.debug "Running externalTrigger..."
    def errorMsg = "Blue Iris Control could not trigger '${biCamera}'"
    try {
        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"login"]) { response ->
            if (parent.loggingOn) log.debug response.data
            if (parent.loggingOn) log.debug "Logging in..."
            if (response.data.result == "fail") {
               if (parent.loggingOn) log.debug "BI_Inside initial call fail, proceeding to login..."
               def session = response.data.session
               def hash = parent.username + ":" + response.data.session + ":" + parent.password
               hash = hash.encodeAsMD5()
               httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                    if (response2.data.result == "success") {
                        if (parent.loggingOn) log.debug ("BI_Logged In")
                        if (parent.loggingOn) log.debug response2.data
                        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                            if (parent.loggingOn) log.debug ("BI_Retrieved Status")
                            if (parent.loggingOn) log.debug response3.data
                            if (response3.data.result == "success"){
                                    httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"trigger","camera":biCamera,"session":session]) { response4 ->
                                        if (parent.loggingOn) log.debug response4.data
                                        if (parent.loggingOn) log.debug "camera triggerd"
                                        if (response4.data.result == "success") {
                                            httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                                if (parent.loggingOn) log.debug response5.data
                                                if (parent.loggingOn) log.debug "Logged out"
                                            }
                                        } else {
                                            if (parent.loggingOn) log.debug "BI_FAILURE, NOT triggered"
                                            if (parent.loggingOn) log.debug(response4.data.data.reason)
                                            if (!receiveAlerts) sendNotificationEvent(errorMsg)
                                            if (receiveAlerts) parent.send(errorMsg)
                                        }
                                    }
                            } else {
                                if (parent.loggingOn) log.debug "BI_FAILURE, didn't receive status"
                                if (parent.loggingOn) log.debug(response3.data.data.reason)
                                if (!receiveAlerts) sendNotificationEvent(errorMsg)
                                if (receiveAlerts) parent.send(errorMsg)
                            }
                        }
                    } else {
                        if (parent.loggingOn) log.debug "BI_FAILURE, didn't log in"
                        if (parent.loggingOn) log.debug(response2.data.data.reason)
                        if (!receiveAlerts) sendNotificationEvent(errorMsg)
                        if (receiveAlerts) parent.send(errorMsg)
                    }
                }
            } else {
                if (parent.loggingOn) log.debug "FAILURE"
                if (parent.loggingOn) log.debug(response.data.data.reason)
                if (!receiveAlerts) sendNotificationEvent(errorMsg)
                if (receiveAlerts) parent.send(errorMsg)
            }
        }
    } catch(Exception e) {
        if (parent.loggingOn) log.debug(e)
        if (!receiveAlerts) sendNotificationEvent(errorMsg)
        if (receiveAlerts) parent.send(errorMsg)
    }
}

private getAllOk() {
    modeOk && daysOk && timeOk
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    log.trace "daysOk = $result"
    return result
}

private getTimeOk() {
    def result = true
    if ((starting && ending) ||
    (starting && endingX in ["Sunrise", "Sunset"]) ||
    (startingX in ["Sunrise", "Sunset"] && ending) ||
    (startingX in ["Sunrise", "Sunset"] && endingX in ["Sunrise", "Sunset"])) {
        def currTime = now()
        def start = null
        def stop = null
        def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
        if(startingX == "Sunrise") start = s.sunrise.time
        else if(startingX == "Sunset") start = s.sunset.time
        else if(starting) start = timeToday(starting,location.timeZone).time
        s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
        if(endingX == "Sunrise") stop = s.sunrise.time
        else if(endingX == "Sunset") stop = s.sunset.time
        else if(ending) stop = timeToday(ending,location.timeZone).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
    log.trace "timeOk = $result"
    return result
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    return result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private hideOptionsSection() {
    (starting || ending || days || modes || startingX || endingX) ? false : true
}

private offset(value) {
    def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}

private timeIntervalLabel() {
    def result = ""
    if (startingX == "Sunrise" && endingX == "Sunrise") result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunrise" + offset(endSunriseOffset)
    else if (startingX == "Sunrise" && endingX == "Sunset") result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunset" + offset(endSunsetOffset)
    else if (startingX == "Sunset" && endingX == "Sunrise") result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunrise" + offset(endSunriseOffset)
    else if (startingX == "Sunset" && endingX == "Sunset") result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunset" + offset(endSunsetOffset)
    else if (startingX == "Sunrise" && ending) result = "Sunrise" + offset(startSunriseOffset) + " to " + hhmm(ending, "h:mm a z")
    else if (startingX == "Sunset" && ending) result = "Sunset" + offset(startSunsetOffset) + " to " + hhmm(ending, "h:mm a z")
    else if (starting && endingX == "Sunrise") result = hhmm(starting) + " to " + "Sunrise" + offset(endSunriseOffset)
    else if (starting && endingX == "Sunset") result = hhmm(starting) + " to " + "Sunset" + offset(endSunsetOffset)
    else if (starting && ending) result = hhmm(starting) + " to " + hhmm(ending, "h:mm a z")
}
