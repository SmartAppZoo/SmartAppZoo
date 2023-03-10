/**
 *  ****************  Who Took The Car  ****************
 *
 *  Design Usage:
 *  This was designed to indicate who took the shared car..... 
 *  It was created as a response to the creation of a special DTH:
 *  This DTH was originally developed by SmartThings for a garage door, 
 *  then modified by Robin Winbourne for use with a dog feeder to give access to four 'states'.
 *  Then modified by me (@cobra) to change the text/colours/icons to be able to use it to show who took a shared car :)
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
 *  Last Update: 10/08/2017
 *
 *  Changes:
 *
 *  V1.2.0 - Added driver's own car 
 *  V1.1.1 - Debug & Typos
 *  V1.1.0 - Added Enable/Disable switch - Added paragraph & header
 *  V1.0.0 - POC
 */
 
 
 
 
 
 
definition(
    name: "Who took the car?",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Sets a switch when a car leaves, and tries to work out who took it",
    category: "Family",
   iconUrl: "http://54.246.165.27/img/icons/car.png",
	iconX2Url: "http://54.246.165.27/img/icons/car.png",
    iconX3Url: "http://54.246.165.27/img/icons/car.png",
)

preferences {


section("") {
        paragraph "V1.2.0"
       paragraph image: "http://54.246.165.27/img/icons/cobra3.png",
                  title: "Who Took The Car?",
                  required: false,
                  "This app is designed to use a special 'Virtual Switch' to indicate who left with a shared vehicle"
    }

 section(){
            input "enableApp", "bool", title: "Enable App", required: true, defaultValue: true
        }




	section("Select Shared Car Presence Sensor ") {
		input "sharedCar", "capability.presenceSensor", title: "Car Presence Sensor", multiple: false, required: true
   	}
    
     section("Select Driver 1 "){
     input "carDriver1", "capability.presenceSensor", title: "Driver 1's Presence Sensor", multiple: false, required: true
    }
     section("Select Driver 2"){
     input "carDriver2", "capability.presenceSensor", title: "Driver 2's Presence Sensor", multiple: false, required: true
    }
    
    section("Select Driver 1's Car Presence Sensor") {
		input "driver1Car", "capability.presenceSensor", title: "Car Presence Sensor", multiple: false, required: true
     }
    section("Select Driver 2's Car Presence Sensor ") {
    	input "driver2Car", "capability.presenceSensor", title: "Car Presence Sensor", multiple: false, required: true
    }
   
    section("Select Virtual Car Status Indicator Switch"){
     input "switch1", "capability.doorControl", title: "Virtual Presence Sensor", multiple: true, required: true
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

	subscribe(sharedCar, "presence", "sharedCarHandler")
    subscribe(carDriver1, "presence", "driver1Handler")
    subscribe(carDriver2, "presence", "driver2Handler")
    subscribe(driver1Car, "presence", "driver1OwnCarHandler")
    subscribe(driver2Car, "presence", "driver2OwnCarHandler")
    
}

def driver1OwnCarHandler(evt){
state.d1car = evt.value
log.debug "$driver1Car = $state.d1car"
}

def driver2OwnCarHandler(evt){
state.d2car = evt.value
log.debug "$driver2Car = $state.d2car"
}


def driver1Handler(evt) {
   state.currS2 = evt.value 
 log.debug "$carDriver1 = $state.currS2"
 if (state.currS2  == "not present") { 
 process() 
 }
 
}
def driver2Handler(evt) {
   state.currS3 = evt.value  
   log.debug "$carDriver2 = $state.currS3"
    if (state.currS3  == "not present") { 
    process() 
    }
}



def sharedCarHandler(evt) {
if (state.appGo == true){ 

state.currS1 = evt.value

 if (state.currS1 == "not present"){
  log.debug "$sharedCar = $state.currS1"
 def delay = 10  // **********************************************************
  log.info "Car left so checking who took it"
 log.info "Waiting for $delay seconds before checking"
 runIn(delay, process)
 }
 else if (state.currS1 == "present"){
 log.info "$sharedCar arrived"
 switch1.on_drive()
 }
 }
 }
 
 def process(){
 
 if (state.appGo == true){
 if (state.currS1  == "not present") {
 carAway()
 }
 else {
 log.info " Car is present"
 switch1.on_drive()
 }
 }
 }
 
 def carAway(){
 if (state.appGo == true){
 log.info "Checking now..."
 carTest()
 log.info "$carDriver1 = $state.currS2, $carDriver2 = $state.currS3 - $carDriver1's Car = $state.d1car, $carDriver2's Car = $state.d2car "
 
    if (state.currS2  == "present" && state.currS3 == "not present" && state.d2CarHome == true) {
 log.info " $carDriver2 took the car"
  switch1.driver2()
 }

 else if (state.currS3  == "present" && state.currS2 == "not present" && state.d1CarHome == true) {
  log.info " $carDriver1 took the car"
 switch1.driver1()
 
 }
 
 else if (state.currS3  == "not present" && state.currS2 == "not present" && state.d1CarHome == true && state.d2CarHome == false){
  log.info " $carDriver1 & $carDriver2 both left, but  $carDriver1's car is still here and $carDriver2's car has left so, $carDriver1 took the car !"
  switch1.driver1()
  }
   else if (state.currS3  == "not present" && state.currS2 == "not present" && state.d2CarHome == true && state.d1CarHome == false){
  log.info " $carDriver1 & $carDriver2 both left, but  $carDriver2's car is still here and $carDriver1's car has left so, $carDriver2 took the car !"
  switch1.driver2()
 }
  else if (state.currS3  == "not present" && state.currS2 == "not present" && state.d1CarHome == true && state.d2CarHome == true){
  log.info " $carDriver1 & $carDriver2 both left. $carDriver1's car is still here and $carDriver2's car is still here so $carDriver1 and $carDriver2 took the car together!"
 switch1.both()
 }
  else if (state.currS3  == "not present" && state.currS2 == "not present" && state.d1CarHome == false && state.d2CarHome == false){
  log.info " All cars and people are 'away' so it's impossible to decide who took the car!"
  }
  }
  }
 
 
 // Own car presence
 
 def carTest(){
 log.debug "CarTest"
 
 if (state.d1car == 'present'){
 state.d1CarHome = true
 }
  
  else if (state.d1car == 'not present'){
 state.d1CarHome = false
 }
 
  if (state.d2car == 'present'){
 state.d2CarHome = true
 
 }
 
 else if (state.d2car == 'not present'){
 state.d2CarHome = false
 }
 }
 
 
 
 
 
 
 
 // Enable/Disable App
def appEnable (){
	if (enableApp == true){ 
    state.appGo = true
    log.debug "App is Enabled" }
    else if (enableApp == false){ 
    state.appGo = false
    log.debug "App is Disabled" }
    
 } 

 
 