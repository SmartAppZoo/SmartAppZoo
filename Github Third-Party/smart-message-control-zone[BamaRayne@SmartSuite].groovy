/* 
* Smart Message Control Zones
*
*
* 
*  Copyright 2018 Jason Headley
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
/**********************************************************************************************************************************************/
import org.apache.log4j.Logger
import org.apache.log4j.Level
definition(
	name			: "Smart Message Control Zone",
    namespace		: "Echo",
    author			: "bamarayne",
	description		: "Smart Message Controls - Only send messages where they will be heard",
	category		: "My Apps",
    parent			: "Echo:Smart Message Control",
	iconUrl			: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Audio.png",
	iconX2Url		: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Audio2x.png",
	iconX3Url		: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Audiox.png")
/**********************************************************************************************************************************************/
private def version() { 
    	def text = "Smart Message Control Ver 1.0 / R.0.0.1, Release date: in development, Initial App Release Date: not released" 
	}

preferences {

    page name: "mainProfilePage"
    page name: "speakers"
    page name: "conditions"
    page name: "messages"
    page name: "restrictions"
    page name: "certainTime"
    page name: "certainTimeR"
   
}

/******************************************************************************
	MAIN PROFILE PAGE
******************************************************************************/
def mainProfilePage() {	
    dynamicPage(name: "mainProfilePage", title:"", install: true, uninstall: installed) {
        section ("Details and Status") {
            label title: "Name this Room", required:true
            }
        section ("Pause Announcements") {
        	input "rPause", "bool", title: "Enable Announcements to this Zone", required: false, submitOnChange: true,
            image: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Enable.png"
            }
        if (rPause == null) {
        	section ("") {
            paragraph "This Zone has not been configured, please activate to continue."
           	}
        }    
        if (rPause == false) {
        section(""){
        	paragraph "This Zones activity has been paused and will not receive any messages.",
            image: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Warn.png"
            }
        }    
        else if(rPause == true) {    
    	    section ("${app.label}" + "'s Configuration") {
                href "speakers", title: "Select Audio Devices in this Zone", description: pDevicesComplete(), state: pDevicesSettings(),
                image: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Speakers.jpg"
                href "conditions", title: "Configure Zone Activity Conditions", description: pConditionComplete(), state: pConditionSettings(),
                image: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Activity.jpg"
                href "restrictions", title: "Configure Zone Restrictions", description: pRestrictionsComplete(), state: pRestrictionsSettings(),
                image: "https://raw.githubusercontent.com/BamaRayne/SmartMessageControl/master/Icons/Restrictions.png"
            }
        }    
    }
}
 
/******************************************************************************
	SPEAKERS SELECTIONS
******************************************************************************/
page name: "speakers"
def speakers() {
	dynamicPage(name: "speakers", title: "Select the speakers for this zone",install: false, uninstall: false) {
        section ("Audio Output Devices"){
        	input "synthDevice", "capability.speechSynthesis", title: "Speech Synthesis Devices", multiple: true, required: false
        	}
        section ("") {
        	input "echoDevice", "device.echoSpeaksDevice", title: "Amazon Alexa Devices", multiple: true, required: false
            	if (echoDevice) { input "eVolume", "number", title: "Set the volume", description: "0-100 (default value = 30)", required: false, defaultValue: 30
                }
            }
        section ("") {
            input "sonosDevice", "capability.musicPlayer", title: "Music Player Devices", required: false, multiple: true, submitOnChange: true    
            if (sonosDevice) {
                input "volume", "number", title: "Temporarily change volume", description: "0-100% (default value = 30%)", required: false
            	}
            }
        section ("Text Messages" ) {
            input "sendText", "bool", title: "Enable Text Notifications", required: false, submitOnChange: true     
            if (sendText){      
                paragraph "You may enter multiple phone numbers separated by comma to deliver the Alexa message. E.g. +18045551122,+18046663344"
                input name: "sms", title: "Send text notification to (optional):", type: "phone", required: false
            	}
            }
        section ("Push Messages") {
            input "push", "bool", title: "Send Push Notification (optional)", required: false, defaultValue: false
        	}
        }
	}    


/******************************************************************************
	CONDITIONS SELECTION PAGE
******************************************************************************/
page name: "conditions"
def conditions() {
    dynamicPage(name: "conditions", title: "Activate this Zone when...",install: false, uninstall: false) {
        section ("Location Settings Conditions") {
            input "cMode", "mode", title: "Location Mode is...", multiple: true, required: false, submitOnChange: true
        	input "cSHM", "enum", title: "Smart Home Monitor is...", options:["away":"Armed (Away)","stay":"Armed (Home)","off":"Disarmed"], multiple: false, required: false, submitOnChange: true
            input "cDays", title: "Days of the week", multiple: true, required: false, submitOnChange: true,
                "enum", options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            href "certainTime", title: "Time Schedule", description: pTimeComplete(), state: pTimeSettings()
        }         
        section ("Switch and Dimmer Conditions") {
            input "cSwitch", "capability.switch", title: "Switches", multiple: true, submitOnChange: true, required:false
            if (cSwitch) {
                input "cSwitchCmd", "enum", title: "are...", options:["on":"On","off":"Off"], multiple: false, required: true, submitOnChange: true
                if (cSwitch?.size() > 1) {
                    input "cSwitchAll", "bool", title: "Activate this toggle if you want ALL of the switches to be $tSwitchCmd as a condition.", required: false, defaultValue: false, submitOnChange: true
                	}
                    if (cSwitch) {
                    	input "cSwitchLogic", "enum", title: "Select logic", options:["or":"Or", "and":"And"], multiple: false, required: false, submitOnChange: true
                        }
            }
            input "cDim", "capability.switchLevel", title: "Dimmers", multiple: true, submitOnChange: true, required: false
            if (cDim) {
                input "cDimCmd", "enum", title: "is...", options:["greater":"greater than","lessThan":"less than","equal":"equal to"], multiple: false, required: false, submitOnChange: true
                if (cDimCmd == "greater" ||cDimCmd == "lessThan" || cDimCmd == "equal") {
                    input "cDimLvl", "number", title: "...this level", range: "0..100", multiple: false, required: false, submitOnChange: true
                if (cDim.size() > 1) {
                    input "cDimAll", "bool", title: "Activate this toggle if you want ALL of the dimmers for this condition.", required: false, defaultValue: false, submitOnChange: true
                	}
                }
            }
        }
        section ("Motion and Presence Conditions") {
            input "cMotion", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false, submitOnChange: true
            if (cMotion) {
                input "cMotionCmd", "enum", title: "are...", options: ["active":"active", "inactive":"inactive"], multiple: false, required: true, submitOnChange: true
            	if (cMotion?.size() > 1) {
                	input "cMotionAll", "bool", title: "Activate this toggle if you want ALL of the Motion Sensors to be $cMotionCmd as a condition."
                    }
                }
        	input "cPresence", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false, submitOnChange: true
            if (cPresence) {
                input "cPresenceCmd", "enum", title: "are...", options: ["present":"Present","not present":"Not Present"], multiple: false, required: true, submitOnChange: true
                if (cPresence?.size() > 1) {
                    input "cPresenceAll", "bool", title: "Activate this toggle if you want ALL of the Presence Sensors to be $cPresenceCmd as a condition.", required: false, defaultValue: false, submitOnChange: true
                	}
                }
            }
        section ("Door, Window, and other Contact Sensor Conditions") {
            input "cContactDoor", "capability.contactSensor", title: "Contact Sensors only on Doors", multiple: true, required: false, submitOnChange: true
            	if (cContactDoor) {
                input "cContactDoorCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (cContactDoor?.size() > 1) {
                	input "cContactDoorAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $cContactDoorCmd as a condition.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            input "cContactWindow", "capability.contactSensor", title: "Contact Sensors only on Windows", multiple: true, required: false, submitOnChange: true
            	if (cContactWindow) {
                input "cContactWindowCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (cContactWindow?.size() > 1) {
                	input "cContactWindowAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $cContactWindowCmd as a condition.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            input "cContact", "capability.contactSensor", title: "All Other Contact Sensors", multiple: true, required: false, submitOnChange: true
            	if (cContact) {
                input "cContactCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (cContact?.size() > 1) {
                	input "cContactAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $cContactCmd as a condition.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            }
		section ("Garage Door and Lock Conditions"){
            input "cLocks", "capability.lock", title: "Smart Locks", multiple: true, required: false, submitOnChange: true
            if (cLocks) {
                input "cLocksCmd", "enum", title: "are...", options:["locked":"locked", "unlocked":"unlocked"], multiple: false, required: true, submitOnChange:true
            }
            input "cGarage", "capability.garageDoorControl", title: "Garage Doors", multiple: true, required: false, submitOnChange: true
            if (cGarage) {
                input "cGarageCmd", "enum", title: "are...", options:["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
        	}
        }
        section ("Environmental Conditions") {
        	input "cHumidity", "capability.relativeHumidityMeasurement", title: "Relative Humidity", required: false, submitOnChange: true
            	if (cHumidity) input "cHumidityLevel", "enum", title: "Only when the Humidity is...", options: ["above", "below"], required: false, submitOnChange: true            
            	if (cHumidityLevel) input "cHumidityPercent", "number", title: "this level...", required: true, description: "percent", submitOnChange: true            
                if (cHumidityPercent) input "cHumidityStop", "number", title: "...but not ${cHumidityLevel} this percentage", required: false, description: "humidity"
            input "cTemperature", "capability.temperatureMeasurement", title: "Temperature", required: false, multiple: true, submitOnChange: true
				if (cTemperature) input "cTemperatureLevel", "enum", title: "When the temperature is...", options: ["above", "below"], required: false, submitOnChange: true
                if (cTemperatureLevel) input "cTemperatureDegrees", "number", title: "Temperature...", required: true, description: "degrees", submitOnChange: true
                if (cTemperatureDegrees) input "cTemperatureStop", "number", title: "...but not ${cTemperatureLevel} this temperature", required: false, description: "degrees"
		}
    }
} 

/******************************************************************************
	RESTRICTIONS SELECTION PAGE
******************************************************************************/
page name: "restrictions"
def restrictions() {
    dynamicPage(name: "restrictions", title: "De-Activate this Zone when...",install: false, uninstall: false) {
        section ("Location Settings") {
            input "rMode", "mode", title: "Location Mode is...", multiple: true, required: false, submitOnChange: true
        	input "rSHM", "enum", title: "Smart Home Monitor is...", options:["away":"Armed (Away)","stay":"Armed (Home)","off":"Disarmed"], multiple: false, required: false, submitOnChange: true
            input "rDays", title: "Days of the week", multiple: true, required: false, submitOnChange: true,
                "enum", options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            href "certainTimeR", title: "Time Schedule", description: rTimeComplete(), state: rTimeSettings()
        }         
        section ("Switch and Dimmer") {
            input "rSwitch", "capability.switch", title: "Switches", multiple: true, submitOnChange: true, required:false
            if (rSwitch) {
                input "rSwitchCmd", "enum", title: "are...", options:["on":"On","off":"Off"], multiple: false, required: true, submitOnChange: true
                if (rSwitch?.size() > 1) {
                    input "rSwitchAll", "bool", title: "Activate this toggle if you want ALL of the switches to be $rSwitchCmd as a restriction.", required: false, defaultValue: false, submitOnChange: true
                	}
                }
            input "rDim", "capability.switchLevel", title: "Dimmers", multiple: true, submitOnChange: true, required: false
            if (rDim) {
                input "rDimCmd", "enum", title: "is...", options:["greater":"greater than","lessThan":"less than","equal":"equal to"], multiple: false, required: false, submitOnChange: true
                if (rDimCmd == "greater" ||rDimCmd == "lessThan" || rDimCmd == "equal") {
                    input "rDimLvl", "number", title: "...this level", range: "0..100", multiple: false, required: false, submitOnChange: true
                if (rDim.size() > 1) {
                    input "rDimAll", "bool", title: "Activate this toggle if you want ALL of the dimmers for this restriction.", required: false, defaultValue: false, submitOnChange: true
                	}
                }
            }
        }
        section ("Motion and Presence") {
            input "rMotion", "capability.motionSensor", title: "Motion Sensors", multiple: true, required: false, submitOnChange: true
            if (rMotion) {
                input "rMotionCmd", "enum", title: "are...", options: ["active":"active", "inactive":"inactive"], multiple: false, required: true, submitOnChange: true
            	if (rMotion?.size() > 1) {
                	input "rMotionAll", "bool", title: "Activate this toggle if you want ALL of the Motion Sensors to be $rMotionCmd as a restriction."
                    }
                }
        	input "rPresence", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false, submitOnChange: true
            if (rPresence) {
                input "rPresenceCmd", "enum", title: "are...", options: ["present":"Present","not present":"Not Present"], multiple: false, required: true, submitOnChange: true
                if (rPresence?.size() > 1) {
                    input "rPresenceAll", "bool", title: "Activate this toggle if you want ALL of the Presence Sensors to be $rPresenceCmd as a restriction.", required: false, defaultValue: false, submitOnChange: true
                	}
                }
            }
        section ("Door, Window, and other Contact Sensors") {
            input "rContactDoor", "capability.contactSensor", title: "Contact Sensors only on Doors", multiple: true, required: false, submitOnChange: true
            	if (rContactDoor) {
                input "rContactDoorCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (rContactDoor?.size() > 1) {
                	input "rContactDoorAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $rContactDoorCmd as a restriction.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            input "rContactWindow", "capability.contactSensor", title: "Contact Sensors only on Windows", multiple: true, required: false, submitOnChange: true
            	if (rContactWindow) {
                input "rContactWindowCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (rContactWindow?.size() > 1) {
                	input "rContactWindowAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $rContactWindowCmd as a restriction.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            input "rContact", "capability.contactSensor", title: "All Other Contact Sensors", multiple: true, required: false, submitOnChange: true
            	if (rContact) {
                input "rContactCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
            	if (rContact?.size() > 1) {
                	input "rContactAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be $rContactCmd as a restriction.", required: false, defaultValue: false, submitOnChange: true
                    }
            	}
            }
		section ("Garage Door and Lock Conditions"){
            input "rLocks", "capability.lock", title: "Smart Locks", multiple: true, required: false, submitOnChange: true
            if (rLocks) {
                input "rLocksCmd", "enum", title: "are...", options:["locked":"locked", "unlocked":"unlocked"], multiple: false, required: true, submitOnChange:true
            }
            input "rGarage", "capability.garageDoorControl", title: "Garage Doors", multiple: true, required: false, submitOnChange: true
            if (rGarage) {
                input "rGarageCmd", "enum", title: "are...", options:["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true
        	}
        }
        section ("Environmental Conditions") {
        	input "rHumidity", "capability.relativeHumidityMeasurement", title: "Relative Humidity", required: false, submitOnChange: true
            	if (rHumidity) input "rHumidityLevel", "enum", title: "Only when the Humidity is...", options: ["above", "below"], required: false, submitOnChange: true            
            	if (rHumidityLevel) input "rHumidityPercent", "number", title: "this level...", required: true, description: "percent", submitOnChange: true            
                if (rHumidityPercent) input "rHumidityStop", "number", title: "...but not ${rHumidityLevel} this percentage", required: false, description: "humidity"
            input "rTemperature", "capability.temperatureMeasurement", title: "Temperature", required: false, multiple: true, submitOnChange: true
				if (rTemperature) input "rTemperatureLevel", "enum", title: "When the temperature is...", options: ["above", "below"], required: false, submitOnChange: true
                if (rTemperatureLevel) input "rTemperatureDegrees", "number", title: "Temperature...", required: true, description: "degrees", submitOnChange: true
                if (rTemperatureDegrees) input "rTemperatureStop", "number", title: "...but not ${rTemperatureLevel} this temperature", required: false, description: "degrees"
		}
    }
} 

/******************************************************************************************************
	TIME CONDITIONS 
******************************************************************************************************/
page name: "certainTime"
def certainTime() {
    dynamicPage(name:"certainTime",title: "", uninstall: false) {
        section("") {
            input "startingX", "enum", title: "Starting at...", options: ["A specific time", "Sunrise", "Sunset"], required: false , submitOnChange: true
            if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false, submitOnChange: true
            else {
                if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
        section("") {
            input "endingX", "enum", title: "Ending at...", options: ["A specific time", "Sunrise", "Sunset"], required: false, submitOnChange: true
            if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false, submitOnChange: true
            else {
                if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
    }
}

/******************************************************************************************************
	TIME RESTRICTIONS 
******************************************************************************************************/
page name: "certainTimeR"
def certainTimeR() {
    dynamicPage(name:"certainTimeR",title: "", uninstall: false) {
        section("") {
            input "startingXr", "enum", title: "Starting at...", options: ["A specific time", "Sunrise", "Sunset"], required: false , submitOnChange: true
            if(startingXr in [null, "A specific time"]) input "startingr", "time", title: "Start time", required: false, submitOnChange: true
            else {
                if(startingXr == "Sunrise") input "startSunriseOffsetr", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(startingXr == "Sunset") input "startSunsetOffsetr", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
        section("") {
            input "endingXr", "enum", title: "Ending at...", options: ["A specific time", "Sunrise", "Sunset"], required: false, submitOnChange: true
            if(endingXr in [null, "A specific time"]) input "endingr", "time", title: "End time", required: false, submitOnChange: true
            else {
                if(endingXr == "Sunrise") input "endSunriseOffsetr", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(endingXr == "Sunset") input "endSunsetOffsetr", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
    }
}
    
/************************************************************************************************************
	Base Process
************************************************************************************************************/
def installed() {
	if (parent.debug) log.debug "Installed with settings: ${settings}"
    state?.isInstalled = true
    initialize()
}

def updated() {
    if (parent.debug) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
	log.info "The parent pause toggle is $parent.aPause and this routines pause toggle is $rPause"
    if (rPause == true) {
    
    // Misc Variables
    state.tSelfHandlerEvt = null
    state.conditions = false
    state.restrictions = false

     	
}

}

/***********************************************************************************************************
   CONDITIONS HANDLER
************************************************************************************************************/
def conditionHandler(evt) {
    if (parent.debug) log.info "Activating Conditions Verification Process."
    def result
    def cSwitchOk = false
    def cDimOk = false
    def cHumOk = false
    def cTempOk = false
    def cSHMOk = false
    def cModeOk = false
    def cMotionOk = false
    def cPresenceOk = false
    def cDoorOk = false
    def cWindowOk = false
    def cContactOk = false
    def cDaysOk = false
    def cPendAll = false
    def timeOk = false
    def cGarageOk = false
    def cLocksOk = false
    def devList = []

	// COMMUNICATIONS PAUSED
    if (rPause == false || rPause == null) {
    	log.warn "The communications to the room, $app.label, have been paused by the user."
        state.speakers = false
        return
        }
    if (rPause == true) {

	// SWITCHES
    if (cSwitch == null) { cSwitchOk = false }
    if (cSwitch) {
    if (parent.trace) log.trace "Conditions: Switches events method activated"
        def cSwitchSize = cSwitch?.size()
        cSwitch.each { deviceName ->
            def status = deviceName.currentValue("switch")
            if (status == "${cSwitchCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cSwitchAll) {
            if (devList?.size() > 0) { 
                cSwitchOk = true
                if (parent.trace) log.trace "Conditions: Switches events method verified"
            }
        }        
        if(cSwitchAll) {
            if (devListSize == cSwitchSize) { 
                cSwitchOk = true
                if (parent.trace) log.trace "Conditions: Switches events method verified"
            }
        }
        if (cSwitchOk == false) if (parent.trace) log.warn "Switches Conditions Handler has Failed"
    }

    // HUMIDITY
    if (cHumidity == null) {cHumOk = false }
    if (cHumidity) {
    if (parent.trace) log.trace "Conditions: Humidity events method activated"
        int cHumidityStopVal = cHumidityStop == null ? 0 : cHumidityStop as int
            cHumidity.each { deviceName ->
                def status = deviceName.currentValue("humidity")
                if (cHumidityLevel == "above") {
                    cHumidityStopVal = cHumidityStopVal == 0 ? 999 :  cHumidityStopVal as int
                        if (status >= cHumidityPercent && status <= cHumidityStopVal) {
                            cHumOk = true
                            if (parent.trace) log.trace "Conditions: Humidity events method verified"
                        }
                }
                if (cHumidityLevel == "below") {
                    if (status <= cHumidityPercent && status >= cHumidityStopVal) {
                        cHumOk = true
                        if (parent.trace) log.trace "Conditions: Humidity events method verified"
                    }
                }    
            }
            if (cHumOk == false) if (parent.trace) log.warn "Humidity Conditions Handler has Failed"
    }

    // TEMPERATURE
    if (cTemperature == null) {cTempOk = false }
    if (cTemperature) {
    if (parent.trace) log.trace "Conditions: Temperature events method activated"
        int cTemperatureStopVal = cTemperatureStop == null ? 0 : cTemperatureStop as int
            cTemperature.each { deviceName ->
                def status = deviceName.currentValue("temperature")
                if (cTemperatureLevel == "above") {
                    cTemperatureStopVal = cTemperatureStopVal == 0 ? 999 :  cTemperatureStopVal as int
                        if (status >= cTemperatureDegrees && status <= cTemperatureStopVal) {
                            cTempOk = true
                            if (parent.trace) log.trace "Conditions: Temperature events method verified"
                        }
                }
                if (cTemperatureLevel == "below") {
                    if (status <= cTemperatureDegrees && status >= cTemperatureStopVal) {
                        cTempOk = true
                        if (parent.trace) log.trace "Conditions: Temperature events method verified"
                    }
                }    
            }
            if (cTempOk == false) if (parent.trace) log.warn "Temperature Conditions Handler has Failed"
    }	

    // DIMMERS
    if (cDim == null) { cDimOk = false }
    if (cDim) {
    if (parent.trace) log.trace "Conditions: Dimmers events method activated"
        cDim.each {deviceD ->
            def currLevel = deviceD.latestValue("level")
            if (cDimCmd == "greater") {
                if (currLevel > cDimLvl) { 
                    def cDimSize = cDim?.size()
                    cDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status > cDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                            }
                    }
                }        
            }
            if (cDimCmd == "lessThan") {
                if (currLevel < cDimLvl) { 
                    def cDimSize = cDim?.size()
                    cDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status < cDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                         }
                    }
                }        
            }
            if (cDimCmd == "equal") {
                if (currLevel == cDimLvl) { 
                    def cDimSize = cDim?.size()
                    cDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status == cDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                        }
                    }
                }        
            }
            def devListSize = devList?.size()
            if(!cDimAll) {
                if (devList?.size() > 0) { 
                    cDimOk = true 
                    if (parent.trace) log.trace "Conditions: Dimmers events method verified"
                }
            }        
            if(cDimAll) {
                if (devListSize == cDimSize) { 
                    cDimOk = true
                    if (parent.trace) log.trace "Conditions: Dimmers events method verified"
                }
            }
        }
        if (cDimOk == false) if (parent.trace) log.warn "Dimmers Conditions Handler has Failed"
    }

    // DAYS OF THE WEEK
    if (cDays == null) { cDaysOk = false }
    if (cDays) {
    	if (parent.trace) log.trace "Conditions: Days of the Week events method activated"
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = cDays.contains(day)
        cDaysOk = result
        if (parent.trace) log.trace "Conditions: Days of the Week events method verified"
        if (cDaysOk == false) if (parent.trace) log.warn "Days Conditions Handler failed"
        
    }

    // SMART HOME MONITOR
    if (cSHM == null) { cSHMOk = false }
    if (cSHM) {
    	if (parent.trace) log.trace "Conditions: SHM events method activated"
        def currentSHM = location.currentState("alarmSystemStatus")?.value
        if (cSHM == currentSHM) {
            cSHMOk = true
            if (parent.trace) log.trace "Conditions: SHM events method verified"
        }
        if (cSHMOk == false) if (parent.trace) log.warn "SHM Conditions Handler failed"
    }    

    // LOCATION MODE
    if (cMode == null) { cModeOk = false }
    if (cMode) {
    if (parent.trace) log.trace "Conditions: Mode events method activated"
        cModeOk = !cMode || cMode?.contains(location.mode)
    	if (cModeOk == false) { 
        	if (parent.trace) log.warn "Mode Conditions Handler failed"
            }
    }

    // MOTION
    if (cMotion == null) { cMotionOk = false }
    if (cMotion) {
    if (parent.trace) log.trace "Conditions: Motion events method activated"
        def cMotionSize = cMotion?.size()
        cMotion.each { deviceName ->
            def status = deviceName.currentValue("motion")
            if (status == "${cMotionCmd}"){ 
                String device  = (String) deviceName
                devList += device
             }   
        }
        def devListSize = devList?.size()
        if(!cMotionAll) {
            if (devList?.size() > 0) { 
                cMotionOk = true
                if (parent.trace) log.trace "Conditions: Motion events method verified"
            }
        }        
        if(cMotionAll) {
            if (devListSize == cMotionSize) { 
                cMotionOk = true 
                if (parent.trace) log.trace "Conditions: Motion events method verified"
            }
        }
        if (cMotionOk == false) if(parent.trace) log.warn "Motion Conditions Handler has Failed"
    }

    // PRESENCE
    if (cPresence == null) { cPresenceOk = false }
    if (cPresence) {
    if (parent.trace) log.trace "Conditions: Presence events method activated"
        def cPresenceSize = cPresence.size()
        cPresence.each { deviceName ->
            def status = deviceName.currentValue("presence")
            if (status == cPresenceCmd){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cPresenceAll) {
            if (devList?.size() > 0) { 
                cPresenceOk = true
                if (parent.trace) log.trace "Conditions: Presence events method verified"
            }
        }        
        if(cPresenceAll) {
            if (devListSize == cPresenceSize) { 
                cPresenceOk = true
                if (parent.trace) log.trace "Conditions: Presence events method verified"
            }
        }
        if (cPresenceOk == false) if (parent.trace) log.warn "Presence Conditions Handler has Failed"
    }

    // CONTACT SENSORS
    if (cContact == null) { cContactOk = false }
    if (cContact) {
    if (parent.trace) log.trace "Conditions: Contacts events method activated"
        def cContactSize = cContact?.size()
        cContact.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == "${cContactCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cContactAll) {
            if (devList?.size() > 0) { 
                cContactOk = true 
                if (parent.trace) log.trace "Conditions: Contacts events method verified"
            }
        }        
        if(cContactAll) {
            if (devListSize == cContactSize) { 
                cContactOk = true 
                if (parent.trace) log.trace "Conditions: Contacts events method verified"
            }
        }
        if (cContactOk == false) if (parent.trace) log.warn "Contacts Conditions Handler has Failed"
    }

    // DOOR CONTACT SENSORS
    if (cContactDoor == null) { cDoorOk = false }
    if (cContactDoor) {
    if (parent.trace) log.trace "Conditions: Door Contacts events method activated"
        def cContactDoorSize = cContactDoor?.size()
        cContactDoor.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == "${cContactDoorCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cContactDoorAll) {
            if (devList?.size() > 0) { 
                cDoorOk = true
                if (parent.trace) log.trace "Conditions: Door Contacts events method verified"
            }
        }        
        if(cContactDoorAll) {
            if (devListSize == cContactDoorSize) { 
                cDoorOk = true 
                if (parent.trace) log.trace "Conditions: Door Contacts events method verified"
            }
        }
        if (cDoorOk == false) if (parent.trace) log.warn "Door Contacts Conditions Handler has Failed"
    }

    // WINDOW CONTACT SENSORS
    if (cContactWindow == null) { cWindowOk = false }
    if (cContactWindow) {
    if (parent.trace) log.trace "Conditions: Window Contacts events method activated"
        def cContactWindowSize = cContactWindow?.size()
        cContactWindow.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == cContactWindowCmd){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cContactWindowAll) {
            if (devList?.size() > 0) { 
                cWindowOk = true
                if (parent.trace) log.trace "Conditions: Window contacts events method verified"
            }
        }        
        if(cContactWindowAll) {
            if (devListSize == cContactWindowSize) { 
                cWindowOk = true
                if (parent.trace) log.trace "Conditions: Window Contacts events method verified"
            }
        }
        if (cWindowOk == false) if (parent.trace) log.warn "Window Contacts Conditions Handler has Failed"
    }

    // GARAGE DOORS
    if (cGarage == null) { cGarageOk = false }
    if (cGarage) {
    if (parent.trace) log.trace "Conditions: Garage Doors events method activated"
        cGarage.each { deviceName ->
            def status = deviceName.currentValue("door")
            if (status == "${cGarageCmd}"){
            cGarageOk = true
            if (parent.trace) log.trace "Conditions: Garage Doors events method verified"
            }
            if (cGarageOk == false) if (parent.trace) log.warn "Garage Conditions Handler has Failed"
        }
    }    
    // LOCKS
    if (cLocks == null) { cLocksOk = false }
    if (cLocks) {
    if (parent.trace) log.trace "Conditions: Locks events method activated"
        cLocks.each { deviceName ->
            def status = deviceName.currentValue("lock")
            if (status == "${cLocksCmd}"){
            cLocksOk = true
            if (parent.trace) log.trace "Conditions: Locks events method verified"
            }
            if (cLocksOk == false) if (parent.trace) log.warn "Locks Conditions Handler has Failed"
        }
    }

	if (cLocksOk==true || cGarageOk==true || cTempOk==true || cHumOk==true || cSHMOk==true || cDimOk==true || cSwitchOk==true || cModeOk==true || 
    	cMotionOk==true || cPresenceOk==true || cDoorOk==true || cWindowOk==true || cContactOk==true || cDaysOk==true || getTimeOk(evt)==true) { 
        state.conditions = true
    	if (parent.debug) log.info "Conditions Handler --> All Conditions have been Verified: Process Complete"
        if (parent.trace) log.info "Conditions Handler Verified -->  \n" +
        "*************************************************************************** \n" +
        "**** CONDITIONS RESULTS:												\n" +
        "**** cLocksOk=$cLocksOk, cGarageOk=$cGarageOk, cTempOk=$cTempOk 		 \n" +
        "**** cHumOk=$cHumOk, SHM=$cSHMOk, cDim=$cDimOk, cSwitchOk=$cSwitchOk 	 \n" + 
        "**** cModeOk=$cModeOk, cMotionOk=$cMotionOk, cPresenceOk=$cPresenceOk 	 \n" +
        "**** cDoorOk=$cDoorOk,	cWindowOk=$cWindowOk, cContactOk=$cContactOk 	 \n" +
        "**** cDaysOk=$cDaysOk, getTimeOk=" + getTimeOk(evt) +					 "\n" +
        "***************************************************************************"
		}
        else if (cLocks==null && cGarage==null && cTemp==null && cHum==null && cSHM==null && cDim==null && cSwitch==null && cMode==null && 
    	cMotion==null && cPresence==null && cDoor==null && cWindow==null && cContact==null && cDays==null) { 
        state.conditions = true
        if (parent.debug) log.info "Conditions Handler --> All Conditions met due to no conditions present: Process Complete"
        
        } 
        else {
   		state.conditions = false
        if (parent.debug) log.info "Conditions Handler --> Failed Verification"
        if (parent.trace) log.warn "Conditions Handler Failed -->  \n" +
        "*************************************************************************** \n" +
        "**** CONDITIONS RESULTS:												\n" +
        "**** cLocksOk=$cLocksOk, cGarageOk=$cGarageOk, cTempOk=$cTempOk 		 \n" +
        "**** cHumOk=$cHumOk, SHM=$cSHMOk, cDim=$cDimOk, cSwitchOk=$cSwitchOk 	 \n" + 
        "**** cModeOk=$cModeOk, cMotionOk=$cMotionOk, cPresenceOk=$cPresenceOk 	 \n" +
        "**** cDoorOk=$cDoorOk,	cWindowOk=$cWindowOk, cContactOk=$cContactOk 	 \n" +
        "**** cDaysOk=$cDaysOk, getTimeOk=" + getTimeOk(evt) +					 "\n" +
        "***************************************************************************"
    	return
		}
    }    
}

/***********************************************************************************************************
   RESTRICTIONS HANDLER
************************************************************************************************************/
def restrictionsHandler(evt) {
    if (parent.debug) log.info "Activating Restrictions Verification Process."
    def result
    def rSwitchOk = false
    def rDimOk = false
    def rHumOk = false
    def rTempOk = false
    def rSHMOk = false
    def rModeOk = false
    def rMotionOk = false
    def rPresenceOk = false
    def rDoorOk = false
    def rWindowOk = false
    def rContactOk = false
    def rDaysOk = false
    def rPendAll = false
    def rtimeOk = false
    def rGarageOk = false
    def rLocksOk = false
    def devList = []

	// COMMUNICATIONS PAUSED
    if (rPause == false || rPause == null) {
    	log.warn "The communications to the zone, $app.label, have been paused by the user."
        state.speakers = false
        return
        }
    if (rPause == true) {

	// SWITCHES
    if (rSwitch == null) { rSwitchOk = false }
    if (rSwitch) {
    if (parent.trace) log.trace "Restrictions: Switches events method activated"
        def rSwitchSize = rSwitch?.size()
        rSwitch.each { deviceName ->
            def status = deviceName.currentValue("switch")
            if (status == "${rSwitchCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!rSwitchAll) {
            if (devList?.size() > 0) { 
                rSwitchOk = true  
            }
        }        
        if(rSwitchAll) {
            if (devListSize == rSwitchSize) { 
                rSwitchOk = true 
            }
        }
        if (rSwitchOk == false) log.warn "Switches Restrictions Handler failed"
    }

    // HUMIDITY
    if (rHumidity == null) {rHumOk = false }
    if (rHumidity) {
    log.trace "Restrictions: Humidity events method activated"
        int rHumidityStopVal = rHumidityStop == null ? 0 : rHumidityStop as int
            rHumidity.each { deviceName ->
                def status = deviceName.currentValue("humidity")
                if (rHumidityLevel == "above") {
                    rHumidityStopVal = rHumidityStopVal == 0 ? 999 :  rHumidityStopVal as int
                        if (status >= rHumidityPercent && status <= rHumidityStopVal) {
                            rHumOk = true
                        }
                }
                if (rHumidityLevel == "below") {
                    if (status <= rHumidityPercent && status >= rHumidityStopVal) {
                        rHumOk = true
                    }
                }    
            }
            if (rHumOk == false) log.warn "Humidity Restrictions Handler failed"
    }

    // TEMPERATURE
    if (rTemperature == null) {rTempOk = false }
    if (rTemperature) {
    log.trace "Restrictions: Temperature events method activated"
        int rTemperatureStopVal = rTemperatureStop == null ? 0 : rTemperatureStop as int
            rTemperature.each { deviceName ->
                def status = deviceName.currentValue("temperature")
                if (rTemperatureLevel == "above") {
                    rTemperatureStopVal = rTemperatureStopVal == 0 ? 999 :  rTemperatureStopVal as int
                        if (status >= rTemperatureDegrees && status <= rTemperatureStopVal) {
                            rTempOk = true
                        }
                }
                if (rTemperatureLevel == "below") {
                    if (status <= rTemperatureDegrees && status >= rTemperatureStopVal) {
                        rTempOk = true
                    }
                }    
            }
            if (rTempOk == false) log.warn "Temperature Restrictions Handler failed"
    }	

    // DIMMERS
    if (rDim == null) { rDimOk = false }
    if (rDim) {
    log.trace "Restrictions: Dimmers events method activated"
        rDim.each {deviceD ->
            def currLevel = deviceD.latestValue("level")
            if (rDimCmd == "greater") {
                if (currLevel > rDimLvl) { 
                    def rDimSize = rDim?.size()
                    rDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status > rDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                            }
                    }
                }        
            }
            if (rDimCmd == "lessThan") {
                if (currLevel < rDimLvl) { 
                    def rDimSize = rDim?.size()
                    rDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status < rDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                         }
                    }
                }        
            }
            if (rDimCmd == "equal") {
                if (currLevel == rDimLvl) { 
                    def rDimSize = rDim?.size()
                    rDim.each { deviceName ->
                        def status = deviceName.currentValue("level")
                        if (status == rDimLvl){ 
                            String device  = (String) deviceName
                            devList += device
                        }
                    }
                }        
            }
            def devListSize = devList?.size()
            if(!rDimAll) {
                if (devList?.size() > 0) { 
                    rDimOk = true  
                }
            }        
            if(rDimAll) {
                if (devListSize == rDimSize) { 
                    rDimOk = true 
                }
            }
        }
        if (rDimOk == false) log.warn "Dimmers Restrictions Handler failed"
    }

    // DAYS OF THE WEEK
    if (rDays == null) { rDaysOk = false }
    if (rDays) {
    	log.trace "Restrictions: Days of the Week events method activated"
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = rDays.contains(day)
        rDaysOk = result
        if (rDaysOk == false) log.warn "Days Restrictions Handler failed"
        
    }

    // SMART HOME MONITOR
    if (rSHM == null) { rSHMOk = false }
    if (rSHM) {
    	log.trace "Restrictions: SHM events method activated"
        def currentSHM = location.currentState("alarmSystemStatus")?.value
        if (rSHM == currentSHM) {
            rSHMOk = true
        }
        if (rSHMOk == false) log.warn "SHM Restrictions Handler failed"
    }    

    // LOCATION MODE
    if (rMode == null) { rModeOk = false }
    if (rMode) {
    log.trace "Restrictions: Mode events method activated"
        rModeOk = !rMode || rMode?.contains(location.mode)
    	if (rModeOk == false) log.warn "Mode Restrictions Handler failed"
    }

    // MOTION
    if (rMotion == null) { rMotionOk = false }
    if (rMotion) {
    log.trace "Restrictions: Motion events method activated"
        def rMotionSize = rMotion?.size()
        rMotion.each { deviceName ->
            def status = deviceName.currentValue("motion")
            if (status == "${rMotionCmd}"){ 
                String device  = (String) deviceName
                devList += device
             }   
        }
        def devListSize = devList?.size()
        if(!rMotionAll) {
            if (devList?.size() > 0) { 
                rMotionOk = true  
            }
        }        
        if(rMotionAll) {
            if (devListSize == rMotionSize) { 
                rMotionOk = true 
            }
        }
        if (rMotionOk == false) log.warn "Motion Restrictions Handler failed"
    }

    // PRESENCE
    if (rPresence == null) { rPresenceOk = false }
    if (rPresence) {
    log.trace "Restrictions: Presence events method activated"
        def rPresenceSize = rPresence.size()
        rPresence.each { deviceName ->
            def status = deviceName.currentValue("presence")
            if (status == rPresenceCmd){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!rPresenceAll) {
            if (devList?.size() > 0) { 
                rPresenceOk = true  
            }
        }        
        if(rPresenceAll) {
            if (devListSize == rPresenceSize) { 
                rPresenceOk = true 
            }
        }
        if (rPresenceOk == false) log.warn "Presence Restrictions Handler failed"
    }

    // CONTACT SENSORS
    if (rContact == null) { rContactOk = false }
    if (rContact) {
    log.trace "Restrictions: Contacts events method activated"
        def rContactSize = rContact?.size()
        rContact.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == "${rContactCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!rContactAll) {
            if (devList?.size() > 0) { 
                rContactOk = true  
            }
        }        
        if(rContactAll) {
            if (devListSize == rContactSize) { 
                rContactOk = true 
            }
        }
        if (rContactOk == false) log.warn "Contacts Restrictions Handler failed"
    }

    // DOOR CONTACT SENSORS
    if (rContactDoor == null) { rDoorOk = false }
    if (rContactDoor) {
    log.trace "Restrictions: Door Contacts events method activated"
        def rContactDoorSize = rContactDoor?.size()
        rContactDoor.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == "${rContactDoorCmd}"){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!rContactDoorAll) {
            if (devList?.size() > 0) { 
                rDoorOk = true  
            }
        }        
        if(rContactDoorAll) {
            if (devListSize == rContactDoorSize) { 
                rDoorOk = true 
            }
        }
        if (rDoorOk == false) log.warn "Door Contacts Restrictions Handler failed"
    }

    // WINDOW CONTACT SENSORS
    if (rContactWindow == null) { rWindowOk = false }
    if (rContactWindow) {
    log.trace "Restrictions: Window Contacts events method activated"
        def rContactWindowSize = rContactWindow?.size()
        rContactWindow.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == rContactWindowCmd){ 
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!rContactWindowAll) {
            if (devList?.size() > 0) { 
                rWindowOk = true  
            }
        }        
        if(rContactWindowAll) {
            if (devListSize == rContactWindowSize) { 
                rWindowOk = true 
            }
        }
        if (rWindowOk == false) log.warn "Window Contacts Restrictions Handler failed"
    }

    // GARAGE DOORS
    if (rGarage == null) { rGarageOk = false }
    if (rGarage) {
    log.trace "Restrictions: Garage Doors events method activated"
        rGarage.each { deviceName ->
            def status = deviceName.currentValue("door")
            if (status == "${rGarageCmd}"){
            rGarageOk = true
            }
            if (rGarageOk == false) log.warn "Garage Restrictions Handler failed"
        }
    }    
    // LOCKS
    if (rLocks == null) { rLocksOk = false }
    if (rLocks) {
    log.trace "Restrictions: Locks events method activated"
        cLocks.each { deviceName ->
            def status = deviceName.currentValue("lock")
            if (status == "${rLocksCmd}"){
            rLocksOk = true
            }
            if (rLocksOk == false) log.warn "Locks Restrictions Handler failed"
        }
    }

	if (rLocksOk==true || rGarageOk==true || rTempOk==true || rHumOk==true || rSHMOk==true || rDimOk==true || rSwitchOk==true || rModeOk==false || 
    	rMotionOk==true || rPresenceOk==true || rDoorOk==true || rWindowOk==true || rContactOk==true || rDaysOk==true) { // || getTimeROk(evt)==true) { 
        state.restrictions = true
    	if (parent.debug) log.info "Restrictions Handler --> All Restrictions have been Verified: Process Complete"
        if (parent.trace) log.info "Restrictions Handler Verified -->  \n" +
        "*************************************************************************** \n" +
        "**** RESTRICTIONS RESULTS:												\n" +
        "**** rLocksOk=$rLocksOk, rGarageOk=$rGarageOk, rTempOk=$rTempOk 		 \n" +
        "**** rHumOk=$rHumOk, rSHM=$rSHMOk, rDim=$rDimOk, rSwitchOk=$rSwitchOk 	 \n" + 
        "**** rModeOk=$rModeOk, rMotionOk=$rMotionOk, rPresenceOk=$rPresenceOk 	 \n" +
        "**** rDoorOk=$rDoorOk,	rWindowOk=$rWindowOk, rContactOk=$rContactOk 	 \n" +
        "**** rDaysOk=$rDaysOk, getTimeROk= 					 \n" + //+ getTimeROk(evt) +
        "***************************************************************************"
		}
        else if (rLocks==null && rGarage==null && rTemp==null && rHum==null && rSHM==null && rDim==null && rSwitch==null && rMode==null && 
    	rMotion==null && rPresence==null && rDoor==null && rWindow==null && rContact==null && rDays==null) { 
        state.restrictions = true
        if (parent.debug) log.info "Restrictions Handler --> All Restrictions met due to no conditions present: Process Complete"
        
        } 
        else {
   		state.restrictions = false
        if (parent.trace) log.warn "Restrictions Handler Failed -->  \n" +
        "*************************************************************************** \n" +
        "**** RESTRICTIONS RESULTS:												\n" +
        "**** rLocksOk=$rLocksOk, rGarageOk=$rGarageOk, rTempOk=$rTempOk 		 \n" +
        "**** rHumOk=$rHumOk, rSHM=$rSHMOk, rDim=$rDimOk, rSwitchOk=$rSwitchOk 	 \n" + 
        "**** rModeOk=$rModeOk, rMotionOk=$rMotionOk, rPresenceOk=$rPresenceOk 	 \n" +
        "**** rDoorOk=$rDoorOk,	rWindowOk=$rWindowOk, rContactOk=$rContactOk 	 \n" +
        "**** rDaysOk=$rDaysOk, getTimeROk= 					 \n" + //+ getTimeROk(evt) +
        "***************************************************************************"
    	return
		}
    }    
}

/***********************************************************************************************************************
    SUN STATE HANDLER CONDITIONS
***********************************************************************************************************************/
def sunsetTimeHandler(evt) {
if (parent.debug) log.info "Sunset Handler activated"
    def sunsetString = (String) evt.value
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
    def sunsetTime = s.sunset.time 
    if(offset) {
        def offsetSunset = new Date(sunsetTime - (-offset * 60 * 1000))
        if (parent.trace) log.debug "Scheduling for: $offsetSunset (sunset is $sunsetTime)"
        runOnce(offsetSunset, "ttsActions")
    }
    else ttsActions("sunset")
}
def sunriseTimeHandler(evt) {
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
    def sunriseTime = s.sunrise.time
    if(offset) {
        def offsetSunrise = new Date(sunriseTime -(-offset * 60 * 1000))
        if (parent.trace) log.debug "Scheduling for: $offsetSunrise (sunrise is $sunriseTime)"
        runOnce(offsetSunrise, "ttsActions")
    }
    else  ttsActions("sunrise")
}

def scheduledTimeHandler(state) {
    def data = [:]
    data = [value: "executed", name:"timer", device:"schedule"] 
    ttsActions(data)
}

/***********************************************************************************************************************
    SUN STATE HANDLER RESTRICTIONS
***********************************************************************************************************************/
def sunsetTimeHandlerr(evt) {
if (parent.debug) log.info "Sunset Restriction Handler activated"
    def sunsetString = (String) evt.value
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
    def sunsetTime = s.sunset.time 
    if(offset) {
        def offsetSunset = new Date(sunsetTime - (-offset * 60 * 1000))
        if (parent.trace) log.debug "Scheduling for: $offsetSunset (sunset is $sunsetTime)"
        runOnce(offsetSunset, "ttsActions")
    }
    else ttsActions("sunset")
}
def sunriseTimeHandlerr(evt) {
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
    def sunriseTime = s.sunrise.time
    if(offset) {
        def offsetSunrise = new Date(sunriseTime -(-offset * 60 * 1000))
        if (parent.trace) log.debug "Scheduling for: $offsetSunrise (sunrise is $sunriseTime)"
        runOnce(offsetSunrise, "ttsActions")
    }
    else  ttsActions("sunrise")
}

def scheduledTimeHandlerr(state) {
    def data = [:]
    data = [value: "executed", name:"timer", device:"schedule"] 
    ttsActions(data)
}

/******************************************************************************************************
SPEECH AND TEXT ACTION
******************************************************************************************************/
def ttsActions(evt) {
	log.info "state.speakers = $state.speakers"
	if(rPause == true) {
    log.info "TTS Actions Handler activated with this message --> $evt.descriptionText"
    conditionHandler(text)
    restrictionsHandler(text)
    if (parent.trace) log.trace "ConditionHandler = $state.conditions && RestrictionsHandler = $state.restrictions"
    if (state.conditions == true && state.restrictions == true) {
        log.info "tts = $evt.descriptionText"
        def tts = evt.descriptionText
            if (echoDevice) {
				echoDevice?.each { spk->
     					spk.setVolumeAndSpeak(eVolume, tts)  //spk?.speak(tts)
					}            	
            	}
            if (synthDevice) {
                synthDevice?.speak(tts) 
            }
            if (sonosDevice){ // 2/22/2017 updated Sono handling when speaker is muted
                def currVolLevel = sonosDevice.latestValue("level")
                def currMuteOn = sonosDevice.latestValue("mute").contains("muted")
                if (parent.debug) log.debug "currVolSwitchOff = ${currVolSwitchOff}, vol level = ${currVolLevel}, currMuteOn = ${currMuteOn} "
                if (currMuteOn) { 
                    if (parent.debug) log.warn "speaker is on mute, sending unmute command"
                    sonosDevice.unmute()
                }
                def sVolume = settings.volume ?: 20
                sonosDevice?.playTextAndResume("Attention " + tts, sVolume)
                if (parent.debug) log.info "Playing message on the music player '${sonosDevice}' at volume '${volume}'" 
            }
        if(recipients || sms){				
            sendtxt(tts)
        }
        if (push) {
            sendPushMessage(tts)
        }	
        state.lastMessage = tts
        return
    }
    state.speakers = false
	}
}    




/***********************************************************************************************************************
	SMS HANDLER
***********************************************************************************************************************/
private void sendtxt(tts) {
    if (parent.debug) log.info "Send Text method activated."
    if (sendContactText) { 
        sendNotificationToContacts(tts, recipients)
        if (push || shmNotification) { 
            sendPushMessage
        }
    } 
    if (notify) {
        sendNotificationEvent(tts)
    }
    if (sms) {
        sendText(sms, tts)
    }
    if (psms) {
        processpsms(psms, tts)
    }
}

private void sendText(number, tts) {
    if (sms) {
        def phones = sms.split("\\,")
        for (phone in phones) {
            sendSms(phone, tts)
        }
    }
}
private void processpsms(psms, tts) {
    if (psms) {
        def phones = psms.split("\\,")
        for (phone in phones) {
            sendSms(phone, tts)
        }
    }
}

/***********************************************************************************************************************
	TIME CONDITIONS 
***********************************************************************************************************************/
private getTimeOk(evt) {
    def result = false
    if (parent.trace) log.trace "getTimeOk Conditions event started: starting = $starting && ending = $ending"
    if (starting == null && ending == null) {
    	result = false
        }
    if ((starting && ending) ||
        (starting && endingX in ["Sunrise", "Sunset"]) ||
        (startingX in ["Sunrise", "Sunset"] && ending) ||
        (startingX in ["Sunrise", "Sunset"] && endingX in ["Sunrise", "Sunset"])) {
        def currTime = now()
        def start = null
        def stop = null
        def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
        if(startingX == "Sunrise") start = s.sunrise.time
        else if(startingX == "Sunset") start = s.sunset.time
            else if(starting) start = timeToday(starting,location.timeZone).time
                s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
            if(endingX == "Sunrise") stop = s.sunrise.time
            else if(endingX == "Sunset") stop = s.sunset.time
                else if(ending) stop = timeToday(ending,location.timeZone).time
                    result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
            }
    if(parent.trace) log.trace "timeOk = $result"
    return result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}
private offset(value) {
    def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}
private timeIntervalLabel() {
    def result = "complete"
    if      (startingX == "Sunrise" && endingX == "Sunrise") result = "Sunrise" + offset(startSunriseOffset) + " to Sunrise" + offset(endSunriseOffset)
    else if (startingX == "Sunrise" && endingX == "Sunset") result = "Sunrise" + offset(startSunriseOffset) + " to Sunset" + offset(endSunsetOffset)
        else if (startingX == "Sunset" && endingX == "Sunrise") result = "Sunset" + offset(startSunsetOffset) + " to Sunrise" + offset(endSunriseOffset)
            else if (startingX == "Sunset" && endingX == "Sunset") result = "Sunset" + offset(startSunsetOffset) + " to Sunset" + offset(endSunsetOffset)
                else if (startingX == "Sunrise" && ending) result = "Sunrise" + offset(startSunriseOffset) + " to " + hhmm(ending, "h:mm a z")
                    else if (startingX == "Sunset" && ending) result = "Sunset" + offset(startSunsetOffset) + " to " + hhmm(ending, "h:mm a z")
                        else if (starting && endingX == "Sunrise") result = hhmm(starting) + " to Sunrise" + offset(endSunriseOffset)
                            else if (starting && endingX == "Sunset") result = hhmm(starting) + " to Sunset" + offset(endSunsetOffset)
                                else if (starting && ending) result = hhmm(starting) + " to " + hhmm(ending, "h:mm a z")
                                    }

/***********************************************************************************************************************
	TIME RESTRICTIONS 
***********************************************************************************************************************/
private getTimeROk(evt) {
    def result = false
    if (parent.trace) log.trace "getTimeROk Restrictions event started: starting = $starting && ending = $ending"
    if (startingr == null && endingr == null) {
    	result = true
        }
    if ((startingr && endingr) ||
        (startingr && endingXr in ["Sunrise", "Sunset"]) ||
        (startingXr in ["Sunrise", "Sunset"] && endingr) ||
        (startingXr in ["Sunrise", "Sunset"] && endingXr in ["Sunrise", "Sunset"])) {
        def currTime = now()
        def start = null
        def stop = null
        def s = getSunriseAndSunsetr(zipCode: zipCode, sunriseOffsetr: startSunriseOffsetr, sunsetOffsetr: startSunsetOffsetr)
        if(startingXr == "Sunrise") start = s.sunrise.time
        else if(startingXr == "Sunset") start = s.sunset.time
            else if(startingr) start = timeToday(startingr,location.timeZone).time
                s = getSunriseAndSunsetr(zipCode: zipCode, sunriseOffsetr: endSunriseOffsetr, sunsetOffsetr: endSunsetOffsetr)
            if(endingXr == "Sunrise") stop = s.sunrise.time
            else if(endingXr == "Sunset") stop = s.sunset.time
                else if(endingr) stop = timeToday(ending,location.timeZone).time
                    result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
            }
    if(parent.trace) log.trace "timeROk = $result"
    return result
}

private hhmmr(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}
private offsetr(value) {
    def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}
private timeIntervalLabelr() {
    def result = "complete"
    if      (startingXr == "Sunrise" && endingXr == "Sunrise") result = "Sunrise" + offsetr(startSunriseOffsetr) + " to Sunrise" + offsetr(endSunriseOffsetr)
    else if (startingXr == "Sunrise" && endingXr == "Sunset") result = "Sunrise" + offsetr(startSunriseOffsetr) + " to Sunset" + offsetr(endSunsetOffsetr)
        else if (startingXr == "Sunset" && endingXr == "Sunrise") result = "Sunset" + offsetr(startSunsetOffsetr) + " to Sunrise" + offsetr(endSunriseOffsetr)
            else if (startingXr == "Sunset" && endingXr == "Sunset") result = "Sunset" + offsetr(startSunsetOffsetr) + " to Sunset" + offsetr(endSunsetOffsetr)
                else if (startingXr == "Sunrise" && endingr) result = "Sunrise" + offsetr(startSunriseOffsetr) + " to " + hhmmr(endinrg, "h:mm a z")
                    else if (startingXr == "Sunset" && endingr) result = "Sunset" + offsetr(startSunsetOffsetr) + " to " + hhmmr(endingr, "h:mm a z")
                        else if (startingr && endingXr == "Sunrise") result = hhmmr(startingr) + " to Sunrise" + offsetr(endSunriseOffsetr)
                            else if (startingr && endingXr == "Sunset") result = hhmmr(startingr) + " to Sunset" + offsetr(endSunsetOffsetr)
                                else if (startingr && endingr) result = hhmmr(startingr) + " to " + hhmmr(endingr, "h:mm a z")
                                    }

/******************************************************************************************************
PARENT STATUS CHECKS
******************************************************************************************************/
// DEVICES
def pDevicesSettings() {
    if (synthDevice||echoDevice||sonosDevice||sendText||push) {
        return "complete"
    }
    return ""
}
def pDevicesComplete() {
    if (synthDevice||echoDevice||sonosDevice||sendText||push) {
        return "Configured!"
    }
    return "Tap here to Select Devices"
}  
// CONDITIONS
def pConditionSettings() {
    if (cTemperature||cHumidity||cDays||cMode||cSHM||cSwitch||cDim||cMotion||cPresence||cContactDoor||cContactWindow||cContact||cLocks||
    	cGarage||startingX||endingX) {
        return "complete"
    }
    return ""
}
def pConditionComplete() {
    if (cTemperature||cHumidity||cDays||cMode||cSHM||cSwitch||cDim||cMotion||cPresence||cContactDoor||cContactWindow||cContact||cLocks||
    	cGarage||startingX||endingX) {
        return "Configured!"
    }
    return "Tap here to Configure Conditions"
}  
def pTimeSettings(){ def result = "" 
                    if (startingX || endingX) { 
                        result = "complete"}
                    result}
def pTimeComplete() {def text = "Tap here to Configure" 
                     if (startingX || endingX) {
                         text = "Configured"}
                     else text = "Tap here to Configure"
                     text}
// RESTRICTIONS
def pRestrictionsSettings() {
    if (rTemperature||rHumidity||rDays||rMode||rSHM||rSwitch||rDim||rMotion||rPresence||rContactDoor||rContactWindow||rContact||rLocks||
    	rGarage||startingXr||endingXr) {
        return "complete"
    }
    return ""
}
def pRestrictionsComplete() {
    if (rTemperature||rHumidity||rDays||rMode||rSHM||rSwitch||rDim||rMotion||rPresence||rContactDoor||rContactWindow||rContact||rLocks||
    	rGarage||startingXr||endingXr) {
        return "Configured!"
    }
    return "Tap here to Configure Restrictions"
}  
def rTimeSettings(){ def result = "" 
                    if (startingXr || endingXr) { 
                        result = "complete"}
                    result}
def rTimeComplete() {def text = "Tap here to Configure" 
                     if (startingXr || endingXr) {
                         text = "Configured"}
                     else text = "Tap here to Configure"
                     text}


////////////////////////////////////////////////////////////////////////////////////////////////
// DEVELOPMENT SECTION - WORK HERE AND MOVE IT WHEN YOU'RE DONE
////////////////////////////////////////////////////////////////////////////////////////////////