/**
 *  BIG TALKER -- Version 1.0.3-Alpha8 -- A SmartApp for SmartThings Home Automation System
 *  Copyright 2014 - rayzur@rayzurbock.com - Brian S. Lowrance
 *  For the latest version, development and test releases visit http://www.github.com/rayzurbock
 *
 *  This SmartApp is free. Donations to support development efforts are accepted via: 
 *      -- Paypal at: rayzur@rayzurbock.com
 *      -- Paypal Donation (for supporters without a Paypal account): https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=WKB9N9MPUGTZS
 *      -- Square Marketplace at: https://squareup.com/market/brian-lowrance#category-a58f6ff3-7380-471b-8432-7e5881654e2c
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *  If modifying this project, please keep the above header in tact.
 *
 *  Feature Requests and Change Log have been moved to the bottom of the source.
 */
 
definition(
    name: "Big Talker Dev",
    namespace: "rayzurbock",
    author: "rayzur@rayzurbock.com",
    description: "Let's talk about mode changes, switches, motions, and so on.",
    category: "Fun & Social",
    iconUrl: "http://rayzurbock.com/ST/icons/BigTalker.png",
    iconX2Url: "http://rayzurbock.com/ST/icons/BigTalker@2x.png",
    iconX3Url: "http://rayzurbock.com/ST/icons/BigTalker@2x.png")


preferences {
    page(name: "pageStart")
    page(name: "pageStatus")
    page(name: "pageConfigure")
    page(name: "pageConfigMotion")
    page(name: "pageConfigSwitch")
    page(name: "pageConfigPresence")
    page(name: "pageConfigLock")
    page(name: "pageConfigContact")
    page(name: "pageConfigMode")
    page(name: "pageConfigThermostat")
	page(name: "pageConfigAcceleration")
    page(name: "pageConfigWater")
    page(name: "pageConfigSmoke")
    page(name: "pageConfigButton")
//End preferences
}

def pageStart(){
    dynamicPage(name: "pageStart", title: "Big Talker"){
        section(){
            href "pageStatus", title:"Status", description:"Tap to view status"
            href "pageConfigure", title:"Configure", description:"Tap to configure"
        }
        section(){
            paragraph "Big Talker is a SmartApp that can make your house talk depending on various triggered events."
            paragraph "Pair with any SmartThings compatible speech synthesis audio device such as Sonos, VLC Thing on your computer or Raspberry Pi!\n"
        }
        section(){
            if (!(state.appversion == null)){ 
                paragraph "Big Talker ${state.appversion}\nhttp://www.github.com/rayzurbock\n" 
            } else {
                paragraph "Big Talker \nhttp://www.github.com/rayzurbock\n" 
            }
        }
    }
}

