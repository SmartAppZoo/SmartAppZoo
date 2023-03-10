/**
 *  ****************  Schedule Voice Alert (If Sensor Open)  ****************
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm happy for you to use this without a donation (but, if you find it useful then it would be nice to get a 'shout out' on the forum!) -  @Cobra
 *
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
 *  If modifying this project, please keep the above header intact and add your comments/credits below -  @Cobra
 *
 *
 *
 *  Last Update: 29/09/2017
 *
 *  Changes:
 *
 * 
 *
 *
 *  
 *  V1.0.0 - POC
 *
 */
 
 
 
 
 
 
 
 
 
definition(
    name: "Schedule Voice Alert (If Sensor Open)",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Schedule a time to check if a contact sensor is open, speak a message if it is",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
	iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    )

preferences {
	section("") {
        paragraph " V1.0.0 "
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                  title: "Schedule Alert (If Sensor Open)",
                  required: false,
                  "This SmartApp was designed to warn me when I'd left the garden shed door open at night - Schedule a couple of times to check if a contact sensor is open, if it is then say something "
         }         

    section("") {
		input (name: "checkTime1", title: "1st Check - At what time?", type: "time",  required: false)
	}
    section("") {
		input (name: "checkTime2", title: "2nd Check - At what time?", type: "time",  required: false)
	}
    section("") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
	
	 section("Message Settings"){
		 input "speaker1", "capability.musicPlayer", title: "Choose a speaker", required: true, multiple: true, submitOnChange:true
		input "volume1", "number", title: "Speaker volume", description: "0-100%", defaultValue: 100, required: true
		input "message1", "text", title: "Message to speak", required: true
       
       
	}    
    section("Check if this contacts is 'Open'"){
		input "contact1", "capability.contactSensor", title: "Door/Windows Contact", required: true, multiple: false
		
	}
    section("Logging"){
            input "debugmode", "bool", title: "Enable logging", required: true, defaultValue: false
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
	setAppVersion()
    logCheck()
LOGINFO( "Initialised with settings: ${settings}")
state.currS1 = 'closed'


if(checkTime1){
     schedule(checkTime1, checkNow)
     subscribe(contact1, "contact", contactHandler)
     }
if(checkTime2){
     schedule(checkTime2, checkNow)
     subscribe(contact1, "contact", contactHandler)
      }
}


def contactHandler (evt) {
 state.currS1 = evt.value 
  LOGINFO( "$contact1 = $evt.value")
						 }


def checkNow (evt) {
	
LOGINFO( "Checking now... $contact1 is $state.currS1")
 def df = new java.text.SimpleDateFormat("EEEE")
   df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the input 'days', contain today?
    def dayCheck = days.contains(day)
    if (dayCheck) {
    
LOGINFO( "Scheduled for operation today")
    	if (state.currS1 != "closed"){ 
        speakNow()
  }
        if (state.currS1 == "closed"){ 
LOGINFO( " $contact1 is  $state.currS1"  )     
        }
               
    			  }
    else {
 LOGINFO("Not scheduled for today!")
		 } 
 				   }




def speakNow(){
LOGINFO("speakNow...")
state.speakerVolume = volume1 as int
state.msg1 = message1
   
	speaker1.setLevel(state.speakerVolume)
	LOGINFO("Speaking now...")
	speaker1.speak(state.msg1)
   
 } 
 

def logCheck(){
state.checkLog = debugmode
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
    state.appversion = "1.0.0"
}