/** 
	Button Controller Plus
  	Copyright 2016  Author: SmartThings, modified by Bruce Ravenel, Dale Coffing 
       
   Control devices using the buttons of an Aeon Minimote, Key Fob, or HomeSeer HS-WD100+ and HS-WS100+
   multi-tap features so the double-tap, triple-tap, press & hold functionality can trigger 
   up to 6 home automation events. 
   
   The HomeSeer HS-WD100+ or HS-WS100+ needs device handlers installed that expose the 
   multi-tap features such as:
  @darwin 
  https://github.com/DarwinsDen/SmartThingsPublic/tree/master/devicetypes/darwinsden/wd100-dimmer.src
  @erocm1231
  https://github.com/erocm123/SmartThingsPublic/tree/master/devicetypes/erocm123/homeseer-hs-wd100-dimmer-switch.src
      
  Change Log
  2016-06-09 Added quirk issue to User's guide
  2016-05-31 User's Guide page and Icon w/ cyan color
  2016-05-29 Used helpButton1() technique 
  2016-05-28 Minor text, typo changes to screens, paragraph separation for devices
  2016-05-27 Added Hardware specific button help paragraphs per button
  2016-05-26 Repo addition, new icon change. 
             Added new 5th, 6th button options to code for HomeSeer switches 6 total functions,
             Renamed Sonos to Speaker to update terminology
  2016-05-25 Initial code modified from @bravenel Button Controller+
             Added label modifications 
  2015-09-14 Added virtual buttons
 
   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at: www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.Button Controller Plus
 	
 
*/
  
definition(
    name: "Button Controller Plus",
    namespace: "dcoffing",
    author: "Bruce Ravenel, Dale Coffing",
    description: "Control devices with buttons using Aeon Labs Minimote, HomeSeer HS-WD100+, HS-WS100+, Key Fob, etc.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/button-controller-plus.src/MultiTapIcon125x125cy.png",
    iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/button-controller-plus.src/MultiTapIcon250x250cy.png",
    iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/button-controller-plus.src/MultiTapIcon250x250cy.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")
	page(name: "configureButton5")
	page(name: "configureButton6")
    page(name: "aboutPage")
    
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, choose your button hardware ...", nextPage: "configureButton1", uninstall: configured()) {
        
        section {
			input "buttonDevice", "capability.button", title: "Select button 'thing' device...", multiple: false, required: true
		}
		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {

			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
        
        section() {
        	label title: "Assign a name:", required: false
        }
 
    	section("Version Info, User's Guide") {
// VERSION
       	href (name: "aboutPage", 
       	title: "Button Controller Plus \n"+"Version 1.0.160609", 
       	description: "Tap to get smartapp information and user's guide.",
       	image: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/button-controller-plus.src/MultiTapIcon125x125cy.png",
       	required: false,
       	page: "aboutPage"
 	   )
   }	
            
            }
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none, install: true, uninstall: true) {
     	section("User's Guide; Button Controller Plus") {
        	paragraph textHelp()
 		}
	}
}
def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the FIRST button... ",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))          
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a SECOND button, set it up here or 'Next'",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}
def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a THIRD button, set it up here or 'Next'",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a FOURTH button, set it up here or 'Next'",
		nextPage: "configureButton5", uninstall: configured(), getButtonSections(4))
}
def configureButton5() {
	dynamicPage(name: "configureButton5", title: "If you have a FIFTH button (WD,WS100+), set it up here or 'Next'",
		nextPage: "configureButton6", uninstall: configured(), getButtonSections(5))
}
def configureButton6() {
	dynamicPage(name: "configureButton6", title: "If you have a SIXTH button (WD,WS100+), set it up here or 'Done'",
		install: true, uninstall: true, getButtonSections(6))
}

