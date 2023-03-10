/**
*  Copyright 2020 jaime20@boteros.org
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
*  Author: jaime20@boteros.org
*
*/

definition(
    name: "Smart Water Heater",
    namespace: "ljbotero",
    author: "jaime20@boteros.org",
    description: "Optimize when you run your water heater",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png"
)

/****************************************************************************/
/*  UI /*
/****************************************************************************/


preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section("<h2>Smart Water Heater</h2>"){
            input "waterHeater", title: "Water heater", required: true, "capability.thermostat"
            input "maxTemp", "number", range: "70..150", title: "Max temperature to set heater", required: true, defaultValue: 115
        }
        section("<h2>Planning</h2>") {
            input "enableSchedule", title: "Enable running on schedule", defaultValue: true, "bool"
            input "allowedModes", title: "Run on specific modes", multiple: true, "mode"
            input "timeStartNextWeekDay", title: "Time I wanto have hot water on <b>weekdays</b>", "time"
            input "timeStartNextWeekend", title: "Time I wanto have hot water on <b>weekends</b> or holidays", "time"
            input "minutesToRunAfterHeated", "number", range: "0..*", title: "Minutes to keep water hot after scheduled time", required: true, defaultValue: 120
            input "minutesToRunAfterHeatedManually", "number", range: "0..*", title: "Minutes to keep water hot after manually activated", required: true, defaultValue: 30
            input "estimateMinutesToHeatWater", title: "Automatically estimate minutes it takes to heat water from cold", defaultValue: true, "bool"
            input "minutesToHeatWater", range: "0..*", title: "Estimated number of minutes it takes to heat water from cold (for next day planning)", "number"
        }
        section("<h2>Control & Status</h2>"){
            input "turnOnWhenWaterIsHot", title: "Turn on when water is hot", "capability.switch"
            input "circulationSwitch", title: "Switch to turn when water heater is active (typically used for water circulation)", "capability.switch"
            input "statusLight", title: "Status light (blinks when heating / solid when ready)", "capability.switch"
            input "toggleSwitches", title: "On/Off switch to manually initiate heater", multiple: true, "capability.switch"
            input "holidaySwitch", title: "Switch that is turned-on on holidays (i.e. <a href='https://github.com/dcmeglio/hubitat-holidayswitcher'>hubitat-holidayswitcher</a>)", "capability.switch"
        }
        section("<h3>Long Shower Detection</h3>"){      
            paragraph("<b>People showered so far today: ${getDailyShowerCount()}</b>")
            input "enableAutoShutOffWhenLongShowerDetected", title: "Enable auto-shut off when long shower detected", defaultValue: true, "bool"
            input "waterFlowDevice", title: "Water Flow Sensor", multiple: false, "capability.liquidFlowRate"
            input "maxShowerDurationInMinutes", range: "2..60", title: "Max shower duration in minutes", "number", defaultValue: getDefaultMaxShowerDurationInMinutes()
            input "numberOfPeopleShoweringDaily", range: "0..10", title: "Number of people showering daily", "number", defaultValue: 0      
            input "notifyWhenLongShowerDetectedDevices", title: "Notify when long shower detected", multiple: true, "capability.notification"
            input "notifyWhenLongShowerDetectedMessage", title: "Notification Message", defaultValue: "Auto shut-off activated after a #minutes# minute shower", "string"
            input "notifyWhenShowerDetectedDevices", title: "Notify when shower is detected", multiple: true, "capability.notification"
            input "notifyWhenShowerDetectedMessage", title: "Notification Message", defaultValue: "Shower detected. Lasted #minutes# minutes", "string"
            input "notifyWhenLongShowerDetectedModes", title: "Notify Only on specific modes", multiple: true, "mode"
        }
        section("<h2>Notifications</h2>"){
            input "notifyWhenStart1Devices", title: "Notify when water heater starts", multiple: true, "capability.notification"
            input "notifyWhenStart1Message", title: "Notification Message", defaultValue: "Water heater has started heating water", "string"
            input "notifyWhenStart1Modes", title: "Only notify on specific modes", multiple: true, "mode"
            input "notifyWhenReady1Devices", title: "Notify when water is ready", multiple: true, "capability.notification"
            input "notifyWhenReady1Message", title: "Notification Message", defaultValue: "Water heater has finished heating water", "string"
            input "notifyWhenReady1Modes", title: "Only notify on specific modes", multiple: true, "mode"
            input "notifyWhenErrorDevices", title: "Notify if problems are detected", multiple: true, "capability.notification"
            input "notifyWhenErrorModes", title: "Only notify on specific modes", multiple: true, "mode"
        }
        section("<h2>Testing</h2>"){
            input "dryRun", title: "Dry-run (won't execute any device changes)", defaultValue: false, "bool"
            input "debugEnabled", title: "Log debug messages", defaultValue: false, "bool"
        }
    }
}

