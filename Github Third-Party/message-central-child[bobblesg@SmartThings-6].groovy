/**
 *  ****************  Message_Central_Child  ****************
 *
 *  Design Usage:
 *  This is the 'Child' app for message automation
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
 *
 *  Last Update: 22/11/2017
 *
 *  Changes:
 *
 *
 *  V2.0.1 - Debug
 *  V2.0.0 - Added 'Weather Report' - Trigger with Switch, Water, Contact, & Time
 *  V1.9.0 - Added 'Open Too Long' to speak when a contact (door?) has been open for more than the configured number of minutes
 *  V1.8.0 - Added ability to speak/send message if contact is open at a certain time (Used to check I closed the shed door)
 *  V1.7.0 - Added ability to SMS/Push instead of speaking
 *  V1.6.0 - Added Routines & Mode Change as triggers
 *  V1.5.1 - Debug - Disable switch not always working
 *  V1.5.0 - Added 'Presence' restriction so will only speak if someone is present/not present
 *  V1.4.0 - Added 'Power' trigger and ability to use 'and stays that way' to use with Washer or Dryer applicance
 *  V1.3.2 - Debug
 *  V1.3.1 - Code cleanup & new icon path
 *  V1.3.0 - Added 'quiet' time to allow different volume levels at certain times
 *  V1.2.2 - New Icons
 *  V1.2.1 - Debug - Time did not have day restriction
 *  V1.2.0 - Added switchable logging
 *	V1.1.0 - Added delay between messages
 *  V1.0.2 - Debug
 *  V1.0.1 - Header & Debug
 *  V1.0.0 - POC
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *-------------------------------------------------------------------------------------------------------------------
 */

