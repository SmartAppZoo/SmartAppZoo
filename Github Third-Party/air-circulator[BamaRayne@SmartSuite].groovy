/**
 *  Air Circulator
 *
 *  Copyright 2018 Anthony Santilli
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

import groovy.transform.Field

definition(
    name: "Air Circulator",
    namespace: "Echo",
    author: "Anthony Santilli",
    description: "Helps manage the Room Comfort using Fan/Switch and different Actuator/Sensor Triggers...",
    category: "Convenience",
    iconUrl: appImg(),
    iconX2Url: appImg(),
    iconX3Url: appImg())

preferences {
    page(name: "mainPage")
}

def textVersion()	{ return "Version: ${appVer()}" }
def textModified()	{ return "Updated: ${appDate()}" }
def appVer() { return "1.0.2" }
def appDate() { return "06/4/2018" }

def mainPage() {
    return dynamicPage(name: "mainPage", install: true, uninstall: true) {
        appInfoSect()
        def fanApps = getChildApps()
        if(!fanApps) { 
            section("") { paragraph "You haven't created any Automations yet!\nTap Create New Automation to get Started" } 
        }
        section("") {
            app(name: "fanApps", appName: "Air Circulator Child", namespace: "tonesto7", multiple: true, title: "Create New Fan Automation")
        }
        getChildStatusSections()
    }
}

def getChildStatusSections() {
    getChildApps()?.each { capp->
        section("${capp?.label}") {
            paragraph title: "Fan Status:", capp?.getFanStatusDesc(), state: "complete"
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state?.isInstalled = true
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def appImg() { return "https://d1nhio0ox7pgb.cloudfront.net/_img/g_collection_png/standard/256x256/fan.png" }
def appInfoSect()	{
    def str = ""
    str += "${app?.name}"
    str += "\n• Version: ${appVer()}"
    str += "\n• Updated: ${appDate()}"
    section() {
        paragraph "${str}", image: appImg()	
    }
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
}