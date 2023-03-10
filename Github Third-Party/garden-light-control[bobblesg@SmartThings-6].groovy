/**
 *  ****************  Garden Light Control  ****************
 *
 *  Design Usage:
 *  This was designed to control a set of garden lights..
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
 *  Last Update: 21.09.2017
 *
 *  Changes:
 *
 * 
 *
 *
 *  V1.0.1 - Icons on GitHub
 *  V1.0.0 - POC
 *
 */
 
 
definition(
    name: "Garden Light Control",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Controls a set of sockets for garden lights turn on at sunset and off at a certain time",
    category: "",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/gardenlights.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/gardenlights.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/gardenlights.png")
 

preferences {


section() {
    
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/gardenlights.png",
                  title: "Garden Light Control",
                  required: false,
                  "Controls a set of sockets/switches for garden lights, turn on at sunset and off at a certain time"
    }
section() {
   
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
     
                  "Version: 1.0.1 Copyright © 2017 Cobra"
    }

	section(""){
            input "enableApp", "bool", title: "Enable App", required: true, defaultValue: true
        }
     	section(""){     
        input (name: "switch1", type: "capability.switch", title: "Control these switches", multiple: true, required: false)   
        input "offset", "number", title: "Turn on this many minutes before sunset"
        input (name: "offTime", title: "Turn Off - At what time?", type: "time",  required: true)
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
		log.debug "Initialised with settings: ${settings}"
        
setAppVersion()
// Check if app is enabled
	appEnable()
    
// Subscriptions
	subscribe(location, "sunsetTime", sunsetTimeHandler)
	schedule(offTime, offNow)
 }
 
 
 
def sunsetTimeHandler(evt) {
if(state.appGo == true){
    scheduleTurnOn(evt.value)
	}
    else if(state.appGo == false){
    log.info "App is diaabled so doing nothing"
}
}
def scheduleTurnOn(sunsetString) {
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    def timeBeforeSunset = new Date(sunsetTime.time - (offset * 60 * 1000))

    log.debug "Scheduling for: $timeBeforeSunset (sunset is $sunsetTime)"
    runOnce(timeBeforeSunset, turnOn)
}


def turnOn() {
    log.debug "Turning on lights"
    switch1.on()
	}


def offNow(evt){
 log.debug "Turning off lights"
    switch1.off()
}


// Enable/Disable App
def appEnable(){
	if (enableApp == true){ 
    state.appGo = true
    log.debug "App is Enabled" }
    else if (enableApp == false){ 
    state.appGo = false
    log.debug "App is Disabled" }
    
 }
 
 // App Version   *********************************************************************************
def setAppVersion(){
    state.appversion = "1.0.1"
}
