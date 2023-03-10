/**
 *  ****************  Which Car  ****************
 *
 *  Design Usage:
 *  This was designed to indicate who took which car..... 
 *  It was created as a response to the creation of a special DTH:
 *  This DTH was originally developed by SmartThings for a garage door, 
 *  then modified by Robin Winbourne for use with a dog feeder to give access to four 'states'.
 *  Then modified by me (@cobra) to change the text/colours/icons to be able to use it to show who took which car :)
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
 
 *
 *  Last Update: 08/11/2017
 *
 *  Changes:
 *
 *  V1.2.1 - Debug
 *  V1.2.0 - Added switchable logging  
 *  V1.1.0 - Added Enable/Disable switch - Added paragraph & header
 *  V1.0.0 - POC
 *
 *----------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *----------------------------------------------------------------------------------------------------------------------------
 */
 
 
 
 
 
 
definition(
    name: "Which Car?",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Sets a switch when a person leaves, and tries to work out who took what car",
    category: "Family",
   iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/car.png",
	iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/car.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/car.png",
)

preferences {


section("") {
        paragraph "V1.2.1"
       paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                  title: "Which Car?",
                  required: false,
                  "This app is designed to use a special 'Virtual Switch' to indicate who left with which vehicle"
    }



	section() {
		input "car1", "capability.presenceSensor", title: "Car 1 Presence Sensor", multiple: false, required: true
   		input "car2", "capability.presenceSensor", title: "Car 2 Presence Sensor", multiple: false, required: true
      	input "car3", "capability.presenceSensor", title: "Car 3 Presence Sensor", multiple: false, required: true
        input "carDelay", "number", title: "Delay after driver left to check for car presence", defaultValue: '5', description: "Minutes", required: true
    }
    
     section("Select Driver 1 "){
     input "carDriver1", "capability.presenceSensor", title: "Driver 1's Presence Sensor", multiple: false, required: true
    }
     section("Select Driver 2"){
     input "carDriver2", "capability.presenceSensor", title: "Driver 2's Presence Sensor", multiple: false, required: true
    }
     section("Select Driver Status Indicator Switch"){
     input "switch1", "capability.doorControl", title: "Driver 1 Virtual Presence Status", multiple: false, required: true
     input "switch2", "capability.doorControl", title: "Driver 2 Virtual Presence Status", multiple: false, required: true
    }
     section("Logging") {
            input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false
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
// Check if app is enabled
appEnable()
// Check Logging
logCheck()

// App Version
setAppVersion()

// Subscriptions
	subscribe(car1, "presence", "car1Handler")
    subscribe(car2, "presence", "car2Handler")
    subscribe(car3, "presence", "car3Handler")
    subscribe(carDriver1, "presence", "driver1Handler")
    subscribe(carDriver2, "presence", "driver2Handler")
    
    state.d1carStatus = 'not taken'
    state.d2carStatus = 'not taken'
    state.d3carStatus = 'not taken'
     
}






def car1Handler(evt){
state.d1car = evt.value
LOGDEBUG("$car1 = $state.d1car")

if (state.d1car  == "present") { 
state.d1carStatus = 'not taken'
LOGDEBUG("Car 1 = $state.d1carStatus")
}


}

def car2Handler(evt){
state.d2car = evt.value
LOGDEBUG("$car2 = $state.d2car")

if (state.d2car  == "present") { 
state.d2carStatus = 'not taken'
LOGDEBUG("Car 2 = $state.d2carStatus")
}
}

def car3Handler(evt){
state.d3car = evt.value
LOGDEBUG("$car3 = $state.d3car")

if (state.d3car  == "present") { 
state.d3carStatus = 'not taken'
LOGDEBUG("Car 3 = $state.d3carStatus")
}
}

def driver1Handler(evt) {
   state.driver1 = evt.value 
LOGDEBUG("$carDriver1 = $state.driver1")
	if (state.driver1  == "present") { 
LOGDEBUG("Driver 1 arrived so setting at home")
 switch1.at_home()
 }
 	if (state.driver2  == "not present") { 
    def driver1Delay = 60 * carDelay as int
LOGDEBUG("Driver 1 left so waiting $driver1Delay seconds then processing")
runIn(driver1Delay, processDriver1) 

 }
 
}
def driver2Handler(evt) {
   state.driver2 = evt.value  
LOGDEBUG("$carDriver2 = $state.driver2")
   
		if (state.driver2  == "present") { 
LOGDEBUG("Driver 2 arrived so setting at home")
  switch2.at_home()
    }
    
		if (state.driver2  == "not present") { 
         def driver2Delay = 60 * carDelay as int
LOGDEBUG("Driver 2 left so waiting $driver2Delay seconds then processing")
 runIn(driver2Delay, processDriver2)

 }    
    
}


def processBoth(){
 LOGDEBUG("processBoth")

if ( state.driver1 == "present"){
LOGDEBUG("$carDriver1 Present")
}
if ( state.driver2 == "present"){
LOGDEBUG("$carDriver2 Present")
}
if ( state.driver1 == "not present" &&  state.driver2 == "not present"){
LOGDEBUG("$carDriver1 and $carDriver2 both left so checking if they are in the same car")
}

if (state.appGo == true && state.d1car == "not present" && state.d2car == "present" && state.d3car == "present") { 
 	switch1.car1()
    switch2.car1()
	state.d1carStatus = 'taken'
LOGDEBUG("$carDriver1 & $carDriver2 are in $car1")

 }
 if (state.appGo == true && state.d2car == "not present" && state.d1car == "present" && state.d3car == "present") { 
 	switch1.car2()
    switch2.car2()
	state.d2carStatus = 'taken'
LOGDEBUG("$carDriver1 & $carDriver1 are in $car2")

 }
 if (state.appGo == true && state.d3car == "not present" && state.d2car == "present" && state.d1car == "present") { 
 	switch1.car3()
    switch2.car3()
	state.d1carStatus = 'taken'
LOGDEBUG("$carDriver1 & $carDriver1 are in $car3")

 }
  if (state.appGo == true && state.d3car == "present" && state.d2car == "present" && state.d1car == "present") { 
 switch1.at_home()
 switch2.at_home()
}
} 
 
 def processDriver1(){ 
 LOGDEBUG("processDriver1")
	if (state.appGo == true && state.d1car == "not present" && state.d1carStatus == 'not taken') { 
 	switch1.car1()
	state.d1carStatus = 'taken'
LOGDEBUG("$carDriver1 is in $car1")
 
} 
	if (state.appGo == true && state.d2car == "not present" && state.d2carStatus == 'not taken') { 
	switch1.car2()
	state.d2carStatus = 'taken'
LOGDEBUG("$carDriver1 is in $car2")
} 
	if (state.appGo == true && state.d3car == "not present" && state.d3carStatus == 'not taken') { 
	switch1.car3()
	state.d3carStatus = 'taken'
LOGDEBUG("$carDriver1 is in $car3")
} 
runIn(10,processBoth)
}
 def processDriver2(){ 
  LOGDEBUG("processDriver2")
	if (state.appGo == true && state.d1car == "not present" && state.d1carStatus == 'not taken') { 
 	switch2.car1()
	state.d1carStatus = 'taken'
LOGDEBUG("$carDriver2 is in $car1")
 
} 
	if (state.appGo == true && state.d2car == "not present" && state.d2carStatus == 'not taken') { 
	switch2.car2()
	state.d2carStatus = 'taken'
LOGDEBUG("$carDriver2 is in $car2")
} 
	if (state.appGo == true && state.d3car == "not present" && state.d3carStatus == 'not taken') { 
	switch2.car3()
	state.d3carStatus = 'taken'
LOGDEBUG("$carDriver2 is in $car3")
} 
runIn(10,processBoth)
}

// define debug action
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
    	if (settings.debugMode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (App Version: ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}

 
 
 
 
 // Enable/Disable App
def appEnable (){
	 state.appGo = true
   }
 
 // App Version   *********************************************************************************
def setAppVersion(){
    state.appversion = "1.2.1"
}