def getButtonSections(buttonNumber) {
	return {
 //       log.debug "buttonNumber($buttonNumber)"
	switch (buttonNumber) {
   	    	case 1:
  	         	section("Hardware specific info on button selection:") {  
            	paragraph "${helpButton1()}"
                }
                break
        	case 2:
            	section("Hardware specific info on button selection:") {  
           		paragraph "${helpButton2()}"
            	}
                break
        	case 3:
            	section("Hardware specific info on button selection:") {  
           		paragraph "${helpButton3()}"
                }
                break
        	case 4:
            	section("Hardware specific info on button selection:") {  
           		paragraph "${helpButton4()}"
                }
            	break
        	case 5:
            	section("Hardware specific info on button selection:") {  
           		paragraph "${helpButton5()}"
                }
                break
        	case 6:
            	section("Hardware specific info on button selection:") {  
           		paragraph "${helpButton6()}"
        		}
                break
        }        	
      
		section("Lights to Toggle") {
			input "lights_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lights_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Dimmers to Toggle") {
			input "lightsDT_${buttonNumber}_pushed", "capability.switchLevel", title: "Pushed", multiple: true, required: false
			input "lightsDTVal_${buttonNumber}_pushed", "number", title: "Dim Level", required: false, description: "0 to 99"
			input "lightsDT_${buttonNumber}_held", "capability.switchLevel", title: "Held", multiple: true, required: false
			input "lightsDTVal_${buttonNumber}_held", "number", title: "Dim Level", required: false, description: "0 to 99"
		}
		section("Lights to Turn On") {
			input "lightOn_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightOn_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Lights to Turn Off") {
			input "lightOff_${buttonNumber}_pushed", "capability.switch", title: "Pushed", multiple: true, required: false
			input "lightOff_${buttonNumber}_held", "capability.switch", title: "Held", multiple: true, required: false
		}
		section("Dimmers to DimLevel 1") {
			input "lightDim_${buttonNumber}_pushed", "capability.switchLevel", title: "Pushed", multiple: true, required: false
			input "lightVal_${buttonNumber}_pushed", "number", title: "DimLevel 1", multiple: false, required: false, description: "0 to 99"
			input "lightDim_${buttonNumber}_held", "capability.switchLevel", title: "Held", multiple: true, required: false
			input "lightVal_${buttonNumber}_held", "number", title: "DimLevel 1", multiple: false, required: false, description: "0 to 99"
		}
		section("Dimmers to DimLevel 2") {
			input "lightD2m_${buttonNumber}_pushed", "capability.switchLevel", title: "Pushed", multiple: true, required: false
			input "lightV2l_${buttonNumber}_pushed", "number", title: "DimLevel 2", multiple: false, required: false, description: "0 to 99"
			input "lightD2m_${buttonNumber}_held", "capability.switchLevel", title: "Held", multiple: true, required: false
			input "lightV2l_${buttonNumber}_held", "number", title: "DimLevel 2", multiple: false, required: false, description: "0 to 99"
		}
		section("Fan to Adjust - Low, Medium, High, Off") {
			input "fanAdjust_${buttonNumber}_pushed", "capability.switchLevel", title: "Pushed", multiple: false, required: false
			input "fanAdjust_${buttonNumber}_held", "capability.switchLevel", title: "Held", multiple: false, required: false
		}
		section("Shade to Adjust - Up, Down, or Stop") {
			input "shadeAdjust_${buttonNumber}_pushed", "capability.doorControl", title: "Pushed", multiple: false, required: false
			input "shadeAdjust_${buttonNumber}_held", "capability.doorControl", title: "Held", multiple: false, required: false
		}
		section("Locks") {
			input "locks_${buttonNumber}_pushed", "capability.lock", title: "Pushed", multiple: true, required: false
			input "locks_${buttonNumber}_held", "capability.lock", title: "Held", multiple: true, required: false
		}
		section("Speaker music player") {
			input "speaker_${buttonNumber}_pushed", "capability.musicPlayer", title: "Pushed", multiple: true, required: false
			input "speaker_${buttonNumber}_held", "capability.musicPlayer", title: "Held", multiple: true, required: false
		}
		section("Modes") {
			input "mode_${buttonNumber}_pushed", "mode", title: "Pushed", required: false
			input "mode_${buttonNumber}_held", "mode", title: "Held", required: false
		}
		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			section("Hello Home Actions") {
				log.trace phrases
				input "phrase_${buttonNumber}_pushed", "enum", title: "Pushed", required: false, options: phrases
				input "phrase_${buttonNumber}_held", "enum", title: "Held", required: false, options: phrases
			}
		}
		section("Sirens") {
			input "sirens_${buttonNumber}_pushed","capability.alarm" ,title: "Pushed", multiple: true, required: false
			input "sirens_${buttonNumber}_held", "capability.alarm", title: "Held", multiple: true, required: false
		}

		section("Custom Message") {
			input "textMessage_${buttonNumber}_pushed", "text", title: "Pushed", required: false
			input "textMessage_${buttonNumber}_held", "text", title: "Held", required: false
		}

		section("Push Notifications") {
			input "notifications_${buttonNumber}_pushed","bool" ,title: "Pushed", required: false, defaultValue: false
			input "notifications_${buttonNumber}_held", "bool", title: "Held", required: false, defaultValue: false
		}

		section("SMS Notifications") {
			input "phone_${buttonNumber}_pushed","phone" ,title: "Pushed", required: false
			input "phone_${buttonNumber}_held", "phone", title: "Held", required: false
		}
        section("Associate a Momentary Button"){
        	input "virtB_${buttonNumber}_pushed","capability.momentary",title: "Pushed", required: false
            input "virtB_${buttonNumber}_held","capability.momentary",title: "Held", required: false
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
    subscribe(virtB_1_pushed,"momentary.pushed",fakebutton1Event)
    subscribe(virtB_2_pushed,"momentary.pushed",fakebutton2Event)
    subscribe(virtB_3_pushed,"momentary.pushed",fakebutton3Event)
    subscribe(virtB_4_pushed,"momentary.pushed",fakebutton4Event)
    subscribe(virtB_5_pushed,"momentary.pushed",fakebutton5hEvent)
    subscribe(virtB_6_pushed,"momentary.pushed",fakebutton6hEvent)
    subscribe(virtB_1_held,"momentary.pushed",fakebutton1hEvent)
    subscribe(virtB_2_held,"momentary.pushed",fakebutton2hEvent)
    subscribe(virtB_3_held,"momentary.pushed",fakebutton3hEvent)
    subscribe(virtB_4_held,"momentary.pushed",fakebutton4hEvent)
    subscribe(virtB_5_held,"momentary.pushed",fakebutton5hEvent)
    subscribe(virtB_6_held,"momentary.pushed",fakebutton6hEvent)
    state.lastshadesUp = true
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4)|| buttonConfigured(5)|| buttonConfigured(6)
}

