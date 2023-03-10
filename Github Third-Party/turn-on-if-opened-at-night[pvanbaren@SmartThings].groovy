/**
 *  Turn On If Opened At Night
 *
 *  Turn on a light or switch whenever a contact sensor is opened
 *  and the current time is between sunset and sunrise.
 *
 *  Copyright 2016 Philip Van Baren
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
	name: "Turn On If Opened At Night",
	namespace: "pvanbaren",
	author: "Philip Van Baren",
	description: "When something is opened after sunset, turn something else on.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpWhenOpened.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpWhenOpened@2x.png"
)

preferences
{
	section("When something is opened...")
	{
		input "sensor1", "capability.contactSensor", title: "What?", multiple: true
	}
	section("Turn on a light...")
	{
		input "switch1", "capability.switch", multiple: true
	}
	section("Between Sunset and Sunrise...")
	{
		paragraph "Sunset offset (optional)"
		input "sunsetOffsetValue", "text", title: "Minutes (+/-)", required: false
		paragraph "Sunrise offset (optional)"
		input "sunriseOffsetValue", "text", title: "Minutes (+/-)", required: false
	}
}

def installed()
{
	subscribe(sensor1, "contact.open", contactOpenHandler)
}

def updated()
{
	unsubscribe()
	installed()
}

def contactOpenHandler(evt)
{
	def now = new Date()
	
	def sunriseOffset = sunriseOffsetValue ?:"00:00"
	def sunsetOffset = sunsetOffsetValue ?:"00:00"
	
	def sunTime = getSunriseAndSunset(sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset);
	
	log.debug "nowTime: $now"
	log.debug "adjusted riseTime: $sunTime.sunrise"
	log.debug "adjusted setTime: $sunTime.sunset"
	
	if (now > sunTime.sunset)
	{
		switch1.on()
		log.debug "Welcome home at night!"
	}
	else if (now < sunTime.sunrise)
	{
		switch1.on()
		log.debug "Welcome home in the morning!"
	}
}
