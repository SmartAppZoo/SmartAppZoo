/**
 *  LIFX Sync
 *  Version 1.0.0 - 10/21/16
 *
 *  1.0.0 - Initial release
 *
 *  This SmartApp will only work with devices using the LIFX Group of Groups device handler. If you attempt
 *  to use other devices there is a chance of getting into an infinite loop. The device handler that is needed
 *  can be found here:
 *
 *  https://github.com/ericvitale/ST-LIFX-Group-of-Groups
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
 *  You can find this smart app @ https://github.com/ericvitale/ST-LIFX-Sync
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
	name: "${appName()}",
	namespace: "ericvitale",
	author: "ericvitale@gmail.com",
	description: "Sync your LIFX groups.",
	category: "My Apps",
    singleInstance: true,
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Create a LFIX Sync.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New LIFX Sync Automation", multiple: true)
        }
        
        section("Settings") {
        	input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
        }
    }
}

def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
		section("Switches") {
        	input "masterSwitches", "capability.switch", title: "Master Switches", multiple: true
            input "slaveSwitches", "capability.switch", title: "Slave Switches", multiple: true
		}
    	section("Setting") {
        	label(title: "Assign a name", required: false)
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    	}
	}
}

private def appName() { return "${parent ? "LFIX Sync Automation" : "LIFX Sync"}" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "LIFX-Sync -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "LIFX-Sync -- Invalid Log Setting"
        }
    }
}

def installed() {   
	initialization() 
}

def updated(){
	unsubscribe()
    initialization()
}

def initialization() {
	log("Begin initialization().", "DEBUG")
    
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }
    
    log("End initialization().", "DEBUG")
}

def initParent() {
	log("initParent()", "DEBUG")
    
    unsubscribe()
}

def initChild() {
	subscribe(masterSwitches, "switch", switchHandler)
}

def switchHandler(evt){
    if(evt?.data == null || (evt.value != "on" && evt.value != "off")) {
    	//Nothing...
    } else {

        def j = parseJson(evt.data)

        if(j.syncing == "true") {
            log("Syncing is true, do nothing.", "DEBUG")
        } else {
            slaveSwitches.each { it->
            	if(evt.device.displayName != it.displayName) {
                    log("LIFX Sync is syncing ${it.displayName}.", "INFO")
                    if(evt.value == "off") {
                        it.off(true)
                    } else {
                        it.on(true)
                    }
                } else {
                	log("Skipping ${it.displayName} as it is the source.", "DEBUG")
                }
            }
        }
	}
}