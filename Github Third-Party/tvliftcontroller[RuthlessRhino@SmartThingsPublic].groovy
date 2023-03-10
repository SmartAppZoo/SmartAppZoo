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
    name: "tvLiftController",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "TV lift virtual supervisor.",
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
	section("Connect these virtual switches to the TV Positions") {
		input "switch1", title: "High", "capability.switch", required: true
        input "switch2", title: "Mid", "capability.switch", required: true
        input "switch3", title: "Low", "capability.switch", required: true
      
	}
    section("Which TV board?") {
		input "arduino", "capability.switch"
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
	subscribe(switch1, "switch.on", highon)
	subscribe(switch1, "switch.off", highoff)
    subscribe(switch2, "switch.on", midon)
	subscribe(switch2, "switch.off", midoff)
    subscribe(switch3, "switch.on", lowon)
	subscribe(switch3, "switch.off", lowoff)
}

 
def highon(evt)
{
	log.debug "switchOn1($evt.name: $evt.value: $evt.deviceId)"
    arduino.highon()
    switch1.off()
}

def highoff(evt)
{
	log.debug "switchOff1($evt.name: $evt.value: $evt.deviceId)"
    arduino.highoff()
}
def midon(evt)
{
	log.debug "switchOn2($evt.name: $evt.value: $evt.deviceId)"
    arduino.midon()
    switch2.off()
}

def midoff(evt)
{
	log.debug "switchOff2($evt.name: $evt.value: $evt.deviceId)"
    arduino.midoff()
}

def lowon(evt)
{
	log.debug "switchOn3($evt.name: $evt.value: $evt.deviceId)"
    arduino.lowon()
    switch3.off()
}

def lowoff(evt)
{
	log.debug "switchOff3($evt.name: $evt.value: $evt.deviceId)"
    arduino.lowoff()
}
