/**
 *  Humidity Control
 *
 *  Copyright 2015 Michael Pfammatter
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
    name: "Humidity Control",
    namespace: "GnomeSoup",
    author: "Michael Pfammatter",
    description: "Turn on a humidifier when the moisture level drops v0.3.1",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Humidity Control") {
    	paragraph("Version 0.3.1")
    }
    section("Select a humidifier to control:") {
    	input(name:"humSwitch", type:"capability.switch", 
        	title:"Humidifier switch...", required: true)
		input(name:"humPower", type:"capability.powerMeter", 
        	title:"and the humidifier's power meter...", required: false)
    }
    section("Based on this humidity sensor:") {
    	input(name:"humLevel", type:"capability.relativeHumidityMeasurement", 
        	title:"Humidity sensor...", required: true)
    }
    section("When the humidity levels reaches:") {
    	input(name:"humMax", type: "number", 
        	title:"Turn off when humidity reaches...", required: true)
    	input(name:"humMin", type: "number", 
        	title:"Turn on when humidity drops to...", required: true)
    }
    section("If the humidifier is not drawing power give a push:") {
    	input(name:"sendPushMessage", type:"bool", 
        	title: "Send a push notification?", required: false)
        input(name:"phone", type:"phone", 
        	title: "Send a text message?", required: false)
    }
    section("How often would you like to be notified?") {
    	input(name:"notifyDelay", type: "number", 
        	title: "Once in this many minutes:", defaultValue:60, required: false) 
    }
    section("Do not notify me in the following modes:") {
        input(name:"notModes", type:"mode", 
        	title: "Choose modes...", multiple: true, required: false)
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

def initialize() {
    	subscribe(humPower, "power", powerHandler)
    	subscribe(humLevel, "humidity", humidityHandler)
        subscribe(humSwitch, "switch", switchHandler)
        state.name = "Humdity Control"
}

def powerHandler(powerEvt) {
	log.debug("$state.name: powerHanlder called")

	def powerValue = powerEvt.value.toInteger()
    def notifyFix = notifyDelay * 60000
    if (!state.lastMessageSent) { state.lastMessageSent = now() - notifyFix}
    def currentTime = now()
    def timeSinceLast = currentTime.toInteger() - state.lastMessageSent.toInteger()
    
    log.debug("${humPower.label} is using $powerValue volts")
    log.debug("Time since last message sent: ${(timeSinceLast/60000).toInteger()} minutes")
    log.debug("state.humLevel: ${state.humLevel}")
    
	if (powerValue < 3 && state.humLevel < humMin) {
    	if((currentTime.toInteger() - state.lastMessageSent.toInteger()) > notifyFix){
            log.debug("$state.name: Humidity and power are low, sending message")
            send("${humSwitch.label} is empty. Humdity is ${state.humLevel}")
            log.debug("$state.name: Resetting last message time")
            state.lastMessageSent = now()
        }
    }
}

def humidityHandler(humidityEvt) {
	def humidityValue = humidityEvt.value.toInteger()
    
    log.debug("$state.name: humdityHandler called")
    log.debug("$state.name: ${humSwitch.label} is ${state.humSwitch}")
    log.debug("$state.name: Humidity is $humidityValue")
    state.humLevel = humidityValue
	if(humidityValue > humMax && state.humSwitch == "on") {
    	humSwitch.off()
        state.humSwitch = "off"
        log.debug("$state.name: Turning humidifier OFF")
    }
    else if(humidityValue < humMin && state.humSwitch == "off") {
    	humSwitch.on()
        state.humSwitch = "on"
        log.debug("$state.name: Turning humidifier ON")
    }
    else {
    	log.debug("$state.name: no action")
    }
}

def switchHandler(switchEvt) {
	def switchValue = switchEvt.value
    log.debug("$state.name: switchHandler called")
    log.debug("$state.name: Switch is $switchValue")
    state.humSwitch = switchValue
}

private send(msg) {
	log.debug("$state.name: Send message called")
    log.debug("sendPushMessage: $sendPushMessage")
    log.debug("mode: ${location.mode}")
    log.debug("notModes = $notModes")
    def dontSend = notModes.contains(location.mode)
    if ( dontSend ) {
    	log.debug("${state.name}: No message was sent because mode is ${location.mode}")
    }
    else {
    	log.debug("$state.name: Okay to send message because mode is ${location.mode}")
        if (sendPushMessage == true) {
            log.debug("$state.name: sending push message")
            sendPush(msg)
        }
        if (phone) {
            log.debug("$state.name: sending text message")
            sendSms(phone, msg)
        }
    }
}