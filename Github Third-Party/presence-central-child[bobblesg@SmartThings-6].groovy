/**
 *  ****************  Presence Central  ****************
 *
 *  Credits: I have to credit Brian Gudauskas (@bridaus -Reliable Presence) & Eric Roberts (@baldeagle072 - Everyones Presence) for stealing some of their code for multiple presence sensor determinations
 *
 *
 *  Design Usage:
 *  This is the 'Child' app for presence automation
 *
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  If you feel it's worth it then, donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  If you find this app useful then it would be nice to get a 'shout out' on the forum! -  @Cobra
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
 *  Last Update: 22/11/2017
 *
 *  Changes:
 *
 *
 *  V1.2.5 - debug - Typo issue with sunrise/sunset which caused the app to only work when switched on.
 *  V1.2.4 - added sunset/sunrise/ restrictions (with offset)
 *  V1.2.3 - Added 'Flash Lights' to available responses
 *  V1.2.2 - Moved 'restriction Options' to last page
 *	V1.2.1 - Changed restrictions from compulsory entries to optional entries 
 *  V1.2.0 - Added Locks & Doors to available responses
 *  V1.1.0 - Added enable/disable switching
 *  V1.0.1 - debug
 *  V1.0.0 - POC
 *
 */

 
definition(
    name: "Presence_Central_Child",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Child App for Presence Automation",
     category: "Fun & Social",

   
    
    parent: "Cobra:Presence Central",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/presence.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/presence.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/presence.png"
    )
    
    
    preferences {
    
    page name: "mainPage", title: "", install: false, uninstall: true, nextPage: "actionPage"
    page name: "actionPage", title: "", install: false, uninstall: true, nextPage: "finalPage"
    page name: "finalPage", title: "", install: true, uninstall: true
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}
def initialize() {
 log.info "Initialised with settings: ${settings}"
    setAppVersion()
    logCheck()
    switchRunCheck()
    state.timer1 = true
	state.timerDoor = true
    state.timerlock = true
	state.riseSetGo = true

// Basic Subscriptions    

	subscribe(enableSwitch, "switch", switchEnable)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	astroCheck()
    
// Trigger subscriptions

		if(trigger == "Single Presence Sensor"){
     	LOGDEBUG( "Trigger is: '$trigger'")
		subscribe(presenceSensor1, "presence", singlePresenceHandler) 
    }
		else if(trigger == "Group 1 \r\n(Anyone arrives or leaves = changed presence)"){
		LOGDEBUG( "Trigger is:  '$trigger'")
        setPresence1()
		subscribe(presenceSensor2, "presence", group1Handler) 
        
    }    
		else if(trigger == "Group 2 \r\n('Present' if anyone is at home)"){
		LOGDEBUG( "Trigger is:  '$trigger'")
        setPresence2()
		subscribe(presenceSensor3, "presence", group2Handler) 
        
    }


		else if(trigger == "Check for presence at a certain time"){
		LOGDEBUG( "Trigger is:  '$trigger'")
        subscribe(presenceSensor4, "presence", timePresenceHandler) 
        schedule(checkTime, checkPresenceTimeNow)
    	state.privatePresence = 'present'
    }


// Other subscriptions.

	if(doorContact1){
    	subscribe(doorContact1, "contact", doorContactHandler) 
    }
   
}






// main page *************************************************************************
def mainPage() {
    dynamicPage(name: "mainPage") {
      
        section {
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/presence.png",
                  title: "Presence Control Child",
                  required: false,
                  "This child app allows you to define different actions upon arrival or departure of one or more presence sensors"
                  }
     section() {
   
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                         "Child Version: $state.appversion - Copyright © 2017 Cobra"
    }             
      section() {
        	basicInputs()
          	  	}
        
  }
}




// action page *************************************************************************
def actionPage() {
    dynamicPage(name: "actionPage") {

 section() {
 
		triggerInput()
		presenceActions()
        outputActions()
        
	}
}
}


