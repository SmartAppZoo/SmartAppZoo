/**
 *  
 *	Button Controller - Enhanced Lighting
 *  
 *	Author: Eric Maycock (erocm123)
 *	email: erocmail@gmail.com
 *	Date: 2015-10-22
 * 
 * 	2015-10-26: Added a "Random" option when choosing a color.
 *  
 *	This SmartApp allows you to, in great detail, control your lighting with button based 
 *	devices like the Aeon Labs Minimote or Enerwave ZWN-SC7. Want a button to turn one
 *  	hue bulb red, one green, one blue, and one orange? This can do it. Want a single button to 
 *	turn on your overhead lights and dim your lamps to 50%. This can do it. The combinations 
 *	are limitless.
 *   
 *	Note: The app treats the Aeon Minimote buttons as 1,2,3,4 for a single press and
 *	5,6,7,8 when the buttons are pressed and held. This should be pretty self explanatory but
 *	holding 1 is equivalent to button 5, holding 2 is equivalent to button 6, and so on.
 */
 


definition(
    name: "Button Controller - Enhanced Lighting",
    namespace: "erocm123",
    author: "Eric Maycock (erocm123)",
    description: "Control lights with buttons like the Aeon Labs Minimote or Enerwave ZWN-SC7.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton1")
	page(name: "configureButton2")
	page(name: "configureButton3")
	page(name: "configureButton4")
	page(name: "configureButton5")
	page(name: "configureButton6")
	page(name: "configureButton7")
	page(name: "configureButton8")
	page(name: "configureLight")

	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: "configureButton1", uninstall: configured()) {
		section {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true, submitOnChange: true
		}
        
		section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
        
			def timeLabel = timeIntervalLabel()

			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null

			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            
            		//input "numberOfButtons", "number", title: "Number of Buttons?", required: false, value: 8
		}
	}
}

