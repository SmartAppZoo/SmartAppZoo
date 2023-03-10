/**
 *  Delayed Switch
 *
 *  Copyright 2016 Chris Liebman
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
    name: "Delayed Switch",
    namespace: "liebman",
    author: "Chris Liebman",
    description: "Turn switch on/off after delay with redundant button press.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    input "master", "capability.switch", title: "Select switch:", required: true
    input "action", "enum", title: "Action:", multiple: false, required: true, options: ["on", "off"], defaultValue: "off"
    input "delay", "number" , title: "Action delay:", required: false, defaultValue: 900
    input "feedback", "capability.switch", title: "Select feedback switch:", required: false
    input "feedbackDuration", "number" , title: "feedback duration (ms):", required: false, defaultValue: 1000
    input "logger", "capability.switch", title: "LogDevice:", required: false
}

def installed() {
	logit "debug", "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logit "debug", "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(master, "switch", switchHandler, [filterEvents: false])
    subscribe(location, null, locationHandler, [filterEvents: false])
}

def locationHandler(evt) {
    logit "info", "location result: ${evt.description}"
}

def switchHandler(evt) {
	logit "info", "CURRENT: value: ${evt.value} phys:${evt.isPhysical()} change:${evt.isStateChange()} date:${evt.date} id:${evt.id} app:${app.label}"
    def history = master.events(max: 10)
    dumpEvents(history)
    // we must have some history
    if (history && history.size() > 1) {
	    // find the previous event
    	def prev = history.get(0)
        if (prev.id.equals(evt.id)) {
            logit "warn", "first history event was the current one, using the one before that!"
            prev = history.get(1)
        }
		// make sure the last two events were physical
        logit "info", "PREVIOUS: value: ${prev.value} phys: ${prev.isPhysical()} change:${prev.isStateChange()} date:${prev.date} id:${prev.id}"
        
        if (evt.isPhysical() && !evt.isStateChange() && !action.equalsIgnoreCase(evt.value) && prev.isPhysical()) {
            if (feedback) {
                startFeedback(evt)
            }
            logit "info", "scheduling delayed ${action} in ${delay} seconds"
            runIn(delay, delayedActionHandler)
            logit "info", "back from runIn()"
        } else if (evt.isPhysical() && evt.isStateChange() && action.equalsIgnoreCase(evt.value)) {
            // cancel if someone physically changes it.
            logit "info", "unscheduling incase we were active!"
            unschedule()
        }
    }
}

def dumpEvents(events) {
   logit "debug", "****** start event history size:${events.size()} ******"
   for(def i = 0; i < events.size(); ++i) {
       def e = events.get(i)
       logit "debug", "index:${i} date:${e.date} value:${e.value} phys:${e.isPhysical()} dgtl:${e.isDigital()} id:${e.id}"
   }

  logit "debug", "****** end event history ******"
}

def delayedActionHandler() {
    logit "info", "delayedActionHandler called!"
    
    if (action == "off") {
        logit "info", "turning it off"
        master.off()
    } else {
        logit "info", "turning it on"
        master.on()
    }
    logit "info", "delayedActionHandler complete"
}

def startFeedback(evt) {
    logit "info", "starting feedback"
    def current = feedback.latestState("switch").value
    if (feedback == master) {
       logit "info", "feedback is same as master - using event info for state"
       current = master.latestState("switch").value
    }
    
    if (current == 'on') {
        logit "info", "feedback current state is 'on' so turning it off"
        feedback.off()
        feedback.on(delay: feedbackDuration)
    } else {
        logit "info", "feedback current state is 'off' so turning it on"
        feedback.on()
        feedback.off(delay: feedbackDuration)
    }
    
    /* runIn(feedbackDuration, endFeedback) */
    logit "info", "feedback started!"
}

def endFeedback() {
    logit "info", "ending feedback"
    if (feedback.currentState == 'on') {
        logit "info", "ending feedback state is 'on' so turning it off"
        feedback.off()
    } else {
        logit "info", "ending feedback state is 'off' so turning it on"
        feedback.on()
    }
    logit "info", "feedback ended!"
}

def logit(level, message) {
    if (logger) {
        logger.log(level, app.label, message)
    }
    
    switch(level) {
    	case "debug": log.debug(message); break;
    	case "info":  log.info(message);  break;
    	case "warn":  log.warn(message);  break;
    	case "error": log.error(message); break;
    	default:      log.info(message);  break;
    }
}

