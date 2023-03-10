/**
 *  Thermostat Manager
 *  Build 2018040102
 *
 *  Copyright 2018 Jordan Markwell
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
 *  ChangeLog:
 *      
 *      20180401
 *          01: Correcting a typo in logNNotify() that D_Gjorgjievski from the support forum discovered.
 *          02: Corrected a problem with the openContact variable inside of the tempHandler() function.
 *
 *      20180327
 *          01: Now accounting for all possible thermostat modes in tempHandler().
 *          02: Disabling Thermostat Manager will now disable Energy Saver.
 *          03: Logging and notifications will now continue to function even if a service is disabled (with the
 *              exception of the notification service itself).
 *          04: General code cleanup.
 *
 *      20180326
 *          01: Now accounting for all possible thermostat modes in contactClosedHandler().
 *          02: Adding (thermostatMode != "off") condition to openContactPause().
 *
 *      20180307
 *          01: If notifications are not configured or are disabled, quietly record qualifying events in the
 *              notification log.
 *          02: Changed logNNotify() log level to, "info".
 *
 *      20180306
 *          01: Added temperature threshold recommendations for Celsius.
 *          02: Correction to iconX2Url.
 *          03: Adding pausable setting to definition.
 *
 *      20180109
 *          01: Verify that a monitored contact remains open before allowing Energy Saver to pause the thermostat.
 *
 *      20180102
 *          01: tempHandler() will now check to ensure that Energy Saver states do not contradict the status of the
 *              contacts being monitored.
 *          02: Deleting a misplaced quotation mark.
 *
 *      20171218
 *          01: Don't set modes if Energy Saver has paused the thermostat due to open contacts.
 *
 *      20171216
 *          01: Apparently handler functions don't work without a parameter variable.
 *          02: Turned setPoint enforcement into scheduled functions.
 *
 *      20171215
 *          01: Added capability to automatically set thermostat to "off" mode in the case that user selected contact
 *              sensors have remained open for longer than a user specified number of minutes.
 *          02: Added ability to override Thermostat Manager by manually setting the thermostat to "off" mode.
 *          03: Added push notification capability.
 *          04: Modified logging behavior. Rearranged menus. General code cleanup.
 *          05: Added ability to disable Smart Home Monitor based setPoint enforcement without having to remove user
 *              defined values.
 *          06: Added ability to disable notifications without having to remove contacts.
 *          07: Missed a comma.
 *          08: Modifying notification messages.
 *          09: Converting tempHandler's event.value to integer.
 *          10: Returned to using thermostat.currentValue("temperature") instead of event.value.toInteger() for the
 *              currentTemp variable in the tempHandler() function.
 *
 *      20171213
 *          01: Standardized optional Smart Home Monitor based setPoint enforcement with corresponding preference
 *              settings.
 *          02: Added notification capabilities.
 *          03: Renamed from, "Simple Thermostat Manager" to, "Thermostat Manager".
 *          04: Corrected an incorrect setPoint preference variable.
 *          05: Edited the text of the text notification preference setting.
 *          06: Menu cleanup.
 *
 *      20171212
 *          01: Added Hello Home mode value and Smart Home Monitor status value to debug logging.
 *          02: Added a preliminary form of setPoint enforcement.
 *
 *      20171210
 *          01: Corrected a mistake in the help paragraph.
 *          02: Reconfigured the placement of the help text.
 *          03: Added the ability to have Simple Thermostat Manager ignore a temperature threshold by manually setting
 *              it to 0.
 *
 *      20171125
 *          01: Reverted system back to using user defined boundaries.
 *          02: Changed fanMode state check to check for "auto" instead of "fanAuto".
 *
 *      Earlier:
 *          Creation
 *          Modified to use established thermostat setPoints rather than user defined boundaries.
 */
 
definition(
    name: "Thermostat Manager",
    namespace: "jmarkwell",
    author: "Jordan Markwell",
    description: "Automatically changes the thermostat mode in response to changes in temperature that exceed user defined thresholds.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    pausable: false
)

