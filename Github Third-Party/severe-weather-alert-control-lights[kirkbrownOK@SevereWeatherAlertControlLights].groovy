/**
 *  Copyright Kirk Brown
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
 *  Severe Weather Alert controls LIGHTS 
 *
 *  Author: Kirk Brown based on Severe Weather Alert from SmartThings
 *  Date: 2013-03-04
 */
definition(
    name: "Severe Weather Alert Control Lights",
    namespace: "kirkbrownOK/SevereWeatherAlertControlLights",
    author: "kirkbrown",
    description: "Get a push notification when severe weather is in your area. And Control lights",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png"
)

preferences {
	section ("In addition to push notifications, send text alerts to...") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone Number 1", required: false
            input "phone2", "phone", title: "Phone Number 2", required: false
            input "phone3", "phone", title: "Phone Number 3", required: false
        }
	}
    section ("Turn on these lights for Tornado Alerts") {
    	input "watchLights", "capability.switch", title: "Tornado Watch Lights", required: false, multiple: true
        input "warningLights", "capability.switch", title: "Tornado Warning Lights", required: false, multiple: true
    }

	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipcode", "text", title: "Zip Code", required: false
	}
    section ("Update on this switch") {
    	input "updateSwitch", "capability.switch", title: "Swith that causes update", required: false, multiple: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	scheduleJob()
    if(updateSwitch) {
    	subscribe(updateSwitch,"switch.on",checkForSevereWeather)
    }
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    state.alertKeys = ""
    unschedule()
	scheduleJob()
    if(updateSwitch) {
    	unsubscribe()
    	subscribe(updateSwitch,"switch.on",checkForSevereWeather)
    }
}

def scheduleJob() {
	def sec = Math.round(Math.floor(Math.random() * 60))
	def min = Math.round(Math.floor(Math.random() * 60))
	def cron = "$sec $min * * * ?"
    log.debug "chron: ${cron}"
	schedule(cron, "checkForSevereWeather")
}

def checkForSevereWeather(evt) {
	def alerts
	if(locationIsDefined()) {
		if(zipcodeIsValid()) {
			alerts = getWeatherFeature("alerts", zipcode)?.alerts
		} else {
			log.warn "Severe Weather Alert: Invalid zipcode entered, defaulting to location's zipcode"
			alerts = getWeatherFeature("alerts")?.alerts
		}
	} else {
		log.warn "Severe Weather Alert: Location is not defined"
	}
	log.debug "alerts: ${alerts}"
    if(alerts == []) {
    	state.tornadoWatch = false
        state.tornadoWarning = false
    }
	def newKeys = alerts?.collect{it.type + it.date_epoch} ?: []
	log.debug "Severe Weather Alert: newKeys: $newKeys"

	def oldKeys = state.alertKeys ?: []
	log.debug "Severe Weather Alert: oldKeys: $oldKeys"

	if (newKeys != oldKeys) {

		state.alertKeys = newKeys

		alerts.each {alert ->
			if (!oldKeys.contains(alert.type + alert.date_epoch) && descriptionFilter(alert.description)) {
				def msg = "Weather Alert! ${alert.description} from ${alert.date} until ${alert.expires}"
				
                if (alert.description.contains("Tornado Watch") && !state.tornadoWatch) {
                	send(msg)
                	log.debug "Turning on tornado Watch Lights"
                    state.tornadoWarning = false
                    state.tornadoWatch = true
                	watchLights.on()               
                }
                if (alert.description.contains("Tornado Warning") && !state.tornadoWarning){
                	log.debug "Turning on tornado Warning Lights"
                    send(msg)
                    state.tornadoWatch = false
                    state.tornadoWarning = true
                	warningLights.on()
                }
			}
		}
	}
}

def descriptionFilter(String description) {
	def filterList = ["special", "statement","thunderstorm", "test"]
	def passesFilter = true
	filterList.each() { word ->
		if(description.toLowerCase().contains(word)) { passesFilter = false }
	}
    log.debug "Description ${description} filter: ${passesFilter}"
	passesFilter
}

def locationIsDefined() {
	zipcodeIsValid() || location.zipCode || ( location.latitude && location.longitude )
}

def zipcodeIsValid() {
	zipcode && zipcode.isNumber() && zipcode.size() == 5
}

private send(message) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        sendPush message
        if (settings.phone1) {
            sendSms phone1, message
        }
        if (settings.phone2) {
            sendSms phone2, message
        }
        if (settings.phone3) {
            sendSms phone3, message
        }
    }
}