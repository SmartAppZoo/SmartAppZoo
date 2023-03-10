/**
 *  Someone's at the Door
 *
 *  Author: Jeff Baumgartner
 *  Date: 1/19/2015
 *
 *  Let me know when someone knocks on the door, but ignore
 *  when someone is opening the door then flash a light and play a custom message
 */

definition(
    name: "Someone's at the Front Door",
    namespace: "",
    author: "Jeff Baumgartner",
    description: "Detect a knock at the front door, flash a light, and play a sound",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png")

preferences {
  section("When Someone Knocks?") {
    input name: "knockSensor", type: "capability.accelerationSensor", title: "Where?"
  }

  section("But not when they open this door?") {
    input name: "openSensor", type: "capability.contactSensor", title: "Where?"
  }

  section("Knock Delay (defaults to 5s)?") {
    input name: "knockDelay", type: "number", title: "How Long?", required: false
  }

  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }

  section("Control these bulbs...") {
    input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:false, multiple:true
  }

  section("Choose light effects...") {
    input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: ["Red","Green","Blue","Yellow","Orange","Purple","Pink"]
    input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
    input "duration", "number", title: "Duration Seconds (Defaults to 10s)?", required: false
  }

  section("Minimum time between messages (optional, defaults to every message)") {
    input "frequency", "decimal", title: "Minutes", required: false
  }
  
  section("Speaker to Play Sound") {
    input "sonos", "capability.musicPlayer", title: "Sonos Device", required: true
    input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
  }
  
  section("What message to you want to say?") {
    input "textHere", "text", title: "Type in the message"
  }
  
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  state.lastClosed = 0
  subscribe(knockSensor, "acceleration.active", handleEvent)
  subscribe(openSensor, "contact.closed", doorClosed)
}

def doorClosed(evt) {
  state.lastClosed = now()
}


def handleEvent(evt) {
  def delay = knockDelay ?: 5
  schedule(now() + delay*1000, doorKnock())
}

def doorKnock() {
  if((openSensor.latestValue("contact") == "closed") && (now() - (1 * 1000) > state.lastClosed)) {    
    log.debug("${knockSensor.label ?: knockSensor.name} detected a knock.")
    send("${knockSensor.label ?: knockSensor.name} detected a knock.")
    sonos.setLevel(volume)
    sonos.playText(textHere)
    lightItup()
  }

  else {
    log.debug("${knockSensor.label ?: knockSensor.name} knocked, but looks like it was just someone opening the door.")
  }
}



private send(msg) {
  if(sendPushMessage != "No") {
    log.debug("Sending push message")
    sendPush(msg)
  }

  if(phone) {
    log.debug("Sending text message")
    sendSms(phone, msg)
  }

  log.debug(msg)
}

private lightItup() {

	def hueColor = 0
	if(color == "Blue")
		hueColor = 70//60
	else if(color == "Green")
		hueColor = 39//30
	else if(color == "Yellow")
		hueColor = 25//16
	else if(color == "Orange")
		hueColor = 10
	else if(color == "Purple")
		hueColor = 75
	else if(color == "Pink")
		hueColor = 83

	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}

	log.debug "current values = $state.previous"

	def newValue = [hue: hueColor, saturation: 100, level: (lightLevel as Integer) ?: 100]
	log.debug "new value = $newValue"

	hues*.setColor(newValue)
	setTimer()
}

def setTimer()
{
	if(!duration) //default to 10 seconds
	{
		log.debug "pause 10"
		pause(10 * 1000)
		log.debug "reset hue"
		resetHue()
	}
    else
	{
		log.debug "pause $duration"
		pause(duration * 1000)
		log.debug "resetHue"
		resetHue()
	}
}


def resetHue()
{
    def rstValue = [hue: 0, saturation: 0, level: 0]
	hues*.setColor(rstValue)
	hues.each {
		it.setColor(state.previous[it.id])   
	}
}  
