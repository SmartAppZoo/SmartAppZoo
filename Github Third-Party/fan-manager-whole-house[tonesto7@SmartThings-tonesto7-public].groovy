/**
 *  Fan Manager Whole House Fan Controller
 *
 *  Copyright 2018 bamarayne
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
 *
 *	09/26/2018		Version:1.0 R.0.0.1		Alpha
 */
import java.text.SimpleDateFormat

public String appVer() { return "1.1.0" }
public String appDate() { return "09/27/2018" }

definition(
    name: "Fan Manager Whole House",
    namespace: "tonesto7",
    author: "bamarayne",
    description: "Quality Control of your homes ventilation system",
    category: "Convenience",
    parent: "tonesto7:Fan Manager",
    iconUrl: appImg(),
    iconX2Url: appImg(),
    iconX3Url: appImg(),
    pausable: true)


preferences {
	page name: "mainPage"
    page name: "condPage"
    page name: "actionsPage"
    page name: "settingsPage"
    page name: "onPage"
    page name: "offPage"
    page name: "condFailPage"
    page name: "certainTime"
}

/******************************************************************************
	MAIN PAGE
******************************************************************************/
def mainPage() {
    dynamicPage(name: "mainPage", title:"", install: true, uninstall: false) {
        section ("Primary Fan") {
            input "priFan", "capability.switch", title: "Select your Whole House Fan", multiple: false, required: false, submitOnChange: true
        }
        if (settings?.priFan) {
            section ("Conditions") {
                href "condPage", title: "Verify these Conditions have been met (only when fan is turned on)", description: condPageComplete(), state: condPageSettings()
            }
            section ("Actions") {
                href "actionsPage", title: "Perform these actions when all conditions are met", description: actionsPageComplete(), state: actionsPageSettings()
            }
        }
        section ("Settings") {
            href "settingsPage", title: "App and Safety\nSettings", description: settingsPageComplete(), state: settingsPageSettings()
        }
        if (settings?.gasSafetyCheck) {
            section ("Disclaimer") {
                paragraph title: "NOTICE:", "You have indicated that you have gas appliances in your home. \n" +
                    "Most gas appliances in your home have a pilot light and they exhuast through a plume " +
                    "out of your home. If there is not adequate ventilation for your whole house fan, it can " +
                    "potentially cause an exhaust backflow situation with those appliances. Because of this, " +
                    "additional safety measures has been added to prevent your whole " +
                    "house fan from running unless there is at least ONE Open door or window in your home. "
            }
        }
    }
}

/******************************************************************************
	ACTIONS PAGE
******************************************************************************/
def actionsPage() {
    dynamicPage(name: "actionsPage", title: "Configure Actions",install: false, uninstall: false) {
        section ("Actions when Fan turns On") {
            href "onPage", title: "Perform these actions when fan is turned on", description: actionsOnPageComplete(), state: actionsOnPageSettings()
        }
        section ("Actions when Fan turns Off") {
            href "offPage", title: "Perform these actions when fan is turned off", description: actionsOffPageComplete(), state: actionsOffPageSettings()
        }
    }
}

/******************************************************************************
	SETTINGS PAGE
******************************************************************************/
def settingsPage() {
    dynamicPage(name: "settingsPage", title: "Configure App Settings",install: true, uninstall: true, nextPage: "mainPage") {
        section ("App Settings") {
            input "logs", "bool", title: "Show logs in the IDE Live Logging", defaultValue: false, submitOnChange: true
        }
        section ("Safety") {
            input "gasSafetyCheck", "bool", title: "Do you have a gas furnace, water heater, or other pilot flame device?", defaultValue: false, submitOnChange: true, image: getAppImg("safety_check.png")
        }
        section ("Conditions Failure Actions") {
            href "condFailPage", title: "Perform these actions when the conditions Fail", description: condFailPageComplete(), state: condFailPageSettings()
        }
    }
}

/******************************************************************************
	CONDITIONS CONFIGURATION PAGE
******************************************************************************/
def condPage() {
    dynamicPage(name: "condPage", title: "Execute this routine when...",install: false, uninstall: false) {
        section ("Location Settings Conditions") {
            input "cMode", "mode", title: "Location Mode is...", multiple: true, required: false, submitOnChange: true, image: getAppImg("mode.png")
            input "cDays", title: "Days of the week", multiple: true, required: false, submitOnChange: true,
                "enum", options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], image: getAppImg("day_calendar2.png")
            href "certainTime", title: "Time Schedule", description: pTimeComplete(), state: pTimeSettings()
        }
        section ("Switches") {
            input "cSwitch", "capability.switch", title: "Switches", multiple: true, submitOnChange: true, required:false, image: getAppImg("switch.png")
            if (settings?.cSwitch) {
                input "cSwitchCmd", "enum", title: "are...", options:["on":"On","off":"Off"], multiple: false, required: true, submitOnChange: true, image: "blank.png"
                if (settings?.cSwitch?.size() > 1) {
                    input "cSwitchAll", "bool", title: "Activate this toggle if you want ALL of the switches to be ${settings?.cSwitchCmd} as a condition.", required: false, defaultValue: false, submitOnChange: true, image: "blank.png"
                }
            }
        }
        section ("Presence Conditions") {
            input "cPresence", "capability.presenceSensor", title: "Presence Sensors", multiple: true, required: false, submitOnChange: true, image: getAppImg("recipient.png")
            if (settings?.cPresence) {
                input "cPresenceCmd", "enum", title: "are...", options: ["present":"Present","not present":"Not Present"], multiple: false, required: true, submitOnChange: true, image: "blank.png"
                if (settings?.cPresence?.size() > 1) {
                    input "cPresenceAll", "bool", title: "Activate this toggle if you want ALL of the Presence Sensors to be ${settings?.cPresenceCmd} as a condition.", required: false, defaultValue: false, submitOnChange: true, image: "blank.png"
                }
            }
        }
        section ("Doors") {
            input "cContactDoor", "capability.contactSensor", title: "Contact Sensors only on Doors", multiple: true, required: false, submitOnChange: true, image: getAppImg("door_close.png")
            if (settings?.cContactDoor) {
                input "cContactDoorCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true, image: "blank.png"
                if (settings?.cContactDoor?.size() > 1) {
                    input "cContactDoorAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be ${settings?.cContactDoorCmd} as a condition.", required: false, defaultValue: false, submitOnChange: true, image: "blank.png"
                }
            }
        }
        section ("Windows") {
            input "cContactWindow", "capability.contactSensor", title: "Contact Sensors only on Windows", multiple: true, required: false, submitOnChange: true, image: getAppImg("window.png")
            if (settings?.cContactWindow) {
                input "cContactWindowCmd", "enum", title: "that are...", options: ["open":"open", "closed":"closed"], multiple: false, required: true, submitOnChange: true, image: "blank.png"
                if (settings?.cContactWindow?.size() > 1) {
                    input "cContactWindowAll", "bool", title: "Activate this toggle if you want ALL of the Doors to be ${settings?.cContactWindowCmd} as a condition.", required: false, defaultValue: false, submitOnChange: true, image: "blank.png"
                    input "cContactWindowMin", "number", title: "Minimum number of windows that must be open?", required: false, defaultValue: 1, submitOnChange: true, image: "blank.png"
                }
            }
        }
        section ("Environmental Conditions") {
            input "cHumidity", "capability.relativeHumidityMeasurement", title: "Relative Humidity", required: false, submitOnChange: true, image: getAppImg("humidity.png")
            if (settings?.cHumidity) { input "cHumidityLevel", "enum", title: "Only when the Humidity is...", options: ["above", "below"], required: false, submitOnChange: true, image: "blank.png" }
            if (settings?.cHumidityLevel) { input "cHumidityPercent", "number", title: "this level...", required: true, description: "percent", submitOnChange: true, image: "blank.png" }
            if (settings?.cHumidityPercent) { input "cHumidityStop", "number", title: "...but not ${settings?.cHumidityLevel} this percentage", required: false, description: "humidity", image: "blank.png" }

            input "cTemperature", "capability.temperatureMeasurement", title: "Temperature", required: false, multiple: true, submitOnChange: true, image: getAppImg("temp.png")
            if (settings?.cTemperature) { input "cTemperatureLevel", "enum", title: "When the temperature is...", options: ["above", "below"], required: false, submitOnChange: true, image: "blank.png" }
            if (settings?.cTemperatureLevel) { input "cTemperatureDegrees", "number", title: "Temperature...", required: true, description: "degrees", submitOnChange: true, image: "blank.png" }
            if (settings?.cTemperatureDegrees) { input "cTemperatureStop", "number", title: "...but not ${settings?.cTemperatureLevel} this temperature", required: false, description: "degrees", image: "blank.png" }
        }
    }
}


