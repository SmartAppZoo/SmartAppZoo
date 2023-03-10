/**
 *  Timer Management with verification if someone at home
 *
 *  Copyright 2016 Philippe Gravel
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
    name: "Light Timer Management Plus",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Open / Close Light at time with verification if someone at home",
    category: "Family",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png"
)

preferences {
	input "lights", "capability.switch", title: "Lights without presence", required: false, multiple: true
   	input "lightsVerif", "capability.switch", title: "Lights with presence needed", required: false, multiple: true
    
	section("Time to and close the light(s)") {
    	input "openTime", "time", title: "Open at", required: true
        input "closeTime", "time", title: "Close at", required: true
	}
    
    section("When people arrive and leave..."){
        input "peopleToWatch", "capability.presenceSensor", title: "Who?", multiple: true, required: false
    }

    section("Visitor") {
        input "visitorSwitch", "capability.switch", title: "Visitor Switch?", required: true
    }
        
    section("Active/Inactive") {
    	input "active", "boolean", title: "Timer Active?", required: true
    }
    
	section("Send Notifications?") {
		input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	log.debug "$app.label Initialize"
    
    if (active) {

        def timeZone = location.timeZone
        def currentTime = timeToday(openTime, timeZone)
        def timeOpenHour = currentTime.format("H", timeZone)
        def timeOpenMinute = currentTime.format("m", timeZone)
        log.debug "Open: $timeOpenHour:$timeOpenMinute"
        schedule("0 $timeOpenMinute $timeOpenHour ? * * *", openHandler)

        currentTime = timeToday(closeTime, timeZone)
        def timeCloseHour = currentTime.format('H', timeZone)
        def timeCloseMinute = currentTime.format('m', timeZone)
        log.debug "Close: $timeCloseHour:$timeCloseMinute"
        schedule("0 $timeCloseMinute $timeCloseHour ? * * *", closeHandler)

        sendNotificationToContacts("$app.label\nOpen at $timeOpenHour:$timeOpenMinute\nClose at $timeCloseHour:$timeCloseMinute", recipients)

        if (lightsVerif != null) {
            subscribe(peopleToWatch, "presence", presenseHandler)
        }
	}
}

def openHandler() {

	def messages = "$app.label: "
    
    if (lights != null) {
    	log.debug "$app.label: Open Light $lights.displayName"
    	lights.on()
    }
    
    if (lightsVerif != null) {
        def presenceValue = peopleToWatch.find{it.currentPresence == "present"}
        if (presenceValue || visitorAtHome()) {
            log.debug "$app.label: Somebody home - Open! $lightsVerif.displayName"
            messages = messages + "Somebody home - Open! $lightsVerif.name"

            lightsVerif.on()

        } else {
            log.debug "$app.label: Nobody at home, stay the lights close!"
            messages = messages + "Nobody at home, stay the lights close!"
        }
	}
    
	sendNotificationToContacts(messages, recipients)
}

def closeHandler() {
	sendNotificationToContacts("$app.label Close!", recipients)
    
    if (lights != null) {
		lights.off()
    }
    
    if (lightsVerif != null) {
    	lightsVerif.off()
    }
}

def presenseHandler(evt) {
	
	log.debug "$app.label: presenceHandler $evt.name: $evt.value, $evt.displayName"
    def messages = "$app.label: "

    if (evt.value == "not present") {
        messages = messages + "Someone left ($evt.displayName)"

        def presenceValue = peopleToWatch.find{it.currentPresence == "present"}
        if (presenceValue) {
            messages = messages + "\nStill somebody home - nothing to do"
		} else {
            messages = messages + "\nEverybody as left"
            
			if (!visitorAtHome()) {
                messages = messages + "\nClose lights $lightsVerif.name!"
                
                lightsVerif.off()
                
            } else {
            	messages = messages + "Visitor at home do nothing!"
            }
        } 
	} else {
        messages = messages + "Someone arrive ($evt.displayName)"

		if (location.mode != "Away") {
            messages = messages + "\nSomebody already home"
		} else {
            messages = messages + "\nFirst arrive - Open lightsVerif.name!"
			           
	    	def between = timeOfDayIsBetween(openTime, closeTime, new Date(), location.timeZone)
    		if (between) {
        		lightsVerif.on()
            }
        }
    }
    
    sendNotificationToContacts(messages, recipients)
    log.debug(messages)
}

def visitorAtHome() {
	return (visitorSwitch.currentSwitch == "on")
}
