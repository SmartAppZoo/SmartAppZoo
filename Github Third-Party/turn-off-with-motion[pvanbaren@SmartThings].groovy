/**
 *  Turn Off With Motion
 *
 *	Turn off one or more switches as soon as movement is detected.
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
 *
 */
definition(
	name: "Turn Off With Motion",
	namespace: "pvanbaren",
	author: "Philip Van Baren",
	description: "Turns off a switch when motion occurs",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/switches.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/switches@2x.png")

preferences
{
	section("When there is movement...")
	{
		input "themotion", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("Turn off switch(es)...")
	{
		input "theswitch", "capability.switch", title: "Which?", multiple: true
	}
}

def installed()
{
	subscribe(themotion, "motion.active", motionDetectedHandler)
}

def updated()
{
	unsubscribe()
	installed()
}

def motionDetectedHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	log.debug "Turning off the switch"

	theswitch.off()
}
