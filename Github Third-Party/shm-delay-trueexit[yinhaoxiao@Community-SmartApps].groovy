/*
 *  SHM Delay TrueDelay 
 *  Functions: Create a true delay for SHM by executing a dummy routine that does nothing,
 			then a real routine in nn seconds
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
 * 	Dec 17, 2017    v1.0.0	Add optional speech at exit delay using code from Keypad_ExitDelay_Talker, 
 *
 * 	Sep 14, 2017    v0.0.1	Add some logic to make it "smarter", selecting routine defaults when possible,
 * 					and removing monitored routine from execution routines list, execution unchanged
 * 	Sep 13, 2017    v0.0.0	create
 *
 */

definition(
    name: "SHM Delay TrueExit",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Create a real exit delay using Smarthome Routines",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

preferences {
	page(name: "pageOne")
}


def pageOne(error_msg)
	{
	dynamicPage(name: "pageOne", title: "SHM Delay TrueExit", install: true, uninstall: true)
		{
		section
			{
 			def actions = location.helloHome?.getPhrases()*.label
			def set_default=false
			if (actions) 
				{
				actions.sort()
				actions.each
					{
					if (it.matches("(.*)(?i)delay(.*)"))
						{set_default=it}
					}
				if (set_default)
					{input "monitor_routine", "enum", title: "Select a routine to monitor for execution", options: actions, submitOnChange: true, defaultValue: set_default}
				else
					{input "monitor_routine", "enum", title: "Select a routine to monitor for execution", options: actions, submitOnChange: true}
  				}
  			}	
		section 
			{
			input "theexitdelay", "number", required: true, range: "10..120", defaultValue: 30,
				title: "How many seconds to wait when monitored routine executes from 10 to 120"
			}
		section
			{
 			def actions = location.helloHome?.getPhrases()*.label
			if (actions) 
				{
				def set_default2=false
				actions.sort()
//				log.debug "actions ${actions}"
				def new_actions=[]
				actions.each		//fails when not defined as multiple contacts
					{
//					log.debug "${it.value} ${it} ${monitor_routine}"
					if (it != monitor_routine)	//if monitor_routine not defined it is set to null for compare
						{
						new_actions+=it
						if (it=="Goodbye!")
							{set_default2=true}
						}
					}
//				log.debug "default is ${set_default2}"	
				if (set_default2)
					{input "execute_routine", "enum", title: "Then execute this routine", options: new_actions, defaultValue: "Goodbye!"}
				else
					{input "execute_routine", "enum", title: "Then execute this routine", options: new_actions}
				}	
  			}	
	    section("Optional Speech Settings") {
			input "theMsg", "string", required: true, title: "The message", 
				defaultValue: "Smart Home Monitor is arming in 30 seconds. Please exit the facility"
			input "theTTS", "capability.speechSynthesis", required: false, multiple: true,
				title: "LanNouncer/DLNA TTS Devices"
			input "theSpeakers", "capability.audioNotification", required: false, multiple: true,
				title: "Speaker Devices?"
			input "theVolume", "number", required: true, range: "1..100", defaultValue: 40,
				title: "Speaker Volume Level from 1 to 100"
			}	

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

def initialize() 
	{
	subscribe(location, "routineExecuted", routineHandler)
	}


def routineHandler(evt)
	{
//	log.debug "routineExecuted: $evt"

// 	name will be "routineExecuted"
//	log.debug "evt name: ${evt.name}"

//	 value will be the ID of the SmartApp that created this event
//	log.debug "evt value: ${evt.value}"

// 	displayName will be the name of the routine
// 	e.g., "I'm Back!" or "Goodbye!"
//	log.debug "evt displayName: ${evt.displayName}"

// 	descriptionText will be the name of the routine, followed by the action
// 	e.g., "I'm Back! was executed" or "Goodbye! was executed"
//	log.debug "evt descriptionText: ${evt.descriptionText}"
	if (evt.name == "routineExecuted" && evt.displayName == monitor_routine)
		{
		log.debug "triggering a delay routine execute"
		def now = new Date()
		def runTime = new Date(now.getTime() + (theexitdelay * 1000))
		runOnce(runTime, executeRoutine) 
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

def executeRoutine()
	{
//	execute the target routine
	log.debug "firing target routine ${execute_routine}"
	location.helloHome.execute(execute_routine)
	}