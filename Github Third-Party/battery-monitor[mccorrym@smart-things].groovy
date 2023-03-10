/**
 *  Battery Monitor
 *
 *  Copyright 2020 Matt
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
    name: "Battery Monitor",
    namespace: "smartthings",
    author: "Matt",
    description: "Monitor the devices on the network requiring batteries and send a notification if one or more batteries are low.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/power_allowance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/power_allowance@2x.png"
)

preferences {
    section("Choose the sensors that use batteries and select a time to check them.") {
        input "sensors", "capability.sensor", required: true, multiple: true, title: "Which sensors require batteries?"
        input "battery_check_time", "time", required: true, title: "Choose a time to check the batteries on the network each day"
    }
}

def installed() {
    schedule(battery_check_time, batteryCheckHandler)
}

def updated() {
	unsubscribe()
    unschedule(batteryCheckHandler)
    schedule(battery_check_time, batteryCheckHandler)
}

def batteryCheckHandler() {
    sensors.each { sensor ->
        def battery_value = sensor.currentValue("battery").toInteger()
        if (battery_value <= 20) {
            sendNotificationEvent("[EVENT] LOW BATTERY: The batteries for the ${sensor.getLabel().toLowerCase()} are low (${sensor.currentValue("battery")}%). Consider replacing them soon.")
            sendPush("The batteries for the ${sensor.getLabel().toLowerCase()} are low (${sensor.currentValue("battery")}%). Consider replacing them soon.")
        }
    }
}