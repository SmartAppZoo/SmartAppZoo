/**
 *  ADT Alert Any Sensor
 *
 *  Copyright 2018 CRAIG KING
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
 */
definition(
    name: "ADT Alert Any Sensor",
    namespace: "Mavrrick",
    author: "CRAIG KING",
    description: "Smartthing ADT tools for additional functions ",
    category: "Safety & Security",
    parent: "Mavrrick:ADT Tools",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/* 
* 10/27/2018 1.0.3
* Added command to set dimmer lights to 100% when just being turned on. 
* Updadated record check routine to look at panel for current state.
* Camera recording can not be set to repeat during a alarm event.
*
* 9/23/2018 1.0.2
* Added Camera rotine and parameters to enable cameras during alarm event.
*
* 8/6/2018 1.0.1
* Changed Trigger to be activated with the hub alarm state is in a armed mode
*
* Initial release v1.0.0
* Trigger action based on ADT Alarm. This is intial release of child app
* 
*/
import groovy.time.TimeCategory

preferences {
	    section("Monitor these devices for activity when alarm is armed"){
		paragraph "What Active alarm mode do you want to monitor for 1= Arm/Stay, 2=Armed/Away. All other numberical valudes wil be ignored"
        	input "alarmtype2", "number", title: "What type of alarm do you want to trigger", required: false, defaultValue: 1        
        	input "motion", "capability.motionSensor", title: "Motion Sensor?", required: false , multiple: true
			input "contact", "capability.contactSensor", title: "Door or Window Sensor?", required: false, multiple: true
		}
		section("Select your ADT Smart Panel..."){
		input "panel", "capability.battery", title: "ADT Panel?", required: true
	}
		section("Action to trigger"){
        	input "alarms", "capability.alarm", title: "Which Alarm(s) to trigger when ADT alarm goes off", multiple: true, required: false
        paragraph "Valid alarm types are 1= Siren, 2=Strobe, and 3=Both. All other numberical valudes wil be ignored"
        	input "alarmtype", "number", title: "What type of alarm do you want to trigger", required: false, defaultValue: 3
        paragraph "Valid Light actions are are 1 = None, 2 = Turn on lights, 3 = Flash Lights and 4 = Both. All other numberical valudes wil be ignored"
        	input "lightaction", "number", title: "What type of light action do you want to trigger", required: true, defaultValue: 1
        paragraph "If you choose Light action 4 do not select the same lights in both values"
        	input "switches2", "capability.switch", title: "Turn these lights on if Light action is set to 2 or 4", multiple: true, required: false
        	input "switches", "capability.switch", title: "Flash these lights (optional) If Light action is set to 3 or 4", multiple: true, required: false
			}
    	section("Camera setup (Optional)"){
        	input "recordCameras", "bool", title: "Enable Camera recording?", description: "This switch will enable cameras to record on alarm events.", defaultValue: false, required: true, multiple: false
			input "recordRepeat", "bool", title: "Enable Camare to trigger recording as long as arlarm is occuring?", description: "This switch will enable cameras generate new clips as long as thre is a active alarm.", defaultValue: false, required: true, multiple: false
			input "cameras", "capability.videoCapture", multiple: true, required: false
        	input name: "clipLength", type: "number", title: "Clip Length", description: "Please enter the length of each recording", required: true, range: "5..120", defaultValue: 120
        }
    	section("Flashing Lights setup (Optional)"){
		input "onFor", "number", title: "On for (default 5000)", required: false
		input "offFor", "number", title: "Off for (default 5000)", required: false
        input "numFlashes", "number", title: "This number of times (default 3)", required: false
		}
        section("Via a push notification and/or an SMS message"){
        input "message", "text", title: "Send this message if activity is detected", required: false
        }
        section("Via a push notification and/or an SMS message"){
			input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Enter a phone number to get SMS", required: false
		paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
		}
	}
		section("Minimum time between messages (optional, defaults to every message)") {
			input "frequency", "decimal", title: "Minutes", required: false
	}
	}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
        subscribe(location, "securitySystemStatus", alarmHandler)
	if (contact) {
		subscribe(contact, "contact.open", triggerHandler)
	}
	if (motion) {
		subscribe(motion, "motion.active", triggerHandler)
	}
}

def devices = " "

