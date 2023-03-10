/*
 *  Light Helper - Helps deal with complex switch-bulb interactions
 */

definition(
    name: "Light Helper",
    namespace: "KristopherKubicki",
    author: "kristopher@acm.org",
    description: "Fixes complex interactions between switches and connected lights",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png")

preferences {
    section("Tie these bulbs...") {
        input "bulbs", "capability.switch", title:"Which bulbs", multiple:true, required:true
    }
    section("To this switch") {
		input "switches", "capability.switch", title: "Which switch?", multiple:false, required: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

private def initialize() {
	log.debug("initialize() with settings: ${settings}")
	subscribe(switches, "switch", switchHandler)
}

def switchHandler(evt) { 
	if(evt.value == "on") {
    	for(bulb in bulbs) {
        	if(bulb.currentValue("switch") != "on") { 
    			bulb.on()
        		bulb.refresh()
            }
        }
    }
    if(evt.value == "off") { 
    	for(bulb in bulbs) { 
        	if(bulb.currentValue("switch") != "off") { 
    			bulb.off()
                //bulb.setLevel(100)
            }
        }
    }
}
