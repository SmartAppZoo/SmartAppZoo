/**
 *  zmcDynamicChild
 *
 *  Copyright 2015 Mike Maxwell
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
    name: "zmcDynamicChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "child for zone motion manager",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "main")
    page(name: "triggers", nextPage	: "main")
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
	state.zoneTriggerActive = false
	subscribe(motionSensors, "motion.inactive", inactiveHandler)
    subscribe(motionSensors, "motion.active", activeHandler)
 	def simMotion = getChildDevices()
    def zName = "mZone-${settings.zoneName}"
    def hub = location.hubs[0]
    log.debug "device:${simMotion.displayName}"
    app.updateLabel("${settings.zoneName} Zone Controller")    
	if (!simMotion) {
    	log.info "create the child device"
        def kid = addChildDevice("MikeMaxwell", "simulatedMotionSensor", app.id, hub.id, [name: zName, label: zName, completedSetup: true])
        kid.inactive()
    } else {
    	log.info "we have the child device already"
        simMotion[0].inactive()
    }
}
def activityTimeoutHandler(){
	def timeout = settings.zoneTimeout.toInteger()
    log.debug "runin: ${timeout} seconds..."
    runIn(timeout,zoneOff)
}

//False motion reduction
def allMotionsActive(evtTime){
	def enable
    def window = settings.activationWindowFD.toInteger()
	enable = motionSensors.currentState("motion").every{ s -> s.value == "active" && (evtTime - s.date.getTime()) < window}
	log.debug "fmrHandler:${enable}"
    return enable
}

//Triggered Activation
def anyTriggersActive(evtTime){
	def enable = false
    def window = settings.activationWindowTA.toInteger()
    def evtStart = new Date(evtTime - window)
   
    if (triggerMotions){
    	enable = triggerMotions.any{ s -> s.statesSince("motion", evtStart).size > 0}
        log.debug "triggerMotions:${enable}"
    }
    if (!enable && triggerContacts){
    	enable = triggerContacts.any{ s -> s.statesSince("contact", evtStart).size > 0}
        log.debug "triggerContacts:${enable}"
    }
    if (!enable && triggerSwitches){
    	enable = triggerSwitches.any{ s -> s.statesSince("switch", evtStart).size > 0}
        log.debug "triggerSwitches:${state}"
    }
    log.debug "anyTriggersActive - final:${enable}"
    return enable
}
def activeHandler(evt){
	def evtTime = evt.date.getTime()
  	switch (settings.zoneType) {
    	//False motion reduction
		case "0":
        	if (allMotionsActive(evtTime)) zoneOn() 
			break
        //Motion Aggregation
		case "1":
        	zoneOn()
        	activityTimeoutHandler()
	        break
        //Triggered Activation
		case "2":
        	if (!state.zoneTriggerActive && anyTriggersActive(evtTime)){
            	zoneOn()
                activityTimeoutHandler()
                state.zoneTriggerActive = true
            } else if (state.zoneTriggerActive){
            	activityTimeoutHandler()
            }
	        break
 	}	
}
def inactiveHandler(evt){
	if (settings.zoneType == "0" && allInactive()){
    	zoneOff()
    }
}
def zoneOn(){
	def simMotion = getChildDevices(true)[0]
	if (simMotion.currentValue("motion") != "active") {
		log.info "Zone: ${simMotion.displayName} is active."
		simMotion.active()
   	}
}
def zoneOff(){
	def simMotion = getChildDevices(true)[0]
	if (simMotion.currentValue("motion") != "inactive") {
		log.info "Zone: ${simMotion.displayName} is inactive."
        state.zoneTriggerActive = false
		simMotion.inactive()
    }
}
def allInactive () {
	//log.debug "allInactive:${motionSensors.currentValue("motion")}"
	def state = motionSensors.currentState("motion").every{ s -> s.value == "inactive"}
    log.debug "allInactive: ${state}"
	return state
}

/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
    def zType = settings.zoneType
    log.info "Installed:${installed} zoneType:${zType}"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section(){
                    input(
                        name		: "zoneName"
                        ,type		: "text"
                        ,title		: "Name of this Zone:"
                        ,multiple	: false
                        ,required	: true
                    )
					input(
            			name		: "motionSensors"
                		,title		: "Motion Sensors:"
                		,multiple	: true
                		,required	: true
                		,type		: "capability.motionSensor"
            		)                    
                    input(
                        name					: "zoneType"
                        ,type					: "enum"
                        ,title					: "Zone Type"
                        ,multiple				: false
                        ,required				: true
                        ,options				: [[0:"False motion reduction"],[1:"Motion Aggregation"],[2:"Triggered Activation"]]
                        ,submitOnChange			: true
                    )
            }
            if (zType){
                section(){
                  	paragraph getDescription(zType)
                	//False motion reduction
                    if (zType == "0"){
            			input(
            				name			: "activationWindowFD"
                			,title			: "Activation Window:"
                			,multiple		: false
                			,required		: true
                			,type			: "enum"
                			,options		: [[1000:"1 Second"],[1500:"1.5 Seconds"],[2000:"2 Seconds"],[2500:"2.5 Seconds"],[3000:"3 Seconds"],[4000:"4 Seconds"],[5000:"5 Seconds"],[6000:"6 Seconds"],[7000:"7 Seconds"],[8000:"8 Seconds"],[9000:"9 Seconds"],[10000:"10 Seconds"]]
                			,defaultValue	: 2000
            			)
                    }
          			if (zType == "2"){
            			input(
            				name			: "activationWindowTA"
                			,title			: "Activation Window:"
                			,multiple		: false
                			,required		: true
                			,type			: "enum"
                			,options		: [[10000:"10 Seconds"],[15000:"15 Seconds"],[30000:"30 Seconds"],[60000:"1 Minute"]]
                			,defaultValue	: 15000
            			)
                     	href(
                        	name		: "dv"
                        	,title		: "Trigger Devices" 
                        	,required	: false
                        	,page		: "triggers"
                        	,description: null
                            ,state		: triggerPageComplete()
                     	)
                    }                    
          			if (zType == "1" || zType == "2"){
        				input(
            				name			: "zoneTimeout"
                			,title			: "Activity Timeout:"
                			,multiple		: false
                			,required		: true
                			,type			: "enum"
                			,options		: [[60:"1 Minute"],[120:"2 Minutes"],[180:"3 Minutes"],[240:"4 Minutes"],[300:"5 Minutes"],[600:"10 Minutes"],[900:"15 Minutes"],[1800:"30 Minutes"],[3600:"1 Hour"]]
                			,defaultValue	: 300
            			)                  
                    }
            	} //end section Zone settings
            } //end if 
            section("Optional settings"){
                    input(
                        name		: "modes"
                        ,type		: "mode"
                        ,title		: "Set for specific mode(s)"
                        ,multiple	: true
                        ,required	: false
                    )
            } //end section Zone settings
	}
}
def triggers(){
	return dynamicPage(
    	name		: "triggers"
        ,title		: "Trigger devices"
        //,install	: installed
        //,uninstall	: installed
        ){
		     section(){
					input(
            			name		: "triggerMotions"
                		,title		: "Motion Sensors"
                		,multiple	: true
                		,required	: false
                		,type		: "capability.motionSensor"
            		)                    
					input(
            			name		: "triggerContacts"
                		,title		: "Contact Sensors"
                		,multiple	: true
                		,required	: false
                		,type		: "capability.contactSensor"
            		)                    
					input(
            			name		: "triggerSwitches"
                		,title		: "Switches"
                		,multiple	: true
                		,required	: false
                		,type		: "capability.switch"
            		)                    
			}
		}
}
def triggerPageComplete(){
	if (triggerMotions || triggerContacts || triggerSwitches){
    	return "complete"
    } else {
    	return null
    }
}
def getDescription(zType){
   switch (zType) {
		case "0":
			return	"When all motion sensors activate within the Activation Window, the zone will activate." +
					"\r\nThe zone will deactivate when all motion sensors are inactive."
			break
		case "1":
			return	"Any motion sensor will activate this zone." +
					"\r\nThe zone remains active while motion continues within the Activity Timeout." +
					"\r\nThe Activity Timeout is restarted on each motion sensor active event."+
                    "\r\nThe zone will deactivate when the Activity Timeout expires."
            break
		case "2":
			return	"Zone is activated when any motion sensor activates within the Activation Window." +
            		"\r\nThe Activation Window is enabled by the Trigger Devices(s)." +
					"\r\nThe zone remains active while motion continues within the Activity Timeout." +
					"\r\nThe Activity Timeout is restarted on each motion sensor active event."+
                    "\r\nThe zone will deactivate when the Activity Timeout expires."
            break
 	}
}