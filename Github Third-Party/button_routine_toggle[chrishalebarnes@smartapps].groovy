/**
 *  Button Routine Toggle
 *
 *  Copyright 2018 Chris Barnes
 *
 */
definition(
    name: "Button Routine Toggle",
    namespace: "chrishalebarnes",
    author: "Chris Barnes",
    description: "Toggles routines via a button press",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "settingsPage")
}

def settingsPage() {
    dynamicPage(name: "settingsPage", title: "", install: true, uninstall: true) {
        section {
            input(name: "buttonPanel", type: "capability.button", title: "Button Panel", multiple: false, required: true, submitOnChange: true)
        }
        if(buttonPanel) {
        	def buttons = getButtons()
            def actions = location.helloHome?.getPhrases().collect({ it.label }).sort().collect({ ["${it}":it] })
            log.debug "actions ${actions}"
            if(buttons && actions) {
            	buttons.each {
                	def buttonName = it
                	section("Actions for ${it}") {
                        input "${buttonName}OnAction",  "enum", title: "Routine to execute when toggled on",  options: actions, required: false
                        input "${buttonName}OffAction", "enum", title: "Routine to execute when toggled off", options: actions, required: false
                    }
                }
            }
        }
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
    subscribe(buttonPanel, "button", buttonHandler)
    getButtons().each {
    	state[it] = "off"
    }
}

def getButtons() {
	buttonPanel.getSupportedAttributes().findAll({ (it as String)
                                        .startsWith("button") })
           								.collect({ it.toString() }).sort()
}

def buttonHandler(evt) {
    def data = parseJson(evt.data)
    // ZBWS3B forces button names like button, button2, button3, this ternary normalizes that
    def buttonName = (data.buttonNumber == "1" && (settings["buttonOnAction"] || settings["buttonOffAction"])) ? "button" : "button${data.buttonNumber}"

    if(state[buttonName] == "on") {
    	log.debug "Executing ${buttonName}OffAction"
    	location.helloHome?.execute(settings["${buttonName}OffAction"])
        state[buttonName] = "off"
    } else if(state[buttonName] == "off") {
    	log.debug "Executing ${buttonName}OnAction"
    	location.helloHome?.execute(settings["${buttonName}OnAction"])
        state[buttonName] = "on"
    }
}