// name page *************************************************************************
def finalPage() {
       dynamicPage(name: "finalPage") {
       
            section("Automation name") {
                label title: "Enter a name for this message automation", required: false
            }
             section("Optional Restrictions ") {
           		mode title: "Only allow actions when in specific mode(s)", required: false
                input "fromTime", "time", title: "Only allow actions from ", required: false
				input "toTime", "time", title: "Only allow actions until", required: false 
				input "days", "enum", title: "Only allow actions on these days of the week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
                
		input "setRise", "bool", title: "Only allow actions between SunSet and SunRise", required: false, submitOnChange: true, defaultValue: false
				
if(setRise){
		input "sunriseOffsetValue", "number", title: "Optional Sunrise Offset (Minutes)", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        input "sunsetOffsetValue", "number", title: "Optional Sunset Offset (Minutes)", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
}

		input "riseSet", "bool", title: "Only allow actions between SunRise and SunSet", required: false, submitOnChange: true, defaultValue: false

if(riseSet){
		input "sunriseOffsetValue", "number", title: "Optional Sunrise Offset (Minutes)", required: false
		input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
        input "sunsetOffsetValue", "number", title: "Optional Sunset Offset (Minutes)", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
}


            }
             section("Logging") {
            input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false
  	        }
            
            
            
            
      }  
    }




def basicInputs(){

	input "enableSwitch", "capability.switch", title: "Select a switch Enable/Disable this automation (Optional)", required: false, multiple: false 
	
}

def triggerInput() {
   input "trigger", "enum", title: "How to trigger actions?",required: true, submitOnChange: true, options: ["Single Presence Sensor", "Group 1 \r\n(Anyone arrives or leaves = changed presence)", "Group 2 \r\n('Present' if anyone is at home)", "Check for presence at a certain time"]
  
}

def presenceActions(){
		if (trigger) {
		state.selection1 = trigger
    
	if(state.selection1 == "Single Presence Sensor"){
	input "presenceSensor1", "capability.presenceSensor", title: "Select presence sensor to trigger action", required: false, multiple: false 
    }
    
	else if(state.selection1 == "Group 1 \r\n(Anyone arrives or leaves = changed presence)"){
	input "presenceSensor2", "capability.presenceSensor", title: "Select presence sensors to trigger action", multiple: true, required: false
  	}
    
	else if(state.selection1 == "Group 2 \r\n('Present' if anyone is at home)"){
	input "presenceSensor3", "capability.presenceSensor", title: "Select presence sensors to trigger action", multiple: true, required: false
    }
    
    else if(state.selection1 == "Check for presence at a certain time"){
    input "checkTime", "time", title: "Time to check presence ", required: true
	input "presenceSensor4", "capability.presenceSensor", title: "Select presence sensor to check", multiple: true, required: false
    }
    
 }
}
    





def outputActions(){
input "presenceAction", "enum", title: "What action to take?",required: true, submitOnChange: true, options: ["Control A Switch", "Speak A Message", "Send A Message", "Change Mode", "Run a Routine", "Control a Door", "Control a Lock", "Flash Lights"]

if (presenceAction) {
    state.selection2 = presenceAction
    
    
    if(state.selection2 == "Flash Lights"){
    input "flashMode", "bool", title: " On = Flash when someone arrives \r\n Off = Flash when someone leaves", required: true, defaultValue: false
    input "switches", "capability.switch", title: "Flash these lights", multiple: true
	input "numFlashes", "number", title: "This number of times (default 3)", required: false
    input "onFor", "number", title: "On for (Milliseconds - default 1000)", required: false
	input "offFor", "number", title: "Off for (Milliseconds - default 1000)", required: false
    }
    
    else if(state.selection2 == "Control A Switch"){
     input "switch1", "capability.switch", title: "Select switch(s) to turn on/off", required: false, multiple: true 
     input "presenceSensor1Action1", "bool", title: "\r\n \r\n On = Switch On when someone arrives, or is present at check time (Off when they leave or if not present) \r\n Off = Switch Off when someone arrives or is present at check time (On when they leave or if not present) ", required: true, defaultValue: true  
    }
    
    
   else if(state.selection2 == "Speak A Message"){ 
   input "speaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true
	input "volume1", "number", title: "Normal Speaker volume", description: "0-100%", defaultValue: "100",  required: true
    input "message1", "text", title: "Message to play when sensor arrives (Or is present at check time)",  required: false
	input "message2", "text", title: "Message to play when sensor leaves (Or is not present at check time)",  required: false
    input "msgDelay", "number", title: "Minutes delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
	}
    
    
     else if(state.selection2 == "Send A Message"){
     input "message1", "text", title: "Message to send when sensor arrives  (Or is present at check time)",  required: false
	 input "message2", "text", title: "Message to send when sensor leaves  (Or is not present at check time)",  required: false
     input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
     }
     }
    
    else if(state.selection2 == "Change Mode"){
    input "newMode1", "mode", title: "Change to this mode when someone arrives (Or is present at check time)",  required: false
    input "newMode2", "mode", title: "Change to this mode when someone leaves (Or is not present at check time)",  required: false
    
    }
    
     else if(state.selection2 == "Run a Routine"){
      def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            input "routine1", "enum", title: "Select a routine to execute when someone arrives (Or is present at check time)", required: false, options: actions
            input "routine2", "enum", title: "Select a routine to execute when someone leaves (Or is not present at check time)" , required: false, options: actions
                    }
            }
    
     else if(state.selection2 == "Control a Door"){
     input "doorAction", "enum", title: "How to control door",required: true, submitOnChange: true, options: ["Single Momentary Switch", "Two Momentary Switch(es)", "Open/Close Door"]
     		selectDoorActions()
           }
           
     else if(state.selection2 == "Control a Lock"){
      		input "lock1", "capability.lock", title: "Select lock(s) ", required: false, multiple: true 
      		input "lockDelay", "number", title: "Minutes delay between actions (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
           }
    
    
    
    
    
	}
}    

