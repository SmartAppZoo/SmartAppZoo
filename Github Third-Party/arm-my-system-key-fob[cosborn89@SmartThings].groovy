/**
 * Arm My System Key Fob
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
    name: "Arm My System Key Fob",
    namespace: "tierneykev",
    author: "Kevin Tierney",
    description: "Arm and disarm the system with a Securifi Key Fob or other button device.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    lastUpdated: "2015-Dec-05 21:29")

preferences {
    page(name: "page1")  
    page(name: "page2")    
}

def page1() {

    // build sorted list of available actions
    def actions = location.helloHome?.getPhrases()*.label.sort()
    if (! actions) {
        log.error "dynamicPrefs: failed to retrieve available actions"
    }

    dynamicPage(name: "page1", uninstall: true, nextPage: "page2") {
        section("Select Key Fob Device") {  
            input "buttonDevice", "capability.button", \
            	title: "Select button device:", required: true 
        }
        section("Configure Arm Button") {
            input "armButtonNumber", "enum", \
            	title: "Button number:", options: ["1","2","3","4"], required: true, defaultValue: "1"   
            paragraph "On the Securifi Key Fob, the \"arm\" button is the top left button and should be identified as number 1."
            input "armButtonAction", "enum", \
            	title: "Run this action when pressed:", options: actions, required: true     
        }
        section("Configure Disarm Button") {
            input "disarmButtonNumber", "enum", \
            	title: "Button number:", options: ["1","2","3","4"], required: true, defaultValue: "3"  
            paragraph "On the Securifi Key Fob, the \"disarm\" button is the bottom left button and should be identified as number 3."
            input "disarmButtonAction", "enum", \
            	title: "Run this action when pressed:", options: actions, required: true 
        }
        section("Configure Home Button (optional)") {
            input "homeButtonNumber", "enum", \
            	title: "Button number:", options: ["1","2","3","4"], required: false, defaultValue: "2"   
            paragraph "On the Securifi Key Fob, the \"home\" button is the top right button and should be identified as number 2."
            input "homeButtonAction", "enum", \
            	title: "Run this action when pressed:", options: actions, required: false 
            paragraph "Leave this unassigned if you don't want to assign an action here."
        }
        section("Configure Star Button") {
        	paragraph "On the Securifi Key Fob, the \"star\" button is used only for pairing/unpairing and cannot be assigned an action."
        }
        section("Feedback") {
            input "switches", "capability.switch", \
            	title: "Select device:", required: false, multiple: true           
       		paragraph "If you select a device, it will flash on and off to confirm an action occurred. (If the device is already on, it flashes off and on.) For example, my porch light flashes after the key fob arms or disarms the system."           
       }
    }
}


def page2() {
	def defName = "Arm My System with ${buttonDevice}"
    dynamicPage(name: "page2", uninstall: true, install: true) {
    	section([mobileOnly:true]) {
        	label title: "Assign a name", required: false, defaultValue: defName
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
    subscribe(buttonDevice, "button", buttonHandler)
}


def buttonHandler(evt) { 
    //def currentButtonAction = evt.value
    def currentButtonNumber = null    
    try {
        def data = evt.jsonData
        currentButtonNumber = evt.jsonData.buttonNumber as String
    } catch (e) {
        log.warn "caught exception getting event data as json: $e"
    }
    log.debug "buttonHandler: buttonDevice = ${buttonDevice}, currentButtonNumber = ${currentButtonNumber}"

	// Build up map of button actions for easy decoding
	def actions = [:]
    if (armButtonNumber && armButtonAction) {
    	actions[armButtonNumber] = armButtonAction
    }
    if (disarmButtonNumber && disarmButtonAction) {
    	actions[disarmButtonNumber] = disarmButtonAction
    }
    if (homeButtonNumber && homeButtonAction) {
    	actions[homeButtonNumber] = homeButtonAction
    }
    
    if (! actions[currentButtonNumber]) {
    	log.debug "buttonHandler: no action assigned to button ${currentButtonNumber}"
        return
    }
    
    def runAction = actions[currentButtonNumber]    
    log.debug "buttonHandler: runAction = ${runAction}"    
    sendNotificationEvent("Button ${currentButtonNumber} on ${buttonDevice} was pressed, so I'm running ${runAction}")
    location.helloHome?.execute(runAction)

	if (switches) {
    flashLights()
    }
	
}
private flashLights() {
	def doFlash = true
	def onFor = onFor ?: 1000
	def offFor = offFor ?: 1000
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 1L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
}