/****************************************************************************/
/*  CONSTANTS /*
/****************************************************************************/

def getMinumumWaterFlow() {
    return 12
}

def getDefaultMaxShowerDurationInMinutes() {
    return 6
}

def getSecondsAverageShower() {
    return 60 // This must be smaller than MaxShowerDurationInMinutes
}

def getWaitSecondsBetweenShowers() {
    return 64
}

/****************************************************************************/
/*  HELPER FUNCTIONS /*
/****************************************************************************/

def debug(msg) {
    if (debugEnabled) {
        log.debug msg
    }
}

def getMaxTemp() {
    def maxTempLimit = new Float(waterHeater.getDataValue("maxTemp"))
    if (maxTempLimit < maxTemp) {
        return maxTempLimit
    }
    return maxTemp
}

def getMinTemp() {
    return new Float(waterHeater.getDataValue("minTemp"))
}

def setWaterHeaterOn() {
    debug("setWaterHeaterOn")
    def minutesSinceLastRan = Math.round((now() - atomicState.timeHeatingEnded) / (60 * 1000)).toInteger()
    debug("minutesSinceLastRan = ${minutesSinceLastRan}, maxMinutesWaterHeaterStaysHot = ${atomicState.maxMinutesWaterHeaterStaysHot}")
    def runInMinutes = (atomicState.approxMinutesToStartWaterHeater + atomicState.rollingVarianceMinutesToStartWaterHeater).toInteger() 
    if (!atomicState.isHeating && minutesSinceLastRan >= atomicState.maxMinutesWaterHeaterStaysHot && runInMinutes > 0) {
        debug("checkWaterHeaterStarted in ${runInMinutes} minutes")
        unschedule(checkWaterHeaterStarted)
        runIn(runInMinutes * 60, "checkWaterHeaterStarted")    
    }    
    if (!dryRun) { waterHeater.setHeatingSetpoint(getMaxTemp()) }
}

def checkWaterHeaterStarted() {
    def minutesSinceLastRan = (atomicState.approxMinutesToStartWaterHeater + atomicState.rollingVarianceMinutesToStartWaterHeater).toInteger() 
    def notifyWhenErrorMessage = "Water heater has not started since ${minutesSinceLastRan} minutes ago"
    sendNotifications(notifyWhenErrorDevices, notifyWhenErrorModes, notifyWhenErrorMessage)
    debug(notifyWhenErrorMessage)
}

def setWaterHeaterOff() {
    debug("setWaterHeaterOff")
    unschedule(checkWaterHeaterStarted)
    unschedule(setWaterHeaterOff)
    atomicState.setWaterHeaterOffAt = 0
    atomicState.startedOnSchedule = false
    if (!dryRun) { waterHeater.setHeatingSetpoint(getMinTemp()) }
}

def circulateWaterOn() {
    if (!dryRun && circulationSwitch != null) {
        circulationSwitch.on()    
        debug("Circulation switch on")
    }
}

def circulateWaterOff() {
    if (!dryRun && circulationSwitch != null) {
        circulationSwitch.off()
        debug("Circulation switch off")
    }
}

def setDailyShowerCount(count) {
   if (waterFlowDevice.hasCommand("showers")) {
       waterFlowDevice.showers(count)
   } else {
       atomicState.dailyShowerCount = count
   }
}

def getDailyShowerCount() {
   if (waterFlowDevice.hasCommand("showers")) {
       return waterFlowDevice.currentValue("dailyShowerCount")
   } else {
       return state?.dailyShowerCount
   }
}

