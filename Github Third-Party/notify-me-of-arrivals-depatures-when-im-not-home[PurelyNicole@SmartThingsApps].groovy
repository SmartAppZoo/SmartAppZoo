/**
 *  Are you home? I'm not.
 *
 *  Copyright 2015 Nicole Zeckner
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
    name: "Notify Me of Arrivals/Depatures When I'm Not Home",
    namespace: "PurelyNicole",
    author: "Nicole Zeckner",
    description: "Let's you know when a family member is arriving or departing, but only if you're not home.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Family/App-IMadeIt.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-IMadeIt@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-IMadeIt@2x.png")


preferences {
	section("When I'm not home...") {
		input "presenceUser1", "capability.presenceSensor", title: "Your Presense Sensor", required: true, multiple: false
	}
    section("Let me know when this person comes and goes...") {
    	input "presenceUser2", "capability.presenceSensor", title: "Sensor to Track", required: true, multiple: false
    }
    section("By sending a message to..."){
    	input "phoneNumber", "phone", title: "Phone number", required: false
        input "pushAlert", "bool", title: "Send push alert?", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribe(presenceUser2, "presence", user2change)
}

def updated() {
	unsubscribe()
    subscribe(presenceUser2, "presence", user2change)

}

def user2change(evt) {
	log.debug "User 2's presense status has changed."
    if (presenceUser1.currentPresence == "not present") {
    	log.debug "User 1 not home, send message."
    
        if (presenceUser2.currentPresence =="not present"){
        	log.debug "User 2 has left home."
            state.alertmsg = presenceUser2.displayName + " has left home."
        } //end user 2 left
        
        if (presenceUser2.currentPresence =="present"){
        	log.debug "User 2 has left home."
            state.alertmsg = presenceUser2.displayName + " has arrived at home."
        } //end user 2 home
        
        if(pushAlert == true){sendPush(state.alertmsg);}
        if(phoneNumber != null){sendSms(phoneNumber, state.alertmsg);}
        
    } //end if user 1 not home   
       if (presenceUser1.currentPresence == "present") {
    	log.debug "User 1 is home, do not send message."
       }
}