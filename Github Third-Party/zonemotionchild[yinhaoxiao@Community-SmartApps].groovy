/**
 *  zoneMotionChild v 2.0.1 2015-12-30
 *
 *  Copyright 2015-10-20 Mike Maxwell
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
 	2.0.1 2015-12-30 fixed agg zone types incorrectly going inactive when a zone motion remained for longer than the timeout setting
 */
 
definition(
    name: "zoneMotionChild",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "child application for 'Zone Motion Manager', do not install directly.",
    category: "My Apps",
    parent: "MikeMaxwell:Zone Motion Manager",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "main")
    page(name: "triggers", nextPage	: "main")
}
def installed() {
	log.debug "Installed with settings: ${settings}"
	//initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}
def initialize() {
	state.nextRunTime = 0
	state.zoneTriggerActive = false
	subscribe(motionSensors, "motion.inactive", inactiveHandler)
    subscribe(motionSensors, "motion.active", activeHandler)
    app.updateLabel("${settings.zoneName} Zone Controller") 
    def hub = location.hubs[0]
    def deviceID = "${app.id}/wonk"
    def zName = "mZone-${settings.zoneName}"
    //def simMotion = getChildDevice(deviceID)
    if (!simMotion) {
    	//log.info "create the virtual motion sensor"
        //simMotion = addChildDevice("MikeMaxwell", "simulatedMotionSensor", deviceID, hub.id, [name: zName, label: zName, completedSetup: true])
        //simMotion.inactive()
    } else {
    	log.info "virtual motion sensor exists"
        simMotion.inactive()
    }
}
def activityTimeoutHandler(evtTime,device){
	def timeout = settings.zoneTimeout.toInteger()
    def text = ","
    if (state.nextRunTime > 0){
    	def timeoutRemaining = (state.nextRunTime - evtTime) / 1000
        text = ", (${timeoutRemaining.toInteger()} seconds remaining)," 
    }
    log.debug "Zone: ${simMotion.displayName} is active via [${device}]${text} zone timeout reset to ${timeout} seconds..."
    state.nextRunTime = evtTime + (timeout * 1000) 
    runIn(timeout,zoneOff)
}

//False motion reduction
def allMotionsActive(evtTime){
	def enable
    def window = settings.activationWindowFD.toInteger()
	enable = motionSensors.currentState("motion").every{ s -> s.value == "active" && (evtTime - s.date.getTime()) < window}
	//log.debug "allMotionsActive:${enable}"
    if (!enable) log.trace "False Motion Detected!"
    return enable
}

//Triggered Activation
def anyTriggersActive(evtTime){
	def enable = false
    def window = settings.activationWindowTA.toInteger()
    def evtStart = new Date(evtTime - window)
	//log.trace "tm:${triggerMotions.inspect()}"
	//log.trace "tc:${triggerContacts.inspect()}"
    if (triggerMotions){
    	//def states = triggerMotions.statesSince("motion", evtStart)
        //triggerMotions.each{s ->
        //	def st = s.latestState("motion")
        //	log.trace "tm: ${s.displayName} ${st.date.format('yyyy-MM-dd HH:mm:ss')}"
        //}
    	//log.trace "tm:${triggertMotions}"
    	enable = triggerMotions.any{ s -> s.statesSince("motion", evtStart).size > 0}
        //log.debug "triggerMotions:${enable}"
    }
    if (!enable && triggerContacts){
    	enable = triggerContacts.any{ s -> s.statesSince("contact", evtStart).size > 0}
        //log.debug "triggerContacts:${enable}"
    }
    if (!enable && triggerSwitches){
    	enable = triggerSwitches.any{ s -> s.statesSince("switch", evtStart).size > 0}
        //log.debug "triggerSwitches:${state}"
    }
    if (!enable) log.trace "Qualifying triggers were not Detected!"
    //log.debug "anyTriggersActive - final:${enable}"
    return enable
}
def activeHandler(evt){
 
    log.trace "active handler fired via [${evt.displayName}] UTC: ${evt.date.format("yyyy-MM-dd HH:mm:ss")}"
	def evtTime = evt.date.getTime()
    //log.trace "active handler evt.date+3:${new Date(evtTime)}"
    
    def device = evt.displayName
    if (modeIsOK()) {
  		switch (settings.zoneType) {
    		//False motion reduction
			case "0":
        		if (allMotionsActive(evtTime)) zoneOn() 
				break
        	//Motion Aggregation
			case "1":
        		zoneOn()
        		activityTimeoutHandler(evtTime,device)
	        	break
        	//Triggered Activation
			case "2":
        		if (!state.zoneTriggerActive && anyTriggersActive(evtTime)){
            		zoneOn()
                	activityTimeoutHandler(evtTime,device)
                	state.zoneTriggerActive = true
            	} else if (state.zoneTriggerActive){
            		activityTimeoutHandler(evtTime,device)
            	}
	        	break
 		}
    } else {
    	log.debug "modeOK: False"
    }
}
def inactiveHandler(evt){
	if (settings.zoneType == "0" && allInactive()){
    	zoneOff()
    }
}
def zoneOn(){
    //def simMotion = getChildDevice("${app.id}/wonk")
	if (simMotion.currentValue("motion") != "active") {
		log.info "Zone: ${simMotion.displayName} is active."
		simMotion.active()
   	}
}
def zoneOff(){
    //def simMotion = getChildDevice("${app.id}/wonk")
	if (simMotion.currentValue("motion") != "inactive") {
    	//check for all inactive
        if (allInactive()){
        	state.nextRunTime = 0
        	log.info "Zone: ${simMotion.displayName} is inactive."
        	state.zoneTriggerActive = false
			simMotion.inactive()
        } else {
        	def timeout = settings.zoneTimeout.toInteger()
            def evt = new Date()
        	def evtTime = evt.getTime()
            def active = motionSensors.findAll{ it.currentValue("motion") == "active" }
        	log.debug "Zone: ${simMotion.displayName} is still active via ${active}, check again in ${timeout} seconds..."
    		state.nextRunTime = evtTime + (timeout * 1000) 
    		runIn(timeout,zoneOff)
        }
    }
}
def allInactive() {
	//log.debug "allInactive:${motionSensors.currentValue("motion")}"
	def state = motionSensors.currentState("motion").every{ s -> s.value == "inactive"}
    //log.debug "allInactive: ${state}"
	return state
}
def modeIsOK() {
	def result = !modes || modes.contains(location.mode)
	return result
}
/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
    def zType = settings.zoneType
    //log.info "Installed:${installed} zoneType:${zType}"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){
		     section(){
             		
                   input(
                        name		: "simMotion"
                        ,title		: "Virtual Motion Sensor for this Zone:"
                        ,multiple	: false
                        ,required	: false
                        ,type		: "capability.motionSensor"
                    )
                    
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
                        ,options				: [[0:"False Motion Reduction"],[1:"Motion Aggregation"],[2:"Triggered Activation"]]
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
                			,options		: [[1000:"1 Second"],[1500:"1.5 Seconds"],[2000:"2 Seconds"],[2500:"2.5 Seconds"],[3000:"3 Seconds"],[4000:"4 Seconds"],[5000:"5 Seconds"],[6000:"6 Seconds"],[7000:"7 Seconds"],[8000:"8 Seconds"],[9000:"9 Seconds"],[10000:"10 Seconds"],[60000:"1 Minute"],[120000:"2 Minutes"],[180000:"3 Minutes"],[240000:"4 Minutes"],[300000:"5 Minutes"],[600000:"10 Minutes"]]
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
            } //end section optional settings
	}
}
def triggers(){
	return dynamicPage(
    	name		: "triggers"
        ,title		: "Trigger devices"
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