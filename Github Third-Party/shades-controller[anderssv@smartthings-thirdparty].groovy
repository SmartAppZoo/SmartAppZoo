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
 *  Based on Big Turn ON
 *
 *  Author: Luis Pinto
 */

definition(
    name: "Shades Controller",
    namespace: "smartthings",
    author: "Luis Pinto",
    description: "Control your shades with buttons.",
    category: "Convenience",
    iconUrl: "http://www.ezex.co.kr/img/st/window_close.png",
    iconX2Url: "http://www.ezex.co.kr/img/st/window_close.png"
)

preferences {
	section("Control Close Buttons...") {
		input "switchesOpen", "capability.button", multiple: true, title: "Open Buttons", required: true
		input "switchesClose", "capability.button", multiple: true, title: "Close Buttons", required: false
		input "switchesPause", "capability.button", multiple: true, title: "Pause Buttons", required: false
		input "dimmers", "capability.switchLevel", multiple: true, title: "Dimmers", required: false
		input "shades", "capability.switchLevel", multiple: true, title: "Shades"
		input "invertControl", "bool", title: "Invert controls"
	}

    section("Timers...") {
      input "theTimeOpen", "time", title: "Time to execute Open", required: false
      input "theTimeHalf", "time", title: "Time to execute 50%", required: false
      input "theTimeClose", "time", title: "Time to execute Close", required: false
	}
    
}

def installed()
{
	subscribe(switchesClose, "button", buttonEventClose)
	subscribe(switchesOpen, "button", buttonEventOpen)
	subscribe(switchesPause, "button", buttonEventPause)
	subscribe(dimmers, "switch.setLevel", dimmersEvent)
	subscribe(dimmers, "switch", dimmersEvent)
    subscribe(dimmers, "switch.on", dimmersEvent)
	subscribe(dimmers, "switch.off", dimmersEvent)
    
    if (theTimeOpen != null && theTimeOpen != "")
	    schedule(theTimeOpen, "handlerSchOpen")
    if (theTimeHalf != null && theTimeHalf != "")
    	schedule(theTimeHalf, "handlerSchHalf")
    if (theTimeClose != null && theTimeClose != "")
	    schedule(theTimeClose, "handlerSchClose")
}


def updated()
{
	unsubscribe()
    unschedule()
    installed()
}

def dimmersEvent(evt) {
	//shades[0].currentValue("level")
	log.info "switchSetLevelHandler Event: ${level}"
	if (evt.value == "on") {
    	shades.setLevel(100)
		return
    }
    if (evt.value == "off" ){
    	shades.setLevel(0)
		return
    }
	def level = evt.value.toFloat()
	level = level.toInteger()
    shades.setLevel(level)
}

def handlerSchOpen(evt) {
    shades.setLevel(0)
}

def handlerSchHalf(evt) {
    shades.setLevel(50)
}

def handlerSchClose(evt) {
    shades.setLevel(100)
}

def buttonEventPause(evt) {
	log.debug "Pausing Shades: $evt"
    shades.pause();
}

def buttonEventClose(evt) {
	log.debug "Closing Shades: $evt"
    
    if (isWorking())
    	shades.pause();
    else if ((evt.value == "held" && !invertControl) || (evt.value == "pushed" && invertControl)) {
    	log.debug "button was held"
        shades.setLevel(100)
  	} else if ((evt.value == "pushed" && !invertControl) || (evt.value == "held" && invertControl)) {
    	log.debug "button was pushed"
        if (getLevel() <100 && getLevel() >=75)
            shades.setLevel(100)
        else if (getLevel() <75 && getLevel() >=50)
            shades.setLevel(75)
        else if (getLevel() <50 && getLevel() >=25)
            shades.setLevel(50)
        else if (getLevel() <25 && getLevel() >=0)
            shades.setLevel(25)
  	}

}

def buttonEventOpen(evt) {
	log.debug "Opening shades: $evt"
   	if (isWorking())
    	shades.pause()
    else if (switchesClose == null)
		if (getLevel() < 50)
	        shades.setLevel(100)
        else
	        shades.setLevel(0)        	
    else if ((evt.value == "held" && !invertControl) || (evt.value == "pushed" && invertControl)) {
    	log.debug "button was held"
        shades.setLevel(0)
  	} else if ((evt.value == "pushed" && !invertControl) || (evt.value == "held" && invertControl)) {
        if (getLevel() <=100 && getLevel() >75)
            shades.setLevel(75)
        else if (getLevel() <=75 && getLevel() >50)
            shades.setLevel(50)
        else if (getLevel() <=50 && getLevel() >25)
            shades.setLevel(25)
        else if (getLevel() <=25 && getLevel() >0)
            shades.setLevel(0)
    }
}

def isWorking(){
	return false
//	return (shades[0].currentState("windowShade").value == "opening" || shades[0].currentState("windowShade").value == "closing")
}

def getLevel(){
	return shades[0].currentValue("level")
}
