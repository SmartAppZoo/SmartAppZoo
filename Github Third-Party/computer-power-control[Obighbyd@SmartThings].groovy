/**
 *  Computer Power Control
 *
 *  Copyright 2017 Sam Steele
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
    name: "Computer Power Control",
    namespace: "c99koder",
    author: "Sam Steele",
    description: "Powers a computer using a physical switch and eventGhost to power off computer.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {    
    section("Switches") {
    	input "theswitch", "capability.switch", required: true, title: "Simulated Switch"
    	input "thephysicalswitch", "capability.switch", required: true, title: "Physical Switch"
    }
    
    section("Computer Information") {
    	input "computerIP","text", required:true, title: "Computer IP Address"
        input "computerPort","number", required: true, title: "Webserver port"
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	state.ignoreOn = false
	subscribe(theswitch, "switch.on", theswitchOnHandler)
    subscribe(theswitch, "switch.off", theswitchOffHandler)
    
    runEvery15Minutes(ping)
    ping()
}

def theswitchOnHandler(evt) {
	if(!state.ignoreOn) {
		thephysicalswitch.off()
    	runIn(10, powerOnHandler)
    } else {
    	state.ignoreOn = false
    }
}

def theswitchOffHandler(evt) {
	def egHost = computerIP + ":" + computerPort
    sendHubCommand(new physicalgraph.device.HubAction("""GET /?Shutdown HTTP/1.1\r\nHOST: $egHost\r\n\r\n""",physicalgraph.device.Protocol.LAN))
}

def powerOnHandler() {
    unschedule(pingTimeout)
	thephysicalswitch.on()
}

def ping() {
    unschedule(pingTimeout)
	runIn(300, pingTimeout)
	def egHost = computerIP + ":" + computerPort
	sendHubCommand(new physicalgraph.device.HubAction("""GET / HTTP/1.1\r\nHOST: $egHost\r\n\r\n""",physicalgraph.device.Protocol.LAN,null,[callback:pingCallback]))
}

def pingCallback(physicalgraph.device.HubResponse hubResponse) {
    unschedule(pingTimeout)
	def v = theswitch.currentSwitch
    if(theswitch.currentSwitch != "on") {
	    log.debug("PC is now online, turning on virtual switch")
        state.ignoreOn = true
        theswitch.on()
    }
}

def pingTimeout() {
	def v = theswitch.currentSwitch
    if(theswitch.currentSwitch != "off") {
	    log.debug("PC is now offline, turning off virtual switch")
		theswitch.off()
    }
}