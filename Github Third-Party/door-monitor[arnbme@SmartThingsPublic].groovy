/**
 *  Door Monitor
 *	Issue Warning when a contact sensor that is not monitored by Smarthome remains open when alarm is set to armed
 *	Multiple sensors are supported
 *
 *  Copyright 2017 Arn Burkhoff
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
 *	Aug 11, 2017 v2.1.2  change icons to hourglass
 *	Aug 10, 2017 v2.1.1  fix defaultvalues
 *	Aug 09, 2017 v2.1.0  add support and code for multiple contact monitoring
 *	Aug 08, 2017 v2.0.0a add routine name to unschedule or it kills everything
 *	Aug 08, 2017 v2.0.0  Add subscription to location alarm state and logic to handle it
 *					define and use standard killit and new_monitor routines
 *					remove uneeded timimg stuff due to catching alarm status 
 *					remove endless cycles when door was open and system unarmed
 *					unable to push out an error message to user at this time if no push or no sms
 *	Aug 07, 2017 v1.0.2  Due to reports of RunIn being unreliable, change to RunOnce
 *	Aug 05, 2017 v1.0.1b change seconds from 60 to thedelay*60-5 on first short delay eliminating a 5 second runIn
 *	Aug 03, 2017 v1.0.1a Remove extraneous unschedule() from contactOpenHandler.
 *	Aug 02, 2017 v1.0.1  Add logic in checkStatus ignoring instusions (handled by dooropens) as much as possible.
 *	Jul 31, 2017 v1.0.0  Coded and Installed
 *
 */
definition(
    name: "Door Monitor",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Warn when door than is not monitored by Smarthome remains open when alarm is armed",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")


preferences 
	{
	section("Monitor Contact Sensors not monitored in SmartHome when alarm is set to armed")
		{
		input "thecontact", "capability.contactSensor", required: true, multiple:true,
			title: "One or more contact sensors"
		input "maxcycles", "number", required: true, range: "1..99", defaultValue: 2, 
			title: "Maximum number of warning messages"
		input "thedelay", "number", required: true, range: "1..15", defaultValue: 1,
			title: "Number of minutes between messages from 1 to 15"  	
		input "thesendPush", "bool", required: false, defaultValue:false,
			title: "Send Push Notification?"
		input "phone", "phone", required: false, 
			title: "Send a text message to this number, for multiple separate with comma (optional)"
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
	subscribe(thecontact, "contact.closed", contactClosedHandler)
	subscribe(location, 'alarmSystemStatus', alarmStatusHandler)
	}

	
def new_monitor()
	{
	log.debug "new_monitor called: cycles: $maxcycles"
	state.cycles = maxcycles
	def now = new Date()
	def runTime = new Date(now.getTime() + (thedelay * 60000))
	runOnce (runTime, checkStatus)
	}

def killit()
	{
	log.debug "killit called"
	state.remove('cycles')
	unschedule(checkStatus)	//kill any pending cycles
	}

def countopenContacts() {
	log.debug "countopenContacts entered"
	def curr_contacts = thecontact.currentContact	//status of each contact in a list(array)
//	count open contacts	
	def open_contacts = curr_contacts.findAll 
		{
		contactVal -> contactVal == "open" ? true : false
		}
	log.debug "countopenContacts exit with count: ${open_contacts.size()}"
	return (open_contacts.size())
	}

def contactClosedHandler(evt) 
	{
	log.debug "contactClosedHandler called: $evt.value"
	if (countopenContacts()==0)
		killit()
	}

def alarmStatusHandler(evt)
	{
	log.debug("Door Monitor caught alarm status change: ${evt.value}")
	if (evt.value=="off")
		{
		killit()
		}
	else
		{
		if (countopenContacts()==0)
			{
			killit()
			}
		else
			{
			new_monitor()
			}
		}
	}

def checkStatus()
	{
	// get the current state for alarm system
	def alarmstate = location.currentState("alarmSystemStatus")
	def alarmvalue = alarmstate.value
	def door_count=countopenContacts()		//get open contact count
	log.debug "In checkStatus: Alarm: $alarmvalue Doors Open: ${door_count} MessageCycles remaining: $state.cycles"


//	Check if armed and one or more contacts are open
	if ((alarmvalue == "stay" || alarmvalue == "away") && door_count>0)
		{
		state.cycles = state.cycles - 1	//decrement cycle count
//		state.cycles--  note to self this does not work

//		calc standard next runOnce time
		def now = new Date()
		def runTime = new Date(now.getTime() + (thedelay * 60000))

//		get names of open contacts for message
		def curr_contacts= thecontact.currentContact	//status of each switch in a list(array)
		def name_contacts= thecontact.displayName		//name of each switch in a list(array)
		def door_names="";
		def door_sep="";
		def ikey=0
		curr_contacts.each
			{ value -> 
			if (value=="open")
				{
				door_names+=door_sep+name_contacts[ikey]
				door_sep=", "
				}
			ikey++;
			}
		if (door_names>"")
			{
			if (door_count > 1)
				door_names+=" are open"
			else	
				door_names+=" is open"
			}	
		def message = "System is armed, but doors ${door_names}"

//		send notification and/or SMS message	
		if (thesendPush)
			{
			sendPush message
			}
		if (phone)
			{
			sendSms(phone, message)
			}
		if (thedelay>0 && state.cycles>0)
			{
			log.debug ("issued next checkStatus cycle $thedelay ${60*thedelay} seconds")
			runOnce(runTime,checkStatus)
			}
		}
	else
		{
		killit()
		}

	}