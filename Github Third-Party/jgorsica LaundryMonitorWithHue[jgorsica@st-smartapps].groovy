/**
 *  Laundry Monitor
 *
 *  Author: John Gorsica
 *
 *  Sends a message and (optionally) turns on or blinks a light to indicate that laundry is done.
 *
 *  Date: 2013-02-21
 */

definition(
	name: "Laundry Monitor with Hue",
	namespace: "jgorsica",
	author: "John Gorsica",
	description: "Sends a message and (optionally) turns on or blinks a light to indicate that laundry is done.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/FunAndSocial/App-HotTubTuner%402x.png"
)

preferences {
	section("Tell me when this washer/dryer has stopped..."){
		input "sensor1", "capability.accelerationSensor"
	}
	section("Via this number (optional, sends push notification if not specified)"){
		input "phone", "phone", title: "Phone Number", required: false
	}
    section("Control these bulbs...") {
		input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
	}
	section("Choose light effects...")
		{
			input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: ["Red","Green","Blue","Yellow","Orange","Purple","Pink"]
			input "lightLevel", "enum", title: "Light Level?", required: false, options: [[0:"Off"],[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
		}
    section("Until this door switch opens") {
		input "contact", "capability.contactSensor", required: false, multiple: false, title: "Which door?"
	}
	section("Time thresholds (in minutes, optional)"){
		input "cycleTime", "decimal", title: "Minimum cycle time", required: false, defaultValue: 10
		input "fillTime", "decimal", title: "Time to fill tub", required: false, defaultValue: 5
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(sensor1, "acceleration.active", accelerationActiveHandler)
	subscribe(sensor1, "acceleration.inactive", accelerationInactiveHandler)
    subscribe(contact, "contact.open", contactOpenHandler)
}

def accelerationActiveHandler(evt) {
	log.trace "vibration"
	if (!state.isRunning) {
		log.info "Arming detector"
		state.isRunning = true
		state.startedAt = now()
	}
	state.stoppedAt = null
}

def accelerationInactiveHandler(evt) {
	log.trace "no vibration, isRunning: $state.isRunning"
	if (state.isRunning) {
		log.debug "startedAt: ${state.startedAt}, stoppedAt: ${state.stoppedAt}"
		if (!state.stoppedAt) {
			state.stoppedAt = now()
			runIn(fillTime * 60, checkRunning, [overwrite: false])
		}
	}
}

def contactOpenHandler(evt) {
	if (state.setColor){
    	state.setColor=false
        state.isRunning = false
		state.stoppedAt = now()
        hues.each {
        	log.debug "switch state: ${it.currentValue("switch")}"
        	if (it.currentValue("switch")==closed){
            	it.setColor(state.previous[it.id])
            }
        }
    }
}

def checkRunning() {
	log.trace "checkRunning()"
	if (state.isRunning) {
		def fillTimeMsec = fillTime ? fillTime * 60000 : 300000
		def sensorStates = sensor1.statesSince("acceleration", new Date((now() - fillTimeMsec) as Long))

		if (!sensorStates.find{it.value == "active"}) {

			def cycleTimeMsec = cycleTime ? cycleTime * 60000 : 600000
			def duration = now() - state.startedAt
			if (duration - fillTimeMsec > cycleTimeMsec) {
				log.debug "Sending notification"

				def msg = "${sensor1.displayName} is finished"
				log.info msg

				if (phone) {
					sendSms phone, msg
				} else {
					sendPush msg
				}

				if (hues) {
					takeAction()
				}
			} else {
				log.debug "Not sending notification because machine wasn't running long enough $duration versus $cycleTimeMsec msec"
			}
			state.isRunning = false
			log.info "Disarming detector"
		} else {
			log.debug "skipping notification because vibration detected again"
		}
	}
	else {
		log.debug "machine no longer running"
	}
}

private takeAction() {

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
	state.colorSet=true
	def newValue = [hue: hueColor, saturation: 100, level: (lightLevel as Integer) ?: 100]
	log.debug "new value = $newValue"
	if(lightLevel as Integer==0){
    	hues*.off()
    } else{
        hues*.setColor(newValue)
    }
}