def selectDoorActions(){
if(doorAction){
    state.actionOnDoor = doorAction
    
    if(state.actionOnDoor == "Two Momentary Switch(es)"){
    input "doorSwitch1", "capability.switch", title: "Switch to open", required: false, multiple: true
    input "doorSwitch2", "capability.switch", title: "Switch to close", required: false, multiple: true
    input "doorMomentaryDelay", "number", title: "How many seconds to hold switch on", defaultValue: '1', description: "Seconds", required: true
    input "doorDelay", "number", title: "Minutes delay between actions (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "doorContact1", "capability.contactSensor", title: "Door Contact (Optional)", required: false, multiple: false, submitOnChange:true
    if(doorContact1){
    input(name: "alertOption", type: "bool", title: "Send a notification when contact open/closed", description: null, defaultValue: false, submitOnChange:true)
    }
    if(alertOption == true){
       
		input "doorContactDelay", "number", title: "Once contact has been open/closed for this number of seconds", defaultValue: '0', description: "Seconds", required: true
        input("recipients", "contact", title: "Send notifications to") {
    	input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
    	input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
        }
    	input "message1", "text", title: "Send this message (Open)",  required: false
    	input "message2", "text", title: "Send this message (Closed)",  required: false
        
     }
    }
    
    else if(state.actionOnDoor == "Open/Close Door"){
    input "door1", "capability.doorControl", title: "Select door to open/close", required: false, multiple: true 
    input "doorDelay", "number", title: "Minutes delay between actions (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
    input "doorContact1", "capability.contactSensor", title: "Door Contact (Optional)", required: false, multiple: false, submitOnChange:true
    if(doorContact1){
    input(name: "alertOption", type: "bool", title: "Send a notification when contact open/closed", description: null, defaultValue: false, submitOnChange:true)
    }
    if(alertOption == true){
       
		input "doorContactDelay", "number", title: "Once contact has been open/closed for this number of seconds", defaultValue: '0', description: "Seconds", required: true
        input("recipients", "contact", title: "Send notifications to") {
    	input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
    	input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
        }
    	input "message1", "text", title: "Send this message (Open)",  required: false
    	input "message2", "text", title: "Send this message (Closed)",  required: false
        
     }
}
    
    else  if(state.actionOnDoor == "Single Momentary Switch"){
    input "doorSwitch1", "capability.switch", title: "Switch to open/close", required: false, multiple: true 
   	input "doorMomentaryDelay", "number", title: "How many seconds to hold switch on", defaultValue: '1', description: "Seconds", required: true
    input "doorDelay", "number", title: "Minutes delay between actions (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "doorContact1", "capability.contactSensor", title: "Door Contact (Optional)", required: false, multiple: false, submitOnChange:true 
    if(doorContact1){
    input(name: "alertOption", type: "bool", title: "Send a notification when contact open/closed", description: null, defaultValue: false, submitOnChange:true)
    }
    if(alertOption == true){
       
		input "doorContactDelay", "number", title: "Once contact has been open/closed for this number of seconds", defaultValue: '0', description: "Seconds", required: true
        input("recipients", "contact", title: "Send notifications to") {
    	input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
    	input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
        }
    	input "message1", "text", title: "Send this message (Open)",  required: false
    	input "message2", "text", title: "Send this message (Closed)",  required: false
        
     }
  }  
}
}
// ************************ Handlers ****************************************



