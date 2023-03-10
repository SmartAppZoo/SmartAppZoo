/**
 *  Air Circulator Child
 *
 *  Copyright 2018 Anthony Santilli
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

import groovy.transform.Field
import java.text.SimpleDateFormat

 definition(
    name: "Air Circulator Child",
    namespace: "Echo",
    author: "Anthony Santilli",
    description: "Helps manage the Room Comfort using Fan/Switch and different Actuator/Sensor Triggers...",
    category: "Convenience",
    parent: "Echo:Air Circulator",
    iconUrl: appImg(),
    iconX2Url: appImg(),
    iconX3Url: appImg(),
    pausable: true)

preferences {
    page(name: "mainPage")
    page(name: "namePage")
}

def textVersion()	{ return "Version: ${appVer()}" }
def textModified()	{ return "Updated: ${appDate()}" }
def appVer() { return "1.0.2" }
def appDate() { return "06/4/2018" }

def mainPage() {
    def allConfigured = true
    return dynamicPage(name: "mainPage", nextPage: (state?.isInstalled ? "" : "namePage"), install: state?.isInstalled, uninstall: true) {
        appInfoSect()
        if(state?.isInstalled) {
            section("Hold this Automation:") {
                input "pauseApp", "bool", title: "Pause this Automation until it's turned back on", description: "", submitOnChange: true, defaultValue: false
            }
        }
        section("Fan Selection") {
            input "fanDevice", "capability.actuator", title: "Select the Fan Device", description: "Tap to select...", multiple: false, required: true, submitOnChange: true
            if(fanDevice) { 
                setFanSpeedsAvail(fanDevice) 
                log.debug "getDeviceSpeeds: ${getDeviceSpeeds(fanDevice)}"
            }
        }
        if(tempSensors || humiditySensors || tstatDevice) {
            section("Current Status:") {
                paragraph title: "Fan Status:", getFanStatusDesc(), state: "complete"
            }
        }
        if(fanDevice) {
            def availSpeeds = getDeviceSpeeds(settings?.fanDevice) ?: []

            if(!motionSensors && !humiditySensors) {
                section("Regulate Fan Based on Temperature") {
                
                    input "tempSensors", "capability.temperatureMeasurement", title: "Select Temperature Sensors...", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    
                    if(tempSensors) {
                        if(state?.speedType in ["3speed", "4speed"]) {
                            
                            def tempRanges = tempRangeValues(true)
                            
                            def offRangeLow = tempRanges?.low
                            def offRangeHigh = lowSpeedRange ? lowSpeedRange-1 : (medSpeedRange ? medSpeedRange-1 : tempRanges?.high)
                            def offRangeStr = !offSpeedRange ? "" : "\n(OFF when BELOW ${offSpeedRange}${tempUnitStr()})"
                            input "offSpeedRange", "number", title: "Turn Off Temp${offRangeStr}", description: "${offRangeLow}\u00b0 to ${offRangeHigh}\u00b0", range: "${offRangeLow}..${offRangeHigh}" , submitOnChange: true, required: true

                            def lowRangeLow = offSpeedRange ? offSpeedRange+1 : tempRanges?.low
                            def lowRangeHigh = medSpeedRange ? medSpeedRange-1 : tempRanges?.high
                            def lowRangeStr = (!lowSpeedRange || !offSpeedRange) ? "" : "\n(Low when ${offSpeedRange+1}${tempUnitStr()} to ${lowSpeedRange}${tempUnitStr()})"
                            input "lowSpeedRange", "number", title: "Low Speed Temp${lowRangeStr}", description: "${lowRangeLow}\u00b0 to ${lowRangeHigh}\u00b0", range: "${lowRangeLow}..${lowRangeHigh}" , submitOnChange: true, required: true

                            def medRangeLow = lowSpeedRange ? lowSpeedRange+1 : (offSpeedRange ? offSpeedRange+1 : tempRanges?.low)
                            def medRangeHigh = medHighSpeedRange ? medHighSpeedRange-1 : (highSpeedRange ? highSpeedRange-1 : tempRanges?.high)
                            def medRangeStr = (!medSpeedRange || !lowSpeedRange) ? "" : "\n(Medium when ${lowSpeedRange+1}${tempUnitStr()} to ${medSpeedRange}${tempUnitStr()})"
                            input "medSpeedRange", "number", title: "Medium Speed Temp${medRangeStr}", description: "${medRangeLow}\u00b0 to ${medRangeHigh}\u00b0", range: "${medRangeLow}..${medRangeHigh}", submitOnChange: true, required: true
                            
                            if(state?.speedType == "4speed") {
                                def medHighRangeLow = medSpeedRange ? medSpeedRange+1 : (lowSpeedRange ? lowSpeedRange+1 : (offSpeedRange ? offSpeedRange+1 : tempRanges?.low))
                                def medHighRangeHigh = highSpeedRange ? highSpeedRange-1 : tempRanges?.high
                                def medHighRangeStr = (!medHighSpeedRange || !medSpeedRange) ? "" : "\n(Med-High when ${medSpeedRange+1}${tempUnitStr()} to ${medHighSpeedRange}${tempUnitStr()})"
                                input "medHighSpeedRange", "number", title: "Medium-High Speed Temp${medHighRangeStr}", description: "${medHighRangeLow}\u00b0 to ${medHighRangeHigh}\u00b0", range: "${medHighRangeLow}..${medHighRangeHigh}", submitOnChange: true, required: true
                            }

                            def highRangeStr = medHighSpeedRange ? medHighSpeedRange : (medSpeedRange ? medSpeedRange : null)
                            state?.allowHighSpeed = (highRangeStr != null)
                            if(highRangeStr) {
                                input "allowHighSpeed", "bool", title: "Set High Speed when Greater than ${highRangeStr}\u00b0", description: "", submitOnChange: true, required: false, defaultValue: false
                            }
                            // log.debug "lowRange: ${lowRangeLow}..${lowRangeHigh} | medRange: ${medRangeLow}..${medRangeHigh} | medHighRange: ${medHighRangeLow}..${medHighRangeHigh} | allowHighSpeed: ${state?.allowHighSpeed}"
                        } else {
                            input "fanNoSpeedOnVal", "number", title: "Turn On When >= this Temp (${tempUnitStr()})", description: "", submitOnChange: true, required: true
                        }
                    }
                }
            }

            if(!motionSensors && !tempSensors) {
                section("Regulate Fan Based on Humidity") {
                    input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Select Humidity Sensors", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    if(humiditySensors) {
                        input "fanOnHumidityVal", "number", title: "Turn On When >= this Humidity (%)", description: "", range: "10..99", submitOnChange: true, required: true
                        if(fanOnHumidityVal) {
                            if(state?.speedType != null) {
                                input "humiditySetFanSpeed", "enum", title: "Set Speed to?", description: "Tap to select...", multiple: false, required: true, submitOnChange: true, options: availSpeeds, defaultValue: "medSpeed"
                            } else {
                                paragraph "This device does not support changing speed. So it will just run when the Humidity is Greater >=${fanOnHumidityVal}%..."
                            }
                        }
                    }
                }
            }
            
            if(!tempSensors && !humiditySensors) {
                section("Control Speeds Based on Motion:") {
                    input "motionSensors", "capability.motionSensor", title: "Select Motion Sensors", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    if(motionSensors) {
                        input "motionChangeDelay", "number", title: "Delay After Motion Change (Seconds)", description: "", range: "0..3600", submitOnChange: true, required: true, defaultValue: 60
                        if(state?.speedType != null) {
                            input "motionActiveFanSpeed", "enum", title: "When Motion Active\nSet Speed to?", description: "Tap to select...", multiple: false, required: true, submitOnChange: true, options: availSpeeds, defaultValue: "medSpeed"
                            input "motionInactiveFanSpeed", "enum", title: "When Motion Inactive\nSet Speed to?", description: "Tap to select...", multiple: false, required: true, submitOnChange: true, options: availSpeeds, defaultValue: "medSpeed"
                        } else {
                            input "motionInactiveTurnOff", "bool", title: "When Motion Inactive\nTurn Off?", description: "", required: true, submitOnChange: true, defaultValue: true
                            paragraph "This device does not support changing speed."
                        }
                    }
                }
            }

            section("Circulate Air while HVAC is Running:") { 
                input "tstatDevice", "capability.thermostat", title: "Select the Thermostat to use", description: "Tap to select...", multiple: false, required: false, submitOnChange: true
                if(tstatDevice) {
                    input "tstatHvacStates", "enum", title: "Choose Thermostat States to activate fan?", description: "Tap to select...", multiple: true, required: true, submitOnChange: true, options: ["cooling", "heating", "fan only"]
                    if(tstatHvacStates) {
                        if(state?.speedType != null) {
                            input "tstatSetFanSpeed", "enum", title: "Set Speed to?", description: "Tap to select...", multiple: false, required: true, submitOnChange: true, options: availSpeeds, defaultValue: "medSpeed"
                        } else {
                            paragraph "This device does not support changing speed. So it will just run when the HVAC is running..."
                        }
                    }
                }
            }

            if(tempSensors || humiditySensors || tstatDevice || motionSensors) {
                section("Turn Fan OFF when ANY of:") { 
                    input "off4SwitchOn", "capability.switch", title: "These Switches are ON", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    input "off4ContactOpen", "capability.contactSensor", title: "These Contacts are OPEN", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    input "off4ContactClose", "capability.contactSensor", title: "These Contacts are CLOSED", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                }
                section("Restrict Fan Changes When:") {
                    input "skipModes", "mode", title: "Any of these Modes are Active", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    input "skipSwitchesOn", "capability.switch", title: "Any of these Switches are ON", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                    input "skipSwitchesOff", "capability.switch", title: "Any of these Switches are OFF", description: "Tap to select...", multiple: true, required: false, submitOnChange: true
                }
                section("Options:") {
                    input "manualOverrideDelay", "enum", title: "When device is Physically Changed Disable Automated Changes for (Seconds)", description: "", submitOnChange: true, required: true, defaultValue: 21600, options: waitValEnum()
                    input "changeDelay", "number", title: "Delay between Changes (Seconds)", description: "", range: "0..3600", submitOnChange: true, required: true, defaultValue: 60
                }
            }
        }
        if(state?.isInstalled) {
            section("Name this Automation:") {
                label title:"Automation Name", required:false, defaultValue: "${settings?.fanDevice?.displayName} - Automator"
            }
        }
    }
}

def getDeviceSpeeds(dev) {
    def fanCmds = dev?.supportedCommands?.collect { it?.name }
    def items = []
    if(fanCmds?.contains("lowSpeed") || fanCmds?.contains("lowspeed") || fanCmds?.contains("low")) {
        items?.push("lowSpeed")
    }
    if(fanCmds?.contains("medSpeed") || fanCmds?.contains("medium") ||fanCmds?.contains("med")) {
        items?.push("medSpeed")
    }
    if(fanCmds?.contains("medHighSpeed") || fanCmds?.contains("mediumHighSpeed") || fanCmds?.contains("medHi")) {
        items?.push("medHighSpeed")
    }
    if(fanCmds?.contains("highSpeed") || fanCmds?.contains("highspeed") || fanCmds?.contains("high")) {
        items?.push("highSpeed")
    }
    return items
}

def getFanStatusDesc() {
    def fanstr = "Device: (${fanDevice?.displayName})"
    fanstr += "\n• Power: (${fanDevice?.currentState("switch")?.value?.toString()?.capitalize()})"
    if(tempSensors) {
        if(state?.speedType) {
            fanstr += "\n• Speed Support: (${state?.speedType})"
            fanstr += "\n• Current Speed: (${getCurrentFanSpeed(settings?.fanDevice)})"
            fanstr += "\n• Desired Speed: (${getFanSpeedToUse(settings?.fanDevice, true)})"
            if(offSpeedRange || lowSpeedRange || medSpeedRange || medHighSpeedRange || allowHighSpeed) {
                fanstr += "\n\n• Speed Thresholds:"
                fanstr += settings?.offSpeedRange ? "\n  - Off: (<${settings?.offSpeedRange}${tempUnitStr()})" : ""
                fanstr += settings?.lowSpeedRange ? "\n  - Low: (>=${settings?.offSpeedRange}${tempUnitStr()} & <=${settings?.lowSpeedRange}${tempUnitStr()})" : ""
                fanstr += settings?.medSpeedRange ? "\n  - Med: (>${settings?.lowSpeedRange}${tempUnitStr()} & <=${settings?.medSpeedRange}${tempUnitStr()})" : ""
                fanstr += settings?.medHighSpeedRange ? "\n  - Med-High: (>${settings?.medSpeedRange}${tempUnitStr()} & <=${settings?.medHighSpeedRange}${tempUnitStr()})" : ""
                fanstr += settings?.allowHighSpeed ? "\n  - High: (>${settings?.medHighSpeedRange ? settings?.medHighSpeedRange : settings?.medSpeedRange}${tempUnitStr()})" : ""
            }
        } else {
            if(fanNoSpeedOnVal) {
                fanstr += "\n• Speed Support: (None)"
                fanstr += "\n• Temp Trigger: (${settings?.fanNoSpeedOnVal}${tempUnitStr()})"
            }
        }
        def tempstr = "(${getDeviceTempAvg(settings?.tempSensors)}${tempUnitStr()})"
        fanstr += "\n\n• Current Temp${settings?.tempSensors?.size() > 1 ? " [Avg]" : ""}: $tempstr"
    }
    if(humiditySensors) {
        if(state?.speedType) {
            fanstr += "\n• Speed Support: (${state?.speedType})"
            fanstr += "\n• Current Speed: (${getCurrentFanSpeed(settings?.fanDevice)})"
            fanstr += "\n• Desired Speed: (${settings?.humiditySetFanSpeed})"
        } else {
            if(fanOnHumidityVal) {
                fanstr += "\n• Speed Support: (None)"
                fanstr += "\n• Humidity Trigger: (${settings?.fanOnHumidityVal}%)"
            }
        }

        def humstr = "(${getDeviceHumidityAvg(settings?.humiditySensors)}%)"
        fanstr += "\n\n• Current Humidity: ${settings?.humiditySensors?.size() > 1 ? " [Avg]" : ""}: $humstr"
    }
    if(tstatDevice) { 
        fanstr += "\n\nThermostat Control:"
        String tstatState = settings?.tstatDevice?.currentState("thermostatOperatingState")?.value ?: ""
        fanstr += "\n• HVAC States: ${settings?.tstatHvacStates}"
        fanstr += "\n• Current HVAC: (${tstatState?.toString()?.replaceAll("\\[|\\]", "")})"
        fanstr += "\n• Running Fan: (${checkTstatState()})"
    }

    def restrict = ignoreActions()
    fanstr += "\n\n• Actions Blocked: (${restrict})"
    fanstr += restrict ? "\n└ Reason: ${ignoreActions(true)}" : ""
    fanstr += state?.lastChangeDt ? "\n• Last Change: (${GetTimeDiffSeconds(state?.lastChangeDt)} sec)" : ""
    return fanstr
}

def getCurrentFanSpeed(dev) {
    if(!dev) { return null }
    if(fanDevice?.hasAttribute("fanSpeedDesc")) {
        return fanDevice?.currentState("fanSpeedDesc")?.value?.toString()?.toLowerCase()?.capitalize()
    } else if(fanDevice?.hasAttribute("currentState")) {
        return fanDevice?.currentState("currentState")?.value?.toString()?.toLowerCase()?.capitalize()
    } else if(fanDevice?.hasAttribute("level")){
        return fanDevice?.currentState("level")?.value + "%"
    } else {
        return null
    }
}

def namePage() {
    return dynamicPage(name: "namePage", install: true, uninstall: true) {
        section("Name this Automation:") {
            label title:"Automation Name", required:false, defaultValue: "${settings?.fanDevice?.displayName} - Automator"
        }
    }
}

def tempUnitStr() {
    return "\u00b0${getTemperatureScale()}"
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state?.isInstalled = true
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule("ruleCheck")
    initialize()
}

def initialize() {
    subscriber()
    runEvery30Minutes("ruleCheck")
    runIn(60,"ruleCheck", [overwrite:true])
    // runEvery1Minute("ruleCheck")
}

def subscriber() {
    if(settings?.pauseApp != true && settings?.fanDevice) {
        if (settings?.skipModes) { subscribe(location, "mode", triggerEvtHandler) }
        subscribe(settings?.fanDevice, "switch", triggerEvtHandler)
        if(settings?.fanDevice?.hasAttribute("level")) { subscribe(settings?.fanDevice, "level", triggerEvtHandler) }
        if(settings?.tempSensors) { subscribe(settings?.tempSensors, "temperature", triggerEvtHandler) }
        if(settings?.humiditySensors) { subscribe(settings?.humiditySensors, "humidity", triggerEvtHandler) }
        if(settings?.tstatDevice) { subscribe(settings?.tstatDevice, "thermostatOperatingState", triggerEvtHandler) }
        
        if(settings?.skipSwitchesOff) { subscribe(settings?.skipSwitchesOff, "switch", triggerEvtHandler) }
        if(settings?.skipSwitchesOn) { subscribe(settings?.skipSwitchesOn, "switch", triggerEvtHandler) }
        if(settings?.motionSensors) { subscribe(settings?.motionSensors, "motion", triggerEvtHandler) }

        //Handles Turn Off Device Events
        if(settings?.off4SwitchOn) 	{ subscribe(settings?.off4SwitchOn, "switch", triggerEvtHandler) }
        if(settings?.off4ContactOpen) { subscribe(settings?.off4ContactOpen, "contactSensor", triggerEvtHandler) }
        if(settings?.off4ContactClose) { subscribe(settings?.off4ContactClose, "contactSensor", triggerEvtHandler) }
    }
}

def triggerEvtHandler(evt) {
    log.trace("Device: ${evt?.displayName} | Type: ${evt?.name.toUpperCase()} | Value: (${evt?.value.toString().capitalize()}${evt.unit ? "${evt.unit}" : ""}) | Delay: (${((now()-evt.date.getTime())/1000).toDouble().round(2)}sec) | Source: (${evt?.source}) | Physical: (${evt.isPhysical()}) | Digital: (${evt?.isDigital()})")
    // TODO: If physical device change that activate change override
    ruleCheck()
}

def checkTstatState() {
    String cur = tstatDevice?.currentState("thermostatOperatingState")?.value ?: ""
    return (cur != "" && cur != null && settings?.tstatHvacStates && settings?.tstatHvacStates?.contains(cur?.toString()?.replaceAll("\\[|\\]", ""))) ? true : false
}

def ignoreActions(returnStr=false) {
    if(settings?.pauseApp == true) {
        log.trace "RuleCheck | Changes Restricted | Reason: (Automation Paused)"
        return returnStr ? "Automation Paused" : true
    }
    if(settings?.skipModes && isInMode(settings?.skipModes)) { 
        log.trace "RuleCheck | Changes Restricted | Reason: (Skip Changes when Mode (${location?.mode}) is ACTIVE)"
        return returnStr ? "Mode (${location?.mode}) is ACTIVE" : true
    }
    if(settings?.skipSwitchesOn && areSwitchesOn(settings?.skipSwitchesOn)) {
        log.trace "RuleCheck | Changes Restricted | Reason: (Skip Changes when these Switches ${getSwitchesOn(settings?.skipSwitchesOn)} are ON)"
        return returnStr ? "Switches ${getSwitchesOn(settings?.skipSwitchesOn)} are ON" : true
    }
    if(settings?.skipSwitchesOff && areSwitchesOff(settings?.skipSwitchesOff)) { 
        log.trace "RuleCheck | Changes Restricted | Reason: (No Changes while these Switches ${getSwitchesOff(settings?.skipSwitchesOff)} are OFF)"
        return returnStr ? "Switches ${getSwitchesOff(settings?.skipSwitchesOff)} are OFF" : true
    }
    return returnStr ? null : false
}

void ruleCheck() {
    if(ignoreActions() == true) { return }
    
    def fanState = settings?.fanDevice?.currentState("switch")?.value
    if(settings?.off4ContactOpen && areContactsOpen(settings?.off4ContactOpen)) { 
        def openCnt = getOpenContacts(settings?.off4ContactOpen)?.collect { it?.displayName }
        if(fanState != "off") {
            log.info "Rule Check | Turning OFF (${settings?.fanDevice?.displayName}) | Reason: (Contact(s) (${openCnt}) are OPEN"
            sendFanCmd(settings?.fanDevice, "off")
        } else { log.trace "Rule Check | Skipping Action there is Nothing to Change..." }
        return
    }
    if(settings?.off4ContactClose && areContactsClosed(settings?.off4ContactClose)) { 
        def clsCnt = getClosedContacts(settings?.off4ContactClose)?.collect { it?.displayName }
        if(fanState != "off") {
            log.info "Rule Check | Turning OFF (${settings?.fanDevice?.displayName}) | Reason: (Contact(s) (${clsCnt}) are CLOSED"
            sendFanCmd(settings?.fanDevice, "off")
        } else { log.trace "Rule Check | Skipping Action there is Nothing to Change..." }
        return
    }
    if(settings?.off4SwitchOn && areSwitchesOn(settings?.off4SwitchOn)) { 
        def onSw = getSwitchesOn(settings?.off4SwitchOn)?.collect { it?.displayName }
        if(fanState != "off") {	
            log.info "Rule Check | Turning OFF (${settings?.fanDevice?.displayName}) | Reason: (Switch${swOn?.size() > 1 ? "es" : ""} (${onSw}) ${swOn?.size() > 1 ? "are" : "is"} ON"
            sendFanCmd(settings?.fanDevice, "off")
        } else { log.trace "Rule Check | Skipping Action there is Nothing to Change..." }
        return
    }
    if(settings?.motionSensors) {
        if(areMotionsActive(settings?.motionSensors)) {
            def mots = getMotionsActive(settings?.motionSensors)?.collect { it?.displayName }
            
            if(state?.speedType != null && settings?.motionActiveFanSpeed) {
                log.info "Rule Check | Setting Speed to ${settings?.motionActiveFanSpeed} on (${settings?.fanDevice?.displayName}) | Reason: (Motion Detected (${mots})"
                sendFanCmd(settings?.fanDevice, settings?.motionActiveFanSpeed)
                return
            } else {
                log.info "Rule Check | Turning ON (${settings?.fanDevice?.displayName}) | Reason: (Motion Detected (${mots})"
                sendFanCmd(settings?.fanDevice, "on")
                return
            }
        } else {
            if(state?.speedType != null && settings?.motionInactiveFanSpeed) {
                log.info "Rule Check | Setting Speed to ${settings?.motionInactiveFanSpeed} on (${settings?.fanDevice?.displayName}) | Reason: (Motion Detected (${mots})"
                sendFanCmd(settings?.fanDevice, settings?.motionInactiveFanSpeed)
                return
            } else if (settings?.motionInactiveTurnOff) {
                log.info "Rule Check | Turning OFF (${settings?.fanDevice?.displayName}) | Reason: (Motion Detected (${mots})"
                sendFanCmd(settings?.fanDevice, "off")
                return
            }
        }
    }
    
    if(settings?.tstatDevice) {
        if(checkTstatState()) {
            state?.tstatRunning = true
            if(state?.speedType) {
                def curSpeedVal = getFanSpeedNumValue(fanSpeedConversion(getCurrentFanSpeed(settings?.fanDevice)))
                def reqSpeedVal = getFanSpeedNumValue(settings?.tstatSetFanSpeed)
                if(curSpeedVal && reqSpeedVal) {
                    if(curSpeedVal < reqSpeedVal) { 
                        log.info "Rule Check | Changing Fan Speed to ${settings?.tstatSetFanSpeed} (${settings?.fanDevice?.displayName}) | Reason: (HVAC is active so Fan Used to Circulate Air)"
                        sendFanCmd(settings?.fanDevice, "${settings?.tstatSetFanSpeed}")
                    } else {
                        log.trace "Rule Check | Skipping Changes... Thermostat Controlled Fan Speed is already Set to (${settings?.tstatSetFanSpeed})"
                    }
                }
            } else {
                if(settings?.fanDevice?.currentState("switch")?.value != "on") {
                    log.info "Rule Check | Turning Fan ON (${settings?.fanDevice?.displayName}) | Reason: (HVAC is active so Fan Used to Circulate Air)"
                    sendFanCmd(settings?.fanDevice, "on")
                }
            }
        } else {
            state?.tstatRunning = false 
            if(!settings?.tempSensors && !settings?.humiditySensors && settings?.fanDevice?.currentState("switch")?.value != "off") {
                log.info "Rule Check | Turning Fan Off (${settings?.fanDevice?.displayName}) | Reason: (HVAC is not active so Stopping Fan)"
                sendFanCmd(settings?.fanDevice, "off")
            }
        }
    } else { state?.tstatRunning = false }

    if(!settings?.tstatDevice || (settings?.tstatDevice && state?.tstatRunning != true)) {
        // fanState = settings?.fanDevice?.currentState("switch")?.value
        def tempReading = settings?.tempSensors ? getDeviceTempAvg(settings?.tempSensors) : null
        def humReading = settings?.humiditySensors ? getDeviceTempAvg(settings?.humiditySensors) : null
        if(state?.speedType) {
            def desiredSpeed = state?.speedType ? getFanSpeedToUse(settings?.fanDevice) : null
            def currentSpeed = fanSpeedConversion(getCurrentFanSpeed(settings?.fanDevice))
            log.debug "SpeedType: (${state?.speedType}) | CurrentSpeed: (${fanSpeedConversion(getCurrentFanSpeed(settings?.fanDevice))}) | DesiredSpeed: ($desiredSpeed) | CurFanState: ($fanState) | Temp: ($tempReading${tempUnitStr()}) | Humidity: ($humReading${humReading ? "%" : ""}) | LastChg: (${GetTimeDiffSeconds(state?.lastChangeDt)}sec)"
            if( settings?.tempSensors && ((desiredSpeed && desiredSpeed != "off") || (desiredSpeed == "off" && fanState != "off")) ) {
                if(desiredSpeed != currentSpeed) {
                    log.info "Rule Check | Changing Fan Speed to ${desiredSpeed} (${settings?.fanDevice?.displayName}) | Reason: (Temperature is in ${desiredSpeed?.toUpperCase()} Range)"
                    sendFanCmd(settings?.fanDevice, desiredSpeed)
                } else {
                    log.trace "Rule Check | Skipping Changes... Fan Speed is already Set to (${desiredSpeed})"
                }
            }
            else if(settings?.humiditySensors && humReading) {
                if(settings?.humiditySetFanSpeed) {
                    desiredSpeed = (humReading > settings?.fanOnHumidityVal) ? settings?.humiditySetFanSpeed : "off" 
                    if(desiredSpeed != currentSpeed) {
                        log.info "Rule Check | Changing Fan Speed to ${desiredSpeed} (${settings?.fanDevice?.displayName}) | Reason: (Humidity Threshold Activity ${settings?.fanOnHumidityVal}%)"
                        sendFanCmd(settings?.fanDevice, desiredSpeed)
                    } else {
                        log.trace "Rule Check | Skipping Changes... Fan Speed is already Set to (${desiredSpeed})"
                    }
                } 
            } else {
                log.trace "Rule Check | Skipping Action there is Nothing to Change..."
            }
        } else {
            if(settings?.humiditySensors && humReading) {
                def desiredState = (humReading > settings?.fanOnHumidityVal) ? "on" : "off"
                log.debug "DesiredSpeed: ($desiredState) | CurFanState: ($fanState) | Humidity: ($humReading${humReading ? "%" : ""}) | LastChg: (${GetTimeDiffSeconds(state?.lastChangeDt)}sec)"
                if(fanState != desiredState) {
                    log.info "Rule Check | Turning Fan ${desiredState?.toString()?.toUpperCase()} (${settings?.fanDevice?.displayName}) | Reason: (Humidity Threshold Activity ${settings?.fanOnHumidityVal}%)"
                    sendFanCmd(settings?.fanDevice, desiredState)
                } else {
                    log.trace "Rule Check | Skipping Changes... Fan State is already Set to ($desiredState)"
                }
            } else if( settings?.tempSensors && tempReading) {
                def desiredState = (tempReading > settings?.fanNoSpeedOnVal) ? "on" : "off"
                log.debug "DesiredSpeed: ($desiredState) | CurFanState: ($fanState) | Temp: ($tempReading${tempUnitStr()}) | LastChg: (${GetTimeDiffSeconds(state?.lastChangeDt)}sec)"
                if(fanState != desiredState) {
                    log.info "Rule Check | Turning Fan ${desiredState?.toString()?.toUpperCase()} (${settings?.fanDevice?.displayName}) | Reason: (Temperature Threshold Activity ${settings?.fanNoSpeedOnVal}${tempUnitStr()})"
                    sendFanCmd(settings?.fanDevice, desiredState)
                } else {
                    log.trace "Rule Check | Skipping Changes... Fan State is already Set to ($desiredState)"
                }
            } else {
                log.trace "Rule Check | Skipping Action there is Nothing to Change..."
            }
        }
    }
}

def sendFanCmd(dev, cmd) {
    if(dev && cmd) {
        if(settings?.changeDelay && state?.lastChangeDt && GetTimeDiffSeconds(state?.lastChangeDt) < settings?.changeDelay?.toInteger()) {
            log.warn "Rule Check | Too Soon to Make Changes... Last Change: ${GetTimeDiffSeconds(state?.lastChangeDt)} sec | Minimum Delay: ${settings?.changeDelay} | Scheduling Rule Check in 60 Seconds"
            runIn(60, "ruleCheck", [overwrite: true])
            return
        }
        if(dev?.hasCommand(cmd)) {
            log.info "Sending ${cmd} to ${dev?.displayName}"
            state?.lastChangeDt = getDtNow()
            dev?."${cmd}"()
        } else {
            log.error "${dev?.displayName} DOES NOT Support the ${cmd} Command... Available Commands are ${dev?.supportedCommands?.collect { it?.name }}"
        }
    }
}

def getFanSpeedNumValue(spd) {
    switch(spd) {
        case "off":
            return 0
            break
        case "lowSpeed":
            return 1
            break
        case "medSpeed":
            return 2
            break
        case "medHighSpeed":
            return 3
            break
        case "highSpeed":
            return 4
            break
    }
    return null
}

def fanSpeedConversion(spd) {
    switch(spd) {
        case "off":
            return "off"
            break
        case "LOW":
        case "Low":
        case "low":
        case "lowSpeed":
            return "lowSpeed"
            break
        case "MED":
        case "Med":
        case "med":
        case "medSpeed":
            return "medSpeed"
            break
        case "MED-HIGH":
        case "Med-High":
        case "med-high":
        case "medHighSpeed":
            return "medHighSpeed"
            break
        case "HIGH":
        case "High":
        case "high":
        case "highSpeed":
            return "highSpeed"
            break
    }
    return null
}

def getFanSpeedToUse(dev, retStr=false) {
    if(!dev) { return null }
    def tempReading = getDeviceTempAvg(settings?.tempSensors)

    if(tempReading?.isNumber()) {
        // log.debug "tempReading: $tempReading | allowHighSpeed: ${settings?.allowHighSpeed} | offSpeedRange: ${settings?.offSpeedRange} | lowSpeedRange: ${settings?.lowSpeedRange} | medSpeedRange: ${settings?.medSpeedRange} | medHighSpeedRange: ${settings?.medHighSpeedRange}"
        
        // log.debug "HighSpeed | (settings?.allowHighSpeed && ((settings?.medHighSpeedRange && tempReading > settings?.medHighSpeedRange) || (!settings?.medHighSpeedRange && settings?.medSpeedRange && tempReading > settings?.medSpeedRange))): [${(settings?.allowHighSpeed && ((settings?.medHighSpeedRange && tempReading > settings?.medHighSpeedRange) || (!settings?.medHighSpeedRange && settings?.medSpeedRange && tempReading > settings?.medSpeedRange))) ? "TRUE" : "FALSE"}]"
        // //log.debug "HighSpeed | (tempReading: $tempReading > medSpeedRange: ${settings?.medSpeedRange}): [${((settings?.allowHighSpeed && !settings?.medHighSpeedRange && settings?.medSpeedRange) && (tempReading > settings?.medSpeedRange)) ? "TRUE" : "FALSE"}]"
        // log.debug "MedHighSpeed | (settings?.medHighSpeedRange && tempReading <= settings?.medHighSpeedRange && tempReading > settings?.medSpeedRange): [${(settings?.medHighSpeedRange && tempReading <= settings?.medHighSpeedRange && tempReading > settings?.medSpeedRange) ? "TRUE" : "FALSE"}]"
        // log.debug "MedSpeed | (tempReading: $tempReading <= medSpeedRange: ${settings?.medSpeedRange} && tempReading: $tempReading > lowSpeedRange: ${settings?.lowSpeedRange}): [${(!settings?.medHighSpeedRange && settings?.medSpeedRange && tempReading <= settings?.medSpeedRange && tempReading > settings?.lowSpeedRange) ? "TRUE" : "FALSE"}]"
        // log.debug "LowSpeed | (tempReading: $tempReading <= lowSpeedRange: ${settings?.lowSpeedRange} && tempReading: $tempReading > offSpeedRange: ${settings?.offSpeedRange}): [${(settings?.lowSpeedRange && settings?.offSpeedRange && tempReading <= settings?.lowSpeedRange && tempReading > settings?.offSpeedRange) ? "TRUE" : "FALSE"}]"
        // log.debug "OffSpeed | (tempReading: $tempReading <= offSpeedRange: ${settings?.offSpeedRange}): [${(settings?.offSpeedRange && tempReading <= settings?.offSpeedRange) ? "TRUE" : "FALSE"}]"
        if (settings?.allowHighSpeed && ((settings?.medHighSpeedRange && tempReading > settings?.medHighSpeedRange) || (!settings?.medHighSpeedRange && settings?.medSpeedRange && tempReading > settings?.medSpeedRange))) {
            return retStr ? "High" : "highSpeed"
        } else if (settings?.medHighSpeedRange && tempReading <= settings?.medHighSpeedRange && tempReading > settings?.medSpeedRange) {
            return retStr ? "Med-High" : "medHighSpeed"
        } else if (settings?.medSpeedRange && settings?.lowSpeedRange && tempReading <= settings?.medSpeedRange && tempReading > settings?.lowSpeedRange) {
            return retStr ? "Med" : "medSpeed"
        } else if (settings?.lowSpeedRange && settings?.offSpeedRange && tempReading <= settings?.lowSpeedRange && tempReading > settings?. offSpeedRange) {
            return retStr ? "Low" : "lowSpeed"
        } else if (settings?.offSpeedRange && tempReading <= settings?.offSpeedRange) {
            return retStr ? "Off" : "off"
        }
    }
}

def getLastChangeSec() { return !state?.lastChangeDt ? 100000 : GetTimeDiffSeconds(state?.lastChangeDt, null, "getLastChangeSec").toInteger() }

def setFanSpeedsAvail(dev) {
    def fanCmds = dev?.supportedCommands?.collect { it?.name }
    if(fanCmds?.contains("lowSpeed") && fanCmds?.contains("medSpeed") && fanCmds?.contains("medHighSpeed") && fanCmds?.contains("highSpeed")) {
        state?.speedType = "4speed"
    } else if(fanCmds?.contains("lowSpeed") && fanCmds?.contains("medSpeed") && fanCmds?.contains("highSpeed")) {
        state?.speedType = "3speed"
    } else {
        state?.speedType = null
    }
    // log.debug "speedType: ${state?.speedType}"
}

def getDeviceTempAvg(items) {
    def tmpAvg = []
    def tempVal = 0
    if(!items) { return tempVal }
    else if(items?.size() > 1) {
        tmpAvg = items*.currentTemperature
        if(tmpAvg && tmpAvg?.size() > 1) { tempVal = (tmpAvg?.sum().toDouble() / tmpAvg?.size().toDouble()).round(1) }
    }
    else { tempVal = getDeviceTemp(items) }
    return tempVal.toDouble()
}

def getDeviceHumidityAvg(items) {
    def humAvg = []
    def humVal = 0
    if(!items) { return humVal }
    else if(items?.size() > 1) {
        humAvg = items*.currentHumidity
        if(humAvg && humAvg?.size() > 1) { humVal = (humAvg?.sum().toDouble()/humAvg?.size().toDouble()).round(1) }
    }
    else { humVal = getDeviceTemp(items) }
    return humVal.toDouble()
}

def getDeviceTemp(dev) {
    def temp = dev?.currentValue("temperature")
    return temp && "$temp"?.isNumber() ? temp?.toDouble() : 0
}

def getDeviceHumidity(dev) {
    def hum = dev?.currentValue("humidity")
    return hum && "$hum"?.isNumber() ? hum?.toDouble() : 0
}

private tempRangeValues(asMap=false) {
    def useC = (getTemperatureScale() == "C")
    def low = useC ? "10" : "50"
    def high = useC ? "32" : "90"
    if(asMap) { return [low:low, high:high] } 
    return "${low}..${high}"
}

def waitValEnum() {
    return [ 1800:"30 Minutes", 3600:"1 Hour", 7200:"2 Hours", 14400:"4 Hours", 21600:"6 Hours", 43200:"12 Hours", 86400:"24 Hours" ]
}

def getSwitchesOn(sw) {
    if(sw) {
        def devs = sw?.findAll { it?.currentValue("switch") == "on" }
        // log.debug "devs: $devs"
        return devs?.size() ? devs : null
    }
    return null
}

def getSwitchesOff(sw) {
    if(sw) {
        def devs = sw?.findAll { it?.currentValue("switch") == "off" }
        // log.debug "devs: $devs"
        return devs?.size() ? devs : null
    }
    return null
}

def getOpenContacts(contacts) {
    if(contacts) {
        def cnts = contacts?.findAll { it?.currentValue("contact") == "open" }
        // log.debug "cnts: $cnts"
        return cnts?.size() ? cnts : null
    }
    return null
}

def getClosedContacts(contacts) {
    if(contacts) {
        def cnts = contacts?.findAll { it?.currentValue("contact") == "closed" }
        // log.debug "cnts: $cnts"
        return cnts?.size() ? cnts : null
    }
    return null
}

def getMotionsActive(dev) {
    if(dev) {
        def devs = dev?.findAll { it?.currentValue("motion") == "active" }
        // log.debug "devs: $devs"
        return devs?.size() ? devs : null
    }
    return null
}

def areMotionsActive(dev) {
    if(devs && devs?.find { it?.currentMotion == "active" }) { return true }
    return false
}

def areContactsOpen(devs) {
    if(devs && devs?.find { it?.currentContact == "open" }) { return true }
    return false
}

def areContactsClosed(devs) {
    if(devs && devs?.find { it?.currentContact == "close" }) { return true }
    return false
}

def areSwitchesOn(devs) {
    if(devs && devs?.find { it?.currentSwitch == "on" }) { return true }
    return false
}

def areSwitchesOff(devs) {
    if(devs && devs?.find { it?.currentSwitch == "on" }) { return false }
    return true
}

def isInMode(modeList) {
    if(modeList) { return location.mode.toString() in modeList }
    return false
}

def getTimeZone() {
    def tz = null
    if(location?.timeZone) { tz = location?.timeZone }
    return tz
}

def getDtNow() {
    def now = new Date()
    return formatDt(now)
}

def formatDt(dt) {
    def tf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy")
    if(getTimeZone()) { tf.setTimeZone(getTimeZone()) }
    else { Logger("SmartThings TimeZone is not set; Please open your ST location and Press Save", "warn") }
    return tf.format(dt)
}

def GetTimeDiffSeconds(strtDate, stpDate=null, methName=null) {
    if((strtDate && !stpDate) || (strtDate && stpDate)) {
        def now = new Date()
        def stopVal = stpDate ? stpDate.toString() : formatDt(now)
        def start = Date.parse("E MMM dd HH:mm:ss z yyyy", strtDate).getTime()
        def stop = Date.parse("E MMM dd HH:mm:ss z yyyy", stopVal).getTime()
        def diff = (int) (long) (stop - start) / 1000 //
        //log.trace("[GetTimeDiffSeconds] Results for '$methName': ($diff seconds)")
        return diff
    } else { return null }
}

private timeComparisonOptionValues() {
    return ["custom time", "midnight", "sunrise", "noon", "sunset"]
}

private timeDayOfWeekOptions() {
    return ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
}

def appImg() { return "https://d1nhio0ox7pgb.cloudfront.net/_img/g_collection_png/standard/256x256/fan.png" }
def appInfoSect()	{
    def str = ""
    str += "${app?.name}"
    str += "\n• Version: ${appVer()}"
    str += "\n• Updated: ${appDate()}"
    section() { paragraph "${str}", image: appImg()	}
}

def Logger(msg, type) {
    if(msg && type) {
        switch(type) {
            case "info":
                log.info "${msg}"
                break
            case "trace":
                log.trace "${msg}"
                break
            case "error":
                log.error "${msg}"
                break
            case "warn":
                log.warn "${msg}"
                break
            default:
                log.debug "${msg}"
                break
        }
    }
}

void settingUpdate(name, value, type=null) {
    LogAction("settingUpdate($name, $value, $type)...", "trace", false)
    if(name && type) {
        app?.updateSetting("$name", [type: "$type", value: value])
    }
    else if (name && type == null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
    Logger("settingRemove($name)...", "trace")
    if(name) { app?.deleteSetting("$name") }
}

def stateUpdate(key, value) {
    if(key) { state?."${key}" = value }
    else { Logger("stateUpdate: null key $key $value", "error") }
}