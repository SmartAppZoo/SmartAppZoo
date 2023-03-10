/**
 *  Blue Iris Control
 *  (Parent app.  Child app is: "Blue Iris Control - Cameras")
 *
 *  Created by Belgarion (programmer_dave@yahoo.com)
 *
 *  Github: https://github.com/dguindon/mySmartThings/tree/master/smartapps/belgarion/blue-iris-control.src
 *
 *  CHILD APP CAN BE FOUND ON GITHUB: https://github.com/dguindon/mySmartThings/tree/master/smartapps/belgarion/blue-iris-control-cameras.src
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
    name: "Blue Iris Control",
    namespace: "belgarion",
    author: "programmer_dave@yahoo.com",
    description: "Blue Iris software integration with SmartThings,",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo.png",
    iconX2Url: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/dguindon/mySmartThings/master/resources/Blue-Iris-Logo@3x.png",
    singleInstance: true)

preferences {
    page(name:"BIC_Cameras")
}

def BIC_Cameras() {
    dynamicPage(name:"BIC_Cameras", title: "Triggers", install: true, uninstall: true, submitOnChange: true) {
        section("") {
            app(name: "Blue Iris Control - Cameras", appName: "Blue Iris Control - Cameras", namespace: "belgarion", title: "Add Camera", multiple: true)
        }
        section("Blue Iris Server Login Settings") {
            paragraph "Local or External Connection to Blue Iris Server (i.e. LAN vs WAN)?"
            input "localOnly", "bool", title: "Local connection?", required: true, submitOnChange: true
            if (localOnly) {
                paragraph "NOTE: When using a local connection, you need to ensure 'Secure only' is not checked in the Blue Iris 'Options' -> 'Web server' settings."
                paragraph "Since you're using a local connection, use the local IP address for the 'BI Webserver Host' parameter.  Do not include 'http://', '.com' or anything except for the IP address."
            } else {
                paragraph "Since you're using an external connection, use the external IP address for 'BI Webserver Host' parameter and be sure to include the full address (i.e. include 'http://' or 'https://', '.com', etc)."
                paragraph "If you are using Stunnel, ensure the SSL certificate is from a Certificate Authority (CA).  The SSL certificate can NOT be self-signed!  You can create a free CA signed certificate at 'www.letsencrypt.org'."
            }
            input "host", "text", title: "BI Webserver Host (only include 'http://' if using an external address)", required:true
            input "port", "number", title: "BI Webserver Port (e.g. 81)", required:true
            paragraph "Note: Blue Iris only allows Admin Users to toggle profiles."
            input "username", "text", title: "BI Username", required: true
            input "password", "password", title: "BI Password", required: true
        }
        section("Blue Iris Profile/SmartThings Mode Integration") {
            paragraph "Enter the number (1-7) of the Blue Iris Profile for each of your SmartThings modes.  To ignore a mode, leave it blank.  Entering '0' sets Blue Iris to 'Inactive'.  If you don't want to integrate SmartThings Modes with Blue Iris Profiles, leave them all blank."
            location.modes.each { mode ->
                def modeId = mode.id.toString()
                input "mode-${modeId}", "number", title: "Mode ${mode}", required: false
            }
        }
        section ("Make 'Hold' or 'Temporary' Profile changes?") {
            paragraph "'Hold' changes remain until the next change is made (e.g. you change it or this app does).  'Temporary' changes will only be in effect for the 'Temp Time' duration set for each Profile in Blue Iris Settings > Profiles.  At the end of that 'Temp Time', Blue Iris will change profiles according to your schedule."
            paragraph "Note: if Blue Iris restarts when the Profile was made with a temp change, it will start in whatever Profile your schedule dictates. A hold change will remain even after a restart."
            input "holdTemp", "bool", title: "Make Hold changes?", required: true
        }
        section("Notifications") {
            paragraph "You can choose to receive Push notifications and an SMS message (optional)."
            input("recipients", "contact", title: "Send Push notifications to") {
                input "pushAndPhone", "enum", title: "Also send an SMS message? (optional)", required: false, options: ["Yes", "No"]
                input "phone", "phone", title: "Phone Number (optional; only for SMS)", required: false
                paragraph "If outside the US please make sure to enter the proper country code."
            }
            paragraph "Each trigger can send it's own notifications.  Do you also want to receive notificaitons for Profile changes?"
            input "receiveAlerts", "enum", title: "Receive Profile Change Notifications?", options: ["Yes", "No"], required: true
        }
        section("Debug"){
            paragraph "You can turn on debug logging, which can be viewed in 'Live Logging' on the SmartThings IDE website."
            def loggingOn = false
            input "loggingOn", "bool", title: "Debug Logging On?"
        }
    }
}

def installed() {
    if (loggingOn) log.debug "Installed executed"
    initialize()
}

def updated() {
    if (loggingOn) log.debug "Updated executed"
    unsubscribe()
    initialize()
}

def initialize() {
    //if (loggingOn) log.debug "Initialized with settings: ${settings}"
    if (loggingOn) log.debug "Initialize executed"
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
    if (evt.name != "mode") {return;}
    if (loggingOn) log.debug "BI_modeChange detected. " + evt.value
    def checkMode = ""

    location.modes.each { mode ->
        if (mode.name == evt.value){
            checkMode = "mode-" + mode.id
            if (loggingOn) log.debug "BI_modeChange matched to " + mode.name
        }
    }

    if (checkMode != "" && settings[checkMode]){
        if (loggingOn) log.debug "BI_Found Profile " + settings[checkMode]
        if(localOnly){
            localAction(settings[checkMode].toInteger())
        } else externalAction(settings[checkMode].toInteger())
    }
}

def localAction(profile) {
    def biHost = "${host}:${port}"
    def biRawCommand = "/admin?profile=${profile}&user=${username}&pw=${password}"
    if (loggingOn) log.debug "Changed Blue Iris Profile to ${profile} via GET to URL $biHost/admin?profile=${profile}"
    if(!holdTemp) {
        if(receiveAlerts == "No") sendNotificationEvent("Temporarily changed Blue Iris to Profile ${profile}")
        if(receiveAlerts == "Yes") send("Temporarily changed Blue Iris to Profile ${profile}")
    }
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
    if(holdTemp) {
        sendHubCommand(hubAction)
        if(receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: 'Hold' changed Blue Iris to Profile ${profile}")
        if(receiveAlerts == "Yes") send("Blue Iris Control: 'Hold' changed Blue Iris to Profile ${profile}")
    }
    //todo - add error notifications (need to figure out how to check for errors first!)
}

def externalAction(profile) {
    def errorMsg = "Blue Iris Control: Could not adjust the Blue Iris Profile"
    if (!holdTemp) {
        try {
            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
                if (loggingOn) log.debug response.data

                if (response.data.result == "fail") {
                   if (loggingOn) log.debug "BI_Inside initial call fail, proceeding to login"
                   def session = response.data.session
                   def hash = username + ":" + response.data.session + ":" + password
                   hash = hash.encodeAsMD5()

                   httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                        if (response2.data.result == "success") {
                            def BIprofileNames = response2.data.data.profiles
                            if (loggingOn) log.debug ("BI_Logged In")
                            if (loggingOn) log.debug response2.data
                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                                if (loggingOn) log.debug ("BI_Retrieved Status")
                                if (loggingOn) log.debug response3.data
                                if (response3.data.result == "success"){
                                    if (response3.data.data.profile != profile){
                                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                            if (loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                    if (loggingOn) log.debug ("Blue Iris to Profile ${profileName(BIprofileNames,profile)}!")
                                                    if(receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: Temporarily changed Blue Iris to Profile ${profileName(BIprofileNames,profile)}")
                                                    if (receiveAlerts == "Yes") send("Blue Iris Control: Temporarily changed Blue Iris to Profile ${profileName(BIprofileNames,profile)}")
                                                } else {
                                                    if (loggingOn) log.debug ("Blue Iris ended up on Profile ${profileName(BIprofileNames,response4.data.data.profile)}? Temp change to ${profileName(BIprofileNames,profile)}. Check your user permissions.")
                                                    if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: Failed to change Profiles!  It is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                    if (receiveAlerts == "Yes") send("Blue Iris Control: Failed to change Profiles!  It is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                }
                                                httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                                    if (loggingOn) log.debug response5.data
                                                    if (loggingOn) log.debug "Logged out"
                                                }
                                            } else {
                                                if (loggingOn) log.debug "BI_FAILURE"
                                                if (loggingOn) log.debug(response4.data.data.reason)
                                                if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                                if (receiveAlerts == "Yes") send(errorMsg)
                                            }
                                        }
                                    } else {
                                        if (loggingOn) log.debug ("Blue Iris is already in Profile ${profileName(BIprofileNames,profile)}.")
                                        sendNotificationEvent("Blue Iris is already in Profile ${profileName(BIprofileNames,profile)}.")
                                    }
                                } else {
                                    if (loggingOn) log.debug "BI_FAILURE"
                                    if (loggingOn) log.debug(response3.data.data.reason)
                                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                    if (receiveAlerts == "Yes") send(errorMsg)
                                }
                            }
                        } else {
                            if (loggingOn) log.debug "BI_FAILURE"
                            if (loggingOn) log.debug(response2.data.data.reason)
                            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                            if (receiveAlerts == "Yes") send(errorMsg)
                        }
                    }
                } else {
                    if (loggingOn) log.debug "FAILURE"
                    if (loggingOn) log.debug(response.data.data.reason)
                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                    if (receiveAlerts == "Yes") send(errorMsg)
                }
            }
        } catch(Exception e) {
            if (loggingOn) log.debug(e)
            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
            if (receiveAlerts == "Yes") send(errorMsg)
        }
    } else {
        try {
            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
                if (loggingOn) log.debug response.data

                if (response.data.result == "fail") {
                   if (loggingOn) log.debug "BI_Inside initial call fail, proceeding to login"
                   def session = response.data.session
                   def hash = username + ":" + response.data.session + ":" + password
                   hash = hash.encodeAsMD5()

                   httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                        if (response2.data.result == "success") {
                            def BIprofileNames = response2.data.data.profiles
                            if (loggingOn) log.debug ("BI_Logged In")
                            if (loggingOn) log.debug response2.data
                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                                if (loggingOn) log.debug ("BI_Retrieved Status")
                                if (loggingOn) log.debug response3.data
                                if (response3.data.result == "success"){
                                    if (response3.data.data.profile != profile){
                                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                            if (loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                if (loggingOn) log.debug "Set Profile to ${profileName(BIprofileNames,profile)} via temp change, trying to set via hold"
                                                if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response5 ->
                                                        if (loggingOn) log.debug response5.data
                                                        if (response5.data.result == "success") {
                                                            if (loggingOn) log.debug ("Set Profile to ${profileName(BIprofileNames,profile)} with a hold change!")
                                                            if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: Hold changed Blue Iris to Profile ${profileName(BIprofileNames,profile)}")
                                                            if (receiveAlerts == "Yes") send("Blue Iris Control: Hold changed Blue Iris to Profile ${profileName(BIprofileNames,profile)}")
                                                        } else {
                                                            if (loggingOn) log.debug ("Blue Iris Control: Failed to 'Hold' Profile!  It is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                            if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: Failed to 'Hold' Profile!  It is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                            if (receiveAlerts == "Yes") send("Blue Iris Control: Failed to 'Hold' Profile!  It is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                        }
                                                   }
                                                } else {
                                                    if (loggingOn) log.debug ("Blue Iris ended up on Profile ${profileName(BIprofileNames,response4.data.data.profile)}? Attempt to set ${profileName(BIprofileNames,profile)} failed, also unable to attempt hold. Check your user permissions.")
                                                    if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Control: Failed to change Profiles!  It is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                    if (receiveAlerts == "Yes") send("Blue Iris Control: Failed to change Profiles!  It is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                }
                                                httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response6 ->
                                                    if (loggingOn) log.debug response6.data
                                                    if (loggingOn) log.debug "Logged out"
                                                }
                                            } else {
                                                if (loggingOn) log.debug "BI_FAILURE"
                                                if (loggingOn) log.debug(response4.data.data.reason)
                                                if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                                if (receiveAlerts == "Yes") send(errorMsg)
                                            }
                                        }
                                    } else {
                                        if (loggingOn) log.debug ("Blue Iris is already in Profile ${profileName(BIprofileNames,profile)}.")
                                        sendNotificationEvent("Blue Iris is already in Profile ${profileName(BIprofileNames,profile)}.")
                                        }
                                } else {
                                    if (loggingOn) log.debug "BI_FAILURE"
                                    if (loggingOn) log.debug(response3.data.data.reason)
                                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                    if (receiveAlerts == "Yes") send(errorMsg)
                                }
                            }
                        } else {
                            if (loggingOn) log.debug "BI_FAILURE"
                            if (loggingOn) log.debug(response2.data.data.reason)
                            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                            if (receiveAlerts == "Yes") send(errorMsg)
                        }
                    }
                } else {
                    if (loggingOn) log.debug "FAILURE"
                    if (loggingOn) log.debug(response.data.data.reason)
                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                    if (receiveAlerts == "Yes") send(errorMsg)
                }
            }
        } catch(Exception e) {
            if (loggingOn) log.debug(e)
            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
            if (receiveAlerts == "Yes") send(errorMsg)
        }
    }
}

def profileName(names, num) {
    if (names[num.toInteger()]) {
        names[num.toInteger()] + " (#${num})"
    } else {
        '#' + num
    }
}

private send(msg) {
    if (location.contactBookEnabled) {
        if (loggingOn) log.debug("Sending notifications to ${recipients?.size()} contact(s)")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        Map options = [:]
        if (phone) {
            options.phone = phone
            if (loggingOn) log.debug 'sending SMS'
        } else if (pushAndPhone == 'Yes') {
            options.method = 'both'
            options.phone = phone
        } else options.method = 'push'
        sendNotification(msg, options)
    }
    if (loggingOn) log.debug msg
}
