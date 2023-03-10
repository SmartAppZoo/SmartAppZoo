/**
 *  Pet Feeder Reminder
 *
 *  Copyright 2015 Jake Burgy
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
/**
 * Based on "Has Barkley Been Fed?" by the SmartThings team.
 * https://github.com/rappleg/SmartThings/blob/master/smartapps/smartthings/has-barkley-been-fed.groovy
 */
 
definition(
    name: "Pet Feeder Reminder",
    namespace: "qJake",
    author: "Jake Burgy",
    description: "Reminds you to feed your pets between two times in a certain day if a motion sensor has not moved within that timeframe.",
    category: "Pets",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Pets/App-FeedMyPet.png",
    iconX2Url: "hhttps://s3.amazonaws.com/smartapp-icons/Pets/App-FeedMyPet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Pets/App-FeedMyPet@2x.png")

preferences {
	section("1. Choose your pet feeder sensor.") {
		input "feeder1", "capability.contactSensor", title: "Which sensor?"
	}
	section("2. Specify when you feed your pets.") {
		input "timefrom", "time", title: "Between..."
		input "timeto", "time", title: "And..."
	}
	section("3. If I forget by the ending time, text me.") {
    	input "msg", "text", title: "Message? (Optional)", description: "Oops! Don't forget to feed the pets!", required: false
		input "phone1", "phone", title: "Phone number?"
		input "phone2", "phone", title: "Secondary number? (Optional)", required: false
	}
}

def installed()
{
	schedule(timeto, "scheduleCheck")
}

def updated()
{
	unschedule()
	schedule(timeto, "scheduleCheck")
}

def scheduleCheck()
{
	def now = new Date()
	def from = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", timefrom)
    
    // Reset date to current date
    from.date = now.date
    from.month = now.month
    from.year = now.year
    
	def feederEvents = feeder1.eventsBetween(from, now)
	def feederOpened = feederEvents.count { it.value && it.value == "open" } > 0

	def textMsg = msg ? msg : "Oops! Don't forget to feed the pets!"

	if (!feederOpened)
    {
		sendSms(phone1, textMsg)
        if(phone2)
        {
			sendSms(phone2, textMsg)
        }
	}
}