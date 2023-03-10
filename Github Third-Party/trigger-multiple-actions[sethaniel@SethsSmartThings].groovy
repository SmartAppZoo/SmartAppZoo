/**
*  Trigger Commands
*
*  Copyright 2016 Seth Munroe
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
    name: "Trigger Multiple Actions",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "This will let you set a virtual switch (simulated switch) to trigger many actions including locking/unlockng doors, turning switches on or off, dimming lights, setting color temps for lights, setting colors for lights, opening/closing doors, opening/closing window shades.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/alfred-app.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/alfred-app@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/alfred-app-full@2x.png")


preferences {
    page(name: "pageTriggerSwitch", nextPage: "pageDevices", install: false, uninstall: true)
    page(name: "pageDevices", nextPage: "pageSwitchSettings", install: false, uninstall: true)
    page(name: "pageSwitchSettings", install: true, uninstall: true)
}

def pageTriggerSwitch() {
    dynamicPage(name: "pageTriggerSwitch", title: "Trigger Switch") {
        section("Trigger Switch Settings") {
            input(name: "triggerSwitch", type: "capability.switch", title: "The trigger switch will cause all of the actions to start. Select a trigger switch:", multiple: false, required: true)
            input(name: "triggerEvent", type: "enum", title: "Which state of this switch should trigger the actions?", options: ["on", "off"], required: true)
            input(name: "triggerReset", type: "bool", title: "Should the event be switched back automatically so that the trigger can be used again?", required: true) 
        }

        section("Notifications") {
            paragraph("Notifications can be sent to specific users by SMS or to everyone by SmartThings App Push notifications.", title: "SMS/Push Notifications:", required: true)
            input(name: "recipients", type: "contact", title: "Send notifications to:", multiple: true) {
                input(name: "inPhone", type: "phone", title: "Send SMS notifications to: (optional: comma separated list of phone numbers)",
                      description: "Phone Number", required: false, multiple: true)
            }
            input(name: "notifyPush", type: "bool", title: "Send notifications to all logged in devices for all users of location: ${location.name}?", defaultValue: false)
        }

        section("Application Information", mobileOnly:true) {
            icon(title: "Pick an Icon for the app.", reuired: false)
            label(title: "Assign a name", required: false)
            mode(title: "Set for specific mode(s)", required: false)
        }

    }
}

def pageDevices() {
    dynamicPage(name: "pageDevices", title: "Devices") {
        section("Select the devices to be controlled") {
            input(name: "selectedLocks", type: "capability.lock", title: "Select all locks that will be controlled:", required: false, multiple: true)
            input(name: "selectedSwitches", type: "capability.switch", title: "Select all switches that will be controlled:", required: false, multiple: true)
        }
    }
}

def pageSwitchSettings() {
    // values in this page were lost if the user went in to update settings
    // using the current values as defaultValue settings lets the user keep current settings when updating.
    dynamicPage(name: "pageSwitchSettings", title: "Device Settings") {
        selectedLocks.each { oneLock ->
            section("settings for Lock: ${oneLock.displayName}") {
                input(name: "lockOn-${oneLock.id}", type: "enum", options: ["lock", "unlock"], defaultValue: getSettingByPrefixAndId("lockOn-", oneLock.id), required: true, title: "Set the ${oneLock.displayName} to:")
            }
        }

        selectedSwitches.each { oneSwitch ->
            section("settings for switch: ${oneSwitch.displayName}") {

                input(name: "switchOn-${oneSwitch.id}", type: "enum", options: ["on", "off"], defaultValue: getSettingByPrefixAndId("switchOn-", oneSwitch.id), required: true, title: "Set the ${oneSwitch.displayName} to:")

                if (oneSwitch.hasCapability("Switch Level")) {
                    input(name: "switchLevel-${oneSwitch.id}", type: "number", range: "0..100", defaultValue: getSettingByPrefixAndId("switchLevel-", oneSwitch.id), required: false, title: "This setting will be ignored if the device is being turned off. Dim the ${oneSwitch.displayName} to (percentage: 0 - 100):")
                }

                if (oneSwitch.hasCapability("Color Temperature")) {
                    input(name: "colorTemp-${oneSwitch.id}", type: "number", range: "0..*", defaultValue: getSettingByPrefixAndId("colorTemp-", oneSwitch.id), required: false, title: "This setting will be ignored if the device is being turned off. Set the Color Temperature of ${oneSwitch.displayName} to (kelvin temp: values of 0 and up are allowed, but many devices only accept values of 2000 - 6500):")
                }

                if (oneSwitch.hasCapability("Color Control")) {
                    input(name: "colorHue-${oneSwitch.id}", type: "number", range: "0..100", defaultValue: getSettingByPrefixAndId("colorHue-", oneSwitch.id), required: false, title: "This setting will be ignored if the device is being turned off or if a color temperature value is set. Set the Color Hue of ${oneSwitch.displayName} to (percentage: 0 - 100):")
                    input(name: "colorSaturation-${oneSwitch.id}", type: "number", defaultValue: getSettingByPrefixAndId("colorSaturation-", oneSwitch.id), range: "0..100", required: false, title: "This setting will be ignored if the device is being turned off or if a color temperature value is set. Set the Color Saturation of ${oneSwitch.displayName} to (percentage: 0 - 100):")
                }
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(triggerSwitch, "switch.${triggerEvent}", triggerThrown)
}

/*
This will return the setting for the specified device
*/
def getSettingByPrefixAndId(settingNamePrefix, idOfDevice) {
    def retSetting = settings.findAll {it.key == settingNamePrefix + idOfDevice}
    def retValue = null
    retSetting.each {thisOne ->
        retValue = thisOne.value
    }

    return retValue
}