// Single Presence =============================================================

def singlePresenceHandler(evt){
state.privatePresence = evt.value
if (state.privatePresence == "present"){
arrivalAction()
}
if (state.privatePresence == "not present"){
departureAction()
}
}
// end single presence =========================================================

// Timed Presence Check =========================================================



def checkPresenceTimeNow(evt){
LOGDEBUG("Activating timed check now....")
if (state.privatePresence == "present"){
arrivalAction()
}
if (state.privatePresence == "not present"){
departureAction()
}
}



def timePresenceHandler(evt){
state.privatePresence = evt.value
LOGDEBUG("state.privatePresence = $state.privatePresence")




}

// end timed presence check =====================================================


// Sunset & Sunrise Handlers ====================================================
def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	LOGDEBUG("riseTime: $riseTime")
	LOGDEBUG("setTime: $setTime")

	if (state.riseTime != riseTime.time) {
		unschedule("sunriseHandler")

		if(riseTime.before(now)) {
			riseTime = riseTime.next()
		}

		state.riseTime = riseTime.time

		LOGDEBUG("scheduling sunrise handler for $riseTime")
		schedule(riseTime, sunriseHandler)
	}

	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }

		state.setTime = setTime.time

		LOGDEBUG("scheduling sunset handler for $setTime")
	    schedule(setTime, sunsetHandler)
	}
}



def sunsetHandler() {
LOGDEBUG("Sun has set!")
def riseCheck = setRise
def setCheck = riseSet

if (riseCheck == true){
state.riseSetGo = true
LOGDEBUG("sunsetHandler - Actions Allowed")
}

if (setCheck == true){
state.riseSetGo = false
LOGDEBUG("sunsetHandler - Actions NOT Allowed")
}

}



def sunriseHandler() {
LOGDEBUG("Sun has risen!")
def riseCheck = setRise
def setCheck = riseSet

if (riseCheck == true){
state.riseSetGo = false
LOGDEBUG("sunriseHandler - Actions NOT Allowed")
}

if (setCheck == true){
state.riseSetGo = true
LOGDEBUG("sunriseHandler - Actions Allowed")
}


}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

// Group 1  ======================================================================
def group1Handler(evt) {

    if (evt.value == "present") {
        if (state.privatePresence1 != "present") {
            state.privatePresence1 = "present"
            state.privatePresence = "present"
           LOGDEBUG("A sensor arrived so setting group to '$state.privatePresence'")
           arrivalAction ()
            }
    } else if (evt.value == "not present") {
        if (state.privatePresence1 != "not present") {
            state.privatePresence1 = "not present"
            state.privatePresence = "not present"
            LOGDEBUG("A sensor left so setting group to '$state.privatePresence'")
            departureAction ()
        }
    }
}


// end group 1 ========================================================


// Group 2 ============================================================

def group2Handler(evt) {
	setPresence2()
}


// end group 2 ========================================================


// Door Contact Handler

def doorContactHandler(evt){
state.contactDoor = evt.value
LOGDEBUG("state.contactDoor = $state.contactDoor")



}

// end handlers *************************************************************



// ************************* Actions ****************************************

// Flash Actions

def checkFlashArrived(){
if (flashMode == true){
flashLights()
}
}
def checkFlashDeparted(){
if (flashMode == false){
flashLights()
}
}






private flashLights() {
	def doFlash = true
	def onFor = onFor ?: 1000
	def offFor = offFor ?: 1000
	def numFlashes = numFlashes ?: 3

	LOGDEBUG("LAST ACTIVATED IS: ${state.lastActivated}")
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		LOGDEBUG("DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}")
	}

	if (doFlash) {
		LOGDEBUG("FLASHING $numFlashes times")
		state.lastActivated = now()
		LOGDEBUG("LAST ACTIVATED SET TO: ${state.lastActivated}")
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			LOGDEBUG("Switch on after  $delay msec")
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			LOGDEBUG("Switch off after $delay msec")
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
}


