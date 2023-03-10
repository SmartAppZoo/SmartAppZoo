/**
 *  ****************  Power Speak  ****************
 *
 *  Design Usage:
 *  This was designed to be used with a power monitor to decide if a light is on or off then speak a message
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
 *  Last Update: 26/09/2017
 *
 *  Changes:
 *
 * 
 *
 *
 *  V1.1.0 - added switchable logging
 *  V1.0.0 - POC
 *
 */



definition(
    name: "Power Speak",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Turn on a switch and speaks a message if power drawn goes above a defined level",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
)

preferences {

	section {
    
    paragraph "V1.1.0"
     paragraph image:  "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
       	title: "Power Speak",
        required: false, 
    	"Turn on a switch and speak a message if power drawn goes above a defined level"
    
    
    
     
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        input(name: "aboveThreshold", type: "number", title: "Reports above...", required: true)
		input(name: "switch1", type: "capability.switch", title: "Turn on this 'Indicator Switch' - Optional", required: false, multiple: false, description: null)
        input "speaker1", "capability.musicPlayer", title: "Choose a speaker", required: true, multiple: true, submitOnChange:true
		input "volume1", "number", title: "Speaker volume", description: "0-100%", defaultValue: 100, required: true
		input "message1", "text", title: "Message to speak", required: true
        input "msgDelay", "number", title: "Delay between messages (Enter 0 for no delay)", defaultValue: '0', description: "Minutes", required: true
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
state.timer1 = true
log.debug "Initialised with settings: ${settings}"
	subscribe(meter, "power", meterHandler)
   subscribe(switch1, "switch", switch1Handler)
  
   }
 



def switch1Handler(evt){
state.currS1 = evt.value
LOGDEBUG("$switch1 is $state.currS1")
}


def meterHandler(evt) {
    state.meterValue = evt.value as double
    def currTime = new Date()
    LOGINFO("$meter shows $state.meterValue Watts")
   
   
   	checkNow()  

}

def checkNow(){
state.speakerVolume = volume1 as int
state.msg1 = message1
LOGDEBUG( "checkNow -  Power is: $state.meterValue")
    state.aboveValue = aboveThreshold as int
    if (state.meterValue > state.aboveValue) {
    LOGDEBUG( "Message is: '$state.msg1' ")
    speakNow()
       }
      else  if (state.meterValue < state.aboveValue) {
       
       if(switch1 != null){
       LOGINFO("Switching off $switch1")
       switch1.off()
      }
      }
  }

 
def speakNow(){
 if(switch1 != null){
	LOGINFO("Switching on $switch1")
     switch1.on()
    }
    if ( state.timer1 == true){
	speaker1.setLevel(state.speakerVolume)
	LOGINFO("Speaking now...")
	speaker1.speak(state.msg1)
   	startTimer1()  
 } 
 
}

def startTimer1(){
state.timer1 = false
state.timeDelay = 60 * msgDelay
LOGDEBUG("Waiting for $state.timeDelay seconds before resetting timer1 to allow further messages")
runIn(state.timeDelay, resetTimer1)
}

def resetTimer1() {
state.timer1 = true
LOGDEBUG( "Timer 1 reset - Messages allowed")
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
    	log.error("LOGTINFO unable to output requested data!")
    }
}


def setAppVersion(){
    state.appversion = "1.1.0"
}