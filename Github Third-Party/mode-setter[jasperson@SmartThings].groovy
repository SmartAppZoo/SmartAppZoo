/*
 *  Mode Setter
 *
 *  Copyright 2016 J.R. Jasperson
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
 *  Initially based on: https://github.com/imbrianj/nobody_home/blob/master/nobody_home.groovy
 */

definition(
    name:			"Mode Setter",
    namespace:		"jasperson",
    author:			"J.R. Jasperson",
    description:	"Set the SmartThings location mode based on presence & sunrise/sunset",
    category:		"Mode Magic",
    iconUrl:		"http://cdn.device-icons.smartthings.com/Weather/weather14-icn.png",
    iconX2Url:		"http://cdn.device-icons.smartthings.com/Weather/weather14-icn@2x.png",
    iconX3Url:		"http://cdn.device-icons.smartthings.com/Weather/weather14-icn@3x.png"
)

// Presented to user on app installation/update for configuration
preferences{
    section("Presence Sensors"){
        input "people", "capability.presenceSensor", multiple: true
    }
    section("Mode Settings"){
		input "awayDayMode",		"mode",		title: "Everyone is away during the day"
        input "awayNightMode",		"mode",		title: "Everyone is away at night"
        input "presentDayMode", 	"mode",		title: "Someone is present during the day"
        input "presentNightMode",	"mode",		title: "Someone is present at night"
        input "ignoreMode",			"mode",		title: "Ignore state changes if in this mode"
    }
    section("Mode Change Delay (minutes)"){
        input "awayThreshold",		"decimal",	title: "Away delay [5m]",		required: false
        input "arrivalThreshold",	"decimal",	title: "Arrival delay [0m]",	required: false
    }
    section("Notifications"){
        input "sendPushMessage",	"bool",		title: "Push notification",		required:false
        // JR TODO: log level in push mode?
        // JR TODO: http://docs.smartthings.com/en/latest/ref-docs/smartapp-ref.html#sendnotificationtocontacts ?
    }
    // JR TODO: SMS?
}

// Invoked on app install
def installed(){
    send("installed() @${location.name}: ${settings}", "debug")
    initialize(true)
}

// Invoked on app update/save
def updated(){
    send("updated() @${location.name}: ${settings}", "debug")
    unsubscribe()
    initialize(false)
}

// Invoked by installed() and updated()
def initialize(isInstall){
    // Subscriptions, attribute/state, callback function
    subscribe(people,   "presence", presenceHandler)
    subscribe(location, "sunrise",  sunriseHandler)
    subscribe(location, "sunset",   sunsetHandler)

	// Default any unspecified optional parameters and set state 
    if (settings.awayThreshold == null){
        settings.awayThreshold = 5
    }
    state.awayDelay = (int) settings.awayThreshold * 60
    send("awayThreshold set to ${state.awayDelay} second(s)", "debug")

    if (settings.arrivalThreshold == null){
        settings.arrivalThreshold = 0
    }
    state.arrivalDelay = (int) settings.arrivalThreshold * 60
    send("arrivalThreshold set to ${state.arrivalDelay} second(s)", "debug")

    state.isPush = settings.sendPushMessage ? true : false
    send("sendPushMessage set to ${state.isPush}", "debug")
  
    // Determine time (with respect to sunrise/sunset) and initialize mode state
	Date now = new Date()   
    def sunInfo = getSunriseAndSunset()
    send("Sunrise defined as: ${sunInfo.sunrise}", "debug")
    send("Sunset defined as: ${sunInfo.sunset}", "debug")

	if (now >= sunInfo.sunrise && now < sunInfo.sunset){	// Day modes
    	state.modeIfPresent = settings.presentDayMode
        state.modeIfAway = settings.awayDayMode
        send("Setting day modes", "debug")
    }
    else {													// Night modes
        state.modeIfPresent	= settings.presentNightMode
        state.modeIfAway	= settings.awayNightMode
        send("Setting night modes", "debug")
    }
    def present = getPresent()
    // JR TODO: Pull next block into getPresent?
    if (present.string){
    	send("${present.string} present", "debug")
    }
    else{
        send("No devices are present", "debug")
    }
	// JR TODO: log level?
	// JR TODO: SMS?

    // Now change the mode
    changeMode()

// Executes during installed() invocations
    if (isInstall){
        state.eventDevice = ""		// Device that generated the last event
        state.timerDevice = null	// Device that triggered timer (not necessarily eventDevice)

        // Set pending operation in state to avoid incorrectly extending timers
        state.pendingOp = "init"

        // Schedule setInitialMode to install faster and reference custom app name in notificaiton
        runIn(7, "setInitialMode")
    }
}