definition(
    name: "Message_Central_Child",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Child App for Message Automation",
     category: "Fun & Social",

   
    
    parent: "Cobra:Message Central",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/voice.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/voice.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/voice.png")

preferences {
    page name: "mainPage", title: "", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "", install: true, uninstall: true
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
      state.timer2 = true
      state.presenceRestriction = true
      state.contact1SW = 'closed' 
     if(state.msgType == "Voice Message"){ 
     checkVolume()
     }
      
// Subscriptions    

subscribe(enableSwitch, "switch", switchEnable)

if(trigger == 'Time'){
   LOGDEBUG("Trigger is $trigger")
   schedule(runTime,timeTalkNow)
    }
if(trigger == 'Time if Contact Open'){
   LOGDEBUG("Trigger is $trigger")
   schedule(runTime,timeTalkNow1)
   subscribe(contact1, "contact", contact1Handler)
    }    
    
else if(trigger == 'Switch'){
     LOGDEBUG( "Trigger is $trigger")
subscribe(switch1, "switch", switchTalkNow)
    }
else if(trigger == 'Water'){
    LOGDEBUG( "trigger is $trigger")
subscribe(water1, "water.wet", waterTalkNow) 
subscribe(water1, "water.dry", waterTalkNow) 
	}
else if(trigger == 'Contact'){
    LOGDEBUG( "trigger is $trigger")
subscribe(contactSensor, "contact", contactTalkNow) 
	}
else if(trigger == 'Presence'){
    LOGDEBUG("trigger is $trigger")
subscribe(presenceSensor1, "presence", presenceTalkNow) 
     
	}
else if(trigger == 'Power'){
    LOGDEBUG("trigger is $trigger")
subscribe(powerSensor, "power", powerTalkNow) 
     
	}
else if(trigger == 'Routine'){
    LOGDEBUG("trigger is $trigger")
 subscribe(location, "routineExecuted", routineChanged)
    
    }
else if(trigger == 'Mode Change'){
    LOGDEBUG("trigger is $trigger")
subscribe(location, "mode", modeChangeHandler)

	}
    
else if(trigger == 'Open Too Long'){
    LOGDEBUG("trigger is $trigger")
subscribe(openSensor, "contact", tooLongOpen)

	}
    
if (restrictPresenceSensor){
subscribe(restrictPresenceSensor, "presence", restrictPresenceSensorHandler)
}    
}


// main page *************************************************************************
def mainPage() {
    dynamicPage(name: "mainPage") {
      
        section {
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/voice.png",
                  title: "Message Control Child",
                  required: false,
                  "This child app allows you use different triggers to create different messages"
                  }
     section() {
   
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                         "Child Version: $state.appversion - Copyright © 2017 Cobra"
    }             
      section() {
        	speakerInputs()
            triggerInput()
            actionInputs()
        }
         }
}


def namePage() {
       dynamicPage(name: "namePage") {
       
            section("Automation name") {
                label title: "Enter a name for this message automation", required: false
            }
             section("Modes") {
           		mode title: "Set for specific mode(s)", required: false
            }
             section("Logging") {
            input "debugMode", "bool", title: "Enable logging", required: true, defaultValue: false
  	        }
      }  
    }



// defaults
def speakerInputs(){	
	input "enableSwitch", "capability.switch", title: "Select switch Enable/Disable this message (Optional)", required: false, multiple: false 
    input "messageAction", "enum", title: "Select Message Type", required: true, submitOnChange: true,  options: ["Voice Message", "SMS/Push Message", "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"]

 if (messageAction){
 state.msgType = messageAction
    if(state.msgType == "Voice Message"){
	input "speaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true
	input "volume1", "number", title: "Normal Speaker volume", description: "0-100%", defaultValue: "100",  required: true
	}

	else if(state.msgType == "Weather Report"){
	input "speaker", "capability.musicPlayer", title: "Choose speaker(s)", required: false, multiple: true
	input "volume1", "number", title: "Normal Speaker volume", description: "0-100%", defaultValue: "80",  required: true

	}
 }
}


// inputs
def triggerInput() {
   input "trigger", "enum", title: "How to trigger message?",required: true, submitOnChange: true, options: ["Time", "Switch", "Presence", "Water", "Contact", "Power", "Mode Change", "Routine", "Time if Contact Open", "Contact - Open Too Long"]
  
}


def actionInputs() {
    if (trigger) {
    state.selection = trigger
    
    
if(state.selection == 'Switch'){
    input "switch1", "capability.switch", title: "Select switch to trigger message/report", required: false, multiple: false 

    
    if(state.msgType == "Voice Message"){
	input "message1", "text", title: "Message to play when switched on",  required: false
	input "message2", "text", title: "Message to play when switched off",  required: false
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay)", defaultValue: '0', description: "Seconds", required: true
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
    if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send when switched On",  required: false
	 input "message2", "text", title: "Message to send when switched Off",  required: false
     input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    }
    }
	if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    input "weatherSwitchMode", "bool", title: "   On = Play when switched on  \r\n   Off = Play when switched off ", required: true, defaultValue: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
    
   
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
}    
 

else if(state.selection == 'Water'){
	
     
    if(state.msgType == "Voice Message"){
    input "water1", "capability.waterSensor", title: "Select water sensor to trigger message", required: false, multiple: false 
	input "message1", "text", title: "Message to play when WET",  required: false
	input "message2", "text", title: "Message to play when DRY",  required: false
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay)", description: "Seconds", required: true
   	input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
     if(state.msgType == "SMS/Push Message"){
     input "water1", "capability.waterSensor", title: "Select water sensor to trigger message", required: false, multiple: false 
     input "message1", "text", title: "Message to send when Wet",  required: false
	 input "message2", "text", title: "Message to send when Dry",  required: false
     input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    }
    }
    if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    input "weatherSwitchMode", "bool", title: "   On = Play when wet  \r\n   Off = Play when dry ", required: true, defaultValue: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
    
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
}   

else if(state.selection == 'Presence'){
	input "presenceSensor1", "capability.presenceSensor", title: "Select presence sensor to trigger message", required: false, multiple: false 
   
    if(state.msgType == "Voice Message"){
	input "message1", "text", title: "Message to play when sensor arrives",  required: false
	input "message2", "text", title: "Message to play when sensor leaves",  required: false
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay)", defaultValue: "0", description: "Seconds", required: true
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
     if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send when sensor arrives",  required: false
	 input "message2", "text", title: "Message to send when sensor leaves",  required: false
     input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
} 

else if(state.selection == 'Contact'){
	input "contactSensor", "capability.contactSensor", title: "Select contact sensor to trigger message", required: false, multiple: false 
   
     
    if(state.msgType == "Voice Message"){
	input "message1", "text", title: "Message to play when sensor opens",  required: false
	input "message2", "text", title: "Message to play when sensor closes",  required: false
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay)", description: "Seconds", required: true
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
	input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
     if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send when sensor opens",  required: false
	 input "message2", "text", title: "Message to send when sensor closes",  required: false
     input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }   
    if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    input "weatherSwitchMode", "bool", title: "   On = Play when open  \r\n   Off = Play when closed ", required: true, defaultValue: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
    
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
} 

