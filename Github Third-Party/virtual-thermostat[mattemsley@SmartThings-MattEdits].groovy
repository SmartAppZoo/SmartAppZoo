/**
 *  Copyright 2015 SmartThings - Edited by Matt Emsley
 * v1 added time window
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
 *  Keep Me Cozy II
 *
 *  Author: SmartThings
 */

definition(
    name: "Virtual Thermostat",
    namespace: "mattemsley",
    author: "SmartThings-Matt Emsley",
    description: "Based on Keep Me Cozy II, but enables specific time window and mode.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
    page name:"Setup"
}

def Setup() {
    def pageProperties = [
        name:       "Setup",
        title:      "Setup",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    def newMode = [
        name:       	"newMode",
        type:       	"mode",
        title:      	"Which?",
        multiple:   	true,
        required:   	true
    ]
    def inputPush      = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Send push notifications?",
        defaultValue:   true
    ]

    def inputSMS       = [
        name:           "phoneNumber",
        type:           "phone",
        title:          "Send SMS notifications to?",
        required:       false
    ]
	return dynamicPage(pageProperties) {
        section("Choose thermostat... ") {
            input "thermostat", "capability.thermostat"
        }
        section("Heat setting..." ) {
            input "heatingSetpoint", "decimal", title: "Degrees"
        }
        section("Air conditioning setting...") {
            input "coolingSetpoint", "decimal", title: "Degrees"
        }
        section("Optionally choose temperature sensor to use instead of the thermostat's... ") {
            input "sensor", "capability.temperatureMeasurement", title: "Temp Sensors", required: false
        }
        section("Which mode change triggers the simulator? (This app will only run in selected mode(s))") {
            input newMode           
        }
        section("What time window?") {
    	        href "timeIntervalInput", title: "Only during a certain time", description: getTimeLabel(starting, ending), state: greyedOutTime(starting, ending), refreshAfterSelection:true
    	}
        section("Notification") {
            input inputPush
            input inputSMS
        }
    }
}

def installed()
{
	log.debug "enter installed, state: $state"
	subscribeToEvents()
}

def updated()
{
	log.debug "enter updated, state: $state"
	unsubscribe()
    unschedule()
	subscribeToEvents()
}