// Scheduled invocation by initialize() on install only
def setInitialMode(){
    changeMode()
    state.pendingOp = null
}

// Invoked at sunrise via subscription
def sunriseHandler(evt){
    state.modeIfPresent	= settings.presentDayMode
    state.modeIfAway	= settings.awayDayMode
    changeMode()
}

// Invoked at sunset via subscription
def sunsetHandler(evt){
    state.modeIfPresent	= settings.presentNightMode
    state.modeIfAway	= settings.awayNightMode
    changeMode()
}

// Invoked by setInitialMode (initialize() on install) and {sunrise, sunset}Handler
def changeMode(){
	def present = getPresent()
	if (! present.count){
    	setMode(state.modeIfAway, " because no one is present")
    }
    else {
    	setMode(state.modeIfPresent, " because ${present.string} is present")
    }
}

// Invoked when a selected presence sensor changes state
def presenceHandler(evt){
    // Set state.eventDevice to the device name that changed state
    state.eventDevice= evt.device?.displayName

    // Ignore if setInitialMode() has not yet completed
    if (state.pendingOp == "init"){
        send("Pending ${state.pendingOp} op still in progress, ignoring presence event", "info")
        return
    }

    if (evt.value == "not present"){
        handleDeparture()
    }
    else {
        handleArrival()
    }
}

// Invoked by presenceHandler when a selected presence sensor changes state to "not present"
def handleDeparture(){
    send("${state.eventDevice} left ${location.name}", "info")

	def present = getPresent()
	if (present.count){
        send("${present.string} present, no action required", "info")
        return
    }

    // Now we set away mode. We perform the following actions even if
    // home is already in away mode because an arrival timer may be
    // pending, and scheduling delaySetMode() has the nice effect of
    // canceling any previous pending timer, which is what we want to
    // do. So we do this even if delay is 0.
    send("Scheduling ${state.modeIfAway} mode in ${state.awayDelay} second(s)", "info")
    state.pendingOp = "away"
    state.timerDevice = state.eventDevice
   
   // we always use runIn(). This has the benefit of automatically
    // replacing any pending arrival/away timer. if any arrival timer
    // is active, it will be clobbered with this away timer. If any
    // away timer is active, it will be extended with this new timeout
    // (though normally it should not happen)
    runIn(state.awayDelay, "delaySetMode")
}

// Invoked by presenceHandler when a selected presence sensor changes state to "present"
def handleArrival(){
    send("${state.eventDevice} arrived at ${location.name}", "info")

    def present = getPresent()
    if (! present.count){
        // No one present, do nothing for now (should NOT happen)
        send("${state.eventDevice} arrived, but getPresent() returned false on count!", "warn")
        return
    }

    if (present.count > 1){
		// Not the first one present, do nothing, as any action that should happen
        // would have already been triggered
        send("${present.string} present, no action required", "info")
        return
    }

    // Check if any pending arrival timer is already active. we want
    // the timer to trigger when the first person arrives, but not
    // extended if a secondperson arrives later. This should not
    // happen because of the >1 check above, but just in case.
    if (state.pendingOp == "arrive"){
        send("Pending ${state.pendingOp} op already in progress, do nothing", "info")
        return
    }

    // Schedule arrival timer
    send("Scheduling ${state.modeIfPresent} mode in ${state.arrivalDelay} second(s)", "info")
    state.pendingOp = "arrive"
    state.timerDevice = state.eventDevice
    
    // if any away timer is active, it will be clobbered with this arrival timer
    runIn(state.arrivalDelay, "delaySetMode")
}

