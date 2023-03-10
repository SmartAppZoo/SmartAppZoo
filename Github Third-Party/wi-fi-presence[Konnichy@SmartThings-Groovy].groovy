/**
 *  Wi-Fi Presence
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

import groovy.json.JsonSlurper

definition(
    name: "Wi-Fi Presence",
    namespace: "konnichy",
    author: "Konnichy",
    description: "Allows the SmartThings hub to receive presence events sent by the local Wi-Fi access point to update the presence state of household members.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn@2x.png"
)

def settings() {
    dynamicPage(name: "settings") {
        // Display all configured presence sensors + 1 empty
        for (int sensor_nb=1 ; ; sensor_nb++) {
            section("Sensor $sensor_nb") {
                input(name: "sensor" + sensor_nb, type: "capability.presenceSensor", title: "Which virtual presence sensor must be updated?", required: false, submitOnChange: true)
                // For each configured presence sensor, display all associated MAC addresses + 1 empty
                if (settings."sensor${sensor_nb}") {
                    for (int macaddress_nb=1 ; ; macaddress_nb++) {
                        input(name: "sensor" + sensor_nb + "_macaddress" + macaddress_nb, type: "text", title: "What MAC address do you want to monitor?", description: "00:00:00:00:00:00", required: false, submitOnChange: true)
                        // Don't go beyond the empty MAC address
                        if (!settings."sensor${sensor_nb}_macaddress${macaddress_nb}")
                            break
                    }
                }
            }
            // Don't go beyond the empty sensor
            if (!settings."sensor${sensor_nb}")
                break
        }
    }
}

preferences {
    page(name: "settings", install: true, uninstall: true)
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
    // Source: https://community.smartthings.com/t/poll-or-subscribe-example-to-network-events/72862/15
    subscribe(location, null, handleLANEvent, [filterEvents:false])
}

def findPresenceSensor(mac_address) {
    for (int sensor_nb=1 ; settings."sensor${sensor_nb}" ; sensor_nb++) {
        for (int macaddress_nb=1 ; settings."sensor${sensor_nb}_macaddress${macaddress_nb}" ; macaddress_nb++) {
            if (settings."sensor${sensor_nb}_macaddress${macaddress_nb}".equalsIgnoreCase(mac_address)) {
                // found it!
                return settings."sensor${sensor_nb}"
            }
        }
    }

    // not found
    return null
}

def handleLANEvent(event)
{
    def message = parseLanMessage(event.value)
    
    // Get the HTTP path used on the first header line
    if (!message.header)
        return
    def path = message.header.split("\n")[0].split()[1]
    
    // Only handle the event if specifically directed to this application
    if (path == "/presence") {
        // Source: https://community.smartthings.com/t/poll-or-subscribe-example-to-network-events/72862/15
        def slurper = new JsonSlurper();
        def json = slurper.parseText(message.body)
        switch (json.event) {
            case "AP-STA-CONNECTED":
                // Search which presence sensor is associated with the connected MAC address
                def sensor = findPresenceSensor(json.mac_address)
                if (sensor) {
                    // Update the presence sensor
                    log.info "${sensor.name} arrived"
                    sensor.arrived()
                }
                break
            case "AP-STA-DISCONNECTED":
                // Search which presence sensor is associated with the disconnected MAC address
                def sensor = findPresenceSensor(json.mac_address)
                if (sensor) {
                    // Update the presence sensor
                    log.info "${sensor.name} departed"
                    sensor.departed()
                }
                break
        }
    }
}
