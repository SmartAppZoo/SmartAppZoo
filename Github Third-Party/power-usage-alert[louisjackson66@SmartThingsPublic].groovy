/**
 *  Power Usage Alert
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.2   31 Jan 2016
 *
 *	Version History
 *
 *	1.0.2   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.1	28 Jan 2016		Added to GitHub and made grammer corrections in alert message
 *	1.0.0	27 Jan 2016		Creation
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
    name: "Power Usage Alert",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Notifies when Power Usage exceeds a given amount",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances17-icn@2x.png")


preferences {
	section("Select Things to Control:") 
    {
		input "PowerMeters", "capability.powerMeter", title: "Meters", multiple: true, required:true
		input "MaxPower", "number", title: "Notify when power exceeds", required: true
	}
    
    section("Via push notification and/or a SMS message") {
        input "recipients", "contact", title: "Send notifications to", required: false }
    
    section ("Version 1.0.2") {}
}

def installed() {
    log.trace "(0A) ${app.label} - installed() - settings: ${settings}"
    def PowerMetersValue = PowerMeters.latestValue("power")
	initialize()
}

def updated() {
    log.info "(0B) ${app.label} - updated()"
	unschedule()
	initialize()
}

def initialize() {
    log.info "(0C) ${app.label} - initialize() - Can schedule? ${canSchedule()}"
    runEvery15Minutes(onHandler) 
    onHandler() //Check now!
}

// --------------------------------------------------------------------------------------------------------------
//
// --------------------------------------------------------------------------------------------------------------
//def onHandler(evt) 
def onHandler() 
{
    def nDevCnt  = 0
    def PowerMetersValue = 0
    def strMessage = ""
                       
	for (PowerMeterDevice in PowerMeters) 
    {
        log.info "(0D) - current value for ${PowerMeterDevice.label} is ${PowerMeterDevice.currentValue("power")}"
    	PowerMetersValue += PowerMeterDevice.currentValue("power")
		strMessage += "- ${PowerMeterDevice.label}: ${PowerMeterDevice.currentValue("power")}W\n"
		nDevCnt++
    }
    
    if ( (PowerMetersValue >  settings.MaxPower.toInteger()) && location.contactBookEnabled)
    {
        log.warn "(0E) - comparing ${PowerMetersValue} to ${settings.MaxPower.toInteger()} - ${settings}"

    	strMessage = "The ${app.label} SmartApp determined you have ${nDevCnt} device(s) using a total of ${PowerMetersValue}W. This is above the alert level of ${settings.MaxPower.toInteger()}W :\n\n${strMessage}"
    	sendNotificationToContacts(strMessage, recipients)
	}
}
