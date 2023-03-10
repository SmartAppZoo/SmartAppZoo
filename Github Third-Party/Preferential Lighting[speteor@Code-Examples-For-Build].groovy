/**
 *  My Lighting
 *
 *  Author: Zach Naimon @ Smart Things
 */
preferences {
	section("Pick your sensors") {
		input "motionSensor", "capability.motionSensor", title: "Which Motion Sensors", multiple: true, required: false
        input "openSensor", "capability.contactSensor", title: "Which Open/Close Sensors", multiple: true, required: false
        input "priority", "enum", title: "If you picked both sensors, which one do you want to come first? (Enter Motion or Contact)", required: false, metadata: [values: ["Motion","Contact"]]
	}
	section("Turn on these lights...") {
		input "lights", "capability.switch", multiple: true
	}
    section("Turn them off after..."){
    	input "offTime", "decimal", title: "Amount of time before lights turn off automatically (minutes)"
    }
}

def installed()
{
	log.debug "App Installed.  Preparing to Initialize."
    initialize()
}

def updated()
{
	unsubscribe()
	log.debug "App Updated. Preparing to Initialize."
    initialize()
}

def initialize(){
    log.debug "Made it to initialize"
	log.debug checkPriority()
    if(!motionSensor && !openSensor){
		sendPush("Sorry, you need to choose a method of switching your lights.")	
	}
	else if(checkPriority() == contact){
		
		subscribe(openSensor, "contact.open", contactFirstOpenHandler)
		subscribe(openSensor, "contact.closed", contactFirstClosedHandler)
	}
	else if(checkPriority() == motion){
		
		subscribe(motionSensor, "motion.active", motionFirstActiveHandler)
		subscribe(motionSensor, "motion.inactive", motionFirstInactiveHandler)
	}
	
	else if(checkPriority() == justmotion){
		
		subscribe(motionSensor, "motion.active", justMotionActiveHandler)
		subscribe(motionSensor, "motion.inactive", justMotionInactiveHandler)
	}
	else if(checkPriority() == justcontact){
		
        subscribe(openSensor, "contact.open", justContactOpenHandler)
		subscribe(openSensor, "contact.closed", justContactClosedHandler)
	}
	
}
def contactFirstOpenHandler(evt){
	log.debug "Door was opened...moving on to motion criteria."
    subscribe(motionSensor, "motion.active", motionSecondActiveHandler)
	subscribe(motionSensor, "motion.inactive", motionSecondInactiveHandler)
}
def contactFirstClosedHandler(evt){
	log.debug "Door was closed.  Motion should no longer have any effect."
    unsubscribe(motionSecondActiveHandler)
	unsubscribe(motionSecondInactiveHandler)
}
def motionFirstActiveHandler(evt){
	log.debug "Motion was detected...moving on to open/close criteria."
    subscribe(openSensor, "contact.open", contactSecondOpenHandler)
	subscribe(openSensor, "contact.closed", contactSecondClosedHandler)
	
}

def motionFirstInactiveHandler(evt){
	log.debug "Motion is no longer being detected.  Open/close should no longer have any effect."
    unsubscribe(contactSecondOpenHandler)
	unsubscribe(contactSecondClosedHandler)
}


def motionSecondActiveHandler(evt){
	log.debug "Door was opened...then motion was detected.  Lights turning on."
    unschedule(lightsOff)
	lights.on()
}



def motionSecondInactiveHandler(evt){
	log.debug "Motion no longer being detected.  Starting countdown for lights off."
    calcThreshLightsOff()
}

def contactSecondOpenHandler(evt){
	log.debug "Motion was detected...then the door was opened.  Lights turning on."
    unschedule(lightsOff)
	lights.on()
}

def contactSecondClosedHandler(evt){
	"Door is now closed.  Starting countdown for lights off."
    calcThreshLightsOff()
}


def justContactOpenHandler(evt){
	log.debug "There is only one sensor set: Contact Sensor.  It was opened.  Lights going on."
    unschedule(lightsOff)
	lights.on()
	
}

def justContactClosedHandler(evt){
	log.debug "There is only one sensor set: Contact Sensor.  It was closed.  Light countdown begins now."
    calcThreshLightsOff()
}

def lightsOff(){
	log.debug "Turning lights off now."
    lights.off()
}

def justMotionActiveHandler(evt){
	log.debug "There is only one sensor set: Motion Sensor.  It has detected motion.  Lights going on."
    unschedule(lightsOff)
	lights.on()
}

def justMotionInactiveHandler(evt){
	log.debug "There is only one sensor set: Motion Sensor.  It has not detected motion recently.  Light countdown begins now."
    def motionValue = motionSensor.find{it.currentMotion == "active"}
	if(!motionValue){
	calcThreshLightsOff()
	}
}

private checkPriority(){
	def result
	if(motionSensor && openSensor){
		log.debug "We have two devices inputted in settings, and..."
        if(priority == "Motion"){
			log.debug "...Motion is the priority"
            return motion	
		}
		if(priority == "Contact"){
			log.debug "...Contact is the priority"
            return contact
		}
		else{
			log.debug "...No Priority.  User can't read properly."
            sendPush("Please tell us which sensor to prioritize.  Thanks!")
		}
	}
	else{
    	if(motionSensor && !openSensor){
			log.debug "Only a motion sensor set."
        	return justmotion
		}
		if(!motionSensor && openSensor){
			log.debug "Only a contact sensor set"
        	return justcontact
		}
     }
	
	result
}


def calcThreshLightsOff(){
	def now = now()
    def offTimeInt = offTime as int
    def thresh = (offTimeInt * 60000)
    def threshTime = (now + thresh)
    def nowPlus = new Date(threshTime)
    log.debug "Lights should be turned off at $nowPlus"
    
    runOnce(nowPlus, lightsOff)
    

}
