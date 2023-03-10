/**
 *
 *  Copyright 2015 John Rucker
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
    name: "Virtual CoopBoss Manager",
    namespace: "JohnRucker",
    author: "John.Rucker@Solar-current.com",
    description: "Connects virtual CoopBoss device handler to physical CoopBoss.",
    category: "My Apps",
    iconUrl: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo.png",
    iconX2Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo2x.png",
    iconX3Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo3x.png")

preferences {
    section("When the Virtual CoopBoss state changes") {
        paragraph "Pick virtual CoopBoss Door."
		input "virtualCoopBoss", "capability.garageDoorControl", title: "Select virtual CoopBoss", required: true, multiple: false      
    
        paragraph "Open or Close the following CoopBoss door."
		input "physicalCoopBoss", "capability.doorControl", title: "Select CoopBoss", required: true, multiple: false            
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
	subscribe(physicalCoopBoss, "doorState", coopDoorStateHandler)
	subscribe(physicalCoopBoss, "currentLightLevel", coopLuxHandler) 
	subscribe(physicalCoopBoss, "TempProb1", coopTemperatureHandler)  
	subscribe(physicalCoopBoss, "Aux1", coopLightHandler)    
    subscribe(virtualCoopBoss, "door", virtualCoopDoorStateHandler)
    subscribe(virtualCoopBoss, "switch", virtualCoopSwitchHandler)    
    syncVirtualToPhysical()
}

def coopDoorStateHandler(evt) {
	log.info "coopDoorStateHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    
    if(evt.name == "doorState" && evt.value == "open"){
        log.debug "Coop door is now open."
        virtualCoopBoss.finishOpening()
    } else if(evt.name == "doorState" && evt.value == "closed"){
        log.debug "Coop door is now closed."
        virtualCoopBoss.finishClosing()        
    }    
}

def coopLuxHandler(evt) {
	log.info "coopLuxHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    virtualCoopBoss.setLux(evt.value)   
}

def coopTemperatureHandler(evt) {
	log.info "coopTemperatureHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    virtualCoopBoss.setTemperature(evt.value)   
}

def coopLightHandler(evt) {
	log.info "coopLightHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    virtualCoopBoss.setLight(evt.value)   
}

def virtualCoopDoorStateHandler(evt) {
	log.info "virtualCoopDoorStateHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    if(evt.name == "door" && evt.value == "opening"){
        log.debug "virtual coopboss sending OPEN command to physical coopboss"
        physicalCoopBoss.openDoor()
    } else if(evt.name == "door" && evt.value == "closing"){
        log.debug "virtual coopboss sending CLOSE command to physical coopboss"
        physicalCoopBoss.closeDoor()        
    }
}

def virtualCoopSwitchHandler(evt) {
	log.info "virtualCoopSwitchHandler(evt) called evt.name = ${evt.name} evt.value = ${evt.value}"  
    if(evt.value == "on"){
        log.debug "virtual coopboss sending on command to Aux 1 on physical coopboss"
        physicalCoopBoss.Aux1On()
    } else {
        log.debug "virtual coopboss sending off command to Aux 1 on physical coopboss"
        physicalCoopBoss.Aux1Off()      
    }
}

def syncVirtualToPhysical(){
    def phyDoorState = physicalCoopBoss.currentState("doorState")
	log.info "Synching virtual with physical. PhyDoorState = ${phyDoorState.value}"
    if(phyDoorState.value == "open"){
        log.debug "Setting virtual coopboss door to open."
        virtualCoopBoss.finishOpening()
    } else if(phyDoorState.value == "closed"){
        log.debug "Setting virtual coopboss door to close."
        virtualCoopBoss.finishClosing()        
    }        
    
}