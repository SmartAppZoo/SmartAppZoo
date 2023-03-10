/**
 *  Link x10 devices
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
    name: "Link ST to x10 V2",
    namespace: "Johnwest80",
    author: "john west",
    description: "This will link SmartThings to x10.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page( name:"page1", title:"Preferences Page 1", uninstall:true, install:true ) {
        section( "Switches..." ) {
            input "switch1", "capability.switch", title:"Switch 1?", required: false
            input "switch1Code", "text", title:"Switch 1 X10 To Control?", required: false
            input "switch2", "capability.switch", title:"Switch 2?", required: false
            input "switch2Code", "text", title:"Switch 2 X10 To Control?", required: false
            input "switch3", "capability.switch", title:"Switch 3?", required: false
            input "switch3Code", "text", title:"Switch 3 X10 To Control?", required: false
            input "switch4", "capability.switch", title:"Switch 4?", required: false
            input "switch4Code", "text", title:"Switch 4 X10 To Control?", required: false
            input "contact1", "capability.contactSensor", title:"Contact Sensor?", required: false
            input "contact1Code", "text", title:"Contact 1 X10 To Control?", required: false
        }
        
        section("Which ST shield to control?") {
			input "shield", "capability.switch"
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
	subscribe(switch1, "switch.on", switch1OnHandler)
	subscribe(switch1, "switch.off", switch1OffHandler)
	subscribe(switch2, "switch.on", switch2OnHandler)
	subscribe(switch2, "switch.off", switch2OffHandler)
	subscribe(switch3, "switch.on", switch3OnHandler)
	subscribe(switch3, "switch.off", switch3OffHandler)
	subscribe(switch4, "switch.on", switch4OnHandler)
	subscribe(switch4, "switch.off", switch4OffHandler)
	subscribe(contact1, "contact.open", contact1OnHandler)
	subscribe(contact1, "contact.closed", contact1OffHandler)

	subscribe(shield, "commandFromX10", switchHandler, [filterEvents: false])
}

def switchHandler(evt)
{
    def onMatch = deviceOn + "-" + commandOn
    def offMatch = deviceOff + "-" + commandOff
    
    def eventValue = evt.value.split("/")[0]

	if (eventValue != "" && eventValue.indexOf("-") > 0)
    {
        def code = eventValue.split("-")[0]
        def command = eventValue.split("-")[1]


        log.debug "LST2x10V2-eventValue $eventValue / code $code / command $command"

        if (code == switch1Code)
            if (command == "On")
                switch1.on()
            else if (command == "Off")
                switch1.off()
        if (code == switch2Code)
            if (command == "On")
                switch2.on()
            else if (command == "Off")
                switch2.off()
        if (code == switch3Code)
            if (command == "On")
                switch3.on()
            else if (command == "Off")
                switch3.off()
        if (code == switch4Code)
            if (command == "On")
                switch4.on()
            else if (command == "Off")
                switch4.off()
    }
    else
    {
    	log.debug "LST2x10V2- ignored evt $evt.value from shield"
    }
}


def switch1OnHandler(evt) {
	handleSwitchCall(switch1Code, "On")
}

def switch1OffHandler(evt) {
	handleSwitchCall(switch1Code, "Off")
}

def switch2OnHandler(evt) {
	handleSwitchCall(switch2Code, "On")
}

def switch2OffHandler(evt) {
	handleSwitchCall(switch2Code, "Off")
}

def switch3OnHandler(evt) {
	handleSwitchCall(switch3Code, "On")
}

def switch3OffHandler(evt) {
	handleSwitchCall(switch3Code, "Off")
}

def switch4OnHandler(evt) {
	handleSwitchCall(switch4Code, "On")
}

def switch4OffHandler(evt) {
	handleSwitchCall(switch4Code, "Off")
}

def contact1OnHandler(evt) {
	handleSwitchCall(contact1Code, "On")
}

def contact1OffHandler(evt) {
	handleSwitchCall(contact1Code, "Off")
}

def handleSwitchCall(code, command)
{
    log.debug "LST2x10V2-sending { $code $command } to shield"
	shield.processX10Command(code, command)
}