def configureButton1() {
	dynamicPage(name: "configureButton1", title: "Now let's decide how to use the first button",
		nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))
}
def configureButton2() {
	dynamicPage(name: "configureButton2", title: "If you have a second button, set it up here",
		nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
	dynamicPage(name: "configureButton3", title: "If you have a third button, you can do even more here",
		nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
	dynamicPage(name: "configureButton4", title: "If you have a fourth button, set it up here",
		nextPage: "configureButton5", uninstall: configured(), getButtonSections(4))
}
def configureButton5() {
	dynamicPage(name: "configureButton5", title: "If you have a fifth button, set it up here",
		nextPage: "configureButton6", uninstall: configured(), getButtonSections(5))
}
def configureButton6() {
	dynamicPage(name: "configureButton6", title: "If you have a sixth button, set it up here",
		nextPage: "configureButton7", uninstall: configured(), getButtonSections(6))
}
def configureButton7() {
	dynamicPage(name: "configureButton7", title: "If you have a seventh button, set it up here",
		nextPage: "configureButton8", uninstall: configured(), getButtonSections(7))
}
def configureButton8() {
	dynamicPage(name: "configureButton8", title: "If you have a eighth button, set it up here",
		install: true, uninstall: true, getButtonSections(8))
}

def configureLight(params) {
    dynamicPage(name: "configureLight", uninstall: false, install: false) {
        section ("$params.name") {
            def switchType = "Switch"
            if ("$params.capabilities".indexOf('Switch Level') > 0) {
            	switchType = "Dimmer"
            }
            if ("$params.capabilities".indexOf('Color Control') > 0) {
            	switchType = "Color"
            }
            switch(switchType) {
				case ~/.*Switch.*/:
                    input "lights_${params.buttonNumber}_${params.lightId}_power", "bool", title: "Turn the light on or off", submitOnChange: false
					break
				case ~/.*Dimmer.*/:
                    input "lights_${params.buttonNumber}_${params.lightId}_power", "bool", title: "Turn the light on or off", submitOnChange: false
                    input "lights_${params.buttonNumber}_${params.lightId}_lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
					break
				case ~/.*Color.*/:
                    input "lights_${params.buttonNumber}_${params.lightId}_power", "bool", title: "Turn the light on or off", submitOnChange: false
                    input "lights_${params.buttonNumber}_${params.lightId}_color", "enum", title: "Hue Color?", required: false, multiple:false, options: [
					["Soft White":"Soft White - Default"],
					["White":"White - Concentrate"],
					["Daylight":"Daylight - Energize"],
					["Warm White":"Warm White - Relax"],
					"Red","Green","Blue","Yellow","Orange","Purple","Pink","Random"]
					input "lights_${params.buttonNumber}_${params.lightId}_lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
					break
            }
            
            
            
        }
    }
}

def getButtonSections(buttonNumber) {
	return {
		section("Lights") {
			input "lights_${buttonNumber}", "capability.switch", multiple: true, required: false, submitOnChange: true
		}
        def lightsConfigured = settings["lights_${buttonNumber}"]
        if (lightsConfigured != null) {
        	lightsConfigured.each {light ->
                def inDescription = "Click to Configure"

                if (settings["lights_${buttonNumber}_${light.id}_power"])
                	inDescription = "Power: on"
                else
                    inDescription = "Power: off"

                if (settings["lights_${buttonNumber}_${light.id}_lightLevel"] != null)
                	inDescription = "$inDescription, Level: ${settings["lights_${buttonNumber}_${light.id}_lightLevel"]}"
                    
                if (settings["lights_${buttonNumber}_${light.id}_color"] != null)
                	inDescription = "$inDescription, Color: ${settings["lights_${buttonNumber}_${light.id}_color"]}"

            	section ("$light"){
           			href(name: "Configure $light",
                 	page: "configureLight",
                 	params: [ name: "$light", type: "$light.name", capabilities: "$light.capabilities", buttonNumber: "$buttonNumber", lightId: "$light.id" ],
                 	description: "$inDescription")
           		}
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
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4) || buttonConfigured(5) || buttonConfigured(6) || buttonConfigured(7) || buttonConfigured(8)
}

def buttonConfigured(idx) {
	return settings["lights_$idx"]
}

def buttonEvent(evt){
	if(allOk) {
        def buttonNumber = evt.jsonData.buttonNumber

		def value = evt.value
		log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"
        if (value == "held")
        	buttonNumber = buttonNumber + 4

		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"

		if(recentEvents.size >= 1){
			switch(buttonNumber) {
				case ~/.*1.*/:
					executeHandlers(1)
					break
				case ~/.*2.*/:
					executeHandlers(2)
					break
				case ~/.*3.*/:
					executeHandlers(3)
					break
				case ~/.*4.*/:
					executeHandlers(4)
					break
                		case ~/.*5.*/:
					executeHandlers(5)
					break
                		case ~/.*6.*/:
					executeHandlers(6)
					break
                		case ~/.*7.*/:
					executeHandlers(7)
					break
                		case ~/.*8.*/:
					executeHandlers(8)
					break
			}
		} else {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
}

def executeHandlers(buttonNumber) {
    log.debug "executeHandlers: $buttonNumber"
    def lightsConfigured = settings["lights_${buttonNumber}"]
    if (lightsConfigured != null) {
        lightsConfigured.each {light ->
            setLight(light, "$light.name", "$light.capabilities", "$buttonNumber", "${light.id}")
        }
    }
}

def setLight(light, lightName, lightCapabilities, buttonNumber, lightId) {
    def power
    def level
    def color
    
    def switchType = "Switch"
    if ("$lightCapabilities".indexOf('Switch Level') > 0) {
        switchType = "Dimmer"
    }
    if ("$lightCapabilities".indexOf('Color Control') > 0) {
        switchType = "Color"
    }
    
    if (settings["lights_${buttonNumber}_${lightId}_power"] != null)
    	power = settings["lights_${buttonNumber}_${lightId}_power"]
    if (settings["lights_${buttonNumber}_${lightId}_lightLevel"] != null)
    	level = settings["lights_${buttonNumber}_${lightId}_lightLevel"]
    if (settings["lights_${buttonNumber}_${lightId}_color"] != null)
    	color = settings["lights_${buttonNumber}_${lightId}_color"]
        
    if (power) {
    	switch(switchType) {
        	case ~/.*Switch.*/:
                light.on()
        		break
        	case ~/.*Dimmer.*/:
                if (level)
                	light.setLevel(level as Integer)
                else
                	light.on()
        		break
        	case ~/.*Color.*/:
                def hueColor = 0
                def saturation = 100

                switch(color) {
                    case "White":
                    hueColor = 52
                    saturation = 19
                    break;
                    case "Daylight":
                    hueColor = 53
                    saturation = 91
                    break;
                    case "Soft White":
                    hueColor = 23
                    saturation = 56
                    break;
                    case "Warm White":
                    hueColor = 20
                    saturation = 80 //83
                    break;
                    case "Blue":
                    hueColor = 70
                    break;
                    case "Green":
                    hueColor = 39
                    break;
                    case "Yellow":
                    hueColor = 25
                    break;
                    case "Orange":
                    hueColor = 10
                    break;
                    case "Purple":
                    hueColor = 75
                    break;
                    case "Pink":
                    hueColor = 83
                    break;
                    case "Red":
                    hueColor = 100
                    break;
                    case "Random":
		    Random rand = new Random()
		    int max = 100
		    hueColor = rand.nextInt(max+1)
                    break;
                }
                def colorValue = [hue: hueColor, saturation: saturation, level: level as Integer ?: 100]
                light.setColor(colorValue)
        		break
		}
    }
    else {
    	light.off()
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
