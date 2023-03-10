/**
 *  HomeAutomations
 *
 *  Copyright 2021 Lukas Weier
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
    name: "HomeAutomations",
    namespace: "sajiko5821",
    author: "Lukas Weier",
    description: "Control your basic automations",
    category: "Convenience",
    parent: parent ? "sajiko5821:HomeAutomations" : null,
    iconUrl: "https://raw.githubusercontent.com/sajiko5821/Home-Automations/main/Icons/Home_Automations_Logo.png",
    iconX2Url: "https://raw.githubusercontent.com/sajiko5821/Home-Automations/main/Icons/Home_Automations_Logo.png",
    iconX3Url: "https://raw.githubusercontent.com/sajiko5821/Home-Automations/main/Icons/Home_Automations_Logo.png")


preferences {
	page name: "mainMenu"
    page name: "parentPage"
    page name: "childPage"
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
	subscribe(presenceDetector, "motion.active", motionHandler)
    subscribe(presenceDetector, "motion.inactive", stopMotionTimer)
}

def mainMenu(){
	parent ? childPage(): parentPage()
}

def parentPage(){
	dynamicPage(name: "parentPage", title: "Rooms", install: true, uninstall: true, submitOnChange: true){
    	section{
        	app(name: "childapp", appName: "HomeAutomations", namespace: "sajiko5821", title: "Create new Room", multiple: true)
        }
    }
}

def childPage(){
	dynamicPage(name: "childPage", install: true, uninstall: true){
    	section(){
        	label title: "Enter Room Name", defaultValue: app.label, required: true
        }
        section(){
        	input(name: "presenceDetector", type: "capability.motionSensor", title: "Presence Detector for this Room", multiple: true, required: true)
            input(name: "turnOffDelay", type: "number", title: "Time after No Motion Reset in Seconds", default: 0, required: false)
        }
        section("Morning"){
        	input(name: "lightsMorning", type: "capability.switch", title: "Select Lights", multiple: true, required: false)
        	input(name: "levelMorning", type: "number", title: "Set Brightness", required: false)
            input(name: "colorMorning", type: "enum", title: "Set Color", required: false,
            	options:["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"])
          	input(name: "tempMorning", type: "number", title: "Set Color Temperature (°K)", required: false)
        }
        section("Day"){
        	input(name: "lightsDay", type: "capability.switch", title: "Select Lights", multiple: true, required: false)
        	input(name: "levelDay", type: "number", title: "Set Brightness", required: false)
            input(name: "colorDay", type: "enum", title: "Set Color", required: false,
            	options:["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"])
          	input(name: "tempDay", type: "number", title: "Set Color Temperature (°K)", required: false)
        }
        section("Evening"){
        	input(name: "lightsEvening", type: "capability.switch", title: "Select Lights", multiple: true, required: false)
        	input(name: "levelEvening", type: "number", title: "Set Brightness", required: false)
            input(name: "colorEvening", type: "enum", title: "Set Color", required: false,
            	options:["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"])
          	input(name: "tempEvening", type: "number", title: "Set Color Temperature (°K)", required: false)
        }
        section("Night"){
        	input(name: "lightsNight", type: "capability.switch", title: "Select Lights", multiple: true, required: false)
        	input(name: "levelNight", type: "number", title: "Set Brightness", required: false)
            input(name: "colorNight", type: "enum", title: "Set Color", required: false,
            	options:["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"])
          	input(name: "tempNight", type: "number", title: "Set Color Temperature (°K)", required: false)
        }
        section("Midnight"){
        	input(name: "lightsMidnight", type: "capability.switch", title: "Select Lights", multiple: true, required: false)
        	input(name: "levelMidnight", type: "number", title: "Set Brightness", required: false)
            input(name: "colorMidnight", type: "enum", title: "Set Color", required: false,
            	options:["Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"])
          	input(name: "tempMidnight", type: "number", title: "Set Color Temperature (°K)", required: false)
        }
    }
}

def stopMotionTimer(evt){
	runIn(turnOffDelay, stopMotionHandler)
}

def stopMotionHandler(evt){
	if(location.mode == "Morning"){
        lightsMorning?.off()
   	}    
 	if(location.mode == "Day"){
     	lightsDay?.off()
   	}
  	if(location.mode == "Evening"){
        lightsEvening?.off()
   	}
   	if(location.mode == "Night"){
        lightsNight?.off()
   	}
   	if(location.mode == "Midnight"){
        lightsMidnight?.off()
  	}
}

def motionHandler(evt){
	if(location.mode == "Morning"){
    	if(levelMorning){
        	lightsMorning?.setLevel(levelMorning)
        }
    	if(tempMorning){
        	lightsMorning?.setColorTemperature(tempMorning)
        }
        if(colorMorning){
    		setColor(colorMorning)
     	}
    }
    
    if(location.mode == "Day"){
   		if(levelDay){
        	lightsDay?.setLevel(levelDay)
        }
        if(tempDay){
        	lightsDay?.setColorTemperature(tempDay)
        }
        if(colorDay){
        	setColor(colorDay)
        }
    }
    
    if(location.mode == "Evening"){
    	if(levelEvening){
        	lightsEvening?.setLevel(levelEvening)
        }
        if(tempEvening){
        	lightsEvening?.setColorTemperature(tempEvening)
        }
        if(colorEvening){
        	setColor(colorEvening)
        }
    }
    
    if(location.mode == "Night"){
    	if(levelNight){
        	lightsNight?.setLevel(levelNight)
        }
    	if(tempNight){
        	lightsNight?.setColorTemperature(tempNight)
        }
        if(colorNight){
    		setColor(colorNight)
     	}
    }
    
    if(location.mode == "Midnight"){
    	if(levelMidnight){
        	lightsMidnight?.setLevel(levelMidnight)
        }
        if(tempMidnight){
        	lightsMidnight?.setColorTemperature(tempMidnight)
        }
        if(colorMidnight){
        	setColor(colorMidnight)
        }
    }
}

def setColor(acolor){
    	def hueColor = 23
        def saturation = 56
        switch(acolor){
			case "Blue":
				hueColor = 70
                saturation = 100
				break;
			case "Green": 
				hueColor = 35
                saturation = 100
				break;
			case "Yellow":
				hueColor = 10
                saturation = 100
				break;
			case "Orange":
				hueColor = 5
                saturation = 100
				break;
			case "Purple":
				hueColor = 75
                saturation = 100
				break;
			case "Pink":
				hueColor = 83
                saturation = 100
				break;
			case "Red":
				hueColor = 99  //for Phillips Hue Bulbs change to 100
                saturation = 100
				break;
        }
 		if(location.mode == "Morning"){
        	lightsMorning?.setColor([hue: hueColor, saturation: saturation])
      	}
        if(location.mode == "Day"){
        	lightsDay?.setColor([hue: hueColor, saturation: saturation])
      	}
        if(location.mode == "Evening"){
        	lightsEvening?.setColor([hue: hueColor, saturation: saturation])
      	}
        if(location.mode == "Night"){
        	lightsNight?.setColor([hue: hueColor, saturation: saturation])
      	}
        if(location.mode == "Midnight"){
        	lightsMidnight?.setColor([hue: hueColor, saturation: saturation])
      	}
    }
