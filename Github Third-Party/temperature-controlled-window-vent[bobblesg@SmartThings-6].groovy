/**
 *  ****************  Temperature Controlled Window/Vent  ****************
 *
 *  Design Usage:
 *  This was designed to monitor a temp sensor and open/close a window to try and regulate temperature (Used for a powered Conservatory skylight)
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
 * 
 *
 *
 *
 *  V1.1.0 - Added 'quiet time' settings
 *  V1.0.0 - POC
 */


definition(
    name: " Temperature Controlled Window/Vent",
    namespace: "Cobra",
    author: "AJ Parker",
    description: "Monitor the temperature and when it goes above or below your Temp setting set the window/vent level.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/temp.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/temp.png"
)





// Config Pages

preferences {


	page name: "introPage", title: "", install: false, uninstall: true, nextPage: "inputPage"
    page name: "inputPage", title: "", install: false, uninstall: true, nextPage: "overridesPage"
    page name: "overridesPage", title: "", install: false, uninstall: true, nextPage: "settingsPage"
    page name: "settingsPage", title: "", install: true, uninstall: true
}




// main page *************************************************************************
def introPage() {
    dynamicPage(name: "introPage") {

section("") {
      
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/temp.png",
                  title: "Temperature Controlled Window/Vent",
                  required: false,
                  "This SmartApp was designed to control a window/vent relay.\r\nOpening and closing with varying temperatures. \r\nIt also has two optional forms of 'override' the first is a switch, the second is a 'Rain Sensor'. \r\nBoth of these can be used to close the window/vent and disable automatic temperature control \r\nThere is a further option: to speak a mesage when the window/vent is closed due to rain to let you know why the window/vent suddenly closed "
    }
    
    section() {
   
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra.png",
                  //       required: false,
                  "Version: $state.appversion - Copyright © 2017 Cobra"
    }
section(""){
            input "enableApp", "bool", title: "Enable App", required: true, defaultValue: true
        }
        
        

 

       
}
}

// Settings Page ***************************************************
def settingsPage(){
	 dynamicPage(name: "settingsPage") {
     
     
 // BASIC SETTINGSS
  section("App Settings"){	
        label title: "Assign a name to this app", required: false
            mode title: "Set for specific mode(s)", required: false
		}  	     
        section("Logging") {
            input "debugMode", "bool", title: "Debug Logging (Optional)", required: true, defaultValue: false
  	        }
            
            }
}


// Input Page  *********************************************************************
def inputPage(){
	 dynamicPage(name: "inputPage") {

	
	section("Which temperature sensor") {
		input "temperatureSensor1", "capability.temperatureMeasurement", title: "Select Sensor", required: true
					  }

	 section("Which window/vent control relay?") {
    input "relay1", "capability.switchLevel" , title: "Select Device", required: true
    											}
	section("Patially Open Level") {
		input(name:"halfLevel", type:"number", title: "% Open", required: true)
		}		

	section("At or below this temperature the window/vent should be fully closed") {
		input "fullClose", "number", title: "Temperature?" , required: true
																			 }
   
    section("At or above this temperature window/vent should be Partially Open - Obviously this MUST be between fully open setting and fully closed setting") {
		input "halfOpen", "number", title: "Temperature?" , required: true
        }
         section("At or above this temperature the window/vent should be fully open") {
		input "fullOpen", "number", title: "Temperature?" , required: true
																		   }
}
}

