/* 
 * KeyPad CoOrdinator - Child app 
 ************************************ FOR INTERNAL USE ONLY ******************************************************
							
 *		7/18/2017		Version:1.0 R.0.0.1a	Initial Release
 *		6/24/2017		Version:1.0 R.0.0.1		Beta Release
 * 
 *  Copyright 2016 Jason Headley & Bobby Dobrescu
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
definition(
	name			: "KeyPad Profiles",
    namespace		: "Echo",
    author			: "JH/BD",
	description		: "Keypad Coordinator Child App",
	category		: "My Apps",
    parent			: "Echo:KeypadCoordinator",
	iconUrl			: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Keypad.png",
	iconX2Url		: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Keypad@2x.png",
	iconX3Url		: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Keypad@2x.png")
/**********************************************************************************************************************************************/
private release() {
	def text = "R.0.0.1a"
}
/**********************************************************************************************************************************************/
preferences {

    page name: "mainProfilePage"
    		page name: "pSend"          
        	page name: "pActions"
        	page name: "pConfig"
            page name: "pGroups"
        	page name: "pRestrict"
  			page name: "pDeviceControl"
            page name: "pPerson"
            page name: "pVirPerAction"
            page name: "pThermo"
}

//dynamic page methods
def mainProfilePage() {	
    dynamicPage(name: "mainProfilePage", title:"", install: true, uninstall: installed) {
        def deviceId = "${app.label}" 
        def d = getChildDevice("${app.label}")
        section ("Name Your Profile") {
            label title:"Profile Name", required:false, defaultValue: "New Profile", submitOnChange: true 
        }
        if (app.label != null) {
            section("${app.label}'s Actions ") {
                href "pActions", title: "Does ${app.label} need to execute actions?", description: pActionsComplete() , state: pActionsSettings()
            }
            section("${app.label}'s devices?") {
            	href "pDeviceControl", title: "Does ${app.label} need to control any devices?", description: pDevicesComplete() , state: pDevicesSettings()
            }
            section("${app.label}'s momentary devices") {
            	href "pMomentaryControl", title: "Does ${app.label} need to control momentary switches?", description: pMomentaryComplete() , state: pMomentarySettings()
            }
            section("${app.label}'s Presence ") {
            	href "pPresence", title: "Does ${app.label} need a Virtual Presence Sensor?", description: pPresenceComplete() , state: pPresenceSettings()
            }
            section("${app.label}'s Garage Doors ") {
                href "pGaragePage", title: "Will ${app.label} have control of the garage doors?", description: pGarageComplete() , state: pGarageSettings()
            }
			section("${app.label}'s SHM controls") {
            	href "pSHMpage", title: "Will ${app.label} have control of Smart Home Monitor?", description: pSHMComplete() , state: pSHMSettings()
            }
			section("${app.label}'s Thermostats") {
            	href "pThermo", title: "Will ${app.label} have control of the thermostats?", description: pThermoComplete() , state: pThermoSettings()
            }
            section("${app.label}'s Panic Button Settings") {
            	href "pPanic", title: "Configure the Panic Button Settings for ${app.label}", description: pPanicComplete() , state: pPanicSettings()
            }    
            section("General Keypad Settings") {
                href "pGenSettings", title: "Configure the General Settings for ${app.label}", description: pGenSetComplete() , state: pGenSetSettings()
            }
            section("General Restrictions for ${app.label}") {
                href "pRestrict", title: "Does ${app.label} need to be restricted?", description: pRestrictComplete(), state: pRestrictSettings()
            }
        }
    }   
}