// Arrival Actions - Check OK to run
def arrivalAction(){
LOGDEBUG("Calling Arrival Action")
checkTime()
checkDay()
if (state.timeOK == true && state.dayCheck == true){
decideActionArrival()
	}
}


// Departure Actions - Check OK to run
def departureAction(){
LOGDEBUG("Calling Departure Action")
checkTime()
checkDay()
if (state.timeOK == true && state.dayCheck == true){
decideActionDeparture()
	}
}





// Decide which action to call
def decideActionArrival() {
if(state.appgo == true && state.riseSetGo == true){
LOGDEBUG("Deciding on correct Arrival Action")

 if(state.selection2 == "Control A Switch"){
  LOGDEBUG("Decided to: 'Control A Switch' ")
  
  def actionType1 = presenceSensor1Action1
 LOGDEBUG("actionType1 = $actionType1") 
  if (actionType1 == true){
   LOGDEBUG("Switching on...")
  switch1.on()
  }
  else  if (actionType1 == false){
  LOGDEBUG("Switching off...")
  switch1.off()
  }
  
  
 }
else if(state.selection2 == "Speak A Message"){
  LOGDEBUG("Decided to: 'Speak A Message' ")
  state.msg1 = message1
	speakNow()
 }
 else if(state.selection2 == "Send A Message"){
 LOGDEBUG("Decided to: 'Send A Message' ")
 def msg = message1
  sendMessage(msg)
 }

 else if(state.selection2 == "Change Mode"){
   LOGDEBUG("Decided to: 'Change Mode'")
 if(newMode1){
 changeMode1()
 }
 }

 else if(state.selection2 == "Run a Routine"){
 LOGDEBUG("Decided to: 'Run a Routine' ") 
 state.routineGo = routine1
 LOGDEBUG("Running routine: $state.routineGo")
 location.helloHome?.execute(state.routineGo)
 }
 
 
  else if(state.selection2 == "Control a Door"){
  LOGDEBUG("Decided to: 'Control a Door' ") 
   if(state.actionOnDoor == "Two Momentary Switch(es)"){
   LOGDEBUG("Using 'two momentary' switching") 
    switchDoorOn()   
   }
   else if(state.actionOnDoor == "Single Momentary Switch"){
    LOGDEBUG("Using single momentary switching")  
   switchDoorOn2()
  } 
  else if(state.actionOnDoor == "Open/Close Door"){
  LOGDEBUG("Using standard door switching")
  LOGDEBUG("Opening door....")
	openDoorNow()
    }
 }
 
 else if(state.selection2 == "Control a Lock"){
  LOGDEBUG("Decided to: 'Control a Lock' ") 
	openLock()
 }
 else if(state.selection2 == "Flash Lights"){
  LOGDEBUG("Decided to: 'Flash Lights' ")
checkFlashArrived()
  
 }
 
} 
else if(state.appgo == false){
LOGDEBUG( "$enableSwitch is off so cannot continue")
}

else if(state.riseSetGo == false){
LOGDEBUG( "Cannot continue because of sunset-sunrise restrictions")
}



}






