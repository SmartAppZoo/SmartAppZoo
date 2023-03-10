/**
 *  Cycle Switch Timer V1.2
 *
 *  Copyright 2017 Andrew Parker
 *
 *  The code for the days of the week is not mine - Unfortunately I cannot remember where it came from
 *	If this is your code please contact me so that I can credit you properly
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
    name: "Cycle Switch On Timer",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "Sets a switch on for a specified time then off for a specified time then on again... ",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
)
preferences {

section() {
   
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra.png",
                  //       required: false,
                  "Version: 1.3.0 - Brought to you by Cobra"
    }
    section() {
    
        paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/power.png",
                  title: "Cycle Switch On Timer",
                  required: false,
                  "Sets a switch on for a specified time then off for a specified time then on again... "
    }

    

	 section("Which switch to enable/disable App") {
        input(name:"switch1", type: "capability.switch", required: false, multiple: true)
	}
    
    
     section("Which days to run") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday": "Saturday", "Sunday": "Sunday"]
    }
      
     section("Which switch to cycle...") {
        input(name:"switch2", type: "capability.switch", required: true, multiple: true)
	}
    section("How long to stay on") {
		input(name:"ondelay", type:"number", title: "Minutes", required: true,)
	}
    section("How long to stay off") {
		input(name:"offdelay", type:"number", title: "Minutes", required: true,)
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
	 subscribe(switch1, "switch", switch1Handler)
     
    
}


// Config & actions

def switch1Handler(evt){
   state.currS1 = evt.value 
   log.trace " $switch1 is $state.currS1"
   if (state.currS1 != "off" ) {
   runswitchOnNow()
   }
    if (state.currS1 != "on" ) {
   switchOff()
}
}



def runswitchOnNow () {
if (state.currS1 != "off" ) {
 def df = new java.text.SimpleDateFormat("EEEE")
    
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    def dayCheck = days.contains(day)
    if (dayCheck) {


	log.debug "Cycling power on switch(es): $switch2 "
    def delay = 60 * ondelay as int
    switch2.on()
    log.debug "switch: $switch2 is on"
    log.debug " Waiting for ${delay} seconds before switching off"
 
    runIn(delay, switchOff)}
   
}
else {
 log.debug " Not today!"
 }
}



def switchOff (){
log.debug "Switching off"
switch2.off()
log.trace "$switch2 is now off"
switchbackOn()
}


def switchbackOn () {
 if (state.currS1 != "off" ) {
 def df = new java.text.SimpleDateFormat("EEEE")
    
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    def dayCheck1 = days.contains(day)
    if (dayCheck1) {


	log.debug "Turning on switch(es): $switch2 "
    def delay1 = 60 * offdelay as int
    log.debug " Waiting for ${delay1} seconds before switching on"
 
    runIn(delay1, switchOn)}
        
 else {
 log.debug " Not today!"
 }
}
}




def switchOn (){
log.trace "Switching back on"
runswitchOnNow ()

}