def buttonConfigured(idx) {
	return settings["lights_$idx_pushed"] ||
    	settings["lightsDT_$idx_pushed"] ||
        settings["lightsDTVal_$idx_pushed"] ||
    	settings["lightOn_$idx_pushed"] ||
    	settings["lightOff_$idx_pushed"] ||
        settings["lightDim_$idx_pushed"] ||
        settings["lightVal_$idx_pushed"] ||
        settings["lightD2m_$idx_pushed"] ||
        settings["lightV2l_$idx_pushed"] ||
        settings["fanAdjust_$idx_pushed"] ||
        settings["shadeAdjust_$idx_pushed"] ||
		settings["locks_$idx_pushed"] ||
		settings["speaker_$idx_pushed"] ||
		settings["mode_$idx_pushed"] ||
        settings["notifications_$idx_pushed"] ||
        settings["sirens_$idx_pushed"] ||
        settings["notifications_$idx_pushed"]   ||
        settings["phone_$idx_pushed"]
}

def fakebutton1Event(evt) {
    executeHandlers(1, "pushed")
}

def fakebutton2Event(evt) {
    executeHandlers(2, "pushed")
}

def fakebutton3Event(evt) {
    executeHandlers(3, "pushed")
}

def fakebutton4Event(evt) {
    executeHandlers(4, "pushed")
}

def fakebutton5Event(evt) {
    executeHandlers(5, "pushed")
}

def fakebutton6Event(evt) {
    executeHandlers(6, "pushed")
}
def fakebutton1hEvent(evt) {
    executeHandlers(1, "held")
}

def fakebutton2hEvent(evt) {
    executeHandlers(2, "held")
}

def fakebutton3hEvent(evt) {
    executeHandlers(3, "held")
}

def fakebutton4hEvent(evt) {
    executeHandlers(4, "held")
}

def fakebutton5hEvent(evt) {
    executeHandlers(5, "held")
}

