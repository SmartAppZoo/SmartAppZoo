/**
*  Bulb Control
*
*  Copyright 2016 Pedro Toledo
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
    name: "Bulb Control",
    namespace: "ptoledo",
    author: "Pedro Toledo",
    description: "To interface the LIFX Bulbs to ensure on/off/color behavior",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "Configuration")
}

def Configuration() {
    dynamicPage(name: "Configuration", title: "", install: true, uninstall: true) {
        section {
        	input name: "theSwitch", title: "Select your controling switch", type: "capability.switch", required: true
            input name: "theBulb", title: "Select your bulb", type: "capability.switch", required: true, submitOnChange: true, multiple: true
        }
        if(theBulb) {
            def color = true;
            theBulb.each {
            	if(!it.hasAttribute("color")){
                	color = false;
                }
            }
            section("On configuration") {
 	            if(color) {
                    input "hue", "number", title: "hue", required: true, range: "0..100"
                    input "saturation", "number", title: "saturation", required: true, range: "0..100"
		    		input "temperature", "number", title: "temperature. Set 0 for color", required: true, range: "2499..9000", defaultValue: "2499"
                    input "level", "number", title: "level", required: true, range: "0..100"                    
                } else {
		    		input "temperature", "number", title: "temperature", required: true, range: "2500..9000"
                    input "level", "number", title: "level", required: true, range: "0..100"
                }   
		    }
   		}
        section() {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
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
    subscribe(theSwitch, "switch", bulbHandler)
}

def bulbHandler(evt) {
	if(evt.value == "on") {
       	onBulb();
    } else {
        if(theSwitch.currentSwitch == "on"){
        	onBulb()
        } else {
        	offBulb()
        }
    }
}

def onBulb() {
  def rerun = 0
  theBulb.each {
    log.debug it.displayName+" - ON"
	def action = 0
    if(theSwitch.currentSwitch == "on"){
      if(temperature == 2499 && (it.currentSwitch != "on" || it.currentHue != hue || it.currentSaturation != saturation || it.currentLevel != level)) {
        action = 1
      }
      if(temperature != 2499 && (it.currentSwitch != "on" || it.currentColorTemperature != temperature || it.currentLevel != level)) {
      	action = 1
      }
    }
    switch(action){
        	case 0:
                if(it.currentSwitch == "on") {
            	    log.debug it.displayName+" - ON done!"
                    break
                }
                log.debug it.displayName+" - ON unexpected termination, retrying"
            case 1:
                log.debug it.displayName+" - ON try!"
                it.setLevel(level)
                if(temperature == 2499){
                    log.debug it.displayName+" - With current status: "+it.currentSwitch+" hue: "+it.currentHue+" saturation: "+it.currentSaturation+" level: "+it.currentLevel
                    it.setHue(hue)
                    it.setSaturation(saturation)
                } else {
                    log.debug it.displayName+" - With current status: "+it.currentSwitch+" temperature: "+it.currentColorTemperature+" level: "+it.currentLevel
                    it.setColorTemperature(temperature)
                }
                rerun = 1;
                break
            default:
            	log.debug it.displayName+" - ON done! (error)"
                break
        }
    }
    if(rerun){
      runIn(4, onBulb)    
    }
}

def offBulb() {
    def rerun = 0
    theBulb.each {
		if(it.currentswitch != "off"){
            log.debug it.displayName+" - OFF try"
            rerun = 1
	    	it.off()    
	    } else {
	    	log.debug it.displayName+" - OFF done!"
	    }
    }
    if(rerun){
        runIn(4, offBulb)
    }
}

