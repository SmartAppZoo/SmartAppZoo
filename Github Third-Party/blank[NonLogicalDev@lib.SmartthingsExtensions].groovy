/**
 *  BLANK TEMPLATE
 *
 *  Copyright 2018 Oleg Utkin
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
 *  Loosely modelled after:
 *    https://github.com/codersaur/SmartThings/blob/master/smartapps/influxdb-logger/influxdb-logger.groovy
 */

definition(
    name: "Blank",
    namespace: "nonlogical",
    author: "Oleg Utkin",
    description: "Does not do anything.",
    category: "SmartThings Labs",

    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX4Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)

//------------------------------------------------------------
// Pages
//------------------------------------------------------------

preferences {
    page(name:"pageMain")
}

def pageMain() {
    return dynamicPage(name:"pageMain", title:"", install:true, uninstall:true) {
        section("Devices:") {
            input "devices", "capability.sensor", title: "Devices:", multiple: true
        }
    }
}

//------------------------------------------------------------
// Lifecycle
//------------------------------------------------------------

def installed() {
    log.debug "INSTALLING..."
}

def updated() {
    log.debug "UPDATING..."
}

def uninstalled() {
    log.debug "UNINSTALLING..."
}