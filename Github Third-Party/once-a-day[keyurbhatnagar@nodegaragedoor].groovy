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
 *  Once a Day
 *
 *  Author: Keyur Bhatnagar
 *
 *  Check state of devices at a given time of day.
 */

definition(
    name: "Once a day!",
    namespace: "keyurbhatnagar",
    author: "Keyur Bhatnagar",
    description: "Check state of devices at a given time of day.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health7-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health7-icn@2x.png"
)

preferences {
	section("Choose one or more..."){
	        input "contacts", "capability.contactSensor", title: "Contact Opens", required: true, multiple: true
        }
	section("Check the status at...") {
		input name: "startTime", title: "Run check at what time?", type: "time"
	}
}

def installed() {
	log.debug "Installed"
	schedule(startTime, "startTimerCallback")

}

def updated(settings) {
	unschedule()
	schedule(startTime, "startTimerCallback")
}

def startTimerCallback() {
	log.debug "Checking door status"
	
    def allClosed = true
	contacts.each { contact -> 
					if( contact.currentValue("contact") == "open" ) {
						log.debug "$contact was opened, sending push message to user"
						sendPush("Your ${contact.label ?: contact.name} is Open!")
                        allClosed = false
					}
				}
                
    if( allClosed ) {
    	sendPush("All doors are closed!")
    }
    
}