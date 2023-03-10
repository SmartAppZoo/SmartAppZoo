/**
 *  Did It Run
 *
 *  Copyright 2014 SmartThings
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
    name: "Did It Run",
    namespace: "awpalmbach",
    author: "Abraham Palmbach",
    description: "SmartThings app detects if smart outlet detects power draws within a period of time",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    pausable: true
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        input(name: "threshold", type: "number", title: "Reports Above...", required: true, description: "in either watts.")
	}
	
	section {
    	input(name: "switch1", type: "capability.switch", title: "Use This Virtual Switch", required: true, multiple: false, description: null)
    }
	
	  section("And notify me if it hasn't exceeded the threshold in more than this many minutes (default 10)") {
		input "openThreshold", "number", description: "Number of minutes", required: false
	  }

	  section("Delay between notifications (default 10 minutes") {
		input "frequency", "number", title: "Number of minutes", description: "", required: false
	  }

	  section("Via text message at this number") {
		input("recipients", "contact", title: "Send notifications to") {
		  input "phone", "phone", title: "Phone number (optional)", required: false
		}
		input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: true)
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
	subscribe(meter, "power", meterHandler)

}

def meterHandler(evt) {
    def meterValue = evt.value as double
	log.trace "meterHandler($evt.name: $evt.value)"

    def thresholdValue = threshold as int

def switchState = switch1.currentState("switch")


    if (meterValue > thresholdValue) {
	    log.debug "${meter} reported energy consumption above ${threshold}."
		
		if (switchState.value != "on") {
			log.debug "${switch1} not on. Turning on."
			switch1.on()
			unschedule(offTooLong)
		}
     }
	else {
		log.debug "${meter} reported energy consumption below ${threshold}."
		if (switchState.value != "off") {
			log.debug "${switch1} not off. Turning off."
			switch1.off()
		}
	}
	
	def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 600
	runIn(delay, offTooLong, [overwrite: true])
	
}


def offTooLong() {
  def switchState = switch1.currentState("switch")
  def freq = (frequency != null && frequency != "") ? frequency * 60 : 600

  if (switchState.value == "off") {
    def elapsed = now() - switchState.rawDateCreated.time
    def offthreshold = ((openThreshold != null && openThreshold != "") ? openThreshold * 60000 : 60000) - 1000
    if (elapsed >= offthreshold) {
      log.debug "Switch has stayed off long enough since last check ($elapsed ms):  calling sendMessage()"
      sendMessage()
      runIn(freq, offTooLong, [overwrite: false])
    } else {
      log.debug "Switch has not stayed off long enough since last check ($elapsed ms):  doing nothing"
    }
  } else {
    log.warn "offTooLong() called but switch is on:  doing nothing"
  }
}

void sendMessage() {
  def minutes = (openThreshold != null && openThreshold != "") ? openThreshold : 10
  def msg = "${meter.displayName} has not run for at least ${minutes} minutes."
  log.info msg
  if (location.contactBookEnabled) {
    sendNotificationToContacts(msg, recipients)
  } else {
    if (phone) {
      sendSms phone, msg
    } 
	if (pushNotification) {
      sendPush msg
    }
  }
}