/***********************************************************************************************************
   CONDITIONS FAIL PAGE
************************************************************************************************************/
def condFailPage() {
    dynamicPage(name: "condFailPage", title: "Perform these actions when conditions have not been met.", install: false, uninstall: false) {
        section ("Conditions Fail Alert Message") {
            input "failMsg", "text", title: "Send this message when the conditions have not been met", required: false, submitOnChange: true
        }
        section ("Send Conditions Failed Message to") {
            input "synthDevice", "capability.speechSynthesis", title: "Speech Synthesis Devices", multiple: true, required: false
            input "echoDevice", "capability.notification", title: "Amazon Alexa Devices", multiple: true, required: false
            input "sonosDevice", "capability.musicPlayer", title: "Music Player Devices", required: false, multiple: true, submitOnChange: true
            if (sonosDevice) {
                input "volume", "number", title: "Temporarily change volume", description: "0-100% (default value = 30%)", required: false
            }
            input "sendText", "bool", title: "Enable Text Notifications", required: false, submitOnChange: true
            if (sendText){
                paragraph "You may enter multiple phone numbers separated by comma to deliver the Alexa message. E.g. +18045551122,+18046663344"
                input name: "sms", title: "Send text notification to (optional):", type: "phone", required: false
            }
            input "push", "bool", title: "Send Push Notification (optional)", required: false, defaultValue: false
        }
    }
}

