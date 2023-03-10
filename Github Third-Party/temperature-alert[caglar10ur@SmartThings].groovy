/**
 *  Temperature Alert
 *
 *  Copyright 2016 S.Çağlar Onur
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
    name: "Temperature Alert",
    namespace: "caglar10ur",
    author: "S.Çağlar Onur",
    description: "Watch temperature sensors to warn of excessive heat or cold",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select Things to Monitor:") {
        input "thermostats", "capability.temperatureMeasurement", title: "Things With Thermostat Sensor", multiple: true, required: true
    }

    section("Set Temperature Alerts:") {
        input "maxThreshold", "number", title: "if above... (default 90°)", defaultValue: 90, required: true
            input "minThreshold", "number", title: "if below... (default 60°)", defaultValue: 60, required: true
    }

    section("Via text message at this number (or via push notification if not specified") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone number (optional)", required: false
        }
    }
}

def installed() {
    log.trace "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.trace "Updated with settings: ${settings}"

    unsubscribe()
    // unschedule all tasks
    unschedule()

    initialize()
}

def initialize() {
    // schedule the task
    runEvery1Hour(checkTemprature)
    // run once
    checkTemprature()
}

def checkTemprature() {
    log.trace "checkTemprature()"

    for (thermostat in thermostats) {
        def temperature = thermostat.currentValue("temperature")
        if (temperature == null || temperature == "") {
            log.error "Skipping ${thermostat.label} as reading currentValue failed"
            continue
        }
        log.trace "Checking ${thermostat.label}: ${temperature}°"

        if (settings.maxThreshold.toInteger() != null && temperature > settings.maxThreshold.toInteger()) {
            notify("${thermostat.label} [${temperature}°] is ABOVE the alert level of ${settings.maxThreshold.toInteger()}°")
        } else if (settings.minThreshold.toInteger() != null && temperature < settings.minThreshold.toInteger()) {
            notify("${thermostat.label} [${temperature}°] is BELOW the alert level of ${settings.minThreshold.toInteger()}°")
        } else {
            log.debug "${thermostat.label} [${temperature}°] is in the acceptable range"
        }
    }
}

private notify(msg) {
    log.trace "sendMessage(${msg})"

    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    } else {
        if (phone) {
            sendSms phone, msg
        } else {
            sendPush msg
        }
    }
}