def decideActionDeparture() {
if(state.appgo == true && state.riseSetGo == true){
LOGDEBUG("Deciding on correct Departure Action")

 if(state.selection2 == "Control A Switch"){
 LOGDEBUG("Decided to: 'Control A Switch' ")
 
  def actionType1 = presenceSensor1Action1
  LOGDEBUG("actionType1 = $actionType1") 
  if (actionType1 == false){
  LOGDEBUG("Switching on...")
  switch1.on()
  }
  else  if (actionType1 == true){
  LOGDEBUG("Switching off...")
  switch1.off()
  }
 }
else if(state.selection2 == "Speak A Message"){
  LOGDEBUG("Decided to: 'Speak A Message' ")
  state.msg1 = message2
	speakNow()
 }
 else if(state.selection2 == "Send A Message"){
 LOGDEBUG("Decided to: 'Send A Message' ")
   def msg = message2
  sendMessage(msg)
 }

 else if(state.selection2 == "Change Mode"){
  LOGDEBUG("Decided to: 'Change Mode'")
 if(newMode2){
 changeMode2()
 }
 }

 else if(state.selection2 == "Run a Routine"){
 LOGDEBUG("Decided to: 'Run a Routine' ")
   state.routineGo = routine2
 LOGDEBUG("Running routine: $state.routineGo")
 location.helloHome?.execute(state.routineGo)
 
 }
 
  else if(state.selection2 == "Control a Door"){
  LOGDEBUG("Decided to: 'Control a Door' ")
  
  if(state.actionOnDoor == "Two Momentary Switch(es)"){
   LOGDEBUG("Using 'two momentary' switching") 
   switchDoorOff()
   }
   else if(state.actionOnDoor == "Single Momentary Switch"){
    LOGDEBUG("Using single momentary switching") 
   switchDoorOff2()
  } 
   else if(state.actionOnDoor == "Open/Close Door"){
   LOGDEBUG("Using standard door switching")
   LOGDEBUG("Closing door....")
	closeDoorNow()
    }
  
 
}
  else if(state.selection2 == "Control a Lock"){
  LOGDEBUG("Decided to: 'Control a Lock' ") 
  secureLock()
  
}
 else if(state.selection2 == "Flash Lights"){
  LOGDEBUG("Decided to: 'Flash Lights' ")
checkFlashDeparted()
  
}
 
}
else if(state.appgo == false){
LOGDEBUG( "$enableSwitch is off so cannot continue")
}
else if(state.riseSetGo == false){
LOGDEBUG( "Cannot continue because of sunset-sunrise restrictions")
}

 
}






// Group 1 Actions ======================================

def setPresence1(){
	def presentCounter1 = 0
    
    presenceSensor2.each {
    	if (it.currentValue("presence") == "present") {
        	presentCounter1++
        }
    }
    
    log.debug("presentCounter1: ${presentCounter1}")
    
    if (presentCounter1 > 0) {
    	if (state.privatePresence1 != "present") {
    		state.privatePresence1 = "present"
            state.privatePresence = "present"
            log.debug("A sensor arrived so setting group to '$state.privatePresence'")
        }
    } else {
    	if (state.privatePresence1 != "not present") {
    		state.privatePresence1 = "not present"
            state.privatePresence = "not present"
            log.debug("A sensor left so setting group to '$state.privatePresence'")
        }
    }
}

// end group 1 actions ==================================

// Group 2 Actions ======================================

def setPresence2(){
def	presentCounter2 = 0
        presenceSensor3.each {
    	if (it.currentValue("presence") == "present") {
        	presentCounter2++
        }
    }
    
    log.debug("Number of sensors present: ${presentCounter2}")
    
    if (presentCounter2 > 0) {
    	if (state.privatePresence2 != "present") {
            state.privatePresence2 = "present"
            state.privatePresence = "present"
            log.debug("Arrived - At least one sensor arrived - set group to '$state.privatePresence'")
             arrivalAction ()
        }
    } else {
    	if (state.privatePresence2 != "not present") {
            state.privatePresence2 = "not present"
            state.privatePresence = "not present"
            log.debug("Departed - Last sensor left - set group to '$state.privatePresence'")
             departureAction ()
        }
    }
}

// end group 2 actions ==================================

// Mode Actions  ======================================

def changeMode1() {
    LOGDEBUG( "changeMode1, location.mode = $location.mode, newMode1 = $newMode1, location.modes = $location.modes")

    if (location.mode != newMode1) {
        if (location.modes?.find{it.name == newMode1}) {
            setLocationMode(newMode1)
        }  else {
            LOGDEBUG( "Tried to change to undefined mode '${newMode1}'")
        }
    }
}
def changeMode2() {
    LOGDEBUG( "changeMode2, location.mode = $location.mode, newMode2 = $newMode2, location.modes = $location.modes")

    if (location.mode != newMode2) {
        if (location.modes?.find{it.name == newMode2}) {
            setLocationMode(newMode2)
        }  else {
            LOGDEBUG( "Tried to change to undefined mode '${newMode2}'")
        }
    }
}

// end mode actions =================================


// Message Actions ==================================


def sendMessage(msg) {
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sms) {
            sendSms(sms, msg)
        }
        if (pushNotification) {
            sendPush(msg)
        }
    }
}

// end message actions ===============================

// Speaking Actions ==================================

