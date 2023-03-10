/**
 *  Alarm Notification Handler for use with Redloro's integration of Honeywell Vista 20p
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
    name: "Alarm Notification Handler",
    namespace: "philh30",
    author: "philh30",
    description: "Handles alarm notifications and Alexa integration for a Vista 20p partition",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
	iconX2Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
	iconX3Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
	singleInstance: false
)


preferences {
	section("Logging:") {
        //input "prefDebugMode", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: true
        input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "0" : "None",
        	    "1" : "Error",
        	    "2" : "Warning",
        	    "3" : "Info",
        	    "4" : "Debug",
        	    "5" : "Trace"
        	],
        	defaultValue: "3",
            displayDuringSetup: true,
        	required: false
        )
    }
    section("Alarm Panel Integration:") {
        input "alarmPanel", "capability.actuator", title: "Alarm Partition", multiple: false, required: true
    }
    section("Alexa Integration:") {
    	input "vStay", "capability.switch", title: "Virtual Stay Switch", multiple: false, required: false
        input "vAway", "capability.switch", title: "Virtual Away Switch", multiple: false, required: false
        input "vInstant", "capability.switch", title: "Virtual Instant Switch", multiple: false, required: false
        input "vDisarm", "capability.switch", title: "Virtual Disarm Switch", multiple: false, required: false
    }
    section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
	}
    section("Text to include in message"){
    	input "msgStart","text", title: "Message text", description: "(ie. Honeywell Partition 1:)", required: false
    }
    section("Send alerts for:"){
    	input "msgAlarm", "bool", title: "Alarm Triggered", required: false, defaultValue: false
        input "msgAlarmCancel", "bool", title: "Alarm Cancelled", required: false, defaultValue: false
        input "msgArm", "bool", title: "Partition Armed", required: false, defaultValue: false
        input "msgDisarm", "bool", title: "Partition Disarmed", required: false, defaultValue: false
    }
	/*section("Minimum time between messages (optional, defaults to every message)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}*/
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	state.loggingLevelIDE = 5
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
	unsubscribe()
	initialize()
}

def initialize() {
	logger("Initializing","trace")

    // Unsubscribe:
    unsubscribe()
        
    // Subscribe to device attributes:
    subscribe(alarmPanel, "dscpartition", handlePartition)
    if(vStay) {
    	subscribe(vStay, "switch", handleStay)
    }
    if(vAway) {
    	subscribe(vAway, "switch", handleAway)
    }
    if(vInstant) {
    	subscribe(vInstant, "switch", handleInstant)
    }
    if(vDisarm) {
    	subscribe(vDisarm, "switch", handleDisarm)
    }
    
    if((alarmPanel.latestState("dscpartition").value=="ready")||(alarmPanel.latestState("dscpartition").value=="notready")||(alarmPanel.latestState("dscpartition").value=="alarmcleared")) {
    	state.lastAlarmStatus="disarmed"
        logger("Last alarm status updated to ${state.lastAlarmStatus}","debug")
    } else if((alarmPanel.latestState("dscpartition").value=="arming")||(alarmPanel.latestState("dscpartition").value=="armedstay")||(alarmPanel.latestState("dscpartition").value=="armedaway")||(alarmPanel.latestState("dscpartition").value=="armedinstant")||(alarmPanel.latestState("dscpartition").value=="armedmax")) {
    	state.lastAlarmStatus="armed"
        logger("Last alarm status updated to ${state.lastAlarmStatus}","debug")
    } else if(alarmPanel.latestState("alarm")) {
    	state.lastAlarmStatus="alarm"
        logger("Last alarm status updated to ${state.lastAlarmStatus}","debug")
    }
}

