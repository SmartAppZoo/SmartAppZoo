/**
 *  Temperature Alert
 *
 *  Copyright 2017 Louis Jackson
 *
 *  Version 1.0.2   8  Jan 2017
 *
 *	Version History
 *
 *	1.0.2   8  Jan 2017		Changed the check\alert from 1 to every 3 hours.
 *	1.0.1   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.0	28 Jan 2016		Added to GitHub
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
    name: "Temperature Alert",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Use sensor temperature to warn of excessive heat or cold",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather2-icn@2x.png")


preferences {
	section("Select Things to Monitor:") 
    {
            input "theThermostat", "capability.temperatureMeasurement", title: "Things With Thermostats", multiple: true,   required: true
    }
    
    section("Set Temperature Alerts:") 
    {
    		input "maxThreshold", "number",   title: "if above... (default 90°)", defaultValue:90,   required: true
            input "minThreshold", "number",   title: "if below... (default 40°)", defaultValue:40,   required: true
    }
  
    section("Via push notification and/or a SMS message") 
    {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
    }
    
    section ("Version 1.0.2") {}
}

def installed() {
  log.trace "(0A) ${app.label} - installed() with settings: ${settings}"
  initialize()
}

def updated() {
	log.info "(0B) - Updated()"
    unschedule() //un-schedule
	initialize()
}

def initialize() {
   	log.info "(0C) - initialize() - Can schedule? ${canSchedule()}"
	//schedule("0 30 11 ? * SAT", doTempCheck) // call doTempCheck at 11:30am every Saturday of the month
    //schedule("0 30 * * * ?", doTempCheck)      // execute doTempCheck every hour on the half hour.  
    //runEvery1Hour(doTempCheck)         //Every hours was too frequent and bug the hell out of me.
    runEvery3Hours(doTempCheck)
    
    doTempCheck() //Check now!
}

def doTempCheck() {
    log.trace "(0D) - doTempCheck() - settings: ${settings}"
    
    def nDevAbove  = 0
    def nDevBelow  = 0
    def strAboveMessage = ""
	def strBelowMessage = ""

	for (thermostatDevice in theThermostat) 
    {
    	def thermostatLevel = thermostatDevice.currentValue("temperature")

		log.trace "(0E) - Checking... ${thermostatDevice.label}: ${thermostatLevel}°\n"

        if ( settings.maxThreshold.toInteger() != null && thermostatLevel >= settings.maxThreshold.toInteger() ) 
        {
       		log.warn "(0F) - ${thermostatDevice.label}: ${thermostatLevel}°\n"
			strAboveMessage += "- ${thermostatDevice.label}: ${thermostatLevel}°.\n"
			nDevAbove++
        }

        if ( settings.minThreshold.toInteger() != null && thermostatLevel <= settings.minThreshold.toInteger() ) 
        {
       		log.warn "(10) - ${thermostatDevice.label}: ${thermostatLevel}°\n"
			strBelowMessage += "- ${thermostatDevice.label}: ${thermostatLevel}°.\n"
			nDevBelow++
        }
    }
        
    if ( nDevAbove ){
    	send("The ${app.label} SmartApp determined you have ${nDevAbove} device(s) ABOVE the Temperature alert level of ${settings.maxThreshold.toInteger()}°:\n\n${strAboveMessage}")
    }
    
    if ( nDevBelow ){
    	send("The ${app.label} SmartApp determined you have ${nDevBelow} device(s) BELOW the Temperature alert level of ${settings.minThreshold.toInteger()}°:\n\n${strBelowMessage}")
    }
}

private send(msg) {
    log.info "(A1) sending message ${msg}"

    if (location.contactBookEnabled) 
    {
    	log.trace "(A2) send to contact ${recipients}"
        sendNotificationToContacts(msg, recipients)
    } 
    else if (phone)
    {
        log.trace "(A3) send to contact ${phone}"
        sendSms(phone, msg)
    }
}
