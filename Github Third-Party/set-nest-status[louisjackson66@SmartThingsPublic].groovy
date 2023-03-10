/**
 *  Modify Nest Presence with Hub Status
 *
 *  Copyright 2014 Dan VanWinkle
 *
 *  Version 1.0.1   03 Feb 2016
 *
 *	Version History
 *
 *	1.0.1   03 Feb 2016		Modified to support multiple Nest thermostats by Lou Jackson
 *	1.0.0	27 Jan 2016		Creation by Dan VanWinkle
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
  name: "Set Nest Status",
  namespace: "dvanwinkle",
  author: "Dan VanWinkle",
  description: "Changes your Nest presence to match your SmartThings Hub presence",
  category: "My Apps",
  iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png"
)

preferences 
{
	section("Change status of the following Nest(s)...") {input ("nest", "capability.thermostat", required: false, multiple: true)}
	section("To home with the following mode...")        {input("presentMode", "mode", title: "Mode?", required: false)}
	section("To away with the following mode...")        {input("awayMode", "mode", title: "Mode?", required: false)}
	section ("Version 1.0.1") { }
}

def installed() 
{
	log.trace "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.trace "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, locationChanged)
	log.trace "Starting to update ${nest.displayName} to home with mode ${presentMode} and to away with mode ${awayMode}."
 }

def locationChanged(evt) 
{
    nest.each 
    {
		if ((presentMode == evt.value) || (awayMode == evt.value))
    	{
			log.info "Changing ${it.label} to ${evt.value}"
            (presentMode == evt.value) ? it.present() : it.away()
    		sendNotificationEvent "I changed ${it.label} to ${evt.value}."
		}
    }
}
