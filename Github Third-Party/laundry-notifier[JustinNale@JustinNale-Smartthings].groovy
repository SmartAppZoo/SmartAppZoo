/**
 *  Laundry Notifier vA
 *
 *  Copyright 2015 Justin Nale
 *  Based heavily on Laundry Monitor by Brandon Miller
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
 
import groovy.time.* 
 
definition(
    name: "Laundry Notifier",
    namespace: "JustinNale",
    author: "Justin Nale",
    description: "This application is a (further) modification of the SmartThings Laundry Monitor SmartApp.  This allows for a janky user selection and instead of using a vibration sensor, this utilizes Power (Wattage) draw from an Aeon Smart Energy Meter.",
    category: "My Apps",
    iconUrl: "http://www.clker.com/cliparts/6/b/d/5/1207431803659474302laundry%20laundomat%20black.svg.med.png",
    iconX2Url: "http://www.clker.com/cliparts/6/b/d/5/1207431803659474302laundry%20laundomat%20black.svg.med.png"
    )


preferences {
	section("Tell me when this washer has stopped..."){
		input "sensor1", "capability.powerMeter"
	}
    
    // Dryer def will go here

        section("User 1"){
		input "myswitchUser1", "capability.switch"
        input "nameUser1", "text", title: "Name?"
        input "phoneUser1", "phone", title: "Send a text message?", required: false
	}
    
    	section("User 2", hidden: true, hideable: true){
		input "myswitchUser2", "capability.switch", required: false
        input "nameUser2", "text", title: "Name?", required: false
        input "phoneUser2", "phone", title: "Send a text message?", required: false
	}
    
    	section("User 3", hidden: true, hideable: true){
		input "myswitchUser3", "capability.switch", required: false
        input "nameUser3", "text", title: "Name?", required: false
        input "phoneUser3", "phone", title: "Send a text message?", required: false
	}    
    
    	section("User 4", hidden: true, hideable: true){
		input "myswitchUser4", "capability.switch", required: false
        input "nameUser4", "text", title: "Name?", required: false
        input "phoneUser4", "phone", title: "Send a text message?", required: false
	}    

    	section("User 5", hidden: true, hideable: true){
		input "myswitchUser5", "capability.switch", required: false
        input "nameUser5", "text", title: "Name?", required: false
        input "phoneUser5", "phone", title: "Send a text message?", required: false
	}
    
    	section("User 6", hidden: true, hideable: true){
		input "myswitchUser6", "capability.switch", required: false
        input "nameUser6", "text", title: "Name?", required: false
        input "phoneUser6", "phone", title: "Send a text message?", required: false
	}    
    
    
    //section("OLD_Notifications") {
	//	input "sendPushMessage", "bool", title: "Push Notifications?"
	//	input "phone", "phone", title: "Send a text message?", required: false
	//}

	section("System Variables"){
    	//input "sendPushMessage", "bool", title: "Push Notifications?"
    	input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 2
        //input "message", "text", title: "Notification message", description: "Washer is done!", required: true
	}
	}
    // JN - I dont know what this is
	//section ("Additionally", hidden: hideOptionsSection(), hideable: true) {
	//    input "phone", "phone", title: "Send a text message to:", required: false
	//    input "speech", "capability.speechSynthesis", title:"Speak message via: ", multiple: true, required: false
	//}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "power", powerInputHandler)
}

def powerInputHandler(evt) {
	def latestPower = sensor1.currentValue("power")
    log.trace "Power: ${latestPower}W"
    
    if (!atomicState.isRunning && latestPower > minimumWattage) {
    	atomicState.isRunning = true
		atomicState.startedAt = now()
        atomicState.stoppedAt = null
        log.trace "Cycle started."
        sendNotificationEvent("Cycle started.")
        sendpush("Cycle started.")
    } else if (atomicState.isRunning && latestPower < minimumWattage) {
    	atomicState.isRunning = false
        atomicState.stoppedAt = now()  
        log.debug "startedAt: ${atomicState.startedAt}, stoppedAt: ${atomicState.stoppedAt}" 
        sendNotificationEvent("Cycle finished.")



		if (myswitchUser1){
        	if (myswitchUser1.currentSwitch == "on"){
            	def message = "${nameUser1} - Your wash is done!"
            	sendSms phoneUser1, message
            }
		}
        
		if (myswitchUser2){
        	if (myswitchUser2.currentSwitch == "on"){
            	def message = "${nameUser2} - Your wash is done!"
            	sendSms phoneUser2, message
            }
        } 
        
		if (myswitchUser3){
        	if (myswitchUser3.currentSwitch == "on"){
            	def message = "${nameUser3} - Your wash is done!"
            	sendSms phoneUser3, message
            }
        }
        
		if (myswitchUser4){
        	if (myswitchUser4.currentSwitch == "on"){
            	def message = "${nameUser4} - Your wash is done!"
            	sendSms phoneUser4, message
            }            
        }
        
		if (myswitchUser5){
        	if (myswitchUser5.currentSwitch == "on"){
            	def message = "${nameUser5} - Your wash is done!"
            	sendSms phoneUser5, message
            }            
        } 
        
		if (myswitchUser6){
        	if (myswitchUser6.currentSwitch == "on"){
            	def message = "${nameUser6} - Your wash is done!"
            	sendSms phoneUser6, message
            }            
        }  
 // For debugging - way too noisy           
 // sendPush("LAUNDRY TRIGGER!")


//        if (phone) {
//            sendSms phone, message
//        } else {
//           sendPush message
//        }
        
        // JN - I dont know what this is
        //speechAlert(message)
    }
    else {
    	// Do Nothing, no change in either direction
    }
}

// JN - I dont know what this is
//private speechAlert(msg) {
//  speech.speak(msg)
//}

// JN - I dont know what this is
//private hideOptionsSection() {
//  (phone) ? false : true
//}

private hideUser3() {
  (myswitchUser2) ? false : true
}
private hideUser4() {
  (myswitchUser3) ? false : true
}
private hideUser5() {
  (myswitchUser4) ? false : true
}
private hideUser6() {
  (myswitchUser5) ? false : true
}