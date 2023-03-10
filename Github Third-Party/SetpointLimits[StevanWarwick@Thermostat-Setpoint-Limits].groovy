/**
 *  Thermostat Setpoint Limits
 *
 *  Copyright 2015 Stevan Warwick
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
        name: "Setpoint Limits",
        namespace: "StevanWarwick",
        author: "Stevan Warwick",
        description: "This app allows you to set the high and low temperature limits on your thermostat.",
        category: "My Apps",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section ("Which thermostat would you like to set limits on?"){
        input "thermostatDevices", "capability.thermostat", title: "Select thermostat to set limits on", multiple: true, required: true
    }
    section ("Set the cooling and heating limits"){
        input "coolingLimit", "number", title: "Cooling Limit", multiple: false, required: true
        input "heatingLimit", "number", title: "Heating Limit", multiple: false, required: true
    }
    section("Send these messages when cooling and heating limits are exceeded (optional, sends standard status message if not specified)"){
        input "coolingLimitMessageText", "text", title: "Cooling Limit Message Text", required: false
        input "heatingLimitMessageText", "text", title: "Heating Limit Message Text", required: false
    }
    section("Via a push notification and/or an SMS message"){
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
            input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
        }
    }
    section("Minimum time between messages (optional, defaults to every message)") {
        input "frequency", "decimal", title: "Minutes", required: false
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
    subscribe(thermostatDevices, "coolingSetpoint", onCoolingSetpointChanged)
    subscribe(thermostatDevices, "heatingSetpoint", onHeatingSetpointChanged)
    for(thermostat in thermostatDevices)
    {
    	log.debug("Cooling set point for ${thermostat.displayName} is currently ${thermostat.currentCoolingSetpoint}");
        log.debug("Heating set point for ${thermostat.displayName} is currently ${thermostat.currentHeatingSetpoint}");
    }
    runEvery5Minutes(ensureSetpointLimits)
}

def ensureSetpointLimits(evt)
{
    log.debug("Checking set point limits")
	for(thermostat in thermostatDevices)
    {
    	if(thermostat.currentCoolingSetpoint != null)
        {
    		ensureCoolingSetpointLimit(thermostat, thermostat.currentCoolingSetpoint.toInteger())
        }
        else
        {
        	log.debug("Current cooling set point is not available for ${thermostat.displayName}")
        }
        if(thermostat.currentHeatingSetpoint != null)
        {
        	ensureHeatingSetpointLimit(thermostat, thermostat.currentHeatingSetpoint.toInteger())
        }
        else
        {
        	log.debug("Current heating set point is not available for ${thermostat.displayName}")
        }
    }
}

def ensureCoolingSetpointLimit(device, Integer current)
{
    if(current < coolingLimit.toInteger())
    {
    	log.debug("Cooling limit exceeded on ${device.displayName} (${current}). Cooling set point will be set to ${coolingLimit}.")
        device.setCoolingSetpoint(coolingLimit.toInteger())
    }
}

def ensureHeatingSetpointLimit(device, Integer current)
{
    if(current > heatingLimit.toInteger())
    {
    	log.debug("Heating limit exceeded on ${device.displayName} (${current}). Heating set point will be set to ${heatingLimit}.")
        device.setHeatingSetpoint(heatingLimit.toInteger())
    }
}

def onCoolingSetpointChanged(evt)
{
    log.debug("Cooling point changed on ${evt.device.displayName} to ${evt.value} and cooling limit is ${coolingLimit}.")
    if(evt.value.toInteger() < coolingLimit.toInteger())
    {
    	log.debug("Cooling limit exceeded on ${evt.device.displayName}. Cooling set point will be set to ${coolingLimit}.")
        evt.device.setCoolingSetpoint(coolingLimit.toInteger(), delay: 3000)
        sendNotification(evt)
    }
}

def onHeatingSetpointChanged(evt)
{
    log.debug("Heating point changed on ${evt.device.displayName} to ${evt.value} and heating limit is ${heatingLimit}.")
    if(evt.value.toInteger() > heatingLimit.toInteger())
    {
    	log.debug("Heating limit exceeded on ${evt.device.displayName}. Heating set point will be set to ${heatingLimit}.")
        evt.device.setHeatingSetpoint(heatingLimit.toInteger(), delay: 3000)
        sendNotification(evt)
    }
}

def sendNotification(evt) {
    log.debug "Notify got evt ${evt}"
    if (frequency) {
        def lastTime = state[evt.deviceId]
        if (lastTime == null || now() - lastTime >= frequency * 60000) {
            sendMessage(evt)
        }
    }
    else {
        sendMessage(evt)
    }
}

private sendMessage(evt) {
    def msg;
    if(evt.name == "coolingSetpoint")
    {
    	msg = coolingLimitMessageText ?: "Cooling set point of ${evt.value} exceeded cooling limit of ${coolingLimit} on ${evt.device.displayName}"
    }
    if(evt.name == "heatingSetpoint")
    {
    	msg = heatingLimitMessageText ?: "Heating set point of ${evt.value} exceeded heating limit of ${heatingLimit} on ${evt.device.displayName}" 
    }
    log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {

        if (!phone || pushAndPhone != "No") {
            log.debug "sending push"
            sendPush(msg)
        }
        if (phone) {
            log.debug "sending SMS"
            sendSms(phone, msg)
        }
    }
    if (frequency) {
        state[evt.deviceId] = now()
    }
}
