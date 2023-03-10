/**
 *  Copyright 2015 AdamV, 2017 PBalogh
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
 *  Assign Buttons to Wireless Wall controllers
 *
 *  TODO
 *  Get better dimming frequency than 1Hz
 *  Turn into Parent / Child app to avoid clutter for multiple switches
 *
 *  Version 2.0
 *  Author: PBalogh
 *  Date 2017-01-23
 *
 *  Changes since 1.6:
 *  - simplified to handle one button per instance. You can add four apps to the same wall controller and select the button number (1-4) in each smartapp
 *  - Improved dimming with a time-based approach
 *  - Added switchon and switchoff devices, so for example the up button will always swich the lights on, never off
 *  - If a light is dimmed and has switchon as well, it'll also set it to max brightness if pressed again while already on
 *  - Added safeguards for dimming, so it'll never overflow/underflow, which could cause trouble
 *
 *  Version 1.6 and earlier by author: AdamV
 *
 *  Changes since 1.5:
 *  - Added more options for routines based off various click events
 *	
 *	Changes since 1.4:
 *	- Click Hold action now works as well! Thanks to Miles Frankland
 *	- Cleaned up log reporting
 * 
 *	Changes since 1.3:
 *	- Double Clicks working now! Thanks to Stuart Buchanan
 *	- Cleaned up labels in setup
 *  To set colour and level of lights on push/hold events, connect to a routine, use smart lighting or Rule Machine
 */
 
definition(
    name: "One button controller with dimming, double clicks, & click-holds",
    namespace: "Devolo",
    author: "AdamV and PBalogh",
    description: "Assign events to button pushes, hold start, whilst held, & hold end to swicthes and level switches.For Z-Wave.me Secure Wireless Wall controller (ZME_WALLC-S), Z-Wave.me Wall controller 2 (ZME_WALLC-2), Popp Wall C Forever, Devolo Wall Switch & Z-Wave.me Key Fob",
    category: "Convenience",
    iconUrl: "http://94.23.40.33/smartthings/assets/rocker.png",
	iconX2Url: "http://94.23.40.33/smartthings/assets/rocker.png",
	iconX3Url: "http://94.23.40.33/smartthings/assets/rocker.png",
)

preferences {
    page(name: "selectController")
    page(name: "configureButton")
}

