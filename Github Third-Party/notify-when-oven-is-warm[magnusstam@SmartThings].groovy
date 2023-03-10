/**
 *  Energy Meter Routine
 *
 *  Copyright 2018 Magnus Stam
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
    name: "Notify when oven is warm",
    namespace: "magnusstam",
    author: "Magnus Stam",
	description: "Send a notification when heater is turned of because the right temperature is reached.",
	category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png")

preferences
{
	page(name: "getPref")
}
	
def getPref()
{
    dynamicPage(name: "getPref", title: "Choose Meter, Threshold, and Routines", install:true, uninstall: true)
    {
        section
        {
            paragraph "Power meter"
            input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        }
        section
        {
            paragraph "Threshold and notification text"
            input(name: "threshold", type: "number", title: "Energy Meter Threshold...", required: true, description: "in either watts or kw.")
            input(name: "NotificationText", type: "string", title: "Send this notification when oven is at right temperature", required: true, description: null)
        }
        section
        {
            paragraph "Warn about forgotten oven"
            input(name: "maxOnTime", type: "number", title: "Minutes before warning", required: true, description: null)
            input(name: "WarningText", type: "string", title: "Send this notification when oven has been on too long", required: true, description: null)
        }
        section
        {
            input(name: "minutesWithoutPower", type: "number", title: "Minutes after last power off detected until power is considered off", required: true, description: null)
        }
    }
}

def installed()
{
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize()
{
	subscribe(meter, "power", powerMeterHandler)
    atomicState.isOn = false
    atomicState.onDetected = now()
    atomicState.offDetected = now()
    atomicState.isFirstTransition = true
}

def powerOffHandler()
{
    atomicState.isOn = false
    log.debug "oven is off"
 }

def powerMeterHandler(evt)
{
    def powerValue = evt.value as double
        if (!atomicState.lastPowerValue)
    {
        atomicState.lastPowerValue = powerValue
    }


    def lastPowerValue = atomicState.lastPowerValue as double
        atomicState.lastPowerValue = powerValue

    def thresholdValue = threshold as int
        if (powerValue < thresholdValue)
    {
        if (lastPowerValue > thresholdValue)
        {
            atomicState.offDetected = now()
            if (atomicState.isFirstTransition == true)
            {
                sendPush(NotificationText)
                atomicState.isFirstTransition = false
            }
        }
        def minutesTilPowerConsideredOff = minutesWithoutPower as int
            runIn(60*minutesTilPowerConsideredOff, powerOffHandler)
    }
    else
    {
        if (atomicState.isOn == false)
        {
            atomicState.isOn = true
            atomicState.isFirstTransition = true
            atomicState.onDetected = now()
            log.debug "oven is on"
        }
        else
        {
            def timeSinceOnDetected = (now() - atomicState.onDetected)/(1000*60) as int
                def max = maxOnTime as int
                    log.debug "Time since oven was turned on is ${timeSinceOnDetected} minutes, max is ${max} minutes"
                if (timeSinceOnDetected > max)
            {
                sendPush(WarningText)
            }
        }
    }
}