preferences {
    page(name: "mainPage")
    page(name: "setPointPage")
    page(name: "notificationPage")
    page(name: "energySaverPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Thermostat Manager", install: true, uninstall: true) {
        section() {
            paragraph "Automatically changes the thermostat mode in response to changes in temperature that exceed user defined thresholds."
        }
        section("Main Configuration") {
            input "thermostat", "capability.thermostat", title: "Thermostat", multiple: false, required: true
            paragraph "When the temperature rises higher than the cooling threshold, Thermostat Manager will set cooling mode. Recommended value: 75F (24C)"
            input name: "coolingThreshold", title: "Cooling Threshold", type: "number", required: false
            paragraph "When the temperature falls below the heating threshold, Thermostat Manager will set heating mode. Recommended value: 70F (21C)"
            input name: "heatingThreshold", title: "Heating Threshold", type: "number", required: false
        }
        section("Tips") {
            paragraph "If you set the cooling threshold at the lowest setting you use in your modes and you set the heating threshold at the highest setting you use in your modes, you will not need to create multiple instances of Thermostat Manager."
            paragraph "If you want to use Thermostat Manager to set cooling mode only or to set heating mode only, remove the value for the threshold that you want to be ignored or set it to 0."
        }
        section("Optional Settings") {
            input name: "setFan", title: "Maintain Auto Fan Mode", type: "bool", defaultValue: true, required: true
            input name: "manualOverride", title: "Allow Manual Thermostat Off to Override Thermostat Manager", type: "bool", defaultValue: false, required: true
            input name: "debug", title: "Debug Logging", type: "bool", defaultValue: false, required: true
            input name: "disable", title: "Disable Thermostat Manager", type: "bool", defaultValue: false, required: true
            
            href "setPointPage", title: "Smart Home Monitor Based SetPoint Enforcement"
            href "energySaverPage", title: "Energy Saver"
            href "notificationPage", title: "Notification Settings"
            
            label(title: "Assign a name", required: false)
            mode(title: "Set for specific mode(s)")
        }
    }
}

def setPointPage() {
    dynamicPage(name: "setPointPage", title: "Smart Home Monitor Based SetPoint Enforcement") {
        section() {
            paragraph "These optional settings allow you use Thermostat Manager to set your thermostat's cooling and heating setPoints based on the status of Smart Home Monitor; SmartThings' built-in security system. SetPoints will be set only when a thermostat mode change occurs (e.g. heating to cooling) and only the setPoint for the incoming mode will be set (e.g. A change from heating mode to cooling mode would prompt the cooling setPoint to be set)."
        }
        section("Disarmed Status") {
            input name: "offCoolingSetPoint", title: "Cooling SetPoint", type: "number", required: false
            input name: "offHeatingSetPoint", title: "Heating SetPoint", type: "number", required: false
        }
        section("Armed (stay) Status") {
            input name: "stayCoolingSetPoint", title: "Cooling SetPoint", type: "number", required: false
            input name: "stayHeatingSetPoint", title: "Heating SetPoint", type: "number", required: false
        }
        section("Armed (away) Status") {
            input name: "awayCoolingSetPoint", title: "Cooling SetPoint", type: "number", required: false
            input name: "awayHeatingSetPoint", title: "Heating SetPoint", type: "number", required: false
        }
        section() {
            input name: "disableSHMSPEnforce", title: "Disable Smart Home Monitor Based SetPoint Enforcement", type: "bool", defaultValue: false, required: true
        }
    }
}

def notificationPage() {
    dynamicPage(name:"notificationPage", title:"Notification Settings") {
        section() {
            input(name: "recipients", title: "Select Notification Recipients", type: "contact", required: false) {
                input name: "phone", title: "Enter Phone Number of Text Message Notification Recipient", type: "phone", required: false
            }
            input name: "pushNotify", title: "Send Push Notifications", type: "bool", defaultValue: false, required: true
            input name: "disableNotifications", title: "Disable Notifications", type: "bool", defaultValue: false, required: true
        }
    }
}

