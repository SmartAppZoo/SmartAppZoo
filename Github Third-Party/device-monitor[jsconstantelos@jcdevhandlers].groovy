/**
 *  Device Monitor
 *
 *  Copyright 2016 Eric Maycock
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
 *  Contributors
 *  ----------------
 *  Based off of code from J.Constantelos (https://github.com/constjs)
 *  Some code modifications provided by "Craig.K.Lyons" (https://github.com/lyons189)
 *
 *  Overview
 *  
 *  This app checks the selected devices to determine if they are comunicating with the SmartThings Hub. 
 *  It checks events from the devices to determine which devices are in a particular state. It works best with sensors, 
 *  and devices that are supposed to report to SmartThings on an occasional basis. It may not work well on devices 
 *  that are sent commands in Routines and SmartApps (i.e. lights - if the devices send events based on a command rather than on a parse). 
 *  I will look into improving this process as time goes on.
 *
 *  Options:
 *
 *  - How long (in hours) before a device is considered inactive.
 *
 *  Check Options:
 *
 *  - Schedule a check either once a day or at certain intervals (5,10,15,30 minutes or 1,3 hours). 
 *    The SmartThings platform will only allow one of these at a time so if you switch from one to another make sure you disable the other one.
 *  - Run a check each time one of the devices sends an event. This can be resource intensive if you have a lot of devices. 
 *    If you do, make sure you set the next option.
 *  - Minimum time between checks. If you have a lot of devices and the above option is true, set this to prevent checks 
 *    from occuring too often. For example, if device A sends an event and 2 seconds later device B sends an event, 
 *    you don't want to run the app each time. I find that 15 minutes or more is more than sufficient for the app.
 *
 *  - To prevent the app from sending notifications when you don't want to get them (4am for example) make sure to use the restriction options.
 *
 *  - Only during a certain time.
 *  - Only on certain days of the week.
 *  - Only when mode is . . .
 *
 *  2016-08-21 - Lot's of changes. Layout has been reorganized to make navigation easier. Battery alerts available. You can
 *               exclude devices from event checks or battery checks. New logo. Internal bug fixes and optimizations. 
 *
 *  2016-04-02 - Changed how the app looks for events. It only looks at "DEVICE" events. These are meant to indicate that
 *               the event came from the device. This should make the app much more accurate.
 *               Also, the ability to get a notification when the device is back online has been added.
 *  2016-03-03 - Initial Release
 */
definition(
    name: "Device Monitor",
    namespace: "jscgs350",
    author: "Eric Maycock",
    description: "SmartApp that monitors selected devices and will send a notification if one has stopped reporting.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/erocm123/SmartThingsPublic/master/smartapps/erocm123/device-monitor.src/device-monitor-icon.png",
    iconX2Url: "https://raw.githubusercontent.com/erocm123/SmartThingsPublic/master/smartapps/erocm123/device-monitor.src/device-monitor-icon-2x.png",
    iconX3Url: "https://raw.githubusercontent.com/erocm123/SmartThingsPublic/master/smartapps/erocm123/device-monitor.src/device-monitor-icon-3x.png")


preferences {

    page name: "pageMain"
    page name: "pageStatus"
    page name: "pageConfigure"
    page name: "pageSettings"
    page name: "pageExclusions"
    page name: "refreshing"
    page(name: "timeIntervalInput", title: "Only during a certain time") {
        section {
            input "starting", "time", title: "Starting", required: false
            input "ending", "time", title: "Ending", required: false
        }
    }
}

