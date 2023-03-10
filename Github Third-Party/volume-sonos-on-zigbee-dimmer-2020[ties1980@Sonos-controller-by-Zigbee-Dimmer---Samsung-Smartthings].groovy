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
 *  The Big Switch
 *
 *  Author: SmartThings
 *
 *  Date: 2013-05-01
 */
definition(
	name: "Volume Sonos on Zigbee dimmer 2020",
	namespace: "smartthings",
	author: "Matthijs van Schendelen",
	description: "Volume",
	category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png"
)



preferences {
	section("When this switch is turned on, off or dimmed") {
		input "master", "capability.switch", title: "Where?"
        
	}
	section("Turn on or off all of these switches as well") {
		input "switches", "capability.switch", multiple: true, required: false
	}
    
     section("Choose Sonos player") {
     
	    //input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: true
        input "sonos", "capability.mediaPlayback", title: "On this speaker player", required: true
        log.debug "Status ophalen Sonos $sonos"
        
    }   
    section("More options", hideable: true, hidden: true) {
			input "volume", "number", title: "Set the volume", description: "0-100%", required: false
	}
	/*
    section("And turn off but not on all of these switches") {
		input "offSwitches", "capability.switch", multiple: true, required: false
	}
	section("And turn on but not off all of these switches") {
		input "onSwitches", "capability.switch", multiple: true, required: false
	}
    */
	section("And Dim these switches") {
		input "dimSwitches", "capability.switchLevel", multiple: true, required: false
	}    
}


def installed()
{   
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	subscribe(master, "level", dimHandler)   
    //subscribe(sonos, "level", sonosHandler)
}

def updated()
{
	unsubscribe()
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	subscribe(master, "level", dimHandler)  
//    subscribe(sonos, "level", sonosHandler)
}

def logHandler(evt) {
	log.debug evt.value
}

def timerTask = null

def sonosHandler(evt) {
    log.debug("Set level of switch to {$evt.value}")

	unschedule()
     runIn(3, syncSwitchWithSonos(evt.value.toInteger()))

}

def syncSwitchWithSonos(int volume) {
	log.debug("Set level of sonos to ${volume}")
	master.setLevel(volume)
}

def onHandler(evt) {
	log.debug("Switch on")
	log.debug onSwitches()
	//onSwitches()?.on()
}

def offHandler(evt) {
	log.debug("Switch off") 
	log.debug offSwitches()
    //sonos.nextTrack()
	//offSwitches()?.off()
}

def dimHandler(evt) {
	log.debug "Dim level: $evt.value"
    log.debug "Sonos level: $sonos.currentLevel"

 	sonos.setVolume(evt.value.toInteger())

}

private onSwitches() {
	if(switches && onSwitches) { switches + onSwitches }
	else if(switches) { switches }
	else { onSwitches }
}

private offSwitches() {
	if(switches && offSwitches) { switches + offSwitches }
	else if(switches) { switches }
	else { offSwitches }
}