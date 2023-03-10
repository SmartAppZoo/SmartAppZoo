/**
 *  Copyright 2015 SmartThings
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
 *  My attempt at alarm manipulation on Sonos
 *
 *  Author: Oilerfan21
 */
definition(
    name: "Sonos Alarm Toggle",
    namespace: "oilerfan21",
    author: "oilerfan21",
    description: "Can I toggle the alarm?",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-UndeadEarlyWarning.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-UndeadEarlyWarning@2x.png"
)

preferences {
	section("When this button is pressed...") {
		input "switch1", "capability.switch", multiple: false, title: "Which button?"
	}
    section("Play message on this player") {
		input "sonos", "capability.musicPlayer", multiple: true, title: "where to say state"
	}
}

def installed()
{
	log.trace "installed with settings: ${settings}"
    initialize()
}

def updated()
{
	unsubscribe()
	initialize()
    log.trace "updated with settings: ${settings}"
}

def initialize()
{
	subscribe(switch1, "switch.on", switchHandler)
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def switchHandler(evt){
	log.debug "got a switch event"
    	state.firstpass = "1"
	BuildgetAction()
}
def sendAction(result){
	log.trace "what exactly am I sending? - ${result}"
    	def end = sendHubCommand(result)
	log.trace "command sent ${result}"

}

private BuildgetAction() {

	log.trace "building SOAP get request"
	def result = new physicalgraph.device.HubSoapAction(
		path:	 "/AlarmClock/Control",
        	urn:     'urn:schemas-upnp-org:service:AlarmClock:1',
		action:  "ListAlarms",
		body:    body,
		headers: [Host:"192.168.8.4:1400", CONNECTION: "close"]
	)
    
    log.trace "Is this our first time through this code? - ${firstpass}"

	sendAction(result)
    
	
}

def lanResponseHandler(evt) {
    def hdr = parseLanMessage(evt.description)
    log.trace "lan Response entry first pass status? - ${state.firstpass}"
    log.trace "header - ${hdr.header}"
    
    if (hdr.header){
    
    	log.trace "Looks like a valid HTTP response - reading Alarm response"
          	
        def env = new XmlSlurper().parseText(hdr.body)
     
        if (env.Body.ListAlarmsResponse.size()>0){
        	log.trace "looks like a valid Alarms list response - First time through? - ${state.firstpass}"
		if (state.firstpass == "1"){
			log.trace "changing state of firstpass"
                	state.firstpass = "0"
        		log.trace "getting the alarms and first pass is now ${state.firstpass}"
    			def alarms = new XmlSlurper().parseText(env.Body.ListAlarmsResponse.CurrentAlarmList.text())
                	alarms.children().each{ processAlarm(it) }
                	log.trace "finished running through each alarm"
    		}
            	else{
            		log.trace "stepping through"
            	}
	}
    }	
}

def processAlarm(alarm) {

        log.trace "Changing Alarm Status: ${alarm.name()}: ${alarm.@ID} - Enabled=${alarm.@Enabled}"
        
		if (alarm.@Enabled == 1){
			alarm.@Enabled = '0'
		}
        	else{
			alarm.@Enabled = '1'
		}
        
        log.trace "Alarm: ${alarm.name()}: ${alarm.@ID}, Changed to Enabled= ${alarm.@Enabled}"      	
        BuildsetAction(alarm)
}

private BuildsetAction(alarm) {

	log.trace "building SOAP Set request for Alarm - ${alarm.@ID}"
	def setresult = new physicalgraph.device.HubSoapAction(
		path:	 "/AlarmClock/Control",
        	urn:     'urn:schemas-upnp-org:service:AlarmClock:1',
		action:  "UpdateAlarm",
		body:   [ID: alarm.@ID, 
			StartLocalTime: alarm.@StartTime, 
			Duration: alarm.@Duration,
			Recurrence: alarm.@Recurrence,
			Enabled: alarm.@Enabled,
			RoomUUID: alarm.@RoomUUID,
              		ProgramURI: alarm.@ProgramURI,
			ProgramMetaData: alarm.@ProgramMetaData,
			PlayMode: alarm.@PlayMode,
			Volume: alarm.@Volume,
			IncludeLinkedZones: alarm.@IncludeLinkedZones
			],
		headers: [Host:"192.168.8.4:1400", CONNECTION: "close"]
	)
    
    //    log.trace "result - ${result}"

    sendAction(setresult)
    log.trace "sent follow-up get request"
    BuildgetAction()
    log.trace "sent set request"
}
