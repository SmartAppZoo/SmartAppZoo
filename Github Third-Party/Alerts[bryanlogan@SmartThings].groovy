
definition(
    name: "Speech, Push, Text Alerts",
    namespace: "shikkie",
    author: "Mike Moore",
    description: "Alerts for various events, with support for simple text to speech",
    category: "Convenience",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.alarm.beep.beep?displaySize=1x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.alarm.beep.beep?displaySize=2x"
)

preferences {
	section("Notify for..."){
		input "contact1", "capability.contactSensor", title: "Open/close sensors", multiple: true, required:false	
    	input "motion", "capability.motionSensor", title: "Motion sensors", multiple: true, required:false        
        input "ArrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "DeparturePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
		input "Smoke1", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
		input "Water", "capability.waterSensor", title: "Water Sensor Wet", required: false,  multiple: true
        input "SendForModeChange", "boolean", title: "Mode changes"
    }
    
    section("Notifications")
    {
    	input "SendPush", "boolean", title: "Send Push?"
        input "SendTTS", "boolean", title: "Send TTS?"
        input "TTSDevice", "capability.speechSynthesis", title: "TTS Device"
        input "SendText", "boolean", title: "Send Text?"
        input "Phone", "phone", title: "Phone Number", required: false       
    }
    
}

def installed()
{
	subscribeEvents();
}

def updated()
{
	unsubscribe()
	subscribeEvents();	
}

def subscribeEvents()
{
	subscribe(contact1, "contact.open", contactOpenHandler);
    subscribe(motion, "motion.active", motionHandler);
    subscribe(ArrivalPresence, "presence.present", presentHandler);
    subscribe(DeparturePresence, "presence.not present", leftHandler);
    subscribe(Smoke1, "smoke.detected", smokeDetectedHandler)
	subscribe(Smoke1, "smoke.tested", smokeTestedHandler)
    subscribe(Smoke1, "carbonMonoxide.detected", carbonMonoxideHandler)
	subscribe(Water, "water.wet", waterHandler)
    
   if(SendForModeChange == "true")
 		subscribe(location, modeChangeHandler);
    
}

def presentHandler(evt)
{
	sendMessage("$evt.linkText has arrived at $location.name")
}

def leftHandler(evt)
{
	sendMessage("$evt.linkText has left $location.name")
}

def smokeDetectedHandler(evt)
{
	sendMessage("Smoke detected $evt.linkText")
}

def smokeTestedHandler(evt)
{
	sendMessage("Smoke tested $evt.linkText")
}

def carbonMonoxideHandler(evt)
{
	sendMessage("Carbon monoxide detected $evt.linkText")
}

def waterHandler(evt)
{
	sendMessage("Moisture detected $evt.linkText")
}


def sendMessage(msg)
{
	log.trace "sendNotification: $msg";    
    
    if (SendPush == "true")
    {
    	sendPush(msg);
    }
    
    if (SendText == "true")
    {
    	if(Phone)
        {
        	sendSms(Phone, msg);
        }
    }
    
    if(SendTTS == "true")
    {
    
    	TTSDevice?.speak(msg);
    }
}

def contactOpenHandler(evt) {
	//log.trace "$evt.value: $evt, $settings"
    log.trace "$evt.displayName"

	log.debug "$evt.displayName was opened, sending message to user"
	//sendPush("Your ${contact1.label ?: contact1.name} was opened")
   
   sendMessage("${evt.displayName} open")
 
}


def modeChangeHandler(evt)
{
	log.trace "Mode is now: ${location.mode}";
    
	sendMessage("Mode is now: ${location.mode}");

}

def motionHandler(evt)
{
	log.trace "Motion";
    sendMessage("Motion detected by $evt.displayName");
}
    
 