else if(state.selection == 'Power'){
	input "powerSensor", "capability.powerMeter", title: "Select power sensor to trigger message", required: false, multiple: false 
    input(name: "belowThreshold", type: "number", title: "Power Threshold (Watts)", required: true, description: "this number of watts")
    input "actionType1", "bool", title: "Select Power Sensor action type: \r\n \r\n On = Alert when power goes ABOVE configured threshold  \r\n Off = Alert when power goes BELOW configured threshold", required: true, defaultValue: false
	input(name: "delay1", type: "number", title: "Only if it stays that way for this number of minutes...", required: true, description: "this number of minutes", defaultValue: '0')
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
    
  if(state.msgType == "Voice Message"){
    input "message1", "text", title: "Message to play ...",  required: false
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay - Seconds)", description: "Seconds", required: true, defaultValue: '0'
   
	input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
  if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send...",  required: false
	 input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }    
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
} 

else if(state.selection == 'Time'){
	input (name: "runTime", title: "Time to run", type: "time",  required: true) 
  if(state.msgType == "Voice Message"){
	input "messageTime", "text", title: "Message to play",  required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
   		}
  if(state.msgType == "SMS/Push Message"){
     input "messageTime", "text", title: "Message to send...",  required: false
     input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	 input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }  
    if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    }
    
   
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
   
}   

else if(state.selection == 'Time if Contact Open'){
	input (name: "runTime", title: "Time to run", type: "time",  required: true) 
    input "contact1", "capability.contactSensor", title: "Select contact sensor to check", required: false, multiple: false 
   
  if(state.msgType == "Voice Message"){
	input "messageTime", "text", title: "Message to play if contact open",  required: true
   		}
  if(state.msgType == "SMS/Push Message"){
     input "messageTime", "text", title: "Message to send if contact open",  required: false
	 input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }  
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
   
}   

else if(state.selection == 'Mode Change'){
	input "newMode1", "mode", title: "Action when changing to this mode",  required: false
    
     
  if(state.msgType == "Voice Message"){
	input "message1", "text", title: "Message to play",  required: true
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay - Seconds)", description: "Seconds", required: true, defaultValue: '0'
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	 input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
    
   if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send...",  required: false
	 input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    } 
     if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
} 
else if(state.selection == 'Routine'){
	  def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            input "routine1", "enum", title: "Action when this routine runs", required: false, options: actions
            }
           
            
   if(state.msgType == "Voice Message"){
	input "message1", "text", title: "Message to play",  required: true
    input "triggerDelay", "number", title: "Delay after trigger before speaking (Enter 0 for no delay - Seconds)", description: "Seconds", required: true, defaultValue: '0'
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
  if(state.msgType == "SMS/Push Message"){
    input "message1", "text", title: "Message to send...",  required: false
	input("recipients", "contact", title: "Send notifications to") {
    input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
    input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    } 
     if(state.msgType == "Weather Report\r\n(Only for Switch, Water, Contact, Mode, Routine & Time)"){
    input "message1", "text", title: "Message to play before weather report",  required: true, defaultValue: "It's %time% on %day%, %date% ,,, Here is your weather forcast for today,,,"
    input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
	input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
} 
if(state.selection == 'Contact - Open Too Long'){
	input "openSensor", "capability.contactSensor", title: "Select contact sensor to trigger message", required: false, multiple: false 
   	input(name: "opendelay1", type: "number", title: "Only if it stays open for this number of minutes...", required: true, description: "this number of minutes", defaultValue: '0')
   
    
  if(state.msgType == "Voice Message"){
    input "message1", "text", title: "Message to play ...",  required: false
    input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
  	input "fromTime", "time", title: "Allow messages from", required: true
    input "toTime", "time", title: "Allow messages until", required: true
    input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    input "volume2", "number", title: "Quiet Time Speaker volume", description: "0-100%", defaultValue: "0",  required: true
    input "fromTime2", "time", title: "Quiet Time Start", required: false
    input "toTime2", "time", title: "Quiet Time End", required: false
    }
  if(state.msgType == "SMS/Push Message"){
     input "message1", "text", title: "Message to send...",  required: false
	 input("recipients", "contact", title: "Send notifications to") {
     input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
     input(name: "pushNotification", type: "bool", title: "Send a push notification to", description: null, defaultValue: true)
    	}
    }    
	input "restrictPresenceSensor", "capability.presenceSensor", title: "Select presence sensor to restrict action", required: false, multiple: false, submitOnChange: true
    if(restrictPresenceSensor){
   	input "restrictPresenceAction", "bool", title: "   On = Action only when someone is 'Present'  \r\n   Off = Action only when someone is 'NOT Present'  ", required: true, defaultValue: false    
	}
}
    

}
}





