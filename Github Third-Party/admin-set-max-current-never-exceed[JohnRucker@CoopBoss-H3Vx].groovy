/**
 *  Set Max Current Never Exceed
 *
 *  Copyright 2015 John Rucker
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
    name: "Admin (Set Max Current Never Exceed)",
    namespace: "JohnRucker",
    author: "John.Rucker@Solar-current.com",
    description: "This CoopBoss admin application sets new Max Current Never Exceed for the door's object detection circuit.",
    category: "My Apps",
    iconUrl: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo.png",
    iconX2Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo2x.png",
    iconX3Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo3x.png")

preferences {
    page(name: "page1", title: "Select CoopBoss", nextPage: "page2", uninstall: true) {
        section {
        	paragraph "CAUTION This SmartApp allows you to set a new Max Current Never Exceed (MCNE) for your CoopBoss. The MCNE is used as a safety setting during a high current close (forced close) or an open.  This setting should be set to match the max current of the 12v power supply connected to your CoopBoss.  Typical settings are 3000mA or 4000mA."
            paragraph "Setting this value incorrectly may cause the door to not close properly or close with too much pressure.  Please consult the CoopBoss online manual for guidelines on setting this value."  
			input(name: "doorSensor", type: "capability.doorControl", title: "Select CoopBoss to change", required: true, multiple: false)
        }
    }

    page(name: "page2", title: "Enter new base door current", install: true, uninstall: true)

}

def page2() {
    dynamicPage(name: "page2") {
        section {
			log.debug "Sending read BaseCurrentNE command "
    		doorSensor.readBaseCurrentNE()        
        	def crntBaseCurrentNE = doorSensor.currentState("baseCurrentNE")
			if (crntBaseCurrentNE == null){
            	paragraph "Enter a new value in milliamps (3000 = 3amps)"
            }else{
            	paragraph "Enter a new value in milliamps the current value is ${crntBaseCurrentNE.value}mA"
            }
        	input(name: "newBaseCurrent", type: "number", title: "Enter new base current and push done.")
         	}
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
    setBaseCurrent()
}

def setBaseCurrent(){
	log.debug "Setting setBaseCurrentNE = ${newBaseCurrent} "
    doorSensor.setBaseCurrentNE(newBaseCurrent)
}