/**
 *  Turn Off If Opened
 *
 *  Turn off a light or switch whenever a contact sensor is opened
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
	name: "Turn Off If Opened",
	namespace: "pvanbaren",
	author: "Philip Van Baren",
	description: "When something is opened, turn something else off.",
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
	section("Turn off a switch...")
	{
		input "switch1", "capability.switch", multiple: true
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
	log.debug "Welcome home!"
	switch1.off()
}