/**
 *  Turn on the water heater if it is needed in the morning
 *	This is for the scenario where an immerison heater is used to heat a water tank, along with other sources.
 *	Currently we have: Solar Water Heating, Boiler Water Heating, Immersion Water Heating, (where the boiler will also heat the house (no separation))
 *	If the water is below a threshold, turn the immersion on for a set time period.
 *	In future we can detect if the PV Panels or Wind Turbine is generating electic (current sensor) to use the most cost effective heating method.
 *
 *	Author: Mike Baird
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
    name: "Water Heater",
    namespace: "m1cs",
    author: "Mike Baird",
    description: "Turn on the immersion heater if needed",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png"
)

preferences {
	section("At a Time...") {
		input name: "startTime", title: "Turn On Time?", type: "time", required: true
	}
	section("Check the water temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement", required: true
	}
	section("If the temperature is below...") {
		input "minimumTemperature", "number", title: "Minimum Temperature?", required: true
	}
	//section("And we are generating electricity...") {
	//	input "generator1", "capability.sensor", title: "Generator?", required: false
	//}
    section("And the Boiler is not on...") {
		input "thermostat1", "capability.thermostat", title: "Thermostat?", required: false
	}
	section("Turn on the Immersion Heater...") {
		input "immersion1", "capability.switch", required: true
	}	
	section("Or turn on the boiler if the house temperature is below...") {
		input "roomMinimumTemperature", "number", title: "Minimum Temperature?", required: false
	}
	section("For a duration of...") {
		input "duration", "number", title: "Minutes?", required: true
	}
    section("Or until the water temperature reaches...") {
		input "targetTemperature", "number", title: "Target Temperature?", required: true
	}
    section("Notifications") {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed() {
	schedule(startTime, "startTimerCallback")
}

def updated() {
	unschedule()
	schedule(startTime, "startTimerCallback")
}

def startTimerCallback() {
    
	def currentTemp = temperatureSensor1.temperatureState.doubleValue
	def minTemp = minimumTemperature
	def myImmersion = settings.immersion1
	def myBoiler = settings.thermostat1

	def tempScale = location.temperatureScale ?: "C"
	
    state.startTemperature = currentTemp

	if (currentTemp <= minTemp) {
        log.debug "Water Temperature ($currentTemp) is below minimum water temperature ($minTemp)" 
        
        def boilerState = thermostat1?.currentValue("thermostatOperatingState")
        log.debug "Boiler is currently: $boilerState"
        
        def roomTemperature = thermostat1?.temperatureState.doubleValue
        log.debug "Room temperature is currently: $roomTemperature"
        
        //we can get the room temperature, there is a minimum room temperature set and the room temp is less than the minimum...
        if (roomTemperature != null && roomMinimumTemperature != null && roomTemperature <= roomMinimumTemperature) {
        	log.debug "Room Temperature ($roomTemperature) is below minimum room temperature ($roomMinimumTemperature)" 
            log.debug "We want to use the boiler instead of the immersion."            
            send("Turning on $myBoiler because ${temperatureSensor1.displayName} is reporting a temperature of ${roomTemperature}${tempScale}")
            turnOnBoiler()
            subscribe(temperatureSensor1, "temperature", temperatureHandlerBoiler)
            def MinuteDelay = 60 * duration
            runIn(MinuteDelay, boilerTimerExpired)
            //make sure the immersion isn't on at the same time
            turnOffImmersion()
        }
        else if (boilerState == null || boilerState == "idle") {
            send("Turning on $myImmersion because ${temperatureSensor1.displayName} is reporting a temperature of ${currentTemp}${tempScale}")
            turnOnImmersion()
            //start monitoring the temperature
            subscribe(temperatureSensor1, "temperature", temperatureHandlerImmersion)
            def MinuteDelay = 60 * duration
            runIn(MinuteDelay, immersionTimerExpired)
        }
        else {
            log.debug "$myBoiler is already heating the water - no need for $myImmersion"
            send("$myBoiler is already heating the water - no need for $myImmersion")        
        }
	}
    else {    
		log.debug "Water Temperature is above $minTemp:  no heating required"
        send("Water Temperature is above $minTemp:  no heating required ( ${currentTemp}${tempScale} )")
    }
}

def temperatureHandlerImmersion(evt) {
	log.trace "Current Water Temperature: $evt.value"

	def targetTemperature = targetTemperature
	def myImmersion = settings.immersion1
    
    def tempScale = location.temperatureScale ?: "C"

	if (evt.doubleValue >= targetTemperature) {
        log.debug "Temperature above $targetTemperature:  sending notification and deactivating $myImmersion"
        send("Turning off the Immersion: ${temperatureSensor1.displayName} ( ${evt.value}${evt.unit?:tempScale} ) is above the target temperature ( $targetTemperature )")
        //turn off the immersion and unsubscribe the event.
        turnOffImmersion()
        unsubscribe()
        unschedule(immersionTimerExpired)
	}
}

def temperatureHandlerBoiler(evt) {
	log.trace "Current Water Temperature: $evt.value"

	def targetTemperature = targetTemperature
	def myBoiler = settings.thermostat1
    
    def tempScale = location.temperatureScale ?: "C"

	if (evt.doubleValue >= targetTemperature) {
        log.debug "Temperature above $targetTemperature:  sending notification and deactivating $myBoiler"
        send("Turning off the Boiler: ${temperatureSensor1.displayName} ( ${evt.value}${evt.unit?:tempScale} ) is above the target temperature ( $targetTemperature )")
        //turn off the boiler and unsubscribe the event.
        turnOffBoiler()
        unsubscribe()
        unschedule(boilerTimerExpired)
	}
}

def immersionTimerExpired() {
	def currentTemp = temperatureSensor1.temperatureState.doubleValue
    
	log.debug "Turning off the Immersion as the timer has expired."
    log.debug "Current Water Temperature is $currentTemp, start Temperature was $state.startTemperature"
    send("Turning off the Immersion:  Current Water Temperature is $currentTemp, start Temperature was $state.startTemperature")
	turnOffImmersion()
    unsubscribe()
}

def boilerTimerExpired() {
	def currentTemp = temperatureSensor1.temperatureState.doubleValue
    
	log.debug "Turning off the boiler as the timer has expired."
    log.debug "Current Water Temperature is $currentTemp, start Temperature was $state.startTemperature"
    send("Turning off the Boiler:  Current Water Temperature is $currentTemp, start Temperature was $state.startTemperature")
	turnOffBoiler()
    unsubscribe()
}

def turnOnImmersion() {
	def mySwitch = settings.immersion1
	log.debug "Enabling $mySwitch"
	immersion1?.on()
}

def turnOffImmersion() {
	def mySwitch = settings.immersion1
	log.debug "Disabling $mySwitch"
	immersion1?.off()
}

def turnOnBoiler() {
	def mySwitch = settings.thermostat1
	log.debug "Enabling $mySwitch"
	thermostat1?.heat()
}

def turnOffBoiler() {
	def mySwitch = settings.thermostat1
	log.debug "Disabling $mySwitch"
	thermostat1?.auto()
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("Sending Notifications To: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("Sending Push Message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("Sending Text Message")
            sendSms(phone1, msg)
        }
   	}
    log.debug msg
}