// OverRidesPage **********************************************************
def overridesPage() {
    dynamicPage(name: "overridesPage") {
 
    section("Select Enable/Disable Switch (Optional)") {
    		input "switch1", "capability.switch", title: "", required: false, multiple: false 
    }     

 section("Select Rain Sensor for emergency close (Optional)"){		
        input "water1", "capability.waterSensor", title: "", submitOnChange: true, required: false, multiple: true
        }
        
        if(water1 != null){
   section("Play Message when Wet/Dry"){	
   
   		input "switch2", "capability.switch", title: "Select switch to enable/disable messages", required: false, multiple: false 
 		input "speaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true
		input "volume", "number", title: "Speaker volume", description: "0-100%", defaultValue: "100",  required: true
        input "message1", "text", title: "Message to play when wet",  required: false
		input "message2", "text", title: "Message to play when dry",  required: false
   	 	input "msgSwitchDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay)", defaultValue: '0', description: "Seconds", required: true
		input "delay2", "number", title: "Number of minutes between messages", description: "Minutes", defaultValue: '0', required: true
		input "fromTime", "time", title: "Allow messages from", required: true
    	input "toTime", "time", title: "Allow messages until", required: true
    	input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
		}  	
        
    section("Set different volume on messages between these times?") {
		input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    	input "fromTime2", "time", title: "Quiet Time Start", required: false
   		input "toTime2", "time", title: "Quiet Time End", required: false
    }
       }
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
log.info "Initialised with settings: ${settings}"
setAppVersion()
appEnable()
	LOGDEBUG("")
    logCheck()
	subscribe(temperatureSensor1, "temperature", temperatureHandler)
    subscribe(switch1, "switch", switchHandler)
    subscribe(switch2, "switch", voiceSwitchHandler)
    subscribe(water1, "water", waterHandler)
    subscribe(relay1, "switch", relayHandler)
     
   				 }

// Handlers

def voiceSwitchHandler(evt){
state.voiceSwitch = evt.value
LOGDEBUG("Voice enable switch is $state.voiceSwitch")
}


def relayHandler(evt) {
   state.currS3 = evt.value  
   LOGDEBUG("$relay1 = $evt.value")

}

def switchHandler(evt) {
   state.currS1 = evt.value  // Note: Optional if switch is used to control action
   LOGDEBUG("$switch1 = $evt.value")
   if (state.currS1 != "on") {
    def myLevel = 0
     LOGDEBUG("$switch1: $evt.value, Setting $relay1 to configured level: $myLevel")
    relay1.setLevel(myLevel)
   						   }
					   }
                       
  def waterHandler(evt) {  
  if (state.appGo == true){
  
   state.currS2 = evt.value  // Note: Optional if water sensor is used to control action
   LOGDEBUG("$water1 = $evt.value")
   
   if (state.currS2 != "dry" && state.currS3 == "on") {
    def myLevel = 0
     LOGDEBUG("$water1: $evt.value, Setting $relay1 to configured level: $myLevel and playing message")
    relay1.setLevel(myLevel) 
    
    // Do I play message?
  
     def deltaSeconds = 20 
     LOGDEBUG("deltaSeconds = $deltaSeconds")

	def timeAgo = new Date(now() - (1000 * deltaSeconds))
	def recentEvents = water1.eventsSince(timeAgo)
	LOGDEBUG("Found ${recentEvents?.size() ?: 0} events in the last $deltaSeconds seconds")

	def alreadyDone = recentEvents.count { it.value && it.value == "wet" } > 1

	if (alreadyDone) {
		LOGDEBUG("Not playing message as this was done within the last $deltaSeconds seconds")
        
	} else {
    def msgdelay = msgSwitchDelay
   LOGDEBUG("Playing message in $msgdelay seconds")
		runIn(msgdelay, messageGo)
			}
    } 
    else  if (state.currS2 == "dry") {
    LOGDEBUG(" $water1 is dry now")
    runIn(msgdelay, messageGo)
    }
    }
	}				 

def temperatureHandler(evt) {
if (state.appGo == true){
LOGDEBUG("temperature: $evt.value, $evt")
  if (state.currS2 != "dry" && state.currS1 != "on") { 
    LOGDEBUG("$water1 is showing rain or $switch1 is off so not active")
     
}

    else if (state.currS2 != "wet" && state.currS1 != "off") { 
          
   
    if (evt.doubleValue <= fullClose) {
    def myLevel = 0
     
    relay1.setLevel(myLevel)
    LOGDEBUG("Reported temperature: $evt.value, Setting $relay1 to configured level: $myLevel because $evt.value is < or = $fullClose")
  									  }
    
	else if (evt.doubleValue >= fullOpen) {
     def myLevel = 100
     LOGDEBUG("Reported temperature: $evt.value, Setting $relay1 to configured level: $myLevel")
    relay1.setLevel(myLevel)
    
    
    									  }
    
		else if (evt.doubleValue >= halfOpen && evt.doubleValue < fullOpen) {
    def myLevel = halfLevel as int
    LOGDEBUG("Reported temperature: $evt.value, This is > or = $halfOpen but < $fullOpen - Setting $relay1 to configured level: $myLevel ")
    relay1.setLevel(myLevel)
  
    								}    								  	  }
}

	}
    
  
  
 // Message 

  def messageGo (){

checkDay()
checkTime()
	state.msg1 = message1
	state.msg2 = message2
    
	if(state.dayCheck == true && state.timeOK == true && state.msg1 != null && state.currS2 == "wet" && state.voiceSwitch != 'off' && state.currS3 == 'on' && state.timer != 'no'){
 LOGDEBUG("Speaking now as the sensor shows wet, the window is open  and the time and day are correct")
setVolume()
   speaker.speak(state.msg1) 
   
    }
else if(state.dayCheck == true && state.timeOK == true && state.currS2 == "dry" && state.msg2 != null && state.voiceSwitch != 'off' && state.timer != 'no'){
 LOGDEBUG(" Speaking now as the sensor shows dry and the time and day are correct")
 setVolume()
   speaker.speak(state.msg2)  
   startTimer()
	}
 }
 
 
// Enable/Disable App
def appEnable (){
	if (enableApp == true){ 
    state.appGo = true
    LOGDEBUG("App is Enabled") }
    else if (enableApp == false){ 
    state.appGo = false
    LOGDEBUG("App is Disabled") }
    
 }    
  
// Check time allowed to run...

def checkTime(){

def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
    if (between) {
    state.timeOK = true
    LOGDEBUG("Time is ok so can continue...")
    
}
else if (!between) {
state.timeOK = false
LOGDEBUG("Time is NOT ok so cannot continue...")
	}
}

def checkDay(){

 def df = new java.text.SimpleDateFormat("EEEE")
    
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    def dayCheck1 = days.contains(day)
    if (dayCheck1) {

  state.dayCheck = true
 }       
 else {
LOGDEBUG(" Not today!")
 state.dayCheck = false
 }
 }


// Delay between messages...

def startTimer(){
state.timer = 'no'
state.timeDelay = 60 * delay2
LOGDEBUG(" Waiting for $state.timeDelay seconds before resetting timer to allow further messages")
runIn(state.timeDelay, resetTimer)
}

def resetTimer() {
state.timer = 'yes'
LOGDEBUG(" Timer reset - Messages allowed")

}



// set volume dependent upton time

def setVolume(){
def timecheck = fromTime2
if (timecheck != null){
def between2 = timeOfDayIsBetween(fromTime2, toTime2, new Date(), location.timeZone)
    if (between2) {
    
    state.volume = volume2
   speaker.setLevel(state.volume)
    
   LOGDEBUG("Quiet Time = Yes - Setting Quiet time volume")
    
}
else if (!between2) {
state.volume = volume1
LOGDEBUG("Quiet Time = No - Setting Normal time volume")

speaker.setLevel(state.volume)
 
	}
}
else if (timecheck == null){

state.volume = volume1
speaker.setLevel(state.volume)

	}
}







 
def LOGDEBUG(txt){
    try {
    	if (settings.debugMode) { log.debug("${app.label.replace(" ",".").toUpperCase()}  (AppVersion: ${state.appversion})  ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
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
 
 
 
 
// App Version   *********************************************************************************
def setAppVersion(){
    state.appversion = "1.1.0"
}