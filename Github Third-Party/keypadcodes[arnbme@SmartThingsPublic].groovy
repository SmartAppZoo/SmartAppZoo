/**
 *  KeypadCodes
 *
 *  Copyright 2017 Arn Burkhoff
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
 *  May 08, 2019 fix for update DTH needed from undocumented system change killing keypads	
 *  Mar 23, 2019 change chimes for Fully TTS device, change delay from 1800  to 1000 ms	
 *  Dec 18, 2018 reinstate keypadcodes, routined wiped by ST, just easier this way	
 *  Jul 25, 2018 kill 1111 and 0000. use routines in SHM Delay User Profile, exit delay done by SHM Delay	
 *  Jul 20, 2018 allow for multiple keypads
 *  Dec 10, 2017 comment out talker code execution. Released in Keypad_ExitDelay_Talker
 *	Dec 04, 2017 add code supporting LanNouncer TTS Chime and text for exitDelays by subscribing to keypad armMode event.
 *					other modes and entry delay handled by Big Talker
 */
definition(
    name: "KeypadCodes",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Use keypad pin codes to control system lighting and other things",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Keypads to monitor") {
        input "thekeypad", "device.CentraliteKeypad", required: true, multiple: true, title: "Keypads?"
        }
    section("Living Room Light") {
        input "theLRlight", "capability.switch", required: true, title: "Living Room Light?"
    	}
    section("Front Door Light") {
        input "theFDlight", "capability.switch", required: true, title: "Front Door Light?"
    	}
    section("Garage Door") {
        input "theGarageDoor", "capability.garageDoorControl", required: true, title: "Garage Door?"
    	}
    section("LanNouncer TTS Device for Exit Delay talk") {
        input "theTTS", "capability.speechSynthesis", required: true, title: "LanNouncer/DLNA?"
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
    subscribe (thekeypad, "codeEntered", buttonHandler)
//	if (theTTS)
//	    subscribe (thekeypad, "armMode", ExitDelayHandler)
	}

def buttonHandler(evt)
	{
	def thePin=evt.value.substring(0,4) as String
	def theMode=evt.value.substring(5,6)
	log.debug "buttonHandler $evt value: $evt.value data: $evt.data thePin:$thePin theMode:$theMode"
	
//	log.debug "buttonHandler $evt value: $evt.value data: $evt.data"
//	def datacodes = new groovy.json.JsonSlurper().parseText(evt.data)
//	log.debug datacodes.armMode
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	if (thePin=="0000")
		{
		def status=theLRlight.currentState("switch").value
		if (status=="on")
			{
			theLRlight.off()
			}
		else
			{
			theLRlight.on()
			}
		}
	else
	if (thePin=="1111")
		{
		def status=theFDlight.currentState("switch").value
		if (status=="on")
			{
			theFDlight.off()
			}
		else
			{
			theFDlight.on()
			}
		}
	else
	if (thePin == "3333" && alarmstatus == "off")
		{
		theGarageDoor.open()
		GarageOpenTalk()
		}
	else
	if (thePin=="4444")
		{
		theGarageDoor.close()
		GarageCloseTalk()
		}
	}
	
def ExitDelayHandler(evt)
	{
//	log.debug("ExitDelay event: ${evt.value}")
	if (evt.value=="exitDelay")
		{
//		theTTS.speak("@|ALARM=CHIME")
		theTTS.chime()
        theTTS.speak("Smart Home Monitor is arming in 30 seconds. Please exit the facility",[delay: 1000])
        }
	}
def GarageOpenTalk()
	{
//	log.debug("EntryDelay event: ${evt.value}")
//	theTTS.speak("@|ALARM=CHIME")
	theTTS.chime()
    theTTS.speak("Requested Garage Door Open",[delay: 1000])
	}
def GarageCloseTalk()
	{
//	log.debug("EntryDelay event: ${evt.value}")
//	theTTS.speak("@|ALARM=CHIME")
	theTTS.chime()
    theTTS.speak("Requested Garage Door Close",[delay: 1000])
	}
	