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
 *  Dimmers Time Based Filter (from original code of Left it Open and Gentle wake up)
 *
 *  Author: SmartThings and Modified by Mariano Colmenarejo 2021-06-08
 *  Date: 2013-05-09
 */
definition(
    name: "Dimmers Time Based Filter",
    namespace: "smartthings",
    author: "SmartThings Mod By MCC",
    description: "Set dimmers level according to time based or Mode filter",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png",
)

preferences {

  section("Select Dimmers to Control") {
    input(name: "dimmers", type: "capability.switchLevel", title: "Dimmers to Control", description: null, multiple: true, required: true, submitOnChange: true)
  }

  section("Select Dimmers levels for Min Level and Max Level Periods") {
    input(name: "MinDimming", type: "number", title: "% Level for Min. Level Period", description: "25", required: false, defaultValue: 25)
    input(name: "MaxDimming", type: "number", title: "% Level for Max. Level Period", description: "100", required: false, defaultValue: 100)
  }

  section("Select Start and Stop Time for Min. Level Period or Location Mode for Min. Level Period") {
			input(name: "startTime", type: "time", title: "Start Time for Min. Level Period", description: null, required: false)
            input(name: "stopTime", type: "time", title: "Stop Time for Min. Level Period", description: null, required: false)
			input(name: "modeStart", title: "Mode for Min. Level Period", type: "mode", required: false, mutliple: false, submitOnChange: true, description: null)
		}
}

def installed() {
  log.trace "installed()"
  subscribe()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  subscribe()
}

def subscribe() {
  subscribe(dimmers, "switch.on", SwitchOn)
}

//*** Detect dimmer On ***
def SwitchOn (evt) {
 log.debug "starTime= $startTime"
 log.debug "stopTime= $stopTime"
 log.debug "modeStart= $modeStart"
 log.debug "location.currentMode= $location.currentMode"
 
 //Detect mode valid
 def modeValid = 0
 if (location.currentMode == modeStart || modeStart == null) {
  modeValid = 1
 } 
 log.debug "modeValid = $modeValid"
 
 //Detect time period valid
 def timeValid = 0
   def between = timeOfDayIsBetween(startTime, stopTime, new Date(), location.timeZone)
    if (between) {
        timeValid = 1
    } 
 log.debug "timeValid = $timeValid"

 // set level for dimmer is on
 def NextLevel = 100
 if (modeValid == 1 || timeValid == 1) {
  NextLevel = MinDimming
 } else {
  NextLevel = MaxDimming
 }
 log.debug "NextLevel= $NextLevel"

for (int i = 0; i < dimmers.size(); i++) {
   //log.debug "$dimmers[i].displayName= $evt.displayName = $evt.displayName"
   if (dimmers[i].displayName == evt.displayName ) { dimmers[i].setLevel(NextLevel) }
 }   
}
