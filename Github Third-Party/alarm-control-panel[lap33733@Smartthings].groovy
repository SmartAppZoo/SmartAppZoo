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
 *  Arm/disarm SHM with a button after x Seconds
 *
 *  Author: Luis Pinto
 */
definition(
    name: "Alarm control Panel",
    namespace: "smartthings",
    author: "Luis Pinto",
    description: "Change SHM with a switch button after x seconds",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png"
)

preferences {
	section("Control Switch") {
		input "theSwitch", "capability.switch",title:"Select a switch", required:false, multiple: true
		input "secondsLater", "number", title: "Arm how many seconds later?", required:true
		input "siren", "capability.alarm", title: "On this Siren", required: false
	}
    section("Options for Arm") {
        input "stLanguageOn", "enum", title: "SmartThings Voice?", required: true, defaultValue: "en-US Salli", options: ["da-DK Naja","da-DK Mads","de-DE Marlene","de-DE Hans","en-US Salli","en-US Joey","en-AU Nicole","en-AU Russell","en-GB Amy","en-GB Brian","en-GB Emma","en-GB Gwyneth","en-GB Geraint","en-IN Raveena","en-US Chipmunk","en-US Eric","en-US Ivy","en-US Jennifer","en-US Justin","en-US Kendra","en-US Kimberly","es-ES Conchita","es-ES Enrique","es-US Penelope","es-US Miguel","fr-CA Chantal","fr-FR Celine","fr-FR Mathieu","is-IS Dora","is-IS Karl","it-IT Carla","it-IT Giorgio","nb-NO Liv","nl-NL Lotte","nl-NL Ruben","pl-PL Agnieszka","pl-PL Jacek","pl-PL Ewa","pl-PL Jan","pl-PL Maja","pt-BR Vitoria","pt-BR Ricardo","pt-PT Cristiano","pt-PT Ines","ro-RO Carmen","ru-RU Tatyana","ru-RU Maxim","sv-SE Astrid","tr-TR Filiz"]
        input "messageOn","text",title:"Play this message when switch is activated", required:false, multiple: false
        input "messageOnURL","text",title:"Play this sound when switch is activated", required:false, multiple: false
        input "messageOnNow","text",title:"Play this message when active", required:false, multiple: false
		input "sonosOn", "capability.musicPlayer", title: "On this Speaker player", required: false
        input "volumeOn", "number", title: "Temporarily change volume", description: "0-100%", required: false
        input "sendPushOn", "bool", required: false, title: "Send Push Notification when switch is activated?"
        input "sendPushOnNow", "bool", required: false, title: "Send Push Notification when alarm is activated?"
        input "modeForArmed", "mode", title: "Change a mode", multiple: false, required:false
        input "sendSirenOnNow", "bool", required: false, title: "Send Siren Notification when alarm is activated?"
	}
    section("Options for disarm") {
        input "stLanguageOff", "enum", title: "SmartThings Voice?", required: true, defaultValue: "en-US Salli", options: ["da-DK Naja","da-DK Mads","de-DE Marlene","de-DE Hans","en-US Salli","en-US Joey","en-AU Nicole","en-AU Russell","en-GB Amy","en-GB Brian","en-GB Emma","en-GB Gwyneth","en-GB Geraint","en-IN Raveena","en-US Chipmunk","en-US Eric","en-US Ivy","en-US Jennifer","en-US Justin","en-US Kendra","en-US Kimberly","es-ES Conchita","es-ES Enrique","es-US Penelope","es-US Miguel","fr-CA Chantal","fr-FR Celine","fr-FR Mathieu","is-IS Dora","is-IS Karl","it-IT Carla","it-IT Giorgio","nb-NO Liv","nl-NL Lotte","nl-NL Ruben","pl-PL Agnieszka","pl-PL Jacek","pl-PL Ewa","pl-PL Jan","pl-PL Maja","pt-BR Vitoria","pt-BR Ricardo","pt-PT Cristiano","pt-PT Ines","ro-RO Carmen","ru-RU Tatyana","ru-RU Maxim","sv-SE Astrid","tr-TR Filiz"]
        input "messageOff","text",title:"Play this message", required:false, multiple: false
        input "messageOffURL","text",title:"Play this sound when switch is activated", required:false, multiple: false
		input "sonosOff", "capability.musicPlayer", title: "On this Speaker player", required: false
        input "volumeOff", "number", title: "Temporarily change volume", description: "0-100%", required: false
        input "sendPushOff", "bool", required: false, title: "Send Push Notification also?"
        input "modeForDisarmed", "mode", title: "Change to mode", multiple: false, required:false
        input "sendSirenOffNow", "bool", required: false, title: "Send Siren Notification when alarm is deactivated?"
	}
   	section("Real Contacts"){
		input "contacts", "capability.contactSensor", multiple: true, required: true
        input(name: "contactState", type: "enum", title: "State", options: ["Open","Closed"])
	}   
	section("Virtual Contact"){
		input "contact2", "capability.contactSensor", multiple: true, required: true
        input "secondsLater", "number", title: "How many seconds?"
	}
    section("Optionaly play a message if SHM armed") {
        input "stLanguage", "enum", title: "SmartThings Voice?", required: true, defaultValue: "en-US Salli", options: ["da-DK Naja","da-DK Mads","de-DE Marlene","de-DE Hans","en-US Salli","en-US Joey","en-AU Nicole","en-AU Russell","en-GB Amy","en-GB Brian","en-GB Emma","en-GB Gwyneth","en-GB Geraint","en-IN Raveena","en-US Chipmunk","en-US Eric","en-US Ivy","en-US Jennifer","en-US Justin","en-US Kendra","en-US Kimberly","es-ES Conchita","es-ES Enrique","es-US Penelope","es-US Miguel","fr-CA Chantal","fr-FR Celine","fr-FR Mathieu","is-IS Dora","is-IS Karl","it-IT Carla","it-IT Giorgio","nb-NO Liv","nl-NL Lotte","nl-NL Ruben","pl-PL Agnieszka","pl-PL Jacek","pl-PL Ewa","pl-PL Jan","pl-PL Maja","pt-BR Vitoria","pt-BR Ricardo","pt-PT Cristiano","pt-PT Ines","ro-RO Carmen","ru-RU Tatyana","ru-RU Maxim","sv-SE Astrid","tr-TR Filiz"]
        input "message","text",title:"Play this message", required:false, multiple: false
		input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: false
        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
        input "sendSirenArmedNotification", "bool", required: false, title: "Send Siren Notification if alarm is activated?"
	} 
}

