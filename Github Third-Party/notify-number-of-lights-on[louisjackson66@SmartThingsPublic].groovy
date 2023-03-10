/**
 *  Notify Number of Lights On
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.2	30 Jan 2016
 *
 *	Version History
 *
 *	1.0.2   31 Jan 2016		Added version number to the bottom of the input screen
 *	1.0.1	30 Jan 2016		Identifies the lights that are on by name.
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
    name: "Notify Number of Lights On",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Send a notification of the number of lights on.  This feature use to be part of the dashboard.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png")


preferences 
{
	section("Select Things to Monitor:") {
		input "switches", "capability.switch", title: "Select Lights/Switches...", multiple: true, required:true
    }
    
    section ("Version 1.0.2") {}
}

def installed() 
{
	log.trace "(0A) ${app.label} - installed() - settings: ${settings}"
	initialize()
}

def updated() 
{
	log.info "(0B) ${app.label} - updated()"
	unsubscribe()
	initialize()
}

def initialize() 
{
	log.info "(0C) ${app.label} - initialize()"
    subscribe(switches, "switch", onHandler)
}

def onHandler(evt) 
{
    def nOnCnt = 0
    def nTotalCnt = 0
  	def strMessage = ""
    
    log.trace "(0D)  ${app.label} - onHandler checking switches"

    switches.each 
    {
    	//log.trace "(0E) ${app.label} - Checking ${it.label} - current state:${it.latestState("switch").value}"
        nTotalCnt++
        
		if(it.latestState("switch").value in ["on", "setLevel"])
    	{
            nOnCnt++
			strMessage += "\n- ${it.label}"
		}
    }
    
    if (nOnCnt || nTotalCnt)
    {
		strMessage = "${app.label} has detected the following ${nOnCnt} out of ${nTotalCnt} switch(s)\\light(s) on:${strMessage}"
    	log.warn "(0F) ${app.label} - ${strMessage}"
    	sendNotificationEvent(strMessage)
    }
    
    log.trace "(10) ${app.label} - onHandler completed check."
}
