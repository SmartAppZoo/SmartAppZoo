/**
 *  tvLiftContrller
 *
 *  Copyright 2014 Mike Maxwell
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
    name: "squeezeController",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "SqueezeBox virtual supervisor.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


/**
 *  VirtualSwitchParent
 *
 *  Author: badgermanus@gmail.com
 *  Date: 2014-03-26
 */
preferences {
	section("Connect these virtual switches to the squeeze virtual devices") {
		input "switch1", title: "family", "capability.switch", required: true
        input "switch2", title: "master", "capability.switch", required: true
        input "switch3", title: "garage", "capability.switch", required: true
      
	}
    section("Which squeeze device?") {
		input "squeeze", "capability.switch"
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe()
}


def subscribe() {
	subscribe(switch1, "switch.on", p1On)
	subscribe(switch1, "switch.off", p1Off)
    subscribe(switch2, "switch.on", p2On)
	subscribe(switch2, "switch.off", p2Off)
    subscribe(switch3, "switch.on", p3On)
	subscribe(switch3, "switch.off", p3Off)
}

 
def p1On(evt)
{
	log.debug "switchOn1($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1On()
}

def p1Off(evt)
{
	log.debug "switchOff1($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1Off()
}
def p2On(evt)
{
	log.debug "switchOn2($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2On()
}

def p2Off(evt)
{
	log.debug "switchOff2($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2Off()
}

def p3On(evt)
{
	log.debug "switchOn3($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3On()
}

def p3Off(evt)
{
	log.debug "switchOff3($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3Off()
}
