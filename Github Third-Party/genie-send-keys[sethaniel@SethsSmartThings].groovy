/**
*  Genie Send Keys
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
    name: "Genie Send Keys",
    namespace: "sethaniel",
    author: "Seth Munroe",
    description: "This app will send a sequence of keys to the DirecTV Genie receiver.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "pageTrigger", nextPage: "pageReceivers", install: false, uninstall: true)
    page(name: "pageReceivers", nextPage: "pageKeys", install: false, uninstall: true)
    page(name: "pageKeys", install: true, uninstall: true)
}

def pageTrigger() {
    dynamicPage(name: "pageTrigger", title: "Trigger and App Settings") {
        section("Trigger Switch Settings") {
            input(name: "triggerSwitch", type: "capability.switch", title: "The trigger switch will cause all of the actions to start. Select a trigger switch:", multiple: false, required: true)
            input(name: "triggerEvent", type: "enum", title: "Which state of this switch should trigger the actions?", options: ["on", "off"], required: true)
            input(name: "triggerReset", type: "bool", title: "Should the event be switched back automatically so that the trigger can be used again?", required: true) 
        }

        section("Number of Genie Receivers") {
            input(name: "numReceivers", type: "number", title: "How many receivers should be controlled?", range: "1..25", required: true)
        }

        section("Number of keypresses") {
            input(name: "numKeys", type: "number", title: "How many keypresses do you want to send?", range: "1..25", required: true)
        }

        section("Application Information", mobileOnly:true) {
            icon(title: "Pick an Icon for the app.", reuired: false)
            label(title: "Assign a name", required: false)
            mode(title: "Set for specific mode(s)", required: false)
        }
    }
}

def pageReceivers() {
    dynamicPage(name: "pageReceivers", title: "Enter Receiver Information") {
        section("Receivers") {
            for (def receiverCount = 1; receiverCount <= numReceivers; receiverCount++) {
                input(name: "masterReceiverIp-${String.format('%02d', receiverCount)}", type: "string", title: "What's the IP address of Master Genie number ${String.format('%02d', receiverCount)}?", defaultValue: getSettingByPrefixAndSuffix("masterReceiverIp-", "${String.format('%02d', receiverCount)}"), required: true)
                input(name: "masterReceiverPort-${String.format('%02d', receiverCount)}", type: "string", title: "What port is Master Genie number ${String.format('%02d', receiverCount)} using for control? (8080 is typical)", defaultValue: getSettingByPrefixAndSuffix("masterReceiverPort-", "${String.format('%02d', receiverCount)}"), required: true)
                input(name: "clientReceiverMac-${String.format('%02d', receiverCount)}", type: "string", title: "What's the MAC address of target Genie number ${String.format('%02d', receiverCount)}? (use 0 to send to master)", defaultValue: getSettingByPrefixAndSuffix("clientReceiverMac-", "${String.format('%02d', receiverCount)}"), required: true)
            }
        }
    }
}

def pageKeys() {
    dynamicPage(name: "pageKeys", title: "Select Keys") {
        section("Keys to send") {
            for (def keyCount = 1; keyCount <= numKeys; keyCount++) {
                input(name: "keys-${String.format('%02d', keyCount)}", type: "enum", options: getKeyList(), title: "Select key number ${String.format('%02d', keyCount)}", defaultValue: getSettingByPrefixAndSuffix("keys-", "${String.format('%02d', keyCount)}"), multiple: false, required: true)
            }
        }
    }
}

private getKeyList() {
    return [
        "0",
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "active",
        "advance",
        "back",
        "blue",
        "chandown",
        "chanup",
        "dash",
        "down",
        "enter",
        "exit",
        "ffwd",
        "format",
        "green",
        "guide",
        "info",
        "left",
        "list",
        "menu",
        "pause",
        "play",
        "power",
        "poweroff",
        "poweron",
        "prev",
        "record",
        "red",
        "replay",
        "rew",
        "rew",
        "right",
        "select",
        "stop",
        "up",
        "yellow"
    ]
}

/*
This will return the setting for the specified device
*/
def getSettingByPrefixAndSuffix(settingNamePrefix, settingNameSuffix) {
    def retSetting = settings.findAll {it.key == settingNamePrefix + settingNameSuffix}
    def retValue = null
    retSetting.each {thisOne ->
        retValue = thisOne.value
    }

    return retValue
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

def sendKey(key, masterIp, masterPort, clientMac) {
	log.debug "sending command: Key: ${key}, Sent To: ${masterIp}:${masterPort}:${(clientMac ?: '0').replace(':', '').toUpperCase()}"
    sendHubCommand(
        new physicalgraph.device.HubAction(
            headers: [
                HOST: "${masterIp}:${masterPort}"
            ],
            method : "GET",
            path   : "/remote/processKey",
            query  : [
                key: "${key}",
                hold: "keyPress",
                clientAddr: "${(clientMac ?: '0').replace(':', '').toUpperCase()}"
            ]
        )
    )
}

def triggerThrown(evt) {

    for (def receiverCount = 1; receiverCount <= numReceivers; receiverCount++) {
        for (def keyCount = 1; keyCount <= numKeys; keyCount++) {
            sendKey(getSettingByPrefixAndSuffix("keys-",String.format('%02d', keyCount)), getSettingByPrefixAndSuffix("masterReceiverIp-", "${String.format('%02d', receiverCount)}"), getSettingByPrefixAndSuffix("masterReceiverPort-", "${String.format('%02d', receiverCount)}"), getSettingByPrefixAndSuffix("clientReceiverMac-", "${String.format('%02d', receiverCount)}"))
        }
    }

    if (triggerReset) {
        if (triggerEvent == "on") {
            triggerSwitch.off()
        } else {
            triggerSwitch.on()
        }
    }
}