def controlSwitches() {
    selectedSwitches.each { oneSwitch ->
        def switchVal = getSettingByPrefixAndId('switchOn-', oneSwitch.id)
        log.debug "${oneSwitch?.displayName}: return from getSettingByPrefixAndId = ${switchVal}"

        if (switchVal == "off") {
            oneSwitch.off()
        } else if (switchVal == "on"){
            oneSwitch.on()

            def switchLevelVal = getSettingByPrefixAndId("switchLevel-", oneSwitch.id)
            def switchColorTemp = getSettingByPrefixAndId("colorTemp-", oneSwitch.id)
            def switchColorHue = getSettingByPrefixAndId("colorHue-", oneSwitch.id)
            def switchColorSaturation = getSettingByPrefixAndId("colorSaturation-", oneSwitch.id)

            log.debug "${oneSwitch.displayName}: switchLevelVal: ${switchLevelVal}"
            log.debug "${oneSwitch.displayName}: switchColorTemp: ${switchColorTemp}"
            log.debug "${oneSwitch.displayName}: switchColorHue: ${switchColorHue}"
            log.debug "${oneSwitch.displayName}: switchColorSaturation: ${switchColorSaturation}"

            if (switchLevelVal != null) {

                oneSwitch.setLevel(switchLevelVal)
            }

            if (switchColorTemp != null) {
                oneSwitch.setColorTemperature(switchColorTemp)
            } else {

                if (switchColorHue != null) {
                    oneSwitch.setHue(switchColorHue)
                }

                if (switchColorSaturation != null) {
                    oneSwitch.setSaturation(switchColorSaturation)
                }
            }
        } else {
            log.debug "no off/on setting was found for ${oneSwitch.displayName}."
        }
    }
}

def controlLocks() {
    selectedLocks.each { oneLock ->
        def lockVal = getSettingByPrefixAndId("lockOn-", oneLock.id)
        log.debug "${oneLock?.displayName}: ${getSettingByPrefixAndId('lockOn-', oneLock.id)}"
        log.debug "${oneLock?.displayName}: return from getSettingByPrefixAndId = ${lockVal}"

        if (lockVal == "lock") {
            oneLock.lock()
        } else if (lockVal == "unlock"){
            oneLock.unlock()
        } else {
            log.debug "no lock/unlock setting was found for ${oneLock.displayName}."
        }
    }
}

def triggerThrown(evt) {
    logEvent(evt)
    controlLocks()
    controlSwitches()

    if (triggerReset) {
        if (triggerEvent == "on") {
            triggerSwitch.off()
        } else {
            triggerSwitch.on()
        }
    }

    sendNotifications("${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss.SSSZ, EEE, MM-dd-yyyy',location.timeZone)}", recipients, inPhone, notifyPush)
}

def logEvent(evt) {
    log.debug "event from [${evt.displayName}]"
    log.debug "event.data [$evt.data]"
    log.debug "event.description [$evt.description]"
    log.debug "event.descriptionText [$evt.descriptionText]"
    log.debug "event.value [$evt.value]"
    log.debug "event.stringValue [$evt.stringValue]"
    log.debug "event.digital [$evt.digital]"
    log.debug "event.physical [$evt.physical]"
    log.debug "event.source [$evt.source]"
    try {
        log.debug "event.jsonValue [$evt.jsonValue]"
    } catch (ex) {
        log.debug "event.jsonValue [no valid json value]"
    }
}

def sendNotifications(message, recipContacts, recipPhones, sendPushToEveryone) {
	log.debug "sendNotifications('${message}, '${recipContacts}, '${recipPhones}, ${sendPushToEveryone})"
    if (location.contactBookEnabled && recipContacts) {
        log.debug "contact book enabled!"
        sendNotificationToContacts(message, recipients)
    } else {
        log.debug "contact book not enabled"
        if (recipPhones) {
            if (recipPhones instanceof java.util.List) {
                recipPhones.each {onePhone ->
                    sendSms(onePhone, message)
                    log.debug "sendSms('${onePhone}', '${message}')"
                }
            } else {
                sendSms(recipPhones, message)
                log.debug "sendSms('${recipPhones}', '${message}')"
            }
        }
    }
    
    if (sendPushToEveryone) {
    	sendPush(message)
        log.debug "sendPush('${message}')"
    }
}