/****************************************************************************/
/*  SETUP /*
/****************************************************************************/

def installed() {
    debug("Installed app")
    initialize()
}

def updated(settings) {
    unsubscribe()
    initialize()
    debug("updated settings")
}

def enableToggleSwitchContactChange() {
    if (toggleSwitches == null) {
        return
    }
    toggleSwitches.each { device -> 
        subscribe(device, "switch", toggleSwitchContactChangeHandler)
    }
}

def resetDailyShowerCounter() {
    debug("Reseting daily shower count")
    setDailyShowerCount(0)
    atomicState.showerStartedTime = 0
}

def initialize() {
    atomicState.timeHeatingEnded = now()
    atomicState.timeHeatingStarted = now()
    atomicState.rollingVarianceMinutesToStartWaterHeater = 0
    atomicState.timeHeaterActiveStarted = now()
    atomicState.waterHeaterActive = false
    atomicState.startedOnSchedule = false
    atomicState.maxMinutesWaterHeaterStaysHot = 15
    atomicState.approxMinutesToStartWaterHeater = 10
    atomicState.showerStartedTime = 0
    atomicState.setWaterHeaterOffAt = 0    
    atomicState.scheduledFlowRateStopped = false
    
    if (maxShowerDurationInMinutes == null) {
        app.updateSetting("maxShowerDurationInMinutes", getDefaultMaxShowerDurationInMinutes())
    }

    //setDailyShowerCount(2)
    unschedule(resetDailyShowerCounter)
    schedule("0 0 0 * * ?", resetDailyShowerCounter)

    subscribe(waterHeater, "heatingSetpoint", heatingSetpointChangeHandler)
    subscribe(waterHeater, "thermostatOperatingState", thermostatOperatingStateChangeHandler)
    subscribe(waterFlowDevice, "rate", liquidFlowRateHandler)

    enableToggleSwitchContactChange()

    if (minutesToHeatWater == null) {
        debug("Setting minutesToHeatWater to: 10")
        app.updateSetting("minutesToHeatWater", 10)
    }
    def minTempLimit = new Float(waterHeater.getDataValue("minTemp"))
    def maxTempLimit = new Float(waterHeater.getDataValue("maxTemp"))

    if (maxTemp <= minTempLimit) {
        app.updateSetting("maxTemp", minTempLimit + 5)
    } else if (maxTemp > maxTempLimit) {
        debug("set maxTemp: ${maxTemp} to maxTempLimit: ${maxTempLimit}")
        app.updateSetting("maxTemp", maxTempLimit)
    }
    initSchedule()

    if (dryRun) {
        debug("Running tests")
        notificationTest()
        onFinishedHeatingWaterTest()
    }
}

def initSchedule() {
    unschedule(onScheduleHandlerWeekday)
    unschedule(onScheduleHandlerWeekend)

    def scheduleString = scheduleSetup(timeStartNextWeekDay, "MON-FRI")
    if (scheduleString != null) {
        schedule(scheduleString, onScheduleHandlerWeekday)
    }

    scheduleString = scheduleSetup(timeStartNextWeekend, "SAT-SUN")
    if (scheduleString != null) {
        schedule(scheduleString, onScheduleHandlerWeekend)
    }
}

def scheduleSetup(timeStartNextDay, weekdaysRange) {
    if (timeStartNextDay == null) {
        debug("No schedule time defined for ${weekdaysRange}")
        return null
    }
    def timeStartNextDayMillis = timeToday(timeStartNextDay).getTime()
    def plannedTimeStartNextDay = new Date(timeStartNextDayMillis - (minutesToHeatWater * 60 * 1000))
    def plannedHourStartNextDay = plannedTimeStartNextDay.format("H")
    def plannedMinuteStartNextDay = plannedTimeStartNextDay.format("m")
    // http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
    def weekdaySchedule = "0 ${plannedMinuteStartNextDay} ${plannedHourStartNextDay} ? * ${weekdaysRange}"
    debug "Schedule: ${weekdaySchedule}"
    return weekdaySchedule
}

/****************************************************************************/
/*  EVENT HANDLERS /*
/****************************************************************************/
def fireToggleOn() {
    if (toggleSwitches == null) {
      return
    }
    debug("fireToggleOn")
    toggleSwitches.each { device -> 
        device.on()
    }
}

