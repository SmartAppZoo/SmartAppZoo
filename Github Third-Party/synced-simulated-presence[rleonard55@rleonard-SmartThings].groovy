/**
 *  Synced Simulated Presence Sensor
 *
 *  Copyright 2017 Rob Leonard
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
    name: "Synced Simulated Presence",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "syncs a simulated presence sensor with mobile presence sensors",
    category: "Convenience",
    iconUrl: "http://icons.iconarchive.com/icons/ampeross/qetto-2/128/sync-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/ampeross/qetto-2/128/sync-icon.png",
    iconX3Url: "http://icons.iconarchive.com/icons/ampeross/qetto-2/128/sync-icon.png")


preferences {
		input("presenceSensors", "capability.presenceSensor", title: "Presence sensor(s) to sync with", multiple: true, required: false)
        input("SimulatedPresenceSensor", "capability.presenceSensor", title: "Slave (Simulated) Device", multiple: false, required: true)
        input("SyncMinutes", "number", title: "Min(s) to wait before syncing the presence settings",range: "5..120", defaultValue:15)
        input("EnableDebug", "bool", title: "Enable debug messages", defaultValue: false)
}

def installed() {
	debugMsg("Installed with settings: ${settings}")

	initialize()
}

def updated() {
	debugMsg("Updated with settings: ${settings}")

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	debugMsg("Initializing with settings: ${settings}")
    
    subscribe(presenceSensors,"presence.present",OnPresenceArrive)
    subscribe(presenceSensors,"presence.not present",OnPresenceDepart)
    
    subscribe(SimulatedPresenceSensor,"presence",SimulatedPresenceChange)
    //subscribe(SimulatedPresenceSensor,"presence.present",SimulatedPresenceChange)
    //subscribe(SimulatedPresenceSensor,"presence.not present",SimulatedPresenceChange)

    PresenceSync()
}

def SimulatedPresenceChange(evt){
	runIn(60*settings.SyncMinutes,PresenceSync)
}

def OnPresenceArrive(evt) {
	debugMsg("Running 'OnPresenceArrive'")
	debugMsg("Value is : ${SimulatedPresenceSensor.currentValue("presence")}")
    
    PresenceSync()
    //runIn(60*settings.SyncMinutes,PresenceSync)
}

def OnPresenceDepart(evt) {
	debugMsg("Running 'OnPresenceDepart'")
	debugMsg("Value is : ${SimulatedPresenceSensor.currentValue("presence")}")
    
    PresenceSync()
    //runIn(60*settings.SyncMinutes,PresenceSync)
}

def PresenceSync() {
	debugMsg("Running 'PresenceSync'")
    unschedule()
    
    def result = presenceSensors.any {p -> 
        p.currentValue("presence") == "present" }
    
    if(result && SimulatedPresenceSensor.currentValue("presence") != "present")
    	SimulatedPresenceSensor.arrived()
    else if(!result && SimulatedPresenceSensor.currentValue("presence") == "present")
    	SimulatedPresenceSensor.departed()
}

private debugMsg(message) {

	if(settings.EnableDebug)
    	log.debug message
}