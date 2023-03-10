/**
 *  Loop through colors and whites
 *
 *  Copyright 2016 Jerome Prunera
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
    name: "Loop through colors and whites",
    namespace: "prunera",
    author: "Jerome Prunera",
    description: "App used to loop through a predefined list of colors and whites using a SYLVANIA LIGHTIFY by Osram.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences {
    section("Select the trigger button") {
        input "triggerButton", "capability.button", title: "Trigger Button", required: true, multiple: false
    }

    section("Select the target light"){
    	input "targetLight", "capability.colorTemperature", title: "Target Light", required: true, multiple: false
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
	// TODO: subscribe to attributes, devices, locations, etc.
    
    state.cptColor = 0
    state.cptWhites = 0
    state.hsArray = [[hue:35, saturation:99.2],[hue:331, saturation:100],[hue:289, saturation:60.4],[hue:187, saturation:100],[hue:169, saturation:100]];
    state.lArray = [50,40.2,37.6,47.3,36.1];
    
    subscribe(triggerButton, "button.pushed", buttonPushed);
    subscribe(triggerButton, "button.level", buttonHeld);
    subscribe(triggerButton, "button.held", buttonHeld);
}

// TODO: implement event handlers
def configured(){
}

// Held button event
def buttonHeld(args){
	setColor();
}

// Push button event
def buttonPushed(args){
	def currentButtonNumber = parseJson(args.data).buttonNumber

    switch(currentButtonNumber){
    	case 1:
            toggleOnOff();
        break;
        
        case 2:
            setTemperature();
        break;
    }
}

// Function to toggle on or off
def toggleOnOff(){
	if(targetLight.currentSwitch == "on")
    	targetLight.off();
    else{
   		targetLight.on();
	}
}

// Function to loop through the different colors
def setColor(){
	state.cptColor = state.cptColor + 1;
    
	def nextColor = state.hsArray[state.cptColor - 1];
    def nextLevel = state.lArray[state.cptColor - 1];
    
    targetLight.setHue(nextColor.hue / 360 * 100);
    targetLight.setSaturation(nextColor.saturation);
    targetLight.setLevel(nextLevel);
    
    if(state.cptColor == 4){
    	state.cptColor = 0;
    }
}

// Function to set the different white temperatures
def setTemperature(){
	state.cptWhites = state.cptWhites + 1;
    if(state.cptWhites == 5){
    	state.cptWhites = 1;
    }
    
	// calculating the color temperature based on the number of Button2 pushed
    def temperature = ((int)(((3 - (state.cptWhites - 1)) * 1266 + 2700) / 100)) * 100;
    
    // the desired temperature is within range. If not reset counter
    targetLight.setLevel(80);

    log.debug "The temperature will be set to ${temperature}";
    targetLight?.setColorTemperature(temperature);
}