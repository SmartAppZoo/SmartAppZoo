/**
 *  TestDateTime
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
    name: "TestDateTime",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "testing",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Title") {
		// TODO: put inputs here
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
	
    state.lastRun = ((new Date())-3).time
    
    if(!RanToday())
    	Run()
    else
    	log.debug "No Run"
        
    log.debug "Last Ran: ${formatLocalTime(state.lastRun)}"    
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

private Run() {
	state.lastRun = now()
    log.debug "Setting..${now()}"
}
private RanToday() {
	if(state.lastRun > midnight())
    	return true
    return false
}
private midnight() {
	return timeToday("00:00", location.timeZone).time
}
private formatLocalTime(time, format = "EEE, MMM-dd-yy h:mm a (zzz)") {
	// Standart ST format "yyyy-MM-dd'T'HH:mm:ssZ"
	if (time instanceof Long) {
		time = new Date(time)
	}
	if (time instanceof String) {
		//get UTC time
		time = timeToday(time, location.timeZone)
	}
	if (!(time instanceof Date)) {
		return null
	}
	def formatter = new java.text.SimpleDateFormat(format)
	formatter.setTimeZone(location.timeZone)
	return formatter.format(time)
}
