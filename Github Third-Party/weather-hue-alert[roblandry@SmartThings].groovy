/**
 *  Weather Hue Alert
 *
 *  Copyright 2015 Rob Landry
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
    name: "Weather Hue Alert",
    namespace: "roblandry",
    author: "Rob Landry",
    description: "Set hue light based on weather",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png")

preferences {
	section ("Select Hue Bulbs") {
        	input "bulbs", "capability.switchLevel", title: "Which Smart Bulbs?", required:false, multiple:true
	}

	section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipcode", "text", title: "Zip Code", required: false
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
	def sec = Math.round(Math.floor(Math.random() * 60))
	def min = Math.round(Math.floor(Math.random() * 60))
	def cron = "$sec $min * * * ?"
	schedule(cron, "checkForSevereWeather")
}

def checkForSevereWeather() {
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

	def newKeys = alerts?.collect{it.type + it.date_epoch} ?: []
	log.debug "Severe Weather Alert: newKeys: $newKeys"

	def oldKeys = state.alertKeys ?: []
	log.debug "Severe Weather Alert: oldKeys: $oldKeys"

	if (newKeys != oldKeys) {

		state.alertKeys = newKeys

		alerts.each {alert ->
			if (!oldKeys.contains(alert.type + alert.date_epoch) && descriptionFilter(alert.description)) {
				def msg = "Weather Alert! ${alert.description} from ${alert.date} until ${alert.expires}"
				//send(msg)
				//def string aType = "severe"
				//alerts(aType)
			}
		}
	}
				def aType = "severe"
				//malerts(aType)
                takeAction(aType)
}

def descriptionFilter(String description) {
	def filterList = ["special", "statement", "test"]
	def passesFilter = true
	filterList.each() { word ->
		if(description.toLowerCase().contains(word)) { passesFilter = false }
	}
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

private malerts(aType) {

	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}
    
	if (aType == "severe") {
		def newValue = [hue: 		4, 
				saturation: 	100, 
				level: 		100]
		

		def duration = 2
		hues*.setColor(newValue)
		log.debug "New hue value = $newValue"

		setTimer(duration)

		hues*.setColor(newValue)
		log.debug "New hue value = $newValue"

		setTimer(duration)
	}

}

private takeAction(aType) {

	if (frequency) {
		state[frequencyKey(evt)] = now()
	}

	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 //83
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}


	log.debug "current values = $state.previous"

def newValue = [hue: 		4, 
				saturation: 	100, 
				level: 		100]
	log.debug "new hue value = $newValue"

	hues*.setColor(newValue)

}

def setTimer(duration)
{
	if(!duration) //default to 10 seconds
	{
		log.debug "pause 10"
		pause(10 * 1000)
		log.debug "reset hue"
		resetHue()
	}
	else if(duration < 10)
	{
		log.debug "pause $duration"
		pause(duration * 1000)
		log.debug "resetHue"
		resetHue()
	}
	else
	{
		log.debug "runIn $duration, resetHue"
		runIn(duration,"resetHue", [overwrite: false])
	}
}


def resetHue()
{
   
	hues.each {
		it.setColor(state.previous[it.id])
		log.debug "New hue value = $state.previous[it.id]"

}
}