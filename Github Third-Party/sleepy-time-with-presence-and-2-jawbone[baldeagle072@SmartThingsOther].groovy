/**
 *  Sleepy Time with Presence and 2 Jawbone
 *
 *  Copyright 2014 Eric Roberts
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
    name: "Sleepy Time with Presence and 2 Jawbone",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "Will only trigger the hello home phrase on the jawbone if the person is at home. If both people are home, both people have to be in sleep mode.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "selectPhrases")
}

def selectPhrases() {
    dynamicPage(name: "selectPhrases", title: "Configure Your Jawbone Phrases.", install: true, uninstall: true) {		
		section("Select user 1's Jawbone UP24 and presence sensor") {
			input "jawbone1", "device.jawboneUser", title: "Jawbone UP24", required: true, multiple: false
            input "presence1", "capability.presenceSensor", title: "Presence", required:true, multiple: false
		}
        
        section("Select user 2's Jawbone UP24 and presence sensor") {
			input "jawbone2", "device.jawboneUser", title: "Jawbone UP24", required: true, multiple: false
            input "presence2", "capability.presenceSensor", title: "Presence", required:true, multiple: false
		}
        
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	phrases.sort()
			section("Hello Home Actions") {
				log.trace phrases
				input "sleepPhrase", "enum", title: "Enter Sleep Mode (Bedtime) Phrase", required: true, options: phrases
				input "wakePhrase", "enum", title: "Exit Sleep Mode (Waking Up) Phrase", required: false, options: phrases
			}
		}
        section("Turn on - waking") {
            input "wakeOnSwitches", "capability.switch", multiple: true, required: false
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
    
    log.debug "Subscribing to sleeping events."
    
   	subscribe (jawbone, "sleeping", jawboneHandler)
    
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    
    log.debug "Subscribing to sleeping events."
        
   	subscribe (jawbone1, "sleeping", jawboneHandler1)
    subscribe (jawbone2, "sleeping", jawboneHandler2)
    
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

def jawboneHandler1(evt) {
	jawboneHandler(presence1, presence2, jawbone2, evt)
}

def jawboneHandler2(evt) {
	jawboneHandler(presence2, presence1, jawbone1, evt)
}

def jawboneHandler(primaryPresence, secondaryPresence, secondaryJawbone, evt) {

	def primaryPresenceValue = primaryPresence.latestValue("presence")
    def secondaryPresenceValue = secondaryPresence.latestValue("presence")
    
	log.debug "In Jawbone Event Handler, Event Name = ${evt.name}, Value = ${evt.value}"
	
    if (primaryPresenceValue == "present") {
    	log.debug "User Present"
        
        if (evt.value == "sleeping") {
        	if (secondaryPresenceValue == "present") {
        		if (evt.value == secondaryJawbone.latestValue("sleeping")) {
                	sendNotificationEvent("Sleepy Time performing \"${sleepPhrase}\" for you as requested.")
                    log.debug "Sleepy Time performing \"${sleepPhrase}\" for you as requested."
            		location.helloHome.execute(settings.sleepPhrase)
                } else {
                	log.debug "Second person not asleep"
                }
        	} else {
            	sendNotificationEvent("Sleepy Time performing \"${sleepPhrase}\" for you as requested.")
                log.debug "Sleepy Time performing \"${sleepPhrase}\" for you as requested."
            	location.helloHome.execute(settings.sleepPhrase)
            }
        }
        else {
        	if (wakePhrase) {
                sendNotificationEvent("Sleepy Time performing \"${wakePhrase}\" for you as requested.")
                log.debug "Sleepy Time performing \"${wakePhrase}\" for you as requested."
                location.helloHome.execute(settings.wakePhrase)
            }
            wakeOnSwitches?.on()
        }
    } else {
    	log.debug "User Not Present"
    }   
}