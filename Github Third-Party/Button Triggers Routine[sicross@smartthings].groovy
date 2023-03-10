/**
 *  Button Triggers Routine
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
    name: "Button Triggers Routine",
    namespace: "sicross",
    author: "Simon Cross",
    description: "Lets a (physical) button trigger a routine",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "getPref")
}
	
def getPref() {    
    dynamicPage(name: "getPref", install:true, uninstall: true) {
    
    	def phrases = location.helloHome?.getPhrases()*.label
        if (!phrases) {
        	section("Setup Some Routines") {
            	paragraph "To use this SmartApp, first setup some Routines."
            }
        	return // lets get out of here
        }
        phrases.sort()
    
    	section("Choose a Trigger Device...") {
			input "buttonDevice", "capability.button", title: "Device", multiple: false, required: true, submitOnChange: true
    	}
        
        log.debug "Switch: ${buttonDevice}"
        
        if (buttonDevice) {
        	def numberOfButtons = buttonDevice.currentValue('numberOfButtons')
            
            section("Assign Routines to Buttons") {
            for (def i=1; i<=numberOfButtons; i++)
            	input "button_${i}", "enum", title: "Button ${i}", options: phrases, multiple: false, required: false 
            }
           
        }
        
		section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false)
    		mode title: "Set for specific mode(s)", required: false
		}
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    subscribe(buttonDevice, "button", switchHandler)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe(buttonDevice, "button", switchHandler)
}

def switchHandler(evt) {

	log.debug "event: button $evt.value"  
    log.debug "event data: ${evt.data}"  
	
    if (evt.value == "pushed") {
		def data = parseJson(evt.data)
    	log.debug "button ${data.buttonNumber} pushed"  
        log.debug "stored buttonNumber: ${buttonNumber}"  
        
        if (data.buttonNumber) {
        	def buttonNumber = data.buttonNumber.toInteger()
            if (settings["button_${buttonNumber}"]) {
            	def routine = settings["button_${buttonNumber}"]
            	log.debug "We have a setting for that button!"
                log.debug "Triggering ${routine}"
                location.helloHome.execute(routine) 
            } else {
            	log.debug "We DO NOT have a setting for that button. Ignoring."
            }
        }
    }
}
