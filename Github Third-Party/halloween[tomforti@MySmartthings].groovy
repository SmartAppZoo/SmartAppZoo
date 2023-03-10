definition(
    name: "Halloween",
    namespace: "Halloween",
    author: "Tom Forti",
    description: "Halloween",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
preferences
{
    section("Detect movement on...") {
        input "motion_detector", "capability.motionSensor", title: "Where?"
    }
    section("Things to turn on 1st.") {
        input "switch1", "capability.switch", multiple: true    
    }
    section("Things to turn on 2nd.") {
        input "switch2", "capability.switch", multiple: true
    }    
    section("Outside Lights") {
        input "switch3", "capability.switch", multiple: true
    } 
    section("Door that triggers off command") {
        input "door", "capability.contactSensor"
    }    
}

def installed()
{
    initialize()
}

def updated()
{
    unsubscribe()
    unschedule()
    initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    subscribe(motion_detector, "motion", motionHandler)
	subscribe(door, "contact", doorHandler)}

def motionHandler(evt)
{
	if (evt.value == "active") 
    {
    	log.debug "${evt.name} is ${evt.value}."
    	if (door.latestValue("contact") == "closed") 
        {
    		start()
    	}
        
     }
}

def start()
{
	log.debug "Starting Event."
    switch3.off()
    switch1.on()
    runIn(4, secondswitch)
}

def secondswitch()
{
	switch2.on()
}

def doorHandler(evt)
{
	if (evt.value == "open")
    {
		log.debug "${evt.name} is ${evt.value}."
        unschedule()
        end()
    }   
/*	if (evt.value == "closed")
    {
    	switch3.off()
        log.debug "${evt.name} is ${evt.value}."

    }    
*/    
}

def end()
{
    log.debug "Stopping Event."
    switch1.off()
    switch2.off()
    switch3.on()
}


    