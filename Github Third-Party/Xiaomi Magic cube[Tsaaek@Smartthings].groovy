/**
 *  Xiaomi Magic cube control
 *
 *  Copyright 2018 Tommy Saaek
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
    name: "Xiaomi Magic cube control",
    namespace: "Magic cube control",
    author: "Tommy Saaek",
    description: "Smartapp for Xiaomi Magic cube.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Magic cube") {
	input "cube", "capability.button", title: "Cube", multiple: false, required: true
    }
    

    
        section("Face 0") {
   	input "slave1", "capability.switch", title: "Device 1", required: false
    input "knock1", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left1", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right1", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake1", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide1", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep1", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10",required: false
    input "colorSwitch1", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
    }
    
        section("Face 1") {
   	input "slave2", "capability.switch", title: "Device 2", required: false
    input "knock2", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left2", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right2", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake2", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide2", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep2", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10", required: false
    input "colorSwitch2", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
    }
    
        section("Face 2") {
   	input "slave3", "capability.switch", title: "Device 3", required: false
    input "knock3", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left3", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right3", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake3", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide3", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep3", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10", required: false
    input "colorSwitch3", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
    }
    
        section("Face 3") {
   	input "slave4", "capability.switch", title: "Device 4", required: false
    input "knock4", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left4", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right4", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake4", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide4", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep4", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10",  required: false
    input "colorSwitch4", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
    }
    
        section("Face 4") {
   	input "slave5", "capability.switch", title: "Device 5", required: false
    input "knock5", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left5", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right5", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake5", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide5", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep5", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10", required: false
    input "colorSwitch5", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
    }
    
        section("Face 5") {
   	input "slave6", "capability.switch", title: "Device 6", required: false
    input "knock6", "enum", title: "Knock", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "left6", "enum", title: "Rotate left", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "right6", "enum", title: "Rotate right", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "shake6", "enum", title: "Shake", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "slide6", "enum", title: "Slide", multiple: false, options: ["toggle", "turnOn", "turnOff", "dimUp", "dimDown", "nextColor"], required: false
    input "dimstep6", "number", title: "Stepsize if dimmer-function choosed", defaultValue: "10", required: false
    input "colorSwitch6", "capability.switch", title: "Device to toggle Hue colorpicker with",required: false
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
	subscribe(cube, "button", buttonHandler)
    subscribe(slave1, "switch.on", colorReset1)
    subscribe(slave2, "switch.on", colorReset2)
    subscribe(slave3, "switch.on", colorReset3)
    subscribe(slave4, "switch.on", colorReset4)
    subscribe(slave5, "switch.on", colorReset5)
    subscribe(slave6, "switch.on", colorReset6)
    
    state.slave1Color = 1
    state.slave2Color = 1
    state.slave3Color = 1
    state.slave4Color = 1
    state.slave5Color = 1
    state.slave6Color = 1
    }
    
/*
buttons 1 to 6 - “push” event on face 0 to 5 activation (corresponds to face pointing up)
buttons 7 to 12 - “push” event on slide gesture with faces 0 to 5 pointing up
buttons 13 to 18 - “push” event on knock gesture with faces 0 to 5 pointing up
buttons 19 to 24 - “push” event on right rotation with faces 0 to 5 pointing up
buttons 25 to 30 - “push” event on left rotation with faces 0 to 5 pointing up
buttons 31 to 36 - “push” event on shake gesture with faces 0 to 5 pointing up

shake + rotate spärras
*/

def buttonHandler(evt) {

      def buttonNumber = evt.jsonData.buttonNumber
      def firstEventId = 0
	  def value = evt.value
	  //log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
	  log.debug "button: $buttonNumber, value: $value"
		sortingHandler(buttonNumber)
}

