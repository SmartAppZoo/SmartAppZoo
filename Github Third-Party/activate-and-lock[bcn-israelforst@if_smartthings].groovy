definition(
        name: "Activate Routine & Lock Door",
        namespace: "",
        author: "Israel Forst",
        description: "This App allows you to assign a switch to trigger a routine and lock a door after a configurable delay",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
    ) 

    preferences {
    	page(name: "configure")
    }
    
def configure() {
    dynamicPage(name: "configure", title: "Configure Switch and Phrase", install: true, uninstall: true) {
            section("When this switch is turned on...") {
                    input "theswitch", "capability.switch", required: true
            }

            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            	actions.sort()
	            section("Execute this action") {                
    	            input "onAction", "enum", title: "Action:", options: actions, required: true
                }
            }
        	section("Then wait this number of seconds") {
    			input "delay_seconds",  "number", title: "Seconds:", required: true, range: "0..600"
    		}
        
    		section("And lock this door") {
    			input "locks", "capability.Lock", title: "Lock:", required: true
    		}  
    		section("Mode") {
    			input "test_mode", "boolean", title: "Run in Test Mode:"
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
    subscribe(theswitch, "switch", handler)
    subscribe(location, "routineExecuted", routineChanged)
    log.debug "selected switch $theswitch"
	log.debug "selected on action $onAction"
    log.debug "Wait  $delay_seconds"
    log.debug "then lock these locks: $locks"
}

def handler(evt) {
    if (evt.value == "on") {
		log.debug "Switch turned on, will execute action ${settings.onAction}"	
		if( settings.test_mode)  {
        	log.debug "TEST MODE: No action taken"
        } else { 
        	location.helloHome?.execute(settings.onAction)
        }

        log.debug "Will lock the folowing locks: ${settings.locks}"
        if( settings.test_mode) {
        	log.debug "TEST MODE: No action taken" 
        } else {
        	runin(settings.delay_seconds, settings.locks.lock())
        }
    }
}

def routineChanged(evt) {
    log.debug "routineChanged: $evt"
    log.debug "evt name: ${evt.name}"
    log.debug "evt value: ${evt.value}"
    log.debug "evt displayName: ${evt.displayName}"
    log.debug "evt descriptionText: ${evt.descriptionText}"
}    
    
