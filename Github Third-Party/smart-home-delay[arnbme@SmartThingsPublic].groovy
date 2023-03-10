/**
 *  Smart Home Delay
 *
 *  Copyright 2017 Arn Burkhoff
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
 *  Aug 11, 2017 v1.0.6 change icons to hourglass
 *  Aug 10, 2017 v1.0.5 Fix defaultValue
 *  Aug 08, 2017 v1.0.4a Unschedule without a routine name is a disaster, add routine name
 *  Aug 08, 2017 v1.0.4 Add subscription to location alarm state and kill when it changes to off
 *  Aug 07, 2017 v1.0.3 Due to reports of RunIn being unreliable, change to RunOnce
 *
 *  Aug 04, 2017 v1.0.2 Change keypad to an optional device. limit time delay range: 10 to 60 seconds
 *						Rename to Smart Home Delay from Front Door Opens, 
 * 						allow for multiple: contact sensors, sirens, and keypads
 *						add unschedule in case there are multiple triggers
 *
 *  Aug 02, 2017 v1.0.1 Change to use alarm system state timing vs keypad lastupdate
 *					Eliminates requirement for keypad, mods to keypad DTH, although I continue using the Mods
 *
 *  Jul 30, 2017 v1.0.0 initial version. Perform Entry Delay missing in SmartHome.
 *					Requires modified version of Mitch Pond's Centralite Keypad DTH adding
 * 					seconds to time field, and also not updating lastUpdate when doing entryDelay.
 * 
 *  This SmartApp creates a (simulated) entry delay missing in SmartHome
 *  Although the app can handle multiple contact sensors per instance, you may prefer to create an instance of 
 *  this app per contact sensor, allowing for: varying entry timings; and using a separate simulated contact sensor 
 *  to make sense of any intrusion messages from SmartHome
 */
definition(
    name: "Smart Home Delay",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "SmartApp simulating missing entry delay option in SmartHome",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")


preferences
	{
    section("Smart Home Delay Parameters") {
        input "thecontact", "capability.contactSensor", required: true, multiple: true,
        	title: "One or more Monitored Contact Sensors, do not monitor in Smarthome"
        input "thesimcontact", "capability.contactSensor", required: true,
        	title: "Simulated Contact Sensor, monitored by SmartHome"
        input "thedelay", "number", required: true, range: "10..60", defaultValue: 30,
        	title: "Alarm delay time in seconds from 10 to 60"
        input "thekeypad", "capability.button", required: false, multiple: true,
        	title: "Zero or more Optional Keypads: sounds entry delay tone "
        input "thesiren", "capability.alarm", required: false, multiple: true,
        	title: "Zero or more Optional Sirens to Beep"
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
	subscribe(location, "alarmSystemStatus", alarmStatusHandler)
	subscribe(thecontact, "contact.open", doorOpensHandler)
}

def doorOpensHandler(evt)
	{
//	Store lastupdate time from keypad
//	def lastupdt = thekeypad.currentValue("lastUpdate") deprecated v 1.0.1
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	def lastupdt = alarm?.date.time
	log.debug "doorOpensHandler called: $evt.value $alarmstatus $lastupdt"

//	alarmstaus values: off, stay, away
	if (alarmstatus == "stay" || alarmstatus == "away")
		{
//		When keypad is defined: Issue an entrydelay for the delay on keypad. Keypad beeps
		if (settings.thekeypad)
			{
			thekeypad.setEntryDelay(thedelay)
			}

//		when siren is defined: wait 2 seconds allowing people to get through door, then blast a siren warning beep
		if (settings.thesiren)
			{
			thesiren.beep([delay: 2000])
			}

//		Trigger Alarm in thedelay seconds by opening the virtual sensor.
//		Do not delay alarm when additional triggers occur by using overwrite: false
//		runIn(thedelay, soundalarm, [data: [lastupdt: lastupdt], overwrite: false])
		def now = new Date()
		def runTime = new Date(now.getTime() + (thedelay * 1000))
		runOnce(runTime, soundalarm, [data: [lastupdt: lastupdt], overwrite: false]) 
		}
	}

def alarmStatusHandler(evt)
	{
	log.debug("alarmStatusHandler caught alarm status change: ${evt.value}")
	if (evt.value=="off")
		{
		unschedule(soundalarm)		//kill any lingering future tasks
		}
	}


//	Sound the Alarm. When SmartHome sees simulated sensor change to open, alarm will sound
def soundalarm(data)
	{
	def alarm2 = location.currentState("alarmSystemStatus")
	def alarmstatus2 = alarm2.value
	def lastupdt = alarm2.date.time
//	def lastupdt = thekeypad.currentValue("lastUpdate")
	log.debug "soundalarm called: $alarmstatus2 $data.lastupdt $lastupdt"
	if (alarmstatus2=="off")		//This compare is optional, but just incase next test fails
		{}
	else
	if (data.lastupdt==lastupdt)	//if this does not match, the system was set off then rearmed in delay period
		{
		log.debug "alarm triggered"
		thesimcontact.close()		//must use a live simulated sensor or this fails in Simulator
		thesimcontact.open()
		thesimcontact.close([delay: 4000])
		}
	unschedule(soundalarm)					//kill any lingering tasks caused by using overwrite false on runIn
	}