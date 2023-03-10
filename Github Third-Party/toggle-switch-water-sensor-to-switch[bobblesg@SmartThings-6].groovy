/**
 *  ****************  Switch Follows Water Sensor  ****************
 *
 *  Design Usage:
 *  This was designed to be used with a physical switch to control a virtual switch
 *  Uses a water sensor to receive physical switch commands via ST and converts that to virtual switch actions
 *
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm very happy for you to use this app without a donation, but if you find it useful then it would be nice to get a 'shout out' on the forum! -  @Cobra
 *  Have an idea to make this app better?  - Please let me know :)
 *
 *  Website: http://securendpoint.com/smartthings
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update: 17/08/2017
 *
 *  Changes:
 *
 *  V1.5.0 - Added switchable logging
 * 	V1.4.0 - Made it toggle for momentary switch
 *  V1.3.0 - Added optional enable/disable switch
 *  V1.2.0 - added off delay
 *  V1.0.0 - POC
 *
 */
 
 
 
 
definition(
    name: "Toggle Switch - Water Sensor to Switch",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Used with a momentary physical switch to toggle switch via a water sensor",
    category: "",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
	iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    )

preferences {
	section("") {
        paragraph " V1.5.0 "
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                  title: "Toggle Switch - Water Sensor to Switch",
                  required: false,
                  "This was designed to be used with a physical switch to control a virtual switch via a water sensor"
         }         

 	section("Switch to enable/disable app"){
		input "enableswitch1",  "capability.switch", title: "Control Switch - Optional", multiple: true, required: false
}
	section("") {
		input "alarm", "capability.waterSensor", title: "Water Sensor", required: true
          input "actionType1", "bool", title: "Select Water Sensor action type: \r\n \r\n On = 'Switch' ON when 'DRY'  \r\n Off = 'Switch' ON when 'WET' ", required: true, defaultValue: true
	}
	 section("Turn on this switch when wet/dry"){
		input "switch1",  "capability.switch", title: "Switch to control", multiple: true, required: false
	}
    section("Logging"){
            input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false
        }
	}


def installed() {
	initialise()
   
}

def updated() {
	unsubscribe()
	initialise()
   
}

def initialise() {
log.debug "Initialised with settings: ${settings}"
 setAppVersion()
 logCheck()
state.currS1 = 'off'
	subscribe(alarm, "water", waterHandler)
	subscribe(enableswitch1, "switch", enableswitchHandler)

}
  
def enableswitchHandler(evt) {
state.enable1 = evt.value
if (state.enable == 'on'){
switch1.on()
	}
else if (state.enable == 'off'){
switch1.off()
	}

} 
  
  
def waterHandler(evt) {
	controlType1 ()
		state.control1 = evt.value
        	LOGINFO(" event = $state.control1")
// goes wet
if (state.enable != 'off' && state.control1 == "wet" && state.type1 == 'off' && state.currS1 == 'on') { 
	turnOff()
		LOGINFO( "turning off...")
}
else if (state.enable != 'off' && state.control1 == "wet" && state.type1 == 'off' && state.currS1 == 'off') { 
	turnOn()
		LOGINFO("Turning on...")
}

// goes dry

else if (state.enable != 'off' && state.control1 == "dry" && state.type1 == 'on' && state.currS1 == 'on') { 
	turnOff()
		LOGINFO("turning off...")
}
else if (state.enable != 'off' && state.control1 == "dry" && state.type1 == 'on' && state.currS1 == 'off') { 
	turnOn()
		LOGINFO("Turning on...")
}
}



def turnOn(){
		switch1.on()
		state.currS1 = 'on'
}



def turnOff(){

		switch1.off()
		state.currS1 = 'off'
}





 
 // check action type switch

 def controlType1 (){
	if (actionType1 == true){ 
    state.type1 = 'on'
    LOGDEBUG( "Switch Type: ON (Toggle when dry)" )}
    else if (actionType1 == false){ 
    state.type1 = 'off'
     LOGDEBUG( "Switch Type: OFF (Toggle when wet") }
     }
     
     
     
     // Logging & App version...
     
def logCheck(){
state.checkLog = debugMode
if(state.checkLog == true){
log.info "All Logging Enabled"
}
else if(state.checkLog == false){
log.info "Further Logging Disabled"
}
}     



def LOGDEBUG(txt){
    try {
    	if (settings.debugmode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (Version ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}
def LOGINFO(txt){
    try {
    	if (settings.debugmode) { log.info("${app.label.replace(" ","_").toUpperCase()}  (Version ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGINFO unable to output requested data!")
    }
}


def setAppVersion(){
    state.appversion = "1.5.0"
}