def speakNow(){
LOGDEBUG("speakNow called...")
checkVolume()

    if ( state.timer1 == true && state.msg1 != null){
	LOGDEBUG("Speaking now - Message: '$state.msg1'")
	speaker.speak(state.msg1)
   	startTimerSpeak()  
 } 
	else if ( state.timer1 == false){
	LOGDEBUG("NOT Speaking now - Too close to last message so I have to wait a while before I can speak again...")
 }
 	else if(state.msg1 == null){
    LOGDEBUG("No message configured")
    
    }
}

def startTimerSpeak(){
state.timer1 = false
state.timeDelay = 60 * msgDelay
LOGDEBUG("Waiting for $msgDelay minutes before resetting timer to allow further messages")
runIn(state.timeDelay, resetTimerSpeak)
}

def resetTimerSpeak() {
state.timer1 = true
LOGDEBUG( "Timer reset - Messages allowed again...")
}


// end speaking actions ==============================



// Door Actions ======================================
def openDoorNow(){
LOGDEBUG( "calling openDoorNow")
if(state.timerDoor == true){
 		if(state.contactDoor != 'open'){
			LOGDEBUG("Door is closed...")
            LOGDEBUG( "Opening door...")
        if(message1){
            def messageDelay = doorContactDelay
        	state.contactMsg = message1
            runIn(messageDelay, runContactMsg)
            }
			door1.open()
            startTimerDoor()
            }
  else if(state.contactDoor == 'open'){
          LOGDEBUG("Door already open!")
}            
            }
           
if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}


def closeDoorNow(){
LOGDEBUG( "calling closeDoorNow")
if(state.timerDoor == true){
			LOGDEBUG("Door is open...")
            LOGDEBUG("Closing door...")
			door1.close()
            if(message2){
            def messageDelay = doorContactDelay
        	state.contactMsg = message2
            runIn(messageDelay, runContactMsg)
            }
            startTimerDoor()
            }
 else if(state.contactDoor == 'open'){
          LOGDEBUG("Door already open!")
}

if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}


def startTimerDoor(){
LOGDEBUG("calling startTimerDoor")
state.timerDoor = false
def doorDelay1 = 60 * doorDelay as int
LOGDEBUG("Waiting for $doorDelay minutes before resetting timer to allow further actions")
runIn(doorDelay1, resetTimerDoor1)
}

def resetTimerDoor1() {
LOGDEBUG("calling resetTimerdoor1")
state.timerDoor = true
LOGDEBUG( "Timer reset - Actions allowed again...")
}


def switchDoorOn(){
LOGDEBUG( "calling switchDoorOn")
	if(state.timerDoor == true){
		def	momentaryTime1 = doorMomentaryDelay as int
       
        if(state.contactDoor != 'open'){
			LOGDEBUG("Opening door....")
	  		doorOn1() 
	  		runIn(momentaryTime1, doorOff1)
            if(message1){
            def messageDelay = doorContactDelay
        	state.contactMsg = message1
            runIn(messageDelay, runContactMsg)
            }
			startTimerDoor()
            }
         else if(state.contactDoor == 'open'){
          LOGDEBUG("Door already open!")
}
	else if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}
}
def switchDoorOff(){
LOGDEBUG( "calling switchDoorOff")
	if(state.timerDoor == true){
		def	momentaryTime1 = doorMomentaryDelay as int
         if(state.contactDoor != 'closed'){
			LOGDEBUG("Closing door....")
			doorOn2() 
			runIn(momentaryTime1, doorOff2)
             if(message2){
            def messageDelay = doorContactDelay
        	state.contactMsg = message2
            runIn(messageDelay, runContactMsg)
            }
			startTimerDoor()
            }
         else if(state.contactDoor == 'closed'){
           LOGDEBUG("Door already closed!")
         }
}
	else if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}

def switchDoorOn2(){
LOGDEBUG( "calling switchDoorOn")
	if(state.timerDoor == true){
		def	momentaryTime1 = doorMomentaryDelay as int
        if(state.contactDoor != 'open'){
			LOGDEBUG("Opening door....")
	  		doorOn1() 
	  		runIn(momentaryTime1, doorOff1)
             if(message1){
            def messageDelay = doorContactDelay
        	state.contactMsg = message1
            RunIn(messageDelay, runContactMsg)
            }
			startTimerDoor()
            }
           else if(state.contactDoor == 'open'){
           LOGDEBUG("Door already open!")
         } 
}
	else if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}

