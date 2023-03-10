/**
 *  Fibaro Universal Sensor App
 *
 *  Copyright 2014 Joel Tamkin
 *
 * 	2015-10-29: erocm123 - I removed the scheduled refreshes for my Philio PAN04 as it supports instant
 * 	status updates with my custom device type
 *  20016-01-25 PukkaHQ - Modified to work with Fibaro Universal Sensor
 *  2017-01-19 Further modified by others to work with temperature sensors from the FUS - removed compatibility with FUS contacts
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
        name: "Fibaro Univeral Sensor App",
        namespace: "",
        author: "Paul Crookes", // and others
        description: "Associates Fibaro Universal Sensor 4 Temperature Probes with four SmartThings simulated temperature sensors",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("FUS Module:") {
        input "fus", "capability.temperatureMeasurement", title: "Which FUS Module?", multiple: false, required: true
        input "temperature1", "capability.temperatureMeasurement", title: "First temp probe?", multiple: false, required: true
        input "temperature2", "capability.temperatureMeasurement", title: "Second temp probe?", multiple: false, required: true
        input "temperature3", "capability.temperatureMeasurement", title: "Third temp probe?", multiple: false, required: true
        input "temperature4", "capability.temperatureMeasurement", title: "Fourth temp probe?", multiple: false, required: true
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

/*def rsmHandler(evt) {
    def t1 = 0
    def t2 = 0
    def t3 = 0
    def t4 = 0

	log.debug "FUS RsMHandler"

    t1 = fus.currentValue("temperature1")
    t2 = fus.currentValue("temperature2")
    t3 = fus.currentValue("temperature3")
    t4 = fus.currentValue("temperature4")

    log.debug t1
    log.debug t2
    log.debug t3
    log.debug t4
        settings.temperature1.setTemperature(t1)
       settings.temperature2.setTemperature(t2)
       settings.temperature3.setTemperature(t3)
       settings.temperature4.setTemperature(t4)
}*/

def rsmHandler1(evt) {
    def t1 = 0
    log.debug "FUS RsMHandler1"
    t1 = fus.currentValue("temperature1")
    log.debug t1
    settings.temperature1.setTemperature(t1)
}

def rsmHandler2(evt) {
    def t2 = 0
    log.debug "FUS RsMHandler2"
    t2 = fus.currentValue("temperature2")
    log.debug t2
    settings.temperature2.setTemperature(t2)
}

def rsmHandler3(evt) {
    def t3 = 0
    log.debug "FUS RsMHandler3"
    t3 = fus.currentValue("temperature3")
    log.debug t3
    settings.temperature3.setTemperature(t3)
}

def rsmHandler4(evt) {
    def t4 = 0
    log.debug "FUS RsMHandler4"
    t4 = fus.currentValue("temperature4")
    log.debug t4
    settings.temperature4.setTemperature(t4)
}

def initialize() {
    subscribe(fus, "temperature1", rsmHandler1)
    subscribe(fus, "temperature2", rsmHandler2)
    subscribe(fus, "temperature3", rsmHandler3)
    subscribe(fus, "temperature4", rsmHandler4)
    /*These are for testing if events not triggering
    runEvery5Minutes(testhandler("temperature1"))
    schedule("23 20/2 * * * ?", testhandler ("temperature4"))
    unschedule()*/
}