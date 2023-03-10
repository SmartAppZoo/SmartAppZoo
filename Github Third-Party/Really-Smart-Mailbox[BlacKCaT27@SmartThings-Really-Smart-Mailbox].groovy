/**
 *  Mail Arrived
 *
 *  Author: Josh Kitchens
 */
preferences {
	section("When mail arrives...") {
		input "accelerationSensor", "capability.accelerationSensor", title: "Where?"
	}
	section("and when this device gets home...") {
		input "presenceSensor", "capability.presenceSensor", title: "Which device?"
	}
    section("give me this much time to get the mail...") {
    	input "blockTimeout", "number", title: "Minutes"
    }
    section("If I don't gather the mail, remind me every..."){
    	input "reminderInterval", "number", title: "Minutes", required:false
    }
}

def installed() {
	state.queueNotification = false
    state.timeOfLastNotification = 0
    state.mailboxFull = false
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
    subscribe(presenceSensor, "presence.present", presencePresentHandler)
}

def updated() {
	state.queueNotification = false
    state.timeOfLastNotification = 0
	state.mailboxFull = false
	unsubscribe()
	subscribe(accelerationSensor, "acceleration.active", accelerationActiveHandler)
    subscribe(presenceSensor, "presence.present", presencePresentHandler)
}


//Called whenever the open/door sensor in the mailbox detects activity (i.e. door open)
//TODO: Determine if there's a way to detect false positives such as mailbox getting pushed around
//TODO: Add reminders
def accelerationActiveHandler(evt) {

	if(state.mailboxFull == true){
    	unschedule()
        state.mailboxFull = false
        return
    }

	//We'll use timeoutCheck to check when the last notification occurred (if we have record of one)
    def timeoutCheck = 0
    //convert blockTimeout minutes to milliseconds
    def blockTimeoutMS = blockTimeout*60000
    
    //If we have record of a previous notification, set timeoutCheck to the number of milliseconds
    //ago it occurred. 
    if (state.timeOfLastNotification != 0 && state.timeOfLastNotification != null){	
        log.debug "Found an old notification record..."
        log.debug "Time of last notification: $state.timeOfLastNotification"
        timeoutCheck = now() - state.timeOfLastNotification
        log.debug "New value of timeoutCheck: $timeoutCheck"
    }
    else{
    	//If timeOfLastNotification IS null or 0, this is the first time running the app, so set
        //timeoutCheck greater than the timeout input to ensure the notification is handled.
    	log.debug "No old notifications found."
        timeoutCheck = blockTimeoutMS+1
    }
    
    //If the time of our last notification is less than the minimum time to wait for a new notification,
    //don't send a notification. (This is used to let you check the mail without having a duplicate
    //notification go off)
    log.debug "timeoutCheck value: $timeoutCheck"
    log.debug "blockTimeout in ms: $blockTimeoutMS"
	if (timeoutCheck < blockTimeoutMS) {
		log.debug "Already notified user within the last $blockTimeout minute(s)"    
	} 
    else {
    	//At this point, we know we're sending a notification, but the question becomes when?
        def isPresent = presenceSensor.currentState("presence")
        
        //if the user is currently home, send the notification immediately
    	if(isPresent.stringValue == "present"){
			notifyUser()
            log.debug "User is home!"
        }
        
        //if they're not home, set a flag to queue the event so the presencePresentHandler
        //can handle the notification when the user arrives
        else{
        	state.queueNotification = true
            log.debug "User is not home. Queuing notification..."
        }
	}
}

def notifyUser(){
	//send the notification and log the time the notification occurred
    log.debug "Sending push notification..."
	sendPush("You\'ve got mail!")
    state.timeOfLastNotification = now()
    state.queueNotification = false
    scheduleNextReminder()
    state.mailboxFull = true

}

def scheduleNextReminder(){
	if(reminderInterval != null) {
    	def reminderTimeInSeconds = reminderInterval*60
        log.debug "Scheduling reminder for $reminderTimeInSeconds seconds from now..."
        runIn(reminderTimeInSeconds, remindUser)
    }
}

def remindUser(){
	log
	sendPush("Reminder: You haven't retreived your mail yet.")
    scheduleNextReminder()
}


//Called when the user arrives at the house
def presencePresentHandler(evt){
	
    //if a notification has been queued by the motionActiveHandler, send it now
    //then clear the flag
	if(state.queueNotification == true){
    	log.debug "Queued event found. Notifying user..."
        notifyUser()
    }
    else{
    	log.debug "No queued events found."
    }
}
