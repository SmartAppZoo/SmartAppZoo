/**
     *  Wake up Light
     *
     *  Copyright 2016 Dieter Rothhardt
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
        name: "Wake-up Light",
        namespace: "dasalien",
        author: "Dieter Rothhardt",
        description: "Switch on switch at a certain time every weekday",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
    
    preferences {
    	section("At this time every day...") {
    		input "theTime", "time", title: "Time to execute every weekday"            
    	}
    	section("... turn on these switches") {
    		input "theSwitch", "capability.switch", title: "Which?", required: true, multiple: true
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
	schedule(theTime, handler)
}

def handler() {
    log.debug "handler called at ${new Date()}"
    def day = Date.parse("yyyy-MM-dd", "2011-03-07").format("EEE")
    if (!(day == "Sat") || (day == "Sun")) {
    	theSwitch.on()
    }
}