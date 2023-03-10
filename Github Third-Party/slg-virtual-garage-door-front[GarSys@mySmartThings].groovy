/**
 *  SLG Virtual Garage Door Controller [SmartApp]
 *
 *  Copyright 2018 Scott Garver
 *
 *  Original Author: LGKahn kahn-st@lgk.com
 */
definition(
	name: "SLG Virtual Garage Door (Front)",
	namespace: "GarSys",
	author: "Scott Garver",
	description: "Sync the Simulated Garage Door device with 3 actual devices; a tilt/contact sensor and a separate opener and closer switch. The virtual device will then control the physical garage door. In addition, the virtual device will sync when the garage door is opened manually, \n It also attempts to double check if the door was actually closed in case the beam was crossed.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)
preferences {
	section("Choose the switch/relay that opens the garage."){
		input "opener", "capability.switch", title: "Physical Garage Opener?", required: true
	}
	section("Choose the switch/relay that closes the garage."){
		input "closer", "capability.switch", title: "Physical Garage Closer?", required: true
	}
	section("Choose the sensor that senses if the garage is open or closed?"){
		input "sensor", "capability.contactSensor", title: "Physical Garage Door Open/Closed sensor?", required: true
	}
	section("Choose the Virtual Garage Door device."){
		input "virtualgd", "capability.doorControl", title: "Virtual Garage Door?", required: true
	}
	section("Choose the Virtual Garage Door device sensor (same as Virtual Garage Door device)."){
		input "virtualgdsensor", "capability.contactSensor", title: "Virtual Garage Door Open/Close sensor?", required: true
	}
	section("Timeout before checking if the door operated correctly."){
		input "checkTimeout", "number", title: "Door Operation Check Timeout?", required: true, defaultValue: 25
	}
	section( "Notifications" ) {
		input("recipients", "contact", title: "Send notifications to:") {
			input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false, defaultValue: "No"
			input "phone1", "phone", title: "Send a Text Message?", required: false
		}
	}
}
def installed()
{
	def realgdstate = sensor.currentContact
	def virtualgdstate = virtualgd.currentContact
	subscribe(sensor, "contact", contactHandler)
	subscribe(virtualgdsensor, "contact", virtualgdcontactHandler)
	// sync them up if need be, set virtual same as physical
	if (realgdstate != virtualgdstate)
	{
		if (realgdstate == "open")
		{
			virtualgd.open()
		}
		else {
			virtualgd.close()
		}
	}
}
def updated()
{
	def realgdstate = sensor.currentContact
	def virtualgdstate = virtualgd.currentContact
	unsubscribe()
	subscribe(sensor, "contact", contactHandler)
	subscribe(virtualgdsensor, "contact", virtualgdcontactHandler)
	// sync them up if need be, set virtual same as physical
	if (realgdstate != virtualgdstate)
	{
		if (realgdstate == "open")
		{
			log.debug "opening virtual door"
			virtualgd.open()
			mysend("virtualgd.displayName Opened!")
		}
		else {
			log.debug "closing virtual door"
			virtualgd.close()
			mysend("$virtualgd.displayName Closed!")
		}
	}
}
def contactHandler(evt) 
{
def virtualgdstate = virtualgd.currentContact
// how to determine which contact
//log.debug "in contact handler for physical door open/close event. event = $evt"

  if("open" == evt.value)
    {
    // contact was opened, turn on a light maybe?
    log.debug "Contact is in ${evt.value} state"
    // reset virtual door if necessary
    if (virtualgdstate != "open")
      {
        mysend("$virtualgd.displayName Opened Manually, syncing with Virtual Device!")   
        virtualgd.open()
      }
     }  
  if("closed" == evt.value)
   {
   // contact was closed, turn off the light?
    log.debug "Contact is in ${evt.value} state"
    //reset virtual door
     if (virtualgdstate != "closed")
      {
       mysend("$virtualgd.displayName Closed Manually, syncing with Virtual Device!")   
       virtualgd.close()
      }
   }
}
def virtualgdcontactHandler(evt) {
	// how to determine which contact
	def realgdstate = sensor.currentContact
	if("open" == evt.value)
	{
		// contact was opened, turn on a light?
		log.debug "Contact is in ${evt.value} state"
		// check to see if door is not in open state if so open
		if (realgdstate != "open")
		{
			log.debug "opening physical door to correspond with button press"
			opener.on()
			mysend("$virtualgd.displayName Opened, syncing with Physical Door!")
			runIn(checkTimeout, checkIfActuallyOpened)
		}
	}
	if("closed" == evt.value)
	{
		// contact was closed, turn off the light?
		log.debug "Contact is in ${evt.value} state"
		if (realgdstate != "closed")
		{
			log.debug "closing physical door to correspond with button press"
			closer.on()
			mysend("$virtualgd.displayName Closed, syncing with Physical Door!")   
			runIn(checkTimeout, checkIfActuallyClosed)
		}
	}
}
private mysend(msg) {
	if (location.contactBookEnabled) {
		log.debug("sending notifications to: ${recipients?.size()}")
		sendNotificationToContacts(msg, recipients)
	}
	else {
		if (sendPushMessage != "No") {
			log.debug("sending push message")
			sendPush(msg)
		}
		if (phone1) {
			log.debug("sending text message")
			sendSms(phone1, msg)
		}
	}
	log.debug msg
}
def checkIfActuallyClosed()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
	// sync them up if need be, set virtual same as actual
	if (realgdstate == "open" && virtualgdstate == "closed")
	{
		log.debug "setting virtual door as open because physical door didn't close."
		virtualgd.open()
		mysend("Resetting $virtualgd.displayName to Open as physical door didn't close!")
	}
}
def checkIfActuallyOpened()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
// sync them up if need be, set virtual same as actual
	if (realgdstate == "closed" && virtualgdstate == "open")
	{
		log.debug "setting virtual door as closed because physical door didn't open."
		virtualgd.close()
		mysend("Resetting $virtualgd.displayName to Closed as physical door didn't open!")
	}
}