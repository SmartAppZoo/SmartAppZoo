
 /**
 *  Laundry Monitor
 *
 *  Copyright 2016 Brandon Miller
 *
 *  Version 1.0.3   27 Dec 2016
 *
 *	Version History
 *
 *	1.0.3   27 Dec 2016		Sonos speaker fixes and in-App button by Lou Jackson
 *	1.0.2   22 Dec 2016		Added Sonos speaker support by Lou Jackson
 *	1.0.1   07 Feb 2016		Modified to support laundry start message and version by Lou Jackson
 *	1.0.0	27 Jan 2016		Creation by Brandon Miller
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
 
import groovy.time.* 
 
definition(
    name: "Laundry Monitor",
    namespace: "bmmiller",
    author: "Brandon Miller",
    description: "This application is a modification of the SmartThings Laundry Monitor SmartApp.  Instead of using a vibration sensor, this utilizes Power (Wattage) draw from an Aeon Smart Energy Meter.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances8-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances8-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances8-icn@2x.png"
    )


preferences {
	section("Tell me when this washer/dryer has stopped..."){input "sensor1", "capability.powerMeter"}
    
    section("System Variables")
    {
    	input "minimumWattage", "decimal", title: "Minimum running wattage", required: false, defaultValue: 50
        input "minimumOffTime", "decimal", title: "Off if below min wattage for (secs)", required: false, defaultValue: 60
	}
    
    section("Notifications") 
    {
		input("recipients", "contact", title: "Send notifications to") 
        {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
	}

	section("Notification Message")
    {
        input "StartMsg", "text", title: "Start Notification message",  description: "Laundry started", required: true
        input "FinishMsg","text", title: "Finish Notification message", description: "Laundry is done!", required: true
	}
	
	section ("Additionally", hidden: hideOptionsSection(), hideable: true) 
    {
        input "sonos", "capability.musicPlayer", title: "On this Speaker player", required: false
        input "switches", "capability.switch", title: "Turn on these switches?", required:false, multiple:true
	}
    
    section ("Version 1.0.3") { }
}

def installed() 
{
	log.trace "Installed with settings: ${settings}"
	initialize()
}

def updated() 
{
	log.trace "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() 
{
	subscribe(app, appTouchHandler)   //In-App button to play Start Message
	subscribe(sensor1, "power", powerInputHandler)
}

def appTouchHandler(evt) 
{
        if (StartMsg) 
        {
        	log.trace "09 - ${StartMsg}"
        	if (sonos) sonos.playTextAndResume(StartMsg, 100)
        }
}

def powerInputHandler(evt) 
{
	def latestPower = sensor1.currentValue("power")
    log.trace "Power: ${latestPower}W ; ${minimumWattage}W"
    
    //if (!atomicState.isRunning && latestPower > minimumWattage) 
    if (latestPower > minimumWattage) 
    {
    	atomicState.isRunning = true
		atomicState.startedAt = now()
        atomicState.stoppedAt = null
        atomicState.midCycleCheck = null
        
        if (StartMsg) 
        {
        	log.trace "0A - ${StartMsg}"
        	sendMessage(StartMsg) 
        	if (sonos) sonos.playTextAndResume(StartMsg, 100)
        }
    } 
    //else if (atomicState.isRunning && latestPower <= minimumWattage)
    if (latestPower <= minimumWattage)
    {
         log.trace "0B - atomicState.midCycleTime=${atomicState.midCycleTime} ; minimumOffTime=${minimumOffTime}"

    	if (atomicState.midCycleCheck == null)
        {
        	atomicState.midCycleCheck = true
            atomicState.midCycleTime = now()
            log.trace "0C - atomicState.midCycleTime=${atomicState.midCycleTime} ; minimumOffTime=${minimumOffTime}"

        }
        //else if (atomicState.midCycleCheck == true)
        //{
        	log.trace "0D - atomicState.midCycleTime=${atomicState.midCycleTime} ; minimumOffTime=${minimumOffTime}"
            
        	// Time between first check and now  
            //if ((now() - atomicState.midCycleTime)/1000 > minimumOffTime)
            //{
            	atomicState.isRunning = false
                atomicState.stoppedAt = now()  
                
                log.debug "startedAt: ${atomicState.startedAt}, stoppedAt: ${atomicState.stoppedAt}"                    
                
                if (FinishMsg) 
                {
                	log.trace "0C - ${FinishMsg}"
					sendMessage(FinishMsg) 
                	if (sonos) sonos.playTextAndResume(FinishMsg, 100)
                }
                
                if (switches) switches*.on()
            //}
        //}             	
    }
}

private hideOptionsSection() 
{
  (sonos || switches) ? false : true
}

private sendMessage(msg) {
    log.info "(01) sending message ${msg}"

	if (location.contactBookEnabled) 
    {
    	log.trace "(02) send to contact ${recipients}"
        sendNotificationToContacts(msg, recipients)  //Sends the specified message to the specified contacts and the Notifications feed.
    } 
    else 
    	sendNotificationEvent(msg)
        
    if (phone)
    {
        log.trace "(03) send to contact ${phone}"
        sendSms(phone, msg) //Sends the message as an SMS message to the specified phone number and displays it in Hello, Home. The message can be no longer than 140 characters.
    }   
}