def fireToggleOff() {
    if (toggleSwitches == null) {
      return
    }
    debug("fireToggleOff")
    toggleSwitches.each { device -> 
        device.off()
    }
}

def toggleSwitchContactChangeHandler(evt) {
    debug("toggleSwitchContactChangeHandler: ${evt.name} = ${evt.value}")
    toggleSwitchChangeValue(evt.value)
}

def toggleSwitchChangeValue(evtValue) {
    if (atomicState.toggleSwitchContactState == evtValue) {
        return
    }
    atomicState.toggleSwitchContactState = evtValue
    if (evtValue == "off") {
        // Stop heating
        setWaterHeaterOff()   
        circulateWaterOff()
    } else {
        // Start heating
        setWaterHeaterOn()
        circulateWaterOn()
        scheduleShutOff()
    }
    debug("toggleSwitchChangeValue = ${evtValue}")
}

def onScheduleHandlerWeekday() {
    if (holidaySwitch != null && holidaySwitch.currentValue("switch") == "on") {
        debug("Running schedule for Holiday")
        atomicState.targetTime = timeToday(timeStartNextWeekend).getTime()
        def targetWaitMillis = (atomicState.targetTime - (minutesToHeatWater * 60 * 1000)) - now()
        if (targetWaitMillis > 0) {
            debug("Delaying start to ${targetWaitMillis/ (60 * 1000)} minutes")
            unschedule(onScheduleHandler)
            runInMillis(targetWaitMillis, "onScheduleHandler")
            return
        }
        debug("Use case not handled: When holiday start stime is earlier than weekday's start times")
    } 
    atomicState.targetTime = timeToday(timeStartNextWeekDay).getTime()
    onScheduleHandler()
}

def onScheduleHandlerWeekend() {
    atomicState.targetTime = timeToday(timeStartNextWeekend).getTime()
    onScheduleHandler()
}

def onScheduleHandler() {
    debug("onScheduleHandler")
    if (!enableSchedule) {
        debug("Schedule cancelled since app is not enabled")
        return
    }
    if (allowedModes != null) {
        if (!allowedModes.any{ it == location.mode }) {
            debug("Schedule cancelled since current mode (${location.mode}) is no allowed to run")
            return
        }
    }
    atomicState.startedOnSchedule = true
    fireToggleOn()
}

def heatingSetpointChangeHandler(evt) {
    if (!evt.value.isNumber()) {
        debug("[ERROR] Invalid Event Value: ${evt.value}")
        return;
    }
    def currSetPoint = new Float(evt.value)
    debug("${evt.name} = ${evt.value}")
    if (getMinTemp() - 0.5 < currSetPoint && getMinTemp() + 0.5 > currSetPoint) {
        debug("Smart water heater is Inactive (${evt.name} = ${evt.value})")
        atomicState.waterHeaterActive = false    
        atomicState.timeHeaterActiveStarted = now()
        if (turnOnWhenWaterIsHot != null) {
            turnOnWhenWaterIsHot.off()
        }
        fireToggleOff()
    } else if (getMaxTemp() - 0.5 < currSetPoint && getMaxTemp() + 0.5 > currSetPoint) {
        debug("Smart water heater is Active (${evt.name} = ${evt.value})")
        atomicState.waterHeaterActive = true
        atomicState.notificationStartedSent = false
        atomicState.notificationEndedSent = false
        fireToggleOn()
    }
    unschedule(updateStatusLight)
    schedule("0/2 * * * * ?", updateStatusLight)
}

