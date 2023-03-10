/**
 *  ****************  Fibaro Colour Switch  ****************
 *
 *  Design Usage:
 *  This was designed to be used with the excellent Fibaro RGBW DTH by: 'codersaur' (David Lomas)
 *  With this app you can assign a virtual switch to turn on/off Fibaro colours or programs
 *
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm very happy for you to use this app without a donation, but if you find it useful then it would be nice to get a 'shout out' on the forum! -  @Cobra
 *  You should also give a 'shout out' to @codersaur, as without his DTH, this app would not be possible
 *
 *  Have an idea to make this app better?  - Please let me know :)
 *
 *  Website: http://securendpoint.com/smartthings
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
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update: 16/08/2017
 *
 *  Changes:
 *
 * 
 *
 *	V1.1.1 - Added switchable logging
 *  V1.1.0 - Added custom colour ability
 *  V1.0.0 - POC
 *
 */



definition(
    name: "Fibaro Colour Switch",
    namespace: "Cobra",
    author: "SmartThings",
    description: "Turn On/Off Fibaro RGBW Controller's colour (and brightness) or program with a switch",
    category: "My Apps",
     iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
	iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
)


section("") {
        paragraph "V1.1.1"
     	paragraph image: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
                  title: "Fibaro Colour Switch",
                  required: false,
                  " This app allows you to use a virtual switch to turn on/off LED colours or the built-in Fibaro programs \r\n Specifically designed to work with the excellent DTH by 'codersaur' (David Lomas)\r\n Dim Levels and White Levels do NOT work with 'Programs' (or Cold/Warm Whites) and should be left as default\r\n These switches can then be used in other apps "
    }


preferences {
	section(){
		input "switch1", "capability.switch", title: "On/Off switch"
	}
	section(){
		input "fibaro1", "capability.switch", title: "Fibaro Controller", required: true, multiple: true
        }
    section(){
		input "setColProg1", "enum", title: "Colour or Program?",required: true, options: ["Custom", "Red", "Blue", "Green", "White", "Fireplace", "Storm", "Deepfade","Litefade", "Police", "WarmWhite", "ColdWhite", ]
        } 
	section(){
        input "dimLevel1", "number", title: "Dim Level (0-100)", defaultValue: "100", multiple: false, required: false
	}
    section(){
        input "whiteLevel1", "number", title: "Add some white? (0-100)", defaultValue: "0", multiple: false, required: false
	}
    
     section("If Using a Custom colour"){ 
        input "redDim1", "number", title: "How much RED? (0-100)", defaultValue: "0", multiple: false, required: false
        input "greenDim1", "number", title: "How much GREEN? (0-100)", defaultValue: "0", multiple: false, required: false
        input "blueDim1", "number", title: "How much BLUE? (0-100)", defaultValue: "0", multiple: false, required: false
        input "whiteDim1", "number", title: "How much White? (0-100)", defaultValue: "0", multiple: false, required: false
     }
     section("Logging"){
            input "debugmode", "bool", title: "Enable logging", required: true, defaultValue: false
        }
       
}


def installed()
{
	initialise()
}

def updated()
{
	unsubscribe()
	initialise()
}


def initialise(){
log.debug "Initialised with settings: ${settings}"
logCheck()
		subscribe(switch1, "switch.on", switchOnHandler)
        subscribe(switch1, "switch.off", switchOffHandler)
           }

def switchOnHandler(evt) {
picker()
}

def switchOffHandler(evt) {
	LOGDEBUG( "$evt.value: $evt, $settings")
	lLOGDEBUG("Turning off $fibaro1")
	fibaro1.off()
}

// Configure colour or program
def picker(){
	def progcol = setColProg1
	def myLevel1 = dimLevel1 as int
   	def addWhite = whiteLevel1 as int


// Colours


if (progcol == "Red" ){
LOGDEBUG("Turning $fibaro1 'RED' with level: $dimLevel1 and WHITE with Level: $whiteLevel1")
fibaro1.setLevelRed(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)


}
else if (progcol == "Blue" ){
LOGDEBUG("Turning $fibaro1 'BLUE' with level: $dimLevel1 and WHITE with Level: $whiteLevel1")

fibaro1.setLevelBlue(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}

else if (progcol == "Green" ){
LOGDEBUG( "Turning $fibaro1 'GREEN' with level: $dimLevel1 and WHITE with Level: $whiteLevel1")

fibaro1.setLevelGreen(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}
else if (progcol == "White" ){
LOGDEBUG( "Turning $fibaro1 WHITE with Level: $myLevel1 %")

fibaro1.setLevelWhite(myLevel1)
}



else if (progcol == "Custom" ){

//	def redDim1 = 10
//	def blueDim1 = 10
//	def greenDim1 = 10	
//    def whiteDim1 = 10


LOGDEBUG( "Turning on $fibaro1 'Custom Colour' ")
def customRedLevel1 = redDim1 
def customBlueLevel1 = blueDim1 
def customGreenLevel1 = greenDim1 
def customWhiteLevel1 = whiteDim1 


fibaro1.setLevelRed(customRedLevel1)
fibaro1.setLevelBlue(customBlueLevel1)
fibaro1.setLevelGreen(customGreenLevel1)
fibaro1.setLevelWhite(customWhiteLevel1)
}



// Programs
else if (progcol == "Fireplace" ){
LOGINFO( "Turning $fibaro1 'Fireplace'")
fibaro1.startFireplace()

}
else if (progcol == "Storm" ){
LOGINFO( "Turning $fibaro1 'Storm'")
fibaro1.startStorm()

}
else if (progcol == "Police" ){
LOGINFO( "Turning $fibaro1 'Police'")
fibaro1.startPolice()

}
else if (progcol == "Deepfade" ){
LOGINFO("Turning $fibaro1 'Deepfade'")
fibaro1.startDeepFade()

}
else if (progcol == "Litefade" ){
LOGINFO("Turning $fibaro1 'Litefade'")
fibaro1.startLiteFade()

}

else if (progcol == "WarmWhite" ){
LOGINFO("Turning $fibaro1 WARMWHITE with Level: 100%")

fibaro1.warmWhite()
}
else if (progcol == "ColdWhite" ){
LOGINFO("Turning $fibaro1 COLDWHITE with Level: 100%")

fibaro1.coldWhite()
}


}


def logCheck(){
state.checkLog = debugmode
if(state.checkLog == true){
log.info "All Logging Enabled"
}
else if(state.checkLog == false){
log.info "Further Logging Disabled"
}
}

def LOGDEBUG(txt){
    try {
    	if (settings.debugmode) { log.debug("${app.label.replace(" ","_").toUpperCase()}  (Version ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGDEBUG unable to output requested data!")
    }
}
def LOGINFO(txt){
    try {
    	if (settings.debugmode) { log.info("${app.label.replace(" ","_").toUpperCase()}  (Version ${state.appversion}) - ${txt}") }
    } catch(ex) {
    	log.error("LOGINFO unable to output requested data!")
    }
}


def setAppVersion(){
    state.appversion = "1.1.1"
}