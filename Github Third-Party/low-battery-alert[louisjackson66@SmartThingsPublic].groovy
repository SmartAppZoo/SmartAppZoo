/**
 *  Low Battery Alert
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.2   31 Jan 2016
 *
 *	Version History
 *
 *	1.0.2   31 Jan 2016		Changed battery check to once a day.
 *	1.0.1   30 Jan 2016		Added version number to the bottom of the input screen
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
 *  for the specific language governing permissions and -limitations under the License.
 *
 */
definition(
    name: "Low Battery Alert",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Determines if the battery is below a given threshold in selected devices.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Health%20&%20Wellness/health6-icn@2x.png")


preferences {
    section("Device to Monitor:") {
            input "thebattery", "capability.battery", title: "with batteries...", multiple: true,   required: true }
  
    section("Battery Settings:") {
      		input "minThreshold", "number",   title: "when below... (default 40)%", defaultValue:40,   required: false }

	section("Notification:") {
        input "time", "time", title: "Notify at what time daily?", required: true
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
        input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: false
    }
    
    section ("Version 1.0.2") {}
}

def installed() {
   	log.trace "(0A) ${app.label} - installed() - settings: ${settings}"
  	initialize()
}

def updated() {
	log.info "(0B) ${app.label} - updated()"
    unschedule() //un-schedule
	initialize()
}

def initialize() {
   	log.info "(0C) ${app.label} - initialize() - Can schedule? ${canSchedule()}"
    
    //runEvery1Hour(doBatteryCheck) // call doBatteryCheck every hour
    //runEvery3Hours(doBatteryCheck) // call doBatteryCheck every 3 hours
	//schedule("2016-01-31T15:45:00.000-0600", doBatteryCheck)  // call doBatteryCheck every day at 3:45 PM CST
    schedule(settings.time, doBatteryCheck) //At user defined time
	//schedule("0 30 11 ? * SAT", doBatteryCheck) // call doBatteryCheck at 11:30am every Saturday of the month
    
    doBatteryCheck() // ...and check now!
}

def doBatteryCheck() {
	log.trace "(0D) ${app.label} - doBatteryCheck() : ${settings}"
    
    def nDevBelow  = 0
    def strMessage = ""

	for (batteryDevice in thebattery) 
    {
    	def batteryLevel = batteryDevice.currentValue("battery")

        if ( batteryLevel <= settings.minThreshold.toInteger() ) 
        {
            log.warn "(0E) - current value for ${batteryDevice.label} is ${batteryLevel}"
			strMessage += "- ${batteryDevice.label}: ${batteryLevel}%.\n"
			nDevBelow++
        }
        else
			log.info "(0F) - current value for ${batteryDevice.label} is ${batteryLevel}"
    }

    if ( nDevBelow ){
    	send("The ${app.label} SmartApp determined you have ${nDevBelow} device(s) below the set battery alert level of ${settings.minThreshold.toInteger()}%:\n\n${strMessage}")
    }
}

private send(msg) {
    log.info "(01) sending message ${msg}"

	if (sonos) sonos.playTextAndResume(msg, 100)

	if (location.contactBookEnabled) 
    {
    	log.trace "(02) send to contact ${recipients}"
        sendNotificationToContacts(msg, recipients)  //Sends the specified message to the specified contacts and the Notifications feed.
    } 
    else 
    	sendNotificationEvent(msg)
        
    if (phone)
    {
        log.trace "(03) send to contact ${phone}"
        sendSms(phone, msg) //Sends the message as an SMS message to the specified phone number and displays it in Hello, Home. The message can be no longer than 140 characters.
    }   
}