/************************************************************************************************************
		Notifications pages
************************************************************************************************************/    
page name: "pVPNotifyPage"
def pVPNotifyPage() {
    dynamicPage(name: "pVPNotifyPage", title: "Notification Settings") {
        section {
            input "vpPhone", "phone", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true
            paragraph "For multiple SMS recipients, separate phone numbers with a comma"
            input "vpPush", "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true
        }
    }
}    
page name: "pShmNotifyPage"
def pShmNotifyPage() {
    dynamicPage(name: "pShmNotifyPage", title: "Notification Settings") {
        section {
            input "shmPhone", "phone", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true
            paragraph "For multiple SMS recipients, separate phone numbers with a comma"
            input(name: "notifySHMArm", title: "Notify when arming SHM", type: "bool", required: false)
            input(name: "notifySHMDisarm", title: "Notify when disarming SHM", type: "bool", required: false)
            input "shmPush", "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true
        }
    }
}        
page name: "pGarageDoorNotify"
def pGarageDoorNotify() {
    dynamicPage(name: "pGarageDoorNotify", title: "Notification Settings") {
        section {
            input(name: "g1Phone", type: "text", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true)
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"
            input(name: "g1Push", type: "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true)
            if (phone != null || garagePush) {
                input(name: "notifyG1doorOpen", title: "Notify when opening", type: "bool", required: false)
                input(name: "notifyG1doorClose", title: "Notify when closing", type: "bool", required: false)
            }
        }
    }
}
page name: "pGarageDoor2Notify"
def pGarageDoor2Notify() {
    dynamicPage(name: "pGarageDoor2Notify", title: "Notification Settings") {
        section {
            input(name: "g2Phone", type: "text", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true)
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"
            input(name: "g2Push", type: "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true)
            if (phone != null || garagePush) {
                input(name: "notifyG2doorOpen", title: "Notify when opening", type: "bool", required: false)
                input(name: "notifyG2doorClose", title: "Notify when closing", type: "bool", required: false)
            }
        }
    }
}
page name: "pGarageDoor3Notify"
def pGarageDoor3Notify() {
    dynamicPage(name: "pGarageDoor3Notify", title: "Notification Settings") {
        section {
            input(name: "g3Phone", type: "text", title: "Text This Number", description: "Phone number", required: false, submitOnChange: true)
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"
            input(name: "g3Push", type: "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true)
            if (phone != null || garagePush) {
                input(name: "notifyG3doorOpen", title: "Notify when opening", type: "bool", required: false)
                input(name: "notifyG3doorClose", title: "Notify when closing", type: "bool", required: false)
            }
        }
    }
}
/************************************************************************************************************
		Smart Home Monitor Pages
************************************************************************************************************/    
page name: "pSHMpage"
def pSHMpage() {
    dynamicPage(name: "pSHMpage", title: "SHM Configurations") {
        section {
            input "sLocksSHM","capability.lockCodes", title: "Select Keypads", required: true, multiple: true, submitOnChange: true
            input "shmCode", "number", title: "Code (4 digits)", required: false, refreshAfterSelection: true
            input "armDelay", "number", title: "Arm Delay (in seconds)", required: false
            input "keypadstatus", "bool", title: "Send status to keypads?", required: false, defaultValue: false, submitOnChange: true
            if (keypadstatus) {
                input "sLocksSHMstatus","capability.lockCodes", title: "Select Keypads to be updated by SHM", required: true, multiple: true, submitOnChange: true
            }
            href "pShmNotifyPage", title: "SHM Profile Notification Settings", description: SHMNotifyComplete(), state: SHMNotifySettings()
		}
        def hhPhrases = location.getHelloHome()?.getPhrases()*.label
        hhPhrases?.sort()
        section("Execute These Routines") {
            input "stayRoutine", "enum", title: "Execute when setting Arm-Stay", options: hhPhrases, required: false
            input "armRoutine", "enum", title: "Execute when setting Arm-Away", options: hhPhrases, required: false
            input "disarmRoutine", "enum", title: "Execute when Disarming", options: hhPhrases, required: false
        }
    }
}
/************************************************************************************************************
		Panic Button Pages
************************************************************************************************************/    
page name: "pPanic"
def pPanic() {
	dynamicPage(name: "pPanic", title: "Panic Button Controls") {
        section("Panic Button Actions") {
            input "panicKeypad","capability.lockCodes", title: "When the panic button is pressed on these keypads...", required: false, multiple: true, submitOnChange: true
        	input "panicSwitches", "capability.switch", title: "Turn on these switches", required: false, multiple: true, submitOnChange: true
            input "panicFlash", "capability.switch", title: "Flash these switches", required: false, multiple: true, submitOnChange: true
            if (panicFlash) {
                input "numFlashes", "number", title: "This number of times (default 3)", required: false, submitOnChange:true
                input "onFor", "number", title: "On for (default 1 second)", required: false, submitOnChange:true			
                input "offFor", "number", title: "Off for (default 1 second)", required: false, submitOnChange:true
            }
            input "panicText", "text", title: "Send this message", required: false
            input(name: "panicPhone", type: "text", title: "To this/these number(s)", description: "Phone number", required: false, submitOnChange: true)
            paragraph "For multiple SMS recipients, separate phone numbers with a semicolon(;)"
            input(name: "panicPush", type: "bool", title: "Send A Push Notification", description: "Notification", required: false, submitOnChange: true)
            input "panicSynthDevice", "capability.speechSynthesis", title: "on this Speech Synthesis Type Devices", multiple: true, required: false
                input "panicSonosDevice", "capability.musicPlayer", title: "on this Sonos Type Devices", required: false, multiple: true, submitOnChange: true    
                if (panicSonosDevice) {
                    input "volume", "number", title: "Temporarily change volume", description: "0-100% (default value = 30%)", required: false
                }  
            }
		}
	}    

    
/************************************************************************************************************
		Thermostat Pages
************************************************************************************************************/    
page name: "pThermo"
def pThermo() {
    dynamicPage(name: "pThermo", title: "Thermostat Controls") {
    section("") {
    	input "tempKeypad", "capability.lockCodes", title: "Enter temperature using these keypads", required: false, multiple: true, submitOnChange: true
        input "tempStat", "capability.thermostat", title: "Change the temperature on these thermostats", required: false, multiple: true, submitOnChange: true
    	}
    }
}
/************************************************************************************************************
		Garage Door Pages
************************************************************************************************************/    
page name: "pGaragePage"
def pGaragePage() {
    dynamicPage(name: "pGaragePage", title: "Garage Door Controls") {
        def hhPhrases = location.getHelloHome()?.getPhrases()*.label
        hhPhrases?.sort()
        section("") {
            input "sLocksGarage","capability.lockCodes", title: "Select the Keypads that ${app.label} can use for the garage door", required: false, multiple: true, submitOnChange: true
            input "sDoor1", "capability.garageDoorControl", title: "${app.label} can control these garage door(s)", multiple: true, required: false, submitOnChange: true
            input "doorCode1", "number", title: "Code (4 digits)", required: false, refreshAfterSelection: true
            href "pGarageDoorNotify",title: "Garage Door One Notification Settings"
            input "gd1OpenRoutines", "enum", title: "Execute this routine when this door is opened via Keypad", options: hhPhrases, required: false, submitOnChange: true
            input "gd1CloseRoutines", "enum", title: "Execute this routine when this door is closed via Keypad", options: hhPhrases, required: false, submitOnChange: true
            if (doorCode1) {
                input "sDoor2", "capability.garageDoorControl", title: "Allow These Garage Door(s)...", multiple: true, required: false, submitOnChange: true
                input "doorCode2", "number", title: "Code (4 digits)", required: false, refreshAfterSelection: true
                href "pGarageDoor2Notify",title: "Garage Door Two Notification Settings"
                input "gd2OpenRoutines", "enum", title: "Execute this routine when this door is opened via Keypad", options: hhPhrases, required: false, submitOnChange: true
                input "gd2CloseRoutines", "enum", title: "Execute this routine when this door is closed via Keypad", options: hhPhrases, required: false, submitOnChange: true
                if (doorCode2) {
                    input "sDoor3", "capability.garageDoorControl", title: "Allow These Garage Door(s)...", multiple: true, required: false, submitOnChange: true
                    input "doorCode2", "number", title: "Code (4 digits)", required: false, refreshAfterSelection: true
                    href "pGarageDoor3Notify",title: "Garage Door Notification Settings"
                    input "gd3OpenRoutines", "enum", title: "Execute this routine when this door is opened via Keypad", options: hhPhrases, required: false, submitOnChange: true
                    input "gd3CloseRoutines", "enum", title: "Execute this routine when this door is closed via Keypad", options: hhPhrases, required: false, submitOnChange: true
                }
            }
        }
    }
}    
/************************************************************************************************************
		Virtual Presence Pages
************************************************************************************************************/    
page name: "pPresence"
def pPresence() {
    dynamicPage(name: "pPresence", title: "Virtual Presence") {
    	def d = getChildDevice("${app.label}")
        section () {
        href "pPerson", title: "Create/Delete a Virtual Presence Device for ${app.label}"
        if(d) {
            input "sLocksVP","capability.lockCodes", title: "${app.label} can check-in using these keypads", required: true, multiple: true, submitOnChange: true
            input "vpCode", "number", title: "${app.label}'s check-in code (4 digits)", required: false, refreshAfterSelection: true
            href "pVPActions", title: "Perform these actions when ${app.label} arrives", description: VPActionsComplete(), state: VPActionsSettings()
            href "pVPActionsDepart", title: "Perform these actions when ${app.label} departs", description: VPActionsDComplete(), state: VPActionsDSettings()
            input "notifyVPArrive", "bool", title: "Notify me when ${app.label} Arrives", required: false, submitOnChange: true
            input "notifyVPDepart", "bool", title: "Notify me when ${app.label} Departs", required: false, submitOnChange: true
            if (notifyVPArrive || notifyVPDepart) {
                href "pVPNotifyPage", title: "${app.label}'s Notification Settings", description: VPNotifyComplete(), state: VPNotifySettings()
            	}
            }
        }
    }    
}
page name: "pPerson"
def pPerson(){
    dynamicPage(name: "pPerson", title: "", uninstall: false){
        section ("Manage the Profile Virtual Person Device", hideWhenEmpty: true){
            href "pPersonCreate", title: "Tap Here to Create the Virtual Person Device ~ '${app.label}'"
            href "pPersonDelete", title: "Tap Here to Delete the Virtual Person Device ~ '${app.label}'"
        }
    }
}
page name: "pPersonCreate"
def pPersonCreate(){
    dynamicPage(name: "pPersonCreate", title: "", uninstall: false) {
        section ("") {
            paragraph "You have created a virtual presence sensor device. You will now see this device in your 'Things' list " +
                " in the SmartThings Mobile App.  You will also see it in the 'MyDevices' tab of the IDE"
        }
        virtualPerson()
    }
}
/************************************************************************************************************
		General Settings Page
************************************************************************************************************/    
page name: "pGenSettings"
def pGenSettings() {
    dynamicPage(name: "pGenSettings", title: "General Settings") {
        section("Send Chime when contacts open") {
            input "chimeKeypad","capability.lockCodes", title: "These keypads will chime...", required: false, multiple: true, submitOnChange: true
            input "chimeContact", "capability.contactSensor", title: "...when these contacts open", required: false, multiple: true, submitOnChange: true
        }
    }
}    
/************************************************************************************************************
		Virtual Person Actions Page
************************************************************************************************************/    
page name: "pVPActions"
def pVPActions() {
    dynamicPage(name: "pVPActions", uninstall: false) {
        def routines = location.helloHome?.getPhrases()*.label 
        if (routines) {routines.sort()}
        section("${app.label}'s Virtual Person Arrives Actions ") {
            def actions = location.helloHome?.getPhrases()*.label 
            actions.sort()
                input "vpMode", "enum", title: "Choose Mode to change to...", options: location.modes.name.sort(), multiple: false, required: false 
                input "vpRoutine", "enum", title: "Select an ST Routine to execute", required: false, options: actions, multiple: false, submitOnChange: true
        }
    }
}
page name: "pVPActionsDepart"
def pVPActionsDepart() {
    dynamicPage(name: "pVPActionsDepart", uninstall: false) {
        def routines = location.helloHome?.getPhrases()*.label 
        if (routines) {routines.sort()}
        section("${app.label}'s Virtual Person Departs Actions ") {
            def actions = location.helloHome?.getPhrases()*.label 
            actions.sort()
                input "vpModeD", "enum", title: "Choose Mode to change to...", options: location.modes.name.sort(), multiple: false, required: false 
                input "vpRoutineD", "enum", title: "Select an ST Routine to execute", required: false, options: actions, multiple: false, submitOnChange: true
        }
    }
}

/************************************************************************************************************
		Profile Actions Page
************************************************************************************************************/    
page name: "pActions"
def pActions() {
    dynamicPage(name: "pActions", uninstall: false) {
        def routines = location.helloHome?.getPhrases()*.label 
        if (routines) {routines.sort()}
        section("${app.label}'s Actions ") {
            def actions = location.helloHome?.getPhrases()*.label 
            actions.sort()
            input "actionsKeypad","capability.lockCodes", title: "${app.label} can run actions using these keypads", required: true, multiple: true, submitOnChange: true
            input "actionsCode", "number", title: "${app.label}'s actions code (4 digits)", required: false, refreshAfterSelection: true
            if (actionsCode) {
                input "pMode", "enum", title: "Choose Mode to change to...", options: location.modes.name.sort(), multiple: false, required: false 
                input "pRoutine", "enum", title: "Select an ST Routine to execute", required: false, options: actions, multiple: false, submitOnChange: true
//                input "myESprofile", "enum", title: "Select an EchoSistant Profile to execute", options: parent.listEchoSistantProfiles() , multiple: false, required: false 
//                input "myPiston", "enum", title: "Select a WebCoRE Piston to execute", options:  parent.webCoRE_list('name'), multiple: false, required: false
            }
        }
    }
}
/************************************************************************************************************
		Momentary Devices on/off
************************************************************************************************************/    
page name: "pMomentaryControl"
def pMomentaryControl(evt) {
    dynamicPage(name: "pMomentaryControl", title: "",install: false, uninstall: false) {
        section ("Momentary Switches", hideWhenEmpty: true) {
            input "mKeypad","capability.lockCodes", title: "Select Keypads", required: true, multiple: true, submitOnChange: true
            input "mCode", "number", title: "Code (4 digits)", required: false, refreshAfterSelection: true
        	input "mSwitches", "capability.switch", title: "Select devices to turn on", multiple: true, required: false, submitOnChange: true
            if (mSwitches) {
            	input "mOff", "number", title: "Turn off after this many seconds", required: true, submitOnChange: true
        	}
        }    
	}
}    
/************************************************************************************************************
		Devices and Groups Control Pages
************************************************************************************************************/    
page name: "pDeviceControl"
def pDeviceControl(evt) {
    dynamicPage(name: "pDeviceControl", title: "",install: false, uninstall: false) {
        section ("Control Keypads") {
        	input "switchKeypad", "capability.lockCodes", title: "Use these keypads for device control"
        }
//        section ("Momentary Switches", hideWhenEmpty: true) {
//        	href "pMomentaryControl", title: "Select devices to turn on momentarily", description: pMomentaryComplete() , state: pMomentarySettings()
//            }    
        section ("Switches On/Off", hideWhenEmpty: true) {
            paragraph "Pressing the 'ON' button will turn these devices on."
            paragraph "Pressing the 'PARTIAL' button will turn these devices off."
            input "bSwitches", "capability.switch", title: "Select devices to turn on/off", multiple: true, required: false, submitOnChange: true
        }
        section ("Switches Toggle", hideWhenEmpty: true) {
        	paragraph "Pressing the 'ON' button toggles these switches:"
            input "tSwitches", "capability.switch", title: "Select devices to toggle", multiple: true, required: false, submitOnChange: true
		}
        section ("Switches Flash", hideWhenEmpty: true) {
        	paragraph "Pressing the 'ON' button flashes these switches:"
            input "fSwitches", "capability.switch", title: "Select devices to flash", multiple: true, required: false, submitOnChange: true
            if (fSwitches) {
                input "numFlashes", "number", title: "This number of times (default 3)", required: false, submitOnChange:true
                input "onFor", "number", title: "On for (default 1 second)", required: false, submitOnChange:true			
                input "offFor", "number", title: "Off for (default 1 second)", required: false, submitOnChange:true
            }
		}
    }
}    
/************************************************************************************************************
		Restrictions Page
************************************************************************************************************/    
page name: "pRestrict"
def pRestrict(){
    dynamicPage(name: "pRestrict", title: "", uninstall: false) {
        section ("Mode Restrictions") {
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }        
        section ("Days - Audio only on these days"){	
            input "days", title: "Only on certain days of the week", multiple: true, required: false, submitOnChange: true,
                "enum", options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
        }
        section ("Time - Audio only during these times"){
            href "certainTime", title: "Only during a certain time", description: pTimeComplete() , state: pTimeSettings()
        }   
    }
}
page name: "certainTime"
def certainTime() {
    dynamicPage(name:"certainTime",title: "Only during a certain time", uninstall: false) {
        section("Beginning at....") {
            input "startingX", "enum", title: "Starting at...", options: ["A specific time", "Sunrise", "Sunset"], required: false , submitOnChange: true
            if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false, submitOnChange: true
            else {
                if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
        section("Ending at....") {
            input "endingX", "enum", title: "Ending at...", options: ["A specific time", "Sunrise", "Sunset"], required: false, submitOnChange: true
            if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false, submitOnChange: true
            else {
                if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false, submitOnChange: true
                    }
        }
    }
}
/************************************************************************************************************
		Base Process
************************************************************************************************************/    
def installed() {
    log.debug "Installed with settings: ${settings}, current app version: ${release()}"
    state.ProfileRelease ="Profile: "  + release()
}

def updated() {
    log.debug "Updated with settings: ${settings}, current app version: ${release()}"
    state.ProfileRelease = "Profile: " + release()
    unsubscribe()
    initialize()
}

def initialize() {
	if (sLocksVP) { subscribe(sLocksVP, "codeEntered", codeEntryHandler) }
    if (sLocksGarage) { subscribe(sLocksGarage, "codeEntered", codeEntryHandler) }
    if (tempKeypad) { subscribe(tempKeypad, "codeEntered", tempHandler) }
    if (chimeContact) { subscribe (chimeContact, "contact.open", chimeHandler) }
    if (panicKeypad) { subscribe (panicKeypad, "contact.open", panicHandler) }
    if (actionsKeypad) { subscribe (actionsKeypad, "codeEntered", codeEntryHandler) }
    if (switchKeypad) { subscribe (switchKeypad, "codeEntered", codeEntryHandler) }
	if (panicKeypad) { subscribe (panicKeypad, "button.pushed", panicButtonHandler) }
    if (mKeypad) { subscribe (mKeypad, "codeEntered", codeEntryHandler) }
//initialize keypad to correct state
}


def codeEntryHandler(evt) {
    def codeEntered = evt.value as String
    def data = evt.data as String
    def stamp = state.lastTime = new Date(now()).format("h:mm aa, dd-MMMM-yyyy", location.timeZone) 
    def armMode = ''
    if (data == '0') armMode = 'off'
    else if (data == '3') armMode = 'away'
    else if (data == '1') armMode = 'stay'
    else if (data == '2') armMode = 'stay'	//Currently no separate night mode for SHM, set to 'stay'
    
    if ("${codeEntered}" == "${vpCode}") {
        pVirToggle(data, codeEntered, evt)
        if (vpMode || vpModeD || vpRoutine || vpRoutineD) {
        	vpAction(data, codeEntered, evt)
        }    
    }
    if ("${codeEntered}" == "${doorCode1}" ||"${codeEntered}" == "${doorCode2}" || "${codeEntered}" == "${doorCode3}") {
        if (getDayOk()==true && getModeOk()==true && getTimeOk()==true) {
        pGarage(data, codeEntered, evt)
    	}
    }
    if ("${codeEntered}" == "0000") {
    	if (getDayOk()==true && getModeOk()==true && getTimeOk()==true) {
        deviceControl(data) 
    	}
    }    
   	if ("${codeEntered}" == "${mCode}") {
   		if (getDayOk()==true && getModeOk()==true && getTimeOk()==true) {
        momentaryDeviceHandler(data, codeEntered, evt, mOff)
    	}
    }    
    if ("${codeEntered}" == "${shmCode}") {
        pSHM(data, codeEntered, evt, armMode, armDelay)
    }
    if ("${codeEntered}" == "${actionsCode}" && data == "3") {
        if (getDayOk()==true && getModeOk()==true && getTimeOk()==true) { 
        takeAction(data, codeEntered, evt)
    }
    }
}
/************************************************************************************************************
		Virtual Person Actions Handler
************************************************************************************************************/    
private vpAction(data, codeEntered, evt) {
    if(vpRoutine && data == "3") {
        location.helloHome?.execute(vpRoutine)
	    }
	if (vpMode && data == "3") {
		setLocationMode(vpMode)
		}        
    if(vpRoutineD && data == "0") {
        location.helloHome?.execute(vpRoutineD)
	    }
	if (vpModeD && data == "0") {
		setLocationMode(vpModeD)
		}        
	}
/***********************************************************************************************************************
    TAKE ACTIONS HANDLER
***********************************************************************************************************************/
private takeAction(data, codeEntered, evt) {
	if(parent.debug) log.warn "version number = ${release()}"
	sendLocationEvent(name: "KeypadCoordinator", value: app.label, data: data, displayed: true, isStateChange: true, descriptionText: "KeypadCoordinator ${app.label} Profile was active")
    if(pRoutine) {
    	log.info "executing smartthings routine ${pRoutine}"
        location.helloHome?.execute(settings.pRoutine)
	    }
	if (pMode) {
		setLocationMode(pMode)
	}        
}
    
/************************************************************************************************************
		Virtual Person Check In/Out Automatically Handler
************************************************************************************************************/    
private pVirToggle(data, codeEntered, evt) {
    def stamp = state.lastTime = new Date(now()).format("h:mm aa, dd-MMMM-yyyy", location.timeZone) 
    def message = ""
	def vp = getChildDevice("${app.label}")
     if(vp) {
     if (data == "3") {
     //if (vp?.currentValue('presence').contains('not')) {
            vp.arrived()
            message = "${app.label} arrived via ${evt.displayName} at ${stamp}"
            if (notifyVPArrive) { sendVPtxt(message) }
            }
        if (data == "0") {
        //else if (vp?.currentValue('presence').contains('present')) {
            vp.departed()
            message = "${app.label} departed via ${evt.displayName} at ${stamp}"
            if (notifyVPDepart) { sendVPtxt(message) }
            }
    	}
    if (parent.debug) { log.info "${message}" }
}
/************************************************************************************************************
		Garage Door Handler
************************************************************************************************************/    
private pGarage(data, codeEntered, evt) {
    def stamp = state.lastTime = new Date(now()).format("h:mm aa, dd-MMMM-yyyy", location.timeZone) 
    def message = ""
    if ("${data}" == "0") {
        if ("${codeEntered}" == "${doorCode1}") {
            if (sDoor1 != null) {
                sDoor1.close()
                location.helloHome?.execute(gd1CloseRoutines)
                message = "The ${sDoor1} was closed by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG1txt(message)
            }
        }    
        if ("${codeEntered}" == "${doorCode2}") {
            if (sDoor2 != null) {
                sDoor2.close() 
                location.helloHome?.execute(gd2CloseRoutines)
                message = "The ${sDoor2} was closed by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG2txt(message)
            }
        }
        if ("${codeEntered}" == "${doorCode3}") {            
            if (sDoor3 != null) {
                sDoor3.close() 
                location.helloHome?.execute(gd3CloseRoutines)
                message = "The ${sDoor3} was closed by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG3txt(message)
            }
        }
        log.info "${message}"
    }    
    if ("${data}" == "3") {
        if ("${codeEntered}" == "${doorCode1}") {            
            if (sDoor1 != null) {
                sDoor1.open() 
                location.helloHome?.execute(gd1OpenRoutines)
                message = "The ${sDoor1} was opened by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG1txt(message)
            }
        }
        if ("${codeEntered}" == "${doorCode2}") {            
            if (sDoor2 != null) {
                sDoor2.open() 
                location.helloHome?.execute(gd2OpenRoutines)
                message = "The ${sDoor2} was opened by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG2txt(message)
            }
        }
        if ("${codeEntered}" == "${doorCode3}") {            
            if (sDoor3 != null) {
                sDoor3.open() 
                location.helloHome?.execute(gd3OpenRoutines)
                message = "The ${sDoor3} was opened by ${app.label} using the ${evt.displayName} at ${stamp}"
                sendG3txt(message)
            }
        }
        log.info "${message}"
    }     
}
/************************************************************************************************************
		Momentary Devices Handler
************************************************************************************************************/    
def momentaryDeviceHandler(data, codeEntered, evt, mOff) {
	if (data == "3") {
    	mSwitches.on()
        runIn(mOff, "turnOff")
        }
    }
def turnOff() {
	mSwitches.off()
    }
/************************************************************************************************************
		Smart Home Monitor Handler
************************************************************************************************************/    
private pSHM(data, codeEntered, evt, armMode, armDelay) {
	def stamp = state.lastTime = new Date(now()).format("h:mm aa, dd-MMMM-yyyy", location.timeZone)
    def message = ""
    if (data == "0") {
        sLocksSHM?.each() { it.acknowledgeArmRequest(0) } 
        message = "${app.label} disarmed SHM using ${evt.displayName} at ${stamp} "
    	sendDisarmCommand()
    		    if (notifySHMArm) {
    				sendSHMtxt(message)
			}
        }
    else if (data == "1") {
        sLocksSHM?.each() { it.acknowledgeArmRequest(1) }
        message = "${app.label} set SHM to Armed-Stay using ${evt.displayName} at ${stamp} "
    	sendStayCommand()
    		    if (notifySHMArm) {
    				sendSHMtxt(message)
			}
        }
    else if (data == "3") {
        if (armDelay != null || armDelay > 0) {
        	sLocksSHMstatus?.setExitDelay(armDelay) 
        	runIn(armDelay, "sendArmAwayCommand")
            message = "${app.label} set SHM to Armed-Away using ${evt.displayName} at ${stamp} with an exit delay of ${armDelay} seconds "
    		    if (notifySHMArm) {
    				sendSHMtxt(message)
			}
		}
        else {
        	sendArmAwayCommand() {
        	message = "${app.label} set SHM to Armed-Away using ${evt.displayName} at ${stamp} "
    		    if (notifySHMArm) {
    				sendSHMtxt(message)
				}
        	}
        }
    }    
    log.info "${message}"
}    
def sendArmAwayCommand() {
    log.debug "Sending Armed-Away Command."
    sendSHMEvent("away")
    sLocksSHM?.each() { it.acknowledgeArmRequest(3) }
    sLocksSHMstatus?.each() { it.setArmedAway(armDelay) }
    location.helloHome?.execute(armRoutine)
}
def sendStayCommand() {
    log.debug "Sending Armed-Stay Command."
    sendSHMEvent("stay")
    sLocksSHMstatus?.each() { it.setArmedStay() }
    location.helloHome?.execute(stayRoutine)
}
def sendDisarmCommand() {
    log.debug "Sending Disarm Command."
    sendSHMEvent("off")
    sLocksSHMstatus?.each() { it.setDisarmed() }
    location.helloHome?.execute(disarmRoutine)
}
private sendSHMEvent(String shmState){
	def event = [name:"alarmSystemStatus", value: shmState, 
    			displayed: true, description: "System Status is ${shmState}"]
    sendLocationEvent(event)
}

/***********************************************************************************************************************
    RESTRICTIONS HANDLER  if (getDayOk()==true && getModeOk()==true && getTimeOk()==true && getFrequencyOk()==true && getConditionOk()==true) {
***********************************************************************************************************************/
private getAllOk() {
    modeOk && daysOk && timeOk
}
private getModeOk() {
    def result = !modes || modes?.contains(location.mode)
    if(parent.debug) log.debug "modeOk = $result"
    result
} 
private getDayOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    if(parent.debug) log.debug "daysOk = $result"
    result
}
private getTimeOk() {
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
            if (parent.debug) log.trace "getTimeOk = $result."
            }
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
    def result = ""
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
    SMS HANDLER
***********************************************************************************************************************/
private void sendPanicText(panicText) {
    if (parent.debug) log.debug "Request to send sms received with message: '${panicText}'"
    if (sendContactText) { 
        sendNotificationToContacts(panicText, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (panicPush) { 
        sendPushMessage(panicText)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (panicPhone) {
        sendPanicText(sms, panicText)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendSHMtxt(message) {
    if (parent.debug) log.debug "Request to send sms received with message: '${message}'"
    if (sendContactText) { 
        sendNotificationToContacts(message, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (shmPush) { 
        sendPushMessage(message)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (shmPhone) {
        sendSHMText(sms, message)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendVPtxt(message) {
    if (parent.debug) log.debug "Request to send sms received with message: '${message}'"
    if (sendContactText) { 
        sendNotificationToContacts(message, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (vpPush) { 
        sendPushMessage(message)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (vpPhone) {
        sendVPText(sms, message)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendG1txt(message) {
    if (parent.debug) log.debug "Request to send sms received with message: '${message}'"
    if (sendContactText) { 
        sendNotificationToContacts(message, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (g1Push) { 
        sendPushMessage(message)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (g1Phone) {
        sendG1Text(sms, message)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendG2txt(message) {
    if (parent.debug) log.debug "Request to send sms received with message: '${message}'"
    if (sendContactText) { 
        sendNotificationToContacts(message, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (g2Push) { 
        sendPushMessage(message)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (g2Phone) {
        sendG2Text(sms, message)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendG3txt(message) {
    if (parent.debug) log.debug "Request to send sms received with message: '${message}'"
    if (sendContactText) { 
        sendNotificationToContacts(message, recipients)
        if (parent.debug) log.debug "Sending sms to selected reipients"
    } 
    if (g3Push) { 
        sendPushMessage(message)
        if (parent.debug) log.debug "Sending push message to selected reipients"
    }
    if (g3Phone) {
        sendG3Text(sms, message)
        if (parent.debug) log.debug "Processing message for selected phones"
    }
}
private void sendPanicText(number, message) {
    if (panicPhone) {
        def phones = panicPhone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}
private void sendSHMText(number, message) {
    if (shmPhone) {
        def phones = shmPhone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}
private void sendVPText(number, message) {
    if (vpPhone) {
        def phones = vpPhone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}
private void sendG1Text(number, message) {
    if (g1Phone) {
        def phones = g1Phone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}    
private void sendG2Text(number, message) {
    if (g2Phone) {
        def phones = g2Phone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}
private void sendG3Text(number, message) {
    if (g3Phone) {
        def phones = g3Phone.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (parent.debug) log.debug "Sending sms to selected phones"
        }
    }
}
/************************************************************************************************************
  Device Control Handlers for press of 'ON' and 'PARTIAL' buttons
************************************************************************************************************/
def deviceControl(data) {  //// Turns switches on and off
    if (data == "3") {
        bSwitches?.on()
        toggle()
        flashLights()
    }
    if (data == "1") {
        bSwitches?.off()
    }
} 
private toggle() {  //// Toggles switches
    if (tSwitches) {
        if (tSwitches?.currentValue('switch').contains('on')) {
            tSwitches?.off()
        }
        else if (tSwitches?.currentValue('switch').contains('off')) {
            tSwitches?.on()
        }
    }
}
private flashLights() {  //// Flashes switches
    if (parent.debug) log.debug "The Flash Switches Option has been activated"
    def doFlash = true
    def onFor = onFor ?: 60000/60
    def offFor = offFor ?: 60000/60
    def numFlashes = numFlashes ?: 3

    if (state.lastActivated) {
        def elapsed = now() - state.lastActivated
        def sequenceTime = (numFlashes + 1) * (onFor + offFor)
        doFlash = elapsed > sequenceTime
    }
    if (doFlash) {
        state.lastActivated = now()
        def initialActionOn = fSwitches.collect{it.currentflashSwitch != "on"}
        def delay = 0L

        numFlashes.times {
            fSwitches.eachWithIndex {s, i ->
                if (initialActionOn[i]) {
                    s.on(delay: delay)
                }
                else {
                    s.off(delay:delay)                   
                } 
            }
            delay += onFor
            fSwitches.eachWithIndex {s, i ->
                if (initialActionOn[i]) {
                    s.off(delay: delay)
                }
                else {
                    s.on(delay:delay)
                }
            }
            delay += offFor
        }
    }
}

/************************************************************************************************************
   Thermostat Handler
************************************************************************************************************/
def tempHandler(evt) {
    def codeEntered = evt.value as String
    if ("${codeEntered}".startsWith("01")) {
        def newSetPoint = codeEntered
        newSetPoint = newSetPoint.replaceAll(/01/,"")
        log.info "Changing the cooling to ${newSetPoint} degrees"
        tempStat.setCoolingSetpoint("${newSetPoint}")
    }
    if (codeEntered.startsWith("02")) {
        def newSetPoint = codeEntered
        newSetPoint = newSetPoint.replaceAll(/02/,"")
        log.info "Changing the heat to ${newSetPoint} degrees"
        tempStat.setHeatingSetpoint("${newSetPoint}")
    }
}
/************************************************************************************************************
   Panic Button Handler
************************************************************************************************************/
def panicButtonHandler(evt) {
	def event = evt.data
    def eVal = evt.value
    def eName = evt.name
    def eDev = evt.device
	def stamp = state.lastTime = new Date(now()).format("h:mm aa, dd-MMMM-yyyy", location.timeZone)
    log.info "The panic button was pressed on the ${evt.displayName} at " + stamp
		def buttonNumUsed = evt.data.replaceAll("\\D+","")
        buttonNumUsed = buttonNumUsed.toInteger()
       	int butNum = buttonNumUsed 
    if (panicSwitches) { panicSwitches.on() }
    if (panicFlash) { panicFlasher() }
	if (panicText) { sendPanicText(panicText) }
    if (panicSynthDevice) { panicTTS(panicText) }
    if (panicSonosDevice) { panicTTS(panicText) }
}

private panicFlasher() {  //// Flashes switches
    if (parent.debug) log.debug "The Panic Button Flash Switches Option has been activated"
    def doFlash = true
    def onFor = onFor ?: 60000/60
    def offFor = offFor ?: 60000/60
    def numFlashes = numFlashes ?: 3

    if (state.lastActivated) {
        def elapsed = now() - state.lastActivated
        def sequenceTime = (numFlashes + 1) * (onFor + offFor)
        doFlash = elapsed > sequenceTime
    }
    if (doFlash) {
        state.lastActivated = now()
        def initialActionOn = panicFlash.collect{it.currentflashSwitch != "on"}
        def delay = 0L

        numFlashes.times {
            panicFlash.eachWithIndex {s, i ->
                if (initialActionOn[i]) {
                    s.on(delay: delay)
                }
                else {
                    s.off(delay:delay)                   
                } 
            }
            delay += onFor
            panicFlash.eachWithIndex {s, i ->
                if (initialActionOn[i]) {
                    s.off(delay: delay)
                }
                else {
                    s.on(delay:delay)
                }
            }
            delay += offFor
        }
    }
}
def panicTTS(panicText) {
	def tts = "Attention, Attention, This is an emergency: ${panicText}"
    if (panicSynthDevice) {
        panicSynthDevice?.speak(tts) 
        if (parent.debug) log.debug "Sending message to Synthesis Devices"
    }
    if (tts) {
        state.sound = textToSpeech(tts instanceof List ? tts[0] : tts)
    }
    else {
        state.sound = textToSpeech("You selected the custom message option but did not enter a message in the $app.label Smart App")
        if (parent.debug) log.debug "You selected the custom message option but did not enter a message"
    }
    if (panicSonosDevice){ // 2/22/2017 updated Sono handling when speaker is muted
        def currVolLevel = panicSonosDevice.latestValue("level")
        def currMuteOn = panicSonosDevice.latestValue("mute").contains("muted")
        if (parent.debug) log.debug "currVolSwitchOff = ${currVolSwitchOff}, vol level = ${currVolLevel}, currMuteOn = ${currMuteOn} "
        if (currMuteOn) { 
            if (parent.debug) log.warn "speaker is on mute, sending unmute command"
            panicSonosDevice.unmute()
        }
        def sVolume = settings.volume ?: 20
        panicSonosDevice?.playTrackAndResume(state.sound.uri, state.sound.duration, sVolume)
        if (parent.debug) log.info "Playing message on the music player '${panicSonosDevice}' at volume '${volume}'" 
    }
}
/************************************************************************************************************
   Contact Chime Handler
************************************************************************************************************/
def chimeHandler(evt) {
    if (getDayOk()==true && getModeOk()==true && getTimeOk()==true) {
    chimeKeypad.beep()
	}
}
/************************************************************************************************************
  Virtual Presence Handlers
************************************************************************************************************/
//// CREATE VIRTUAL PRESENCE
def virtualPerson() {
    log.trace "Creating Virtual Presence Device for Keypad Coordinator"
    def deviceId = "${app.label}" 
    def d = getChildDevice("${app.label}")
    if(!d) {
        d = addChildDevice("EchoSistant", "EchoSistant Simulated Presence Sensor", deviceId, null, [label:"${app.label}"])
        log.trace "Keypad Coordinator Virtual Presence Device - Created ${app.label} "
    }
    else {
        log.trace "NOTICE!!! Found that the EVPD ${d.displayName} already exists. Only one device per profile permitted"
    }
}  

//// DELETE VIRTUAL PRESENCE
page name: "pPersonDelete"
def pPersonDelete(){
    dynamicPage(name: "pPersonDelete", title: "", uninstall: false) {
        section ("") {
            paragraph "You have deleted a virtual presence sensor device. You will no longer see this device in your " +
                " SmartThings Mobile App.  "
        }
        removeChildDevices(getAllChildDevices())
    }
}
private removeChildDevices(delete) {
    log.debug "The Virtual Person Device '${app.label}' has been deleted from your SmartThings environment"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}               
/************************************************************************************************************
CoRE Integration   pistonIdOrName,Map data=[:]
************************************************************************************************************/
public  webCoRE_execute(myPiston){def i=(state.webCoRE?.pistons?:[]).find{(it.name==pistonIdOrName)||(it.id==pistonIdOrName)}?.id;if(i){sendLocationEvent([name:i,value:app.label,isStateChange:true,displayed:false,data:data])}
log.info "piston executed"}
public  webCoRE_list(mode){def p=state.webCoRE?.pistons;if(p)p.collect{mode=='id'?it.id:(mode=='name'?it.name:[id:it.id,name:it.name])}}
public  webCoRE_handler(evt){switch(evt.value){case 'pistonList':List p=state.webCoRE?.pistons?:[];Map d=evt.jsonData?:[:];if(d.id&&d.pistons&&(d.pistons instanceof List)){p.removeAll{it.iid==d.id};p+=d.pistons.collect{[iid:d.id]+it}.sort{it.name};state.webCoRE = [updated:now(),pistons:p];};break;case 'pistonExecuted':def cbk=state.webCoRE?.cbk;if(cbk&&evt.jsonData)"$cbk"(evt.jsonData);break;}}

/************************************************************************************************************
   Page status and descriptions 
************************************************************************************************************/       
def pActionsSettings(){def result = ""
                       def pDevicesProc = ""
                       if (pMode || pRoutine) {
                           result = "complete"
                           pDevicesProc = "complete"}
                       result}
def pActionsComplete() {def text = "Configured" 
                        def pDevicesComplete = pDevicesComplete()
                        if (pMode || pRoutine) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}        
def pDevicesSettings() {def result = ""
                        if (bSwitches || tSwitches || fSwitches || mSwitches) {
                            result = "complete"}
                        result}
def pDevicesComplete() {def text = "Tap here to Configure" 
                        if (bSwitches || tSwitches || fSwitches || mSwitches) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pMomentarySettings() {def result = ""
                        if (mSwitches) {
                            result = "complete"}
                        result}
def pMomentaryComplete() {def text = "Tap here to Configure" 
                        if (mSwitches) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pPresenceSettings() {def result = ""
                        if (sLocksVP) {
                            result = "complete"}
                        result}
def pPresenceComplete() {def text = "Tap here to Configure" 
                        if (sLocksVP) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pGarageSettings() {def result = ""
                        if (sDoor1) {
                            result = "complete"}
                        result}
def pGarageComplete() {def text = "Tap here to Configure" 
                        if (sDoor1) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pSHMSettings() {def result = ""
                        if (sLocksSHM || SHMPhone || SHMPush) {
                            result = "complete"}
                        result}
def pSHMComplete() {def text = "Tap here to Configure" 
                        if (sLocksSHM || SHMPhone || SHMPush) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pThermoSettings() {def result = ""
                        if (tempKeypad) {
                            result = "complete"}
                        result}
def pThermoComplete() {def text = "Tap here to Configure" 
                        if (tempKeypad) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pPanicSettings() {def result = ""
                        if (panicKeypad) {
                            result = "complete"}
                        result}
def pPanicComplete() {def text = "Tap here to Configure" 
                        if (panicKeypad) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pGenSetSettings() {def result = ""
                        if (chimeKeypad) {
                            result = "complete"}
                        result}
def pGenSetComplete() {def text = "Tap here to Configure" 
                        if (chimeKeypad) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}
def pRestrictSettings() {def result = "" 
                        if (modes || days || startingX || endingX) {
                            result = "complete"}
                        result}
def pRestrictComplete() {def text = "Tap here to configure" 
                         if (modes || days ||startingX || endingX) {
                             text = "Configured"}
                         else text = "Tap here to Configure"
                         text}
def pTimeSettings() {def result = "" 
                        if (startingX || endingX) {
                            result = "complete"}
                        result}
def pTimeComplete() {def text = "Tap to set" 
                         if (startingX || endingX) {
                             text = "Time Set"}
                         else text = "Tap to set"
                         text}
def SHMNotifySettings() {def result = "" 
                        if (shmPhone || shmPush) {
                            result = "complete"}
                        result}
def SHMNotifyComplete() {def text = "Tap here to configuret" 
                         if (shmPhone || shmPush) {
                             text = "Configured"}
                         else text = "Tap here to Configure"
                         text}
def VPNotifySettings() {def result = "" 
                        if (vpPhone || vpPush) {
                            result = "complete"}
                        result}
def VPNotifyComplete() {def text = "Tap here to configuret" 
                         if (vpPhone || vpPush) {
                             text = "Configured"}
                         else text = "Tap here to Configure"
                         text}
def VPActionsSettings(){def result = ""
                       if (vpMode || vpRoutine) {
                           result = "complete"}
                       result}
def VPActionsComplete() {def text = "Configured" 
                        if (vpMode || vpRoutine) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}        
def VPActionsDSettings(){def result = ""
                       if (vpModeD || vpRoutineD) {
                           result = "complete"}
                       result}
def VPActionsDComplete() {def text = "Configured" 
                        if (vpModeD || vpRoutineD) {
                            text = "Configured"}
                        else text = "Tap here to Configure"
                        text}        
