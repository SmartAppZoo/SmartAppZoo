/**
 *  Copyright 2017 SecurEndpoint
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
 *  Power Cycle
 *  
 *  V1.3 - changed downtime set method so a number is entered rather than a selection - Trying to fix Android issue 
 *  V1.2 - Added days of the week so can be scheduled to operate only on certain days
 *  V1.1 - Added daily scheduling
 *  V1.0 - Initial release
 *
 *  Last updated 07/05/2017
 *  Sets a schedule to turn off a switch,  after a preset number of seconds it will turn back on again - Used to power cycle some equipment daily
 *
 *  Author: COBRA
 */





definition(
    name: "Scheduled Power Cycle",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Schedule a switch to turn off then automatically turn it back on after a set number of seconds you specify.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png"
)

preferences {

	section() {
    paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                  //       required: false,
                  "Version: 1.3.0  - Copyright © 2017 Cobra"
    }
    section() {
    
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
                  title: "Scheduled Power Cycle",
                  required: false,
                  "Schedule a switch to turn off then automatically turn it back on after a set number of seconds you specify."
    }






	section("Which switch to cycle...") {
        input(name:"theSwitch", type: "capability.switch", required: true, multiple: true)
	}
	section("Power Cycle at...") {
		input (name: "rebootTime", title: "At what time?", type: "time",  required: true)
	}
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
    section("How long to stay off") {
		input(name:"secondsdelay", type:"number", title: "Seconds", required: true)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(rebootTime, rebootNow)
    
    
         
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	schedule(rebootTime, rebootNow)
   
    
   
   
}


def rebootNow(evt) {

 def df = new java.text.SimpleDateFormat("EEEE")
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    def dayCheck = days.contains(day)
    if (dayCheck) {


	log.debug "Cycling power on switch(es): $theSwitch "
    def delay1 = secondsdelay as int
    
    theSwitch.off()
    log.debug "switch is off"
    log.debug " waiting for ${delay1} seconds"
     
    runIn(delay1, switchOn)
    }
 
 else {
 log.debug "Not today!"
 }
}

def switchOn (){
log.debug "Switching back on"
theSwitch.on()
log.debug "$theSwitch is now on"
}