def fakebutton6hEvent(evt) {
    executeHandlers(6, "held")
}
def buttonEvent(evt){
	if(allOk) {
		def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
		def value = evt.value
		log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"

		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

		if(recentEvents.size <= 1){
			switch(buttonNumber) {
				case ~/.*1.*/:
					executeHandlers(1, value)
					break
				case ~/.*2.*/:
					executeHandlers(2, value)
					break
				case ~/.*3.*/:
					executeHandlers(3, value)
					break
				case ~/.*4.*/:
					executeHandlers(4, value)
					break
                case ~/.*5.*/:
					executeHandlers(5, value)
					break
                case ~/.*6.*/:
					executeHandlers(6, value)
					break
			}
		} else {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
}

def executeHandlers(buttonNumber, value) {
	log.debug "executeHandlers: $buttonNumber - $value"

	def lights = find('lights', buttonNumber, value)
	if (lights) toggle(lights)

	def lightsDT = find('lightsDT', buttonNumber, value)
	def dimTVal = find('lightsDTVal', buttonNumber, value)
	if (lightsDT) dimToggle(lightsDT, dimTVal)

	def lights1 = find('lightOn', buttonNumber, value)
	if (lights1) turnOn(lights1)

	def lights2 = find('lightOff', buttonNumber, value)
	if (lights2) turnOff(lights2)

	def lights3 = find('lightDim', buttonNumber, value)
	def dimval3 = find('lightVal', buttonNumber, value)
	if (lights3) turnDim(lights3,dimval3)

	def lights4 = find('lightD2m', buttonNumber, value)
	def dimval4 = find('lightV2l', buttonNumber, value)
	if (lights4) turnDim(lights4,dimval4)

	def fan = find('fanAdjust', buttonNumber, value)
	if (fan) adjustFan(fan)
    
	def shade = find('shadesAdjust', buttonNumber, value)
	if (shade) adjustShade(shade)

	def locks = find('locks', buttonNumber, value)
	if (locks) toggle(locks)

	def speaker = find('speaker', buttonNumber, value)
	if (speaker) toggle(speaker)

	def mode = find('mode', buttonNumber, value)
	if (mode) changeMode(mode)

	def phrase = find('phrase', buttonNumber, value)
	if (phrase) location.helloHome.execute(phrase)

	def textMessage = findMsg('textMessage', buttonNumber, value)

	def notifications = find('notifications', buttonNumber, value)
	if (notifications?.toBoolean()) sendPush(textMessage ?: "Button $buttonNumber was pressed" )

	def phone = find('phone', buttonNumber, value)
	if (phone) sendSms(phone, textMessage ?:"Button $buttonNumber was pressed")

	def sirens = find('sirens', buttonNumber, value)
	if (sirens) toggle(sirens)
}

def find(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def findMsg(type, buttonNumber, value) {
	def preferenceName = type + "_" + buttonNumber + "_" + value
	def pref = settings[preferenceName]
	if(pref != null) {
		log.debug "Found: $pref for $preferenceName"
	}

	return pref
}

def turnOn(devices) {
	log.debug "turnOn: $devices = ${devices*.currentSwitch}"

	devices.on()
}

def turnOff(devices) {
	log.debug "turnOff: $devices = ${devices*.currentSwitch}"

	devices.off()
}

def turnDim(devices, level) {
	log.debug "turnDim: $devices = ${devices*.currentSwitch}"

	devices.setLevel(level)
}

def adjustFan(device) {
	log.debug "adjust: $device = ${device.currentLevel}"
    
	def currentLevel = device.currentLevel

	if(device.currentSwitch == 'off') device.setLevel(15)
	else if (currentLevel < 34) device.setLevel(50)
  	else if (currentLevel < 67) device.setLevel(90)
	else device.off()
}

def adjustShade(device) {
	log.debug "shades: $device = ${device.currentMotor} state.lastUP = $state.lastshadesUp"

	if(device.currentMotor in ["up","down"]) {
    	state.lastshadesUp = device.currentMotor == "up"
    	device.stop()
    } else {
    	state.lastshadesUp ? device.down() : device.up()
//    	if(state.lastshadesUp) device.down()
//        else device.up()
        state.lastshadesUp = !state.lastshadesUp
    }
}

def toggle(devices) {
	log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

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

def dimToggle(devices, dimLevel) {
	log.debug "dimToggle: $devices = ${devices*.currentValue('switch')}"

	if (devices*.currentValue('switch').contains('on')) devices.off()
	else devices.setLevel(dimLevel)
}

def changeMode(mode) {
	log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

	if (location.mode != mode && location.modes?.find { it.name == mode }) {
		setLocationMode(mode)
	}
}

// execution filter methods
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
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
	log.trace "daysOk = $result"
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
	log.trace "timeOk = $result"
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


private def helpButton1() {
    def text =
"WD100+ or WS100+ devices; this FIRST Button action occurs" +
" with a double-tap on upper paddle." +
"\n"+
"*Select 'Pushed' (not 'Held') options." +
"\n\n"+
"Aeon Minimote; FIRST button is upper left" +
" when operating in hand."+
"\n"+
"*Select 'Pushed' and/or 'Held' options." 
}

private def helpButton2() {
    def text =
"WD100+ or WS100+ devices; this SECOND Button action occurs" +
" with a double-tap on lower paddle." +
"\n"+
" *Select 'Pushed' (not 'Held') options." +
"\n\n"+
"Aeon Minimote; SECOND button is upper right" +
" when operating in hand.)"+
"\n"+
"*Select 'Pushed' and/or 'Held' options."  
}
private def helpButton3() {
    def text =
"WD100+ or WS100+ devices; this THIRD Button action occurs" +
" with a triple-tap on upper paddle." +
"\n"+
" *Select 'Pushed' (not 'Held') options." +
"\n\n"+
"Aeon Minimote; THIRD button is lower left" +
" when operating in hand.)"+
"\n"+
"*Select 'Pushed' and/or 'Held' options."
}
private def helpButton4() {
    def text =
"WD100+ or WS100+ devices; this FOURTH Button action occurs" +
" with a triple-tap on lower paddle." +
"\n"+
" *Select 'Pushed' (not 'Held') options." +
"\n\n"+
"Aeon Minimote; FOURTH button is lower right" +
" when operating in hand." +
"\n"+
"*Select 'Pushed' and/or 'Held' options." 
}
private def helpButton5() {
    def text =
"(See user guide on quirk for WD100+) For WS100+ devices; this FIFTH Button action occurs" +
" with a press & hold on upper paddle." + 
"\n"+
"*Select 'Pushed' (not 'Held') options." 
}
private def helpButton6() {
    def text =
"(See user guide on quirk for WD100+) For WS100+ devices; SIXTH Button action occurs" +
" with a press & hold on lower paddle." +
"\n"+
"*Select 'Pushed' (not 'Held') options." 
}

private def textHelp() {
	def text =
		"This smartapp allows you to use a device with buttons like the Aeon Labs Minimote,"+
        " Key Fob, HomeSeer HS-WD100+ or HS-WS100+ switches that have the added abilities of"+
        " double-tap, triple-tap and press & hold features to the paddle switch to do home"+
        " automation control. This smartapp modified @bravenel's"+
        " Button Controller+ which had previously fixed all the shortcomings of the stock Button Controller"+
        " smartapp. The original app had four buttons but the new"+
        " Homeseer switches needed six button controls to handle the additional features. \n\n"+
        "The control options available are: \n"+
        "	Lights to Toggle \n"+
        "	Dimmers to Toggle \n"+
        "	Lights to Turn On \n"+
        "	Lights to Turn Off \n"+
        "	Dimmers to DimLevel 1 \n"+
        "	Dimmers to DimLevel 2 \n"+
        "	Fan to Adjust - Low, Medium, High, Off \n"+
        "	Shade to Adjust - Up, Down, or Stop \n"+
        "	Locks \n"+
        "	Speaker music player \n"+
        "	Modes \n"+
        "	Hello Home Actions \n"+
        "	Sirens \n"+
        "	Custom Message \n"+
        "	Push Notifications \n"+
        "	SMS Notifications \n"+
        "	Associate a Momentary Button \n\n"+
        "** Quirk for HS-WD100+ on 5/6 buttons **\n"+
        "Because a dimmer switch already uses press&hold to manually set the dimming level"+
        " please be aware of this operational behavior. If you only want to manually change"+
        " the dim level to the lights that are wired to the switch you will automatically"+
        " trigger the 5/6 button event as well. And the same is true in reverse, if you"+ 
        " only want to trigger a 5/6 button event action with press&hold you will manually"+
        " be changing the dim level of the switch simultaneously as well.\n"+
        "This quirk doesn't exist of course with the HS-HS100+ since it is not a dimmer."
	}