// ********** helper functions **********

// Evaluate and change the system to the new mode if necessary
def setMode(newMode, reason=""){
    if (location.mode == settings.ignoreMode){
        send("${location.name} is in ignore mode: ${location.mode}", "info")
        return
    }
    else if (location.mode != newMode){
        setLocationMode(newMode)
        send("${location.name} changed mode from ${location.mode} to ${newMode} ${reason}", "info")
    } 
    else {
        send("${location.name} is already in ${newMode} mode, no action required", "info")
    }
}

// Generate a natural language departure/arrival reason string
def reasonStr(isAway, delaySec, delayMin){
    def reason

    // If invoked by timer, use the stored timer trigger device, otherwise use the last event device
    if (state.timerDevice){
        reason = " because ${state.timerDevice} "
    } 
    else {
        reason = " because ${state.eventDevice} "
    }

    if (isAway){
        reason += "left"
    } 
    else {
        reason += "arrived"
    }

    if (delaySec){
        if (delaySec > 60){
            if (delayMin == null){
                delayMin = (int) delaySec / 60
            }
            reason += " ${delayMin} minute(s) ago"
        }
        else {
            reason += " ${delaySec} second(s) ago"
        }
    }
    return reason
}

// http://docs.smartthings.com/en/latest/smartapp-developers-guide/scheduling.html#schedule-from-now
//
// By default, if a method is scheduled to run in the future, and then
// another call to runIn with the same method is made, the last one
// overwrites the previously scheduled method.
//
// We use the above property to schedule our arrval/departure delay
// using the same function so we don't have to worry about
// arrival/departure timer firing independently and complicating code.
def delaySetMode(){
    def newMode = null
    def reason = ""
    def present = getPresent()
    
    // Timer has elapsed, check presence status to determine action
    if (! present.count){
        reason = reasonStr(true, state.awayDelay, awayThreshold)
        newMode = state.modeIfAway
        if (state.pendingOp){
            send("${state.pendingOp} timer elapsed: everyone is away", "info")
        }
    } 
    else {
        reason = reasonStr(false, state.arrivalDelay, arrivalThreshold)
        newMode = state.modeIfPresent
        if (state.pendingOp){
            send("${state.pendingOp} timer elapsed: someone is present", "info")
        }
    }

    // Now change the mode
    setMode(newMode, reason);
    state.pendingOp = null
    state.timerDevice = null
}

// Returns Map of the count and a string with a natural language list of present devices
private getPresent(){
	def present = [count:0, string:""]
    List presentList = []
    for (person in people.findAll { it?.currentPresence == "present" }){
        if (person.currentPresence == "present"){
        	present.count = present.count + 1 // JR: WTF, increment doesn't work on map value?
            presentList.add(person.displayName)
        }
    }

    // Build natural language string from list
    while (presentList){
    	if (! present.string){			// First iteration
        	present.string = presentList.pop() 
        }
        else if (presentList.size() == 1){	// Final iteration
        	present.string = "${present.string} and ${presentList.pop()}"
        }
        else{								// Middle iteration(s)
        	present.string = "${present.string}, ${presentList.pop()}"
        }
    }

    return present
}

// Sends events, messages and logs per app configuration
private send(msg, logLevel){
	// log levels: [trace, debug, info, warn, error, fatal]
    if (logLevel != "debug"){
    	if (state.isPush){
        	sendPush("${app.label}: ${msg}")	// sendPush() sends the specified message as a push notification to users mobile devices and displays it in Hello, Home
            // JR TODO: Add log level?
    	} // JR TODO: Add SMS else if
    	else {
    		sendNotificationEvent(msg)			// sendNotificationEvent() displays a message in Hello, Home, but does not send a push notification or SMS message.
		}
	}
    log."$logLevel"("${app.label}: ${msg}")
}