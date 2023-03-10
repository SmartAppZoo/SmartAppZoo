/**
 *  ****************  Fibaro Bright/Dim Switch ****************
 *
 *  Design Usage:
 *  This was designed to be used with the excellent Fibaro RGBW DTH by: '@codersaur' (David Lomas)
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
 *
 *
 *  V1.0.0 - POC
 *
 */



definition(
    name: "Fibaro Bright/Dim Switch - Custom Colours",
    namespace: "Cobra",
    author: "SmartThings",
    description: "Turn On/Off Fibaro RGBW Controller's colour (and brightness) or program with a switch",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)


section("") {
        paragraph "V1.0.0"
        paragraph image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                  title: "Fibaro Bright/Dim Switch - Custom Colours",
                  required: false,
                  " This app allows you to use a virtual switch to turn on/off LED colours or the built-in Fibaro programs \r\n Specifically designed to work with the excellent DTH by 'codersaur' (David Lomas) \r\n These switches can then be used in other apps "
    }


preferences {
	section(){
		input "switch1", "capability.switch", title: "On/Off switch"
	}
    section(){
		input "switch2", "capability.switch", title: "Dim/Bright switch"
	}
	section(){
		input "fibaro1", "capability.switch", title: "Fibaro Controller", required: true, multiple: true
        }
  
      section("Custom colour 1"){ 
        input "redDim1", "number", title: "How much RED? (0-100)", defaultValue: "0", multiple: false, required: false
        input "greenDim1", "number", title: "How much GREEN? (0-100)", defaultValue: "0", multiple: false, required: false
        input "blueDim1", "number", title: "How much BLUE? (0-100)", defaultValue: "0", multiple: false, required: false
        input "whiteDim1", "number", title: "How much White? (0-100)", defaultValue: "0", multiple: false, required: false
          }
   section("Custom colour 2"){         
		 input "redDim2", "number", title: "How much RED? (0-100)", defaultValue: "0", multiple: false, required: false
        input "greenDim2", "number", title: "How much GREEN? (0-100)", defaultValue: "0", multiple: false, required: false
        input "blueDim2", "number", title: "How much BLUE? (0-100)", defaultValue: "0", multiple: false, required: false
        input "whiteDim2", "number", title: "How much White? (0-100)", defaultValue: "0", multiple: false, required: false
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

		subscribe(switch1, "switch.on", switchOnHandler1)
        subscribe(switch1, "switch.off", switchOffHandler1)
        subscribe(switch2, "switch.on", switchOnHandler2)
        subscribe(switch2, "switch.off", switchOffHandler2)
        
        
           }

def switchOnHandler1(evt) {
picker1()
}

def switchOffHandler1(evt) {
	log.debug "$evt.value: $evt, $settings"
	log.trace "Turning off $fibaro1"
    	fibaro1.setLevelRed(0)
		fibaro1.setLevelBlue(0)
		fibaro1.setLevelGreen(0)
		fibaro1.setLevelWhite(0)
		fibaro1.off()
}

def switchOnHandler2(evt) {
	picker2()
}

def switchOffHandler2(evt) {
	picker1()
}

// Configure colour or program
def picker1(){
	

log.trace "Turning $fibaro1 'Custom Colour' "
def customRedLevel1 = redDim1 as int
def customBlueLevel1 = blueDim1 as int
def customGreenLevel1 = greenDim1 as int
def customWhiteLevel1 = whiteDim1 as int


fibaro1.setLevelRed(customRedLevel1)
fibaro1.setLevelBlue(customBlueLevel1)
fibaro1.setLevelGreen(customGreenLevel1)
fibaro1.setLevelWhite(customWhiteLevel1)
}


// Configure colour or program
def picker2(){
log.trace "Turning $fibaro1 'Custom Colour' "
def customRedLevel2 = redDim2 as int
def customBlueLevel2 = blueDim2 as int
def customGreenLevel2 = greenDim2 as int
def customWhiteLevel2 = whiteDim2 as int


fibaro1.setLevelRed(customRedLevel2)
fibaro1.setLevelBlue(customBlueLevel2)
fibaro1.setLevelGreen(customGreenLevel2)
fibaro1.setLevelWhite(customWhiteLevel2)
}


