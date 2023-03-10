/**
 *  Link x10 to ST
 *
 *  Copyright 2014 john west
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
    name: "Link x10 to ST",
    namespace: "johnwest80",
    author: "john west",
    description: "Links x10 executed commands to ST",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page( name:"page1", title:"Preferences Page 1", nextPage:"page2", uninstall:true, install:false ) {
        section( "When x10 is executed..." ) {
            input("switchx", "capability.switch", title:"X10 Interface?", required: true)
            input("deviceOn", "text", title: "Turn on with device match of", description: "Ex. A1 or P16", required: false)
    		input("commandOn", "text", title: "Turn on when command match of", description: "On or Off", required: false)
            input("deviceOff", "text", title: "Turn off with device match of", description: "Ex. A1 or P16", required: false)
    		input("commandOff", "text", title: "Turn off when command match of", description: "On or Off", required: false)
        }
    }

    page( name:"page2", title:"Preferences Page 2", uninstall:true, install:true ) {
        section( "Change switch..." ) {
            input "switch1", "capability.switch", title:"Which light?"
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
	//subscribe(switchx, "executed", executedHandler, [filterEvents: false])
	subscribe(switchx, "commandFromX10", switchHandler, [filterEvents: false])
	//subscribe(switchx, "commandFromX10", commandFromX10Handler, [filterEvents: false])
}

def switchHandler(evt)
{
	log.debug "Switch Handler called"
    log.debug "evt: $evt.value"
    
    def onMatch = deviceOn + "-" + commandOn
    def offMatch = deviceOff + "-" + commandOff
    
    def eventValue = evt.value.split("/")[0]
    
    log.debug "eventValue $eventValue"
    
    if (eventValue == onMatch)
    	switch1.on()
    if (eventValue == offMatch)
    	switch1.off()
}

def commandFromX10Handler(evt) {
	log.debug "Smartapp x10 to st commandFromX10Handler called"
    //log.debug evt
	//log.debug "Value of commandFromX10 ${switchx.latestValue("commandFromX10")}"
	//log.debug "executed handler executed - $evt and $evt.value"
}
