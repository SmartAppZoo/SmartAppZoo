/**
 * Button Controls Dimmer
 *
 * By Chip Rosenthal <chip@unicom.com>
 *  
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
 
definition(
    name: "Button Controls Dimmer",
    namespace: "chip-rosenthal",
    author: "Chip Rosenthal",
    description: "Control a dimmer device with a button. Short push adjusts dimmer level. Long push turns off.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    lastUpdated: "2016-Apr-17 15:33")

preferences {
    page(name: "page1", nextPage: "page2", uninstall: true) {
        section("Assign this button") {  
            input "myButton", "capability.button", \
                title: "Button device:", required: true 
            input "myButtonNumber", "enum", \
                title: "Button number (for remote with multiple buttons):", options: ["1","2","3","4"], required: false
            paragraph '''\
If your device has a single button, leave the button number blank.

For an \"Aeon Minimote\", select the button number (1-4) to assign.

Other multi-button devices currently are untested.
'''
        }
        section("To control these dimmers") {
            input "myDimmers", "capability.switchLevel", \
                title: "Dimmers:", multiple: true
                
            input "myDimmerLevels", "string", \
            	title: "Dimmer step specification:", required: true, defaultValue: "10,60,100"
                
            paragraph '''\
The \"Dimmer step specification\" is a list of dimmer values in ascending order, separated by commas.

For example: 10,60,100

Each press of the button advances the dimmer to the next in the list. When the end of the list is reached, the next button press returns the dimmer to the first value in the list.
'''
        }
    }
    
    page(name: "page2")
}


def page2() {
    def dfltLabel = (myButtonNumber ? "${myButton}, button ${myButtonNumber}" : myButton)
    dynamicPage(name: "page2", install: true, uninstall: true) {
    	section([mobileOnly:true]) {
        	label title: "Assign a name", required: false, defaultValue: "$dfltLabel"
            mode title: "Set for specific mode(s)"
        }
        section("App Info") {
        	paragraph "Last updated: ${metadata.definition.lastUpdated}"
        }
	}
}

def installed() {
	log.debug "installed: settings = ${settings}"
	initialize()
}

def updated() {
	log.debug "updated: settings = ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(myButton, "button", buttonHandler)
}


def buttonHandler(evt) {
	log.debug "buttonHandler, invoked: button = ${myButton}, buttonNumber = ${myButtonNumber}, dimmers = ${myDimmers}"
    
	if (myDimmers.size() == 0) {
    	log.debug "buttonHandler: no dimmers selected"
        return
    }
   
    def currentButtonAction = evt.value
    def currentButtonNumber = null
    
    if (myButtonNumber) {   
        try {
            def data = evt.jsonData
            currentButtonNumber = evt.jsonData.buttonNumber as String
        } catch (e) {
            log.warn "caught exception getting event data as json: $e"
        }
        if (myButtonNumber != currentButtonNumber) {
        	log.debug "buttonHandler: currentButtonNumber = ${currentButtonNumber}, ignoring button event"
        	return
        }
    }    
    log.debug "buttonHandler: currentButtonAction = ${currentButtonAction}, currentButtonNumber = ${currentButtonNumber}"

    switch (currentButtonAction) {
    
    case "held":
        log.debug "buttonHandler: turning off, myDimmers = ${myDimmers}"
        for (d in myDimmers) {
        	if (d.hasCommand("off")) {
            	d.off()
            } else {
                d.setLevel(0)
            }
        }
        break
        
    case "pushed":
    	def currentLevel = getCurrentLevel(myDimmers.first())
        def newLevel = calculateNewLevel(currentLevel)
        log.debug "buttonHandler: currentLevel = ${currentLevel}, newLevel = ${newLevel}, myDimmers = ${myDimmers}"
        myDimmers.each {d -> d.setLevel(newLevel)}
        break
        
    default:
    	log.warn "buttonHandler: bad button event value \"${evt.value}\""
        return      
        
    }
    
}


/**
 * Given a dimmer device, return it's current level. Returns 0 for switch off or device doesn't exist.
 */
def getCurrentLevel(dimmer) {
	if (! dimmer) {
    	return 0
    }
    if (dimmer.currentValue("switch") == "off") {
    	return 0
    }
    return dimmer.currentValue("level") ?: 0
}


/**
 * Given a current dimmer level, calculate next value from the "myDimmerLevels" specification.
 */
def calculateNewLevel(currentLevel) {

    /*
     * I've got a bug where this app is setting my GE Link bulbs at 60 but they read
     * back as 59. This means they always get set to 60 and never change.
     * This tweak ensures it will go on to the next step.
     */
     currentLevel += 5;

	/*
     * There is a bug where if I run this in the emulator, myDimmerLevels is an array
     * but in the device it is a string.
     */
    def levels = [100]
    if (myDimmerLevels instanceof List) {
    	levels = myDimmerLevels*.toInteger()
    } else if (myDimmerLevels instanceof String) {
        levels = myDimmerLevels.split(",")*.toInteger()
    } else {
    	log.warn "calculateNewLevel: bad myDimmerLevels value ${myDimmerLevels.dump()}"
    }
    
    for (lvl in levels) {
    	if (currentLevel < lvl) {
        	return lvl
        }
    }
    return levels.first()
}
