/**
 *  Automated Lock Notifier
 *
 *  Copyright 2019 kenobob
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
    name: "Automated Lock Notifier",
    namespace: "kenobob",
    author: "kenobob",
    description: "Makes sure the lock actually locks when you want it to.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section() {
        input (name: "secondsDelay", type: "number", title: "Number of Seconds you want to Delay to let the lock... lock.", required: true)
    }
    section() {
        input "doorLock", "capability.lock", title: "The Lock to Control", multiple: false, required: true
        input "modes", "mode", title: "Select modes where the door should be locked", multiple: true, required: true
    }
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneNumber", "phone", title: "Send a Text Message?", required: false
        }
        input("notificationText", type: "text", title: "What would you like your notification to say?", required: true)
    }
}

def installed()
{
    logtrace("Executing 'installed'")
    logdebug("Installed with settings: ${settings}")
    initialize()
    logtrace("End Executing 'installed'")
}

def updated()
{
    logtrace("Executing 'updated'")
    logdebug("Updated with settings: ${settings}")
    initialize()
    logtrace("End Executing 'updated'")
}

def initialize() {
    logtrace("Executing 'initialize'")
    
    // remove all scheduled executions for this SmartApp install
    unschedule()
    // unsubscribe all listeners.
    unsubscribe()
    
    //Reset variables
    state.schedulerActive = false
    
    //Subscribe to Sensor Changes
    logdebug("Subscribing to Mode Events")
    subscribe(location, "mode", modeChangeEventHandler)
	
    logtrace("End Executing 'initialize'")
}
def modeChangeEventHandler(evt)
{
    logtrace("Executing 'modeChangeEventHandler'")
    logtrace(evt);

    //Check to See if the Mode Changed
    if(evt.isStateChange()){

        def currentMode = location.currentMode;
        //Mode changed, check our list
        if(modes.contains(currentMode)){
            //Current Mode should lock the door. Start Scheduler
            runIn(secondsDelay, checkIfDoorLocked, [overwrite: true])

            state.schedulerActive = true  
            logdebug("checkIfDoorLocked Scheduled")
        } else {
            //Not in the right mode, kill any schedules                
            state.schedulerActive = false
            unschedulecheckIfDoorLocked()
        }
        
    }
    
    logtrace("End Executing 'lockChangeEventHandler'")
}

def checkIfDoorLocked(){
    logtrace("Executing 'checkIfDoorLocked'")
    
    if(!isDoorLocked()){
        //The door didn't lock. Notify the user
        //Lets Lock the Door!!
        log.info("Door Didn't Lock, warn users")
        def eventData =  [
                sendPushMessage: null,
                phoneNumber: null,
                notificationText: null
            ]
            
            eventData.phoneNumber = phoneNumber
            eventData.sendPushMessage = sendPushMessage
            eventData.notificationText = notificationText

            notifyUser(eventData)

    } else {
        log.info("Door Locked Successfully")
    }

    
    state.schedulerActive = false
    logtrace("End Executing 'checkIfDoorLocked'")
}

private def unschedulecheckIfDoorLocked(){
    logtrace("Executing 'unscheduleCheck'")
    //Cancel Scheduler
    unschedule(checkIfDoorLocked)
    
    //Check to see if still can Schedule more events
    if(!canSchedule()){
        // remove all scheduled executions for this SmartApp
        unschedule()
    }
    
    //Reset Variables
    state.schedulerActive = null
    
    logtrace("End Executing 'unscheduleCheck'")
}

def notifyUser(data){
    logtrace("Executing 'notifyUser'")
    logdebug("Notificaiton Data: ${data}")
    
    if(data.sendPushMessage == "Yes" || data.phoneNumber){
        logdebug("Notifications Turned on")
        def options = null
        
        if(data.sendPushMessage == "Yes" && data.phoneNumber) {
            options = [method: "both", phone: data.phoneNumber]
        } else if(data.sendPushMessage == "Yes"){
            options = [method: "push"]
        } else {
            options = [method: "phone", phone: data.phoneNumber]
        }
        logdebug("Options for Notification: ${options}")
        
        sendNotification(data.notificationText, options)
    } else {
        log.error("No notification settings selected")
    }
    
    logtrace("End Executing 'notifyUser'")
}

private def isDoorLocked(){
    log.info("Door Lock is Currently ${doorLock.latestValue("lock")}")
    if(doorLock.latestValue("lock")=="locked"){
        return true
    } else {
        return false
    }
}

private def logtrace(message){
    if(false){
        log.trace(message)
    }
}

private def logdebug(message){
    if(false){
        log.debug(message)
    }
}