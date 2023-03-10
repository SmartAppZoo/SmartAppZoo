/**
 * MirrorSwitch
 *
 *  Copyright 2016 Michael Lasevich
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
		name: "MirrorSwitch",
		namespace: "com.legrig",
		author: "Michael Lasevich",
		description: "Mirror Switch",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
		)


preferences {
	section("Switch Linkage") {
		input "realswitch", "capability.switch", title: "Switch To Mirror From...", required: true
		input "mirrorswitches", "capability.switch", title: "Switch(es) To Mirror To...", multiple: true
	}
}

private subscribe_to_events(){
	log.debug("Subscribing...")
	subscribe(realswitch, "switch", switchEventHandler)
	subscribe(realswitch, "level", levelEventHandler)
	//    subscribe(realswitch, "switch.off", switchOffHandler)
	//    subscribe(realswitch, "switch.setLevel", switchLevelHandler)
}

def installed() {
	log.debug "Installed"
	subscribe_to_events()
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	subscribe_to_events()
	//state.isActive = false
}



def turnOn() {
	log.debug "turnOn()"
	def level = realswitch.currentLevel
	mirrorswitches.each{
		log.debug("Turning on ${it.displayName}")
		it.on()
		log.debug("Setting ${it.displayName} level from ${it.currentLevel} to ${level}")
		it.setLevel(level)
	}
}

def turnOff(){
	log.debug "turnOn()"
	mirrorswitches.each{
		log.debug("Turning off ${it.displayName}")
		it.off()
	}
}

def setLevel(level){
	log.debug "Level set to: ${level}"
	mirrorswitches.each{
		if (realswitch.currentValue("switch") == "on"){
			if (it.currentValue("switch") != "on"){
				log.debug "Correcting switch ${it.displayName} to be on"
				it.on();
			}
			log.debug "Setting switch ${it.displayName} to level ${level}"
			it.setLevel(level)
		}else{
			log.debug("Skipping device that is not on!")
		}
	}
}

def levelEventHandler(evt){
	def level_raw = evt.value.toInteger()
	def level = level_raw
	if ( level_raw < 0 ) {
		level = 0
	} else if ( level_raw > 100){
		level = 100
	} else {
		level = level_raw
	}
	log.debug "Level set to: ${level} (${level_raw})"
	setLevel(level)
}

def switchEventHandler(evt){
	if (evt.value == "on") {
		log.debug "switch turned on!"
		turnOn()
	} else if (evt.value == "off") {
		log.debug "switch turned off!"
		turnOff()
	} else {
		log.debug "Unknown switch event: ${evt.value}!"
	}
}
def uninstalled() {
	//unsubscribe()
}