def updateWaterHeaterApproxTimes() {
    if (atomicState.waterHeaterActive == false) {
        debug("updateWaterHeaterApproxTimes: canceled")
        return
    }
    if (atomicState.timeHeaterActiveStarted > atomicState.timeHeatingStarted) {
        // First time started heating    
        def minutesToStartWaterHeater = Math.round((now() - atomicState.timeHeaterActiveStarted) / (60 * 1000)).toInteger()
        if (minutesToStartWaterHeater > atomicState.approxMinutesToStartWaterHeater * 4 && atomicState.approxMinutesToStartWaterHeater > 0) {
            debug("[WARNING] Not updating approxMinutesToStartWaterHeater since difference is too high: ${minutesToStartWaterHeater} > ${atomicState.approxMinutesToStartWaterHeater}")
            return;
        }
        atomicState.approxMinutesToStartWaterHeater = atomicState.approxMinutesToStartWaterHeater - (atomicState.approxMinutesToStartWaterHeater / 10)
        atomicState.approxMinutesToStartWaterHeater = atomicState.approxMinutesToStartWaterHeater + (minutesToStartWaterHeater / 10)
        // Update max variance
        def varianceMinutesToStartWaterHeater = Math.abs(atomicState.approxMinutesToStartWaterHeater - minutesToStartWaterHeater)
        if (atomicState.rollingVarianceMinutesToStartWaterHeater < varianceMinutesToStartWaterHeater) {
            atomicState.rollingVarianceMinutesToStartWaterHeater = varianceMinutesToStartWaterHeater
        } else {
            atomicState.rollingVarianceMinutesToStartWaterHeater = 
                (atomicState.rollingVarianceMinutesToStartWaterHeater - (atomicState.rollingVarianceMinutesToStartWaterHeater/10)) +
                (atomicState.rollingVarianceMinutesToStartWaterHeater + (varianceMinutesToStartWaterHeater / 10))
        }
        debug("approxMinutesToStartWaterHeater: ${atomicState.approxMinutesToStartWaterHeater}")
        debug("rollingVarianceMinutesToStartWaterHeater: ${atomicState.rollingVarianceMinutesToStartWaterHeater}")
    } else {    
        // Re-heating
        def minutesWaterHeaterStaysHot = Math.round((now() - atomicState.timeHeatingEnded) / (60 * 1000)).toInteger()
        if (minutesWaterHeaterStaysHot > atomicState.maxMinutesWaterHeaterStaysHot) {
            debug("[WARNING] Not updating maxMinutesWaterHeaterStaysHot since difference is too high: ${minutesWaterHeaterStaysHot} > ${atomicState.maxMinutesWaterHeaterStaysHot}")
            return;
        }
        atomicState.maxMinutesWaterHeaterStaysHot = atomicState.maxMinutesWaterHeaterStaysHot - (atomicState.maxMinutesWaterHeaterStaysHot / 10)
        atomicState.maxMinutesWaterHeaterStaysHot = atomicState.maxMinutesWaterHeaterStaysHot + (minutesWaterHeaterStaysHot / 10)
        debug("maxMinutesWaterHeaterStaysHot: ${atomicState.maxMinutesWaterHeaterStaysHot}")
    } 
}

def setWaterHeaterOffAuto() {
    debug("Long shower detected - Shutting water heater off")
    fireToggleOff()
    def minutesSinceHeating = Math.round((now() - atomicState.showerStartedTime) / (60 * 1000)).toString()
    def notificationMessage = notifyWhenLongShowerDetectedMessage.replace("#minutes#", minutesSinceHeating)
    sendNotifications(notifyWhenLongShowerDetectedDevices, notifyWhenLongShowerDetectedModes, notificationMessage)
}

