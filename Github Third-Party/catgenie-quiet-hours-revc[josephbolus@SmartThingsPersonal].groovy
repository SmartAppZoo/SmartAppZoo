/**
 *  Pollster - The SmartThings Polling Daemon.
 *
 *  Many SmartThings devices rely on polling to update their status
 *  periodically. Pollster works behind the scenes and calls poll()
 *  function for selected devices so you don't have to rely on SmartThings
 *  built-in polling engine. Devices can be arranged in four groups with
 *  configurable polling intervals. The polling interval can be as short as
 *  one minute.
 *
 *  Copyright (c) 2014 Statusbits.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License  for the specific language governing permissions and limitations
 *  under the License.
 *
 *  The latest version of this file can be found at:
 *  https://github.com/statusbits/smartthings/blob/master/Pollster/Pollster.app.groovy
 *
 *  Revision History
 *  ----------------
 *
 *  2014-08-23: Version: 1.1.0
 *  Allow 4 device groups with different polling interval. 
 *
 *  2014-07-14: Version: 1.0.0
 *  Published to SmartThings shared apps directory.
 */

definition(
    name: "Catgenie Quiet Hours RevC",
    namespace: "statusbits",
    author: "geko@statusbits.com",
    description: "Calls poll() function periodically for selected devices.",
    category: "My Apps",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Bath.bath5-icn?displaySize=2x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Bath.bath5-icn?displaySize=2x")

preferences {

  	section ("Disable?") {
    	input "disableLogic", "bool", title: "Disable Logic?"
  	}

	section ("When this device stops drawing power") {
    	input "meter", "capability.powerMeter", multiple: false, required: true
        input "DeviceNotRunning", "number", title: "Device not running when power drops below (W)", description: "8", required: true
        input "timeBegin", "time", title: "Time of Day to start"
        input "timeEnd", "time", title: "Time of Day to stop"
 	}
    
    section("Turn off these switches..."){
		input "switches", "capability.switch", multiple: true
	}
    
  	section ("Debug?") {
    	input "debugMessages", "bool", title: "Debug Messages?"
  	}


    for (int n = 1; n <= 4; n++) {
        section("Polling Group ${n}") {
            input "group_${n}", "capability.polling", title:"Select devices to be polled", multiple:true, required:false
            input "interval_${n}", "number", title:"Set polling interval (in minutes)", defaultValue:5
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def pollingTask1() {
    TRACE("pollingTask1()")
    settings.group_1*.poll()
    
if (!disableLogic) {   
    
    def latestPower = meter.currentValue("power")
    log.debug "Power: ${latestPower}W"
    
    if (latestPower <= DeviceNotRunning){
    	state.deviceInStandby = 1
    } else {
    	state.deviceInStandby = 0
    }
    log.debug "state.deviceInStandby: ${state.deviceInStandby}"

    
    def now = new Date()
    def startCheck = timeToday(timeBegin)
    def stopCheck = timeToday(timeEnd)
    
    log.debug "now: ${now}"
    log.debug "startCheck: ${startCheck}"
    log.debug "stopCheck: ${stopCheck}"
    
    def between = timeOfDayIsBetween(startCheck, stopCheck, now, location.timeZone)
    log.debug "between: ${between}"
        
        
    if (state.deviceInStandby==1 && between){
    	switches?.off()
        
        if (debugMessages) {
        	sendNotificationEvent("ST Turned off the ${switches}")
        }
        
    } else {
    	if (state.deviceInStandby == 0) {
        	
            if (debugMessages) {
            	log.debug "Device was not turned off because: Not in standby"
            	sendNotificationEvent("Device was not turned off because: Not in standby")
            }
        }
        if (!between) {
        	
            if (debugMessages) {
        		log.debug "Device was not turned off because: Not between quiet hours"
        		sendNotificationEvent("Device was not turned off because: Not between quiet hours")
            }
        }
    }
    
  }
}

def pollingTask2() {
    TRACE("pollingTask2()")
    settings.group_2*.poll()
}

def pollingTask3() {
    TRACE("pollingTask3()")
    settings.group_3*.poll()
}

def pollingTask4() {
    TRACE("pollingTask4()")
    settings.group_4*.poll()
}

private def initialize() {
    TRACE("initialize() with settings: ${settings}")

    for (int n = 1; n <= 4; n++) {
        def minutes = settings."interval_${n}".toInteger()
        def group = settings."group_${n}"
        if (minutes > 0 && group?.size()) {
            TRACE("Scheduling polling task ${n} to run every ${minutes} minutes.")
            def sched = "0 0/${minutes} * * * ?"
            switch (n) {
            case 1:
                schedule(sched, pollingTask1)
                break;
            case 2:
                schedule(sched, pollingTask2)
                break;
            case 3:
                schedule(sched, pollingTask3)
                break;
            case 4:
                schedule(sched, pollingTask4)
                break;
            }
        }
    }
}

private def TRACE(message) {
    log.debug message
}