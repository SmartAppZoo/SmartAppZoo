/**
 *  VirtualSwitchParent
 *
 *  Copyright 2015 badgermanus
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
    name: "VirtualSwitchParent",
    namespace: "badgermanus",
    author: "badgermanus",
    description: "VirtualSwitchParent",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Connect these virtual switches to the Arduino's relays") {
		input "switch1", title: "Switch for relay 1", "capability.switch"
        input "switch2", title: "Switch for relay 2", "capability.switch", required: false
        input "switch3", title: "Switch for relay 3", "capability.switch", required: false
        input "switch4", title: "Switch for relay 4", "capability.switch", required: false 
        input "switch5", title: "Switch for relay 5", "capability.switch", required: false
        input "switch6", title: "Switch for relay 6", "capability.switch", required: false
        input "switch7", title: "Switch for relay 7", "capability.switch", required: false
        input "switch8", title: "Switch for relay 8", "capability.switch", required: false
        input "ledgreen", title: "Switch for ledgreen", "capability.switch", required: false
        input "ledyellow", title: "Switch for ledyellow", "capability.switch", required: false
	}
    section("Which Arduino relay board to control?") {
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

	// Listen to the virtual switches
	subscribe(switch1, "switch.on", switchOn1)
	subscribe(switch1, "switch.off", switchOff1)
    subscribe(switch2, "switch.on", switchOn2)
	subscribe(switch2, "switch.off", switchOff2)
    subscribe(switch3, "switch.on", switchOn3)
	subscribe(switch3, "switch.off", switchOff3)
    subscribe(switch4, "switch.on", switchOn4)
	subscribe(switch4, "switch.off", switchOff4)
    subscribe(switch5, "switch.on", switchOn5)
	subscribe(switch5, "switch.off", switchOff5)
    subscribe(switch6, "switch.on", switchOn6)
	subscribe(switch6, "switch.off", switchOff6)
    subscribe(switch7, "switch.on", switchOn7)
	subscribe(switch7, "switch.off", switchOff7)
    subscribe(switch8, "switch.on", switchOn8)
	subscribe(switch8, "switch.off", switchOff8)
    subscribe(ledgreen, "ledgreen.on", ledgreenOn)
	subscribe(ledgreen, "ledgreen.off", ledgreenOff)
    subscribe(ledyellow, "ledyellow.on", ledyellowOn)
	subscribe(ledyellow, "ledyellow.off", ledyellowOff)
       
}

def switchOn1(evt)
{
    	arduino.rcswitch1on()
}

def switchOff1(evt)
{
    	arduino.rcswitch1off()
}
def switchOn2(evt)
{
    	arduino.rcswitch2on()
}

def switchOff2(evt)
{
    	arduino.rcswitch2off()
}

def switchOn3(evt)
{
    	arduino.rcswitch3on()
}

def switchOff3(evt)
{
    	arduino.rcswitch3off()
}
def switchOn4(evt)
{
    	arduino.rcswitch4on()
}

def switchOff4(evt)
{
    	arduino.rcswitch4off()
}

def switchOn5(evt)
{
    	arduino.rcswitch5on()
}

def switchOff5(evt)
{
    	arduino.rcswitch5off()
}
def switchOn6(evt)
{
    	arduino.rcswitch6on()
}

def switchOff6(evt)
{
    	arduino.rcswitch6off()
}

def switchOn7(evt)
{
    	arduino.rcswitch7on()
}

def switchOff7(evt)
{
    	arduino.rcswitch7off()
}

def switchOn8(evt)
{
    	arduino.rcswitch8on()
}

def switchOff8(evt)
{
    	arduino.rcswitch8off()
}

def ledgreenOn(evt)
{
    	arduino.ledgreenon()
}

def ledgreenOff(evt)
{
    	arduino.ledgreenoff()
}

def ledyellowOn(evt)
{
    	arduino.ledyellowon()
}

def ledyellowOff(evt)
{
    	arduino.ledyellowoff()
}