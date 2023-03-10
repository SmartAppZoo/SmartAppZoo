/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Notify Me When There's Motion
 *
 *  Author: Steve Short
 */

definition(
	name: "Notify Me When There's Motion",
	namespace: "stshort",
	author: "Steve Short",
	description: "Send notification(s) when there is motion.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion-presence.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion-presence@2x.png"
)

preferences {
	section("When there's movement...") {
		input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("Only when this door is closed...") {
		input "door", "capability.contactSensor", title: "This door is closed ?"
	}
	section("Between these times...") {
		input "startTime", "time", title: "From what time?", required: false
		input "endTime", "time", title: "Until what time?", required: false
	}
	section ("Notification...") {
		input "actionType", "enum", title: "Action?", required: true, defaultValue: "Bell 1", options: [
			"Custom Message",
			"Bell 1",
			"Bell 2",
			"Dogs Barking",
			"Fire Alarm",
			"The mail has arrived",
			"A door opened",
			"There is motion",
			"Smartthings detected a flood",
			"Smartthings detected smoke",
			"Someone is arriving",
			"Piano",
			"Lightsaber"]
		input "message","text",title:"Message", required:false, multiple: false
	}
	section {
		input "speaker", "capability.musicPlayer", title: "On these Speakers", required: false, multiple: true
	}
	section("More options", hideable: true, hidden: true) {
		input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
	}

	//section("Only when mode is...") {
		  //input "modeIs", "mode", title: "Mode is equal to ?"
		//mode(name: "modeIs", title: "Set for specific mode(s)")
	//}
}

def installed() {
	init()
}

def updated() {
	unsubscribe()
	init()
}

def init() {
	loadText()

	if (!startTime) {
		log.debug ("No start time - defaulting to 07:00")
		//state.startTime = getDateForTime(new Date(), 7, 0, 0)
		state.startTime = timeToday("07:00", location.timeZone)
	} else {
		state.startTime = timeToday (startTime, location.timeZone)
	}
	
	if (!endTime) {
		log.debug ("No end time - defaulting to 22:00")
		//state.endTime = getDateForTime(new Date(), 22, 0, 0)
		state.endTime = timeToday("22:00", location.timeZone)
	} else {
		state.endTime = timeToday (endTime, location.timeZone)
	}
  
	  if (message) {
		state.message = message
	} else {
		state.message = "Ding Dong!"
	}
	
	logSettings()
	subscribe(motion1, "motion.active", motionActiveHandler)
}

def logSettings() {

	log.debug "Settings:"
	log.debug "  Door is $door"
	log.debug "  Start time is ${state.startTime}"
	log.debug "  End time is ${state.endTime}"
	log.debug "  Action type is $actionType"
	log.debug "  Message is $message"
	log.debug "  Speaker is $speaker"
	log.debug "  Sound is ${state.sound.uri}"

	//log.debug "Mode is $modeIs"
}

def getDateForTime(date, hour, minute, second) {
	Calendar calendar = Calendar.getInstance();
	calendar.setTime(date);
	calendar.set(Calendar.HOUR, hour);
	calendar.set(Calendar.MINUTE, minute);
	calendar.set(Calendar.SECOND, second);

	return calendar.getTime();
}

def checkTime() {
	def timeNow = new Date()
	
	log.debug "Checking time $timeNow"
	
	def start = Date.parse( "yyyy-MM-dd'T'HH:mm:ss", state.startTime )
	def end = Date.parse( "yyyy-MM-dd'T'HH:mm:ss", state.endTime )
	
	// if between the start and end time
	if(timeOfDayIsBetween(start, end, timeNow, location.timeZone))
	{
		return true
	}
	
	return false
}

def motionActiveHandler(evt) {
	log.trace "$evt.value: $evt, $settings"

	def contactState = door.currentState("contact")
	//def m1 = modeIs
	
	log.debug "Door is ${contactState.value}"
	log.debug "Current mode is ${location.mode}"
	
   if (contactState.value == "closed") {
		   
		//if (location.mode == modeIs) {
		//	log.debug "Mode matches ${modeIs}"
		//} else {
		//	log.debug "Mode does not match ${modeIs}"
		//}
		
		   if (checkTime()) {
			   log.debug "Need to send notification!"
			   sendPush state.message
			   
			   if (speaker) {
					   log.debug "Sending to speaker ${speaker}"
					speaker.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
			   }
		   } else {
			   log.debug "Outside time range for notifications"
		   }
		   
	} else {
		log.debug "Doing nothing because the door is open"
	}
}

private loadText() {
	switch ( actionType ) {
		case "0":
		case "Custom Message":
			if (message) {
				state.sound = textToSpeech(message instanceof List ? message[0] : message) // not sure why this is (sometimes) needed)
			}
			else {
				state.sound = textToSpeech("You selected the custom message option but did not enter a message in the $app.label Smart App")
			}
			break;
		case "1":
		case "Bell 1":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/bell1.mp3", duration: "10"]
			break;
		case "2":
		case "Bell 2":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/bell2.mp3", duration: "10"]
			break;
		case "3":
		case "Dogs Barking":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/dogs.mp3", duration: "10"]
			break;
		case "4":
		case "Fire Alarm":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/alarm.mp3", duration: "17"]
			break;
		case "5":
		case "The mail has arrived":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/the+mail+has+arrived.mp3", duration: "1"]
			break;
		case "6":
		case "A door opened":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/a+door+opened.mp3", duration: "1"]
			break;
		case "7":
		case "There is motion":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/there+is+motion.mp3", duration: "1"]
			break;
		case "8":
		case "Smartthings detected a flood":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/smartthings+detected+a+flood.mp3", duration: "2"]
			break;
		case "9":
		case "Smartthings detected smoke":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/smartthings+detected+smoke.mp3", duration: "1"]
			break;
		case "10":
		case "Someone is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/someone+is+arriving.mp3", duration: "1"]
			break;
		case "11":
		case "Piano":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/piano2.mp3", duration: "10"]
			break;
		case "12":
		case "Lightsaber":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/lightsaber.mp3", duration: "10"]
			break;
		default:
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/bell2.mp3", duration: "10"]
			break;
	}
}