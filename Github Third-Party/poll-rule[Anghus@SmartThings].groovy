/**
 *  Poll Rule
 *
 *  Copyright 2016 Jerry Honeycutt
 *
 *  Version 0.1   8 Dec 2016
 *
 *	Version History
 *
 *  0.1		08 Dec 2016		Initial version
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
    name: "Poll Rule",
    namespace: "Anghus",
    author: "Jerry Honeycutt",
    description: "Poll devices based upon a timer or motion sensor state changes.",
    category: "My Apps",

    parent: "Anghus:Home Rules",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home2-icn@3x.png")

/**/

preferences {
	page(name: "devicePage")
    page(name: "triggerPage")
	page(name: "installPage")
}

def devicePage() {
	dynamicPage(name: "devicePage", nextPage: "triggerPage", uninstall: true) {
        section("TARGET") {
            input name: "targetDevice", type: "capability.polling", title: "Device to poll", submitOnChange: true, required: true
        }
	}
}

def triggerPage() {
	dynamicPage(name: "triggerPage", nextPage: "installPage", uninstall: true) {
    	section("TRIGGER") {
        	input name: "triggerType", type: "enum", title: "Trigger type", options: ["Timer", "Motion"], submitOnChange: true, required: true
            if(triggerType == "Timer") {
                input name: "frequency", type: "number", title: "Poll timer (minutes)", required: true
                paragraph "Specify the frequency to poll the target."
            }
            else if(triggerType == "Motion") {
                input name: "motionDevices", type: "capability.motionSensor", title: "Motion devices", multiple: true, required: true
                paragraph "Events from these devices will poll the target."
            }
		}
    }
}

def installPage() {
	dynamicPage(name: "installPage", uninstall: true, install: true) {
    	section("NAME") {
        	label title: "Rule name", defaultValue: targetDevice.label, required: false
        }
        section("DEBUG") {
        	input name: "debug", type: "bool", title: "Debug rule?", defaultValue: false, required: true
        }
    }
}

/**/

def installed() {
	trace("installed()")
	initialize()
}

def updated() {
	trace("updated()")
    unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	trace("initialize()")
    if(frequency) schedule("0 0/$frequency * 1/1 * ? *", timerEvent)
	if(motionDevices) subscribe(motionDevices, "motion", triggerEvent)
}

/**/

def timerEvent() {
	trace("timerEvent()")
    if(targetDevice)
    	targetDevice.poll()
}

def triggerEvent(evt) {
	trace("triggerEvent($evt.value)")
    if(targetDevice)
    	targetDevice.poll()
}

/**/

private debug(message) {
	if(debug)
    	log.debug(message)
}

private trace(function) {
	if(debug)
    	log.trace(function)
}