def sortingHandler(buttonNumber){

log.debug "Sortinghandler $buttonNumber"
	switch(buttonNumber){
    
    /* Flipped to side*/
    case "1":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    case "2":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    case "3":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    case "4":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    case "5":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    case "6":
    state.timedOut = false
    runIn(30, timeOut)
    break;
    
    /* Slide */
    
    case "7":
    actionHandler(slave1, slide1, dimstep1, colorSwitch1)
    break;
    
    case "8":
    actionHandler(slave2, slide2, dimstep2, colorSwitch2)
    break;
    
    case "9":
    actionHandler(slave3, slide3, dimstep3, colorSwitch3)
    break;
   
    case "10":
    actionHandler(slave4, slide4, dimstep4, colorSwitch4)
    break;
   
    case "11":
    actionHandler(slave5, slide5, dimstep5, colorSwitch5)
    break;
    
    case "12":
    actionHandler(slave6, slide6, dimstep6, colorSwitch6)
    break;
    
    /* Knock */
    
    case "13":
    actionHandler(slave1, knock1, dimstep1, colorSwitch1)
    break;
    
    case "14":
    actionHandler(slave2, knock2, dimstep2, colorSwitch2)
    break;
    
    case "15":
    actionHandler(slave3, knock3, dimstep3, colorSwitch3)
    break;
   
    case "16":
    actionHandler(slave4, knock4, dimstep4, colorSwitch4)
    break;
   
    case "17":
    actionHandler(slave5, knock5, dimstep5, colorSwitch5)
    break;
    
    case "18":
    actionHandler(slave6, knock6, dimstep6, colorSwitch6)
    break;
    
    /* Rotate right */
    
    case "19":
    if (state.timedOut == false){
    actionHandler(slave1, right1, dimstep1, colorSwitch1)
    }
    break;
    
    case "20":
    if (state.timedOut == false){
    actionHandler(slave2, right2, dimstep2, colorSwitch2)
    }
    break;
    
    case "21":
    if (state.timedOut == false){
    actionHandler(slave3, right3, dimstep3, colorSwitch3)
    }
    break;
   
    case "22":
    if (state.timedOut ==false){
    actionHandler(slave4, right4, dimstep4, colorSwitch4)
    }
    break;
   
    case 23:
    log.debug "case 23"
    log.debug dimstep5
    if (state.timedOut == false){
    actionHandler(slave5, right5, dimstep5, colorSwitch5)}
    else if(state.timedOut == true){
    log.debug "timed out"
    }
    
    break;
    
    case "24":
    if (state.timedOut == false){
    actionHandler(slave6, right6, dimstep6, colorSwitch6)
    }
    break;
    
    /* Rotate Left */
    
        
    case "25":
    if (state.timedOut == false){
    actionHandler(slave1, left1, dimstep1, colorSwitch1)
    }
    break;
    
    case "26":
    if (state.timedOut == false){
    actionHandler(slave2, left2, dimstep2, colorSwitch2)
    }
    break;
    
    case "27":
    if (state.timedOut == false){
    actionHandler(slave3, left3, dimstep3, colorSwitch3)
    }
    break;
   
    case "28":
    if (state.timedOut == false){
    actionHandler(slave4, left4, dimstep4, colorSwitch4)
    }
    break;
   
    case "29":
    if (state.timedOut == false){
    actionHandler(slave5, left5, dimstep5, colorSwitch5)
    }
    break;
    
    case "30":
    if (state.timedOut == false){
    actionHandler(slave6, left6, dimstep6, colorSwitch6)
    }
    break;
   	
    /* Shake */
    
        
    case "31":
    if (state.timedOut == false){
    actionHandler(slave1, shake1, dimstep1, colorSwitch1)
    }
    break;
    
    case "32":
    if (state.timedOut == false){
    actionHandler(slave2, shake2, dimstep2, colorSwitch2)
    }
    break;
    
    case "33":
    if (state.timedOut == false){
    actionHandler(slave3, shake3, dimstep3, colorSwitch3)
    }
    break;
   
    case "34":
    if (state.timedOut == false){
    actionHandler(slave4, shake4, dimstep4, colorSwitch4)
    }
    break;
   
    case "35":
    if (state.timedOut == false){
    actionHandler(slave5, shake5, dimstep5, colorSwitch5)
    }
    break;
    
    case "36":
    if (state.timedOut == false){
    actionHandler(slave6, shake6, dimstep6, colorSwitch6)
    }
    break;
    
    // lägg in övriga här
   
    }
    }
    
def actionHandler(devices, action, dimstep, colorSwitch) {

log.debug "actionHandler $devices, $action, $dimstep"

	state.timedOut = false
	runIn(30, timeOut)

	switch(action) {
    case "toggle":
    	if (devices*.currentValue('switch').contains('off')) {
		devices.on()
    	}
    	else if (devices*.currentValue('switch').contains('on')) {
        devices.off()
  	  	}
    	else {
		devices.on()
    	}
        log.debug "$devices toggled"
    break;
    
    case "turnOn":
    	devices.on()
    break;
    
    case "turnOff":
    	devices.off()
    break;
    
    case "dimUp":
    log.debug "dimUp $devices with $dimstep %"
    	dimmerHandler(devices, "up", dimstep)
    break;
    
    case "dimDown":
    	dimmerHandler(devices, "down",  dimstep)
	break;
    
    case "nextColor":
    	colorSwitch.on()
    break;
    
    }
}

def dimmerHandler(devices, direction, dimstep){

log.debug "dimmerHandler $devices, $direction, $dimstep"

	def currentLevel = devices*.currentLevel[0].toInteger()
    log.debug "currentlevel $currentLevel"
    
    switch("dimstep"){
    	case "10":
        log.debug "case 10"
        break;
        }
    
    switch(direction) {
    	case "up":
        log.debug "case up"
        def nextLevel = currentLevel + dimstep
        log.debug "going up from $currentLevel to $nextLevel"
        devices.setLevel(nextLevel)
    	break;
        
        case "down":
        def nextLevel = currentLevel - dimstep
        devices.setLevel(nextLevel)
    	break;
        
       }

	runIn(30, timeOut)
    
 }
 

 
 def timeOut() {
 	state.timedOut = true
    log.debug "Time out"
   	}
