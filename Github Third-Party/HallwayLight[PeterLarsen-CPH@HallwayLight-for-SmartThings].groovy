/*
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Description:
 *		This app is called “Hallway Light” as it is meant for places where you often pass through and 
 *		therefore don’t need much light to find your way.  On the other hand, if you stay in the room a
 *		little bit longer, you would probably like to have more light.
 *		
 *		The main difference between this app and Smart Light by SmartThings, is that this app allows
 *		you to control the lights in more ways, like the minimum and maximum levels of light
 *		depending on evening/morning and night settings and detecting if other apps have taken control
 *		over the lights and therefore should leave the lights as they are etc.
 *
 *	Compared to Smart Light by SmartThings, this app solves the following problems:
 *		Color of LIFX bulbs is not handled very well (e.g. Warm White does not work at all).
 *		Start and end times does (or did?) not work in Smart Light.
 *		Destinctions between evening and night settings in one app.
 *		Definition of a ownership concept, where the app can detect if other apps is used to set the lights.
 *		Better control of levels of lights than just on/off.
 *		Adding more than one contact/motion sensors.
 *		You can add a button controller to activate/deactivating the app, e.g. if you don't need the light for a period.
 *
 *	For limitations and missing things, please see the readme file at github:
 *		https://github.com/PeterLarsen-CPH/HallwayLight-for-SmartThings.git
 *
*/
definition(
        name: "Hallway Light",
        namespace: "PeterLarsen-CPH",
        author: "Peter Larsen",
        description: "Control the light if no one else control the lights",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {

	section("When activity on any of these sensors") {
        input "contactSensors", "capability.contactSensor", title: "Open/close sensors", multiple: true, required: false, submitOnChange: true
        input "motionSensors", "capability.motionSensor", title: "Motion sensors?", multiple: true, required: false, submitOnChange: true
    }
    if (contactSensors || motionSensors)
    {
        section("Then turn on these color temperature controllable lights") {
            input "switches", "capability.colorTemperature", multiple: true, required: false, submitOnChange: true
        }
        if (switches)
        {
            section("Color Temperature") {
                input "colorTemperature", "enum", title: "", options:
                    [[2500: "Warm White (2500K)"], [2750: "Soft White (2750K)"], [3300: "White (3300K)"], [4100: "Moonlight (4100K)"],
                    [5000: "Cool White (5000K)"], [6500: "Daylight (6500K)"]], defaultValue: "2750", required: true, multiple: false
            }
		}
		section("-or these level controllable lights") {
            input "switchLevels", "capability.switchLevel", multiple: true, required: false
        }
		
        section("-or these outlet switches") {
            input "outletSwitches", "capability.switch", multiple: true, required: false
        }
        
        section("Select the bulbs used to state the ownership") {
            input "OwnershipSwitches", "capability.switchLevel", multiple: true, required: false
        }
        section("Select a contact to turn this schema on and off") {
            input "onOffButton", "capability.button", multiple: false, required: false, submitOnChange: true
        }
        if (onOffButton)
        {
	        section("Choose the button") {
    	        input "onOffbuttonNumber", "enum", title: "Button", options:
        	        [[1: "Button 1"], [2: "Button 2"], [3: "Button 3"], [4: "Button 4"]], required: true, multiple: false
            }
        }
        section("Set MIN and MAX levels for lights and different timings for the evening") {
            input "startLight", "number", title: "Minimum level before switching lights off (1-8)", defaultValue: "2", range: "1..8",
                multiple: false, required: true
            input "maxLight", "number", title: "Maximum level (1-9)", defaultValue: "9", range: "1..9",
                multiple: false, required: true
            input "turnOnSteps", "number", title: "Set how quickly the lights turns on and increase in the level of light (1-9)", defaultValue: "1", range: "1..9",
                multiple: false, required: true
            input "timeBeforeDecreasing", "enum", title: "Specify how long the lights stays on before decreasing", options:
                [[10: "Short (10 sec)"], [60: "Middle (1 min)"], [300: "Long (5 min)"], [1800: "Longer (30 min)"]], defaultValue: "300", required: true, multiple: false
            input "timeBeforeLightOff", "enum", title: "Specify how long the lights stays at minimum level before turning off", defaultValue: "1", required: true, multiple: false,
                options: [[1: "Short (1 min)"], [5: "Middle (5 min)"], [30: "Long (30 min)"], [60: "Longer (1 hour)"]]
        }
        section("Night settings") {
            input "lightsOnInNight", "bool", title: "Lights on during the night", defaultValue: false,
                multiple: false, required: true, submitOnChange: true
        }
        if (lightsOnInNight)
        {
            section("Set MIN and MAX levels for the night time (between 11pm and 6am (or sunrise)") {
                input "startLightNight", "number", title: "Minimum level before switching lights off (1-8)", defaultValue: "1", range: "1..8",
                    multiple: false, required: true
                input "maxLightNight", "number", title: "Maximum level (1-9)", defaultValue: "2", range: "1..9",
                    multiple: false, required: true
                input "timeBeforeDecreasingNight", "enum", title: "Specify how long the lights stays on before decreasing", options:
                    [[10: "Short (10 sec)"], [60: "Middle (1 min)"], [300: "Long (5 min)"], [1800: "Longer (30 min)"]], defaultValue: "10", required: true, multiple: false
            	input "turnOffNightIfInactive", "bool", title: "Disable schema at night if no motion for 30 mins", defaultValue: false,
                	multiple: false, required: true, submitOnChange: true
                    
            }
        }
        
	}
    
	section([mobileOnly:true]) {
    	label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
	}

}}


