/**
*  TriggerSwitchTimed
*
*  Copyright 2017 Seth Munroe
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
    name: "TriggerSwitchTimed",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "Set this to wait for a specific number of triggers in the configured time period. When the number of triggers is met, it will either turn a switch off or on for a configurable amount of time (or set it to leave the switch off or on).",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Trigger Switch Settings") {
        input(name: "triggerSwitch", type: "capability.contactSensor", title: "The trigger sensor will cause all of the actions to start. Select a trigger switch:", multiple: false, required: true)
        input(name: "triggerTime", type: "number", title: "How many seconds can the trigger be open before action is taken?", range: "0..*", defaultValue: "15", required: true)
        input(name: "triggerNumber", type: "number", title: "How many triggers should be allowed before triggering?", range: "0..*", defaultValue: "0", required: true)
        input(name: "triggerSeconds", type: "number", title: "How many seconds back should triggers be counted? This will be ignored if the number of allowed triggers is zero.", range: "0..*", defaultValue: "0", required: true)
    }

    section("Device to be swithed when triggered.") {
        input(name: "switchedDevice", type: "capability.switch", title: "Select the device to be controled:", required: true, multiple: false)
        input(name: "switchedState", type: "enum", title: "What state should the device be switched to?", options: ["on","off"], defaultValue: "on", required: true);
        input(name: "switchedMinTime", type: "number", title: "Minimum seconds that the device should stay in the switched state? Set to zero to have device stay in the state.", range: "0..*", defaultValue: "0", required: true)
        input(name: "switchedMaxTime", type: "number", title: "Maximum seconds that the device should stay in the switched state? Set to zero to have device stay in the state.", range: "0..*", defaultValue: "0", required: true)
    }

    section("Notifications") {
        paragraph("Notifications will be sent about the trigger and the switched device.", title: "SMS/Push Notifications:", required: true)
        input(name: "recipients", type: "contact", title: "Send notifications to:", multiple: true) {
            input(name: "inPhone", type: "phone", title: "Send SMS notifications to: (optional: comma separated list of phone numbers)",
                  description: "Phone Number", required: false, multiple: true)
        }

        input(name: "notifyPush", type: "bool", title: "Send notifications to all logged in devices for all users of location: ${location.name}?", defaultValue: false)
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
    state.triggers = []
    subscribe(triggerSwitch, "contact.open", triggered)
    subscribe(triggerSwitch, "contact.closed", untriggered)
    subscribe(switchedDevice, "switch", switchChanged)
}

def untriggered(evt) {
    log.debug "untriggered"
    unschedule(triggeredTooLong)
}

def triggeredTooLong() {
    if (changeState(switchedDevice, switchedState) && switchedMinTime > 0) {
        def rand = new Random()
        runIn((switchedMinTime + rand.nextInt(switchedMaxTime - switchedMinTime)), timeEnded)
    }
}

def triggered(evt) {
    def rand = new Random()

    log.debug "triggers before: ${state.triggers}"
    cleanupOldTriggers()
    state.triggers.add evt.date.time

    if (triggerTime > 0) {
        runIn(triggerTime, triggeredTooLong)
    }

    log.debug "triggers: ${state.triggers.size()}"
    if (state.triggers.size() > triggerNumber) {
        log.debug "switch device: ${switchedDevice}.${switchedState}"

        if (changeState(switchedDevice, switchedState) && switchedMinTime > 0) {
            runIn((switchedMinTime + rand.nextInt(switchedMaxTime - switchedMinTime)), timeEnded)
        }
    }

    sendNotifications("${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy',location.timeZone)}", recipients, inPhone, notifyPush)

    log.debug "triggers after: ${state.triggers}"
}

def switchChanged(evt) {
	log.debug "Device: ${evt.displayName} stringValue: ${evt.stringValue}"
    if (switchedState != evt.stringValue) {
        unschedule(timeEnded)
    }
    sendNotifications("${evt.displayName} ${evt.stringValue.toUpperCase()} at ${location.name}: ${evt.date.format('HH:mm:ss.SSS Z, EEE, MM-dd-yyyy',location.timeZone)}", recipients, inPhone, notifyPush)
}

def cleanupOldTriggers() {
    if (triggerNumber > 0 && triggerSeconds > 0) {
        def oldestTrigger = (now() - (triggerSeconds * 1000))
        state.triggers.removeAll {it < oldestTrigger}
    } else {
        state.triggers.clear()
    }
}

def changeState(device, state) {
    return changeState(device, state, false)
}

def changeState(device, state, opposite) {
    if (device?.supportedCommands.find {it.name == "refresh"} != null) {
        log.debug "refresh: ${device.displayName}"
        device.refresh()
    }

    def startingSwitchState = device?.currentSwitch

    log.debug "changeState: Device: ${device}; State: ${state}, Opposite: ${opposite}"
    if (opposite) {
        if (state == "on") {
            device.off()
        } else {
            device.on()
        }
    } else {
        if (state == "on") {
            device.on()
        } else {
            device.off()
        }
    }

    if (device?.supportedCommands.find {it.name == "refresh"} != null) {
        log.debug "refresh: ${device.displayName}"
        device.refresh()
    }

    def switchChanged = ((opposite && startingSwitchState == state) || (!opposite && startingSwitchState != state))

    log.debug "startingSwitchState: ${startingSwitchState}, switchChanged: ${switchChanged}"
    return switchChanged
}

def timeEnded() {
    changeState(switchedDevice, switchedState, true)
}

def sendNotifications(message, recipContacts, recipPhones, sendPushToEveryone) {
    log.debug "sendNotifications('${message}', '${recipContacts}', '${recipPhones}', ${sendPushToEveryone})"
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