def alarmHandler(evt) {
	if (evt.value == 'disarmed') {
    log.debug "Alarm switch to disarmed. Turing off siren."
    	alarms?.off() }
}

def triggerHandler(evt) {
/*        def alarmState = panel.currentSecuritySystemStatus  */
        def alarmState = location.currentState("alarmSystemStatus")?.value
		if (alarmState == "stay" && alarmtype2 == 1) {
        log.debug "Current alarm mode: ${alarmState}."
		alarmAction()
        }
        else if (alarmState == "away" && alarmtype2 == 2) {
        log.debug "Current alarm mode: ${alarmState}."
        alarmAction()
        }
        else
        log.debug "Current alarm mode: ${alarmState}. Ignoring event"
    }
    
def alarmAction()    
	{
/*	log.debug "Notify got alarm event ${evt}"
    log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"
        log.debug "The event id to be compared is ${evt.value}"     
		def devices = settings.contact
        log.debug "These devices were found ${devices.id} are being reviewed."
    	devices.findAll { it.id == evt.value } .each { 
        log.debug "Found device: ID: ${it.id}, Label: ${it.label}, Name: ${it.name}, Is water Event" */
        switch (alarmtype.value)
        	{
            	case 1 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on siren"
                    alarms?.siren()
                    break
                case 2 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on strobe"
                    alarms?.strobe()
                    break
                case 3 :
                	log.debug "Alarm type ${alarmtype.value} detected. Turning on Siren and Strobe"
                    alarms?.both()
                    break
                default:
					log.debug "Ignoring unexpected alarmtype mode."
        			log.debug "Alarm type ${alarmtype.value} detected"
                    break
                    }
         switch (lightaction.value)
        	{
            	case 1 :
                	log.debug "Light action ${lightaction.value} detected. No Light Action"                    
                    break
                case 2 :
                	log.debug "Light action ${lightaction.value} detected. Turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    break
                case 3 :
                	log.debug "Light Action ${lightaction.value} detected. Flashing Selected lights"                    
                    flashLights()
                    break
                case 4 :
                	log.debug "Light Action ${lightaction.value} detected. Flash and turning on selected lights"
                    switches2?.on()
                    switches2?.setLevel(100)
                    flashLights()
                    break
                default:
					log.debug "Ignoring unexpected Light Action type."
        			log.debug "Light Action ${lightaction.value} detected"
                    break
			} 		
sendnotification()
	if (settings.recordCameras)
	{
		cameraRecord()
	}
}


def continueFlashing()
{
	unschedule()
	if (state.alarmActive) {
		flashLights(10)
		schedule(util.cronExpression(now() + 10000), "continueFlashing")
	}
}

private flashLights() {
	def doFlash = true
	def onFor = onFor ?: 5000
	def offFor = offFor ?: 5000
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			switches.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
}

def sendnotification() {
def msg = message
        if ( msg == null ) {
        	log.debug "Message not configured. Skipping notification"
            }
        else {
        log.debug "Alarm Notification., '$msg'"
/*        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'" */

	Map options = [:]	

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (phone) {
			options.phone = phone
			if (pushAndPhone != 'No') {
				log.debug 'Sending push and SMS'
				options.method = 'both'
			} else {
				log.debug 'Sending SMS'
				options.method = 'phone'
			}
		} else if (pushAndPhone != 'No') {
			log.debug 'Sending push'
			options.method = 'push'
		} else {
			log.debug 'Sending nothing'
			options.method = 'none'
		}
		sendNotification(msg, options)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}
}

def cameraRecord() {	
	log.debug "Refreshing cameras with ${clipLength} second capture"
    Date start = new Date()
    Date end = new Date()
    use( TimeCategory ) {
    	end = start + clipLength.seconds
 	} 
    log.debug "Capturing..."
    cameras.capture(start, start, end)
    	if (settings.recordRepeat)
	{
		runIn(clipLength, cameraRepeatChk)
	}
}

def cameraRepeatChk() {
		def alarmActive = panel.currentSecuritySystemStatus
    	log.debug "Current alarms is in ${alarmActive} state"
		if (alarmActive != "disarmed") 
        	{
        	log.debug "Alarm Event is still occuring. Submitting another clip to record"
        cameraRecord()   
        	}
		else {
        log.debug "Alarm has cleared and is no longer active recordings are stoping."
		}
        }