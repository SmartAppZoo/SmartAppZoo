/**
 *  Coop Light Management  based on info from this URL http://scoopfromthecoop.nutrenaworld.com/winter-lighting-for-chickens/
 *
 *  Copyright 2016 John Rucker
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
    name: "Coop Light Management",
    namespace: "JohnRucker",
    author: "John Rucker",
    description: "Turns on coop light based on length of day",
    category: "My Apps",
    iconUrl: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo.png",
    iconX2Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo2x.png",
    iconX3Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo3x.png")


preferences {
        section(hideable: true, "Overview") {
        	paragraph "Hens need 14 to 15 hours of light for optimal egg production.  This SmartApp allows you to set a target amount of light for your hens and will supplement their light in the morning. For example, lets say you want your hens to have 14 hours of light each day but there is only 10 hours of daylight during the winter.  This SmartApp will turn on the coop's light 4 hours before sunrise to supplement for the short winter days."  
        }
        section("SmartApp Settings") {
			input(name: "coopBoss", type: "capability.doorControl", title: "Select CoopBoss to manage.", required: true, multiple: false)            
			input(name: "coopLight", type: "capability.switch", title: "Select Coop light(s) to manage.", required: true, multiple: true)
            input(name: "targetLightHours", type: "decimal", title: "Enter the target daylight hours for your hens (for example to enter twelve and half hours type 12.5).", required: true)
            input(name: "offLightValue", type: "number", title: "Enter the CoopBoss light level that will turn off the light in the morning.", defaultValue: 300, required: false)   
            input "sendPushMessage", "enum", title: "Notify me when this app turns the light on or off (sends a SmartThings notification to your phone).", metadata: [values: ["Yes", "No"]], defaultValue: "No", required: false
            input "skipTomorrow", "enum", title: "Let your hens sleepin.  Select yes to disable rule and let your hens sleep in tomorrow.", metadata: [values: ["Yes", "No"]], defaultValue: "No", required: false
        }
}


def installed() {
	log.trace "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.trace "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	atomicState.timerState = "off"
    atomicState.clmTurnedLightOn = "no"
	subscribe(coopBoss, "currentLightLevel", checkLight)     
}

def checkLight(evt){
    def outsideLightLevel = evt.value as int
    def sleepTime = 24 - targetLightHours
    
    log.info "outside light level: ${outsideLightLevel}, coop light has been turned on by app: ${atomicState.clmTurnedLightOn}, sleep time: ${sleepTime} hours, timer is ${atomicState.timerState}"  
     
    if (outsideLightLevel >= offLightValue && atomicState.clmTurnedLightOn == "yes"){
    	log.debug "Turning coop light off"
        if (sendPushMessage == "Yes"){sendPush("Coop Light Management app turned coop light off.")}
        coopLight*.off()
        atomicState.clmTurnedLightOn = "no"
    }
    
    if (outsideLightLevel == 0 && atomicState.timerState == "off"){
    	def secondsToLightOn = sleepTime * 3600
        secondsToLightOn = (int)secondsToLightOn
    	atomicState.timerState = "on"
        log.debug "Sunset detected, light will be switched on in ${secondsToLightOn} seconds."
        runIn(secondsToLightOn, "turnLightOn")	
    }
}

def turnLightOn(){
	def lightLevelObject = coopBoss.currentState("currentLightLevel")
    def outsideLightLevel = lightLevelObject.value as int
    
    log.debug "Its time to wake up the hens, the outside light level is ${outsideLightLevel}"
    if (outsideLightLevel <= offLightValue){
    	if (skipTomorrow == "Yes"){
        	log.debug "Skipping light turn on. Letting hens sleep in is set to Yes"
        }else{
            log.debug "Its dark, turning on coop light"
            if (sendPushMessage == "Yes"){sendPush("Coop Light Management app waking up hens, coop light on.")}
            coopLight*.on()	
            atomicState.clmTurnedLightOn = "yes"
        }
    } else {
    	log.debug "Sun is already up no need to turn on light"
    }
    atomicState.timerState = "off"
}