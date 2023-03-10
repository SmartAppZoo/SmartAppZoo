/**
 *  MAC Address Virtual Presence Sensor
 *
 *  Copyright 2018 Austen Dicken
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
 *  Based on the original work by Patrick Stuart (pstuart)
 *
 */
definition(
    name: "MAC Address Virtual Presence Sensor",
    namespace: "cvpcs",
    author: "Austen Dicken",
    description: "Child MAC Address Virtual Presence Sensor SmartApp",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi.png",
    iconX2Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/mac-address-virtual-presence-sensor-webapi.src/WiFi@2x.png")


preferences {
    page(name: "mainPage", title: "Install Sensor", install: true, uninstall:true) {
        section("Sensor Settings") {
            label(name: "label", title: "Name", required: true)
            input("macAddress","text", title: "MAC Address", description: "Enter the MAC address of the device", required: true, displayDuringSetup: true)
        }
    }
    
}

def installed() {
    log.debug "Installed"

    initialize()
}

def updated() {
    log.debug "Updated"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "MAC Address is ${macAddress}"

    try {
        def DNI = (Math.abs(new Random().nextInt()) % 99999 + 1).toString()
        def sensors = getChildDevices()
        if (sensors) {
            removeChildDevices(getChildDevices())
        }
        def childDevice = addChildDevice("cvpcs", "Virtual Presence Sensor", DNI, null, [name: app.label, label: app.label, completedSetup: true])
    } catch (e) {
    	log.error "Error creating device: ${e}"
    }
}

def setPresence(status) {
	getChildDevices().each {device -> device.setPresence(status)}
}

private removeChildDevices(delete) {
    delete.each {
    	log.debug "deleting device ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
}