def subscribeToEvents() {
    if(theSwitch)
    {
		subscribe(theSwitch, "switch.on", switchOnHandler, [filterEvents: false])
		subscribe(theSwitch, "switch.off", switchOffHandler, [filterEvents: false])
	}
    if (contactState == "Open"){
		subscribe(contacts, "contact.open", contactHandler)
    }
    else{
		subscribe(contacts, "contact.closed", contactHandler)
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribeToEvents();
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    subscribeToEvents();
}

def switchOffHandler(evt) {
//	log.debug "Switch ${theSwitch} turned: ${evt.value}"
    log.debug "Alarm was turned OFF"

//    def status = location.currentState("alarmSystemStatus")?.value
//	log.debug "SHM Status is ${status}"
    
	if (evt.value == "off"){
        if (siren && sendSirenOffNow)
        {
        	siren.BeepCode()
        }
		if (sonosOff && messageOff) {
        	if (sendPushOff) {
        		sendPush(messageOff)
        	}
			log.debug "Playing message Off"
            
            state.sound = textToSpeech(messageOff instanceof List ? messageOff[0] : messageOff, stLanguageOff.substring(6))
	        sonosOff.playTrack(state.sound.uri, volumeOff)

//			state.sound = textToSpeech(messageOff instanceof List ? messageOff[0] : messageOff)
//			sonosOff.playTrack(state.sound.uri, state.sound.duration, volumeOff)
		}
        if (messageOffURL) {
			state.sound = [uri: messageOffURL, duration: "10"]
			sonosOff.playTrack(state.sound.uri, state.sound.duration, volumeOff)        
		}
		sendLocationEvent(name: "alarmSystemStatus", value: "off")
        if (modeForDisarmed)
			location.setMode(modeForDisarmed)
	}
}

def switchOnHandler(evt) {
//	log.debug "Switch ${theSwitch} turned: ${evt.value}"
	def delay = secondsLater
    log.debug "Alarm was turned ON"
//	def status = location.currentState("alarmSystemStatus")?.value
//	log.debug "SHM status is ${status}"
    
	if (evt.value == "on") {
    	if (siren && sendSirenOnNow)
        {
        	siren.BeepCode()
        }
		if (sonosOn && messageOn) {
        	if (sendPushOn) {
        		sendPush(messageOn)
        	}
        	log.debug "Playing message On"
//			state.sound = textToSpeech(messageOn instanceof List ? messageOn[0] : messageOn)
//			sonosOn.playTrack(state.sound.uri, state.sound.duration, volumeOn)

			state.sound = textToSpeech(messageOn instanceof List ? messageOn[0] : messageOn, stLanguageOn.substring(6))
	        .playTrack(state.sound.uri, volumeOn)
		}
        if (messageOnURL) {
			state.sound = [uri: messageOnURL, duration: "10"]
			sonosOff.playTrack(state.sound.uri, state.sound.duration, volumeOn)        
		}
		state.hasInformOpenElements = false
		runIn(delay, turnOnAlarm)
	}
}

def turnOnAlarm() {
 	def currentState = theSwitch[0].currentState("switch")?.value
// 	log.debug "Keypad Switch status is ${currentState}"
    if (currentState == "on") {
	    log.debug "Going to test if I can put alarm ON"

        def delay = secondsLater
        def isAnyOpen = false
        contacts.each {
//            log.debug "${it.name} status is ${it.contactState.value}"
            if (it.contactState.value == "open") {
                isAnyOpen = true
            }
        }
    
        if (isAnyOpen) {
            log.debug "There are still some open triggers, delaying alarm"
            if (state.hasInformOpenElements == false && siren && sendSirenOnNow)
            {
                siren.BeepCode()
                siren.BeepCode()
            }
        	if (state.hasInformOpenElements == false && sendPushOn) {
        		sendPush("There are sensors opened can't turn alarm on")
        	}

			state.hasInformOpenElements = true

            runIn(delay, turnOnAlarm)
            return
        }
        
        if (siren && sendSirenOnNow)
        {
            siren.BeepCode()
        }

        if (sonosOn && messageOnNow) {
            if (sendPushOnNow) {
                sendPush(messageOnNow)
            }
            log.debug "Playing message On Now"

            state.sound = textToSpeech(messageOnNow instanceof List ? messageOnNow[0] : messageOnNow, stLanguageOn.substring(6))
            sonosOn.playTrack(state.sound.uri, volumeOn)

    //		state.sound = textToSpeech(messageOnNow instanceof List ? messageOnNow[0] : messageOnNow)
    //        sonosOn.playTrack(state.sound.uri, state.sound.duration, volumeOn)
        }

        sendLocationEvent(name: "alarmSystemStatus", value: "stay")
        if (modeForArmed)
            location.setMode(modeForArmed)
    }
}

def contactHandler(evt) {
	def delay = secondsLater
	def status = location.currentState("alarmSystemStatus")?.value

    if (siren && sendSirenArmedNotification && status != "off")
    {
        siren.BeepCode()
    }
    if (sonos && message && status != "off") {
		log.debug "Playing message"
        state.sound = textToSpeech(message instanceof List ? message[0] : message, stLanguage.substring(6))
        sonos.playTrackAndResume(state.sound.uri, volume)
    }

	runIn(delay, turnOffSwitch)
}

def turnOffSwitch() {
	contact2.open()
	contact2.close()
}