def energySaverPage() {
    dynamicPage(name:"energySaverPage", title:"Energy Saver") {
        section() {
            paragraph "Energy Saver will temporarily pause the thermostat (by placing it in \"off\" mode) in the case that any selected contact sensors are left open for a specified number of minutes."
            input name: "contact", title: "Contact Sensors", type: "capability.contactSensor", multiple: true, required: false
            input name: "openContactMinutes", title: "Minutes", type: "number", required: false
            input name: "disableEnergySaver", title: "Disable Energy Saver", type: "bool", defaultValue: false, required: true
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    state.clear()
    
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(thermostat, "temperature", tempHandler)
    subscribe(contact, "contact.open", contactOpenHandler)
    subscribe(contact, "contact.closed", contactClosedHandler)
}

def tempHandler(event) {
    def openContact     = contact?.currentValue("contact")?.contains("open")
    def currentTemp     = thermostat.currentValue("temperature")
    def coolingSetpoint = thermostat.currentValue("coolingSetpoint")
    def heatingSetpoint = thermostat.currentValue("heatingSetpoint")
    def thermostatMode  = thermostat.currentValue("thermostatMode")
    def fanMode         = thermostat.currentValue("thermostatFanMode")
    def homeMode        = location.mode
    def securityStatus  = location.currentValue("alarmSystemStatus")
    
    if (!openContact && state.lastThermostatMode) {
        // Invalid values could potentially occur if using multiple instances of Thermostat Manager.
        state.clear()
    }
    
    if (debug) {
        if (!disableEnergySaver && contact) {
            log.debug "Thermostat Manager - At least one contact is open: ${openContact}"
            if (state.lastThermostatMode) { log.debug "Thermostat Manager is currently paused." }
        }
        log.debug "Thermostat Manager - Smart Home Monitor Status: ${securityStatus}"
        log.debug "Thermostat Manager - Hello Home Mode: ${homeMode}"
        log.debug "Thermostat Manager - Fan Mode: ${fanMode}"
        log.debug "Thermostat Manager - Mode: ${thermostatMode}"
        log.debug "Thermostat Manager - Cooling Setpoint: ${coolingSetpoint} | Heating Setpoint: ${heatingSetpoint}"
        log.debug "Thermostat Manager - Temperature: ${currentTemp}"
    }
   
    if ( (!disable) && (setFan) && (fanMode != "auto") ) {
        logNNotify("Thermostat Manager setting fan mode auto.")
        thermostat.fanAuto()
    }
    
    // Temperature setPoints only stick for the active mode.
    if (
            !disable && (disableEnergySaver || !state.lastThermostatMode) &&
            ( (thermostatMode != "cool") && ( !manualOverride || (manualOverride && (thermostatMode != "off") ) ) ) &&
            coolingThreshold && ( Math.round(currentTemp) > Math.round(coolingThreshold) )
       ) {
        
        logNNotify("Thermostat Manager - The temperature has risen to ${currentTemp}. Setting cooling mode.")
        thermostat.cool()
        
        if (!disableSHMSPEnforce) {
            if ( (securityStatus == "off") && (offCoolingSetPoint) ) {
                // SetPoints won't be changed unless the thermostat is already in the required mode.
                runIn( 60, enforceCoolingSetPoint, [data: [setPoint: offCoolingSetPoint] ] )
            } else if ( (securityStatus == "stay") && (stayCoolingSetPoint) ) {
                runIn( 60, enforceCoolingSetPoint, [data: [setPoint: stayCoolingSetPoint] ] )
            } else if ( (securityStatus == "away") && (awayCoolingSetPoint) ) {
                runIn( 60, enforceCoolingSetPoint, [data: [setPoint: awayCoolingSetPoint] ] )
            }
        }
    } else if (
            !disable && (disableEnergySaver || !state.lastThermostatMode) &&
            ( (thermostatMode != "heat") && ( !manualOverride || (manualOverride && (thermostatMode != "off") ) ) ) &&
            heatingThreshold && ( Math.round(currentTemp) < Math.round(heatingThreshold) )
        ) {
        
        logNNotify("Thermostat Manager - The temperature has fallen to ${currentTemp}. Setting heating mode.")
        thermostat.heat()
        
        if (!disableSHMSPEnforce) {
            if ( (securityStatus == "off") && (offHeatingSetPoint) ) {
                runIn( 60, enforceHeatingSetPoint, [data: [setPoint: offHeatingSetPoint] ] )
            } else if ( (securityStatus == "stay") && (stayHeatingSetPoint) ) {
                runIn( 60, enforceHeatingSetPoint, [data: [setPoint: stayHeatingSetPoint] ] )
            } else if ( (securityStatus == "away") && (awayHeatingSetPoint) ) {
                runIn( 60, enforceHeatingSetPoint, [data: [setPoint: awayHeatingSetPoint] ] )
            }
        }
    } else if (debug) {
        log.debug "Thermostat Manager standing by."
    }
}

def logNNotify(message) {
    log.info message
    if ( (!disableNotifications) && ( (location.contactBookEnabled && recipients) || phone || pushNotify ) ) {
        if (location.contactBookEnabled && recipients) {
            sendNotificationToContacts(message, recipients)
        } else if (phone) {
            sendSms(phone, message)
        }
        
        if (pushNotify) {
            sendPush(message)
        }
    } else {
        sendNotificationEvent(message)
    }
}

def enforceCoolingSetPoint(data) {
    logNNotify("Thermostat Manager is setting the cooling setPoint to ${data.setPoint}.")
    thermostat.setCoolingSetpoint(data.setPoint)
}

def enforceHeatingSetPoint(data) {
    logNNotify("Thermostat Manager is setting the heating setPoint to ${data.setPoint}.")
    thermostat.setHeatingSetpoint(data.setPoint)
}

def contactOpenHandler(event) {
    def thermostatMode = thermostat.currentValue("thermostatMode")
    
    if (debug) {
        log.debug "Thermostat Manager - A contact has been opened."
    }
    
    if (!disable && !disableEnergySaver && (thermostatMode != "off") && !state.openContactReported) {
        // If the thermostat is not off and all of the contacts were closed previously.
        state.openContactReported = true
        runIn( (openContactMinutes * 60), openContactPause )
        log.debug "Thermostat Manager - A contact has been opened. Initiating countdown to thermostat pause."
    }
}

def contactClosedHandler(event) {
    if (debug) {
        log.debug "Thermostat Manager - A contact has been closed."
    }
    
    if (!disable && !disableEnergySaver && state.openContactReported) {
        // If there was an open contact previously.
        if ( !contact.currentValue("contact").contains("open") ) {
            // All monitored contacts have been closed. Discontinue any existing countdown.
            log.debug "Thermostat Manager - All contacts have been closed. Discontinuing any existing thermostat pause countdown."
            unschedule(openContactPause)
            
            if (state.lastThermostatMode) {
                // If the thermostat is currently paused, restore it to its previous state.
                if (state.lastThermostatMode == "cool") {
                    logNNotify("Thermostat Manager - All contacts have been closed. Restoring cooling mode.")
                    thermostat.cool()
                } else if (state.lastThermostatMode == "auto") {
                    logNNotify("Thermostat Manager - All contacts have been closed. Restoring auto mode.")
                    thermostat.auto()
                } else { // state.openContactReported will not be set unless (state.lastThermostatMode != "off").
                    // It appears that most thermostat device handlers disallow, "emergency heat" mode.
                    logNNotify("Thermostat Manager - All contacts have been closed. Setting heating mode.")
                    thermostat.heat()
                }
                state.lastThermostatMode = null
            }
            state.openContactReported = false
        }
    }
}

def openContactPause() {
    def thermostatMode = thermostat.currentValue("thermostatMode")
    
    if ( (thermostatMode != "off") && contact.currentValue("contact").contains("open") ) {
        // If the thermostat is not in, "off" mode and any monitored contact is open.
        state.lastThermostatMode = thermostat.currentValue("thermostatMode")
        logNNotify("Thermostat Manager is turning the thermostat off temporarily due to an open contact.")
        thermostat.off()
    } else { // If the thermostat was turned off after an open contact was reported or no monitored contacts remain open.
        state.clear()
    }
}
