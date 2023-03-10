import groovy.time.TimeCategory

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
    			input "theLock", "capability.Lock", title: "Lock:", required: true
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
    subscribe(theswitch, "switch", switchHandler)
    subscribe(theLock, "lock", lockHandler)
   // subscribe(location, "routineExecuted", routineChanged)
    log.debug "when selected switch $theswitch is turned on"
	log.debug "run the selected action: $onAction"
    log.debug "Wait  $delay_seconds seconds" 
    log.debug "then lock these locks: $theLock"
}

def switchHandler(evt) {
    if (evt.value == "on") {
		log.debug "Switch turned on, will execute action ${settings.onAction}"	 
        location.helloHome?.execute(settings.onAction)

        log.debug "Will lock the folowing lock: ${settings.theLock} in ${settings.delay_seconds} seconds"


		use( TimeCategory ) {
        	def currentDate =  new Date()
    		def runAtTime =  currentDate + settings.delay_seconds.seconds
            log.debug "Door: ${settings.theLock} will lock at: ${runAtTime}"
            runOnce(runAtTime, lockDoor)
		}
        
        //runIn(settings.delay_seconds, lockDoor)
        log.debug "Flipping switch: ${settings.theswitch} back off"        
        theswitch.off()
    }
}

def lockDoor() {
	log.debug "lockDoor triggered. Door will lock"        
	theLock.lock()
}


def routineChanged(evt) {
    log.debug "routineChanged: $evt"
    log.debug "evt name: ${evt.name}"
    log.debug "evt value: ${evt.value}"
    log.debug "evt displayName: ${evt.displayName}"
    log.debug "evt descriptionText: ${evt.descriptionText}"
} 



def lockHandler(evt)
{
    log.debug "Lock ${evt.name} is ${evt.value}."
    if (evt.value == "locked") {                  // If the human locks the door then...
        //log.debug "Cancelling previous lock task..."
       // unschedule( lockDoor )                  // ...we don't need to lock it later.
    }
    else {                                      // If the door is unlocked then...
        //def delay = minutesLater * 60          // runIn uses seconds
        //log.debug "Re-arming lock ."
        //runIn( delay, lockDoor )                // ...schedule to lock in x minutes.
    }    
}
    