/**
 *  Zone Builder
 *
 *  Copyright 2021 Michael Pierce
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

String getName() { return "Zone Builder" }
String getVersionNum() { return "9.0.0" }
String getVersionLabel() { return "${getName()}, version ${getVersionNum()}" }

definition(
    name: "${getName()}",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Creates Zone apps and devices to manage the occupancy status of each zone in your home based on the devices contained within it.",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    importUrl: "https://raw.githubusercontent.com/mikee385/hubitat-mikee385/master/zones/zone-builder.groovy")

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "${getVersionLabel()}", install: true, uninstall: true) {
        if (app.getInstallationState() != "COMPLETE") {
		    section {
		        paragraph "Please click 'Done' to finish installation."
		    }
  	    } else {
            section {
                app(name: "childApps", appName: "Zone App", namespace: "mikee385", title: "New Zone...", multiple: true)
            }
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    createParentDevice()
}

def uninstalled() {
    deleteAllZones()
}

def logDebug(msg) {
    if (logEnable) {
        log.debug msg
    }
}

def getParentId() {
    return "zone:" + app.getId()
}

def createParentDevice() {
    def parentId = getParentId()
    def parent = getChildDevice(parentId)
    if (!parent) {
        parent = addChildDevice("mikee385", "Zone Parent", parentId, [label:"Zones", isComponent:true, name:"Zone Parent"])
    }
}

def addZoneDevice(appId, name) {
    def parentId = getParentId()
    def parent = getChildDevice(parentId)
    if (parent) {
        parent.addZoneDevice(appId, name)
    } else {
        log.error "No Parent Device Found."
    }
}

def getZoneDevice(appId) {
    def parentId = getParentId()
    def parent = getChildDevice(parentId)
    if (parent) {
        return parent.getZoneDevice(appId)
    } else {
        log.error "No Parent Device Found."
    }
}

def deleteZoneDevice(appId) {
    def parentId = getParentId()
    def parent = getChildDevice(parentId)
    if (parent) {
        parent.deleteZoneDevice(appId)
    } else {
        log.error "No Parent Device Found."
    }
}

def deleteAllZones() {
    def parentId = getParentId()
    def parent = getChildDevice(parentId)
    if (parent) {
        parent.deleteAllZones()
        deleteChildDevice(parentId)
    } else {
        log.error "No Parent Device Found."
    }
}