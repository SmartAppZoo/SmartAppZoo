/**
 *  Hue Effect Toggler with Button
 *
 */
definition(
    name: "Hue Effect Toggler with Button",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Cycle through hue lighting effects by pressing a button",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When this button is pressed:") {
    	input "button", "capability.button"
    }

	section("Turn on this switch:") {
    	input "onLight", "capability.switch", multiple: false, required: false
    }
    
	section("And cycle colors on these Hue Bulbs:") {
    	input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
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
	subscribe(button, "button", pushHandler)
    state.effect="gold"
    state.hue=18.4
}

def pushHandler(evt) {
    // Handle the button being pushed
    
	log.debug "$evt.name: $evt.value"
    
    
    if ((evt.value == "released") || (evt.value == "off")) {
    	def lightSwitchOn=true
        if (onLight)
            if (onLight.currentSwitch=="off"){
            	log.debug onLight.currentSwitch
                lightSwitchOn=false
                log.debug "turning on light switches: ${onLight}"
                onLight.on()
            }
        if(lightSwitchOn)
        	hueNext()
        def newValue = [hue: state.hue, saturation: 100, level: 100]
        log.debug "new value = $newValue"
        hues*.setColor(newValue)
    }
}
def hueNext(){
	log.debug "advance color... previous effect = $state.effect"
	if (state.effect=="gold") {
    	state.effect="midnight"
        state.hue=73.2
    }
    else if (state.effect=="midnight") {
    	state.effect="laser"
        state.hue=0

    }
    else if (state.effect=="laser") {
    	state.effect="daylight"
        state.hue=55
    }
    else if (state.effect=="daylight") {
    	state.effect="meadow"
        state.hue=33.9
    }
    else if (state.effect=="meadow") {
    	state.effect="gold"
        state.hue=18.2
    }
    //else if (state.effect=="gold") {
    //	state.effect="calm"
    //    state.hue=18.4
    //}
}
