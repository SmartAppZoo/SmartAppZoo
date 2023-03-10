/**
 *  Thermostat Setpoint Manager
 *
 *  Copyright 2015 Eric Roberts
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
    name: "Thermostat Setpoint Manager",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "To manage your thermostats' setpoints",
    category: "Green Living",
    singleInstance: true,
    iconUrl: "http://baldeagle072.github.io/icons/thermostat@1x.png",
    iconX2Url: "http://baldeagle072.github.io/icons/thermostat@2x.png",
    iconX3Url: "http://baldeagle072.github.io/icons/thermostat@3x.png")


preferences {
	page name:"pageMain"
}

def pageMain() {
	return dynamicPage(name: "pageMain", title: "Create and/or edit setpoints", install: true, uninstall: true) {
        section {
            app(name: "childSetpoint", appName: "Setpoint", namespace: "baldeagle072", title: "Create a new setpoint automation", multiple: true)
        }
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

def initialize() {
	log.debug "PARENT:  there are ${childApps.size()} child Apps:"

	childApps.each {child ->
//    	log.debug "child.name is ${child.name}."
		if ( child.name == "Setpoint" ) {
			log.debug "child Setpoint: ${child.label}"
        }  
    }
}