/**
 *  AverageThings Temperature SmartApp for SmartThings
 *
 *  Copyright (c) 2014 Brandon Gordon (https://github.com/notoriousbdg)
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
 *  Overview
 *  ----------------
 *  These SmartApps have two primary uses. The first is to create separate temperature and humidity tiles for multi-sensors. 
 *  This will allow an Aeon Lab Multi-Sensor to show the motion sensor on its tile while having its temperature and humidity 
 *  mirrored to separate tiles.
 *  
 *  The second use case is to calculate the average temperature and humidity for multiple sensors. The average temperature 
 *  tile could be used as an external temperature sensor for a thermostat SmartApp or it can be used to calculate the average 
 *  of multiple outside temperature sensors.
 *  
 *  These SmartApps rely on the Virtual Temperature Tile and Virtual Humidity Tile available at 
 *  https://github.com/statusbits/smartthings/tree/master/VirtualThings.
 *
 *  Install Steps
 *  ----------------
 *  1. Create new SmartApps at https://graph.api.smartthings.com/ide/apps using the SmartApps at https://github.com/notoriousbdg/SmartThings.AverageThings.
 *  2. Create a new virtual temperature tile or virtual humidity tile device based on https://github.com/statusbits/smartthings/tree/master/VirtualThings
 *  3. Install the newly created SmartApp in the SmartThings mobile application.
 *  4. For target virtual tile prompt, select the virtual tile created in step 2.
 *  5. Select one or more source sensors.
 *  6. Assign a name.
 *  7. Tap done.
 *
 *  Revision History
 *  ----------------
 *  2014-10-13  v0.0.1  Initial release
 *
 *  The latest version of this file can be found at:
 *    https://github.com/notoriousbdg/SmartThings.AverageThings
 *
 */

definition(
    name: "AverageThings Temperature",
    namespace: "notoriousbdg",
    author: "Brandon Gordon",
    description: "SmartApp to average the temperature from one or more temperature sensors to a Virtual Temperature Tile.  If a single source sensor is specified, then the virtual tile will mirror the source sensor, which can be useful to create a dedicated tile for a multi-sensor.  A Virtual Temperature Tile must be created from https://github.com/statusbits/smartthings/blob/master/VirtualThings/VirtualTemperatureTile.device.groovy device type to serve as the target Virtual Temperature Tile.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Choose a target virtual temperature tile... "){
        input "target", "capability.temperatureMeasurement", title: "Tile"
    }
    section("Choose a source temperature sensors... "){
        input "sensors", "capability.temperatureMeasurement", title: "Sensors", multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(sensors, "temperature", temperatureHandler)
}

def temperatureHandler(evt) {
    def sum     = 0
    def count   = 0
    def average = 0

    for (sensor in settings.sensors) {
        count += 1
        sum   += sensor.currentTemperature
    }

    average = sum/count
    log.debug "average: $average"

    settings.target.parse("temperature: ${average}")
}