definition(
  name: "Hello World",
  namespace: "mvevitsis",
  author: "Matvei Vevitsis", 
  description: "Speak a message on a connected speaker when a switch is turned on",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  page(name: "mainPage", title: "Speak a message on a connected speaker when a switch is turned on", install: true, uninstall: true)
  page(name: "timeNotificationInput", title: "Only send notifications during a certain time:") {
		section {
			input "notifyStarting", "time", title: "Starting", required: false
			input "notifyEnding", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
 dynamicPage(name: "mainPage") {
  section("Input"){
   input "mySwitch", "capability.switch", title: "Select a switch", required: false, multiple: true
   input "myContact", "capability.contactSensor", title: "Select a contact sensor", required: false, multiple: true
}
  section("Output"){
   input "audioDevices", "capability.audioNotification", title: "Select a speaker", required: false, multiple: true
   input "sendPushMessage", "enum", title: "Send a push notification", options: ["Yes", "No"], defaultValue: "No", required: true
   href "timeNotificationInput", title: "Only during a certain time", description: getTimeLabel(notifyStarting, notifyEnding) ?: "Tap to set", state: getTimeLabel(notifyStarting, notifyEnding) ? "complete" : "incomplete"
   input "messageText", "text", title: "Message Text", defaultValue: "Hello World", required: false
  }
 }
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	return f.format(t)
}

private getTimeLabel(starting, ending) {
	return (starting && ending) ? hhmm(starting) + " - " + hhmm(ending, "h:mm a z") : ""
}
def installed(){
 subscribeToEvents()
}

def updated(){
 unsubscribe()
 subscribeToEvents()
}

def subscribeToEvents(){
 subscribe(mySwitch, "switch.on", eventHandler)
 subscribe(myContact, "contact.open", eventHandler)
}

def eventHandler(evt){
	if (isTimeOK(notifyStarting, notifyEnding)) {
		sendMessage(evt)
    } else {
    log.debug "Not sending notifications outside configured time period."
    }
}

private sendMessage(evt){
def msg = messageText
	if(sendPushMessage == "Yes") {
     	sendPush(msg)
  	}
  	if(audioDevices){
  		audioDevices?.each { audioDevice -> 
       	if (audioDevice.hasCommand("playText")) { //Check if speaker supports TTS 
             audioDevice.playText(msg)
        } else {
        if (audioDevice.hasCommand("speak")) { //Check if speaker supports speech synthesis  
       		 audioDevice.speak(msg.toString())
        } else {
             audioDevice.playTrack(textToSpeech(msg)?.uri) 
        }
        } 
  
        }
   }
}

private isTimeOK(starting, ending) {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "Time period OK to send = $result"
	return result
}

