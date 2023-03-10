import groovy.time.TimeCategory

definition(
        name: "Lock Door XX seconds After Routine Activates",
        namespace: "",
        author: "Israel Forst",
        description: "This app will lock a door after a delay",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
    ) 

    preferences {
    	page(name: "configure")
    }
    
def configure() {
    dynamicPage(name: "configure", title: "Configure Routine, Door and Delay", install: true, uninstall: true) {


            def actions = location.helloHome?.getPhrases()*.label
            if (actions) {
            	actions.sort()

                log.debug "Actions: ${actions}"
	            section("Which This Action runs") {                
    	            input "onAction", "enum", title: "Action:", options: actions, required: true
                }
            }
        section("Then wait this number of seconds") {
    			input "delay_seconds",  "number", title: "Seconds:", required: true, range: "0..600"
    		}
        
    		section("And lock this door") {
    			input "theLock", "capability.Lock", title: "Lock:", required: true
    		}          
		
        section("Run in Test Mode") {
    			input "isTestMode",  "bool", title: "Run in Test Mode:", required: true
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
    subscribe(theLock, "lock", lockHandler)
    subscribe(location, "routineExecuted", routineExecuted)
    log.debug "Loaded with settings: ${settings}"
}



def lockDoor() {
	if(! settings.isTestMode) {
     	if(theLock.currentValue("lock") == "locked") {
			log.debug "lockDoor triggered but door is already locked. Ignoring command."     
        } else {
			log.debug "lockDoor triggered. Door will lock"     
			theLock.lock()        
        }
    } else {
    	log.debug "TESTMODE: lockDoor triggered. Ignoring Command"     
    }
}


def routineExecuted(evt) {
	if (evt.name =="routineExecuted") {
    	def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
        	actions.sort()
            //if (actions.get(settings.onAction.toInteger()) == evt.displayName ) {
            if (settings.onAction == evt.displayName ) {            
            	log.debug "Selected Action Triggered. evt name: ${evt.displayName}"
				use( TimeCategory ) {
        			def currentDate =  new Date()
    				def runAtTime =  currentDate + settings.delay_seconds.seconds
            		log.debug "Door: ${settings.theLock} will lock at: ${runAtTime}"
            		runOnce(runAtTime, lockDoor)
				}                
            }
            else {
            	log.debug "Skipping. Action Triggered was: ${evt.displayName} & selected action was: ${settings.onAction}"
            }
	    }
	    	//log.debug "routineChanged: $evt"
    		//log.debug "evt name: ${evt.name}"
    		//log.debug "evt value: ${evt.value}"
    		//log.debug "evt displayName: ${evt.displayName}"
    		//log.debug "evt descriptionText: ${evt.descriptionText}"
    }
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