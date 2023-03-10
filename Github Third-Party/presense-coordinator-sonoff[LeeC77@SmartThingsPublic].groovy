/**
 *  Presense coordinator Sonoff
 *
 *  Copyright 2020 Lee Charlton
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
    name: "Presense coordinator Sonoff",
    namespace: "LeeC77",
    author: "Lee Charlton",
    description: "This smart App uses the custom device \u201CSonoff Wifi Switch and Presence Sensor\u201D to  control up to 10 Virtual Presence Sensors, without the need to install any additional Apps (ie. No Smartthings) on your WiFi capable personal devices or moblie phones, al that is required is a static IP.",
    category: "",
    iconUrl: "https://static.thenounproject.com/png/1539512-200.png",
    iconX2Url: "https://static.thenounproject.com/png/1539512-200.png",
    iconX3Url: "https://static.thenounproject.com/png/1539512-200.png")


preferences {
	section("Devices:") {
    	input "sonoff", "capability.Sensor", title: "Which Compound Sonoff device  detects presence ", multiple: false, required: true
    	input "who1", "capability.presenceSensor", title: "First Forced Mobile Presense?", multiple: false, required: true
    	input "who2", "capability.presenceSensor", title: "Second Forced Mobile Presense?", multiple: false, required: false
    	input "who3", "capability.presenceSensor", title: "Third Forced Mobile Presense?", multiple: false, required: false
    	input "who4", "capability.presenceSensor", title: "Fourth Forced Mobile Presense?", multiple: false, required: false
        input "who5", "capability.presenceSensor", title: "First Forced Mobile Presense?", multiple: false, required: false
    	input "who6", "capability.presenceSensor", title: "Second Forced Mobile Presense?", multiple: false, required: false
    	input "who7", "capability.presenceSensor", title: "Third Forced Mobile Presense?", multiple: false, required: false
    	input "who8", "capability.presenceSensor", title: "Fourth Forced Mobile Presense?", multiple: false, required: false
        input "who9", "capability.presenceSensor", title: "First Forced Mobile Presense?", multiple: false, required: false
    	input "who10", "capability.presenceSensor", title: "Second Forced Mobile Presense?", multiple: false, required: false
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
	// subscribe to the Sosnoff events
    subscribe(sonoff, "presence0", presence1)
    subscribe(sonoff, "presence1", presence2)
    subscribe(sonoff, "presence2", presence3)
    subscribe(sonoff, "presence3", presence4)
    subscribe(sonoff, "presence4", presence5)
    subscribe(sonoff, "presence5", presence6)
    subscribe(sonoff, "presence6", presence7)
    subscribe(sonoff, "presence7", presence8)
    subscribe(sonoff, "presence8", presence9)
    subscribe(sonoff, "presence9", presence10)
}

// Event handlers

def presence1(evt){
	//log.debug " Start "
    //log.debug "Event : ${evt.value}, ${evt.data}"
    def who = who1
    //def name = who.name
    def whoState = who.currentState("presence")
    //log.debug "${who}, ${whoState.value}"
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence2(evt){
    def who = who2
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence3(evt){
    def who = who3
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence4(evt){
    def who = who4
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence5(evt){
    def who = who5
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence6(evt){
    def who = who6
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence7(evt){
    def who = who7
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence8(evt){
    def who = who8
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence9(evt){
    def who = who9
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}
def presence10(evt){
    def who = who10
    def whoState = who.currentState("presence")
    if ((evt.value =~ /present/)&&(whoState.value=="not present")){
    	present(who)
    }
    if ((evt.value =~ /absent/)&&(whoState.value=="present")){
    	absent(who)
    }
}

def present (who){
	who.arrived()
}

def absent (who){
	who.departed()
}

//device.currentValue("who${i}")