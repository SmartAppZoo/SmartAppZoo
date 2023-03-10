/**
 *  Author: Baur
 */

definition(
    name: "Contact Switch Open Too Long",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Monitor your contact sensors and get a text message if they are open too long",
    category: "Safety & Security",
    parent: "baurandr:Baur Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Select contacts...") {
		input("contact1", "capability.contactSensor", title: "Which contact sensor?", multiple: true)
        input(name: "openOrClosed", type: "enum", title: "Notify when open or closed?", options: ["open","closed"])
    }
	section("For too long...") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
    section("Keep sending reminder texts every ??? minutes (optional)") {
		input "reminderTime", "number", title: "Minutes?", required: false
	}
	section("Text me at (optional, sends a push notification if not specified)...") {
        input("recipients", "contact", title: "Notify", description: "Send notifications to (separate multiple inputs with commas)") {
            input "phone", "phone", title: "Phone number?", required: false
        }
	}
    section("Notify on google home:") {
        input("speaker1", "capability.actuator", title: "Which speaker?", multiple: false)
    }
}

def installed()
{
	clearStatus()
	subscribe(contact1, "contact", contactHandler)
}

def updated()
{
	unsubscribe()
    clearStatus()
	subscribe(contact1, "contact", contactHandler)
}

def contactHandler(evt) {
	log.debug "$evt.device $evt.name: $evt.value"
	def isOpen = evt.value == openOrClosed
    //def deviceName = evt.device
    def isNotScheduled = state.status != "scheduled"
    def openContacts = contact1.findAll{it.currentValue("contact") == openOrClosed}
	def scheduledContacts = state.scheduledContacts
	def bSchedule = false

    if (!openContacts) {
        log.debug "All contacts closed. Cancelling runIn and clearing status."
        clearStatus()
        unschedule(takeAction)
    } else if (isOpen){
    	openContacts.each {
        	if(!scheduledContacts?.contains(it.label)){
            	scheduledContacts << it.label
				bSchedule = true
				log.debug "New scheduled contacts:$it, All scheduled contacts:$scheduledContacts"
			}
        }
        state.scheduledContacts = scheduledContacts
        if (bSchedule) {
            state.status = "scheduled"
            runIn(maxOpenTime * 60, takeAction, [overwrite: false])
        }
    }
}

def takeAction(){
	if (state.status == "scheduled")
	{
        def openContacts = contact1.findAll{it.currentValue("contact") == openOrClosed}
        def openTooLong = []
        
        openContacts.each { 
        	def openState = it.currentState("contact")
            def elapsed = now() - openState.rawDateCreated.time
            def threshold = 1000 * 60 * maxOpenTime - 1000
            log.debug "Contact:$it, OpenTime:$openState.rawDateCreated.time, Elapsed:$elapsed, Threshold:$threshold"
            if (elapsed >= threshold) {
            	//log.debug "$it Open Too Long"
                openTooLong << it
            }
        }
        //log.debug "Contacts that have been open too long:$openTooLong"
        if (openTooLong){
            sendTextMessage(openTooLong, maxOpenTime)
            if (reminderTime) {
                runIn(reminderTime * 60, takeAction, [overwrite: false])
            }
        }
	} else {
		log.trace "Status is no longer scheduled. Not sending text."
	}
}

def sendTextMessage(openContacts, openMinutes) {

	log.debug "$openContacts was open too long, texting phone"
    
    def msg = "Your ${openContacts.label ?: openContacts.name} has been ${openOrClosed} for more than ${openMinutes} minutes!"
    
    if (maxOpenTime <= 0){
    	if(openOrClosed == "open"){
    		msg = "Your ${openContacts.label ?: openContacts.name} has been opened!"
        }
        else{
	    	msg = "Your ${openContacts.label ?: openContacts.name} has been closed!"
        }
    }
    
	if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
            if ( phone.indexOf(",") > 1){
              def phones = phone.split(",")
              for ( def i = 0; i < phones.size(); i++) {
                log.debug "sending SMS ${i+1} to ${phones[i]}"
                  sendSms(phones[i], msg)
                }
                speaker1.customBroadcast(msg)
              } else {
                log.debug "sending SMS to ${phone}"
                sendSms(phone, msg)
                speaker1.customBroadcast(msg)
              }
        } else {
            sendPush msg
            speaker1.customBroadcast(msg)
        }
    }
}

def clearStatus() {
	state.status = null
    state.scheduledContacts = []
}