def installed() {
	log.debug("Installed")
    allLightsOff();
    initialize()
}

def updated() {
	log.debug("Updated")
    unsubscribe()
    allLightsOff();
    initialize();
}

def allLightsOff(){
	log.debug "All lights off"
    switches?.off()
    switchLevels?.off()
    outletSwitches?.off()
}

def initialize() {
    if (!((contactSensors || motionSensors) && (switches || switchLevels || outletSwitches)))
	{
		log.error "install error - missing input"
        assert false //How to throw an ArgumentException ??
    }
	log.debug "Initialized with settings: ${settings}"
	switches.each{s -> log.debug "Color lights added: ${s}, level: ${s.currentValue("level")}"; }
	switchLevels.each{s -> log.debug "Level lights added: ${s}, level: ${s.currentValue("level")}"; }
	OwnershipSwitches.each{s -> log.debug "Ownership lights added: ${s}, level: ${s.currentValue("level")}"; }
    outletSwitches.each{s -> log.debug "Outlet switches added: ${s}, switch: ${s.currentValue("switch")}"; }
	
    contactSensors.each({s -> log.debug "Contact sensors added: ${s} ${s.currentState("contact").value}" });
	motionSensors.each({s -> log.debug "Motion sensors added: ${s} ${s.currentState("motion").value}" });    
	onOffButton?.each({s -> log.debug "Contact button added ${s}"});    

	state.levels = [00, 11, 22, 33, 44, 55, 66, 77, 88, 99]
    state.startLightUseThis = startLight
    state.maxLightUseThis = maxLight
    state.timeBeforeDecreasingUseThis = timeBeforeDecreasing
    state.schemaOff = false;
    state.schemaOffTime = new Date().time;

	doWeOnwTheLights();	
	dayPeriod()

    if (contactSensors)
    {
    	subscribe(contactSensors, "contact.open", sensorDetectedHandler)
       	subscribe(contactSensors, "contact.closed", sensorStoppedHandler)
    }
    if (motionSensors)
    {
    	subscribe(motionSensors, "motion.active", sensorDetectedHandler)
       	subscribe(motionSensors, "motion.inactive", sensorStoppedHandler)
    }
    if (onOffButton)
    {
		subscribe(onOffButton, "button.pushed", onOffbuttonEvent)
	}
}

def sensorDetectedHandler(evt) {
    log.debug "sensorDetectedHandler called: $evt"
    
    def period = dayPeriod();
    
    if (state.schemaOff){
    	if (period != 'NIGHT' || new Date().time > state.schemaOffTime)
        {
            log.debug "Schema inactivity is timed out or the day mode changed from NIGHT"
            state.schemaOff = false
        }
        else
        {
            log.debug "Schema is off"
            return
        }
    }

    if (period == 'DAY')
    	return;
    if (period == 'NIGHT' && !lightsOnInNight)
    	return;
    increaseLights();
}

def sensorStoppedHandler(evt) {
    log.debug "sensorStoppedHandler called: $evt"
    if (state.schemaOff){
    	log.debug "Schema is off"
        return
    }
    def period = dayPeriod();
	runIn(state.timeBeforeDecreasingUseThis.toInteger(), decreaseLights, [overwrite: true]);
    
    if (period == 'NIGHT' && lightsOnInNight && turnOffNightIfInactive)
		runIn(30 * 60, nightSchemaOff, [overwrite: true]); //schedule schema off in 30 mins
}

