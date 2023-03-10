/**
 *  Generic Video Camera Child
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
    name: "ReoLink RLN16-410 NVR Camera",
    namespace: "cvpcs",
    author: "Austen Dicken",
    description: "Child Video Camera SmartApp",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/reolink-rln16-410-nvr.src/Reolink.png",
    iconX2Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/reolink-rln16-410-nvr.src/Reolink@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/cvpcs/smartthings/master/smartapps/cvpcs/reolink-rln16-410-nvr.src/Reolink@2x.png")


preferences {
    page(name: "mainPage", title: "Install Video Camera", install: true, uninstall:true) {
        section("Camera Settings") {
            label(name: "label", title: "Name", required: true)
            input("camera","text", title: "Camera", description: "Enter the path for the camera you wish to capture from the NVR", required: true, displayDuringSetup: true)
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
    state.uri = "rtsp://${parent.user}:${parent.pass}@${parent.inhost}:${parent.port}/${camera}"
    log.debug "uri is ${state.uri}"

    try {
        def DNI = (Math.abs(new Random().nextInt()) % 99999 + 1).toString()
        def cameras = getChildDevices()
        if (cameras) {
            removeChildDevices(getChildDevices())
        }
        def childDevice = addChildDevice("cvpcs", "ReoLink RLN16-410 NVR Camera", DNI, null, [name: app.label, label: app.label, completedSetup: true])
    } catch (e) {
    	log.error "Error creating device: ${e}"
    }
}

private removeChildDevices(delete) {
    delete.each {
    	log.debug "deleting device ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
}
