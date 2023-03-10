/**
 *  HEM Daily Reset
 *
 *  Copyright 2018
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
 *
 */
definition(
    name: "HEM Daily Reset",
    namespace: "jasonrwise77",
    author: "jasonrwise77",
    description: "Resets the Energy Monitor Daily on a specified time",
    parent: "jasonrwise77:HEM Reset Manager",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Monthly.png",
    iconX2Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Monthly.png",
    iconX3Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Monthly.png"
    )

preferences {
    section("Energy Meters To Reset") {
        input(name: "meters", type: "capability.energyMeter", title: "Select Energy Meters", description: null, multiple: true, required: true, submitOnChange: true)
    }
    section("Time of The Day To Reset") {
        input "time", "time", title: "Select A Time Of Day"
    }
}
def installed() {
    log.debug "Daily Energy Meter Reset Manager SmartApp installed, now preparing to schedule the first reset."
}
def updated() {
    log.debug "Daily Energy Meter Reset Manager SmartApp updated, so update the user defined schedule and schedule another check for the next day."
    unschedule()
    initialize()
}
def initialize() {
    unschedule()
    schedule(time, resetEnergyUsage)
}
def resetEnergyUsage() {
    log.debug "Daily Energy Meter reset schedule triggered..."
    log.debug "...resetting the energy meter because it's when the user requested it."
    meters?.each { meter->
        log.debug "Reset Energy on (${meter?.getLabel()})"
        meter?.resetEnergyUsage()
    }
    log.debug "Process completed, now schedule the reset to check on the next day."
    initialize()
}