// Handlers


// Open Too Long
def tooLongOpen(evt){
state.openContact = evt.value

if (state.openContact == 'open' && state.appgo == true && state.presenceRestriction == true){
LOGDEBUG("tooLongOpen - Contact is open")
openContactTimer1()
}

else if (state.openContact == 'closed'){
LOGDEBUG("tooLongOpen - Contact is closed")
}
 else if(state.appgo == false){
    LOGDEBUG("App disabled by $enableswitch being off")
}

}


def openContactTimer1(){

LOGDEBUG( "tooLongOpen - openContactTimer1 -  Contact is: $state.openContact")
   def mydelayOpen = 60 * opendelay1 as int
   LOGDEBUG( "openContactTimer1 - Checking again after delay: $opendelay1 minute(s)... ")
       runIn(mydelayOpen, openContactSpeak)     
      }
      
      
def openContactSpeak(){
LOGDEBUG( "openContactSpeak -  Contact is: $state.openContact")
state.msg1 = message1
state.msgNow = 'oneNow'


if (state.openContact == 'open'){
     LOGDEBUG( "openContactSpeak -  Still open...")
    
    if(state.msgType == "Voice Message"){
    talkSwitch()
   }          
  
      
  else if(state.msgType == "SMS/Push Message"){
	def msg = message1
LOGDEBUG("tooLongOpen - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
	}
  
   
  }
 }

// Mode Change

def modeChangeHandler(evt){
state.modeNow = evt.value
LOGDEBUG("state.modeNow = $state.modeNow")
 state.msg1 = message1
 LOGDEBUG("state.msg1 = $state.msg1")
	
 
 state.msgNow = 'oneNow'
 
		if (evt.isStateChange){
LOGDEBUG(" State Change - The value of this event is different from its previous value: ${evt.isStateChange()}")
def modeChangedTo = newMode1
		if(state.modeNow == modeChangedTo){
   	LOGDEBUG( "Mode is now $modeChangedTo")
    
 	if(state.msgType == "Voice Message"){    
def mydelay = triggerDelay
checkVolume()
LOGDEBUG("Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
}
	

if(state.msgType == "SMS/Push Message"){
def msg = message1
LOGDEBUG("Mode Change - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
	} 
    
if(state.msgType == "Weather Report"){
LOGDEBUG("Mode - Weather Report")
getWeatherReport()
}    
   }
}
}

// Routines
def routineChanged(evt) {
state.newRoutine = evt.displayName

state.msg1 = message1
state.msgNow = 'oneNow'
def routineToCheckRun = routine1
LOGDEBUG("state.newRoutine = $state.newRoutine")
  
 LOGDEBUG("state.msg1 = $state.msg1") 
 
 if(state.newRoutine == routineToCheckRun){
 
 	LOGDEBUG( "Routine running: $state.newRoutine")
	if(state.msgType == "Voice Message"){     
def mydelay = triggerDelay
checkVolume()
LOGDEBUG("Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
}

if(state.msgType == "SMS/Push Message"){
def msg = message1
LOGDEBUG("Routine running - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
	} 
if(state.msgType == "Weather Report"){
LOGDEBUG("Routine - Weather Report")
getWeatherReport()
}

 }


   
}


// Define restrictPresenceSensor actions
def restrictPresenceSensorHandler(evt){

state.presencestatus1 = evt.value
LOGDEBUG("Presence = $state.presencestatus1")
def actionPresenceRestrict = restrictPresenceAction


if (state.presencestatus1 == "present" && actionPresenceRestrict == true){
LOGDEBUG("Presence ok")
state.presenceRestriction = true
}
else if (state.presencestatus1 == "not present" && actionPresenceRestrict == true){
LOGDEBUG("Presence not ok")
state.presenceRestriction = false
}

if (state.presencestatus1 == "not present" && actionPresenceRestrict == false){
LOGDEBUG("Presence ok")
state.presenceRestriction = true
}
else if (state.presencestatus1 == "present" && actionPresenceRestrict == false){
LOGDEBUG("Presence not ok")
state.presenceRestriction = false
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
def LOGDEBUG(txt){
    try {
    	if (settings.debugMode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (Childapp Version: ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}



// Enable Switch

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
LOGDEBUG("AppGo = $state.appgo")
}
else if(state.sEnable == 'off'){
state.appgo = false
LOGDEBUG("AppGo = $state.appgo")
}
}


// Time
def timeTalkNow(evt){
checkDay()


LOGDEBUG("state.appgo = $state.appgo - state.dayCheck = $state.dayCheck - state.volume = $state.volume - runTime = $runTime")
if(state.appgo == true && state.dayCheck == true && state.presenceRestriction == true){
LOGDEBUG("Time trigger -  Activating now! ")

if(state.msgType == "Voice Message"){ 
def msg = messageTime
checkVolume()
LOGDEBUG( "Speaker(s) in use: $speaker set at: $state.volume% - Message to play: $msg"  )
speaker.speak(msg)
}

if(state.msgType == "SMS/Push Message"){
def msg = messageTime
LOGDEBUG("Time - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
	} 
    
if(state.msgType == "Weather Report"){
LOGDEBUG("Time - Weather Report")
getWeatherReport()
}

}

else if(state.appgo == false){
LOGDEBUG( "$enableSwitch is off so cannot continue")
}
else if(state.dayCheck == false){
LOGDEBUG( "Cannot continue - Daycheck failed")
}
else if(state.presenceRestriction ==  false){
LOGDEBUG( "Cannot continue - Presence failed")
}

}


// Time if Contact Open
def contact1Handler (evt) {
 state.contact1SW = evt.value 
LOGDEBUG( "$contact1 = $evt.value")
						 }



def timeTalkNow1(evt){
checkDay()


LOGDEBUG("state.appgo = $state.appgo - state.dayCheck = $state.dayCheck - state.volume = $state.volume - runTime = $runTime")
if(state.appgo == true && state.dayCheck == true && state.presenceRestriction == true && state.contact1SW == 'open' ){
LOGDEBUG("Time trigger -  Activating now! ")

if(state.msgType == "Voice Message"){ 
def msg = messageTime
checkVolume()
LOGDEBUG( "Speaker(s) in use: $speaker set at: $state.volume% - Message to play: $msg"  )
speaker.speak(msg)
}

if(state.msgType == "SMS/Push Message"){
def msg = messageTime
LOGDEBUG("Time - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
	} 

}

else if(state.appgo == false){
LOGDEBUG( "$enableSwitch is off so cannot continue")
}
else if(state.dayCheck == false){
LOGDEBUG( "Cannot continue - Daycheck failed")
}
else if(state.presenceRestriction ==  false){
LOGDEBUG( "Cannot continue - Presence failed")
}

else if(state.contact1SW != 'open'){
LOGDEBUG( "Cannot continue - $contact1 is Closed")
}


}




// Switch
def switchTalkNow(evt){
state.talkswitch = evt.value
state.msg1 = message1
state.msg2 = message2

if(state.msgType == "Weather Report"){
LOGDEBUG("Switch - Weather Report")
if (weatherSwitchMode == true && state.talkswitch == 'on'){
getWeatherReport()
}
if (weatherSwitchMode == false && state.talkswitch == 'off'){
getWeatherReport()
}




}
if(state.msgType == "Voice Message"){
LOGDEBUG("Switch - Voice Message")

	if(state.talkswitch == 'on'){
state.msgNow = 'oneNow'
    }

	else if (state.talkswitch == 'off'){
state.msgNow = 'twoNow'
	}

LOGDEBUG( "$switch1 is $state.talkswitch")
def mydelay = triggerDelay
checkVolume()
LOGDEBUG("Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
}

if(state.msgType == "SMS/Push Message"){
LOGDEBUG("Switch - SMS/Push Message")
	if(state.talkswitch == 'on' && state.msg1 != null){
def msg = message1
LOGDEBUG("Switch - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
    }
    
    
    if(state.talkswitch == 'off' && state.msg2 != null){
def msg = message2
LOGDEBUG("Switch - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
    }

}

}



// Contact
def contactTalkNow(evt){
state.talkcontact = evt.value
state.msg1 = message1
state.msg2 = message2

if(state.msgType == "Weather Report"){
LOGDEBUG("Contact - Weather Report")
if (weatherSwitchMode == true && state.talkcontact == 'open'){
getWeatherReport()
}
if (weatherSwitchMode == false && state.talkcontact == 'closed'){
getWeatherReport()
}
}
if(state.msgType == "Voice Message"){
	if(state.talkcontact == 'open'){
state.msgNow = 'oneNow'
}
	else if (state.talkcontact == 'closed'){
state.msgNow = 'twoNow'
}

LOGDEBUG("$contactSensor is $state.talkcontact")
def mydelay = triggerDelay
checkVolume()
LOGDEBUG( "Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
}

if(state.msgType == "SMS/Push Message"){
	if(state.talkcontact == 'open' && state.msg1 != null){
def msg = message1
LOGDEBUG("Contact - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}

	else if (state.talkcontact == 'closed' && state.msg2 != null){
def msg = message2
LOGDEBUG("Contact - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}


	}
}






// Water
def waterTalkNow(evt){
state.talkwater = evt.value
state.msg1 = message1
state.msg2 = message2

if(state.msgType == "Weather Report"){
LOGDEBUG("Water - Weather Report")
if (weatherSwitchMode == true && state.talkwater == 'wet'){
getWeatherReport()
}
if (weatherSwitchMode == false && state.talkwater == 'dry'){
getWeatherReport()
}
}		

if(state.msgType == "Voice Message"){
        
	if(state.talkwater == 'wet'){
state.msgNow = 'oneNow'
	}
	else if (state.talkwater == 'dry'){
state.msgNow = 'twoNow'
	}

LOGDEBUG( "$water1 is $state.talkwater")
def mydelay = triggerDelay
checkVolume()
LOGDEBUG( "Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
	}
    
if(state.msgType == "SMS/Push Message"){
	if(state.talkwater == 'wet' && state.msg1 != null){
def msg = message1
LOGDEBUG("Water - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}

	else if(state.talkwater == 'dry' && state.msg2 != null){
def msg = message2
LOGDEBUG("Water - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}    
}    
    
    
}

// Presence
def presenceTalkNow(evt){
state.talkpresence = evt.value
state.msg1 = message1
state.msg2 = message2

if(state.msgType == "Voice Message"){

	if(state.talkpresence == 'present'){
state.msgNow = 'oneNow'
	}

	else if (state.talkpresence == 'not present'){
state.msgNow = 'twoNow'
	}

LOGDEBUG( "$presenceSensor1 is $state.talkpresence")
def mydelay = triggerDelay
checkVolume()
LOGDEBUG("Speaker(s) in use: $speaker set at: $state.volume% - waiting $mydelay seconds before continuing..."  )
runIn(mydelay, talkSwitch)
}


if(state.msgType == "SMS/Push Message"){
	if(state.talkpresence == 'present' && state.msg1 != null){
def msg = message1
LOGDEBUG("Presence - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}

	else if (state.talkpresence == 'not present' && state.msg2 != null){
def msg = message2
LOGDEBUG("Presence - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)

}    
} 





}


// Power 
def powerTalkNow (evt){


 state.meterValue = evt.value as double
    
	LOGDEBUG("$powerSensor shows $state.meterValue Watts")
    if(state.appgo == true){
	checkNow1()  
	}
    else if(state.appgo == false){
    LOGDEBUG("App disabled by $enableswitch being off")

}
}
def checkNow1(){
if( actionType1 == false){
LOGDEBUG( "checkNow1 -  Power is: $state.meterValue")
    state.belowValue = belowThreshold as int
    if (state.meterValue < state.belowValue) {
   def mydelay = 60 * delay1 as int
   LOGDEBUG( "Checking again after delay: $delay1 minutes... Power is: $state.meterValue")
       runIn(mydelay, checkAgain1)     
      }
      }
      
else if( actionType1 == true){
LOGDEBUG( "checkNow1 -  Power is: $state.meterValue")
    state.belowValue = belowThreshold as int
    if (state.meterValue > state.belowValue) {
   def mydelay = 60 * delay1 as int
   LOGDEBUG( "Checking again after delay: $delay1 minutes... Power is: $state.meterValue")
       runIn(mydelay, checkAgain2)     
      }
      }
  }

 

def checkAgain1() {
   
     if (state.meterValue < state.belowValue) {
      LOGDEBUG( " checkAgain1 - Checking again now... Power is: $state.meterValue")
    
      speakNow()
        
			}
     else  if (state.meterValue > state.belowValue) {
     LOGDEBUG( "checkAgain1 -  Power is: $state.meterValue so cannot run yet...")
	}	
}		

def checkAgain2() {
   
     if (state.meterValue > state.belowValue) {
      LOGDEBUG( "checkAgain2 - Checking again now... Power is: $state.meterValue")
    
      speakNow()
        
			}
     else  if (state.meterValue < state.belowValue) {
     LOGDEBUG( "checkAgain2 -  Power is: $state.meterValue so cannot run yet...")
	}	
}		



def speakNow(){
LOGDEBUG("Power - speakNow...")

state.msg1 = message1
    if ( state.timer1 == true && state.presenceRestriction == true){
  if(state.msgType == "Voice Message"){
  checkVolume()
	LOGDEBUG("Speaking now...")
	speaker.speak(state.msg1)
   	startTimerPower()  
    }
   if(state.msgType == "SMS/Push Message" && state.msg1 != null){
def msg = message1
LOGDEBUG("Power - SMS/Push Message - Sending Message: $msg")
  sendMessage(msg)
startTimerPower()
 
} 
}    
  if(state.presenceRestriction ==  false){
LOGDEBUG( "Cannot continue - Presence failed")
}
}


def startTimerPower(){
state.timer1 = false
state.timeDelay = 60 * msgDelay
LOGDEBUG("Waiting for $msgDelay minutes before resetting timer to allow further messages")
runIn(state.timeDelay, resetTimerPower)
}

def resetTimerPower() {
state.timer1 = true
LOGDEBUG( "Timer reset - Messages allowed")
}











// Talk now....

def talkSwitch(){
LOGDEBUG("Calling.. talkSwitch")
if(state.appgo == true){
LOGDEBUG("Calling.. CheckTime")
checkTime()
LOGDEBUG("Calling.. CheckDay")
checkDay()

LOGDEBUG("state.appgo = $state.appgo - state.timeOK = $state.timeOK - state.dayCheck = $state.dayCheck - state.timer1 = $state.timer1 - state.timer2 = $state.timer2 - state.volume = $state.volume")
if(state.timeOK == true && state.dayCheck == true && state.presenceRestriction == true){

LOGDEBUG( " Continue... Check delay...")

if(state.msgNow == 'oneNow' && state.timer1 == true && state.msg1 != null){
LOGDEBUG("All OK! - Playing message 1: '$state.msg1'")
speaker.speak(state.msg1)
startTimer1()
}
else if(state.msgNow == 'twoNow'  && state.msg2 != null && state.timer2 == true){
LOGDEBUG( "All OK! - Playing message 2: '$state.msg2'")
speaker.speak(state.msg2)
startTimer2()
}


else if(state.timeOK == false){
LOGDEBUG("Not enabled for this time so cannot continue")
}
else if(state.presenceRestriction ==  false){
LOGDEBUG( "Cannot continue - Presence failed")
}

else if(state.msgNow == 'oneNow' && state.msg1 == null){
LOGDEBUG( "Message 1 is empty so nothing to say")
}
else if(state.msgNow == 'twoNow' && state.msg2 == null){
LOGDEBUG( "Message 2 is empty so nothing to say")
}
}
}
else if(state.appgo == false){
LOGDEBUG("$enableSwitch is off so cannot continue")
}

}

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
LOGDEBUG( " Day ok so can continue...")
 }       
 else {
LOGDEBUG( " Not today!")
 state.dayCheck = false
 }
 }
 
 // Delay between messages...

def startTimer1(){
state.timer1 = false
state.timeDelay = 60 * msgDelay
LOGDEBUG("Waiting for $state.timeDelay seconds before resetting timer1 to allow further messages")
runIn(state.timeDelay, resetTimer1)
}

def startTimer2(){
state.timer2 = false
state.timeDelay = 60 * msgDelay
LOGDEBUG( "Waiting for $state.timeDelay seconds before resetting timer2 to allow further messages")
runIn(state.timeDelay, resetTimer2)
}

def resetTimer1() {
state.timer1 = true
LOGDEBUG( "Timer 1 reset - Messages allowed")
}
def resetTimer2() {
state.timer2 = true
LOGDEBUG("Timer 2 reset - Messages allowed")
}


private getWeatherReport() {
	if (location.timeZone || zipCode) {
		def isMetric = location.temperatureScale == "C"
        def sb = new StringBuilder()
      	def weather = getWeatherFeature("forecast", zipCode)
        def inputMsg = message1
            sb << inputMsg 
			if (isMetric) {
        		sb << weather.forecast.txt_forecast.forecastday[0].fcttext_metric 
        	}
        	else {
          		sb << weather.forecast.txt_forecast.forecastday[0].fcttext
        	}
        
        
		def msg = sb.toString()
        msg = msg.replaceAll(/([0-9]+)C/,'$1 degrees')
        msg = msg.replaceAll(/([0-9]+)F/,'$1 degrees')
        compileMsg(msg)		
	}
	else {
		msg = "Please set the location of your hub with the SmartThings mobile app, or enter a zip code to receive weather forecasts."
		compileMsg(msg)
    }
}


private compileMsg(msg) {
	log.debug "msg = ${msg}"
	convertVariables(msg)
}


private convertVariables(msgIn){
    def msgOut = ""
    msgOut = msgIn.toUpperCase()
    
// Weather Variables    
    msgOut = msgOut.replace(" N ", " North ")
    msgOut = msgOut.replace(" S ", " South ")
    msgOut = msgOut.replace(" E ", " East ")
    msgOut = msgOut.replace(" W ", " West ")
    msgOut = msgOut.replace(" NNE ", " North Northeast ")
    msgOut = msgOut.replace(" NNW ", " North Northwest ")
    msgOut = msgOut.replace(" SSE ", " South Southeast ")
    msgOut = msgOut.replace(" SSW ", " South Southwest ")
    msgOut = msgOut.replace(" ENE ", " East Northeast ")
    msgOut = msgOut.replace(" ESE ", " East Southeast ")
    msgOut = msgOut.replace(" WNW ", " West Northeast ")
    msgOut = msgOut.replace(" WSW ", " West Southwest ")
    msgOut = msgOut.replace(" MPH", " Miles Per Hour")
    
// Day, Date, Time Variables    

	if (msgOut.contains("%TIME%")) {msgOut = msgOut.toUpperCase().replace('%TIME%', getTime(false,true))}  
	if (msgOut.contains("%DAY%")) {msgOut = msgOut.toUpperCase().replace('%DAY%', getDay() )}  
	if (msgOut.contains("%DATE%")) {msgOut = msgOut.toUpperCase().replace('%DATE%', getdate() )}  
   state.fullPhrase = msgOut
  log.trace "$state.fullPhrase"
  speakWeatherNow()
  
}

private getTime(includeSeconds, includeAmPm){
    def calendar = Calendar.getInstance()
	calendar.setTimeZone(location.timeZone)
	def timeHH = calendar.get(Calendar.HOUR) 
    def timemm = calendar.get(Calendar.MINUTE)
    def timess = calendar.get(Calendar.SECOND)
    def timeampm = calendar.get(Calendar.AM_PM) ? "pm" : "am"
    def timestring = "${timeHH}:${timemm}"
    if (includeSeconds) { timestring += ":${timess}" }
    if (includeAmPm) { timestring += " ${timeampm}" }
    log.trace "$timestring"
    return timestring
}

private getDay(){
	def df = new java.text.SimpleDateFormat("EEEE")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	def day = df.format(new Date())
    return day
}

private parseDate(date, epoch, type){
    def parseDate = ""
    if (epoch){
    	long longDate = Long.valueOf(epoch).longValue()
        parseDate = new Date(longDate).format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
    }
    else {
    	parseDate = date
    }
    new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", parseDate).format("${type}", timeZone(parseDate))
}
private getdate() {
    def month = parseDate("", now(), "MMMM")
    def year = parseDate("", now(), "yyyy")
    def dayNum = parseDate("", now(), "dd")
	
    log.debug "Date:  $dayNum $month $year"
         
    return dayNum + " " + " " + month + " " + year + " "
}



def speakWeatherNow(){
if(state.appgo == true){
LOGDEBUG("speakWeatherNow - Calling.. CheckTime")
checkTime()
LOGDEBUG("speakWeatherNow - Calling.. CheckDay")
checkDay()
LOGDEBUG("speakWeatherNow - Calling.. CheckVolume")
checkVolume()

LOGDEBUG("speakWeatherNow - state.appgo = $state.appgo - state.timeOK = $state.timeOK - state.dayCheck = $state.dayCheck - state.timer1 = $state.timer1 - state.timer2 = $state.timer2 - state.volume = $state.volume")

if (state.dayCheck == true && state.timeOK == true && state.presenceRestriction == true){
speaker.speak(state.fullPhrase)
	}
}
}


// App Version   *********************************************************************************
def setAppVersion(){
    state.appversion = "2.0.1"
}
