/**
 *  Door Left Open
 *
 *  Copyright 2015 Nicole Zeckner
 *
 *  Based on code written by brian@rayzurbock.com
 *
 *  Icon is Door by Rohith M S from the Noun Project. Used under Creative Commons
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
    name: "Door Left Open",
    namespace: "PurelyNicole",
    author: "Nicole",
    description: "Sends a notification when a door is left open after X seconds. Can send SMS messages to up to 2 numbers, push notification is optional.",
    category: "Safety & Security",
    iconUrl: "http://i.imgur.com/75YntiU.png",
    iconX2Url: "http://i.imgur.com/C9Jj0K5.png",
    iconX3Url: "http://i.imgur.com/C9Jj0K5.png")
  
preferences {

	section("When this door is left open..."){
		input "contactSensor", "capability.contactSensor", title: "Door"
	}
    section("For this long...") {
        input "numSeconds", "number", title: "Seconds", required: true
    }  
    section("Send a message that says..." ){
    		input "messageText", "text", title: "Message", required: true
    }
    section("To..." ){
    	input "phoneNumber", "phone", title: "Phone number", required: false
        input "phoneNumber2", "phone", title: "Second phone number", required: false
        input "pushAlert", "bool", title: "Send push alert?", required: true
    } 
    section("Tell them this many times..." ){
        input "repeatAlert", "number", title: "Number of alerts", required: true
    }
}


def installed() {
    subscribe(contactSensor, "contact", onContactChange);
    state.count = 0;
    state.maxrepeat = repeatAlert;
    state.alertmsg = "";
}

def updated() {
    unsubscribe()
    subscribe(contactSensor, "contact", onContactChange);
    state.count = 0;
    state.maxrepeat = repeatAlert;
    state.alertmsg = "";
}

def onContactChange(evt) {
    log.debug "onContactChange";
    if (evt.value == "open") {
        state.count = 0;
        state.maxrepeat = repeatAlert;
        runIn(numSeconds, onContactLeftOpenHandler);
    } else {
        //Door closed
        unschedule(onContactLeftOpenHandler);
        state.count = 0
    }
}

def onContactLeftOpenHandler() {
    if (contactSensor.latestValue("contact") == "open") {
    log.debug "Add one";
        state.count = state.count + 1
        log.debug "Door still open, alert! (Alert #${state.count})"
        if (state.count == 1) {
            //Run the following only on the first alert trigger
            state.alertmsg = messageText
        }
        
        if (state.count > 1 && state.count < state.maxrepeat) {state.alertmsg = "${messageText}. Repeat #${state.count}."}
        if (state.count == state.maxrepeat) {state.alertmsg = "${messageText}. Last notice."}
        
        if(pushAlert == true){sendPush(state.alertmsg);}
        log.debug "phone1 ${phoneNumber}";
        log.debug "phone2 ${phoneNumber2}";
        if(phoneNumber != null){sendSms(phoneNumber, state.alertmsg);}
		if(phoneNumber2 != null){sendSms(phoneNumber2, state.alertmsg);}
        
        if (repeatAlert > 0) {
            if (state.count < state.maxrepeat) {
                log.debug "Rescheduling repeat alert";
                unschedule();
                runIn(numSeconds, onContactLeftOpenHandler);
            }
        }
    }
     else {
        log.debug "Door closed, cancel alert"
    }

}
