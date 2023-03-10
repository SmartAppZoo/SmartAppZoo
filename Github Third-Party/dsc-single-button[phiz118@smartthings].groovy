/**
 *  DSC Single Button
 *
 *  Copyright 2015 David Cauthron
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
    name: "DSC Single Button",
    namespace: "phiz118",
    author: "David Cauthron",
    description: "Command Center SmartApp for DSC Alarms",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	preferences {
    	section("Alarm Server Settings") {
            input("ip", "text", title: "IP", description: "The IP of your AlarmServer")
            input("port", "text", title: "Port", description: "The port")
            input("alarmCodePanel", "text", title: "Alarm Code", description: "The code for your alarm panel.", required: true)
        }
        section("Button for Alarm") {
            input "theswitch", "capability.switch", required: true
        }
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(theswitch, "switch.on", switchUpdate)
	subscribe(theswitch, "switch.off", switchUpdate)
}

def switchUpdate(evt) {
	callAlarmServer(evt)
}

private callAlarmServer(evt) {
	try {
        def eventMap = [
          'on':"/api/alarm/arm",
          'off':"/api/alarm/disarm"
        ]
    
        def path = eventMap."${evt.value}"
        
        log.debug "Setting Parameters: ${params}"
        
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: path,
            headers: [
                HOST: "${ip}:${port}"
            ],
    		query: [alarmcode: "${alarmCodePanel.value}"]
        ))
        log.debug result
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

