/**
 *  Scenes
 *
 *  Copyright 2016 Glenn Brockett
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
    name: "Scenes",
    namespace: "ac7ss",
    author: "Glenn Brockett",
    description: "Use as macro to set scenes allowing for multiple lighting groups, capabilities and levels.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Triggering:") {
    	input (name: "TriggerSw", type: "capability.momentary", title: "Pushbutton Trigger", required: true, multiple: false)
		// TODO: put inputs here
	}
    section("Lights to turn on:"){

		input (name: "GroupD", type: "capability.switchLevel", title: "Dimmable lights group 1", required: false, multiple: true)
        input (name: "LevelD", type: "number", title: "Dim to level", range: "0..100", defaultValue: "100", required: true)
		input (name: "GroupD2", type: "capability.switchLevel", title: "Dimmable lights group 2", required: false, multiple: true)
        input (name: "LevelD2", type: "number", title: "Dim to level", range: "0..100", defaultValue: "100", required: true)

		input (name: "GroupH", type: "capability.colorControl", title: "Colour lights group", required: false, multiple: true)
	    input (name: "HueH", type: "number", title: "Set hue, 0 to 100", defaultValue: "75", range: "0..100", required: true)
    	input (name: "SatH", type: "number", title: "Set saturation %", defaultValue: "76", range: "0..100", required: true)
        input (name: "LevelH", type: "number", title: "Set level %", defaultValue: "100", range: "0..100", required: true)

		input (name: "GroupT", type: "capability.colorTemperature", title: "Temperature lights group", required: false, multiple: true)
        input (name: "TempT", type: "number", title: "Set Temperature, 2700K to 6500K", defaultValue: "2700", range: "2700..6500" , required: true)
        input (name: "LevelT", type: "number", title: "Set level %",  range: "0..100",  defaultValue: "100", required: true)

		input (name: "GroupS", type: "capability.switch", title: "Switched items group", required: false, multiple: true)
    }
    section("Items to turn off:"){
    	input (name: "GroupOff", type: "capability.switch", required: false, multiple: true)
    }
    section(){}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize();
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	TriggerSw.off();
	unsubscribe();
	initialize();
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(TriggerSw,"switch.on",Triggered);
}

// TODO: implement event handlers
def Triggered(evt){
	log.debug"Event"
	if(GroupD){
    	GroupD.setLevel(LevelD);
    	GroupD.on();
    }
	if(GroupD2){
    	GroupD2.setLevel(LevelD2);
    	GroupD2.on();
    }
    if(GroupH){
    	GroupH.setHue(HueH);
        GroupH.setSaturation(SatH);
        GroupH.setLevel(LevelH);
        GroupH.on();
    }
    if(GroupT){
    	GroupT.setColorTemperature(TempT);
        GroupT.setLevel(LevelT);
    	GroupT.on();
    }
    if(GroupS){
    	GroupS.on();
    }
    if(GroupOff){
    	GroupOff.off();
    }
//    TriggerSw.off()
}