def handlePartition(evt) {
    logger("handlePartition(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
 	
    //["ready", "notready", "arming", "armedstay", "armedaway", "armedinstant", "armedmax", "alarmcleared", "alarm"]
    
	switch (evt.value) {
    case "ready":
        logger("handlePartition(): alarm is ready to arm","debug")
        //sendMessage(evt)
        handleStates("disarmed", "off", "off", "off", "on")
        break
    case "notready":
        logger("handlePartition(): zones are faulted","debug")
        handleStates("disarmed", "off", "off", "off", "on")
        break
    case "arming":
        logger("handlePartition(): alarm is arming","debug")
        handleStates("armed", "off", "off", "off", "on")
        break
    case "armedstay":
        logger("handlePartition(): alarm is armed stay","debug")
        handleStates("armed", "on", "off", "off", "off")
        break
	case "armedaway":
        logger("handlePartition(): alarm is armed away","debug")
        handleStates("armed", "off", "on", "off", "off")
        break
    case "armedinstant":
        logger("handlePartition(): alarm is armed instant","debug")
        handleStates("armed", "off", "off", "on", "off")
        break
    case "armedmax":
        logger("handlePartition(): alarm is armed max","debug")
        handleStates("armed", "off", "off", "off", "off")
        break
    case "alarmcleared":
        logger("handlePartition(): alarm cleared","debug")
        handleStates("canceled", "off", "off", "off", "off")
        break
    case "alarm":
    	logger("handlePartition(): alarm","debug")
        handleStates("alarm", "off", "off", "off", "off")
        break
    default:
        logger("handlePartition(): unhandled event: ${evt.value}","debug")
	}
}

def handleStay(evt) {
    logger("handleStay(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
 
 	switch (evt.value) {
    case "on":
        if(alarmPanel.latestState("dscpartition").value!="armedstay")
        {
        	logger("handleStay(): turning on arm stay","debug")
            alarmPanel.armStay()
        }
        break
	default:
    	logger("handleStay(): unhandled event: ${evt.value}","debug")
    }
}

def handleAway(evt) {
    logger("handleAway(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
 
 	switch (evt.value) {
    case "on":
        if(alarmPanel.latestState("dscpartition").value!="armedaway")
        {
        	logger("handleAway(): turning on arm away","debug")
            alarmPanel.armAway()
        }
        break
	default:
    	logger("handleAway(): unhandled event: ${evt.value}","debug")
    }
}

def handleInstant(evt) {
    logger("handleInstant(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
    
	switch (evt.value) {
    case "on":
        if(alarmPanel.latestState("dscpartition").value!="armedinstant")
        {
        	logger("handleInstant(): turning on arm instant","debug")
            alarmPanel.armInstant()
        }
        break
	default:
    	logger("handleInstant(): unhandled event: ${evt.value}","debug")
    }
}

def handleDisarm(evt) {
    logger("handleDisarm(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
    
	switch (evt.value) {
    case "on":
        if((alarmPanel.latestState("dscpartition").value!="ready")&&(alarmPanel.latestState("dscpartition").value!="notready"))
        {
        	logger("handleDisarm(): disarming","debug")
            alarmPanel.disarm()
        }
        break
	default:
    	logger("handleDisarm(): unhandled event: ${evt.value}","debug")
    }
}

private handleStates(newStatus, stay, away, instant, disarm)
{
	def msg
    if(settings.msgStart) {
    	msg=settings.msgStart
    } else
    {
		msg="Security panel"
    }
	if((newStatus!="alarm")&&(newStatus!="canceled"))
    {
	    if(vStay) {
        	if(vStay.latestState("switch").value!=stay)
	    	{
	       		if(stay=="on")
	        	{
	        		vStay.on()
	        	} else
	        	{
	        		vStay.off()
	        	}
	    	}
        }
        if(vAway) {
        	if(vAway.latestState("switch").value!=away)
	    	{
	        	if(away=="on")
	        	{
	        		vAway.on()
	        	} else
	        	{
	        		vAway.off()
	        	}
	    	}
        }
	    if(vInstant) {
        	if(vInstant.latestState("switch").value!=instant)
	    	{
	     		if(instant=="on")
	        	{
	        		vInstant.on()
        		} else
        		{
        			vInstant.off()
        		}
    		}
        }
        if(vDisarm) {
        	if(vDisarm.latestState("switch").value!=disarm)
	    	{
	     		if(disarm=="on")
	        	{
	        		vDisarm.on()
        		} else
        		{
        			vDisarm.off()
        		}
    		}
        }
    }
    if(state.lastAlarmStatus!=newStatus)
    {
    	switch (newStatus) {
    	case "disarmed":
	    	if(msgDisarm){
            	sendMessage("${msg} disarmed")
            }
    		break
    	case "armed":
        	if(msgArm){
	    		sendMessage("${msg} armed")
    		}
            break
    	case "alarm":
    		if(msgAlarm){
            	sendMessage("${msg} alarm triggered")
    		}
            break
        case "canceled":
    		if(msgAlarmCancel){
            	sendMessage("${msg} alarm canceled")
    		}
            break
    	default:
        	logger("Unhandled new alarm status: ${newStatus}","warn")
    	}
    }
    
    state.lastAlarmStatus=newStatus
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

private sendMessage(msg) {
	Map options = [:]

	logger("Sending:$msg, pushAndPhone:$pushAndPhone","debug")


	if (phone) {
		options.phone = phone
		if (pushAndPhone != 'No') {
			logger("Sending push and SMS","debug")
			options.method = 'both'
		} else {
			logger("Sending SMS","debug")
			options.method = 'phone'
		}
	} else if (pushAndPhone != 'No') {
		logger("Sending push","debug")
		options.method = 'push'
	} else {
		logger("Sending nothing","debug")
		options.method = 'none'
	}
    logger("about to send ${msg}","debug")
	sendNotification(msg, options)
	
	/*if (frequency) {
		state[evt.deviceId] = now()
	}*/
}
