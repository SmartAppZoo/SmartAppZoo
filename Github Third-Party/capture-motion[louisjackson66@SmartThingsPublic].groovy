/**
 *  Capture Motion
 *
 *  Copyright 2016 Louis Jackson
 *
 *  Version 1.0.1   31 Jan 2016
 *
 *	Version History
 *
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
    name: "Capture Motion",
    namespace: "lojack66",
    author: "Louis Jackson",
    description: "Uses a camera to snap a picture when motion is detected",
    category: "My Apps",
    iconUrl:   "http://cdn.device-icons.smartthings.com/Entertainment/entertainment9-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment9-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment9-icn@2x.png")


preferences {
    page(name: "page1", uninstall: true, install: true)
}

def page1() {
    dynamicPage(name: "page1") {
    
        section("Select Things to Control:") 
    	{
			input "themotion", "capability.motionSensor", title: "Select motion sensor", multiple: false, required: true
			input "camera", "capability.imageCapture", title: "Select camera", multiple: false, required: true
            input "bBurst", "bool", title: "Burst shots?", required: false, defaultValue:false, submitOnChange: true
			if(bBurst) {input "nBurstShots", "number", title: "Snap shots... (default 4)", defaultValue:4,  required: false}
    	}
        
        section("Advanced Options") 
        {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
    	}
        
        section ("Version 1.0.1") { }
    }
}

def installed() 
{
	log.info "(0A) ${app.label} - installed() - settings: ${settings}"
	initialize()
}

def updated() 
{
	log.trace "(0B) ${app.label} - updated()"
    unsubscribe()
    initialize()
}

def initialize() 
{
	log.trace "(0C) ${app.label} - initialize()"
    subscribe(themotion, "motion.active", SnapPicture)
}

def SnapPicture(evt) 
{
	log.info "(0D) ${app.label} - ${themotion.label} is active - ${settings}"
        
    if(bBurst)
    {
    	(1..nBurstShots).each 
    	{
			camera.take(delay: (1000 * it))
        	log.trace "(0E) ${app.label} - Burst ${it} - ${camera.currentImage}"
    	}
     } else
     {
         camera.take()
         log.trace "(0E) ${app.label} - Single - ${camera.currentImage}"
     }
}
