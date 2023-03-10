/**
 *  Sleepy Time
 *
 *  Copyright 2014 Physical Graph Corporation
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
    name: "Sleepy Time",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Use Jawbone sleep mode events to automatically execute Hello, Home phrases. Automatially put the house to bed or wake it up in the morning by pushing the button on your UP.",
    category: "SmartThings Labs",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/jawbone-up.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/jawbone-up@2x.png"
)

preferences {
	page(name: "selectPhrases")
	page(name: "loginToTesla", title: "Tesla")
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectPhrases() {
    dynamicPage(name: "selectPhrases", title: "Configure Your Jawbone Phrases.", install: true, uninstall: true) {		
		section("Select your Jawbone UP") {
			input "jawbone", "attribute.jawboneUser", title: "Jawbone UP", required: true, multiple: false,  submitOnChange:true
		}
        
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        		phrases.sort()
			section("Hello Home Actions") {
				input "sleepPhrase", "enum", title: "Enter Sleep Mode (Bedtime) Phrase", required: false, options: phrases,  submitOnChange:true
				input "wakePhrase", "enum", title: "Exit Sleep Mode (Waking Up) Phrase", required: false, options: phrases,  submitOnChange:true
			}
		}
    }
}

def loginToTesla() {
	def showUninstall = username != null && password != null
	dynamicPage(name: "loginToTesla", title: "Connect your Tesla", nextPage:"selectCars", uninstall:showUninstall) {
		section("Log in to your Tesla account:") {
			input "username", "text", title: "Username", required: true, autoCorrect:false
			input "password", "password", title: "Password", required: true, autoCorrect:false
		}
		section("To use Tesla, SmartThings encrypts and securely stores your Tesla credentials.") {}
	}
}

def installed() {
 
   	subscribe (jawbone, "sleeping", jawboneHandler)
    
}

def updated() {

	unsubscribe()
   	subscribe (jawbone, "sleeping", jawboneHandler)
    
}

def jawboneHandler(evt) {
	if (evt.value == "sleeping" && sleepPhrase) {
		log.debug "Sleeping"
		sendNotificationEvent("Sleepy Time performing \"${sleepPhrase}\" for you as requested.")
		location.helloHome.execute(settings.sleepPhrase)
	}else if (evt.value == "not sleeping" && wakePhrase) {
		log.debug "Awake"
		sendNotificationEvent("Sleepy Time performing \"${wakePhrase}\" for you as requested.")
		location.helloHome.execute(settings.wakePhrase)
	}
        
}