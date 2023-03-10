/**
 *  Fan Manager
 *
 *  Copyright 2018 Anthony Santilli and Jason
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
public String appVer() { return "1.1.0" }
public String appDate() { return "09/27/2018" }

definition(
    name: "Fan Manager",
    namespace: "tonesto7",
    author: "Anthony Santilli",
    description: "Helps manage Room Comfort using Fans/Switches and different Actuator/Sensor Triggers... Also Controls and Regulates Whole House Fans",
    category: "Convenience",
    iconUrl: appImg(),
    iconX2Url: appImg(),
    iconX3Url: appImg())

preferences {
    page(name: "mainPage")
    page(name: "manageFanAutoPage")
    page(name: "manageFanWholePage")
    page(name: "fanAutoStatusPage")
    page(name: "fanWholeStatusPage")
    page(name: "uninstallPage")
}

def mainPage() {
    return dynamicPage(name: "mainPage", install: true, uninstall: true) {
        appInfoSect()
        def fanApps = getChildApps()?.findAll { it?.name == "Fan Manager Child" }
        def wholeHouseApp = getChildApps()?.findAll { it?.name == "Fan Manager Whole House" }
        section("Fan Automations:") {
            if(fanApps?.size()) { 
                href "manageFanAutoPage", title: "Manage Fan Automations\n(${fanApps?.size()} Configured)", description: "Tap to view", state: "complete", image: getAppImg("fan_manager_orange.png")
            } else {
                paragraph "You haven't created any Fan Automations yet!\nTap Create New Automation to get started"
                app(name: "fanApps", appName: "Fan Manager Child", namespace: "tonesto7", multiple: true, title: "Create New Fan Automation", image: getAppImg("fan_manager_orange.png"))
            }
        }
        
        section("Whole House Fan:") {
            if(!wholeHouseApp) {
                app(name: "wholeHouseApp", appName: "Fan Manager Whole House", namespace: "tonesto7", multiple: true, title: "Control Whole House Fan", image: getAppImg("fan_manager_whole.png"))
            } else {
                href "manageFanWholePage", title: "Manage Whole House\n(${wholeHouseApp?.size()} Configured)", description: "Tap to view", state: "complete", image: getAppImg("fan_manager_whole.png")
            }
        }
        if(fanApps || wholeHouseApp) {
            section("Status Info:") {
                if(fanApps) {
                    href "fanAutoStatusPage", title: "Fan Automation Status", description: "Tap to view", state: "complete", image: getAppImg("info.png")
                }
                if(wholeHouseApp) {
                    href "fanWholeStatusPage", title: "Whole House Status", description: "Tap to view", state: "complete", image: getAppImg("info.png")
                }
            }
        }
        section(" ") {
            href "uninstallPage", title: "Remove App & All Reminders", description: "", image: getAppImg("uninstall.png")
        }
    }
}

def uninstallPage() {
	dynamicPage(name: "uninstallPage", title: "Uninstall", install: false, uninstall: true) {
		section() {
			paragraph title: "NOTICE", "This will completely uninstall this App and All Child Apps.", required: true, state: null
		}
		remove("Remove this App and All (${getChildApps()?.size()}) Automations!", "WARNING!!!", "Last Chance to Stop!\nThis action is not reversible\n\nThis App and All Fan Automations will be removed...")
	}
}

def manageFanAutoPage() {
    return dynamicPage(name: "manageFanAutoPage", install: false, uninstall: false) {
        def fanApps = getChildApps()?.findAll { it?.name == "Fan Manager Child" }
        section("") {
            app(name: "fanApps", appName: "Fan Manager Child", namespace: "tonesto7", multiple: true, title: "Create New Fan Automation", image: getAppImg("fan_manager_orange.png"))
        }
    }
}

def manageFanWholePage() {
    return dynamicPage(name: "manageFanWholePage", install: false, uninstall: false) {
        def fanApps = getChildApps()?.findAll { it?.name == "Fan Manager Whole House" }
        section("") {
            app(name: "wholeHouseApp", appName: "Fan Manager Whole House", namespace: "tonesto7", multiple: true, title: "Control Whole House Fan", image: getAppImg("fan_manager_whole.png"))
        }
    }
}

def fanAutoStatusPage() {
    return dynamicPage(name: "fanAutoStatusPage", install: false, uninstall: false) {
        getChildApps()?.each { capp->
            if(capp?.name == "Fan Manager Child") {
                section("${capp?.label}") {
                    paragraph capp?.getFanStatusDesc(), state: "complete"
                }
            }
        }
    }
}

def fanWholeStatusPage() {
    return dynamicPage(name: "fanWholeStatusPage", install: false, uninstall: false) {
        getChildApps()?.each { capp->
            if(capp?.name == "Fan Manager Whole House") {
                section("${capp?.label}") {
                    paragraph capp?.getFanStatusDesc(), state: "complete"
                }
            }
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

String appImg() { return getAppImg("fan_manager_blue.png") }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/tonesto7/smartthings-tonesto7-public/master/resources/icons/$imgName" }

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
    if(app?.getLabel() != "Fan Manager") { app?.updateLabel("Fan Manager") }
}
