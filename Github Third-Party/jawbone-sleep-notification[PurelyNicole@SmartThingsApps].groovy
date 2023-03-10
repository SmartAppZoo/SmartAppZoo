/**
 *  Jawbone Sleep Notification
 *
 *  Copyright 2016 Nicole
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
    name: "Jawbone Sleep Notification",
    namespace: "PurelyNicole",
    author: "Nicole",
    description: "Sends a notification if the sleep status of a Jawbone UP changes.",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/HealthAndWellness/App-SleepyTime@2x.png")


preferences {
	section("When this Jawbone changes mode...") {
		input "myJawbone", "capability.sleepSensor", title: "Your Jawbone", required: true, multiple: false
	}
    section("Text this number...") {
    	input "phoneNumber", "phone", title: "Your Phone number", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribe(myJawbone, "sleeping", jawboneChange)

}

def updated() {
    unsubscribe()
    subscribe(myJawbone, "sleeping", jawboneChange)
}

def jawboneChange(evt) {
	log.debug "The Jawbone staus has changed."
    if (myJawbone.currentSleeping == "not sleeping") {
    	log.debug "Jawbone has woken up."
        state.alertmsg = myJawbone.displayName + " has woken up."
    }//end jawbone awake
    
    if (myJawbone.currentSleeping =="sleeping"){
        log.debug "Jawbone has gone to sleep"
        state.alertmsg = myJawbone.displayName + " has gone to sleep."
    } //end user 2 left

    if(phoneNumber != null){sendSms(phoneNumber, state.alertmsg);}
        
} //end jawboneChange