def handleFlowRateStopped() {
    unschedule(setWaterHeaterOffAuto)
    def endedMillis = now() - (getWaitSecondsBetweenShowers() * 1000)
    def durationSecs = Math.round((endedMillis - atomicState.showerStartedTime) / 1000).toInteger()
    def durationMins = (durationSecs / 60).toString()
    if (durationSecs <= getSecondsAverageShower()) {
        debug("Long shower detector stopped after ${durationMins} minutes "
              +"- not long enough (<${(getSecondsAverageShower() / 60).toString()}) to account as a shower.")
    } else {
        debug("Long shower detector stopped after ${durationMins} minutes")
    }
    if (atomicState.showerStartedTime > 0 && numberOfPeopleShoweringDaily > 0 
        && durationSecs > getSecondsAverageShower()) {
        def dailyShowerCount = getDailyShowerCount() + 1
        setDailyShowerCount(dailyShowerCount)
        def notificationMessage = notifyWhenShowerDetectedMessage.replace("#minutes#", durationMins)
        sendNotifications(notifyWhenShowerDetectedDevices, notifyWhenLongShowerDetectedModes, notificationMessage)
        if (dailyShowerCount >= numberOfPeopleShoweringDaily) {
            debug("Last ${dailyShowerCount} ${notificationMessage}")
            fireToggleOff()
        } else {
            debug("${notificationMessage}: ${dailyShowerCount} of ${numberOfPeopleShoweringDaily} daily showers.")
            // Turn it on for next person unless heating period has passed
            /*
            def waitMillis = atomicState.setWaterHeaterOffAt - now()
            if (waitMillis > 1000 * 60 * 5) {
                def waitMins = Math.round(waitMillis / (60 * 1000)).toInteger()
                debug("Turning on water heater for next person for ${waitMins} more minutes")                
                atomicState.setWaterHeaterOffAt = now() + waitMillis
                unschedule(setWaterHeaterOff)
                runInMillis(waitMillis, "setWaterHeaterOff")
                fireToggleOn()
            } else if (waitMillis > 0) {
                debug("Not turning on water heater for next person, since there are only ${waitMins} more minutes remaining") 
            } else {
                debug("Not turning on water heater for next person, since there are no more minutes remaining")
            }
            */
            fireToggleOn()
        }
    }
    atomicState.showerStartedTime = 0
    atomicState.scheduledFlowRateStopped = false
}

def handleFlowRateStarted() {
    if (atomicState.showerStartedTime == 0) {
        debug("Long shower detector started counting time")
        unschedule(setWaterHeaterOffAuto)
        runIn(maxShowerDurationInMinutes * 60, "setWaterHeaterOffAuto")
        atomicState.showerStartedTime = now()
    }
}

def liquidFlowRateHandler(evt) {
    debug("${evt.name} = ${evt.value}")
    if (!enableAutoShutOffWhenLongShowerDetected) {
        atomicState.showerStartedTime = 0
        return
    }
    
    if (evt.value.toInteger() <= getMinumumWaterFlow() && atomicState.showerStartedTime > 0 && !atomicState.scheduledFlowRateStopped) {
        runIn(getWaitSecondsBetweenShowers(), "handleFlowRateStopped")
        atomicState.scheduledFlowRateStopped = true
    } else if (atomicState.waterHeaterActive && evt.value.toInteger() > getMinumumWaterFlow()) {
        unschedule(handleFlowRateStopped)
        atomicState.scheduledFlowRateStopped = false
        handleFlowRateStarted()
    }
}

def thermostatOperatingStateChangeHandler(evt) {
    debug("${evt.name} = ${evt.value}")
    unschedule(setWaterHeaterOffAuto)
    if (evt.value == "heating") {
        unschedule(checkWaterHeaterStarted)
        updateWaterHeaterApproxTimes()
        atomicState.timeHeatingStarted = now()
        atomicState.isHeating = true
        if (atomicState.waterHeaterActive && !atomicState.notificationStartedSent) {
            sendNotifications(notifyWhenStart1Devices, notifyWhenStart1Modes, notifyWhenStart1Message)
            atomicState.notificationStartedSent = true
        }
        debug("Started at ${new Date()}")
    } else {
        atomicState.timeHeatingEnded = now()
        atomicState.isHeating = false
        onFinishedHeatingWater()
        debug("Ended at ${new Date()}")
    }
    unschedule(updateStatusLight)
    schedule("0/2 * * * * ?", updateStatusLight)
}

def updateStatusLight() {
    if (statusLight == null) {
        debug("No status light has beed defined")
        unschedule(updateStatusLight)
        return;
    }
    if (atomicState.waterHeaterActive == false) {
        unschedule(updateStatusLight)
        debug("Turning statusLight off")
        if (!dryRun) { statusLight.off() }
        return
    }
    if (!atomicState.isHeating) {
        unschedule(updateStatusLight)
        debug("Turning statusLight on")
        if (!dryRun) { statusLight.on() }
        return
    }
    if (atomicState.statusLightOn) {
        //debug("Turning statusLight off - toggle")
        if (!dryRun) { statusLight.off() }
    } else {
        //debug("Turning statusLight on - toggle")
        if (!dryRun) { statusLight.on() }
    }
    atomicState.statusLightOn = !atomicState.statusLightOn
}

