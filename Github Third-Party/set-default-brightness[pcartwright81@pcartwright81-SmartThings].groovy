/**
 *  Set Default Brightness
 *
 *  Copyright 2016 Patrick Cartwright
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
    name: "Set Default Brightness",
    namespace: "pcartwright81",
    author: "Patrick Cartwright",
    description: "Sets the default brightness for one light.  Was created because Smart Lighting does not have a 1%",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Config") {
			input(
            	name		: "dimmer"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.switchLevel")
            input "defaultlevel", "number", title: "Default brightness"
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
	subscribe(dimmer, "switch.on", dimHandler)
}

def dimHandler(evt) {
	//get the dimmer that's been turned on    
    log.trace "${dimmer.displayName} was turned on..."   
    def crntDimmerLevel = dimmer.currentValue("level").toInteger()
    def dimmerDefault = defaultlevel.toInteger()
    if(crntDimmerLevel == dimmerDefault){
    	return
    }
    dimmer.setLevel(dimmerDefault)
}