/***********************************************************************************************************
   ACTIONS ON PAGE
************************************************************************************************************/
def onPage() {
    dynamicPage(name: "onPage", title: "Perform these actions when conditions have not been met.", install: false, uninstall: false) {
        List actOptions = ["Switches","Dimmers","Fans & Ceiling Fans","Thermostats, Vents, & Shades"]
        section ("Select Actions Capabilities") {
            input "actions", "enum", title: "Select Actions Capabilities", options: actOptions, multiple: true, required: true, submitOnChange: true, image: getAppImg("trigger.png")
        }
        if (actions?.contains("Switches")) {
            section("Simple On/Off/Toggle Switches", hideable: true, hidden: false) {
                input "aOtherSwitches", "capability.switch", title: "On/Off/Toggle Lights & Switches", multiple: true, required: false, submitOnChange: true
                if (aOtherSwitches) {
                    input "aOtherSwitchesCmd", "enum", title: "...will turn...", options: ["on":"on","off":"off","toggle":"toggle"], multiple: false, required: false, submitOnChange: true
                }
            }
            if (aOtherSwitchesCmd != null) {
                section("More Simple On/Off/Toggle Switches", hideable: true, hidden: false) {
                    input "aOtherSwitches2", "capability.switch", title: "On/Off/Toggle Lights & Switches", multiple: true, required: false, submitOnChange: true
                    if (aOtherSwitches2) {
                        input "aOtherSwitchesCmd2", "enum", title: "...will turn...", options: ["on":"on","off":"off","toggle":"toggle"], multiple: false, required: false, submitOnChange: true
                    }
                }
            }
        }
        if (actions?.contains("Dimmers")) {
            section ("Dimmers - Selection", hideable: true, hidden: false) {
                input "aDim", "capability.switchLevel", title: "Dimmable Lights and Switches", multiple: true, required: false , submitOnChange:true
                if (aDim) {
                    input "aDimCmd", "enum", title: "...will...", options:["on":"turn on","off":"turn off","set":"set the level","decrease":"decrease","increase":"increase"], multiple: false, required: false, submitOnChange: true
                    if (aDimCmd=="decrease") {
                        input "aDimDecrease", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aDimCmd == "increase") {
                        input "aDimIncrease", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aDimCmd == "set") {
                        input "aDimLVL", "number", title: "...of the lights to...", description: "this percentage", range: "0..100", required: false, submitOnChange: true
                    }
                    input "aDimDelay", "number", title: "Delay this action by this many seconds.", required: false, defaultValue: 0, submitOnChange: true
                }
            }
            section("More Dimmers - Selection", hideable: true, hidden: false) {
                input "aOtherDim", "capability.switchLevel", title: "More Dimmers", multiple: true, required: false , submitOnChange:true
                if (aOtherDim) {
                    input "aOtherDimCmd", "enum", title: "...will...", options:["on":"turn on","off":"turn off","set":"set the level","decrease":"decrease","increase":"brighten"], multiple: false, required: false, submitOnChange:true
                    if (aOtherDimCmd=="decrease") {
                        input "aOtherDimDecrease", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aOtherDimCmd == "increase") {
                        input "aOtherDimIncrease", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aOtherDimCmd == "set") {
                        input "aOtherDimLVL", "number", title: "...of the lights to...", description: "this percentage", range: "0..100", required: false, submitOnChange: true
                    }
                    input "otherDimDelay", "number", title: "Delay this action by this many seconds.", required: false, defaultValue: 0, submitOnChange: true
                }
            }
        }
        if (actions?.contains("Fans & Ceiling Fans")) {
            section ("Fans connected to switches", hideable: true, hidden: false) {
                input "aFans", "capability.switch", title: "These Fans...", multiple: true, required: false, submitOnChange: true, image: "https://raw.githubusercontent.com/bamarayne/master/LogicRulz/icons/fan.png"
                if (aFans) {
                    input "aFansCmd", "enum", title: "...will...", options:["on":"turn on","off":"turn off"], multiple: false, required: false, submitOnChange:true
                    if (aFansCmd=="on") {
                        input "aFansDelayOn", "number", title: "Delay turning on by this many seconds", defaultValue: 0, submitOnChange:true
                        if (aFansDelayOn) input "aFansPendOn", "bool", title: "Activate for pending state change cancellation", required: false, default: false
                    }
                    if (aFansCmd=="off") {
                        input "aFansDelayOff", "number", title: "Delay turning off by this many seconds", defaultValue: 0, submitOnChange:true
                        if (aFansDelayOff) input "aFansPendOff", "bool", title: "Activate for pending state change cancellation", required: false, default: false
                    }
                }
            }
            section ("Fans and Ceiling Fan Settings (adjustable)", hideable: true, hidden: false) {
                input "aCeilingFans", "capability.switchLevel", title: "These ceiling fans...", multiple: true, required: false, submitOnChange: true
                if (aCeilingFans) {
                    input "aCeilingFansCmd", "enum", title: "...will...", options:["on":"turn on","off":"turn off","low":"set to low","med":"set to med","high":"set to high","incr":"speed up","decr":"slow down"], multiple: false, required: false, submitOnChange:true
                    if (aCeilingFansCmd == "incr") {
                        input "aCeilingFansIncr", "number", title: "...by this percentage", required: true, submitOnChange: true
                    }
                    if (aCeilingFansCmd == "decr") {
                        input "aCeilingFansDecr", "number", title: "...by this percentage", required: true, submitOnChange: true
                    }
                }
            }
        }

        if (actions?.contains("Thermostats, Vents, & Shades")) {
            section ("Thermostat", hideable: true, hidden: false) {
                input "cTstat", "capability.thermostat", title: "...and these thermostats will...", multiple: true, required: false, submitOnChange:true
                if (cTstat) {
                    input "cTstatFan", "enum", title: "...set the fan mode to...", options:["auto":"auto","on":"on","off":"off","circ":"circulate"], multiple: false, required: false, submitOnChange:true
                    input "cTstatMode", "enum", title: "...set the operating mode to...", options:["cool":"cooling","heat":"heating","auto":"auto","on":"on","off":"off","incr":"increase","decr":"decrease"], multiple: false, required: false, submitOnChange:true
                    if (cTstatMode in ["cool","auto"]) { input "coolLvl", "number", title: "Cool Setpoint", required: true, submitOnChange: true}
                    if (cTstatMode in ["heat","auto"]) { input "heatLvl", "number", title: "Heat Setpoint", required: true, submitOnChange: true}
                    if (cTstatMode in ["incr","decr"]) {
                        if (cTstatMode == "decr") {paragraph "NOTE: This will decrease the temp from the current room temp minus what you choose."}
                        if (cTstatMode == "incr") {paragraph "NOTE: This will increase the temp from the current room temp plus what you choose."}
                        input "tempChange", "number", title: "By this amount...", required: true, submitOnChange: true }
                }
            }
            if(cTstat) {
                section("Thermostats", hideable: true, hidden: false) {
                    input "cTstat1", "capability.thermostat", title: "More Thermostat(s)...", multiple: true, required: false, submitOnChange:true
                    if (cTstat1) {
                        input "cTstat1Fan", "enum", title: "Fan Mode", options:["auto":"Auto","on":"On","off":"Off","circ":"Circulate"],multiple: false, required: false, submitOnChange:true
                        input "cTstat1Mode", "enum", title: "Operating Mode", options:["cool":"Cool","heat":"Heat","auto":"Auto","on":"On","off":"Off","incr":"Increase","decr":"Decrease"],multiple: false, required: false, submitOnChange:true
                        if (cTstat1Mode in ["cool","auto"]) { input "coolLvl1", "number", title: "Cool Setpoint", required: true, submitOnChange: true }
                        if (cTstat1Mode in ["heat","auto"]) { input "heatLvl1", "number", title: "Heat Setpoint", required: true, submitOnChange: true }
                        if (cTstat1Mode in ["incr","decr"]) {
                            if (cTstat1Mode == "decr") {paragraph "NOTE: This will decrease the temp from the current room temp minus what you choose."}
                            if (cTstat1Mode == "incr") {paragraph "NOTE: This will increase the temp from the current room temp plus what you choose."}
                            input "tempChange1", "number", title: "By this amount...", required: true, submitOnChange: true }
                    }
                }
            }
            section ("Vents", hideable: true, hidden: false) {
                input "aVents", "capability.switchLevel", title: "These vents...", multiple: true, required: false, submitOnChange: true
                if (aVents) {
                    input "aVentsCmd", "enum", title: "...will...",
                        options:["on":"open","off":"close","25":"change to 25% open","50":"change to 50% open","75":"change to 75% open"], multiple: false, required: false, submitOnChange:true
                }
            }
            section ("Shades", hideable: true, hidden: false){
                input "aShades", "capability.windowShade", title: "These window coverings...", multiple: true, required: false, submitOnChange: true
                if (aShades) {
                    input "aShadesCmd", "enum", title: "...will...", options:["on":"open","off":"close","25":"change to 25% oetn","50":"change to 50% open","75":"change to 75% open"], multiple: false, required: false, submitOnChange:true
                }
            }
        }
    }
}
/***********************************************************************************************************
   ACTIONS OFF PAGE
************************************************************************************************************/
def offPage() {
    dynamicPage(name: "offPage", title: "Perform these actions when conditions have not been met.", install: false, uninstall: false) {
        section ("Select Actions Capabilities") {
            input "actionsOff", "enum", title: "Select Actions Capabilities", options:["Switches","Dimmers",
                                                                                    "Fans & Ceiling Fans","Thermostats, Vents, & Shades"], multiple: true, required: true, submitOnChange: true
        }
        if (actionsOff?.contains("Switches")) {
            section("Simple On/Off/Toggle Switches", hideable: true, hidden: false) {
                input "aOtherSwitchesOff", "capability.switch", title: "On/Off/Toggle Lights & Switches", multiple: true, required: false, submitOnChange: true
                if (aOtherSwitchesOff) {
                    input "aOtherSwitchesCmdOff", "enum", title: "...will turn...", options: ["on":"on","off":"off","toggle":"toggle"], multiple: false, required: false, submitOnChange: true
                }
            }
            if (aOtherSwitchesCmdOff != null) {
                section("More Simple On/Off/Toggle Switches", hideable: true, hidden: false) {
                    input "aOtherSwitches2Off", "capability.switch", title: "On/Off/Toggle Lights & Switches", multiple: true, required: false, submitOnChange: true
                    if (aOtherSwitches2Off) {
                        input "aOtherSwitchesCmd2Off", "enum", title: "...will turn...", options: ["on":"on","off":"off","toggle":"toggle"], multiple: false, required: false, submitOnChange: true
                    }
                }
            }
        }
        if (actionsOff?.contains("Dimmers")) {
            section ("Dimmers - Selection", hideable: true, hidden: false) {
                input "aDimOff", "capability.switchLevel", title: "Dimmable Lights and Switches", multiple: true, required: false , submitOnChange:true
                if (aDimOff) {
                    input "aDimCmdOff", "enum", title: "...will...", options:["on":"turn on","off":"turn off","set":"set the level","decrease":"decrease","increase":"increase"], multiple: false, required: false, submitOnChange: true
                    if (aDimCmdOff=="decrease") {
                        input "aDimDecreaseOff", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aDimCmdOff == "increase") {
                        input "aDimIncreaseOff", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aDimCmdOff == "set") {
                        input "aDimLVLOff", "number", title: "...of the lights to...", description: "this percentage", range: "0..100", required: false, submitOnChange: true
                    }
                    input "aDimDelayOff", "number", title: "Delay this action by this many seconds.", required: false, defaultValue: 0, submitOnChange: true
                }
            }
            section("More Dimmers - Selection", hideable: true, hidden: false) {
                input "aOtherDimOff", "capability.switchLevel", title: "More Dimmers", multiple: true, required: false , submitOnChange:true
                if (aOtherDimOff) {
                    input "aOtherDimCmdOff", "enum", title: "...will...", options:["on":"turn on","off":"turn off","set":"set the level","decrease":"decrease","increase":"brighten"], multiple: false, required: false, submitOnChange:true
                    if (aOtherDimCmdOff=="decrease") {
                        input "aOtherDimDecreaseOff", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aOtherDimCmdOff == "increase") {
                        input "aOtherDimIncreaseOff", "number", title: "the lights by this %", required: false, submitOnChange: true
                    }
                    if (aOtherDimCmdOff == "set") {
                        input "aOtherDimLVLOff", "number", title: "...of the lights to...", description: "this percentage", range: "0..100", required: false, submitOnChange: true
                    }
                    input "otherDimDelayOff", "number", title: "Delay this action by this many seconds.", required: false, defaultValue: 0, submitOnChange: true
                }
            }
        }
        if (actionsOff?.contains("Fans & Ceiling Fans")) {
            section ("Fans connected to switches", hideable: true, hidden: false) {
                input "aFansOff", "capability.switch", title: "These Fans...", multiple: true, required: false, submitOnChange: true, image: "https://raw.githubusercontent.com/bamarayne/master/LogicRulz/icons/fan.png"
                if (aFansOff) {
                    input "aFansCmdOff", "enum", title: "...will...", options:["on":"turn on","off":"turn off"], multiple: false, required: false, submitOnChange:true
                    if (aFansCmdOff=="on") {
                        input "aFansDelayOnOff", "number", title: "Delay turning on by this many seconds", defaultValue: 0, submitOnChange:true
             //           if (aFansDelayOnOff) input "aFansPendOnOff", "bool", title: "Activate for pending state change cancellation", required: false, default: false
                    }
                    if (aFansCmdOff=="off") {
                        input "aFansDelayOffOff", "number", title: "Delay turning off by this many seconds", defaultValue: 0, submitOnChange:true
             //           if (aFansDelayOffOff) input "aFansPendOffOff", "bool", title: "Activate for pending state change cancellation", required: false, default: false
                    }
                }
            }
            section ("Fans and Ceiling Fan Settings (adjustable)", hideable: true, hidden: false) {
                input "aCeilingFansOff", "capability.switchLevel", title: "These ceiling fans...", multiple: true, required: false, submitOnChange: true
                if (aCeilingFansOff) {
                    input "aCeilingFansCmdOff", "enum", title: "...will...", options:["on":"turn on","off":"turn off","low":"set to low","med":"set to med","high":"set to high","incr":"speed up","decr":"slow down"], multiple: false, required: false, submitOnChange:true
                    if (aCeilingFansCmdOff == "incr") {
                        input "aCeilingFansIncrOff", "number", title: "...by this percentage", required: true, submitOnChange: true
                    }
                    if (aCeilingFansCmdOff == "decr") {
                        input "aCeilingFansDecrOff", "number", title: "...by this percentage", required: true, submitOnChange: true
                    }
                }
            }
        }

        if (actionsOff?.contains("Thermostats, Vents, & Shades")) {
            section ("Thermostat", hideable: true, hidden: false) {
                input "cTstatOff", "capability.thermostat", title: "...and these thermostats will...", multiple: true, required: false, submitOnChange:true
                if (cTstatOff) {
                    input "cTstatFanOff", "enum", title: "...set the fan mode to...", options:["auto":"auto","on":"on","off":"off","circ":"circulate"], multiple: false, required: false, submitOnChange:true
                    input "cTstatModeOff", "enum", title: "...set the operating mode to...", options:["cool":"cooling","heat":"heating","auto":"auto","on":"on","off":"off","incr":"increase","decr":"decrease"], multiple: false, required: false, submitOnChange:true
                    if (cTstatModeOff in ["cool","auto"]) { input "coolLvl", "number", title: "Cool Setpoint", required: true, submitOnChange: true}
                    if (cTstatModeOff in ["heat","auto"]) { input "heatLvl", "number", title: "Heat Setpoint", required: true, submitOnChange: true}
                    if (cTstatModeOff in ["incr","decr"]) {
                        if (cTstatModeOff == "decr") {paragraph "NOTE: This will decrease the temp from the current room temp minus what you choose."}
                        if (cTstatModeOff == "incr") {paragraph "NOTE: This will increase the temp from the current room temp plus what you choose."}
                        input "tempChangeOff", "number", title: "By this amount...", required: true, submitOnChange: true }
                }
            }
            if(cTstatOff) {
                section("ThermostatsOff", hideable: true, hidden: false) {
                    input "cTstat1Off", "capability.thermostat", title: "More Thermostat(s)...", multiple: true, required: false, submitOnChange:true
                    if (cTstat1Off) {
                        input "cTstat1FanOff", "enum", title: "Fan Mode", options:["auto":"Auto","on":"On","off":"Off","circ":"Circulate"],multiple: false, required: false, submitOnChange:true
                        input "cTstat1ModeOff", "enum", title: "Operating Mode", options:["cool":"Cool","heat":"Heat","auto":"Auto","on":"On","off":"Off","incr":"Increase","decr":"Decrease"],multiple: false, required: false, submitOnChange:true
                        if (cTstat1ModeOff in ["cool","auto"]) { input "coolLvl1", "number", title: "Cool Setpoint", required: true, submitOnChange: true }
                        if (cTstat1ModeOff in ["heat","auto"]) { input "heatLvl1", "number", title: "Heat Setpoint", required: true, submitOnChange: true }
                        if (cTstat1ModeOff in ["incr","decr"]) {
                            if (cTstat1ModeOff == "decr") {paragraph "NOTE: This will decrease the temp from the current room temp minus what you choose."}
                            if (cTstat1ModeOff == "incr") {paragraph "NOTE: This will increase the temp from the current room temp plus what you choose."}
                            input "tempChange1Off", "number", title: "By this amount...", required: true, submitOnChange: true }
                    }
                }
            }
            section ("Vents", hideable: true, hidden: false) {
                input "aVentsOff", "capability.switchLevel", title: "These vents...", multiple: true, required: false, submitOnChange: true
                if (aVentsOff) {
                    input "aVentsCmdOff", "enum", title: "...will...",
                        options:["on":"open","off":"close","25":"change to 25% open","50":"change to 50% open","75":"change to 75% open"], multiple: false, required: false, submitOnChange:true
                }
            }
            section ("Shades", hideable: true, hidden: false){
                input "aShadesOff", "capability.windowShade", title: "These window coverings...", multiple: true, required: false, submitOnChange: true
                if (aShadesOff) {
                    input "aShadesCmdOff", "enum", title: "...will...", options:["on":"open","off":"close","25":"change to 25% oetn","50":"change to 50% open","75":"change to 75% open"], multiple: false, required: false, submitOnChange:true
                }
            }
        }
    }
}


