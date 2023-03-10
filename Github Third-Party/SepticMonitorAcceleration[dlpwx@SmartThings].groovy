/**
 *  Notify When Acceleration Sensor not detected for xx hours
 *
 *  Original "Notify When Left Open" Author: olson.lukas@gmail.com
 *  Accelleration/Septic not active version: dlee
 *  Date: 2013-06-24, Updated: 2014-7-15
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
    name: "Septic Pump Monitor",
    namespace: "dlee",
    author: "David Lee",
    description: "Septic Pump Monitor with notification",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When . . .") {
		input "accSensor", "capability.accelerationSensor", title: "Pump sensor inactive"
        input "numHours", "number", title: "For how many hours"
        input "messageText", "text", title: "Send notification that says"
        input "phoneNumber", "phone", title: "Send SMS message to"
	}
}

def installed() {
	subscribe(accSensor, "acceleration", onAccelerationChange);
}

def updated() {
	unsubscribe()
   	subscribe(accSensor, "acceleration", onAccelerationChange);
}

def onAccelerationChange(evt) {
	log.debug "onAccelerationChange";
	if (evt.value == "inactive") {
    	runIn(numHours * 3600, onAccelerationInactiveHandler);
    } else {
    	unschedule(onAccelerationInactiveHandler);
    }
}

def onAccelerationInactiveHandler() {
	log.debug "onAccelerationInactiveHandler";
	sendPush(messageText);
    sendSms(phoneNumber, messageText);
}  