def selectController() {
    state.dimMsPerDim = 30
    dynamicPage(name: "selectController", title: "First, select your button device", install: true, uninstall: true) {
        section([mobileOnly:true]) {
            label title: "Name this Switch configuration:", required: true
            }
        section {
            input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
			state.smoothness = 1
            state.dimDelay = 1000
			}
        section {
            paragraph "\n Select button number (1..4)"
            input "buttonnumber", "number", title: "Button number", required: true, range: "1..4", submitOnChange: true
            state.buttonnumber = buttonnumber
            }
        section {
            input "dimIncrement", "number", title: "Dimming Increment (5-50)", required: false, range: "5..50", submitOnChange: true
                if(dimIncrement > 0) {
                    state.dimIncrement = dimIncrement
                }
                else if(!dimIncrement) {
                    state.dimIncrement = 10
                }
            }
            section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

            def timeLabel = timeIntervalLabel()

            href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
                options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }

        def phrases = location.helloHome?.getPhrases()*.label
            if (phrases) {
                section("Button Press") {
                    log.trace phrases
                    input "DevicePress", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
                    input "DevicePressOn", "capability.switch", title: "Device(s) to switch on", multiple: true, required: false
                    input "DevicePressOff", "capability.switch", title: "Device(s) to switch off", multiple: true, required: false
                    input "DevicePressDimUp", "capability.switch", title: "Device(s) to increment up", multiple: true, required: false
                    input "DevicePressRoutine", "enum", title: "Routine(s) to trigger", required: false, options: phrases
                }                 
        section ("On button hold (only fired once)")  {
            input "DeviceLongholdSwitch", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
            input "DeviceLongholdRoutine", "enum", title: "Routine(s) to trigger", required: false, options: phrases
            input "DeviceLongholdDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceLongholdDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
            }
        section ("Whilst button is held (fired every 1s whilst held)")  {
            input "DeviceHeldDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceHeldDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
            }
        section ("When button is released")  {
            input "DeviceReleaseSwitch", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
            input "DeviceReleaseDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceReleaseDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
        	}
       	section ("When button is double clicked")  {
            input "DeviceDoubleSwitch", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
            input "DeviceDoubleRoutine", "enum", title: "Routine(s) to trigger", required: false, options: phrases
            input "DeviceDoubleDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceDoubleDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
        	}
        section ("When button is click-held (only fired once)")  {
            input "DeviceClickholdSwitch", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
            input "DeviceClickholdRoutine", "enum", title: "Routine(s) to trigger", required: false, options: phrases
            input "DeviceClickholdDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceClickholdDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
            }
        section ("When button is click-held-released (only fired once)")  {
            input "DeviceClickholdreleaseSwitch", "capability.switch", title: "Device(s) to switch on/off", multiple: true, required: false
            input "DeviceClickholdreleaseDimUp", "capability.switchLevel", title: "Device(s) to Dim / Roll Up", multiple: true, required: false
            input "DeviceClickholdreleaseDimDown", "capability.switchLevel", title: "Device(s) to Dim / Roll Down", multiple: true, required: false
            }
        section {
            input "debug", "number", title: "Debug", multiple: false, required: false
			}
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(buttonDevice, "button", buttonEvent)
}

def configured() {
    return buttonDevice || buttonConfigured
}

def buttonConfigured() {
    return settings["DeviceHoldStartDimUp"] ||
        settings["DeviceHoldStartDimUp"]

}

def buttonEvent(evt){
    if(allOk) {
        if (state.debug) log.debug(evt)
        if (state.debug) log.debug(evt.data)
        
        String [] extra = evt.data.split( "," )
        String extrapayload = extra[ 0 ]  

        String [] sections = extrapayload.split( ":" )
    //    log.debug("sections: $sections")

        String payload = sections[ 1 ]
        if (state.debug) log.debug( "Command: $payload" )
        
        String payload2 = payload.replaceAll("[/}/g]","")
        Integer payload3 = payload2.toInteger()
        
        def buttonNumber = payload3 // why doesn't jsonData work? always returning [:]
        if (buttonNumber!=state.buttonnumber) 
        {
        	if (state.debug) log.debug ("Not for me");
        	return;
        }
        def value = evt.value
      if (state.debug) log.debug "button: $buttonNumber, value: $value"

        if (DeviceHeldDimUp != null) {
        	if(DeviceHeldDimUp[0].currentSwitch == "off") atomicState.deviceHeldDimUpLevel = 0
    		else atomicState.deviceHeldDimUpLevel = DeviceHeldDimUp[0].currentLevel
            }
        if (DeviceHeldDimDown != null) {
        	if(DeviceHeldDimDown[0].currentSwitch == "off") atomicState.deviceHeldDimDownLevel = 0
    		else atomicState.deviceHeldDimDownLevel = DeviceHeldDimDown[0].currentLevel
            }
        executeHandlers(buttonNumber, value)
    }
}

def startPulsing() 
{
	state.dimMsPerDim = 40
    Integer currentTime = now(); //milliseconds, please. 
    if( atomicState.currentButton == -1 || atomicState.buttonIsHolding == false || atomicState.direction == null )
    { 
    	unschedule("startPulsing")
    	return 
    } 

	Integer dt = currentTime - atomicState.startHoldTime;
    if( dt >= state.dimMsPerDim && dt>100) 
    {
        if (dt>1000) dt = 1000;
        atomicState.startHoldTime = (Integer)currentTime;
        atomicState.pulseNumber = atomicState.pulseNumber + 1
        def diff = (Integer)dt/(Integer)state.dimMsPerDim;
        if (state.debug) log.debug("pulse diff $diff")
        if (atomicState.direction == "Up")
        {
            def newLevel = incLevel(atomicState.deviceHeldDimUpLevel,diff);
            if (atomicState.deviceHeldDimUpLevel != newLevel)
            {
                atomicState.deviceHeldDimUpLevel = newLevel;
                DeviceHeldDimUp.setLevel(newLevel);
            }
        }
        else if (atomicState.direction == "Down")
        {
            def newLevel = decLevel(atomicState.deviceHeldDimDownLevel,diff);
            if (atomicState.deviceHeldDimDownLevel != newLevel)
            {
                atomicState.deviceHeldDimDownLevel = newLevel;
                DeviceHeldDimDown.setLevel(newLevel);
            }
        }
    }
        
    if (currentTime - atomicState.startHoldTime<20000)
    {
	    runIn( 1, startPulsing )
    }
}

def incLevel(Integer dimLevel, Integer dimIncrement)
{
	Integer newLevel = (Integer)dimLevel+(Integer)dimIncrement;
    if (newLevel>99) newLevel = 99;
    if (newLevel<1) newLevel = 1;
    return newLevel;
}

def decLevel(dimLevel, dimDecrement)
{
	def newLevel = dimLevel-dimDecrement;
    if (newLevel<1) newLevel = 1;
    if (newLevel>99) newLevel = 99;
    return newLevel;
}

def MySleep (int ms)
{
	Integer startms = now()+ms;
	while (startms>now());
}

def onButtonHoldStart() {
	if (!atomicState.buttonIsHolding)
    {
        atomicState.buttonIsHolding = true
        atomicState.startHoldTime = (Integer)now()
        atomicState.pulseNumber = 0
 	    if (state.debug) log.debug ("<<< Hold start")
        startPulsing()
    }
}

def onButtonHoldEnd() {
    if (atomicState.buttonIsHolding)
    {
        if (state.debug) log.debug (">>> Hold end")
        startPulsing();
        atomicState.buttonIsHolding = false;
        atomicState.currentButton = -1;
        atomicState.direction = null
    }
}

def executeHandlers(buttonNumber, value) {
    if (state.debug) log.debug "executeHandlers: $buttonNumber - $value"
    if (value == "pushed") {
        onButtonHoldEnd()
        if (DevicePress != null) toggle(DevicePress)
        if (DevicePressOn != null) toggleOn(DevicePressOn)
        if (DevicePressOff != null) toggleOff(DevicePressOff)
        if (DevicePressRoutine != null) location.helloHome?.execute(settings.DevicePressRoutine)
        if (DevicePressDimUp != null) {
            def newLevel = DevicePressDimUp[0].currentLevel = incLevel(DevicePressDimUp[0].currentLevel, state.dimIncrement);
            DevicePressDimUp.setLevel(newLevel)
        }
        if (state.debug) log.debug "$buttonNumber $value"
    }
    else if (value == "held") {
        atomicState.currentButton = buttonNumber
        if (DeviceLongholdSwitch != null) toggle(DeviceLongholdSwitch)
        if (DeviceLongholdRoutine != null) location.helloHome?.execute(settings.DeviceLongholdRoutine)
        if (DeviceLongholdDimUp != null) {
            def newLevel = DeviceLongholdDimUp[0].currentLevel = incLevel(DeviceLongholdDimUp[0].currentLevel, state.dimIncrement);
            if (state.debug) log.debug("newLevel is $newLevel")
            DeviceLongholdDimUp.setLevel(newLevel)
            // Device1longholdDimUp.levelUp()
            if (state.debug) log.debug "Button $buttonNumber long hold going up"
        }
        if (DeviceLongholdDimDown != null) {
            def newLevel = DeviceLongholdDimDown[0].currentLevel = decLevel(DeviceLongholdDimDown[0].currentLevel, state.dimIncrement);
            DeviceLongholdDimDown.setLevel(newLevel)
            // Device1longholdDimDown.levelDown()
            if (state.debug) log.debug "Button $buttonNumber long hold going down"
        }
        if (DeviceHeldDimUp != null) {
            atomicState.direction = "Up"
            if (state.debug) log.debug "Button $buttonNumber Hold Start to go up"
        }
        if (DeviceHeldDimDown != null) {
            atomicState.direction = "Down"
            if (state.debug) log.debug "Button $buttonNumber Hold Start to go down"
        }
        onButtonHoldStart()
        if (state.debug) log.debug "$buttonNumber $value"
    }
    else if (value == "holdRelease") {         
        onButtonHoldEnd()
        if (DeviceReleaseSwitch != null) toggle(DeviceReleaseSwitch)
        if (DeviceReleaseDimUp != null) {
            //     Device1ReleaseDimUp.levelUp()
            def newLevel = DeviceReleaseDimUp[0].currentLevel = incLevel(DeviceReleaseDimUp[0].currentLevel, state.dimIncrement);
            DeviceReleaseDimUp.setLevel(newLevel)
        }
        if (DeviceReleaseDimDown != null) {
            //     Device1ReleaseDimDown.levelDown()
            def newLevel = DeviceReleaseDimDown[0].currentLevel = decLevel(DeviceReleaseDimDown[0].currentLevel, state.dimIncrement);
            DeviceReleaseDimDown.setLevel(newLevel)
        }
        if (state.debug) log.debug "$buttonNumber $value"
    }
    else if (value == "doubleClick") {
        onButtonHoldEnd()
        if (DeviceDoubleSwitch != null) toggle(DeviceDoubleSwitch)
        if (DeviceDoubleRoutine != null) location.helloHome?.execute(settings.DeviceDoubleRoutine)
        if (DeviceDoubleDimUp != null) {
            def newLevel = DeviceDoubleDimUp[0].currentLevel = incLevel(DeviceDoubleDimUp[0].currentLevel, state.dimIncrement);
            DeviceDoubleDimUp.setLevel(newLevel)
        }
        if (DeviceDoubleDimDown != null) {
            def newLevel = DeviceDoubleDimDown[0].currentLevel = decLevel(DeviceDoubleDimDown[0].currentLevel, state.dimIncrement);
            DeviceDoubleDimDown.setLevel(newLevel)
        }
        if (state.debug) log.debug "$buttonNumber $value"
    }
    else if (value == "clickHoldStart") {
        onButtonHoldEnd()
        atomicState.currentButton = buttonNumber
        if (DeviceClickholdSwitch != null) toggle(DeviceClickholdSwitch)
        if (DeviceClickholdRoutine != null) location.helloHome?.execute(settings.DeviceClickholdRoutine)
        if (DeviceClickholDimUp != null) {
            def newLevel = DeviceClickholdDimUp[0].currentLevel = incLevel(DeviceClickholdDimUp[0].currentLevel, state.dimIncrement);
            if (state.debug) log.debug("newLevel is $newLevel")
            DeviceClickholdDimUp.setLevel(newLevel)
            // Device1longholdDimUp.levelUp()
            if (state.debug) log.debug "Button $buttonNumber click hold start going up"
        }
        if (DeviceClickholdDimDown != null) {
            def newLevel = DeviceClickholdDimDown[0].currentLevel = decLevel(DeviceClickholdDimDown[0].currentLevel, state.dimIncrement);
            DeviceClickholDimDown.setLevel(newLevel)
            // Device1longholdDimDown.levelDown()
            if (state.debug) log.debug "Button $buttonNumber click hold start going down"
        }
    }
    else if (value == "clickHoldStop") {
        onButtonHoldEnd()
        atomicState.currentButton = buttonNumber
        if (DeviceClickholdreleaseSwitch != null) toggle(DeviceLongholdSwitch)
        if (DeviceClickholdreleaseDimUp != null) {
            def newLevel = DeviceClickholdreleaseDimUp[0].currentLevel = incLevel(DeviceClickholdreleaseDimUp[0].currentLevel, state.dimIncrement);
            if (state.debug) log.debug("newLevel is $newLevel")
            DeviceClickholdreleaseDimUp.setLevel(newLevel)
            // Device1longholdDimUp.levelUp()
            if (state.debug) log.debug "Button $buttonNumber long hold going up"
        }
        if (DeviceClickholdreleaseDimDown != null) {
            def newLevel = DeviceClickholdreleaseDimDown[0].currentLevel = decLevel(DeviceClickholdreleaseDimDown[0].currentLevel, state.dimIncrement);
            DeviceClickholdreleaseDimDown.setLevel(newLevel)
            // Device1longholdDimDown.levelDown()
            if (state.debug) log.debug "Button $buttonNumber long hold going down"
        }
    }
    else
    {
        if (state.debug) log.debug ("WHAT?")
        onButtonHoldEnd()
    }
}

def toggleOn(devices) {
    if (state.debug) log.debug "toggleOn: $devices = ${devices*.currentValue('switch')}"

    if (devices*.currentValue('switch').contains('on')) {
    	if (DeviceHeldDimUp) {
	        DeviceHeldDimUp.setLevel(99)
        }
    }
	devices.on()
}

def toggleOff(devices) {
    if (state.debug) log.debug "toggleOff: $devices = ${devices*.currentValue('switch')}"

    devices.off()
}

def toggle(devices) {
    if (state.debug) log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

    if (devices*.currentValue('switch').contains('on')) {
        devices.off()
    }
    else if (devices*.currentValue('switch').contains('off')) {
        devices.on()
    }
    else if (devices*.currentValue('lock').contains('locked')) {
        devices.unlock()
    }
    else if (devices*.currentValue('alarm').contains('off')) {
        devices.siren()
    }
    else {
        devices.on()
    }
}
// execution filter methods
private getAllOk() {
    modeOk && daysOk && timeOk
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
//  log.trace "modeOk = $result"
    result
}

private getDaysOk() {
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
//  log.trace "daysOk = $result"
    result
}

private getTimeOk() {
    def result = true
    if (starting && ending) {
        def currTime = now()
        def start = timeToday(starting).time
        def stop = timeToday(ending).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
//  log.trace "timeOk = $result"
    result
}

private hhmm(time, fmt = "h:mm a")
{
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private hideOptionsSection() {
    (starting || ending || days || modes) ? false : true
}

private timeIntervalLabel() {
    (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