def subscribeToEvents()
{
	subscribe(location, changedLocationMode)
	if (sensor) {
		subscribe(sensor, "temperature", temperatureHandler)
		subscribe(thermostat, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostatMode", temperatureHandler)
	}
	evaluate()

//    def t = timeToday(time, location.timeZone)
    schedule("0 30 17 * * ?",resumeThermostat) 
}

def changedLocationMode(evt)
{
	log.debug "changedLocationMode mode: $evt.value, heat: $heat, cool: $cool"
	evaluate()
}

def temperatureHandler(evt)
{
	evaluate()
}

private evaluate()
{
    if(allOk){
        log.debug("Keep Me Cozy II-Time Window Running")
        if (sensor) {
            def threshold = 1.0
            def MaxDifference = 3.0
            def MaxTempSetpoint = heatingSetpoint+MaxDifference
            def MinTempSetpoint = coolingSetpoint-MaxDifference
            def sensorCurrentTemp = sensor.currentTemperature
            def thermostatMode = thermostat.currentThermostatMode
            def thermostatCurrentTemp = thermostat.currentTemperature
            def thermostatCurrentHeatingSetpoint = thermostat.currentHeatingSetpoint
            def thermostatCurrentCoolingSetpoint = thermostat.currentCoolingSetpoint
            
            log.trace("evaluate:, mode: $thermostatMode -- temp: $thermostatCurrentTemp, heat: $thermostat.currentHeatingSetpoint, cool: $thermostat.currentCoolingSetpoint -- "  +
                "sensor: $sensorCurrentTemp, heat: $heatingSetpoint, cool: $coolingSetpoint")
            if (thermostatMode in ["cool","auto"]) {
                // air conditioner
                if (sensorCurrentTemp - thermostat.currentCoolingSetpoint >= MaxDifference) {
            		send("Thermostat Max Temp Difference Error")
                    log.debug "MaxDifference temperature reached"
                    resumeThermostat()
                }
				if (sensorCurrentTemp - coolingSetpoint >= threshold) {
					thermostat.setCoolingSetpoint(thermostatCurrentTemp - 2)
					log.debug "thermostat.setCoolingSetpoint(${thermostatCurrentTemp - 2}), ON"
                	send("Sensor(${sensorCurrentTemp}) > Setpoint(${coolingSetpoint}): Thermostat Set to ${thermostatCurrentTemp + 2}")
				}
                else if (coolingSetpoint > sensorCurrentTemp) {
					thermostat.setHeatingSetpoint(coolingSetpoint)
                    log.debug "sensor temperature below setpoint"
                    send("Sensor(${sensorCurrentTemp}) > Setpoint(${coolingSetpoint}): Thermostat Set to ${coolingSetpoint}")
                }                
				else if (coolingSetpoint == sensorCurrentTemp && thermostatCurrentCoolingSetpoint < coolingSetpoint) {
					thermostat.setCoolingSetpoint(thermostatCurrentTemp)
                    log.debug "sensor temperature equal setpoint"
                    send("Sensor(${sensorCurrentTemp}) = Setpoint(${coolingSetpoint}): Thermostat Set to ${thermostatCurrentTemp}")
                }
                //else if (coolingSetpoint - sensorCurrentTemp >= threshold && thermostatCurrentTemp - thermostat.currentCoolingSetpoint >= threshold) {
                    //thermostat.setCoolingSetpoint(thermostatCurrentTemp + 2)
                    //log.debug "thermostat.setCoolingSetpoint(${thermostatCurrentTemp + 2}), OFF"
                    //log.debug "temperature reached resuming program"
                    //send("Sensor(${sensorCurrentTemp}) < Thermostat Setpoint(${thermostat.currentCoolingSetpoint}): Resume")
                    //resumeThermostat()                    
                //}
            }
			if (thermostatMode in ["heat","emergency heat","auto"]) {
				// heater
				if (heatingSetpoint - sensorCurrentTemp >= threshold) {
                	if (thermostatCurrentTemp + 2 >= MaxTempSetpoint) {
                        thermostat.setHeatingSetpoint(MaxTempSetpoint)
                        send("Thermostat Max Temp Diff: Sensor(${sensorCurrentTemp}) < Setpoint(${heatingSetpoint}): Thermostat(${thermostatCurrentTemp}) Set to ${MaxTempSetpoint}")
                        log.debug "Max Difference temperature reached"
                    }
                    else {
                        thermostat.setHeatingSetpoint(thermostatCurrentTemp + 2)
                        log.debug "thermostat.setHeatingSetpoint(${thermostatCurrentTemp + 2}), ON"
                        send("Sensor(${sensorCurrentTemp}) < Setpoint(${heatingSetpoint}): Thermostat(${thermostatCurrentTemp}) Set to ${thermostatCurrentTemp + 2}")
                    }
				}
				else if (sensorCurrentTemp > heatingSetpoint && thermostatCurrentHeatingSetpoint > heatingSetpoint) {
					thermostat.setHeatingSetpoint(thermostatCurrentTemp - 1)
                    log.debug "sensor temperature above setpoint"
                    send("Sensor(${sensorCurrentTemp}) > Setpoint(${heatingSetpoint}): Thermostat(${thermostatCurrentTemp}) Set to ${thermostatCurrentTemp - 1}")
                }
				else if (sensorCurrentTemp == heatingSetpoint && thermostatCurrentHeatingSetpoint > heatingSetpoint) {
					thermostat.setHeatingSetpoint(thermostatCurrentTemp)
                    log.debug "sensor temperature equal setpoint"
                    send("Sensor(${sensorCurrentTemp}) = Setpoint(${heatingSetpoint}): Thermostat(${thermostatCurrentTemp}) Set to ${thermostatCurrentTemp}")
                }
			}	
        }
        else {
            thermostat.setHeatingSetpoint(heatingSetpoint)
            thermostat.setCoolingSetpoint(coolingSetpoint)
            thermostat.poll()
        }
    }
}

//below is used to check restrictions
private getAllOk() {
	modeOk && timeOk
}

private getModeOk() {
	def result = !newMode || newMode.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
    
    else if (starting){
    	result = currTime >= start
    }
    else if (ending){
    	result = currTime <= stop
    }
    log.trace "ending= $ending"
    log.trace "starting= $starting"
	log.trace "timeOk = $result"
	result
}

def getTimeLabel(starting, ending){

	def timeLabel = "Tap to set"
	
    if(starting && ending){
    	timeLabel = "Between" + " " + hhmm(starting) + " "  + "and" + " " +  hhmm(ending)
    }
    else if (starting) {
		timeLabel = "Start at" + " " + hhmm(starting)
    }
    else if(ending){
    timeLabel = "End at" + hhmm(ending)
    }
	timeLabel
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}
def greyedOut(){
	def result = ""
    if (switches) {
    	result = "complete"	
    }
    result
}

def greyedOutSettings(){
	def result = ""
    if (starting || ending ) {
    	result = "complete"	
    }
    result
}

def greyedOutTime(starting, ending){
	def result = ""
    if (starting || ending) {
    	result = "complete"	
    }
    result
}

def send(msg) {
    log.debug msg

    if (settings.pushMessage) {
        sendPush(msg)
    } else {
        sendNotificationEvent(msg)
    }

    if (settings.phoneNumber != null) {
        sendSms(phoneNumber, msg) 
    }
}

// for backward compatibility with existing subscriptions
def coolingSetpointHandler(evt) {
	log.debug "coolingSetpointHandler()"
}
def heatingSetpointHandler (evt) {
	log.debug "heatingSetpointHandler ()"
}

def resumeThermostat(){
	log.debug "resumeThermostat()"
    send("Thermostat Resume")
    thermostat.resumeProgram()
}

page(name: "timeIntervalInput", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "starting", "time", title: "Starting", required: false 
			input "ending", "time", title: "Ending", required: false 
		}
}