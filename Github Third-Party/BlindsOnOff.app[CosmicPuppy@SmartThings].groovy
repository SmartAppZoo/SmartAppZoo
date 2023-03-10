private def myVersion() { "v0.2.0-Alpha+029" }
/**
 *  Copyright 2015 Terry Gauchat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
	name: "Blinds On Off",
	namespace: "CosmicPuppy",
	author: "Terry Gauchat",
	description: "Tracks and Syncs single Switch Blinds as On/Off (Up/Down, Open/Closed).",
	category: "My Apps",
	iconUrl: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg",
	iconX2Url: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg",
    iconX3Url: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg"
)

/**
 * Frequently edited options, parameters, constants.
 */

/**
 * Flicker Sequence Values
 *    - How long to keep real Switch Off before turning it back On
 *    - How long to keep real Switch On before turning it back Off (unused currently).
 */
def flickOffDuration() { return 1000 }  // If Cloud unrelaible may need more or less than 1000 (1 second).
def flickOnDuration()  { return 15000 } // Currently unused. See TODO: in def flickRealSwitch.
 
/**
 * Disable specific level of logging by commenting out log.* expressions as desired.
 * NB: Someday SmartThings's live log viewer front-end should provide dynamic filter-by-level, right?
 */
private def myDebug(text) {
    log.debug myLogFormat(text) // NB: Debug level messages including the PIN number! Keep debug off mostly.
}
private def myTrace(text) {
    log.trace myLogFormat(text) // NB: Trace messages are farely minimal. Still helpful even if debug on.
}
private def myInfo(text) {
    log.info myLogFormat(text)  // NB: No usages in this program. TODO: Should some Trace be Info?
}
private def myLogFormat(text) {
    return "\"${app.label}\".(\"${app.name}\")[\"${myVersion()}\"]: ${text}"
}

private def myPause(millis) {
   def passed = 0
   def now = new Date().time
   myTrace "Pausing... at Now: $now."
   /* This loop is an impolite busywait. We need to be given a true sleep() method, please. */
   while ( passed < millis ) {
       passed = new Date().time - now
   }
   now = new Date().time
   myTrace "... DONE pausing at Now: $now."
   return NULL
}

/* Preferences */
preferences {
	page(name: "pagePreferences", install: true, uninstall: true) {
    	section {
    		paragraph: "${myVersion}"
    	}
		section("When this Virtual Blinds Switch is turned on / off:") {
			input (name:"virtualSwitch", type:"device.blindsVirtualSwitch", title: "Blinds VirtualSwitch", multiple: false, required: true)
		}
		section("Trigger off/on this Switch as needed:") {
			input (name:"realSwitch", type:"capability.Switch", multiple: false, required: true)
		}
        section("What are the real blinds' current physical State (on=up-open, off=down-closed)?:") {
        	input (name:"declaredBlindsState", type:"enum", multiple: false, required: false, options: ["on","off"])
        }
	}        
}

/* Special Private Methods */
private def processDeclaredBlindsState() {
	if( declaredBlindsState != NULL ) {
    	if( declaredBlindsState == "on" ) {
        	myTrace "Declaring and syncing ${virtualSwitch.displayName} as  onPhysical."
        	virtualSwitch.onPhysical()
            atomicState.blindsState = "on"
        } else {
            myTrace "Declaring and syncing ${virtualSwitch.displayName} as offPhysical."
        	virtualSwitch.offPhysical()
            atomicState.blindsState = "off"
        }
    }
}

private def flickRealSwitch() {
    myTrace "Sending real \"${realSwitch.displayName}.off()\"."
	realSwitch.off()
    virtualSwitch.updateOutletAsOff()
    myPause( flickOffDuration() )
    myTrace "Sending real \"${realSwitch.displayName}.on()\"."
    realSwitch.on()
    virtualSwitch.updateOutletAsOn()
    /* TODO: Consider myPause( flickOnDuration() ~= 15 seconds ) and then realSwitch.off() ?*/
}

/* Standard Methods */
def installed()
{   
	unsubscribe()
    processDeclaredBlindsState()
	subscribeToCommand(virtualSwitch, "switch.on", onHandler)
	subscribeToCommand(virtualSwitch, "switch.off", offHandler)
}

def updated()
{
	unsubscribe()
    processDeclaredBlindsState()
	subscribeToCommand(virtualSwitch, "on", onHandler)
	subscribeToCommand(virtualSwitch, "off", offHandler)
}

def onHandler(evt) {
    myTrace "Handler caught ON  request from virtualSwitch: ${evt.stringValue}"
    if( atomicState.blindsState == "on" ) {
    	myTrace "Blinds already  ON. Doing nothing"
    } else {
    	myTrace "Blinds are     OFF. Attempting real power flick."
        flickRealSwitch()
        atomicState.blindsState = "on"
        myTrace "Done power flick."
    }
}

def offHandler(evt) {
    myTrace "Handler caught OFF request from virtualSwitch: ${evt.stringValue}"
    if( atomicState.blindsState == "off" ) {
    	myTrace "Blinds already OFF. Doing nothing"
    } else {
    	myTrace "Blinds are      ON. Attempting real power flick."
        flickRealSwitch()
        atomicState.blindsState = "off"
        myTrace "Done power flick."
    }
}


/* =========== */
/* End of File */
/* =========== */