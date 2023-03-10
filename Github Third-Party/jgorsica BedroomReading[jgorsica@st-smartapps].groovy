/**
 *  Hue Effect Toggler with Button
 *
 */
definition(
    name: "Bedroom Reading",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Uses Buttons to cycle through bedroom light states:  off, reading left, reading right, reading both, and on.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Bedside Buttons that will...") {
    	input "buttonLeft", "capability.button",title: "Left?", required: false
        input "buttonRight", "capability.button",title: "Right?", required: false
    }

	section("...turn on this switch...") {
    	input "lightSwitch", "capability.switch", multiple: false, required: false
    }
    
	section("...and control these lights:") {
    	input "huesLeft", "capability.colorControl", title: "Left?", required:true, multiple:true
        input "huesRight", "capability.colorControl", title: "Right?", required:true, multiple:true
    }
	section("Optionally say good night when lights turned off via buttons?") {
    	input "goodNightActive", "boolean"
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
	subscribe(buttonLeft, "button", leftPushHandler)
    subscribe(buttonRight, "button", rightPushHandler)
    subscribe(lightSwitch, "switch", switchHandler)
    state.setting="off"
}

def leftPushHandler(evt) {
    // Handle the button being pushed
    
	log.debug "Left: $evt.name: $evt.value"
    if (evt.value == "pushed") {
    	log.debug "Previous State: $state.setting"
    	if(doubleTapL()){
        	huesLeft.each{
                it.off()
            }
            huesRight.each{
                it.off()
            }
            state.setting="off"
            goodNight()
        }
        else if (lightSwitch)
            if (lightSwitch.currentSwitch=="off"){
                log.debug "turning on light switch: ${lightSwitch}"
                lightSwitch.on()
            }
        else if(state.setting=="on"){
        	def newValue = [hue: 22, saturation: 19, level: 64]
            log.debug "new value = $newValue"
        	huesLeft*.setColor(newValue)
            huesRight*.setColor(newValue)
            state.setting="readBoth"
        }
        else if((state.setting=="readBoth")){
        	huesLeft*.off()
            state.setting="readRight"
        }
        else if((state.setting=="readRight")){
        	huesLeft*.on()
            state.setting="readBoth"
        }
        else if((state.setting=="readLeft")){
        	huesLeft*.off()
            state.setting="off"
            goodNight()
        }
        else if((state.setting=="off")){
        	huesLeft*.on()
            state.setting="readLeft"
        }
        log.debug "New State: $state.setting"
    }
}

def rightPushHandler(evt) {
    // Handle the button being pushed
    
	log.debug "Right: $evt.name: $evt.value"
    if (evt.value == "pushed") {
        log.debug "Previous State: $state.setting"
        if(doubleTapR()){
        	huesLeft.each{
                it.off()
            }
            huesRight.each{
                it.off()
            }
            state.setting="off"
            goodNight()
        }
        else if (lightSwitch)
            if (lightSwitch.currentSwitch=="off"){
                log.debug "turning on light switch: ${lightSwitch}"
                lightSwitch.on()
            }
        else if(state.setting=="on"){
        	def newValue = [hue: 22, saturation: 19, level: 64]
            log.debug "new value = $newValue"
        	huesLeft*.setColor(newValue)
            huesRight*.setColor(newValue)
            state.setting="readBoth"
        }
        else if((state.setting=="readBoth")){
        	huesRight*.off()
            state.setting="readLeft"
        }
        else if((state.setting=="readLeft")){
        	huesRight*.on()
            state.setting="readBoth"
        }
        else if((state.setting=="readRight")){
        	huesRight*.off()
            state.setting="off"
            goodNight()
        }
        else if((state.setting=="off")){
        	huesRight*.on()
            state.setting="readRight"
        }
    	log.debug "New State: $state.setting"
    }
}

def switchHandler(evt){
    if (evt.value == "on"){
        def newValue = [hue: 21, saturation: 32, level: 99]
        log.debug "new value = $newValue"
        huesLeft*.on()
        huesRight*.on()
        huesLeft.each{
            it.setColor(newValue)
        }
        huesRight.each{
            it.setColor(newValue)
        }
        huesRight.each{
            it.setColor(newValue)
        }
        state.setting="on"
    } else{
        huesLeft.each{
            it.off()
        }
        huesRight.each{
            it.off()
        }
        state.setting="off"
    }
}

def goodNight() {
	if(goodNightActive){
    	//say goodnight
    }
}

def boolean doubleTapL() {
	def ret = false
	def recentStates = buttonLeft.statesSince("button", new Date(now() - 2000))
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"
    if (recentStates?.size()>2) {
        log.debug "detected two taps"
        ret = true
    }
    return ret
}

def boolean doubleTapR() {
	def ret = false
	def recentStates = buttonRight.statesSince("button", new Date(now() - 2000))
	log.debug "${recentStates?.size()} STATES FOUND, LAST AT ${recentStates ? recentStates[0].dateCreated : ''}"
    if (recentStates?.size()>2) {
        log.debug "detected two taps"
        ret = true
    }
    return ret
}