/************************************************************************************************************
	Base Process
************************************************************************************************************/
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
	if(settings?.priFan && settings?.gasSafetyCheck) {
        subscribe(settings?.priFan, "switch.on", safetyCheck)
    } else {
        subscribe(settings?.priFan, "switch.on", conditionHandler)
    }
    subscribe(settings?.priFan, "switch.off", processOffActions)
}

String appImg() { return getAppImg("fan_manager_whole.png") }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/tonesto7/smartthings-tonesto7-public/master/resources/icons/$imgName" }

def appInfoSect()	{
    def str = ""
    str += "${app?.name}"
    str += "\n• Version: ${appVer()}"
    str += "\n• Updated: ${appDate()}"
    section() { paragraph "${str}", image: appImg()	}
}

def Logger(msg, type) {
    if(msg && type) {
        switch(type) {
            case "info":
                log.info "${msg}"
                break
            case "trace":
                log.trace "${msg}"
                break
            case "error":
                log.error "${msg}"
                break
            case "warn":
                log.warn "${msg}"
                break
            default:
                log.debug "${msg}"
                break
        }
    }
}

void settingUpdate(name, value, type=null) {
    LogAction("settingUpdate($name, $value, $type)...", "trace", false)
    if(name && type) {
        app?.updateSetting("$name", [type: "$type", value: value])
    }
    else if (name && type == null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
    Logger("settingRemove($name)...", "trace")
    if(name) { app?.deleteSetting("$name") }
}

def stateUpdate(key, value) {
    if(key) { state?."${key}" = value }
    else { Logger("stateUpdate: null key $key $value", "error") }
}

def getFanStatusDesc() {
    def fanstr = "Not Ready Yet"
    // fanstr += "\n• Power: (${fanDevice?.currentState("switch")?.value?.toString()?.capitalize()})"
    // if(tempSensors) {
    //     if(state?.speedType) {
    //         fanstr += "\n• Speed Support: (${state?.speedType})"
    //         fanstr += "\n• Current Speed: (${getCurrentFanSpeed(settings?.fanDevice)})"
    //         fanstr += "\n• Desired Speed: (${getFanSpeedToUse(settings?.fanDevice, true)})"
    //         if(offSpeedRange || lowSpeedRange || medSpeedRange || medHighSpeedRange || allowHighSpeed) {
    //             fanstr += "\n\n• Speed Thresholds:"
    //             fanstr += settings?.offSpeedRange ? "\n  - Off: (<${settings?.offSpeedRange}${tempUnitStr()})" : ""
    //             fanstr += settings?.lowSpeedRange ? "\n  - Low: (>=${settings?.offSpeedRange}${tempUnitStr()} & <=${settings?.lowSpeedRange}${tempUnitStr()})" : ""
    //             fanstr += settings?.medSpeedRange ? "\n  - Med: (>${settings?.lowSpeedRange}${tempUnitStr()} & <=${settings?.medSpeedRange}${tempUnitStr()})" : ""
    //             fanstr += settings?.medHighSpeedRange ? "\n  - Med-High: (>${settings?.medSpeedRange}${tempUnitStr()} & <=${settings?.medHighSpeedRange}${tempUnitStr()})" : ""
    //             fanstr += settings?.allowHighSpeed ? "\n  - High: (>${settings?.medHighSpeedRange ? settings?.medHighSpeedRange : settings?.medSpeedRange}${tempUnitStr()})" : ""
    //         }
    //     } else {
    //         if(fanNoSpeedOnVal) {
    //             fanstr += "\n• Speed Support: (None)"
    //             fanstr += "\n• Temp Trigger: (${settings?.fanNoSpeedOnVal}${tempUnitStr()})"
    //         }
    //     }
    //     def tempstr = "(${getDeviceTempAvg(settings?.tempSensors)}${tempUnitStr()})"
    //     fanstr += "\n\n• Current Temp${settings?.tempSensors?.size() > 1 ? " [Avg]" : ""}: $tempstr"
    // }
    // if(humiditySensors) {
    //     if(state?.speedType) {
    //         fanstr += "\n• Speed Support: (${state?.speedType})"
    //         fanstr += "\n• Current Speed: (${getCurrentFanSpeed(settings?.fanDevice)})"
    //         fanstr += "\n• Desired Speed: (${settings?.humiditySetFanSpeed})"
    //     } else {
    //         if(fanOnHumidityVal) {
    //             fanstr += "\n• Speed Support: (None)"
    //             fanstr += "\n• Humidity Trigger: (${settings?.fanOnHumidityVal}%)"
    //         }
    //     }

    //     def humstr = "(${getDeviceHumidityAvg(settings?.humiditySensors)}%)"
    //     fanstr += "\n\n• Current Humidity: ${settings?.humiditySensors?.size() > 1 ? " [Avg]" : ""}: $humstr"
    // }
    // if(tstatDevice) {
    //     fanstr += "\n\nThermostat Control:"
    //     String tstatState = settings?.tstatDevice?.currentState("thermostatOperatingState")?.value ?: ""
    //     fanstr += "\n• HVAC States: ${settings?.tstatHvacStates}"
    //     fanstr += "\n• Current HVAC: (${tstatState?.toString()?.replaceAll("\\[|\\]", "")})"
    //     fanstr += "\n• Running Fan: (${checkTstatState()})"
    // }

    // def restrict = ignoreActions()
    // fanstr += "\n\n• Actions Blocked: (${restrict})"
    // fanstr += restrict ? "\n└ Reason: ${ignoreActions(true)}" : ""
    // fanstr += state?.lastChangeDt ? "\n• Last Change: (${GetTimeDiffSeconds(state?.lastChangeDt)} sec)" : ""
    return fanstr
}

/***********************************************************************************************************
   SAFETY CHECK
************************************************************************************************************/
def safetyCheck(evt) {
    def devList = []
    def safetyCheck = true
    def safetyTime = 2
    def msg = "Hey, The ${settings?.priFan} is being turned off due to there not being adequate ventilation available. Please open some windows and select those " +
        "windows in the House Fan Controller app."
    log.warn "Performing Safety Check by Verifying proper ventilation due to gas appliances present"
    if (settings?.cContactWindow == null) {
        sendPush(msg)
        runIn(safetyTime, safetyMethod)
        return
    }
    def cContactWindowSize = settings?.cContactWindow?.size()
    settings?.cContactWindow.each { deviceName ->
        def status = deviceName.currentValue("contact")
        if (status == "open"){
            String device = (String) deviceName
            devList += device
        }
    }
    def devListSize = devList?.size()
    if (devListSize == 0 || devListSize == null) {
        safetyCheck = false
    }
    if (safetyCheck == true) {
        conditionHandler(evt)
    }
    if (safetyCheck == false) {
        runIn(safetyTime, safetyMethod)
        if (failMsg == null) {
            ttsActions(msg)
            sendPush(msg)
        }
        else {
            ttsActions(failMsg)
        }
    }
}

/***********************************************************************************************************
   CONDITIONS HANDLER
************************************************************************************************************/
def conditionHandler(evt) {
    def result
    def cSwitchOk = false
    def cHumOk = false
    def cTempOk = false
    def cModeOk = false
    def cPresenceOk = false
    def cDoorOk = false
    def cWindowOk = false
    def cDaysOk = false
    def cPendAll = false
    def timeOk = false
    def cGarageOk = false
    def devList = []
    def safetyTime = 5
    def cContactWindowMin = 1
    def msg = "The ${settings?.priFan} is being turned off due to your preset conditions having not been met. Please see the House Fan Controller app for more information."

    log.info "Verifying Conditions:"

    // SWITCHES
    if (cSwitch == null) { cSwitchOk = true }
    if (cSwitch) {
        log.trace "Conditions: Switches events method activated"
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
            }
        }
        if(cSwitchAll) {
            if (devListSize == cSwitchSize) {
                cSwitchOk = true
            }
        }
        if (cSwitchOk == false) log.warn "Switches Conditions Handler failed"
    }

    // HUMIDITY
    if (cHumidity == null) {cHumOk = true }
    if (cHumidity) {
        log.trace "Conditions: Humidity events method activated"
        int cHumidityStopVal = cHumidityStop == null ? 0 : cHumidityStop as int
            cHumidity.each { deviceName ->
                def status = deviceName.currentValue("humidity")
                if (cHumidityLevel == "above") {
                    cHumidityStopVal = cHumidityStopVal == 0 ? 999 :  cHumidityStopVal as int
                        if (status >= cHumidityPercent && status <= cHumidityStopVal) {
                            cHumOk = true
                        }
                }
                if (cHumidityLevel == "below") {
                    if (status <= cHumidityPercent && status >= cHumidityStopVal) {
                        cHumOk = true
                    }
                }
            }
        if (cHumOk == false) log.warn "Humidity Conditions Handler failed"
    }

    // TEMPERATURE
    if (cTemperature == null) {cTempOk = true }
    if (cTemperature) {
        log.trace "Conditions: Temperature events method activated"
        int cTemperatureStopVal = cTemperatureStop == null ? 0 : cTemperatureStop as int
            cTemperature.each { deviceName ->
                def status = deviceName.currentValue("temperature")
                if (cTemperatureLevel == "above") {
                    cTemperatureStopVal = cTemperatureStopVal == 0 ? 999 :  cTemperatureStopVal as int
                        if (status >= cTemperatureDegrees && status <= cTemperatureStopVal) {
                            cTempOk = true
                        }
                }
                if (cTemperatureLevel == "below") {
                    if (status <= cTemperatureDegrees && status >= cTemperatureStopVal) {
                        cTempOk = true
                    }
                }
            }
        if (cTempOk == false) log.warn "Temperature Conditions Handler failed"
    }

    // DAYS OF THE WEEK
    if (cDays == null) { cDaysOk = true }
    if (cDays) {
        log.trace "Conditions: Days of the Week events method activated"
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        if (cDaysOk == false) log.warn "Days Conditions Handler failed"
        result = cDays.contains(day)
    }

    // LOCATION MODE
    if (cMode == null) { cModeOk = true }
    if (cMode) {
        log.trace "Conditions: Mode events method activated"
        cModeOk = !cMode || cMode?.contains(location.mode)
        if (cModeOk == false) log.warn "Mode Conditions Handler failed"
    }

    // PRESENCE
    if (cPresence == null) { cPresenceOk = true }
    if (cPresence) {
        log.trace "Conditions: Presence events method activated"
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
            }
        }
        if(cPresenceAll) {
            if (devListSize == cPresenceSize) {
                cPresenceOk = true
            }
        }
        if (cPresenceOk == false) log.warn "Presence Conditions Handler failed"
    }

    // DOOR CONTACT SENSORS
    if (cContactDoor == null) { cDoorOk = true }
    if (cContactDoor) {
        log.trace "Conditions: Door Contacts events method activated"
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
            }
        }
        if(cContactDoorAll) {
            if (devListSize == cContactDoorSize) {
                cDoorOk = true
            }
        }
        if (cDoorOk == false) log.warn "Door Contacts Conditions Handler failed"
    }

    // WINDOW CONTACT SENSORS
    if (cContactWindow == null) { cWindowOk = true }
    if (cContactWindow) {
        log.trace "Conditions: Window Contacts events method activated"
        def cContactWindowSize = cContactWindow?.size()
        cContactWindow.each { deviceName ->
            def status = deviceName.currentValue("contact")
            if (status == "open"){
                String device  = (String) deviceName
                devList += device
            }
        }
        def devListSize = devList?.size()
        if(!cContactWindowAll) {
            if (devListSize >= cContactWindowMin) {
                log.info "devListSizedevListSize = $devListSize"
                cWindowOk = true
            }
            else {
                cWindowOk = false
                log.warn "Minimum of $cContactWindowMin windows are required to be open, there are ${devList?.size()} windows open"
            }
        }
        if(cContactWindowAll) {
            if (devListSize == cContactWindowSize) {
                cWindowOk = true
            }
        }
        if (cWindowOk == false) log.warn "Window Contacts Conditions Handler failed"
    }

    // GARAGE DOORS
    if (cGarage == null) { cGarageOk = true }
    if (cGarage) {
        log.trace "Conditions: Garage Doors events method activated"
        cGarage.each { deviceName ->
            def status = deviceName.currentValue("door")
            if (status == "${cGarageCmd}"){
                cGarageOk = true
            }
            if (cGarageOk == false) log.warn "Garage Conditions Handler failed"
        }
    }


    if (cGarageOk==true && cTempOk==true && cHumOk==true && cSwitchOk==true && cModeOk==true &&
        cPresenceOk==true && cDoorOk==true && cWindowOk==true && cDaysOk==true && getTimeOk(evt)==true) {
        result = true
    }
    if (result == true) {
        log.warn "Conditions Verified ==> All Conditions have been met"
        processOnActions()
    } else {
        log.warn "Conditions Verified ==> All Conditions have NOT been met, ${settings?.priFan} will turn off in 10 seconds."
        if (failMsg == null) {
            runIn(10, safetyMethod)
            ttsActions(msg)
        }
        else {
            ttsActions(failMsg)
            runIn(10, safetyMethod)
        }
    }
    return result
}

