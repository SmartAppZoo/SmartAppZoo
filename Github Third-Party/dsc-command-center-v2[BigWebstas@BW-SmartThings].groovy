/**
 *  DSC Command Center
 *
 *  Copyright 2015
 *  Author: David Cauthron
 *  Also Attributed:  JTT-AE <aesystems@gmail.com>
 *                    Rob Fisher <robfish@att.net>
 *					  Carlos Santiago <carloss66@gmail.com> 
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
import groovy.json.JsonSlurper;

definition(
    name: "DSC Command Center v2",
    author: "David Cauthron",
    description: "Command Center SmartApp for DSC Alarms",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	preferences {
    	section("Alarm Server Settings") {
        	//Get IP & Port Address for the AlarmServer
            input("ip", "text", title: "IP", description: "The IP of your AlarmServer", required: true)
            input("port", "text", title: "Port", description: "The port", required: true)
            //Get Alarm Code
            input("alarmCodePanel", "text", title: "Alarm Code", description: "The code for your alarm panel.", required: false)
            //Allow user to turn off the Smart Monitor Integration if they arn't using it or use it for another purpose
            input "smartMonitorInt", "enum", title: "Integrate w/ Smart Monitor?", options: ["Yes", "No"], required: true
            //Allow user to turn off the Physical Panel Integration
            input "physicalMonitorInt", "enum", title: "Integrate w/ Physical Panels?", options: ["Yes", "No"], required: true
        }
        section("Button for Alarm") {
        	//Grab the DSC Command Switch
            input "thecommand", "capability.Switch", required: false
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "Version 2.1"
	log.debug "Initialize with settings: ${settings}"
    //Don't subscribe to the Smart Home Monitor status if user turned it off
    if(smartMonitorInt.value[0] != "N")
    {
    	//Subscribe to Smart Home Monitor
    	subscribe(location, "alarmSystemStatus", alarmStatusUpdate)
    }
    //Subscribe to button pushes within Device Switch
	subscribe(thecommand, "switch", switchUpdate)
    //Subscribe to responses from sendHubCommand
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
    if(physicalMonitorInt.value[0] != "N")
    {
    	runIn(15, checkAlarm)
    }
}

def checkAlarm() {
	log.debug "Check Alarm"
    runIn(15, checkAlarm)
    //Call Alarm Server to determine status
    callAlarmServer("/api")
}

//Sync changes on your physical alarm panels back to Smart Things.
//Unfortunately, we have to poll for these event changes which can take up to 5 minutes to sync.
//A call back would work, but Alarm Server would need to be overhauled because it doesn't send back the mode (away vs stay)
def lanResponseHandler(evt) {
    def jsonSlurper = new JsonSlurper()
    def systemArmed = false
    def systemEntryDelay = false
    def description = evt.description
    
    try {
        //Ensure we received at least 4 messages in a CSV format from the sendHubCommand Response
        if (description.count(",") > 4) {
            //Split and decode Base64 the response for the body
            def bodyString = new String(description.split(',')[6].split(":")[1].decodeBase64())
            def resp = jsonSlurper.parseText(bodyString)

            //Make sure I'm seeing a response from my API call and not a call to Arm/Disarm the alarm system
            if(resp.version != null)
            {
                log.debug "Syncing Physical Panel with Smartthings (if needed)"
                //Get Alarm Status (Armed (or Exit Delay) vs Disarmed) - If any partition is armed, the system is "armed"
                def partitions = resp.partition
                partitions.each {k, v -> 
                    if(v.status.armed || v.status.exit_delay) {
                        systemArmed = true
                    }
                    if(v.status.entry_delay) {
                        systemEntryDelay = true
                    }
                }

                //Run through the last event messages to determine if armed in Stay or Away mode
                //I dont like this method, but there is no other status that shows this information
                def messages = resp.partition.lastevents
                def found = false
                def filteredMsgs = messages.findAll {it.message.contains("Armed")}
                def lastMsg = filteredMsgs.last().message

                //If the systems entry delay is going off, let's wait to sync.
                if (!systemEntryDelay)
                {
                    //Sync!
                    if (!systemArmed) {
                        log.debug "Physical Panel Disarmed"
                        setCommandSwitch("disarm")
                        setSmartHomeMonitor("off")
                    }
                    else if(lastMsg.contains("Away")) {
                        log.debug "Physical Panel Armed in Away Mode"
                        setCommandSwitch("arm")
                        setSmartHomeMonitor("away")
                    }
                    else if(lastMsg.contains("Stay")) {
                        log.debug "Physical Panel Armed in Stay Mode"
                        setCommandSwitch("stayarm")
                        setSmartHomeMonitor("stay")
                    }
                }
            }
        }
    
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

//When a button is pressed in the DSC Command, this will capture the event and send that to Alarm Server.
//It will also sync the button press over to Smart Home Monitor
def switchUpdate(evt) {
	def eventMap = [
        'stayarm':"/api/alarm/stayarm",
        'disarm':"/api/alarm/disarm",
        'arm':"/api/alarm/armwithcode"
    ]
	
    def securityMonitorMap = [
        'stayarm':"stay",
        'disarm':"off",
        'arm':"away"
    ]
    
    def path = eventMap."${evt.value}"
    setSmartHomeMonitor(securityMonitorMap."${evt.value}")
	callAlarmServer(path)
}

//When a button is pressed in Smart Home Monitor, this will capture the event and send that to Alarm Server
//It will also sync the status change over to the DSC Command Switch
def alarmStatusUpdate(evt) {
	def eventMap = [
        'stay':"/api/alarm/stayarm",
        'off':"/api/alarm/disarm",
        'away':"/api/alarm/armwithcode"
    ]
	
    def securityMonitorMap = [
        'stay':"stayarm",
        'off':"disarm",
        'away':"arm"
    ]
    
    def command = securityMonitorMap."${evt.value}";
    setCommandSwitch(command)
    def path = eventMap."${evt.value}"
	callAlarmServer(path)
}

//Method to Call Alarmserver
private callAlarmServer(path) {
	try {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: path,
            headers: [
                HOST: "${ip}:${port}"
            ],
    		query: [alarmcode: "${alarmCodePanel.value}"]
        ))
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private setSmartHomeMonitor(status)
{
	//Let's make sure the user turned on Smart Home Monitor Integration and the value I'm trying to set it to isn't already set
	if(smartMonitorInt.value[0] != "N" && location.currentState("alarmSystemStatus").value != status)
    {
    	log.debug "Set Smart Home Monitor to $status"
    	sendLocationEvent(name: "alarmSystemStatus", value: status)
    }
}

private setCommandSwitch(command)
{
	//Let's make sure the switch isn't already set to that value
	if(thecommand.currentSwitch != command)
    {
    	log.debug "Set Command Switch to $command"
		thecommand."$command"()
    }
}