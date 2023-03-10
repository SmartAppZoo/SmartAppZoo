/**
 *  Video Recording Automation
 *
 *  Copyright 2020 Gabriel
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
 *  Contributors
 *  ----------------
 *  Strongly based off of code from Mavrrick (https://github.com/Mavrrick)
 */
definition (
	name: "Video Recording Automation",
	namespace: "gabrielstelmach",
	author: "Gabriel Stelmach",
	description: "Trigger video recording, including push message, based on device action and timing schedule.",
	category: "My Apps",
	iconUrl: "https://farm9.staticflickr.com/8632/16461422990_e5121d68ee_o.jpg",
	iconX2Url: "https://farm9.staticflickr.com/8632/16461422990_e5121d68ee_o.jpg",
	iconX3Url: "https://farm9.staticflickr.com/8632/16461422990_e5121d68ee_o.jpg"
)

import groovy.time.TimeCategory 

preferences 
{
	section("About this") 
    {
        paragraph "Select which devices you would like to have to trigger a video capture. Next, you can define which cameras, and optionally, the interval of day time when capturing is active. Also, it is possible to include a message to be pushed.";
    }
	section("When any of the following devices...")
    {
		input "selectedMotionSensor", "capability.motionSensor", title: "Motion sensor?", required: false;
		input "selectedContactSensor", "capability.contactSensor", title: "Contact sensor?", required: false;
		input "selectedButton", "capability.button", title: "What button?", required: false;
		input "selectedAccelerationSensor", "capability.accelerationSensor", title: "Acceleration sensor?", required: false;
		input "selectedSwitch", "capability.switch", title: "Switch?", required: false;
		input "selectedPresenceSensor", "capability.presenceSensor", title: "Presence sensor?", required: false;
		input "selectedWaterSensor", "capability.waterSensor", title: "Moisture sensor?", required: false;
	}
	section("Recording setup")
    {
		input "selectedVideoCapture", "capability.videoCapture", title: "Which camera?", description: "Select the cameras to capture when triggered", required: true, multiple: true;
		input "clipLength", type: "number", title: "Clip length", description: "Length of recording in seconds", required: true, range: "5..300";
    	input "notifyMessage", type: "text", title: "Notification message", description: "Message will be pushed including event details", required: false;
		input "betweenTimeFrom", "time", title: "Time from", description: "Day time starting at", required: false;
		input "betweenTimeTo", "time", title: "Time to", description: "Day time finishing at", required: false;
	}
}

def installed() 
{
	log.debug "Installed with settings: ${settings}";
	initialize();
}

def updated() 
{
	log.debug "Updated with settings: ${settings}";
	unsubscribe();
	initialize();
}

def initialize() 
{
	if (settings.selectedContactSensor) 
    {
		subscribe(settings.selectedContactSensor, "contact.open", startVideoCapture);
	}
	if (settings.selectedAccelerationSensor) 
    {
		subscribe(settings.selectedAccelerationSensor, "acceleration.active", startVideoCapture);
	}
	if (settings.selectedMotionSensor) 
    {
		subscribe(settings.selectedMotionSensor, "motion.active", startVideoCapture);
	}
	if (settings.selectedSwitch) 
    {
		subscribe(settings.selectedSwitch, "switch.on", startVideoCapture);
	}
	if (settings.selectedPresenceSensor) 
    {
		subscribe(settings.selectedPresenceSensor, "presence", startVideoCapture);
	}
    if (settings.selectedWaterSensor) 
    {
    	subscribe(settings.selectedWaterSensor, "water.wet", startVideoCapture);
	}
    if (settings.selectedButton) 
	{
		subscribe(settings.selectedButton, "button.pushed", startVideoCapture);
	}
}

def startVideoCapture(evt) 
{
	log.debug "Begin...";
    /*
    log.debug "Event descriptionText: ${evt.descriptionText}";
    log.debug "Event description: ${evt.description}";
    log.debug "Event displayName: ${evt.displayName}";
    log.debug "Event name: ${evt.name}"
    log.debug "Event device: ${evt.device}";
    log.debug "Event source: ${evt.source}";
    log.debug "Event value: ${evt.value}"
    log.debug "Event date: ${evt.date}"
    log.debug "Event data: ${evt.data}"
    log.debug "Event state changed: ${evt.isStateChange()}"
    */
    def isTimeBetween = timeOfDayIsBetween(settings.betweenTimeFrom, settings.betweenTimeTo, new Date(), location.timeZone);
    if (isTimeBetween)
    {
    	def now = new Date((now()) + location.timeZone.rawOffset).format("dd/MM/yy' at 'HH:mm:ss");
        def notification = "Actioned on " + now + " by ${evt.device} the ${evt.value} ${evt.name}, refreshing cameras with ${settings.clipLength} seconds capture";
		log.debug notification;
        
        Date start = new Date();
        Date end = new Date();
        use (TimeCategory) 
        {
            end = start + settings.clipLength.seconds;
        }
		
        log.debug "Capturing..."
        settings.selectedVideoCapture.capture(start, start, end);
		
        if (settings.notifyMessage)
        {
            sendNotificationEvent(settings.notifyMessage + " " + notification);
            sendPush(settings.notifyMessage + " " + notification);
        }
    }
    log.debug "...end";
}