/**
 *  Severe Weather Alerts and Actions
 *
 *  Copyright 2020 Brian Beaird
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
    name: "Weather Alarm",
    namespace: "brbeaird",
    author: "Brian Beaird",
    description: "Simple weather alerts - trigger scenes when weather watches and warnings affect your location.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "configure")
}

def configure() {
	dynamicPage(name: "configure", title: "Configure Switch and Phrase", install: true, uninstall: true) {
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            actions.sort()
        }
        //location.helloHome?.execute(settings.ontornadoWatch)
        section("Title") {
            input "ontornadoWatch", "enum", title: "Routine to execute for Tornado Watch", options: actions, required: false
            input "ontornadoWarning", "enum", title: "Routine to execute for Tornado Warning", options: actions, required: false
            input "ontStormWatch", "enum", title: "Routine to execute for Thunderstorm Watch", options: actions, required: false
            input "ontStormWarning", "enum", title: "Routine to execute for Thunderstorm Warning", options: actions, required: false
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	//unsubscribe()
	initialize()
}

def initialize() {
	runEvery5Minutes(getCurrentAlerts)
}

def getCurrentAlerts(){
	log.debug "Checking for current alerts..."
    def alerts = getTwcAlerts("${location.latitude},${location.longitude}")
    //def alerts = []
    //def testWarning = [eventDescription: 'Tornado Watch']
    //alerts << testWarning
    log.debug alerts
    if (alerts) {
        alerts.each {alert ->
            if (alert.eventDescription == "Tornado Watch"){
            	if (soundAlarm("tornadoWatch")){location.helloHome?.execute(settings.ontornadoWatch)}
            }
            if (alert.eventDescription == "Tornado Warning"){
            	if (soundAlarm("tornadoWarning")){location.helloHome?.execute(settings.ontornadoWarning)}
            }
            if (alert.eventDescription == "Thunderstorm Watch"){
            	if (soundAlarm("tStormWatch")){location.helloHome?.execute(settings.ontStormWatch)}
            }
            if (alert.eventDescription == "Thunderstorm Warning"){
            	if (soundAlarm("tStormWatch")){location.helloHome?.execute(settings.ontStormWarning)}}
        	}
    } else {
        log.info "No current alerts"
        state.tornadoWatch = 'INACTIVE'
        state.tornadoWarning = 'INACTIVE'
        state.tStormWatch = 'INACTIVE'
        state.tStormWarning = 'INACTIVE'
    }
}

def soundAlarm(alertType){
    if (state[alertType] == 'ACTIVE'){return false}
    
    state[alertType]  = 'ACTIVE'
    def alertMessage = "${alertType} has been issued!"
    sendNotificationEvent(alertMessage)
    log.debug(alertMessage)
    sendPush(alertMessage)
    return true
}