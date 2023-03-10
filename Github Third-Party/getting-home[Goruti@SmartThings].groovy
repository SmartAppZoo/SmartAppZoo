/**
 *  Getting Home
 *
 *  Copyright 2016 Diego
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
 */
definition(
    name: "Getting Home",
    namespace: "DiegoAntonino",
    author: "Diego",
    description: "If I arrive home after sunset and mode is \"away\", turn on a light and set mode to Home.\r\nif  I arrive home before sunset and mode is \"away\", set mode to Home.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-MindYourHome@2x.png")


preferences {
	page(name: "mainPage", title: "Play a message on your Speaker when something happens", install: true, uninstall: true)
	page(name: "chooseTrack", title: "Select a song or station")
}


def mainPage() {
	dynamicPage(name: "mainPage") {
        section("When Someone arrive..."){
            input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
        }
        section("Turn On switch..."){
            input "switch1", "capability.switch", multiple: true
        }
        section("Turn On TV..."){
            input "tv", "capability.switch", multiple: true
        }
        section {
            input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: true
        }
        section("More options", hideable: true, hidden: true) {
            href "chooseTrack", title: "Or play this music or radio station", description: song ? state.selectedSong?.station : "Tap to set", state: song ? "complete" : "incomplete"
            input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
            input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
        }
        section ("Change home to this mode"){
            input "ModeToSet", "mode", title: "select a mode"
        }
        section("Dimmer to make bright when Mode is Night") {
            input "dimmerNight", "capability.switchLevel", title: "Which dimmer?", required: false
            input "brightness", "number", title: "Light Level", required: false
        }
        section("Selects Night Modes...") {
            input "NightMode", "mode", required: false, title: "Night Mode?", multiple: true
        }
        
        }
}

def chooseTrack() {
	dynamicPage(name: "chooseTrack") {
		section{
			input "song","enum",title:"Play this track", required:true, multiple: false, options: songOptions()
		}
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
    subscribeToEvents()
}

def subscribeToEvents(){
    subscribe(presence1, "presence", presenceHandler)
    if (song) {
        saveSelectedSong()
    }
}

def presenceHandler(evt) {
	def now = new Date()
	def sunTime = getSunriseAndSunset(sunsetOffset: "-00:30")
	def curMode = location.mode

	log.debug "presenceHandler $evt.name: $evt.value"

	def current = presence1.currentValue("presence")
	log.debug current
	def presenceValue = presence1?.find{it.currentPresence == "present"}
	log.debug presenceValue
    
    if(presenceValue) {
    	if (curMode == "Away") {
        	sendLocationEvent(name: "alarmSystemStatus", value: "off") //  Set alarm status to "Disarm" state.
        	setHomeStatus(curMode, ModeToSet) // Set Home to Home Mode
            if(now > sunTime.sunset) {
            	switch1.on()	//Turn on the switch
            	log.debug "Welcome home at sunset!"
			}
            tv.on() //Turn TV on
            
    	} else if (NightMode?.find{it == curMode}) { // Home is in Night/TV or Siesta Mode
            if(now < sunTime.sunrise) {
            	dimmerNight.setLevel(brightness)
    			log.debug "Welcome home at night!"
			}
    		
    	}  
	}
}

def setHomeStatus(currentMode,ModeToBeSet) {
    if (currentMode != ModeToBeSet) {
        if (location.modes?.find{it.name == ModeToBeSet}) {
            setLocationMode(ModeToBeSet)
            log.debug "setHomeTo: $ModeToBeSet"
        }  
        else {
            log.warn "Tried to change to undefined mode $ModeToBeSet"
        }
    }
}

private songOptions() {

    // Make sure current selection is in the set

    def options = new LinkedHashSet()
    if (state.selectedSong?.station) {
        options << state.selectedSong.station
    }
    else if (state.selectedSong?.description) {
        // TODO - Remove eventually? 'description' for backward compatibility
        options << state.selectedSong.description
    }

    // Query for recent tracks
    def states = sonos.statesSince("trackData", new Date() -5, [max:30])
    def dataMaps = states.collect{it.jsonValue}
    options.addAll(dataMaps.collect{it.station})

    log.trace "${options.size()} songs in list"
    options.take(20) as List
}

private saveSelectedSong() {
	try {
		def thisSong = song
		log.info "Looking for $thisSong"
		def songs = sonos.statesSince("trackData", new Date(0), [max:30]).collect{it.jsonValue}
		log.info "Searching ${songs.size()} records"

		def data = songs.find {s -> s.station == thisSong}
		log.info "Found ${data?.station}"
		if (data) {
			state.selectedSong = data
			log.debug "Selected song = $state.selectedSong"
		}
		else if (song == state.selectedSong?.station) {
			log.debug "Selected existing entry '$song', which is no longer in the last 20 list"
		}
		else {
			log.warn "Selected song '$song' not found"
		}
	}
	catch (Throwable t) {
		log.error t
	}
}