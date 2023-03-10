/**
 *  Based on the code for "I'm Back - Change to Home Mode on Code Unlock", Copyright 2014 Barry A. Burke
 *
 *  Copyright 2014 Brian S. Lowrance
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
    name: "Run \"Hello Home\" on any unlock code",
    namespace: "",
    author: "Brian Lowrance",
    description: "Set mode to Home when specific lock is unlocked with any authorized code.(V1.0.0)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

import groovy.json.JsonSlurper

preferences {
	page(name: "selectPhrases")
}

def selectPhrases() {
	def configured = (settings.lock1 && settings.homePhrase)
    //dynamicPage(name: "selectPhrases", title: "Configure your code and phrases.", install: configured, uninstall: true) {	
    dynamicPage(name: "selectPhrases", title: "Configure your code and phrases.", install: true, uninstall: true) {	
		section("Which Lock?") {
			input "lock1","capability.lock", title: "Lock"
    	}
           
    	def phrases = location.helloHome?.getPhrases()*.label
    	if (phrases) {
       		phrases.sort()
            log.trace phrases
			section("Hello Home actions...") {
				input "homePhrase", "enum", title: "Home Mode Phrase (I'm Back!)", required: true, options: phrases, refreshAfterSelection:true
        	}        
		}

		section( "Notifications" ) {
        	input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
        	input "phone", "phone", title: "Send a Text Message?", required: false
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
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", CodeModeCheck)
    //subscribe(lock1, "lock", CodeModeCheckTest)
}


def CodeModeCheck(evt)
{
	//log.debug "The ${lock1.displayName} lock is ${lock1.latestValue("lock")}."
	//log.debug "|BSL| DoorHandler event called: ${evt.name}"
	if (location.mode != "Home") {  	// Only if we aren't already in Home mode
		if (evt.name == "lock") {		// Only if the event is related to the lock
    		//log.debug "|BSL| Lock related event detected."
    		if (evt.value == "unlocked") {		//Only if the lock action was Unlock
        		log.debug "|BSL| Door was unlocked"
	    		if ((evt.data != "") && (evt.data != null)) {					// Only if we have extended data
            		//log.debug "|BSL| We have extended lock data!"
	    			def data = new JsonSlurper().parseText(evt.data)
            		if ((data.usedCode != "") && (data.usedCode != null)) {		// Only if we have usedCode data
                   		log.debug "|BSL| ${lock1.displayName} unlocked with ${data.usedCode}."
                       	//log.debug "|BSL| Home mode was not HOME, executing phrase chosen"
    	    			sendNotificationEvent("Running, ${homePhrase}, because ${lock1.displayName} unlocked by code ${data.usedCode}.")
						location.helloHome.execute(settings.homePhrase)	// Wake up the house - we're HOME!!!
                        //log.debug "|BSL| phrase execution complete"
                        send("${lock1.displayName} unlocked with code ${data.usedCode} while not Home, executing ${settings.homePhrase}")
                	}
            	}
        	}
    	}
	}
}

private send(msg) {
    if ( sendPushMessage != "No" ) {
        log.debug( "sending push message" )
        sendPush( msg )
    }

    if ( phone ) {
        log.debug( "sending text message" )
        sendSms( phone, msg )
    }

    log.debug msg
}