def nightSchemaOff(){
    if (dayPeriod() == 'NIGHT' && doWeOnwTheLights() && areAllSensorsOff())
    {
    	log.debug "Schema is automatically deactivated due to inactivity"
        setSchemaOff()
    }
}

def onOffbuttonEvent(evt){
    def buttonNumber = evt.data
    def value = evt.value
    log.debug "button: $buttonNumber, value: $value"

    def button = -1;
    switch(buttonNumber) {
        case ~/.*1.*/:
	        button = 1;
        break
        case ~/.*2.*/:
    	    button = 2;
        break
        case ~/.*3.*/:
        	button = 3;
        break
        case ~/.*4.*/:
        	button = 4;
        break
    }
    log.debug "Button number: $button"
    if (button.toString() == onOffbuttonNumber)
    {
    	if (state.schemaOff) 
        { //schema is currently off
        	log.debug "Schema on"
        	state.schemaOff = false;
	    	switches?.setLevel(state.levels[state.startLightUseThis])
            switchLevels?.setLevel(state.levels[state.startLightUseThis])
			runIn(10, decreaseLights, [overwrite: true]);
        }
        else
        { //schema is currently on
			setSchemaOff()
        }
    }
}

def setSchemaOff(){
	log.debug "Schema off"
    state.schemaOff = true;
    state.schemaOffTime = new Date().time + 28800 * 1000; //8 hours (in miliseconds) - same as never go off by itself
    allLightsOff();
}

def changeColorTemperatureIfChanged(){
	if (switches == null)
    	return;
    log.debug "requested temp: $colorTemperature"
	def colortemp = 0;
    switches.each{s -> colortemp = s.currentValue("colorTemperature"); }
    //switches[0].currentValue("colorTemperature")
    log.debug "Found temp: $colortemp"
    
    if (colortemp != colorTemperature.toInteger())
    {
    	switches.setColorTemperature(colorTemperature)
    	log.debug "ColorTemperatore has changed and is set back"
    }
}

def levelFromBulbLevel(){
	def levelBulbs = OwnershipSwitches;
	if (!levelBulbs)
    	levelBulbs = switchLevels;
	if (!levelBulbs)
    	levelBulbs = switches;
	if (!levelBulbs)
    	return state.maxLightUseThis; //if not possible to state the level, then just return max-level

	def allOff = !levelBulbs.any({s -> s.currentSwitch == "on"});
    if (allOff)
    {
    	return state.startLightUseThis
    }
    else
    {
		def level = 0
		levelBulbs.each{s -> level = s.currentValue("level"); }
		//levelBulbs.each[0].currentValue("level")
		level = (((level / 10) - (level / 10).toInteger()) * 10).toInteger();
        return level
    }
}

def dayPeriod(){
	def sunInfo = getSunriseAndSunset()
  	def now = new Date()
   	//now.set(hourOfDay: 00, minute: 51, second: 00);
    state.startLightUseThis = startLight
    state.maxLightUseThis = maxLight
    state.timeBeforeDecreasingUseThis = timeBeforeDecreasing
    
	//What if location is not set ?? (test that)
    
    if (timeOfDayIsBetween(sunInfo.sunrise, sunInfo.sunset, now, location.timeZone))
    //if (sunInfo.sunrise.compareTo(now) * now.compareTo(sunInfo.sunset) > 0)
    {
    	log.debug 'DAY time'
        state.timeBeforeDecreasingUseThis = 5 * 60 //Lights off in 5 minutes regardless of current settings
    	return 'DAY'
	}

	def nightTime = new Date(); 
	//Date year: 2016, month: Calendar.APRIL, dayOfMonth: 5, hourOfDay: 14, minute: 12, second: 45) 
    Calendar localCalendar = Calendar.getInstance()
    localCalendar.setTimeZone(location.timeZone)
    int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)

	if (currentDayOfWeek == Calendar.FRIDAY || currentDayOfWeek == Calendar.SATURDAY)
	   	nightTime = timeToday("23:59:00", location.timeZone);
    else
    	nightTime = timeToday("23:00:00", location.timeZone);
    //nightTime = new Date(nightTime.time - location.timeZone.getRawOffset())

	def morningTime = new Date(); 
    
	if (currentDayOfWeek == Calendar.SATURDAY || currentDayOfWeek == Calendar.SUNDAY)
		morningTime = timeToday("07:45:00", location.timeZone);
    else
		morningTime = timeToday("06:00:00", location.timeZone); 
    //morningTime = new Date(morningTime.time - location.timeZone.getRawOffset())

