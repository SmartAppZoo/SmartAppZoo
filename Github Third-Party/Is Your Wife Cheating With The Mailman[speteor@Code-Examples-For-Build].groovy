/**
 *  Is Your Wife Cheating With The Mailman?
 *
 *  Author: zach@beamlee.com
 *  Date: 2013-07-17
 */
 

preferences {
	section("Your Mailbox") {
		input "mailbox", "capability.contactSensor", title: "Pick your Mailbox Sensor"
	}
	section("Your Preferred Door of Furtive Entrance") {
		input "door", "capability.contactSensor", title: "Pick your Door Sensor"
		input "timeToDoor", "decimal", title: "Maximum Time (in minutes) from Mailbox to Door"
	}
	section("The Bed Where Motion Will Be Detected.") {
		input "bed", "capability.motionSensor", title: "Pick your Bed Motion Sensor"
		input "timeToBed", "decimal", title: "Maximum Time (in minutes) from Door to Bed"
	}
	section("Time Window During Which You're Out of the House"){
		input "startTime", "time", title: "Start Time"
		input "endTime", "time", title: "End Time"
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(mailbox, "contact.closed", mailMan)
}

def mailMan(evt){
	if(inTimeWindow() == true){
		subscribe(door, "contact", hesMadeItToTheDoor)
		def freq = timeToDoor * 60000
		schedule("0 0/$freq * * * ?", unsubDoor)	
		unsubscribe(mailbox)
	}
	else{
		log.debug "Nothing suspicious...we think"
	}
}

def hesMadeItToTheDoor(evt){
	subscribe(bed, "motion", theyreOnTheBed)
	def freq = timeToBed * 60000
	def freq = 1
	schedule("0 0/$freq * * * ?", unsubBed)	
}

def theyreOnTheBed(evt){
	sendPush("Your Wife Is Cheating On You With The Mailman")
	unsubscribe(bed)
	subscribe(mailbox, "contact.closed", mailMan)
}

def unsubDoor(){
	unsubscribe(door)
	log.debug "Time Elapsed.  Unsubscribing."
}

def unsubBed(){
	unsubscribe(bed)
	log.debug "Time Elapsed.  Unsubscribing."
}
private inTimeWindow(){
	def result
	def now = now()
	if(now > startTime && now < endTime){
		return true
	}
	else{
		return false
	}
	result
}
