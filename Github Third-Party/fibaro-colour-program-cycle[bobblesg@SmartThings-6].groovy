/**
 *  ****************  Fibaro Colour/Program Cycle  ****************
 *
 *  Design Usage:
 *  This was designed to be used with the excellent Fibaro RGBW DTH by: 'codersaur' (David Lomas)
 *  With this app you can assign a virtual switch to turn on/off Fibaro and cycle through colours or programs
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
 *  Last Update: 17/08/2017
 *
 *  Changes:
 *
 * 
 *
 *
 *  V1.2.0 - added up to 4 colours/programs
 *  V1.0.0 - POC
 *
 */



definition(
    name: "Fibaro Colour/Program Cycle",
    namespace: "Cobra",
    author: "AJ Parker",
    description: "Turn On/Off Fibaro RGBW Controller's colour (and brightness) and cycle between 4 colours",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)


section("") {
        paragraph "V1.2.0"
        paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                  title: "Fibaro Colour/Program Cycle",
                  required: false,
                  " This app allows you to use a virtual switch to cycle LED colours or the built-in Fibaro programs \r\n Specifically designed to work with the excellent DTH by 'codersaur' (David Lomas)\r\n Dim Levels and White Levels do NOT work with 'Programs' (or Cold/Warm Whites) and should be left as default\r\n The virtual switche can then be used in other apps "
    }