//***************************
//Show Main Page
//***************************
def pageMain() {

    def pageProperties = [
        name: "pageMain",
        title: "Device Monitor",
        nextPage: null,
        install: true,
        uninstall: true
    ]
    
    def helpPage = "Select devices that you wish to check with this SmartApp. These devices will get checked at the desired intervals and notifications will be sent based on the settings specified"
    
    return dynamicPage(pageProperties) {
    section("About This App") {
            paragraph helpPage
            
        }
    section("Menu") {
            href "pageStatus", title: "Device Status", description: "Tap to see the status of devices", params: [refresh: true]
            href "pageConfigure", title: "Configure Devices", description: "Tap to manage your list of devices"
            href "pageSettings", title: "Settings", description: "Tap to manage app settings"
            href "pageExclusions", title: "Exclusions", description: "Tap to manage device exclusions"
            href "pageStatus", title: "Reset", description: "Tap to reset the stored device info in this app. Useful if a \"ghost\" device is being tracked.", params: [refresh: true, reset: true]
        }
    }
    
}

//***************************
//Show Settings Page
//***************************
def pageSettings() {

    def pageProperties = [
        name: "pageSettings",
        title: "Device Monitor - Settings",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    return dynamicPage(pageProperties) {
        section("Inactivity Timeout") {
            input "timer", "number", title: "How long (in hours) before a device is considered inactive?", required: false
        }
        section("Check at these times") {
            paragraph "Check your devices on a schedule and as events come in from the selected devices"
            input "inputTime", "time", title: "Check at this time daily?", required: false
            input "checkFrequency", "enum", title: "Check at this interval", required: false, options: [
                1: "5 Minutes",
                2: "10 Minutes",
                3: "15 Minutes",
                4: "30 Minutes",
                5: "1 Hour",
                6: "3 Hours"
            ]

            input "checkEvent", "boolean", title: "Run a check each time one of the selected devices sends an event?", required: false, submitOnChange: true, value: false
            if (settings.checkEvent != null && checkEvent.toBoolean() == true) {
                input "minimumCheck", "number", title: "Minimum time (in minutes) between checks. Useful if you use the above option and subscribe to many devices.", required: false, value: 15
            }
            def timeLabel = timeIntervalLabel()
            href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        
        section("Battery Settings") {
        input "checkBattery", "boolean", title: "Check battery values?", required: false, submitOnChange: true, value: false
            if (settings.checkBattery != null && checkBattery.toBoolean() == true) {
                input "batteryThreshold", "number", title: "Be notified if battery is below this level.", required: false, value: 20
            }
        }
        section("Notifications") {
            input("recipients", "contact", title: "Send notifications to") {
                input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false, value: "No"
                input "phoneNumber", "phone", title: "Enter phone number to send text notification.", required: false
            }
            input "deviceOnline", "boolean", title: "Send a notification if a device comes back online?", required: false, submitOnChange: false, value: false
        }
        section([title: "Other Options", mobileOnly: true]) {
            label title: "Assign a name for the app (optional)", required: false
        }    
    }
    
}

//***************************
//Show Exclusions Page
//***************************
def pageExclusions() {

    def pageProperties = [
        name: "pageExclusions",
        title: "Device Monitor - Exclusions",
        nextPage: null,
        install: false,
        uninstall: false
    ]
    return dynamicPage(pageProperties) {
    
        def allDevices = []
        def eventExclude = [:]
        def batteryExclude = [:]
        
        [motiondevices, humiditydevices, leakdevices, thermodevices, tempdevices, contactdevices,
            lockdevices, alarmdevices, switchdevices, presencedevices, smokedevices].each { n ->
                if (n != null){ 
                   allDevices += n
                }
        }
        
        def allDevicesUnique = allDevices.unique({
                a,
                b -> a["id"] <=> b["id"]
            }).sort({
                a,
                b -> a["displayName"] <=> b["displayName"]
            })

            allDevicesUnique.each() {
               eventExclude["${it.id}"] = "${it.displayName}"
                if (it.hasCapability("Battery")) batteryExclude["${it.id}"] = "${it.displayName}"
            }
        
        section("Exclusions") {
            input "deviceExclusions", "enum", title: "Exclude these devices from event checks", multiple: true, required: false,
                options: eventExclude
            input "batteryExclusions", "enum", title: "Exclude these devices from battery checks", multiple: true, required: false,
                options: batteryExclude
        }
    
    }
    
}

//***************************
//Show Status page
//***************************
def pageStatus(params) {

    def pageProperties = [
        name: "pageStatus",
        title: "Device Monitor - Status",
        nextPage: null,
        install: false,
        uninstall: false
    ]

    if (settings.motiondevices == null &&
        settings.humiditydevices == null &&
        settings.leakdevices == null &&
        settings.thermodevices == null &&
        settings.tempdevices == null &&
        settings.contactdevices == null &&
        settings.lockdevices == null &&
        settings.alarmdevices == null &&
        settings.switchdevices == null &&
        settings.smokedevices == null &&
        settings.presencedevices == null) {
        return pageConfigure()
    }

    return dynamicPage(pageProperties) {

        if (params.reset) {
            atomicState.batterygoodlist = ""
            atomicState.batterybadlist = ""
            atomicState.batterylist = ""
            atomicState.goodlist = ""
            atomicState.badlist = ""
            atomicState.errorlist = ""
            atomicState.delaylist = ""
            atomicState.delayListCheck = ""
            atomicState.batteryerrorlist = ""
            atomicState.batteryerrorlistMap = []
            atomicState.batterylistMap = []
            atomicState.goodlistMap = []
            atomicState.badlistMap = []
            atomicState.errorlistMap = []
            atomicState.delaylistMap = []
            atomicState.delaylistCheckMap = []
            
        }
        if (params.refresh) doCheck()

        if (atomicState.delaylist) {
            section("Devices that have not reported for $timer hour(s)") {
                paragraph atomicState.delaylist.trim()
            }
        }

        if (atomicState.badlist) {
            section("Devices NOT Reporting Events") {
                paragraph atomicState.badlist.trim()
            }
        }

        if (atomicState.errorlist) {
            section("Devices with Errors") {
                paragraph atomicState.errorlist.trim()
            }
        }

        if (atomicState.goodlist) {
            section("Devices Reporting (hrs old)") {
                paragraph atomicState.goodlist.trim()
            }
        }
        
        if (atomicState.batterybadlist) {
            section("Devices With Low Battery") {
                paragraph atomicState.batterybadlist.trim()
            }
        }
        
        if (atomicState.batteryerrorlist) {
            section("Devices Not Reporting Battery") {
                paragraph atomicState.batteryerrorlist.trim()
            }
        }
        
        if (atomicState.batterygoodlist) {
            section("Device Battery Levels") {
                paragraph atomicState.batterygoodlist.trim()
            }
        }

    }
}

def eventCheck(evt = false) {
    if (allOk) {
        if ((settings.checkEvent != null && settings.checkEvent.toBoolean()) || evt == false) {
            if (settings.minimumCheck == null || settings.minimumCheck == "") settings.minimumCheck = 15
            if (atomicState.lastExe != null && now() - atomicState.lastExe > settings.minimumCheck * 60 * 1000) {
                atomicState.lastExe = now()
                doCheck()
            } else {
                log.debug "Minimum time of $settings.minimumCheck minutes has not elapsed."
                if (atomicState.lastExe == null) atomicState.lastExe = now()
            }
        } else {
            log.debug "Event Check Disabled."
        }
    }
}

def doCheck() {
    if (atomicState.isRunning != null && atomicState.isRunning != 1) {
        atomicState.isRunning = 1

        log.debug "doCheck()"
        def rightNow = new Date()

        def batterylist = ""
        def batterygoodlist = ""
        def batterybadlist = ""
        def batterybadlistMap = []
        def batterygoodlistMap = []
        def batteryerrorlist = ""
        def batteryerrorlistMap = []
        def batterylistMap = []
        def goodlist = ""
        def goodlistMap = []
        def badlist = ""
        def badlistMap = []
        def errorlist = ""
        def errorlistMap = []
        def delaylist = ""
        def delaylistMap = []
        def delaylistCheck = ""
        def delaylistCheckMap = []
        def allDevices = []
        def batteryDevices = []

        [motiondevices, humiditydevices, leakdevices, thermodevices, tempdevices, contactdevices,
            lockdevices, alarmdevices, switchdevices, presencedevices, smokedevices].each { n ->
                if (n != null){ 
                   allDevices += n
                }
        }

        if (allDevices != null) {
            
            def allDevicesUnique = allDevices.unique({
                a,
                b -> a["id"] <=> b["id"]
            }).sort({
                a,
                b -> a["displayName"] <=> b["displayName"]
            })
            def myDeviceId
            allDevicesUnique.each() {
            def bexclude = false
            batteryExclusions.each { n ->
               if (n == it.id) bexclude = true
            }
            if (bexclude == false){
                if (it.hasCapability("Battery") && settings.checkBattery != null && checkBattery.toBoolean() == true) {
                   if(it.currentValue("battery") != null) {
                      batterygoodlistMap += [
                         [battery: it.currentValue("battery"), name: "$it.displayName"]
                      ]
                   }
                   else {
                      batteryerrorlistMap += [
                         [name: "$it.displayName"]
                      ]
                   }
                   if(it.currentValue("battery") != null && it.currentValue("battery").toInteger() < settings.batteryThreshold.toInteger()){
                      batterybadlistMap += [
                         [battery: it.currentValue("battery"), name: "$it.displayName"]
                      ]
                      batterylistMap += [
                         [name: "$it.displayName"]
                      ]
                   }
               }
            }else{
               log.debug "Excluding $it.displayName from battery check."
            }
               
            def dexclude = false
            deviceExclusions.each { n ->
               if (n == it.id) dexclude = true
            }
            if (dexclude == false){
                def lastTime = it.events([all: true, max: 100]).find {
                    (it.source as String) == "DEVICE"
                }

                try {
                    if (lastTime) {
                        lastTime = lastTime.date.time
                        def hours = (((rightNow.time - lastTime) / 60000) / 60)
                        def xhours = (hours.toFloat() / 1).round(2)
                        if (xhours > timer) {
                            //def thours = (hours.toFloat()/1).round(0)
                            delaylistMap += [
                                [time: "$xhours", name: "$it.displayName"]
                            ]
                            delaylistCheckMap += [
                                [name: "$it.displayName"]
                            ]
                        }
                        goodlistMap += [
                            [time: "$xhours", name: "$it.displayName"]
                        ]
                    } else {
                        badlistMap += [
                            [name: "$it.displayName"]
                        ]
                    }

                } catch (e) {
                    log.trace "Caught error checking a device."
                    log.trace e
                    errorlistMap += [
                        [name: "$it.displayName"]
                    ]
                }
                }else{
               log.debug "Excluding $it.displayName from event check."
            }
            }
            
            def batteryerrorlistUniqueSorted = batteryerrorlistMap.sort({
                a,
                b -> a["name"] <=> b["name"]
            })
            batteryerrorlistUniqueSorted.each {
                batteryerrorlist += "${it.name}\n"
            }
            def batterylistUniqueSorted = batterylistMap.sort({
                a,
                b -> a["name"] <=> b["name"]
            })
            batterylistUniqueSorted.each {
                batterylist += "${it.name}\n"
            }
            def goodlistUniqueSorted = goodlistMap.sort({
                a,
                b -> b["time"] as float <=> a["time"] as float
            })
            goodlistUniqueSorted.each {
                goodlist += "${it.time} - ${it.name}\n"
            }
            def batterygoodlistUniqueSorted = batterygoodlistMap.sort({
                a,
                b -> b["battery"] as Integer <=> a["battery"] as Integer
            })
            batterygoodlistUniqueSorted.each {
                batterygoodlist += "${it.battery}% - ${it.name}\n"
            }
            def batterybadlistUniqueSorted = batterybadlistMap.sort({
                a,
                b -> a["battery"] as Integer <=> b["battery"] as Integer
            })
            batterybadlistUniqueSorted.each {
                batterybadlist += "${it.battery}% - ${it.name}\n"
            }
            def badlistUniqueSorted = badlistMap
            badlistUniqueSorted.each {
                badlist += "${it.name}\n"
            }
            def delaylistCheckUniqueSorted = delaylistCheckMap
            delaylistCheckUniqueSorted.each {
                delaylistCheck += "${it.name}\n"
            }
            def errorlistUniqueSorted = errorlistMap
            errorlistUniqueSorted.each {
                errorlist += "${it.name}\n"
            }
            def delaylistUniqueSorted = delaylistMap.sort({
                a,
                b -> b["time"] as float <=> a["time"] as float
            })
            delaylistUniqueSorted.each {
                delaylist += "${it.time} - ${it.name}\n"
            }
            def tempMap = []
            atomicState.delaylistCheckMap.each {
                tempMap += [
                    [name: "$it.name"]
                ]
            }
            def delaylistCheckMapDiff = delaylistCheckUniqueSorted - tempMap
            def onlinedelaylistMapDiff = tempMap - delaylistCheckUniqueSorted
            tempMap = []
            atomicState.errorlistMap.each {
                tempMap += [
                    [name: "$it.name"]
                ]
            }
            def errorlistMapDiff = errorlistUniqueSorted - tempMap
            def onlineerrorlistMapDiff = tempMap - errorlistUniqueSorted
            tempMap = []
            atomicState.badlistMap.each {
                tempMap += [
                    [name: "$it.name"]
                ]
            }
            def badlistMapDiff = badlistUniqueSorted - tempMap
            def onlinebadlistMapDiff = tempMap - badlistUniqueSorted
            tempMap = []
            atomicState.batteryerrorlistMap.each {
                tempMap += [
                    [name: "$it.name"]
                ]
            }
            def batteryerrorlistMapDiff = batteryerrorlistUniqueSorted - tempMap
            tempMap = []
            atomicState.batterylistMap.each {
                tempMap += [
                    [name: "$it.name"]
                ]
            }
            log.debug tempMap 
            log.debug batterylistUniqueSorted
            def batterylistMapDiff = batterylistUniqueSorted - tempMap

            if ((batteryerrorlistMapDiff || batterylistMapDiff || badlistMapDiff || errorlistMapDiff || delaylistCheckMapDiff || onlinedelaylistMapDiff || onlineerrorlistMapDiff || onlinebadlistMapDiff) && ((location.contactBookEnabled && recipients) || (sendPushMessage != "No") || (phoneNumber != "0"))) {

                log.trace "Preparing Notification"

                def text = ""
                def check = ""
                def notifications = []

                if (delaylistCheckMapDiff) {
                    def notificationDelaylist = ""
                    def newMap = []
                    delaylistCheckMapDiff.each {
                        n ->
                            newMap += delaylistUniqueSorted.find {
                                it.name == n.name
                            }
                    }
                    newMap.sort({
                        a,
                        b -> b["time"] as float <=> a["time"] as float
                    }).each {
                        notificationDelaylist += "${it.time} - ${it.name}\n"
                    }
                    notifications += ["Devices delayed:\n${notificationDelaylist.trim()}"]
                }
                if (deviceOnline != null && deviceOnline.toBoolean() == true && (onlinedelaylistMapDiff || onlineerrorlistMapDiff || onlinebadlistMapDiff)) {
                    def onlineListCombined = onlinedelaylistMapDiff + onlineerrorlistMapDiff + onlinebadlistMapDiff
                    def errorListCombined = badlistMapDiff + errorlistMapDiff + delaylistCheckMapDiff
                    def onlineListDiff = onlineListCombined - errorListCombined
                    def notificationOnlinelist = ""

                    onlineListDiff.each {
                        notificationOnlinelist += "${it.name}\n"
                    }
                    
                    if (notificationOnlinelist != "" ) notifications += ["Devices Are Now Online:\n${notificationOnlinelist.trim()}"]
                }
                if (badlistMapDiff) {
                    def notificationBadlist = ""
                    badlistMapDiff.each {
                        notificationBadlist += "${it.name}\n"
                    }
                    notifications += ["Devices Not Reporting Events:\n${notificationBadlist.trim()}"]
                }
                if (batterylistMapDiff) {
                    def notificationBatterylist = ""
                    def newMap = []
                    batterylistMapDiff.each {
                        n ->
                            newMap += batterygoodlistUniqueSorted.find {
                                it.name == n.name
                            }
                    }
                    newMap.sort({
                        a,
                        b -> b["battery"] as Integer <=> a["battery"] as Integer
                    }).each {
                        notificationBatterylist += "${it.battery}% - ${it.name}\n"
                    }
                    
                    notifications += ["Devices With Low Battery:\n${notificationBatterylist.trim()}"]
                }
                if (batteryerrorlistMapDiff) {
                    def notificationBatteryErrorlist = ""
                    batteryerrorlistMapDiff.each {
                        notificationBatteryErrorlist += "${it.name}\n"
                    }
                    notifications += ["Devices not reporting battery:\n${notificationBatteryErrorlist.trim()}"]
                }

                if (errorlistMapDiff) {
                    def notificationErrorlist = ""
                    errorlistMapDiff.each {
                        notificationErrorlist += "${it.name}\n"
                    }
                    notifications += ["Devices with Errors:\n${notificationErrorlist.trim()}"]
                }

                if ((location.contactBookEnabled && recipients) || (sendPushMessage != "No") || (phoneNumber != "0")) {
                    notifications.each() {
                        send(it)
                    }
                }
            }

            atomicState.batterygoodlist = batterygoodlist
            atomicState.batterybadlist = batterybadlist
            atomicState.batterylist = batterylist
            atomicState.batteryerrorlist = batteryerrorlist
            atomicState.batteryerrorlistMap = batteryerrorlistMap
            atomicState.goodlist = goodlist
            atomicState.badlist = badlist
            atomicState.errorlist = errorlist
            atomicState.delaylist = delaylist
            atomicState.delayListCheck = delayListCheck
            atomicState.batterylistMap = batterylistMap
            atomicState.goodlistMap = goodlistMap
            atomicState.badlistMap = badlistMap
            atomicState.errorlistMap = errorlistMap
            atomicState.delaylistMap = delaylistMap
            atomicState.delaylistCheckMap = delaylistCheckMap


            atomicState.isRunning = now()
        } else {
            log.debug "There are no devices selected"
            atomicState.goodlist = "There are no devices selected"
        }
    } else {
        log.debug "The application is already running"
        if (atomicState.isRunning == null) atomicState.isRunning = now()
        if ((now() - atomicState.isRunning) > 60000) atomicState.isRunning = now()
    }

}

//***************************
//Show Configure Page
//***************************
def pageConfigure() {

    def inputMotionDevices = [name: "motiondevices", type: "capability.motionSensor", title: "Which motion sensors?", multiple: true, required: false]
    def inputHumidityDevices = [name: "humiditydevices", type: "capability.relativeHumidityMeasurement", title: "Which humidity sensors?", multiple: true, required: false]
    def inputLeakDevices = [name: "leakdevices", type: "capability.waterSensor", title: "Which leak sensors?", multiple: true, required: false]
    def inputThermoDevices = [name: "thermodevices", type: "capability.thermostat", title: "Which thermostats?", multiple: true, required: false]
    def inputTemperature = [name: "tempdevices", type: "capability.temperatureMeasurement", title: "Which temperature sensors?", multiple: true, required: false]
    def inputContactDevices = [name: "contactdevices", type: "capability.contactSensor", title: "Which open/close contact sensors?", multiple: true, required: false]
    def inputLockDevices = [name: "lockdevices", type: "capability.lock", title: "Which locks?", multiple: true, required: false]
    def inputAlarmDevices = [name: "alarmdevices", type: "capability.alarm", title: "Which alarms/sirens?", multiple: true, required: false]
    def inputSwitchDevices = [name: "switchdevices", type: "capability.switch", title: "Which switches?", multiple: true, required: false]
    def inputPresenceDevices = [name: "presencedevices", type: "capability.presenceSensor", title: "Which presence sensors?", multiple: true, required: false]
    def inputSmokeDevices = [name: "smokedevices", type: "capability.smokeDetector", title: "Which Smoke/CO2 detectors?", multiple: true, required: false]

    def pageProperties = [name: "pageConfigure",
        title: "Device Monitor - Configure Devices",
        nextPage: null,
        params: [refresh: true],
        uninstall: false
    ]

    return dynamicPage(pageProperties) {
        
        section("Devices To Check") {
            input inputMotionDevices
            input inputHumidityDevices
            input inputLeakDevices
            input inputThermoDevices
            input inputTemperature
            input inputContactDevices
            input inputLockDevices
            input inputAlarmDevices
            input inputSwitchDevices
            input inputPresenceDevices
            input inputSmokeDevices
        }
        
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    log.trace "Initializing Device Monitor"
    scheduleCheck()
    subscribeDevices()
}

def scheduleCheck() {
    if (settings.inputTime != null && settings.inputTime != "") schedule(settings.inputTime, eventCheck)
    if (settings.checkFrequency != null && settings.checkFrequency != "") {
        switch (settings.checkFrequency as Integer) {
            case 1:
                runEvery5Minutes(eventCheck)
                break
            case 2:
                runEvery10Minutes(eventCheck)
                break
            case 3:
                runEvery15Minutes(eventCheck)
                break
            case 4:
                runEvery30Minutes(eventCheck)
                break
            case 5:
                runEvery1Hour(eventCheck)
                break
            case 6:
                runEvery3Hours(eventCheck)
                break
            default:
                log.debug "No regular check frequency chosen."
                break
        }
    }

}

def subscribeDevices() {

    log.trace "subscribing to Devices"
    subscribe(motiondevices, "motion", eventCheck, [filterEvents: false])
    subscribe(humiditydevices, "relativeHumidity", eventCheck, [filterEvents: false])
    subscribe(leakdevices, "water", eventCheck, [filterEvents: false])
    subscribe(thermodevices, "Temperature", eventCheck, [filterEvents: false])
    subscribe(thermodevices, "heatingSetpoint", eventCheck, [filterEvents: false])
    subscribe(thermodevices, "coolingSetpoint", eventCheck, [filterEvents: false])
    subscribe(tempdevices, "temperature", eventCheck, [filterEvents: false])
    subscribe(contactdevices, "contact", eventCheck, [filterEvents: false])
    subscribe(lockdevices, "lock", eventCheck, [filterEvents: false])
    subscribe(alarmdevices, "alarm", eventCheck, [filterEvents: false])
    subscribe(switchdevices, "switch", eventCheck, [filterEvents: false])
    subscribe(presencedevices, "presence", eventCheck, [filterEvents: false])
    subscribe(smokedevices, "smokeDetector", eventCheck, [filterEvents: false])

}


private send(message) {
    log.debug("Send Notification Function")
        // check that contact book is enabled and recipients selected
    if (location.contactBookEnabled && recipients) {
        log.debug("Sending notifications to selected contacts...")
        sendNotificationToContacts(message, recipients)
    } else if (sendPushMessage != "No") {
        log.debug("Sending Push Notification...")
        sendPush(message)
    } else if (phoneNumber != "0") {
        log.debug("Sending text message...")
        sendSms(phoneNumber, message)
    }
}

// execution filter methods
private getAllOk() {
    modeOk && daysOk && timeOk
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    if (!result) log.trace "modeOk = $result"
    result
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        } else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    if (!result) log.trace "daysOk = $result"
    result
}

private getTimeOk() {
    def result = true
    if (starting && ending) {
        def currTime = now()
        def start = timeToday(starting).time
        def stop = timeToday(ending).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
    if (!result) log.trace "timeOk = $result"
    result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private timeIntervalLabel() {
    (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z"): ""
}