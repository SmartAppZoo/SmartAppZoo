definition(
    name: "Initialize with default preferences",
    namespace: "kris-schaller",
    author: "Kris Schaller",
    description: "n/a",
    category: "My Apps",
    iconUrl: "http://action-dashboard.github.io/icon.png",
    iconX2Url: "http://action-dashboard.github.io/icon.png")


preferences {
    page(name: "selectDevices")
	page(name: "preferences", title: "preferences")
}

def selectDevices() {
	if(!settings.Option1) {
    	log.debug "no install"
        dynamicPage(name: "selectDevices", nextPage: "preferences", uninstall: false) {
    		section() {
				href "preferences", title: "Preferences"
			}
    	}
    } else {
   	 	log.debug "install"
        dynamicPage(name: "selectDevices", install: true, uninstall: true) {
    		section() {
				href "preferences", title: "Preferences"
			}
    	}
    }
    
    
}

def preferences() {
	dynamicPage(name: "preferences", title: "Preferences", install: false) {
	
		section() {
           	label title: "Title", required: false, defaultValue: "${location != null ? location : "none"}"
        }
		section() {
			input "Switch", "capability.switch", title: "Switch...", multiple: true, required: false
		}
		
		section() {
			input "Option1", title: "Pref 1", "enum", multiple: false, required: true, defaultValue: "default1", options: [default: "Default option", one: "one", two: "two", three: "three"]
			input "Option2", title: "Pref 2", "enum", multiple: false, required: true, defaultValue: "default2", options: ["SmartThings", "SmarterThings", "SmartestThings"]
			input "Option3", title: "Pref 3", "enum", multiple: false, required: true, defaultValue: "default3", options: ["Red", "Blue", "Green"]
			input "Option4", title: "Pref 4", "bool", required: true, defaultValue: false
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	
	initialize()
}

def initialize() {
	log.debug "initialized with settings: ${settings}"
}