def switchDoorOff2(){
LOGDEBUG( "calling switchDoorOff2")
	if(state.timerDoor == true){
		def	momentaryTime1 = doorMomentaryDelay as int
         if(state.contactDoor != 'closed'){
			LOGDEBUG("Closing door....")
			doorOn1() 
			runIn(momentaryTime1, doorOff1)
             if(message2){
            def messageDelay = doorContactDelay
        	state.contactMsg = message2
            RunIn(messageDelay, runContactMsg)
            }
			startTimerDoor()
            }
           else if(state.contactDoor == 'closed'){
           LOGDEBUG("Door already closed!")
         }
}
	else if(state.timerDoor == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}



def doorOn1(){doorSwitch1.on()}
def doorOff1(){doorSwitch1.off()}
def doorOn2(){doorSwitch2.on()}
def doorOff2(){doorSwitch2.off()}

def runContactMsg(){
LOGDEBUG("runContactMsg - Sending message...")
def msg = state.contactMsg
  sendMessage(msg)

}


// end door actions ==================================


// Lock Actions ======================================

def secureLock(){
LOGDEBUG( "Securing Lock(s)")
if(state.timerlock == true){
// def anyLocked = lock1.count{it.currentLock == "unlocked"} != lock1.size()
//			if (anyLocked) {
			lock1.lock()
            LOGDEBUG("Locked")
            startTimerLock()
//            }
}
if(state.timerLock == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}

def openLock(){
LOGDEBUG( "Opening Lock(s)")
if(state.timerlock == true){
//	def anyUnlocked = lock1.count{it.currentLock == "locked"} != lock1.size()
//			if (anyUnlocked) {
			lock1.unlock()
            LOGDEBUG("Unlocked")
            startTimerLock()
//            }
}
if(state.timerLock == false){
LOGDEBUG("Too soon since last action to do anything - I need to wait for the timer to expire")
}
}


def startTimerLock(){
state.timerlock = false
state.timeDelayLock = 60 * lockDelay as int
LOGDEBUG("Waiting for $lockDelay minutes before resetting timer to allow further actions")
runIn(state.timeDelayLock, resetTimerLock1)
}

def resetTimerLock1() {
state.timerlock = true
LOGDEBUG( "Timer reset - Actions allowed again...")
}

// end lock actions ==================================

// end Actions ****************************************************************************



// Check time allowed to run... *******************************

def checkTime(){
if(fromTime){
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
else {
state.timeOK = true
}    
}

def checkDay(){
if(days){
 def df = new java.text.SimpleDateFormat("EEEE")
    
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    def dayCheck1 = days.contains(day)
    if (dayCheck1) {

  state.dayCheck = true
LOGDEBUG( " Day ok so can continue...")
 }       
 else {
LOGDEBUG( " Not today!")
 state.dayCheck = false
 }
}
else{
state.dayCheck = true
}
 
 }

// Check volume levels ****************************************

def checkVolume(){
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

// Enable Switch  **********************************************

def switchRunCheck(){
if(enableSwitch){
def switchstate = enableSwitch.currentValue('switch')
LOGDEBUG("Enable switch is used. Switch is: $enableSwitch ")

if(switchstate == 'on'){
state.appgo = true
LOGDEBUG("$enableSwitch - Switch State = $switchstate - Appgo = $state.appgo")
}
else if(switchstate == 'off'){
state.appgo = false
LOGDEBUG("$enableSwitch - Switch State = $switchstate - Appgo = $state.appgo")
}
}


if(!enableSwitch){
LOGDEBUG("Enable switch is NOT used. Switch is: $enableSwitch ")
state.appgo = true
LOGDEBUG("AppGo = $state.appgo")
}
}

def switchEnable(evt){
state.sEnable = evt.value
LOGDEBUG("$enableSwitch = $state.sEnable")
if(state.sEnable == 'on'){
state.appgo = true
}
else if(state.sEnable == 'off'){
state.appgo = false
}
}
// end enable switch ********************************************



// Define debug action  *****************************************
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
    	if (settings.debugMode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (Childapp Version: ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}
// end debug action ********************************************



// App Version   ***********************************************
def setAppVersion(){
    state.appversion = "1.2.5"
}
// end app version *********************************************