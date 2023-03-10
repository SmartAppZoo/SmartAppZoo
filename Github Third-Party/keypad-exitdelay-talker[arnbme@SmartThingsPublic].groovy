/**
 *  Keypad_ExitDelay_Talker
 *  Supplements Big Talker adding speech when Keypad is set into Exit Delay Mode
 *		For LanNouncer Device: Chime, TTS text, Chime
 *		For speakers (such as Sonos)  TTS text
 *	Supports multiple keypads, LanNouncer devices and speakers
 *	When keypads use differant delay times, install multiple copies of this code
 *	When speakers need different volumes, install multiple copies of this code
 *
 *	Requires: 
 *		1. a keypad DTH that issues command sendEvent([name: "armMode".....
 *		2. a smartapp issuing commands that execute the command in #1 above suchas: EThayer's Lock Manager
 *			Usually this is done with command SetExitDelay. If your keypad device beeps at exit delay
 *			it should work.
 *		3. one output device suchas: LanNouncer or a Speaker	
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
 *	Mar 10, 2018 add entry delay mesage 
 *	Dec 08, 2017 Create 
 */
definition(
    name: "Keypad_ExitDelay_Talker",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Speak during Exit Delay. Used in conjunction with Big Talker or similar apps",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("The Exit Delay Message Settings") {
		input "theMsg", "string", required: true, title: "Exit message", 
			defaultValue: "Smart Home Monitor is arming in 30 seconds. Please exit the facility"
		input "theEntryMsg", "string", required: false, title: "Entry message", 
			defaultValue: "Please enter your pin on the keypad"
		input "thekeypads", "capability.button", required: true, multiple: true,
			title: "Keypads to monitor"
        input "theTTS", "capability.speechSynthesis", required: false, multiple: true,
        	title: "LanNouncer/DLNA TTS Devices"
        input "theSpeakers", "capability.audioNotification", required: false, multiple: true,
        	title: "Speaker Devices?"
		input "theVolume", "number", required: true, range: "1..100", defaultValue: 40,
			title: "Speaker Volume Level from 1 to 100"
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
	subscribe (thekeypads, "armMode", TalkerHandler)
	}


def TalkerHandler(evt)
	{
//	log.debug("TalkerHandler entered, event: ${evt.value}")
	if (evt.value=="entryDelay" && theEntryMsg>"")
		{
		if (theTTS)
			{
			theTTS.speak("@|ALARM=CHIME")
			theTTS.speak(theEntryMsg,[delay: 1800])
			theTTS.speak("@|ALARM=CHIME", [delay: 5000])
			}
		if (theSpeakers)
			{
			theSpeakers.playTextAndResume(theEntryMsg,theVolume)
			}
		}
	else
	if (evt.value=="exitDelay")
		{
		if (theTTS)
			{
			theTTS.speak("@|ALARM=CHIME")
			theTTS.speak(theMsg,[delay: 1800])
			theTTS.speak("@|ALARM=CHIME", [delay: 8000])
			}
		if (theSpeakers)
			{
			theSpeakers.playTextAndResume(theMsg,theVolume)
			}
		}
	}