def scheduleShutOff() {
    if (atomicState.setWaterHeaterOffAt > now()) {
        return
    }
    // Calculate for how much longer keep it running
    def waitMillis = (minutesToHeatWater + minutesToRunAfterHeatedManually) * 60 * 1000
    if (atomicState.startedOnSchedule) {
        waitMillis = (minutesToHeatWater + minutesToRunAfterHeated) * 60 * 1000
    }

    unschedule(setWaterHeaterOff)
    runInMillis(waitMillis, "setWaterHeaterOff")
    atomicState.setWaterHeaterOffAt = now() + waitMillis

    def waitMinsUntilShutOff = Math.round(waitMillis / (60 * 1000)).toInteger()
    debug("Wait ${waitMinsUntilShutOff} minutes until turning water heater off")
}

def onFinishedHeatingWater() {
    if (atomicState.waterHeaterActive ==  false) {
        return
    }
    if (estimateMinutesToHeatWater && atomicState.startedOnSchedule && atomicState.showerStartedTime == 0) {
        // Update estimate
        atomicState.minutesHeating = Math.round((atomicState.timeHeatingEnded - atomicState.timeHeatingStarted) / (60 * 1000)).toInteger()
        // Assuming 5 is the minimum it takes to heat the water from min temp and 60 is the max time it could take
        if (atomicState.minutesHeating > 5 && atomicState.minutesHeating < 60) {
            debug("Updating minutesToHeatWater to: ${atomicState.minutesHeating}")
            unschedule(initSchedule)
            runInMillis(5000, "initSchedule") // Wait a bit to ensure minutesToHeatWater is already persisted
            app.updateSetting("minutesToHeatWater", atomicState.minutesHeating)
        }
    }
    if (!atomicState.notificationEndedSent) {
        sendNotifications(notifyWhenReady1Devices, notifyWhenReady1Modes, notifyWhenReady1Message)
        if (turnOnWhenWaterIsHot != null) {
            turnOnWhenWaterIsHot.on()
        }
        atomicState.notificationEndedSent = true
    }
}

def sendNotifications(notifyDevices, notifyModes, notifyMessage) {
    if (notifyDevices == null) {
        return
    }
    if (notifyModes != null) {
        if (!notifyModes.any{ it == location.mode }) {
            return
        }
    }
    notifyDevices.each { 
        device -> device.deviceNotification(notifyMessage)
    }
}

/****************************************************************************/
/*  UNIT TESTS                                                              /*
/****************************************************************************/
def notificationTest() {
    def minutesSinceHeating = Math.round((now() - atomicState.timeHeatingStarted) / (60 * 1000)).toString()
    def notificationMessage = notifyWhenLongShowerDetectedMessage.replace("#minutes#", minutesSinceHeating)
    debug(notificationMessage);
}

def onFinishedHeatingWaterTest() {
    // atomicState.waterHeaterActive = true
    // atomicState.startedOnSchedule = true
    // atomicState.timeHeatingStarted = 1608413794070
    // atomicState.timeHeatingEnded = 1608415254031
    // atomicState.targetTime = now() + (20 * 60 * 1000)
    // onFinishedHeatingWater(10)

    // // Unit test: Updating extimate
    // if (atomicState.minutesHeating != 24) {
    //   log.error("Failed unit test - onFinishedHeatingWaterTest - Updating extimate");
    // }

    // // Unit test: Keep it running for 10 minute, but still 20 minutes until reahing the target time
    // if (atomicState.waitMinsUntilShutOff != 30) {
    //   log.error("Failed unit test - onFinishedHeatingWaterTest -  Keep it running for 10 minute, but still 20 minutes until reahing the target time (atomicState.waitMinsUntilShutOff=${atomicState.waitMinsUntilShutOff})");
    // }

    // atomicState.targetTime = now() - (9 * 60 * 1000)
    // onFinishedHeatingWater(10)
    // // Unit test: Keep it running for 10 minute, but it passed 9 minutes ago
    // if (atomicState.waitMinsUntilShutOff != 1) {
    //   log.error("Failed unit test - onFinishedHeatingWaterTest - Keep it running for 10 minute, but it passed 9 minutes ago (atomicState.waitMinsUntilShutOff=${atomicState.waitMinsUntilShutOff})");
    // }

}
