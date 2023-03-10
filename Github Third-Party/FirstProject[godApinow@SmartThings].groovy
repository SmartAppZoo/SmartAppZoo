/**
 *  Smart GarageDoor Button
 *
 *  Copyright 2017 Travis Spire-Sweet
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
    name: "Smart GarageDoor Button",
    namespace: "godApinow",
    author: "Travis Spire-Sweet",
    description: "Converting Aeon sos button to smart garage door opener with appropriate alerts",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Button") {
		input "button", "capability.button", title: "Button Pushed", required: false, multiple: true //tw
	}
	section("GarageDoor"){
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true	
	}

	section("Send this message"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
			//input"recipients", "contact", title: "Send notifications to",required: false
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
	section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
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
	//subscribeToEvents()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "initializing"
    subscribe(button, "button.pushed", eventHandler)
	//subscribe(contact, "contact.open", eventHandler)
    //subscribe(contact, "contact.closed", eventHandler)
	//subscribe(mySwitch, "switch.on", eventHandler)
	//subscribe(mySwitch, "switch.off", eventHandler)
    
    def myButtonCaps = button.capabilities

// log each capability supported by the "mySwitch" device, along
// with all its supported commands
	myButtonCaps.each {cap ->
    log.debug "Capability name: ${cap.name}"
    cap.commands.each {comm ->
        log.debug "-- Command name: ${comm.name}"
    }
    cap.attributes.each {attr ->
        log.debug "-- Attribute name; ${attr.name}"
    }
}
	def batteryAttr = button.currentState("battery").stringValue
    log.debug "battery ${batteryAttr}"
}

def subscribeToEvents(){

}

def eventHandler(evt) {
//evt.device.currentState("battery").stringValue

	def garage = mySwitch.currentState("switch").stringValue
    log.debug "garage state ${garage}"
         def stingOn = "on"
   		 def stringOff = "off"
    	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
        log.debug "inside if statement"
     	 if(stringOff.equalsIgnoreCase(garage)) { 
   				 log.debug "same" 
         def messageString = "Opening Garage Door Battery %${batteryAttr}"
         messageString = messageString.replace("[", "")
         messageString = messageString.replace("]", "");
                 sendMessage(messageString.toString())
            	log.debug "opening garage door"
				mySwitch.on()
                return;
  			}else{ 
    			log.debug "not same" 
                log.debug "inside myswitch current Value"
                     def messageString = "Closing Garage Door Battery %${batteryAttr}"
         messageString = messageString.replace("[", "")
         messageString = messageString.replace("]", "");
                 sendMessage(messageString.toString())
            	 log.debug "closing garage door"
            	 mySwitch.off()
            	 return;
  				}    
		}
	} else {
    		def batteryAttr = button.currentState("battery").stringValue
    	log.debug "battery ${batteryAttr}"
    	log.debug "inside if else statment"
        log.debug "garage state in else statment is ${garage}"
     	 if(stringOff.equalsIgnoreCase(garage)) { 
   				 log.debug "same" 
                      def messageString = "Opening Garage Door Battery %${batteryAttr}"
         messageString = messageString.replace("[", "")
         messageString = messageString.replace("]", "");
                 sendMessage(messageString.toString())
            	log.debug "opening garage door"
				mySwitch.on()
                return;
  			}else{ 
    			log.debug "not same" 
                log.debug "inside myswitch current Value"
                     def messageString = "Closing Garage Door Battery %${batteryAttr}"
         messageString = messageString.replace("[", "")
         messageString = messageString.replace("]", "");
           		 sendMessage(messageString.toString())
            	 log.debug "closing garage door"
            	 mySwitch.off()
            	 return;
  				} 
    }
}


private sendMessage(evt) {
	log.debug "sendMessage ${evt}"
	String msg = evt
	Map options = [:]

	if (!messageText) {
		//msg = defaultText(evt)
		options = [translatable: true, triggerEvent: evt]
	}
	//log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (location.contactBookEnabled) {
		//sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone == 'Yes') {
        	log.debug "pushAndPhoneState ${pushAndPhone}"
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private defaultText(evt) {
//if (evt.name == 'presence') {
	if (evt == 'presence') {
		if (evt.value == 'present') {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has arrived at the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has arrived at {{ location.name }}'
			}
		} else {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has left the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has left {{ location.name }}'
			}
		}
	} else {
		'${evt}'
	}
}


private getIncludeArticle() {
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}
