/**
 *  Copyright 2016 Leon Schwartz.  All rights reserved.
 *
 *  
 *  This SmartApp allows the use of lights controlled by smart dimmers as nightlights, assuming that the dimmers are using a modified device handler
 *	that contains methods for running the dimmers as nightlights and resetting them in the morning.  Look for the My Dimmer Switch device type.
 *
 *  Author: Leon Schwartz
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
    name: "Nightlights On (Leviton)",
    namespace: "octadox",
    author: "Leon Schwartz",
    description: "Dim lights for nighttime and reset them in the morning, with optional turn-off prevention and optional presence condition",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light18-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light18-icn@2x.png",
    //iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	page(name: "selectDimmers", title: "Select Dimmers", uninstall: true, nextPage: "selectRoutines")
    {
    	 section("Dimmers") {
            input "nightlightSwitches", title: "Select dimmers to use as nightlights", "capability.switchLevel", multiple: true
         	input "limitSwitches", title: "Select dimmers to keep off, but limit how bright they turn on at night", "capability.switchLevel", required: false, multiple: true
         }
         section("Presence") {
         	input "presenceTrigger", title: "Only turn on if this presence sensor is present?", "capability.presenceSensor", required: false
         	input "keepOn", "bool", title: "Prevent the lights from being turned off at the switch while in nightlight mode?"
         }
    }
    page(name: "selectRoutines", nextPage: "pageThree")
    page(name: "pageThree", title: "Name app and configure modes", install: true, uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
    }
}

def selectRoutines() {
    dynamicPage(name: "selectRoutines", title: "Select Routines to listen to", uninstall: true) {

        // get the available routines
        def routines = location.helloHome?.getPhrases()*.label
        if (routines) {
            // sort them alphabetically
            routines.sort()
            // select routine(s) that turn the lights on as dimmers
            section("Routines that turn ON nightlights") {
                input "routinesForOn", "enum", title: "Select a routine to listen to", options: routines, multiple: true
            }
            // select routine(s) that turn the lights off (after reset)
            section("Routines that turn OFF nightlights") {
                input "routinesForOff", "enum", title: "Select a routine to listen to", options: routines, multiple: true
            }
		}
    }
}

def installed()
{
	//log.debug "nightlights on: installed"
    initialize()
}

def updated()
{
	//log.debug "nightlights on: updated" 
    unsubscribe()
	unschedule()
    initialize()
}

def initialize() {
    //log.debug "nightlights on: initialize"
    subscribe(app, appTouch)
    subscribe(location, "routineExecuted", routineChanged)
    subscribe(nightlightSwitches, "switch.off", nightlightTurnedOffHandler)
    subscribe(limitSwitches, "switch.off", limitTurnedOffHandler)
    //Initial state of nightlight flag is false (meaning nightlights not on).
    state.IsNightLightMode = false
}

//Toggle the current state if the application button is pushed (if currently on as a nightlight, reset, otherwise, turn on as a nightlight).
//This should not be used on a regular basis, should be triggered by routines running instead, but this is here for overrides and such.
def appTouch(evt) {
	//log.debug "appTouch: $evt"
    if ( state.IsNightlightMode )
    {
    	turnOff()
    }
    else
    {
    	turnOn()
    }
}

//This handler is used by the optional prevention of turning the nightlights off.
def nightlightTurnedOffHandler (evt)
{
	/*if (state.IsNightlightMode && keepOn)
    {
    	evt.device.nightlightDim()
    }
    */
}

//This handler is used by the optional prevention of turning the nightlights off.
def limitTurnedOffHandler (evt)
{
	/*if (state.IsNightlightMode && keepOn)
    {
    	//log.debug(evt.device.currentLevel)
        if ( evt.device.currentLevel > 30 )
        {
        	evt.device.nightlightLimit()
        }
    }
    */
}

//This handler is used to turn the nightlights on
def turnOn() {
	//log.debug "reset dimmers: turning off night lights"
    //log.debug "turning on dimmers!!!"
    def override = false;
    if (presenceTrigger != null)
    {
 		if (presenceTrigger.currentPresence != "present")
		{
        	override = true 
    	}
    }
    if (!override)
    {
    	nightlightSwitches?.localNightlightDim()
        //limitSwitches?.localNightlightLimit()
        state.IsNightlightMode = true
    }
}

//This handler is used to turn the nightlights off (after reset)
def turnOff() {
    state.IsNightlightMode = false
	//log.debug "reset dimmers: turning off night lights"
    for (dimmer in nightlightSwitches)
    {
    	//dimmer.off()
        dimmer.resetLevel()
    }
    	//log.debug "reset dimmers: turning off night lights"
    for (dimmer in limitSwitches)
    {
 	    //dimmer.off()
        dimmer.resetLevel()
    }
}

//This handler is used to listen for routines to turn the lights on or off.
def routineChanged(evt) {
   	if (evt.name == "routineExecuted") {
        log.debug ("routine: ${evt.displayName}")
        for (routine in routinesForOn)
    	{
        	if (evt.displayName == routine) {
    			turnOn()
       		}
    	}
        for (routine in routinesForOff)
    	{
        	if (evt.displayName == routine) {
    			turnOff()
       		}
    	}
    }
}
