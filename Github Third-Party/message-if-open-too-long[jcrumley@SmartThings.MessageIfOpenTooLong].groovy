/**
 *  Message if open too long
 *
 *  If it is between starttime and endtime and a contact is open, then start a timer and wait openalarmthreshold minutes.  If
 *  the contact is still open, then send an alert.  When the contact is closed, an alert will be sent.
 *
 *  If outside the starttime and endtime, then send an alert immediately.
 *
 *  If the contact is open at the endtime, an alert will be sent.
 *
 *  If "sendImmediate" is true, an alert will be sent when first opened and then openalarmthreshold minutes later if still open.
 *
 *  If "sendImmediate" is false, an alert will be sent after "openalarmthreshold" minutes.
 *
 *  If "repeat" is true, an alerts will keep being sent every "repeatmessagedelay" minutes until the contact is closed.
 *
 *
 *  So if you set starttime = 6:00 AM, endtime = 9:00 pm, openalarmthreshold = 10 minutes, checktime = 9:00 pm, then you will get 
 *  an alert if the door is open for more than 10 minutes from 6:00 am until 9:00 pm.  In addition, you will get notified at 9:00 pm
 *  if the door is open (in case you ignored the earlier notice).
 * 
 *
 *  Copyright 2019 John Crumley
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
    name: "Message if open too long",
    namespace: "jcrumley",
    author: "John Crumley",
    description: "Will send a notification if an open/close sensor is in the open state for longer than a certain time frame.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: false)


preferences {
	section("Title") {
		// TODO: put inputs here
	}
    section("Turn on when activated:") {
        input "tomonitor", "capability.contactSensor", required: false, title: "Contact Sensor to monitor"
		input "tomonitorWet", "capability.waterSensor", required: false, multiple: false, title:"Moisture Sensor to monitor"
    	input "startTime", "time", title: "Time to start monitoring", required: true
    	input "endTime", "time", title: "Time to stop monitoring", required: true
        input "sendImmediate", "bool", title: "Send immediate notification", required: true
        input "repeat", "bool", title: "Repeat notifications", required: true
    }
    section("Send a text message to...") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone number?", required: true
            input "phone2", "phone", title: "Phone number 2?", required: false
        }
	}
    section("Open alarm threshold (defaults to 5 min)") {
		input "openalarmthreshold", "number", title: "Send after open x minutes", required: false
        input "repeatmessagedelay", "number", title: "Repeat every X minutes", required: false
	}
    section("Identifier") {
    	input "identifier", "text", title: "Text in message", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() 
{
	if (tomonitor != null)
    {
    	log.debug "initialize:  contact"
		subscribe(tomonitor, "contact", contactHandler)
    }
    else
    {
        log.debug "initialize: no contact"
    }
    if (tomonitorWet != null)
    {
    	log.debug "initialize:  water"
		subscribe(tomonitorWet, "water", contactHandler)
    }
    else
    {
    	log.debug "initialize: no water"
    }
    schedule(startTime, checkOpenTimeBounded)
    
    state.counter = 0
    state.initial = true
    
    //check at a specific time to see if it is open
    if (endTime) //was checkTime
    {
    	schedule(endTime, checkOpen) //was checkTime
    }
}

def contactHandler(evt) 
{
	log.debug "contactHandler: Called"
    log.debug "contactHandler: Contact is in ${evt.value} state"
  	if(("open" == evt.value) || ("wet" == evt.value))
  	{
    	if (inTimeWindow())
        {
        	if (sendImmediate == true)
            {
            	checkOpen()
                log.debug "contactHandler: immediate checkOpen"
            }
            
    		// check in a delayed time
            if (sendImmediate != true || repeat != true)
            {
				final openDoorAwayInterval = openalarmthreshold ? openalarmthreshold * 60 : 5
    			runIn(openDoorAwayInterval, checkOpenTimeBounded)
        		log.debug "contactHandler: checkOpen scheduled in " + openDoorAwayInterval.toString()
            }
        }
        else
        {
        	checkOpen()  //check immediately
            log.debug "contactHandler: immediate checkOpen2"
        }
  	}
    
  	if("closed" == evt.value || "dry" == evt.value)   {
        if (state.triggered == true)
        {
        	log.debug "contactHandler has been triggered"
        	state.triggered = false
            state.counter = 0
            def message = identifier + " Closed/Dry"
    		if (location.contactBookEnabled) 
        	{
        		sendNotificationToContacts(message, recipients)
         		log.debug "contactHandler:  notificationContacts"
    		}	   
        	else 
        	{
       			sendSms(phone1, message)
            	if (phone2)
            	{
            		sendSms(phone2, message)
            	}
       			log.debug "contactHandler:  sendSMS"
    		}
        }
    }	
}

def checkOpenTimeBounded() 
{
	log.debug "checkOpenTimeBounded: Called"
    
	if (inTimeWindow())
    {
    	checkOpen()
    }
}

//see if we are within the window to monitor
def inTimeWindow()
{
 	def now = new Date()
    def startTimeUse = timeToday(startTime)
    def endTimeUse = timeToday(endTime)
    //log.debug now.toString() + " " + startTime.toString() + " " + endTime.toString()
    
    if (now.getTime() < startTimeUse.getTime())
    {
    	log.debug "checkOpen:  not yet time to start"
        return false
    }
    else if (now.getTime() > endTimeUse.getTime())
    {
    	log.debug "checkOpen:  afterTimeToEnd"
        return false
    }
    
    return true
}

def checkOn()
{
	if (tomonitor?.currentContact == "open")
    {
        log.debug "checkOn:  contact on"
    	return true;
    }
    if (tomonitorWet?.currentWater == "wet")
    {
    	log.debug "checkOn:  moisture on"
    	return true;
    }
    
    log.debug "checkOn:  off"
    return false;
}

//is the door now open?
def checkOpen()
{
	if (checkOn()) 
    {
        def message = identifier + " Open/Wet (" + state.counter.toString() + ")"
    	if (location.contactBookEnabled) 
        {
        	sendNotificationToContacts(message, recipients)
         	log.debug "checkOpen:  notificationContacts"
    	}	   
        else 
        {
       		sendSms(phone1, message)
            if (phone2)
            {
            	sendSms(phone2, message)
            }
       		log.debug "checkOpen:  sendSMS (" + state.counter.toString() + ")"
    	}
        
        //save that we have sent an alert
        state.triggered = true
        state.counter = state.counter + 1
        
        if (repeat == true)
        {
            final repeatTime = repeatmessagedelay ? (repeatmessagedelay * 60) : (openalarmthreshold ? openalarmthreshold * 60 : 5)
        	log.debug "checkOpen:  repeat in " + repeatTime.toString()
            
            //make sure we don't have another one running due to the normal delay
            unschedule(checkOpenTimeBounded)
            unschedule(checkOpen)
            
            //notifiy again
            runIn(repeatTime, checkOpen)
        }
        def summarySound = textToSpeech(message, true)  
     	//sonos.playTrack(summarySound.uri) 
    }
    else 
    {
    	log.debug "checkOpen:  has been closed/dry.  don't send"
    }
}