preferences {
	section(){
		input "switch1", "capability.switch", title: "On/Off switch"
	}
	section(){
		input "fibaro1", "capability.switch", title: "Fibaro Controller", required: true, multiple: true
        }
    section(){
		input "setColProg1", "enum", title: "1st Colour or Program",required: true, options: [ "Red", "Blue", "Green", "White", "Fireplace", "Storm", "Deepfade","Litefade", "Police", "WarmWhite", "ColdWhite", ]
       	input "setColProg2", "enum", title: "2nd Colour or Program",required: false, options: [ "Red", "Blue", "Green", "White", "Fireplace", "Storm", "Deepfade","Litefade", "Police", "WarmWhite", "ColdWhite", ]
        input "setColProg3", "enum", title: "3rd Colour or Program",required: false, options: [ "Red", "Blue", "Green", "White", "Fireplace", "Storm", "Deepfade","Litefade", "Police", "WarmWhite", "ColdWhite", ]
        input "setColProg4", "enum", title: "4th Colour or Program",required: false, options: [ "Red", "Blue", "Green", "White", "Fireplace", "Storm", "Deepfade","Litefade", "Police", "WarmWhite", "ColdWhite", ]
        } 
     section(){
        input "delay1", "number", title: "How long to keep 1st Colour or Program on? (seconds)", defaultValue: "10", multiple: false, required: true
        input "delay2", "number", title: "How long to keep 2nd Colour or Program on? (seconds)", defaultValue: "10", multiple: false, required: true
        input "delay3", "number", title: "How long to keep 3rd Colour or Program on? (seconds)", defaultValue: "10", multiple: false, required: true
        input "delay4", "number", title: "How long to keep 4th Colour or Program on? (seconds)", defaultValue: "10", multiple: false, required: true
	}
	section(){
        input "dimLevel1", "number", title: "Dim Level (0-100)", defaultValue: "100", multiple: false, required: true
	}
    section(){
        input "whiteLevel1", "number", title: "Add some white to the colours? (0-100)", defaultValue: "0", multiple: false, required: true
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

		subscribe(switch1, "switch.on", switchOnHandler)
        subscribe(switch1, "switch.off", switchOffHandler)
           }

def switchOnHandler(evt) {
state.control1  = evt.value
log.debug "state.control1 = $state.control1"
log.debug "$evt.value: $evt, $settings"
picker1()
}


def switchOffHandler(evt) {
state.control1  = evt.value
log.debug "state.control1 = $state.control1"
	log.debug "$evt.value: $evt, $settings"
	log.trace "Turning off $fibaro1"
    	fibaro1.setLevelRed(0)
		fibaro1.setLevelBlue(0)
		fibaro1.setLevelGreen(0)
		fibaro1.setLevelWhite(0)
		fibaro1.off()
}

// Configure colour or program for 1st colour
def picker1(){

fibaro1.off()
	def progcol = setColProg1
	def myLevel1 = dimLevel1 as int
	def addWhite = whiteLevel1 as int


// Colours

if (progcol == "Red" ){
log.trace "Turning $fibaro1 'RED' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"
fibaro1.setLevelRed(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)


}
else if (progcol == "Blue" ){
log.trace "Turning $fibaro1 'BLUE' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelBlue(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}

else if (progcol == "Green" ){
log.trace "Turning $fibaro1 'GREEN' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelGreen(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}
else if (progcol == "White" ){
log.trace "Turning $fibaro1 WHITE with Level: $myLevel1 %"

fibaro1.setLevelWhite(myLevel1)
}

// Programs
else if (progcol == "Fireplace" ){
log.trace "Turning $fibaro1 'Fireplace'"
fibaro1.startFireplace()

}
else if (progcol == "Storm" ){
log.trace "Turning $fibaro1 'Storm'"
fibaro1.startStorm()

}
else if (progcol == "Police" ){
log.trace "Turning $fibaro1 'Police'"
fibaro1.startPolice()

}
else if (progcol == "Deepfade" ){
log.trace "Turning $fibaro1 'Deepfade'"
fibaro1.startDeepFade()

}
else if (progcol == "Litefade" ){
log.trace "Turning $fibaro1 'Litefade'"
fibaro1.startLiteFade()

}

else if (progcol == "WarmWhite" ){
log.trace "Turning $fibaro1 WARMWHITE with Level: 100%"

fibaro1.warmWhite()
}
else if (progcol == "ColdWhite" ){
log.trace "Turning $fibaro1 COLDWHITE with Level: 100%"

fibaro1.coldWhite()
}
if(state.control1 == "on"){
def myDelay1 = delay1
runIn(myDelay1, picker2)
}
else if(state.control1 == "off"){
log.debug " End of 1st cycle and state.control1 is: $state.control1"
fibaro1.off()
}

}


// Configure colour or program for 2nd colour
def picker2(){

fibaro1.off()
	def progcol = setColProg2
	def myLevel1 = dimLevel1 as int
	def addWhite = whiteLevel1 as int


// Colours

if (progcol == "Red" ){
log.trace "Turning $fibaro1 'RED' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"
fibaro1.setLevelRed(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)


}
else if (progcol == "Blue" ){
log.trace "Turning $fibaro1 'BLUE' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelBlue(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}

else if (progcol == "Green" ){
log.trace "Turning $fibaro1 'GREEN' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelGreen(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}
else if (progcol == "White" ){
log.trace "Turning $fibaro1 WHITE with Level: $myLevel1 %"

fibaro1.setLevelWhite(myLevel1)
}

// Programs
else if (progcol == "Fireplace" ){
log.trace "Turning $fibaro1 'Fireplace'"
fibaro1.startFireplace()

}
else if (progcol == "Storm" ){
log.trace "Turning $fibaro1 'Storm'"
fibaro1.startStorm()

}
else if (progcol == "Police" ){
log.trace "Turning $fibaro1 'Police'"
fibaro1.startPolice()

}
else if (progcol == "Deepfade" ){
log.trace "Turning $fibaro1 'Deepfade'"
fibaro1.startDeepFade()

}
else if (progcol == "Litefade" ){
log.trace "Turning $fibaro1 'Litefade'"
fibaro1.startLiteFade()

}

else if (progcol == "WarmWhite" ){
log.trace "Turning $fibaro1 WARMWHITE with Level: 100%"

fibaro1.warmWhite()
}
else if (progcol == "ColdWhite" ){
log.trace "Turning $fibaro1 COLDWHITE with Level: 100%"

fibaro1.coldWhite()
}
if(state.control1 == "on"){
def myDelay2 = delay2
runIn(myDelay2, picker3)
}
else if(state.control1 == "off"){
log.debug " End of 2nd cycle and state.control1 is: $state.control1"
fibaro1.off()
}

}

// Configure colour or program for 3rd colour
def picker3(){

fibaro1.off()
	def progcol = setColProg3
	def myLevel1 = dimLevel1 as int
	def addWhite = whiteLevel1 as int


// Colours

if (progcol == "Red" ){
log.trace "Turning $fibaro1 'RED' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"
fibaro1.setLevelRed(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)


}
else if (progcol == "Blue" ){
log.trace "Turning $fibaro1 'BLUE' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelBlue(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}

else if (progcol == "Green" ){
log.trace "Turning $fibaro1 'GREEN' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelGreen(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}
else if (progcol == "White" ){
log.trace "Turning $fibaro1 WHITE with Level: $myLevel1 %"

fibaro1.setLevelWhite(myLevel1)
}

// Programs
else if (progcol == "Fireplace" ){
log.trace "Turning $fibaro1 'Fireplace'"
fibaro1.startFireplace()

}
else if (progcol == "Storm" ){
log.trace "Turning $fibaro1 'Storm'"
fibaro1.startStorm()

}
else if (progcol == "Police" ){
log.trace "Turning $fibaro1 'Police'"
fibaro1.startPolice()

}
else if (progcol == "Deepfade" ){
log.trace "Turning $fibaro1 'Deepfade'"
fibaro1.startDeepFade()

}
else if (progcol == "Litefade" ){
log.trace "Turning $fibaro1 'Litefade'"
fibaro1.startLiteFade()

}

else if (progcol == "WarmWhite" ){
log.trace "Turning $fibaro1 WARMWHITE with Level: 100%"

fibaro1.warmWhite()
}
else if (progcol == "ColdWhite" ){
log.trace "Turning $fibaro1 COLDWHITE with Level: 100%"

fibaro1.coldWhite()
}
if(state.control1 == "on"){
def myDelay3 = delay3
runIn(myDelay3, picker4)
}
else if(state.control1 == "off"){
log.debug " End of 3rd cycle and state.control1 is: $state.control1"
fibaro1.off()
}

}

// Configure colour or program for 2nd colour
def picker4(){

fibaro1.off()
	def progcol = setColProg4
	def myLevel1 = dimLevel1 as int
	def addWhite = whiteLevel1 as int


// Colours

if (progcol == "Red" ){
log.trace "Turning $fibaro1 'RED' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"
fibaro1.setLevelRed(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)


}
else if (progcol == "Blue" ){
log.trace "Turning $fibaro1 'BLUE' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelBlue(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}

else if (progcol == "Green" ){
log.trace "Turning $fibaro1 'GREEN' with level: $dimLevel1 and WHITE with Level: $whiteLevel1"

fibaro1.setLevelGreen(myLevel1)
fibaro1.setLevelWhite(whiteLevel1)
}
else if (progcol == "White" ){
log.trace "Turning $fibaro1 WHITE with Level: $myLevel1 %"

fibaro1.setLevelWhite(myLevel1)
}

// Programs
else if (progcol == "Fireplace" ){
log.trace "Turning $fibaro1 'Fireplace'"
fibaro1.startFireplace()

}
else if (progcol == "Storm" ){
log.trace "Turning $fibaro1 'Storm'"
fibaro1.startStorm()

}
else if (progcol == "Police" ){
log.trace "Turning $fibaro1 'Police'"
fibaro1.startPolice()

}
else if (progcol == "Deepfade" ){
log.trace "Turning $fibaro1 'Deepfade'"
fibaro1.startDeepFade()

}
else if (progcol == "Litefade" ){
log.trace "Turning $fibaro1 'Litefade'"
fibaro1.startLiteFade()

}

else if (progcol == "WarmWhite" ){
log.trace "Turning $fibaro1 WARMWHITE with Level: 100%"

fibaro1.warmWhite()
}
else if (progcol == "ColdWhite" ){
log.trace "Turning $fibaro1 COLDWHITE with Level: 100%"

fibaro1.coldWhite()
}
if(state.control1 == "on"){
def myDelay4 = delay4
runIn(myDelay4, picker1)
}
else if(state.control1 == "off"){
log.debug " End of 2nd cycle and state.control1 is: $state.control1"
fibaro1.off()
}

}