def pageStatus(){
    //dynamicPage(name: "pageStatus", title: "Big Talker is configured as follows:", nextPage: "pageConfigure"){
    dynamicPage(name: "pageStatus", title: "Big Talker is configured as follows:"){
        String enabledDevices = ""
        
        //BEGIN STATUS DEFAULTS
        enabledDevices = "Default Speech Devices:\n"
        enabledDevices += "   "
        settings.speechDeviceDefault.each(){
            enabledDevices += "${it.displayName},"
        }
        enabledDevices += "\n\n"
        if (settings.speechVolume) {
            enabledDevices += "Adjust Volume To: ${settings.speechVolume}%\n\n"
        }
        enabledDevices += "Default Modes:\n"
        enabledDevices += "   "
        settings.speechModesDefault.each(){
            enabledDevices += "${it},"
        }
        section ("Defaults:"){
            paragraph enabledDevices
        }
        enabledDevices = ""
        //END STATUS DEFAULTS
  
        //BEGIN STATUS CONFIG MOTION GROUP 1
        if (settings.motionDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.motionDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n"
            if (settings.motionTalkActive1) {
                enabledDevices += "Say on active:\n ${settings.motionTalkActive1}\n\n"
            }
            if (settings.motionTalkInactive1) {
                enabledDevices += "Say on inactive:\n ${settings.motionTalkInactive1}\n\n"
            }
            if (settings.motionSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.motionSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.motionModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.motionModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Motion Sensor Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG MOTION GROUP 1
        //BEGIN STATUS CONFIG MOTION GROUP 2
        if (settings.motionDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.motionDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n"
            if (settings.motionTalkActive2) {
                enabledDevices += "Say on active:\n ${settings.motionTalkActive2}\n\n"
            }
            if (settings.motionTalkInactive2) {
                enabledDevices += "Say on inactive:\n ${settings.motionTalkInactive2}\n\n"
            }
            if (settings.motionSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.motionSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.motionModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.motionModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Motion Sensor Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG MOTION GROUP 2
        //BEGIN STATUS CONFIG MOTION GROUP 3
        if (settings.motionDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.motionDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n"
            if (settings.motionTalkActive3) {
                enabledDevices += "Say on active:\n ${settings.motionTalkActive3}\n\n"
            }
            if (settings.motionTalkInactive3) {
                enabledDevices += "Say on inactive:\n ${settings.motionTalkInactive3}\n\n"
            }
            if (settings.motionSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.motionSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.motionModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.motionModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Motion Sensor Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG MOTION GROUP 3
        
        //BEGIN STATUS CONFIG SWITCH GROUP 1
        if (settings.switchDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.switchDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.switchTalkOn1) {
                enabledDevices += "Say when switched ON:\n ${settings.switchTalkOn1}\n\n"
            }
            if (settings.switchTalkOff1) {
                enabledDevices += "Say when switched OFF:\n ${settings.switchTalkOff1}\n\n"
            }
            if (settings.switchSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.switchSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.switchModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.switchModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Switch Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SWITCH GROUP 1
        //BEGIN STATUS CONFIG SWITCH GROUP 2
        if (settings.switchDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.switchDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.switchTalkOn2) {
                enabledDevices += "Say when switched ON:\n ${settings.switchTalkOn2}\n\n"
            }
            if (settings.switchTalkOff1) {
                enabledDevices += "Say when switched OFF:\n ${settings.switchTalkOff2}\n\n"
            }
            if (settings.switchSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.switchSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.switchModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.switchModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Switch Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SWITCH GROUP 2
        //BEGIN STATUS CONFIG SWITCH GROUP 3
        if (settings.switchDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.switchDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.switchTalkOn3) {
                enabledDevices += "Say when switched ON:\n ${settings.switchTalkOn3}\n\n"
            }
            if (settings.switchTalkOff3) {
                enabledDevices += "Say when switched OFF:\n ${settings.switchTalkOff3}\n\n"
            }
            if (settings.switchSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.switchSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.switchModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.switchModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Switch Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SWITCH GROUP 3
        
        //BEGIN STATUS CONFIG PRESENCE GROUP 1
        if (settings.presDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.presDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.presTalkOnArrive1) {
                enabledDevices += "Say on arrive:\n ${settings.presTalkOnArrive1}\n\n"
            }
            if (settings.presTalkOnLeave1) {
                enabledDevices += "Say on leave:\n ${settings.presTalkOnLeave1}\n\n"
            }
            if (settings.presSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.presSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.presModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.presModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Presence Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG PRESENCE GROUP 1
        //BEGIN STATUS CONFIG PRESENCE GROUP 2
        if (settings.presDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.presDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.presTalkOnArrive2) {
                enabledDevices += "Say on arrive:\n ${settings.presTalkOnArrive2}\n\n"
            }
            if (settings.presTalkOnLeave2) {
                enabledDevices += "Say on leave:\n ${settings.presTalkOnLeave2}\n\n"
            }
            if (settings.presSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.presSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.presModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.presModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Presence Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG PRESENCE GROUP 2
        //BEGIN STATUS CONFIG PRESENCE GROUP 3
        if (settings.presDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.presDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.presTalkOnArrive3) {
                enabledDevices += "Say on arrive:\n ${settings.presTalkOnArrive3}\n\n"
            }
            if (settings.presTalkOnLeave3) {
                enabledDevices += "Say on leave:\n ${settings.presTalkOnLeave3}\n\n"
            }
            if (settings.presSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.presSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.presModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.presModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Presence Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG PRESENCE GROUP 3
        
        //BEGIN STATUS CONFIG LOCK GROUP 1
        if (settings.lockDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.lockDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.lockTalkOnLock1) {
                enabledDevices += "Say when locked:\n ${settings.lockTalkOnLock1}\n\n"
            }
            if (settings.lockTalkOnUnlock1) {
                enabledDevices += "Say when unlocked:\n ${settings.lockTalkOnUnlock1}\n\n"
            }
            if (settings.lockSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.lockSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.lockModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.lockModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Lock Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG LOCK GROUP 1
        //BEGIN STATUS CONFIG LOCK GROUP 2
        if (settings.lockDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.lockDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.lockTalkOnLock2) {
                enabledDevices += "Say when locked:\n ${settings.lockTalkOnLock2}\n\n"
            }
            if (settings.lockTalkOnUnlock2) {
                enabledDevices += "Say when unlocked:\n ${settings.lockTalkOnUnlock2}\n\n"
            }
            if (settings.lockSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.lockSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.lockModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.lockModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Lock Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG LOCK GROUP 2
        //BEGIN STATUS CONFIG LOCK GROUP 3
        if (settings.lockDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.lockDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.lockTalkOnLock3) {
                enabledDevices += "Say when locked:\n ${settings.lockTalkOnLock1}\n\n"
            }
            if (settings.lockTalkOnUnlock3) {
                enabledDevices += "Say when unlocked:\n ${settings.lockTalkOnUnlock1}\n\n"
            }
            if (settings.lockSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.lockSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.lockModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.lockModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Lock Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG LOCK GROUP 3
        
        //BEGIN STATUS CONFIG CONTACT GROUP 1
        if (settings.contactDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.contactDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.contactTalkOnOpen1) {
                enabledDevices += "Say when opened:\n ${settings.contactTalkOnOpen1}\n\n"
            }
            if (settings.contactTalkOnClose1) {
                enabledDevices += "Say when closed:\n ${settings.contactTalkOnClose1}\n\n"
            }
            if (settings.contactSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.contactSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.contactModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.contactModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Contact Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG CONTACT GROUP 1
        //BEGIN STATUS CONFIG CONTACT GROUP 2
        if (settings.contactDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.contactDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.contactTalkOnOpen2) {
                enabledDevices += "Say when opened:\n ${settings.contactTalkOnOpen2}\n\n"
            }
            if (settings.contactTalkOnClose2) {
                enabledDevices += "Say when closed:\n ${settings.contactTalkOnClose2}\n\n"
            }
            if (settings.contactSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.contactSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.contactModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.contactModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Contact Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG CONTACT GROUP 2
        //BEGIN STATUS CONFIG CONTACT GROUP 3
        if (settings.contactDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.contactDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.contactTalkOnOpen3) {
                enabledDevices += "Say when opened:\n ${settings.contactTalkOnOpen3}\n\n"
            }
            if (settings.contactTalkOnClose3) {
                enabledDevices += "Say when closed:\n ${settings.contactTalkOnClose3}\n\n"
            }
            if (settings.contactSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.contactSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.contactModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.contactModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Contact Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG CONTACT GROUP 3
        
        //BEGIN STATUS CONFIG MODE CHANGE GROUP 1
        if (settings.modePhraseGroup1) {
            enabledDevices += "Modes:  \n"
            enabledDevices += "   "
            settings.modePhraseGroup1.each() {
                enabledDevices += "${it},"
            }
            enabledDevices += "\n\n"
            if (settings.modeExcludePhraseGroup1) {
                enabledDevices += "Remain silent if mode is changed from:\n "
                enabledDevices += "   "
                settings.modeExcludePhraseGroup1.each(){
                    enabledDevices += "${it},"
                }
                enabledDevices += "\n\n"
            }            
            if (settings.contactTalkOnOpen1) {
                enabledDevices += "Say when changed:\n ${settings.TalkOnModeChange1}\n\n"
            }
            if (settings.modeSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.contactSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Mode Change:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG MODE CHANGE GROUP 1
        
        //BEGIN STATUS CONFIG THERMOSTAT GROUP 1
        if (settings.thermostatDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.thermostatDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.thermostatTalkOnIdle1) {
                enabledDevices += "Say when Idle:\n ${settings.thermostatTalkOnIdle1}\n\n"
            }
            if (settings.thermostatTalkOnHeating1) {
                enabledDevices += "Say when Heating:\n ${settings.thermostatTalkOnHeating1}\n\n"
            }
            if (settings.thermostatTalkOnCooling1) {
                enabledDevices += "Say when Cooling:\n ${settings.thermostatTalkOnCooling1}\n\n"
            }
            if (settings.thermostatTalkOnFan1) {
                enabledDevices += "Say when Fan:\n ${settings.thermostatTalkOnFan1}\n\n"
            }
            if (settings.thermostatSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.thermostatSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.thermostatModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.thermostatModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Thermostat Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG THERMOSTAT GROUP 1
        
        //BEGIN STATUS CONFIG ACCELERATION GROUP 1
        if (settings.accelerationDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.accelerationDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.accelerationTalkOnActive1) {
                enabledDevices += "Say when acceleration activated:\n ${settings.accelerationTalkOnActive1}\n\n"
            }
            if (settings.accelerationTalkOnInactive1) {
                enabledDevices += "Say when acceleration stops:\n ${settings.accelerationTalkOnInactive1}\n\n"
            }
            if (settings.accelerationSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.accelerationSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.accelerationModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.accelerationModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Acceleration Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG ACCELERATION GROUP 1
        //BEGIN STATUS CONFIG ACCELERATION GROUP 2
        if (settings.accelerationDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.accelerationDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.accelerationTalkOnActive2) {
                enabledDevices += "Say when acceleration activated:\n ${settings.accelerationTalkOnActive2}\n\n"
            }
            if (settings.accelerationTalkOnInactive2) {
                enabledDevices += "Say when acceleration stops:\n ${settings.accelerationTalkOnInactive2}\n\n"
            }
            if (settings.accelerationSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.accelerationSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.accelerationModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.accelerationModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Acceleration Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG ACCELERATION GROUP 2
        //BEGIN STATUS CONFIG ACCELERATION GROUP 3
        if (settings.accelerationDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.accelerationDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.accelerationTalkOnActive3) {
                enabledDevices += "Say when acceleration activated:\n ${settings.accelerationTalkOnActive3}\n\n"
            }
            if (settings.accelerationTalkOnInactive3) {
                enabledDevices += "Say when acceleration stops:\n ${settings.accelerationTalkOnInactive3}\n\n"
            }
            if (settings.accelerationSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.accelerationSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.accelerationModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.accelerationModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Acceleration Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG ACCELERATION GROUP 3
        
        //BEGIN STATUS CONFIG WATER GROUP 1
        if (settings.waterDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.waterDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.waterTalkOnWet1) {
                enabledDevices += "Say this when wet:\n ${settings.waterTalkOnWet1}\n\n"
            }
            if (settings.waterTalkOnWet1) {
                enabledDevices += "Say this when dry:\n ${settings.waterTalkOnDry1}\n\n"
            }
            if (settings.waterSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.waterSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.waterModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.waterModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Water Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG WATER GROUP 1
        //BEGIN STATUS CONFIG WATER GrOUP 2
        if (settings.waterDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.waterDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.waterTalkOnWet2) {
                enabledDevices += "Say this when wet:\n ${settings.waterTalkOnWet2}\n\n"
            }
            if (settings.waterTalkOnWet2) {
                enabledDevices += "Say this when dry:\n ${settings.waterTalkOnDry2}\n\n"
            }
            if (settings.waterSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.waterSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.waterModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.waterModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Water Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG WATER GROUP 2
        //BEGIN STATUS CONFIG WATER GROUP 3
        if (settings.waterDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.waterDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.waterTalkOnWet3) {
                enabledDevices += "Say this when wet:\n ${settings.waterTalkOnWet3}\n\n"
            }
            if (settings.waterTalkOnWet3) {
                enabledDevices += "Say this when dry:\n ${settings.waterTalkOnDry3}\n\n"
            }
            if (settings.waterSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.waterSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.waterModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.waterModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Water Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG WATER GROUP 3
        
        //BEGIN STATUS CONFIG SMOKE GROUP 1
        if (settings.smokeDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.smokeDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.smokeTalkOnDetect1) {
                enabledDevices += "Say this when smoke detected:\n ${settings.smokeTalkOnDetect1}\n\n"
            }
            if (settings.smokeTalkOnClear1) {
                enabledDevices += "Say this when smoke cleared:\n ${settings.smokeTalkOnClear1}\n\n"
            }
            if (settings.smokeTalkOnTest1) {
                enabledDevices += "Say this when smoke tested:\n ${settings.smokeTalkOnTest1}\n\n"
            }
            if (settings.smokeSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.smokeSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.smokeModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.smokeModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Smoke Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SMOKE GROUP 1
        //BEGIN STATUS CONFIG SMOKE GROUP 2
        if (settings.smokeDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.smokeDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.smokeTalkOnDetect2) {
                enabledDevices += "Say this when smoke detected:\n ${settings.smokeTalkOnDetect2}\n\n"
            }
            if (settings.smokeTalkOnClear2) {
                enabledDevices += "Say this when smoke cleared:\n ${settings.smokeTalkOnClear2}\n\n"
            }
            if (settings.smokeTalkOnTest2) {
                enabledDevices += "Say this when smoke tested:\n ${settings.smokeTalkOnTest2}\n\n"
            }
            if (settings.smokeSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.smokeSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.smokeModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.smokeModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Smoke Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SMOKE GROUP 2
        //BEGIN STATUS CONFIG SMOKE GROUP 3
        if (settings.smokeDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.smokeDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.smokeTalkOnDetect3) {
                enabledDevices += "Say this when smoke detected:\n ${settings.smokeTalkOnDetect3}\n\n"
            }
            if (settings.smokeTalkOnClear3) {
                enabledDevices += "Say this when smoke cleared:\n ${settings.smokeTalkOnClear3}\n\n"
            }
            if (settings.smokeTalkOnTest3) {
                enabledDevices += "Say this when smoke tested:\n ${settings.smokeTalkOnTest3}\n\n"
            }
            if (settings.smokeSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n"
                enabledDevices += "   "
                settings.smokeSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.smokeModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.smokeModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Smoke Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG SMOKE GROUP 3
        
        //BEGIN STATUS CONFIG BUTTON GROUP 1
        if (settings.buttonDeviceGroup1) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.buttonDeviceGroup1.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.buttonTalkOnDetect1) {
                enabledDevices += "Say this when button pressed:\n ${settings.buttonTalkOnPress1}\n\n"
            }
            if (settings.buttonSpeechDevice1) {
                enabledDevices += "Custom Speech Device(s):\n\n"
                enabledDevices += "   "
                settings.buttonSpeechDevice1.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.buttonModes1) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.buttonModes1.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Button Group 1:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG BUTTON GROUP 1
        //BEGIN STATUS CONFIG BUTTON GROUP 2
        if (settings.buttonDeviceGroup2) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.buttonDeviceGroup2.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.buttonTalkOnDetect2) {
                enabledDevices += "Say this when button pressed:\n ${settings.buttonTalkOnPress2}\n\n"
            }
            if (settings.buttonSpeechDevice2) {
                enabledDevices += "Custom Speech Device(s):\n\n"
                enabledDevices += "   "
                settings.buttonSpeechDevice2.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.buttonModes2) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.buttonModes2.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Button Group 2:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG BUTTON GROUP 2
        //BEGIN STATUS CONFIG BUTTON GROUP 3
        if (settings.buttonDeviceGroup3) {
            enabledDevices += "Devices:  \n"
            enabledDevices += "   "
            settings.buttonDeviceGroup3.each() {
                enabledDevices += "${it.displayName},"
            }
            enabledDevices += "\n\n"
            if (settings.buttonTalkOnDetect3) {
                enabledDevices += "Say this when button pressed:\n ${settings.buttonTalkOnPress3}\n\n"
            }
            if (settings.buttonSpeechDevice3) {
                enabledDevices += "Custom Speech Device(s):\n\n"
                enabledDevices += "   "
                settings.buttonSpeechDevice3.each() {
                    enabledDevices += "${it.displayName},"
                }
                enabledDevices += "\n\n"
            }
            if (settings.buttonModes3) {
                enabledDevices += "Custom mode(s):\n"
                enabledDevices += "   "
                settings.buttonModes3.each() {
                    enabledDevices += "${it},"
                }
            }
            if (!(enabledDevices == "")) {
                section ("Button Group 3:"){
                    paragraph enabledDevices
                }
            }
            enabledDevices = ""
        }
        //END STATUS CONFIG BUTTON GROUP 3
    }
}

def pageConfigure(){
    if (state.installed == null) { state.installed = false }
    dynamicPage(name: "pageConfigure", title: "Configure", install: true, uninstall: true) {
        section ("Talk with:"){
            //input "speechDeviceDefault", "capability.speechSynthesis", title: "Talk with these text-to-speech devices (default)", multiple: true, required: false, refreshAfterSelection: false
            input "speechDeviceDefault", "capability.musicPlayer", title: "Talk with these text-to-speech devices (default)", multiple: true, required: true, refreshAfterSelection: false
        }
        section ("Adjust volume during announcement (optional; Supports: Sonos, VLC-Thing):"){
            input "speechVolume", "number", title: "Set volume to (1-100%):", required: false
        }
        section ("When in these modes:"){
            input "speechModesDefault", "mode", title: "Talk only while in these modes (default)", multiple: true, required: true, refreshAfterSelection: false
        }
        section("Talk on events:") {
            href "pageConfigMotion", title:"Motion", description:"Tap to configure"
            href "pageConfigSwitch", title:"Switch", description:"Tap to configure"
            href "pageConfigPresence", title:"Presence", description:"Tap to configure"
            href "pageConfigLock", title:"Lock", description:"Tap to configure"
            href "pageConfigContact", title:"Contact", description:"Tap to configure"
            href "pageConfigMode", title:"Mode", description:"Tap to configure"
            href "pageConfigThermostat", title:"Thermostat", description:"Tap to configure"
            href "pageConfigAcceleration", title: "Acceleration", description:"Tap to configure"
            href "pageConfigWater", title: "Water", description:"Tap to configure"
            href "pageConfigSmoke", title: "Smoke", description:"Tap to configure"
            href "pageConfigButton", title: "Button", description:"Tap to configure"
        }
//        section([mobileOnly:true]){
//            label title: "SmartApp Name (Front Door Left Open)", required: true
//            mode title: "Set for specific mode(s)", required: false
//        }
    }
//End pageConfigure()
}

def pageConfigMotion(){
    dynamicPage(name: "pageConfigMotion", title: "Configure talk on motion", install: false, uninstall: false) {
        section("Motion Sensor Group 1"){
            input name: "motionDeviceGroup1", type: "capability.motionSensor", title: "Motion Sensor(s)", required: false, multiple: true
            input name: "motionTalkActive1", type: "text", title: "Say this on motion active:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "motionTalkInactive1", type: "text", title: "Say this on motion inactive:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "motionSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "motionModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Motion Sensor Group 2"){
            input name: "motionDeviceGroup2", type: "capability.motionSensor", title: "Motion Sensor(s)", required: false, multiple: true
            input name: "motionTalkActive2", type: "text", title: "Say this on motion active:", required: false
            input name: "motionTalkInactive2", type: "text", title: "Say this on motion inactive:", required: false
            input name: "motionSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "motionModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Motion Sensor Group 3"){
            input name: "motionDeviceGroup3", type: "capability.motionSensor", title: "Motion Sensor(s)", required: false, multiple: true
            input name: "motionTalkActive3", type: "text", title: "Say this on motion active:", required: false
            input name: "motionTalkInactive3", type: "text", title: "Say this on motion inactive:", required: false
            input name: "motionSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "motionModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigMotion()
}

def pageConfigSwitch(){
    dynamicPage(name: "pageConfigSwitch", title: "Configure talk on switch", install: false, uninstall: false) {
        section("Switch Group 1"){
            input name: "switchDeviceGroup1", type: "capability.switch", title: "Switch(es)", required: false, multiple: true
            input name: "switchTalkOn1", type: "text", title: "Say this when switch is turned ON:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "switchTalkOff1", type: "text", title: "Say this when switch is turned OFF:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "switchSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "switchModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Switch Group 2"){
            input name: "switchDeviceGroup2", type: "capability.switch", title: "Switch(es)", required: false, multiple: true
            input name: "switchTalkOn2", type: "text", title: "Say this when switch is turned ON:", required: false
            input name: "switchTalkOff2", type: "text", title: "Say this when switch is turned OFF:", required: false
            input name: "switchSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "switchModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Switch Group 3"){
            input name: "switchDeviceGroup3", type: "capability.switch", title: "Switch(es)", required: false, multiple: true
            input name: "switchTalkOn3", type: "text", title: "Say this when switch is turned ON:", required: false
            input name: "switchTalkOff3", type: "text", title: "Say this when switch is turned OFF:", required: false
            input name: "switchSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "switchModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigSwitch()
}

def pageConfigPresence(){
    dynamicPage(name: "pageConfigPresence", title: "Configure talk on presence", install: false, uninstall: false) {
        section("Presence Group 1"){
            input name: "presDeviceGroup1", type: "capability.presenceSensor", title: "Presence Sensor(s)", required: false, multiple: true
            input name: "presTalkOnArrive1", type: "text", title: "Say this when someone arrives:", required: false, defaultValue: "%devicename% has arrived"
            input name: "presTalkOnLeave1", type: "text", title: "Say this when someone leaves:", required: false, defaultValue: "%devicename% has left"
            input name: "presSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "presModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Presence Group 2"){
            input name: "presDeviceGroup2", type: "capability.presenceSensor", title: "Presence Sensor(s)", required: false, multiple: true
            input name: "presTalkOnArrive2", type: "text", title: "Say this when someone arrives:", required: false
            input name: "presTalkOnLeave2", type: "text", title: "Say this when someone leaves:", required: false
            input name: "presSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "presModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Presence Group 3"){
            input name: "presDeviceGroup3", type: "capability.presenceSensor", title: "Presence Sensor(s)", required: false, multiple: true
            input name: "presTalkOnArrive3", type: "text", title: "Say this when someone arrives:", required: false
            input name: "presTalkOnLeave3", type: "text", title: "Say this when someone leaves:", required: false
            input name: "presSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "presModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigPresence()
}

def pageConfigLock(){
    dynamicPage(name: "pageConfigLock", title: "Configure talk on lock", install: false, uninstall: false) {
        section("Lock Group 1"){
            input name: "lockDeviceGroup1", type: "capability.lock", title: "Lock(s)", required: false, multiple: true
            input name: "lockTalkOnUnlock1", type: "text", title: "Say this when unlocked:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "lockTalkOnLock1", type: "text", title: "Say this when locked:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "lockSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "lockModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Lock Group 2"){
            input name: "lockDeviceGroup2", type: "capability.lock", title: "Lock(s)", required: false, multiple: true
            input name: "lockTalkOnUnlock2", type: "text", title: "Say this when unlocked:", required: false
            input name: "lockTalkOnLock2", type: "text", title: "Say this when locked:", required: false
            input name: "lockSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "lockModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Lock Group 3"){
            input name: "lockDeviceGroup3", type: "capability.lock", title: "Lock(s)", required: false, multiple: true
            input name: "lockTalkOnUnlock3", type: "text", title: "Say this when unlocked:", required: false
            input name: "lockTalkOnLock3", type: "text", title: "Say this when locked:", required: false
            input name: "lockSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "lockModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigLock()
}

def pageConfigContact(){
    dynamicPage(name: "pageConfigContact", title: "Configure talk on contact sensor", install: false, uninstall: false) {
        section("Contact Group 1"){
            input name: "contactDeviceGroup1", type: "capability.contactSensor", title: "Contact sensor(s)", required: false, multiple: true
            input name: "contactTalkOnOpen1", type: "text", title: "Say this when opened:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactTalkOnClose1", type: "text", title: "Say this when closed:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "contactModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Contact Group 2"){
            input name: "contactDeviceGroup2", type: "capability.contactSensor", title: "Contact sensor(s)", required: false, multiple: true
            input name: "contactTalkOnOpen2", type: "text", title: "Say this when opened:", required: false
            input name: "contactTalkOnClose2", type: "text", title: "Say this when closed:", required: false
            input name: "contactSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "contactModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Contact Group 3"){
            input name: "contactDeviceGroup3", type: "capability.contactSensor", title: "Contact sensor(s)", required: false, multiple: true
            input name: "contactTalkOnOpen3", type: "text", title: "Say this when opened:", required: false
            input name: "contactTalkOnClose3", type: "text", title: "Say this when closed:", required: false
            input name: "contactSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "contactModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigContact()
}

def pageConfigMode(){
    def locationmodes = []
    location.modes.each(){
       locationmodes += it
    }
    LOGDEBUG("locationmodes=${locationmodes}")
    dynamicPage(name: "pageConfigMode", title: "Configure talk on home mode change", install: false, uninstall: false) {
        section("Mode Group 1"){
            //input name: "modePhraseGroup1", type:"enum", title:"When mode changes to: ", options:locationmodes, required:false, multiple:true, refreshAfterSelection:true
            //input name: "modePhraseGroup1", type:"enum", title:"When mode changes to: ", metadata:[values:(locationmodes)], required:false, multiple:true, refreshAfterSelection:true
            input name: "modePhraseGroup1", type:"mode", title:"When mode changes to: ", required:false, multiple:true, refreshAfterSelection:false
            input name: "modeExcludePhraseGroup1", type: "mode", title: "But not when changed from (optional): ", required: false, multiple: true
            input name: "TalkOnModeChange1", type: "text", title: "Say this when home mode is changed", required: false, defaultValue: "%locationname% mode has changed from %lastmode% to %mode%"
            input name: "modePhraseSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigMode()
}

def pageConfigThermostat(){
    dynamicPage(name: "pageConfigThermostat", title: "Configure talk when thermostat state is:", install: false, uninstall: false) {
        section("Contact Group 1"){
            input name: "thermostatDeviceGroup1", type: "capability.thermostat", title: "Thermostat(s)", required: false, multiple: true
            input name: "thermostatTalkOnIdle1", type: "text", title: "Say this on change to Idle:", required: false, defaultValue: "%devicename% is now off"
            input name: "thermostatTalkOnHeating1", type: "text", title: "Say this on change to heating:", required: false, defaultValue: "%devicename% is now heating"
            input name: "thermostatTalkOnCooling1", type: "text", title: "Say this on change to cooling:", required: false, defaultValue: "%devicename% is now cooling"
            input name: "thermostatTalkOnFan1", type: "text", title: "Say this on change to fan only:", required: false, defaultValue: "%devicename% is now circulating fan"
            input name: "thermostatSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "thermostatModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
/*        
        section("Contact Group 2"){
            input name: "contactDeviceGroup2", type: "capability.contactSensor", title: "Contact sensor(s)", required: false, multiple: true
            input name: "contactTalkOnOpen2", type: "text", title: "Say this when opened:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactTalkOnClose2", type: "text", title: "Say this when closed:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
        }
        section("Contact Group 3"){
            input name: "contactDeviceGroup3", type: "capability.contactSensor", title: "Contact sensor(s)", required: false, multiple: true
            input name: "contactTalkOnOpen3", type: "text", title: "Say this when opened:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactTalkOnClose3", type: "text", title: "Say this when closed:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "contactSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
        }
*/
    }
//End pageConfigContact()
}

def pageConfigAcceleration(){
    dynamicPage(name: "pageConfigAcceleration", title: "Configure talk on acceleration", install: false, uninstall: false) {
        section("Acceleration Group 1"){
            input name: "accelerationDeviceGroup1", type: "capability.accelerationSensor", title: "Acceleration sensor(s)", required: false, multiple: true
            input name: "accelerationTalkOnActive1", type: "text", title: "Say this when activated:", required: false, defaultValue: "%devicename% acceleration %devicechange%"
            input name: "accelerationTalkOnInactive1", type: "text", title: "Say this when inactivated:", required: false, defaultValue: "%devicename% acceleration is no longer active"
            input name: "accelerationSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "accelerationModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Acceleration Group 2"){
            input name: "accelerationDeviceGroup2", type: "capability.accelerationSensor", title: "Acceleration sensor(s)", required: false, multiple: true
            input name: "accelerationTalkOnActive2", type: "text", title: "Say this when activated:", required: false
            input name: "accelerationTalkOnInactive2", type: "text", title: "Say this when inactivated:", required: false
            input name: "accelerationSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "accelerationModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Acceleration Group 3"){
            input name: "accelerationDeviceGroup3", type: "capability.accelerationSensor", title: "Acceleration sensor(s)", required: false, multiple: true
            input name: "accelerationTalkOnActive3", type: "text", title: "Say this when activated:", required: false
            input name: "accelerationTalkOnInactive3", type: "text", title: "Say this when inactivated:", required: false
            input name: "accelerationSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "accelerationModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigAcceleration()
}

def pageConfigWater(){
    dynamicPage(name: "pageConfigWater", title: "Configure talk on water", install: false, uninstall: false) {
        section("Water Group 1"){
            input name: "waterDeviceGroup1", type: "capability.waterSensor", title: "Water sensor(s)", required: false, multiple: true
            input name: "waterTalkOnWet1", type: "text", title: "Say this when wet:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "waterTalkOnDry1", type: "text", title: "Say this when dry:", required: false, defaultValue: "%devicename% is now %devicechange%"
            input name: "waterSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "waterModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Water Group 2"){
            input name: "waterDeviceGroup2", type: "capability.waterSensor", title: "Water sensor(s)", required: false, multiple: true
            input name: "waterTalkOnWet2", type: "text", title: "Say this when wet:", required: false
            input name: "waterTalkOnDry2", type: "text", title: "Say this when dry:", required: false
            input name: "waterSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "waterModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Water Group 3"){
            input name: "waterDeviceGroup3", type: "capability.waterSensor", title: "Water sensor(s)", required: false, multiple: true
            input name: "waterTalkOnWet3", type: "text", title: "Say this when wet:", required: false
            input name: "waterTalkOnDry3", type: "text", title: "Say this when dry:", required: false
            input name: "waterSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "waterModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigWater()
}

def pageConfigSmoke(){
    dynamicPage(name: "pageConfigSmoke", title: "Configure talk on smoke", install: false, uninstall: false) {
        section("Smoke Group 1"){
            input name: "smokeDeviceGroup1", type: "capability.smokeDetector", title: "Smoke detector(s)", required: false, multiple: true
            input name: "smokeTalkOnDetect1", type: "text", title: "Say this when detected:", required: false, defaultValue: "Smoke, %devicename% has detected smoke"
            input name: "smokeTalkOnClear1", type: "text", title: "Say this when cleared:", required: false, defaultValue: "Smoke, %devicename% has cleared smoke alert"
            input name: "smokeTalkOnTest1", type: "text", title: "Say this when tested:", required: false, defaultValue: "Smoke, %devicename% has been tested"
            input name: "smokeSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "smokeModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Smoke Group 2"){
            input name: "smokeDeviceGroup2", type: "capability.smokeDetector", title: "Smoke detector(s)", required: false, multiple: true
            input name: "smokeTalkOnDetect2", type: "text", title: "Say this when detected:", required: false
            input name: "smokeTalkOnClear2", type: "text", title: "Say this when cleared:", required: false
            input name: "smokeTalkOnTest2", type: "text", title: "Say this when tested:", required: false
            input name: "smokeSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "smokeModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Smoke Group 3"){
            input name: "smokeDeviceGroup3", type: "capability.smokeDetector", title: "Smoke detector(s)", required: false, multiple: true
            input name: "smokeTalkOnDetect3", type: "text", title: "Say this when detected:", required: false
            input name: "smokeTalkOnClear3", type: "text", title: "Say this when cleared:", required: false
            input name: "smokeTalkOnTest3", type: "text", title: "Say this when tested:", required: false
            input name: "smokeSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "smokeModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigSmoke()
}

def pageConfigButton(){
    dynamicPage(name: "pageConfigButton", title: "Configure talk on button press", install: false, uninstall: false) {
        section("Button Group 1"){
            input name: "buttonDeviceGroup1", type: "capability.button", title: "Button(s)", required: false, multiple: true
            input name: "buttonTalkOnPress1", type: "text", title: "Say this when pressed:", required: false, defaultValue: "%devicename% button pressed"
            input name: "buttonSpeechDevice1", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "buttonModes1", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Button Group 2"){
            input name: "buttonDeviceGroup2", type: "capability.button", title: "Button(s)", required: false, multiple: true
            input name: "buttonTalkOnPress2", type: "text", title: "Say this when pressed:", required: false
            input name: "buttonSpeechDevice2", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "buttonModes2", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
        section("Button Group 3"){
            input name: "buttonDeviceGroup3", type: "capability.button", title: "Button(s)", required: false, multiple: true
            input name: "buttonTalkOnPress3", type: "text", title: "Say this when pressed:", required: false
            input name: "buttonSpeechDevice3", type: "capability.musicPlayer", title: "Talk with these text-to-speech devices (overrides default)", multiple: true, required: false
            input name: "buttonModes3", type: "mode", title: "Talk when in these mode(s) (overrides default)", multiple: true, required: false
        }
    }
//End pageConfigSmoke()
}

def installed() {
	state.installed = true
    LOGTRACE("Installed with settings: ${settings}")

	initialize()
//End installed()
}

def updated() {
	LOGTRACE("Updated with settings: ${settings}")

	unsubscribe()
	initialize()
//End updated()
}

def initialize() {
    setAppVersion()
    //Subscribe Motions
    if (motionDeviceGroup1) { subscribe(motionDeviceGroup1, "motion", onMotion1Event) }
    if (motionDeviceGroup2) { subscribe(motionDeviceGroup2, "motion", onMotion2Event) }
    if (motionDeviceGroup3) { subscribe(motionDeviceGroup3, "motion", onMotion3Event) }
    //Subscribe Switches
    if (switchDeviceGroup1) { subscribe(switchDeviceGroup1, "switch", onSwitch1Event) }
    if (switchDeviceGroup2) { subscribe(switchDeviceGroup2, "switch", onSwitch2Event) }
    if (switchDeviceGroup3) { subscribe(switchDeviceGroup3, "switch", onSwitch3Event) }
    //Subscribe Presence
    if (presDeviceGroup1) { subscribe(presDeviceGroup1, "presence", onPresence1Event) }
    if (presDeviceGroup2) { subscribe(presDeviceGroup2, "presence", onPresence2Event) }
    if (presDeviceGroup3) { subscribe(presDeviceGroup3, "presence", onPresence3Event) }
    //Subscribe Lock
    if (lockDeviceGroup1) { subscribe(lockDeviceGroup1, "lock", onLock1Event) }
    if (lockDeviceGroup2) { subscribe(lockDeviceGroup2, "lock", onLock2Event) }
    if (lockDeviceGroup3) { subscribe(lockDeviceGroup3, "lock", onLock3Event) }
    //Subscribe Contact
    if (contactDeviceGroup1) { subscribe(contactDeviceGroup1, "contact", onContact1Event) }
    if (contactDeviceGroup2) { subscribe(contactDeviceGroup2, "contact", onContact2Event) }
    if (contactDeviceGroup3) { subscribe(contactDeviceGroup3, "contact", onContact3Event) }
    //Subscribe Thermostat
    if (thermostatDeviceGroup1) { subscribe(thermostatDeviceGroup1, "thermostatOperatingState", onThermostat1Event) }
    if (thermostatDeviceGroup2) { subscribe(thermostatDeviceGroup2, "thermostatOperatingState", onThermostat2Event) }
    if (thermostatDeviceGroup3) { subscribe(thermostatDeviceGroup3, "thermostatOperatingState", onThermostat3Event) }
    //Subscribe Acceleration
    if (accelerationDeviceGroup1) { subscribe(accelerationDeviceGroup1, "acceleration", onAcceleration1Event) }
    if (accelerationDeviceGroup2) { subscribe(accelerationDeviceGroup2, "acceleration", onAcceleration2Event) }
    if (accelerationDeviceGroup3) { subscribe(accelerationDeviceGroup3, "acceleration", onAcceleration3Event) }
    //Subscribe Water
    if (waterDeviceGroup1) { subscribe(waterDeviceGroup1, "water", onWater1Event) }
    if (waterDeviceGroup2) { subscribe(waterDeviceGroup2, "water", onWater2Event) }
    if (waterDeviceGroup3) { subscribe(waterDeviceGroup3, "water", onWater3Event) }
    //Subscribe Smoke
    if (smokeDeviceGroup1) { subscribe(smokeDeviceGroup1, "smoke", onSmoke1Event) }
    if (smokeDeviceGroup2) { subscribe(smokeDeviceGroup2, "smoke", onSmoke2Event) }
    if (smokeDeviceGroup3) { subscribe(smokeDeviceGroup3, "smoke", onSmoke3Event) }
    //Subscribe Button
    if (buttonDeviceGroup1) { subscribe(buttonDeviceGroup1, "button", onButton1Event) }
    if (buttonDeviceGroup2) { subscribe(buttonDeviceGroup2, "button", onButton2Event) }
    if (buttonDeviceGroup3) { subscribe(buttonDeviceGroup3, "button", onButton3Event) }
    //Subscribe Mode
    if (modePhraseGroup1) { subscribe(location, onModeChangeEvent) }
    state.lastMode = location.mode
	LOGTRACE("Initialized")
    
//End initialize()
}

//BEGIN HANDLE MOTIONS
def onMotion1Event(evt){
    processMotionEvent(1, evt)
}
def onMotion2Event(evt){
    processMotionEvent(2, evt)
}
def onMotion3Event(evt){
    processMotionEvent(3, evt)
}

def processMotionEvent(index, evt){
    LOGDEBUG("(onMotionEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("motion",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "active") {
        if (index == 1) { state.TalkPhrase = settings.motionTalkActive1; state.speechDevice = motionSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.motionTalkActive2; state.speechDevice = motionSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.motionTalkActive3; state.speechDevice = motionSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "inactive") {
        if (index == 1) { state.TalkPhrase = settings.motionTalkInactive1; state.speechDevice = motionSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.motionTalkInactive2; state.speechDevice = motionSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.motionTalkInactive3; state.speechDevice = motionSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE MOTIONS


//BEGIN HANDLE SWITCHES
def onSwitch1Event(evt){
    processSwitchEvent(1, evt)
}

def onSwitch2Event(evt){
    processSwitchEvent(2, evt)
}

def onSwitch3Event(evt){
    processSwitchEvent(3, evt)
}

def processSwitchEvent(index, evt){
    LOGDEBUG("(onSwitchEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("switch",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "on") {
        if (index == 1) { state.TalkPhrase = settings.switchTalkOn1; state.speechDevice = switchSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.switchTalkOn2; state.speechDevice = switchSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.switchTalkOn3; state.speechDevice = switchSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "off") {
        if (index == 1) { state.TalkPhrase = settings.switchTalkOff1; state.speechDevice = switchSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.switchTalkOff2; state.speechDevice = switchSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.switchTalkOff3; state.speechDevice = switchSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE SWITCHES

//BEGIN HANDLE PRESENCE
def onPresence1Event(evt){
    processPresenceEvent(1, evt)
}
def onPresence2Event(evt){
    processPresenceEvent(2, evt)
}
def onPresence3Event(evt){
    processPresenceEvent(3, evt)
}

def processPresenceEvent(index, evt){
    LOGDEBUG("(onPresenceEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("presence",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "present") {
        if (index == 1) { state.TalkPhrase = settings.presTalkOnArrive1; state.speechDevice = presSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.presTalkOnArrive2; state.speechDevice = presSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.presTalkOnArrive3; state.speechDevice = presSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "not present") {
        if (index == 1) { state.TalkPhrase = settings.presTalkOnLeave1; state.speechDevice = presSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.presTalkOnLeave2; state.speechDevice = presSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.presTalkOnLeave3; state.speechDevice = presSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE PRESENCE

//BEGIN HANDLE LOCK
def onLock1Event(evt){
    LOGDEBUG("onLock1Event(evt) ${evt.value}")
    processLockEvent(1, evt)
}
def onLock2Event(evt){
    processLockEvent(2, evt)
}
def onLockEvent(evt){
    processLockEvent(3, evt)
}

def processLockEvent(index, evt){
    LOGDEBUG("(onLockEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("lock",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "locked") {
        if (index == 1) { state.TalkPhrase = settings.lockTalkOnLock1; state.speechDevice = lockSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.lockTalkOnLock2; state.speechDevice = lockSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.lockTalkOnLock3; state.speechDevice = lockSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "unlocked") {
        if (index == 1) { state.TalkPhrase = settings.lockTalkOnUnlock1; state.speechDevice = lockSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.lockTalkOnUnlock2; state.speechDevice = lockSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.lockTalkOnUnlock3; state.speechDevice = lockSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE LOCK

//BEGIN HANDLE CONTACT
def onContact1Event(evt){
    processContactEvent(1, evt)
}
def onContact2Event(evt){
    processContactEvent(2, evt)
}
def onContactEvent(evt){
    processContactEvent(3, evt)
}

def processContactEvent(index, evt){
    LOGDEBUG("(onContactEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("contact",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "open") {
        if (index == 1) { state.TalkPhrase = settings.contactTalkOnOpen1; state.speechDevice = contactSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.contactTalkOnOpen2; state.speechDevice = contactSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.contactTalkOnOpen3; state.speechDevice = contactSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "closed") {
        if (index == 1) { state.TalkPhrase = settings.contactTalkOnClose1; state.speechDevice = contactSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.contactTalkOnClose2; state.speechDevice = contactSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.contactTalkOnClose3; state.speechDevice = contactSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE CONTACT

//BEGIN MODE CHANGE
def onModeChangeEvent(evt){
    processModeChangeEvent(1, evt)
}
def processModeChangeEvent(index, evt){
    LOGDEBUG("(onModeEvent): Last Mode: ${state.lastMode}, New Mode: ${location.mode}")
    if (settings.modePhraseGroup1.contains(location.mode)){
        if (!(settings.modeExcludePhraseGroup1.contains(state.lastMode))) {
            state.TalkPhrase = null
            state.speechDevice = null
            state.TalkPhrase = settings.TalkOnModeChange1; state.speechDevice = modePhraseSpeechDevice1
            Talk(state.TalkPhrase, state.speechDevice, evt)
            state.TalkPhrase = null
            state.speechDevice = null
        } else {
            LOGDEBUG("Mode change silent due to exclusion configuration (${state.lastMode} >> ${location.mode})")
        }
    }
    state.lastMode = location.mode
}
//END MODE CHANGE

//BEGIN HANDLE THERMOSTAT
def onThermostat1Event(evt){
    processThermostatEvent(1, evt)
}
def onThermostat2Event(evt){
    processThermostatEvent(2, evt)
}
def onThermostatEvent(evt){
    processThermostatEvent(3, evt)
}

def processThermostatEvent(index, evt){
    LOGDEBUG("(onThermostatEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("thermostat",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "idle") {
        if (index == 1) { state.TalkPhrase = settings.thermostatTalkOnIdle1; state.speechDevice = thermostatSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.thermostatTalkOnIdle2; state.speechDevice = thermostatSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.thermostatTalkOnIdle3; state.speechDevice = thermostatSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "heating") {
        if (index == 1) { state.TalkPhrase = settings.thermostatTalkOnHeating1; state.speechDevice = thermostatSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.thermostatTalkOnHeating2; state.speechDevice = thermostatSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.thermostatTalkOnHeating3; state.speechDevice = thermostatSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "cooling") {
        if (index == 1) { state.TalkPhrase = settings.thermostatTalkOnCooling1; state.speechDevice = thermostatSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.thermostatTalkOnCooling2; state.speechDevice = thermostatSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.thermostatTalkOnCooling3; state.speechDevice = thermostatSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "fan only") {
        if (index == 1) { state.TalkPhrase = settings.thermostatTalkOnFan1; state.speechDevice = thermostatSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.thermostatTalkOnFan2; state.speechDevice = thermostatSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.thermostatTalkOnFan3; state.speechDevice = thermostatSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }

    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE THERMOSTAT

//BEGIN HANDLE ACCELERATION
def onAcceleration1Event(evt){
    processAccelerationEvent(1, evt)
}
def onAcceleration2Event(evt){
    processAccelerationEvent(2, evt)
}
def onAcceleration3Event(evt){
    processAccelerationEvent(3, evt)
}

def processAccelerationEvent(index, evt){
    LOGDEBUG("(onAccelerationEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("acceleration",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "active") {
        if (index == 1) { state.TalkPhrase = settings.accelerationTalkOnActive1; state.speechDevice = accelerationSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.accelerationTalkOnActive2; state.speechDevice = accelerationSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.accelerationTalkOnActive3; state.speechDevice = accelerationSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "inactive") {
        if (index == 1) { state.TalkPhrase = settings.accelerationTalkOnInactive1; state.speechDevice = accelerationSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.accelerationTalkOnInactive2; state.speechDevice = accelerationSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.accelerationTalkOnInactive3; state.speechDevice = accelerationSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE ACCELERATION

//BEGIN HANDLE WATER
def onWater1Event(evt){
    processWaterEvent(1, evt)
}
def onWater2Event(evt){
    processWaterEvent(2, evt)
}
def onWater3Event(evt){
    processWaterEvent(3, evt)
}

def processWaterEvent(index, evt){
    LOGDEBUG("(onWaterEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("water",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "wet") {
        if (index == 1) { state.TalkPhrase = settings.waterTalkOnWet1; state.speechDevice = waterSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.waterTalkOnWet2; state.speechDevice = waterSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.waterTalkOnWet3; state.speechDevice = waterSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "dry") {
        if (index == 1) { state.TalkPhrase = settings.waterTalkOnDry1; state.speechDevice = waterSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.waterTalkOnDry2; state.speechDevice = waterSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.waterTalkOnDry3; state.speechDevice = waterSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE WATER

//BEGIN HANDLE SMOKE
def onSmoke1Event(evt){
    processSmokeEvent(1, evt)
}
def onSmoke2Event(evt){
    processSmokeEvent(2, evt)
}
def onSmoke3Event(evt){
    processSmokeEvent(3, evt)
}

def processSmokeEvent(index, evt){
    LOGDEBUG("(onSmokeEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("smoke",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (evt.value == "detected") {
        if (index == 1) { state.TalkPhrase = settings.smokeTalkOnDetect1; state.speechDevice = smokeSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.smokeTalkOnDetect2; state.speechDevice = smokeSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.smokeTalkOnDetect3; state.speechDevice = smokeSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "clear") {
        if (index == 1) { state.TalkPhrase = settings.smokeTalkOnClear1; state.speechDevice = smokeSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.smokeTalkOnClear2; state.speechDevice = smokeSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.smokeTalkOnClear3; state.speechDevice = smokeSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    if (evt.value == "tested") {
        if (index == 1) { state.TalkPhrase = settings.smokeTalkOnTest1; state.speechDevice = smokeSpeechDevice1}
        if (index == 2) { state.TalkPhrase = settings.smokeTalkOnTest2; state.speechDevice = smokeSpeechDevice2}
        if (index == 3) { state.TalkPhrase = settings.smokeTalkOnTest3; state.speechDevice = smokeSpeechDevice3}
        Talk(state.TalkPhrase, state.speechDevice, evt)
    }
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE SMOKE

//BEGIN HANDLE BUTTON
def onButton1Event(evt){
    processButtonEvent(1, evt)
}
def onButton2Event(evt){
    processButtonEvent(2, evt)
}
def onButton3Event(evt){
    processButtonEvent(3, evt)
}

def processButtonEvent(index, evt){
    LOGDEBUG("(onButtonEvent): ${evt.name}, ${index}, ${evt.value}")
    //Are we in a talking mode?
    if (!(modeAllowed("button",index))) { 
        LOGDEBUG "Remain silent while in mode ${location.mode}"
        return
    }
    state.TalkPhrase = null
    state.speechDevice = null
    if (index == 1) { state.TalkPhrase = settings.buttonTalkOnPress1; state.speechDevice = buttonSpeechDevice1}
    if (index == 2) { state.TalkPhrase = settings.buttonTalkOnPress2; state.speechDevice = buttonSpeechDevice2}
    if (index == 3) { state.TalkPhrase = settings.buttonTalkOnPress3; state.speechDevice = buttonSpeechDevice3}
    Talk(state.TalkPhrase, state.speechDevice, evt)
    state.TalkPhrase = null
    state.speechDevice = null
}
//END HANDLE BUTTON

def processPhraseVariables(phrase, evt){
    phrase = phrase.replace('%devicename%', evt.displayName)  //User given name of the device
    phrase = phrase.replace('%devicetype%', evt.name)  //Device type: motion, switch, etc...
    phrase = phrase.replace('%devicechange%', evt.value)  //State change that occurred: on/off, active/inactive, etc...
    phrase = phrase.replace('%locationname%', location.name)
    phrase = phrase.replace('%lastmode%', state.lastMode)
    phrase = phrase.replace('%mode%', location.mode)
    return phrase
}

def Talk(phrase, customSpeechDevice, evt){
    def currentSpeechDevices = []
    if (!(phrase == null)) {
        phrase = processPhraseVariables(phrase, evt)
        if (!(customSpeechDevice == null)) {
            currentSpeechDevices = customSpeechDevice
        } else {
            //Use Default Speech Device
            currentSpeechDevices = settings.speechDeviceDefault
        }
        //Iterate Speech Devices and talk
        LOGTRACE("TALK(${evt.name}) >> ${phrase}")
        currentSpeechDevices.each(){
            def currentStatus = it.currentValue("status")
            def currentTrack = it.currentState("trackData")?.jsonValue
            def currentVolume = it.currentState("level")?.integerValue ? it.currentState("level")?.integerValue : 0
            if (settings.speechVolume) { LOGDEBUG("${it.displayName} | Volume: ${currentVolume}, Desired Volume: ${settings.speechVolume}") }
            if (!(settings.speechVolume)) { LOGDEBUG("${it.displayName} | Volume: ${currentVolume}") }
            if (!(currentTrack == null)){
                //currentTrack has data
                LOGTRACE("${it.displayName} | (1)Current Status: ${currentStatus}, CurrentTrack: ${currentTrack}, CurrentTrack.Status: ${currentTrack.status}.")
                if (currentTrack.status == 'playing') {
                    LOGTRACE("${it.displayName} | Resuming play. Sending playTextAndResume().")
                    if (settings.speechVolume) { 
                        it.playTextAndResume(phrase, settings.speechVolume) 
                    } else { 
                        it.playTextAndResume(phrase, currentVolume) 
                    }
                } else
                {
                    LOGDEBUG("${it.displayName} | (2)Nothing playing. Sending playTextAndResume()")
                    if (settings.speechVolume) { 
                        it.playTextAndResume(phrase, settings.speechVolume) 
                    } else { 
                        it.playTextAndResume(phrase, currentVolume) 
                    }
                }
            } else {
                //currentTrack doesn't have data or is not supported on this device
                if (currentStatus == "disconnected") {
                    //VLCThing?
                    LOGTRACE("${it.displayName} | (3)VLCThing? | Current Status: ${currentStatus}.")
                    if (settings.speechVolume) { 
                        it.setLevel(settings.speechVolume)
                        it.playText(phrase)
                        it.setLevel(currentVolume)
                    } else { 
                        it.playText(phrase) 
                    }
                } else {
                    LOGTRACE("${it.displayName} | (4)Current Status: ${currentStatus}.")
                    if (settings.speechVolume) { 
                        it.setLevel(settings.speechVolume)
                        it.playText(phrase)
                        it.setLevel(currentVolume)
                    } else { 
                        it.playText(phrase) 
                    }
                }
            }
        }
    }
}

def modeAllowed(devicetype,index) {
    //Determine if we are allowed to speak in our current mode based on the calling device or default setting
    //devicetype = motion, switch, presence, lock, contact, thermostat, acceleration, water, smoke, button
    switch (devicetype) {
        case "motion":
            if (index == 1) {
                //Motion Group 1
                if (settings.motionModes1) {
                    if (settings.motionModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Motion Group 2
                if (settings.motionModes2) {
                    if (settings.motionModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Motion Group 3
                if (settings.motionModes3) {
                    if (settings.motionModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "motion"
        case "switch":
            if (index == 1) {
                //Switch Group 1
                if (settings.switchModes1) {
                    if (settings.switchModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Switch Group 2
                if (settings.switchModes2) {
                    if (settings.switchModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Switch Group 3
                if (settings.switchModes3) {
                    if (settings.switchModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "switch"
        case "presence":
            if (index == 1) {
                //Presence Group 1
                if (settings.presenceModes1) {
                    if (settings.presenceModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Presence Group 2
                if (settings.presenceModes2) {
                    if (settings.presenceModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Presence Group 3
                if (settings.presenceModes3) {
                    if (settings.presenceModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "presence"
        case "lock":
            if (index == 1) {
                //Lock Group 1
                if (settings.lockModes1) {
                    if (settings.lockModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Lock Group 2
                if (settings.lockModes2) {
                    if (settings.lockModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Lock Group 3
                if (settings.lockModes3) {
                    if (settings.lockModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "lock"
        case "contact":
            if (index == 1) {
                //Contact Group 1
                if (settings.contactModes1) {
                    if (settings.contactModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Contact Group 2
                if (settings.contactModes2) {
                    if (settings.contactModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Contact Group 3
                if (settings.contactModes3) {
                    if (settings.contactModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "contact"
        case "thermostat":
            if (index == 1) {
                //Thermostat Group 1
                if (settings.thermostatModes1) {
                    if (settings.thermostatModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Thermostat Group 2
                if (settings.thermostatModes2) {
                    if (settings.thermostatModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Thermostat Group 3
                if (settings.thermostatModes3) {
                    if (settings.thermostatModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "thermostat"
        case "acceleration":
            if (index == 1) {
                //Acceleration Group 1
                if (settings.accelerationModes1) {
                    if (settings.accelerationModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Acceleration Group 2
                if (settings.accelerationModes2) {
                    if (settings.accelerationModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Acceleration Group 3
                if (settings.accelerationModes3) {
                    if (settings.accelerationModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "acceleration"
        case "water":
            if (index == 1) {
                //Water Group 1
                if (settings.waterModes1) {
                    if (settings.waterModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Water Group 2
                if (settings.waterModes2) {
                    if (settings.waterModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Water Group 3
                if (settings.waterModes3) {
                    if (settings.waterModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "water"
        case "smoke":
            if (index == 1) {
                //Smoke Group 1
                if (settings.smokeModes1) {
                    if (settings.smokeModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Smoke Group 2
                if (settings.smokeModes2) {
                    if (settings.smokeModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Smoke Group 3
                if (settings.smokeModes3) {
                    if (settings.smokeModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "smoke"
        case "button":
            if (index == 1) {
                //Button Group 1
                if (settings.buttonModes1) {
                    if (settings.buttonModes1.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 2) {
                //Button Group 2
                if (settings.buttonModes2) {
                    if (settings.buttonModes2.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
            if (index == 3) {
                //Button Group 3
                if (settings.buttonModes3) {
                    if (settings.buttonModes3.contains(location.mode)) {
                        //Custom mode for this event is in use and we are in one of those modes
                        return true
                    } else {
                        //Custom mode for this event is in use and we are not in one of those modes
                        return false
                    }
                } else {
                    return (settings.speechModesDefault.contains(location.mode)) //True if we are in an allowed Default mode, False if not
                }
            }
        //End: case "button"
            
    } //End: switch (devicetype)
}

def TalkQueue(phrase, customSpeechDevice, evt){
    //IN DEVELOPMENT
    // Already talking or just recently (within x seconds) started talking
    // Queue up current request(s), give time for current action to complete, then speak and flush queue
    LOGDEBUG("TALKQUEUE()")
}

def LOGDEBUG(txt){
    log.debug("${app.label.replace(" ","").toUpperCase()}(${state.appversion}) || ${txt}")
}
def LOGTRACE(txt){
    log.trace("${app.label.replace(" ","").toUpperCase()}(${state.appversion}) || ${txt}")
}
def setAppVersion(){
    state.appversion = "1.0.3-Alpha8"
}

 /*
FEATURE REQUESTS:
  - Multiple home mode change groups for various configurations (To prevent overlap, may need to exclude mode selections from being used in other groups; exclusions can be used in all groups)
  - AND/OR logic for devices within a device group (require all devices in a group to be in a state before talking or talk on individual device state changes)

CHANGE LOG for 1.0.3-Alpha1
   12/26/2014 - Acceleration (active/inactive) event added and tested
   12/26/2014 - Water (wet/dry) event added
   12/26/2014 - Smoke (detected/clear/tested) event added
   12/26/2014 - Button (press) event added, to be tested...
CHANGE LOG for 1.0.3-Alpha2
   12/27/2014 - Adjusted some debug/trace log info
   12/27/2014 - Added default "talk while in mode(s)" with custom mode overrides for each event group.
   12/27/2014 - Status page: add defaults, cleanup look
CHANGE LOG for 1.0.3-Alpha3   
   12/27/2014 - Added Volume Change (supported for Sonos, VLC-Thing, not supported for Ubi due to lack of support in it's device type)
CHANGE LOG for 1.0.3-Alpha4
   12/27/2014 - BugFix: Corrected small bug on status page
CHANGE LOG for 1.0.3-Alpha5
   1/2/2015 - BugFix: VLCThing reporting "stopped" instead of "disconnected" therefore it was calling "playTextAndResume" and cutting off phrases.  Adjusted to playText if no trackdata found.
   1/4/2015 - BugFix: Switch Group 3 was not working.  onSwitch3Event() function missing; Added.  Thanks GitHub @roblandry (Issue #5).
   1/4/2015 - Feature: Mode change exclusion: Remain silent when changed to a configured mode, when coming from an excluded mode.  Thanks for the request SmartThingsCommunity:Greg.
CHANGE LOG for 1.0.3-Alpha6
   1/4/2015 - BugFix: Mode change exclusion contained a logic processing bug, corrected.
CHANGE LOG for 1.0.3-Alpha7
   1/6/2015 - BugFix: Ensure uninstall option is always available on the Configure page.
CHANGE LOG for 1.0.3-Alpha8
   1/6/2015 - BugFix: Mode change announcement may announce previous mode incorrectly.  Resolved.
 */