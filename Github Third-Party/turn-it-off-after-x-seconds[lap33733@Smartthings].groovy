/**
 *  Copyright 2015 SmartThings
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
 *  Power Allowance
 *
 *  Author: SmartThings
 */
definition(
    name: "Turn it off after x seconds",
    namespace: "smartthings",
    author: "Luis Pinto",
    description: "When a switch turns on, automatically turn it back off after a set number of seconds you specify.",
    category: "Green Living",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health & Wellness/health7-icn@2x.png"
)

preferences {
	section("When a switch turns on...") {
		input "theSwitch", "capability.switch"
	}
    section("Turn off how many seconds later?") {
        input "theSwitch2","capability.switch",title:"Switch to turn Off", required:true, multiple: true
		input "switchSHM", type: "bool", title: "Switch SHM", required: false
		input "secondsLater", "number", title: "How many seconds?"
		input "alsoTurnOn", type: "bool", title: "Also turn on?", required: true
	}
    section("Optionaly play a message") {
        input "stLanguage", "enum", title: "SmartThings Voice?", required: true, defaultValue: "en-US Salli", options: ["da-DK Naja","da-DK Mads","de-DE Marlene","de-DE Hans","en-US Salli","en-US Joey","en-AU Nicole","en-AU Russell","en-GB Amy","en-GB Brian","en-GB Emma","en-GB Gwyneth","en-GB Geraint","en-IN Raveena","en-US Chipmunk","en-US Eric","en-US Ivy","en-US Jennifer","en-US Justin","en-US Kendra","en-US Kimberly","es-ES Conchita","es-ES Enrique","es-US Penelope","es-US Miguel","fr-CA Chantal","fr-FR Celine","fr-FR Mathieu","is-IS Dora","is-IS Karl","it-IT Carla","it-IT Giorgio","nb-NO Liv","nl-NL Lotte","nl-NL Ruben","pl-PL Agnieszka","pl-PL Jacek","pl-PL Ewa","pl-PL Jan","pl-PL Maja","pt-BR Vitoria","pt-BR Ricardo","pt-PT Cristiano","pt-PT Ines","ro-RO Carmen","ru-RU Tatyana","ru-RU Maxim","sv-SE Astrid","tr-TR Filiz"]
        input "message","text",title:"Play this message", required:false, multiple: false
		input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: true
        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
        input "fromTime", "time", title: "From", required: false
        input "toTime", "time", title: "To", required: false
        input "onlyOnce", "bool", title: "Run Only Once a day", required: false
    	input "person", "capability.presenceSensor", title: "if is present", required: false
 	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(theSwitch, "switch.on", switchOnHandler, [filterEvents: false])
    schedule("24 00 * * * ?", resetHasRun)
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	subscribe(theSwitch, "switch.on", switchOnHandler, [filterEvents: false])
    
    unschedule()
    schedule("24 00 * * * ?", resetHasRun)
    
}

def switchOnHandler(evt) {
	log.debug "Switch ${theSwitch} turned: ${evt.value}"
	def delay = secondsLater
	log.debug "Turning off Switch ${theSwitch2} in ${secondsLater}"

    if (state.hasRunToday == null)
    	state.hasRunToday = false


    if (alsoTurnOn == true)
    {
    	theSwitch2.on()
		log.debug "Turning on switches"
    }
//    def shouldPlayMessage = true
    if (message) {
    	if (fromTime != null && toTime != null)
        {
            def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
            if (between) {
                playMessage()
            } else {
                log.debug "Not correct time"
            }
        } else if (onlyOnce == true && state.hasRunToday == false)
            playMessage()
        else if (onlyOnce == false) 
            playMessage()
        else
            log.debug "Did not run because hasRunToday=" + state.hasRunToday
//	if (person != null && person.currentPresence == "present")

    }

	runIn(delay, turnOffSwitch)
}

def resetHasRun() {
	state.hasRunToday = false
}

def playMessage (){
    log.debug "Playing message $message"
	if (onlyOnce == true) {
    	state.hasRunToday = true
    }
	state.sound = textToSpeech(message instanceof List ? message[0] : message, stLanguage.substring(6)) // not sure why this is (sometimes) needed)
    sonos.playTrackAndResume(state.sound.uri, volume)
//    sonos.playTrack(state.sound.uri, volume, 0)
}

def turnOffSwitch() {
	log.debug "Turning off Switch ${theSwitch2} now"
	if (switchSHM == true){
    	def status = location.currentState("alarmSystemStatus")?.value
		if (status == "off"){
		    sendLocationEvent(name: "alarmSystemStatus", value: "stay")
        } else {
		    sendLocationEvent(name: "alarmSystemStatus", value: "off")        
        }
    }
    theSwitch2.off()
    theSwitch2.off()
}