/***********************************************************************************************************************
	SAFETY METHOD THAT TURNS OFF FAN AUTOMATICALLY
***********************************************************************************************************************/
def safetyMethod() {
    log.info "Turning off the ${settings?.priFan}"
    settings?.priFan?.off()
    return
}

/***********************************************************************************************************************
	PROCESS ACTIONS HANDLER WHEN FAN TURNS ON
***********************************************************************************************************************/
def processOnActions(evt){
    log.info "Process On Actions Method activated."
//    if (conditionHandler()==true && getTimeOk()==true) {
        def result
        def devList = []
        def aSwitchSize = settings?.aSwitch?.size()

        // OTHER SWITCHES
        if (aOtherSwitches) {
            if (settings?.aOtherSwitchesCmd == "on") { settings?.aOtherSwitches?.on() }
            if (settings?.aOtherSwitchesCmd == "off") { settings?.aOtherSwitches?.off() }
            if (settings?.aOtherSwitchesCmd == "toggle") { toggle2() }
        }
        if (settings?.aOtherSwitches2) {
            if (settings?.aOtherSwitchesCmd2 == "on") { settings?.aOtherSwitches2?.on() }
            if (settings?.aOtherSwitchesCmd2 == "off") { settings?.aOtherSwitches2?.off() }
            if (settings?.aOtherSwitchesCmd2 == "toggle") { toggle3() }
        }

        // DIMMERS
        if (settings?.aDim) {
            runIn(settings?.aDimDelay, dimmersHandler)
        }
        if (settings?.aOtherDim) {
            runIn(settings?.otherDimDelay, otherDimmersHandler)
        }

        // CEILING FANS
        if (settings?.aCeilingFans) {
            if (settings?.aCeilingFansCmd == "on") { settings?.aCeilingFans.on() }
            else if (settings?.aCeilingFansCmd == "off") { settings?.aCeilingFans.off() }
            else if (settings?.aCeilingFansCmd == "low") { settings?.aCeilingFans.setLevel(33) }
            else if (settings?.aCeilingFansCmd == "med") { settings?.aCeilingFans.setLevel(66) }
            else if (settings?.aCeilingFansCmd == "high") { settings?.aCeilingFans.setLevel(99) }
            if (settings?.aCeilingFansCmd == "incr") {
                def newLevel
                settings?.aCeilingFans?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aCeilingFansIncr
                    newLevel = newLevel + currLevel
                    newLevel = newLevel < 0 ? 0 : newLevel > 99 ? 99 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
            if (settings?.aCeilingFansCmd == "decr") {
                def newLevel
                settings?.aCeilingFans?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aCeilingFansDecr
                    newLevel = currLevel - newLevel
                    newLevel = newLevel < 0 ? 0 : newLevel > 99 ? 99 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
        }
        // FANS
        if (settings?.aFansCmd == "on") {
            runIn(settings?.aFansDelayOn, aFansOn) }
        if (settings?.aFansCmd == "off") {
            runIn(settings?.aFansDelayOff, aFansOff) }

        // VENTS
        if (settings?.aVents) {
            if (settings?.sVentsCmd == "on") { settings?.aVents.setLevel(100) }
            else if (aVentsCmd == "off") { settings?.aVents.off() }
            else if (aVentsCmd == "25") { settings?.aVents.setLevel(25) }
            else if (aVentsCmd == "50") { settings?.aVents.setLevel(50) }
            else if (aVentsCmd == "75") { settings?.aVents.setLevel(75) }
        }

        // WINDOW COVERINGS
        if (settings?.aShades) {
            if (settings?.aShadesCmd == "open") { aShades.setLevel(100) }
            else if (settings?.aShadesCmd == "close") { aShades.setLevel(0) }
            else if (settings?.aShadesCmd == "25") { aShades.setLevel(25) }
            else if (settings?.aShadesCmd == "50") { aShades.setLevel(50) }
            else if (settings?.aShadesCmd == "75") { aShades.setLevel(75) }
        }

        // THERMOSTATS
        if (settings?.cTstat) { thermostats() }
        if (settings?.cTstat1) { thermostats1() }
//    }
}

/***********************************************************************************************************************
    DIMMERS HANDLER - FOR ACTIONS ON PROCESS
***********************************************************************************************************************/
def dimmersHandler() {
	if (logs) log.info "Dimmers Handler activated"
		if (settings?.aDim) {
            if (settings?.aDimCmd == "on") { settings?.aDim.on() }
            else if (settings?.aDimCmd == "off") { settings?.aDim.off() }
            if (settings?.aDimCmd == "set") {
                def level = aDimLVL < 0 || !aDimLVL ?  0 : aDimLVL >100 ? 100 : aDimLVL as int
                    aDim.setLevel(level)
            }
            if (settings?.aDimCmd == "increase") {
                def newLevel
                settings?.aDim?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aDimIncrease
                    newLevel = newLevel + currLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
            if (settings?.aDimCmd == "decrease") {
                def newLevel
                settings?.aDim?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aDimDecrease
                    newLevel = currLevel - newLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
        }
    }

/***********************************************************************************************************************
    OTHER DIMMERS HANDLER - FOR ACTIONS ON PROCESS
***********************************************************************************************************************/
def otherDimmersHandler() {
	if (settings?.logs) log.info "Other Dimmers Handler activated"
        if (settings?.aOtherDim) {
            if (settings?.aOtherDimCmd == "on") { settings?.aOtherDim.on() }
            else if (settings?.aOtherDimCmd == "off") { settings?.aOtherDim.off() }
            if (settings?.aOtherDimCmd == "set") {
                def otherLevel = (aOtherDimLVL < 0 || !aOtherDimLVL) ? 0 : (aOtherDimLVL > 100 ? 100 : aOtherDimLVL as int)
                settings?.aOtherDim?.setLevel(otherLevel)
            }
            if (settings?.aOtherDimCmd == "increase") {
                def newLevel
                settings?.aOtherDim.each { deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aOtherDimIncrease
                    newLevel = newLevel + currLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
            if (settings?.aOtherDimCmd == "decrease") {
                def newLevel
                settings?.aOtherDimCmd?.each { deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aOtherDimDecrease
                    newLevel = currLevel - newLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
        }
	}

/************************************************************************************************************
FANS Handler  - FOR ACTIONS ON PROCESS
************************************************************************************************************/
def aFansOn(evt) {
    if (parent.debug) { log.info "Fan device handler turn on activated" }
    settings?.aFans?.on()
}
def aFansOff(evt) {
    if (parent.debug) { log.info "Fan device handler turn off activated" }
    settings?.aFans?.off()
}

/************************************************************************************************************
THERMOSTATS HANDLERS - FOR ACTIONS ON PROCESS
************************************************************************************************************/
private thermostats(evt) {
    if (settings?.logs) { log.info "thermostats handler method activated" }
    settings?.cTstat.each {deviceD ->
        def currentMode = deviceD.currentValue("thermostatMode")
        def currentTMP = deviceD.currentValue("temperature")
        if (settings?.cTstatMode == "off") { settings?.cTstat.off() }
        if (settings?.cTstatMode == "auto" || settings?.cTstatMode == "on") {
            settings?.cTstat.auto()
            settings?.cTstat.setCoolingSetpoint(coolLvl)
            settings?.cTstat.setHeatingSetpoint(heatLvl)
        }
        if (settings?.cTstatMode == "cool") {
            settings?.cTstat.cool()
            settings?.cTstat.setCoolingSetpoint(coolLvl)
        }
        if (settings?.cTstatMode == "heat") {
            settings?.cTstat.heat()
            settings?.cTstat.setHeatingSetpoint(heatLvl)
        }
        if (settings?.cTstatMode == "incr") {
            def cNewSetpoint = tempChange
            cNewSetpoint = tempChange + currentTMP
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange
            hNewSetpoint = tempChange + currentTMP
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (settings?.cTstatMode == "decr") {
            def cNewSetpoint = tempChange
            cNewSetpoint = currentTMP - tempChange
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange
            hNewSetpoint = currentTMP - tempChange
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (settings?.cTstatFan == "auto" || settings?.cTstatFan == "off") { settings?.cTstat.fanAuto() }
        if (settings?.cTstatFan == "on") { settings?.cTstat.fanOn() }
        if (settings?.cTstatFan == "circ") { settings?.cTstat.fanCirculate() }
    }
}
private thermostats1(evt) {
    settings?.cTstat1.each {deviceD ->
        def currentMode = deviceD.currentValue("thermostatMode")
        def currentTMP = deviceD.currentValue("temperature")
        if (settings?.cTstat1Mode == "off") { settings?.cTstat1.off() }
        if (settings?.cTstat1Mode == "auto" || settings?.cTstat1Mode == "on") {
            settings?.cTstat1.auto()
            settings?.cTstat1.setCoolingSetpoint(coolLvl1)
            settings?.cTstat1.setHeatingSetpoint(heatLvl1)
        }
        if (settings?.cTstat1Mode == "auto" || settings?.cTstat1Mode == "on") {
            settings?.cTstat1.auto()
            settings?.cTstat1.setCoolingSetpoint(coolLvl1)
            settings?.cTstat1.setHeatingSetpoint(heatLvl1)
        }
        if (settings?.cTstat1Mode == "incr") {
            def cNewSetpoint = tempChange1
            cNewSetpoint = tempChange1 + currentTMP
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange1
            hNewSetpoint = tempChange1 + currentTMP
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (settings?.cTsta1tMode == "decr") {
            def cNewSetpoint = tempChange1
            cNewSetpoint = currentTMP - tempChange1
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange1
            hNewSetpoint = currentTMP - tempChange1
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (settings?.cTstat1Fan == "auto" || settings?.cTstat1Fan == "off") { settings?.cTstat1.fanAuto() }
        if (settings?.cTstat1Fan == "on") { settings?.cTstat1.fanOn() }
        if (settings?.cTstat1Fan == "circ") { settings?.cTstat1.fanCirculate() }
    }
}

/***********************************************************************************************************************
	PROCESS ACTIONS HANDLER WHEN FAN TURNS OFF
***********************************************************************************************************************/
def processOffActions(evt){
    log.info "Process Off Actions Method activated."
    def result
    def devList = []
    def aSwitchSize = settings?.aSwitch?.size()

    // OTHER SWITCHES
    if (settings?.aOtherSwitchesOff) {
        if (settings?.aOtherSwitchesCmdOff == "on") { settings?.aOtherSwitchesOff?.on() }
        if (settings?.aOtherSwitchesCmdOff == "off") { settings?.aOtherSwitchesOff?.off() }
        if (settings?.aOtherSwitchesCmdOff == "toggle") {toggle2Off()}
    }
    if (aOtherSwitches2Off) {
        if (settings?.aOtherSwitchesCmd2Off == "on") { settings?.aOtherSwitches2Off?.on() }
        if (settings?.aOtherSwitchesCmd2Off == "off") { settings?.aOtherSwitches2Off?.off() }
        if (settings?.aOtherSwitchesCmd2Off == "toggle") { toggle3Off() }
    }

    // DIMMERS
    if (settings?.aDimOff) {
        runIn(settings?.aDimDelayOff, dimmersHandlerOff)
    }
    if (settings?.aOtherDimOff) {
        runIn(settings?.otherDimDelayOff, otherDimmersHandlerOff)
    }

    // CEILING FANS
    if (settings?.aCeilingFansOff) {
        if (settings?.aCeilingFansCmdOff == "on") { settings?.aCeilingFansOff.on() }
        else if (settings?.aCeilingFansCmdOff == "off") { settings?.aCeilingFansOff.off() }
        else if (settings?.aCeilingFansCmdOff == "low") { settings?.aCeilingFansOff.setLevel(33) }
        else if (settings?.aCeilingFansCmdOff == "med") { settings?.aCeilingFansOff.setLevel(66) }
        else if (settings?.aCeilingFansCmdOff == "high") { settings?.aCeilingFansOff.setLevel(99) }
        if (settings?.aCeilingFansCmdOff == "incr") {
            def newLevel
            settings?.aCeilingFansOff?.each {deviceD ->
                def currLevel = deviceD.latestValue("level")
                newLevel = aCeilingFansIncrOff
                newLevel = newLevel + currLevel
                newLevel = newLevel < 0 ? 0 : newLevel > 99 ? 99 : newLevel
                deviceD.setLevel(newLevel)
            }
        }
        if (settings?.aCeilingFansCmdOff == "decr") {
            def newLevel
            settings?.aCeilingFansOff?.each {deviceD ->
                def currLevel = deviceD.latestValue("level")
                newLevel = aCeilingFansDecrOff
                newLevel = currLevel - newLevel
                newLevel = newLevel < 0 ? 0 : newLevel > 99 ? 99 : newLevel
                deviceD.setLevel(newLevel)
            }
        }
    }
    // FANS
    if (settings?.aFansCmdOff == "on") {
        runIn(settings?.aFansDelayOnOff, aFansOnOff) }
    if (settings?.aFansCmdOff == "off") {
        runIn(settings?.aFansDelayOffOff, aFansOffOff) }

    // VENTS
    if (settings?.aVentsOff) {
        if (settings?.sVentsCmdOff == "on") { settings?.aVentsOff.setLevel(100) }
        else if (settings?.aVentsCmdOff == "off") { settings?.aVentsOff.off() }
        else if (settings?.aVentsCmdOff == "25") { settings?.aVentsOff.setLevel(25) }
        else if (settings?.aVentsCmdOff == "50") { settings?.aVentsOff.setLevel(50) }
        else if (settings?.aVentsCmdOff == "75") { settings?.aVentsOff.setLevel(75) }
    }

    // WINDOW COVERINGS
    if (settings?.aShadesOff) {
        if (settings?.aShadesCmdOff == "open") { settings?.aShadesOff.setLevel(100) }
        else if (settings?.aShadesCmdOff == "close") { settings?.aShadesOff.setLevel(0) }
        else if (settings?.aShadesCmdOff == "25") { settings?.aShadesOff.setLevel(25) }
        else if (settings?.aShadesCmdOff == "50") { settings?.aShadesOff.setLevel(50) }
        else if (settings?.aShadesCmdOff == "75") { settings?.aShadesOff.setLevel(75) }
    }

    // THERMOSTATS
    if (settings?.cTstatOff) { thermostatsOff() }
    if (settings?.cTstat1Off) { thermostats1Off() }

}

/***********************************************************************************************************************
    DIMMERS HANDLER - FOR ACTIONS OFF PROCESS
***********************************************************************************************************************/
def dimmersOffHandlerOff() {
	if (logs) log.info "Dimmers Handler activated"
		if (aDimOff) {
            if (aDimCmdOff == "on") {aDimOff.on()}
            else if (aDimCmdOff == "off") {aDimOff.off()}
            if (aDimCmdOff == "set" && aDimOff) {
                def level = aDimLVLOff < 0 || !aDimLVLOff ?  0 : aDimLVLOff >100 ? 100 : aDimLVLOff as int
                    aDimOff.setLevel(level)
            }
            if (aDimCmdOff == "increase" && aDim) {
                def newLevel
                aDimOff?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aDimIncrease
                    newLevel = newLevel + currLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
            if (aDimCmdOff == "decrease" && aDimOff) {
                def newLevel
                aDimOff?.each {deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aDimDecrease
                    newLevel = currLevel - newLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
        }
    }

/***********************************************************************************************************************
    OTHER DIMMERS HANDLER - FOR ACTIONS OFF PROCESS
***********************************************************************************************************************/
def otherDimmersOffHandler() {
	if (logs) log.info "Other Dimmers Handler activated"
        if (aOtherDimOff) {
            if (aOtherDimCmdOff == "on") {aOtherDimOff.on()}
            else if (aOtherDimCmdOff == "off") {aOtherDimOff.off()}
            if (aOtherDimCmdOff == "set" && aOtherDimOff) {
                def otherLevel = aOtherDimLVLOff < 0 || !aOtherDimLVLOff ?  0 : aOtherDimLVLOff >100 ? 100 : aOtherDimLVLOff as int
                    aOtherDimOff?.setLevel(otherLevel)
            }
            if (aOtherDimCmdOff == "increase" && aOtherDimOff) {
                def newLevel
                aOtherDimOff.each { deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aOtherDimIncreaseOff
                    newLevel = newLevel + currLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
            if (aOtherDimCmdOff == "decrease" && aOtherDimOff) {
                def newLevel
                aOtherDimCmdOff?.each { deviceD ->
                    def currLevel = deviceD.latestValue("level")
                    newLevel = aOtherDimDecreaseOff
                    newLevel = currLevel - newLevel
                    newLevel = newLevel < 0 ? 0 : newLevel >100 ? 100 : newLevel
                    deviceD.setLevel(newLevel)
                }
            }
        }
	}

/************************************************************************************************************
FANS Handler  - FOR ACTIONS OFF PROCESS
************************************************************************************************************/
def aFansOnOff(evt) {
    if (logs) log.info "Fan device handler turn on activated"
    aFansOff?.on()
}
def aFansOffOff(evt) {
    if (logs) log.info "Fan device handler turn off activated"
    aFansOff?.off()
}

/************************************************************************************************************
THERMOSTATS HANDLERS - FOR ACTIONS OFF PROCESS
************************************************************************************************************/
private thermostatsOff(evt) {
    if (logs) log.info "thermostats handler method activated"
    cTstatOff.each {deviceD ->
        def currentMode = deviceD.currentValue("thermostatMode")
        def currentTMP = deviceD.currentValue("temperature")
        if (cTstatModeOff == "off") { cTstatOff.off()
                                 }
        if (cTstatModeOff == "auto" || cTstatModeOff == "on") {
            cTstatOff.auto()
            cTstatOff.setCoolingSetpoint(coolLvl)
            cTstatOff.setHeatingSetpoint(heatLvl)
        }
        if (cTstatModeOff == "cool") {
            cTstatOff.cool()
            cTstatOff.setCoolingSetpoint(coolLvl)
        }
        if (cTstatModeOff == "heat") {
            cTstatOff.heat()
            cTstatOff.setHeatingSetpoint(heatLvl)
        }
        if (cTstatModeOff == "incr") {
            def cNewSetpoint = tempChange
            cNewSetpoint = tempChange + currentTMP
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange
            hNewSetpoint = tempChange + currentTMP
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (cTstatModeOff == "decr") {
            def cNewSetpoint = tempChange
            cNewSetpoint = currentTMP - tempChange
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange
            hNewSetpoint = currentTMP - tempChange
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (cTstatFanOff == "auto" || cTstatFanOff == "off") { cTstatOff.fanAuto() }
        if (cTstatFanOff == "on") { cTstatOff.fanOn() }
        if (cTstatFanOff == "circ") { cTstatOff.fanCirculate() }
    }
}
private thermostats1Off(evt) {
    cTstat1Off.each {deviceD ->
        def currentMode = deviceD.currentValue("thermostatMode")
        def currentTMP = deviceD.currentValue("temperature")
        if (cTstat1ModeOff == "off") { cTstat1Off.off()
                                  }
        if (cTstat1ModeOff == "auto" || cTstat1ModeOff == "on") {
            cTstat1Off.auto()
            cTstat1Off.setCoolingSetpoint(coolLvl1)
            cTstat1Off.setHeatingSetpoint(heatLvl1)
        }
        if (cTstat1ModeOff == "auto" || cTstat1ModeOff == "on") {
            cTstat1Off.auto()
            cTstat1Off.setCoolingSetpoint(coolLvl1)
            cTstat1Off.setHeatingSetpoint(heatLvl1)
        }
        if (cTstat1ModeOff == "incr") {
            def cNewSetpoint = tempChange1
            cNewSetpoint = tempChange1 + currentTMP
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange1
            hNewSetpoint = tempChange1 + currentTMP
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (cTsta1tModeOff == "decr") {
            def cNewSetpoint = tempChange1
            cNewSetpoint = currentTMP - tempChange1
            cNewSetpoint = cNewSetpoint < 60 ? 60 : cNewSetpoint > 85 ? 85 : cNewSetpoint
            def hNewSetpoint = tempChange1
            hNewSetpoint = currentTMP - tempChange1
            hNewSetpoint = hNewSetpoint < 60 ? 60 : hNewSetpoint > 85 ? 85 : hNewSetpoint
            if (currentMode == "auto" || currentMode == "on") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
            if (currentMode == "cool") {
                deviceD.setCoolingSetpoint(cNewSetpoint)
            }
            if (currentMode == "heat") {
                deviceD.setHeatingSetpoint(hNewSetPoint)
            }
        }
        if (cTstat1FanOff == "auto" || cTstat1FanOff == "off") { cTstat1Off.fanAuto() }
        if (cTstat1FanOff == "on") { cTstat1Off.fanOn() }
        if (cTstat1FanOff == "circ") { cTstat1Off.fanCirculate() }
    }
}


/******************************************************************************************************
	CONDITIONS - CERTAIN TIME RESTRICTION
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

// TIME RESTRICTIONS - ENTIRE ROUTINE
private getTimeOk(evt) {
    def result = true
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
    if(logging) log.trace "timeOk = $result"
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

/******************************************************************************************************
SPEECH AND TEXT ACTION
******************************************************************************************************/
def ttsActions(msg) {
    log.info "TTS Actions Handler activated"
    def tts = msg
    if (echoDevice) {
        log.info "echoDevice: $echoDevice activated"
        echoDevice?.setVolumeAndSpeak(eVolume, tts)
    }
    if (synthDevice) {
        synthDevice?.speak(tts)
    }
    if (tts) {
        state.sound = textToSpeech(tts instanceof List ? tts[9] : tts)
    }
    else {
        state.sound = textToSpeech("You selected the custom message option but did not enter a message in the $app.label Smart App")
    }
    if (sonosDevice){
        def currVolLevel = sonosDevice.latestValue("level")
        def currMuteOn = sonosDevice.latestValue("mute").contains("muted")
        if (currMuteOn) {
            sonosDevice.unmute()
        }
        def sVolume = settings.volume ?: 20
        sonosDevice?.playTrackAndResume(state.sound.uri, state.sound.duration, sVolume)
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

/***********************************************************************************************************************
	SMS HANDLER
***********************************************************************************************************************/
private void sendtxt(tts) {
    if (logging) log.info "Send Text method activated."
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

/******************************************************************************************************
MAIN PAGE STATUS CHECKS
******************************************************************************************************/
// SETTINGS PAGE
String settingsPageSettings() {
    if (settings?.logs || settings?.gasSafetyCheck) { return "complete" }
    return "Tap here to Configure"
}
String settingsPageComplete() {
    if (settings?.logs || settings?.gasSafetyCheck) { return "Configured!" }
    return "Tap here to Configure"
}

// CONDITIONS PAGE
String condPageSettings() {
    if (settings?.cMode || settings?.cDays) { return "complete" }
    return "Tap here to Configure"
}
String condPageComplete() {
    if (settings?.cMode || settings?.cDays) { return "Conditions have been Configured!" }
    return "Tap here to Configure"
}

// CONDITIONS FAIL PAGE
String condFailPageSettings() {
    if (blank) { return "complete" }
    return "Tap here to Configure"
}
String condFailPageComplete() {
    if (blank) { return "Configured!" }
    return "Tap here to Configure"
}

// ACTIONS PAGE
String actionsPageSettings() {
    if (actionsOnPageSettings() == "complete" || actionsOffPageSettings() == "complete" ) { return "complete" }
    return "Tap here to Configure"
}
String actionsPageComplete() {
    if (actionsOnPageSettings() == "complete" || actionsOffPageSettings() == "complete" ) { return "Actions have been Configured!" }
    return "Tap here to Configure"
}

// ACTIONS ON PAGE
String actionsOnPageSettings() {
    if (blank) { return "complete" }
    return "Tap here to Configure"
}
String actionsOnPageComplete() {
    if (blank) { return "On Actions have been Configured!" }
    return "Tap here to Configure"
}

// ACTIONS OFF PAGE
String actionsOffPageSettings() {
    if (blank) { return "complete" }
    return "Tap here to Configure"
}
String actionsOffPageComplete() {
    if (blank) { return "Off Actions have been Configured!" }
    return "Tap here to Configure"
}
// TIME RESTRICTIONS
String pTimeSettings() { 
    if (settings?.startingX || settings?.endingX) { return "complete" }
    return ""
}

String pTimeComplete() {
    if (settings?.startingX || settings?.endingX) { return "Configured" }
    return "Tap here to Configure"
}



