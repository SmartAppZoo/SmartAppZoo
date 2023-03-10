/**
 *  Add Stateless Virtual Switch
 *
 *  Copyright 2017 Michael McGarry
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
    name: "Add Stateless Virtual Switch",
    namespace: "mcgarryplace-michael",
    author: "Michael McGarry",
    description: "Adds a Stateless Virtual Switch",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "inputPage", title: "Adds a Stateless Virtual Switch", install: false, uninstall: false) {
        section("Stateless Virtual Switch") {
            input "svsName", "text", title: "Switch Name", required: true
            input "networkID", "number", title: "Network ID", required: true
            href "pageAddSwitch", title: "Add Switch", description: "Tap to add this switch", image: addBtn()
        }
    }
    
    page name: "pageAddSwitch"
}

def pageAddSwitch() {
    dynamicPage(name: "pageAddSwitch", title: "Add Stateless Virtual Switch Results", install: false, uninstall: false) {
        def repsonse = addStatelessVirtualSwitch()
        section {paragraph repsonse}
    }
}

def addStatelessVirtualSwitch(){
    def deviceID = newDeviceID()
    def nameSpace = "mcgarryplace-michael"
    def result
    
    try {
        def childDevice = addChildDevice(nameSpace, svsDeviceName(), deviceID, null, [name: deviceID, label: svsName, completedSetup: true])
        log.debug "Created Switch ${svsName}: ${deviceID}"
        result ="The Stateless Virtual Switch named '${svsName}' has been created."
    } 
    catch (e) {
        log.debug "Error creating switch: ${e}"
        result = "Houston, we have a problem"
    }
    
    return result   
}

def addBtn() { 
    return "https://raw.githubusercontent.com/mcgarryplace-michael/SmartThingsPublic/master/img/add-btn.png" 
}

def newDeviceID() {
    def deviceID = String.format("SVS_%02d", networkID)
    log.debug "New Device ID: ${deviceID}"
    deviceID
}

def svsDeviceName() {
    return "Stateless Virtual Switch"
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
}