//log.debug "night time: $nightTime"
//log.debug "morning time: $morningTime"
//log.debug "sunrise $sunInfo.sunrise"
//log.debug "sunset $sunInfo.sunset"
//log.debug "now $now"

	//if (timeOfDayIsBetween(sunInfo.sunset, nightTime, now, location.timeZone))
    if (sunInfo.sunset.compareTo(now) * now.compareTo(nightTime) > 0)
    {
    	log.debug 'EVENING time'
    	return 'EVENING'
	}        

	//if (timeOfDayIsBetween(morningTime, sunInfo.sunrise, now, location.timeZone))
    if (morningTime.compareTo(now) * now.compareTo(sunInfo.sunrise) > 0)
    {
    	log.debug 'MORNING time'
    	return 'MORNING'
	}        
    
    //if (timeOfDayIsBetween(nightTime, sunInfo.sunrise, now, location.timeZone))
    //if (nightTime.compareTo(now) * now.compareTo(sunInfo.sunrise) > 0)
    //{ //if none of the others, then it is night
    	if (lightsOnInNight)
        {
            state.startLightUseThis = startLightNight
            state.maxLightUseThis = maxLightNight
            state.timeBeforeDecreasingUseThis = timeBeforeDecreasingNight
        }
        log.debug "NIGHT"
    	return 'NIGHT'
    //}
}

def doWeOnwTheLights() {
	if (!OwnershipSwitches)
    	return true; //if Ownership switches is not set, then just say we own the lights
        
    def allOff = !OwnershipSwitches.any({s -> s.currentSwitch == "on"});
    def allOn = !OwnershipSwitches.any({s -> s.currentSwitch == "off"});

	def expectedLevel = levelFromBulbLevel();
	def allSameLevelSetByUs = !OwnershipSwitches.any({s -> s.currentValue("level") != state.levels[expectedLevel]})
    def weOwnTheLights = allOff || allOn && allSameLevelSetByUs;

	//OwnershipSwitches.each({s -> log.trace "$s ${s.currentValue("level")}" })
    log.debug "We own the lights: ${weOwnTheLights}"
    return weOwnTheLights;
}

def areAllSensorsOff(){
	//contactSensors.each({m -> log.trace m.currentState("contact").value});
	//motionSensors.each({m -> log.trace m.currentState("motion").value});
	def allContactsOff = !contactSensors.any({m -> m.currentState("contact").value == "open"});
	def allMotionsOff = !motionSensors.any({m -> m.currentState("motion").value == "active"});
    
    log.debug "All sensors are off: ${allContactsOff && allMotionsOff}"
    return allContactsOff && allMotionsOff;
}

def increaseLights(){
    log.debug "increaseLights called"
	def minRoundtrip = 1500; //1.5 seconds
    def maxStayingInMethod = 15000; //15 seconds

	if (doWeOnwTheLights() && !state.schemaOff)
   	{
        def enter = now()
    	changeColorTemperatureIfChanged()

		while(true){
	        def roundtrip = now()
			if (areAllSensorsOff() || state.schemaOff)
				return;

			def level = levelFromBulbLevel() + turnOnSteps;
		    if (level > state.maxLightUseThis)
               	level = state.maxLightUseThis
	    	switches?.setLevel(state.levels[level])
            switchLevels?.setLevel(state.levels[level])
		    log.trace "Setting bulbs to: ${state.levels[level]}%";    
		    if (level >= state.maxLightUseThis)
            {
            	outletSwitches?.on();
            	return;
            }
            
            if ((now() - roundtrip) < minRoundtrip )
            {
            	def goToLseep = minRoundtrip - (now() - roundtrip)
            	log.trace "sleeping ${goToLseep}"
            	pause(goToLseep);
            }

            if (now() - enter > maxStayingInMethod)
            {	 //for the projection - otherwise we get an ugly exception
            	log.error "timed out"
            	return;
            }
		}
	}
}

def decreaseLights(){
    log.debug "decreaseLights called"

	if (doWeOnwTheLights() && areAllSensorsOff())
   	{
    	def level = levelFromBulbLevel()
    	if (level > state.startLightUseThis)
    	{
			level = state.startLightUseThis;
	    	switches?.setLevel(state.levels[level])
            switchLevels?.setLevel(state.levels[level])
           	outletSwitches?.off();
    		log.trace "Decreasing bulbs to: ${state.levels[level]}%";    
    		runIn(timeBeforeLightOff.toInteger() * 60, decreaseLights, [overwrite: true]);
		}
		else
		{
			log.debug "Lights off";    
			allLightsOff()
		}
	}
}

