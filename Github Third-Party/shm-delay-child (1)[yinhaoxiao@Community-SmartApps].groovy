/**
 *  Smart Home Delay and Open Contact Monitor Child
 *	Functions: 
 *		Simulate contact entry delay missing from SmartHome.					
 *		Since contact is no longer monitored by SmartHome, monitor it for "0pen" status when system is armed
 *	Warning: SmartHome is fully armed during operation of this SmartApp. Tripping any non simulated sensor 
 *			immediately triggers an intrusion alert
 *
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
 *	Feb 03, 2018 v1.7.5  When Entry Delay time is 0, alarm did not trigger. Mostly an Exit Delay testing issue.
 *							in doorOpensHandler add a test for theentrydelay < 1
 *	Jan 08, 2018 v1.7.4  In doorOpensHandler reduce overhead when monitored contact sensor opens by exiting when alarm=off and
 *							eliminate read of old events that is no longer needed or used
 *	Jan 04, 2018 v1.7.3  After having to issue 1.7.2 and 1.7.1, added optional user supplied override name field
 *							Hopefully ends this mishagas
 *							Simplify and slim down the error logic code by using trim() and non null start field
 *							Change use of siren "on" command to "siren" to be more correct
 *	Jan 03, 2018 v1.7.2  Allow RG Linear devices as real. Check for RG Linear in typeName 
 *	Jan 02, 2018 v1.7.1  Allow ADT NOVA devices as real. Check for Nortek in typeName 
 *	Jan 02, 2018 v1.7.0  Allow motion sensors to have a short delay.
 *							Sometimes a motion sensor sees a door and triggers alarm
 *							before the door contact sensor registers as open. See routine waitfordooropen
 *							added field themotiondelay in profile. 
 *	Dec 31, 2017 v1.6.0  Allow for multiple Motion Sensors in profile, use global to set multiple motion sensors
 *							keeping current user profiles intact for existing users
 *                       Per user request allow up to 90 seconds on exit and entry delays	
 *	Dec 20, 2017 v1.5.2  Motion Sensor in Away mode during exit delay may trigger extraneous alarm
 * 							when door not currently or not recently opened. 
 *	Dec 02, 2017 v1.5.1  Motion Sensor in Away mode during entry delay time period, triggers extraneous alarm. 
 *							When a followed motion sensor senses motion, and the contact sensor is closed after being open,
 *							and prior to disarming the alarm, a false alarm was issued
 *						 In other words --- During away mode: you open a door starting entry delay time, walk in, then close the door,
 *							trigger a followed motion sensor prior to disarming, created a false intrusion alert.
 *	Nov 14, 2017 v1.5.0  error sendnotificationtocontacts always logging
 *	Nov 12, 2017 V1.5.0  Add support for Smartthings Contacts
 *	Oct 03, 2017 V1.4.2  in routine childalarmStatusHandler only issue setArmedNight for each Xfinity 3400 keypad,
 *					Iris does not have a night icon
 *	Sep 27, 2017 v1.4.1  soundalarm when open door at arming, then optional motion sensor trips, 
 *                          and door open for greater than entry delay seconds 
 *	Sep 26, 2017 v1.4.0  Add optional motion sensor to silence when monitored contact sensor opens in away mode 
 *	Sep 22, 2017 v1.3.0  Add logic for True Entry Delay 
 *	Sep 22, 2017 v1.2.1c Modify allowing Connect and Konnect as "real" devices 
 *	Sep 22, 2017 v1.2.1b Add Z-Wave in type as valid real device. User could not select as real 
 *	Sep 22, 2017 v1.2.1a Konnect not being allowed as real device, add parens around Konnect|honeywell 
 * 	Sep 17, 2017 v1.2.1  In true night and stay modes alarm not sounding.
 *					soundalarm was not firing, perhaps encountered a RunOnce timing issue with server
 *						created then passed a map to soundalarm rather than issuing a RunOnce
 *						should greatly improve reliability of the instant trigger
 *					manually setting virtual in phone app did not trigger alarm (WTF)
 *						had to resave the SmartHome Security parameters to get it to fire
 *	
 * 	Sep 02, 2017 v1.2.0  Repackage ModeFix into child module, skip running fix when bad 'night mode' is found
 * 	Aug 31, 2017 v1.1.0e Add Honeywell to valid Simulated contacts
 * 	Aug 31, 2017 v1.1.0f Simulate beep with on/off if no beep command, fails with GoControl Siren
 * 	Aug 30, 2017 v1.1.0e keypad acts up when commands come in to fast. always use a one second delay on setarmed night
 * 	Aug 30, 2017 v1.1.0e issue setArmedNight only when using upgraded Keypad
 * 	Aug 30, 2017 v1.1.0d verify keypad can issue setEntryDelay, or issue error msg
 * 	Aug 30, 2017 v1.1.0c verify siren has a beep command, or issue error msg
 * 	Aug 30, 2017 v1.1.0b change passing of error data back to pages to a state field
 * 	Aug 29, 2017 v1.1.0a add State of 'batteryStatus' when testing for real or simulated device
 * 	Aug 28, 2017 v1.1.0  when globaFixMode is on, eliminate 2 second delay issuing keypad setArmedNight
 * 	Aug 25, 2017 v1.1.0  Setting alarmstatus in Smart Home Monitor does not set Mode
 *					disabled testing mode with TrueNight and stay with 2 armed modes vs 3 available on keypad
 * 	Aug 24, 2017 v1.1.0  SmartHome sends stay mode when going into night mode lighting the stay mode on
 *  					the Xfinity keypad. Force keypad to show night mode and have no entry delay 
 * 	Aug 28, 2017 v1.0.9a Allow Konnect simulated sensors as only real devices
 * 	Aug 24, 2017 v1.0.9  insure keypads cannot be used for any type of contact sensor
 * 	Aug 23, 2017 v1.0.8  Add test device.typeName for Simulated, police numbers into intrusion message
 *					use standard routine for messages
 * 	Aug 21, 2017 v1.0.7c Add logic to prevent installation this child module pageZero and PageZeroVerify
 * 	Aug 20, 2017 v1.0.7b When globalIntrusionMsg is true suppress non unique sensor notice messages
 * 	Aug 19, 2017 v1.0.7a A community created DTH did not set a manufacturer or model
 *					causing the device reject as a real device. Add test for battery.
 *					simulated devices dont have batteries (hopefully)		
 * 	Aug 19, 2017 v1.0.7  simulated sensor being unique or not is controlled by switch globalSimUnique in parent
 * 	    			   when globalIntrusionMsg is true, issue notifications 
 * 	    			   Open door monitor failing due to single vs multiple sensor definition adjust code
 *					to run as single for now	
 * 	Aug 17, 2017 v1.0.6a require simulated sensor to be unique
 * 	Aug 16, 2017 v1.0.6  add logic check if sensors for unique usage. Stop on real sensor, Warn on simulated
 *	Aug 16, 2017 v1.0.5  add verification editing on sensors and illogical conditions
 *	Aug 15, 2017 v1.0.4  fill Label with real sensor name
 *	Aug 14, 2017 v1.0.3  add exit delay time and logic: 
 *					When away mode do not react to contact opens less than exit delay time
 *	Aug 12, 2017 v1.0.2  add log to notifications, fix push and sms not to log, add multiple SMS logic
 *	Aug 12, 2017 v1.0.1  Allow profile to be named by user with Label parameter on pageOne
 *	Aug 12, 2017 v1.0.0  Combine Smart Delay and Door Monitor into this single child SmartApp
 *
 */
definition(
    name: "SHM Delay Child",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Simulate missing SmartHome entry and exit delay parameters, Child module",
    category: "My Apps",
    parent: "arnbme:SHM Delay",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

preferences {
	page(name: "pageZeroVerify")
	page(name: "pageZero", nextPage: "pageZeroVerify")
	page(name: "pageOne", nextPage: "pageOneVerify")
	page(name: "pageOneVerify")
	page(name: "pageTwo", nextPage: "pageTwoVerify")
	page(name: "pageTwoVerify")
	page(name: "pageThree", nextPage: "pageThreeVerify")
	}


def pageZeroVerify()
	{
	if (parent && parent.getInstallationState()=='COMPLETE')
		{
		pageOne()
		}
	else
		{
		pageZero()
		}
	}	

def pageZero()
	{
	dynamicPage(name: "pageZero", title: "This App cannot be installed", uninstall: true, install:false)
		{
		section
			{
			paragraph "This SmartApp, SHMDelay Child, cannot be installed. Please install and use SHM Delay."
			}
		}
	}	


def pageOne()
	{
	dynamicPage(name: "pageOne", title: "The Sensors", uninstall: true)
		{
		section
			{
			if (state.error_data)
				{
				paragraph "${state.error_data}"
				state.remove("error_data")
				}
			input "thecontact", "capability.contactSensor", required: true, 
				title: "Real Contact Sensor (Remove from SmartHome Monitoring)", submitOnChange: true
			}
		section
			{	
			input "thesimcontact", "capability.contactSensor", required: true,
				title: "Simulated Contact Sensor (Must Monitor in SmartHome)"
			}
		section
			{
			if (parent?.globalMultipleMotion)
				{
				input "themotionsensors", "capability.motionSensor", required: false, multiple: true,
						title: "(Optional!) Ignore these Motion Sensors when Real Contact Sensor opens (Remove from SmartHome Monitoring)"
				}	
			else
				{	
				input "themotionsensor", "capability.motionSensor", required: false,
					title: "(Optional!) Ignore this Motion Sensor when Real Contact Sensor opens (Remove from SmartHome Monitoring)"
				}
			}	
		section
			{
			input "contactname", type: "text", required: false, 
				title: "(Optional!) Contact Name: When Real Contact Sensor is rejected as simulated, enter 4 to 8 alphanumeric characters from the IDE Device Type field to force accept device", submitOnChange: true
			}

		if (thecontact)
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", defaultValue: "Profile: ${thecontact.displayName}", required: false
				}

			}	
		else	
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", required: false
				}
			}	
		}
	}


def pageOneVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data = ""
	def pageTwoWarning
	def ok_names = "(.*)(?i)((C|K)onnect|honeywell|Z[-]Wave|Nortek|RG Linear"
	if (contactname)
		{
		def wknm=contactname.trim()
		if (wknm.matches("([a-zA-Z0-9 ]{4,8})"))
			{ok_names = ok_names + "|" + wknm + ")(.*)"}		
		else
			{
			ok_names = ok_names + ")(.*)"
			error_data = "Contact Name length must be alphanumeric 4 to 8 characters, please reenter\n\n"
			}
		}	
	else	
		{
		ok_names = ok_names + ")(.*)"
//		log.debug "contact name field not provided"
		}
	if (thecontact)
		{
/*		log.debug "editing contact name ${thecontact.typeName}"
		def txt = "xfinity 3400 Keypad xyz"		//test code for failing match group test 
		def m
		if ((m = txt =~ /(.*)(?i)(keypad)(.*)/)) {
		  log.debug "m $m"	
		  def match = m.group(1)			//fails with error message here
		  log.debug "MATCH=$match"}
*/		if (thecontact.typeName.matches("(.*)(?i)(keypad)(.*)"))
			{
			error_data+="Device: ${thecontact.displayName} is not a valid real contact sensor! Please select a differant device or tap 'Remove'\n\n"
			}
		else
		if (thecontact.typeName.matches("(.*)(?i)simulated(.*)") ||
		   (thecontact.getManufacturerName() == null && thecontact.getModelName()==null &&
		    thecontact?.currentState("battery") == null && thecontact?.currentState("batteryStatus") == null &&
		    !thecontact.typeName.matches(ok_names)))
//	Jan 3, 2018	    !thecontact.typeName.matches("(.*)(?i)((C|K)onnect|honeywell|Z[-]Wave|Nortek|RG Linear)(.*)")))
			{
			error_data+="The 'Real Contact Sensor' appears to be simulated. Please select a differant real contact sensor, or enter data into Contact Name field, or tap 'Remove'\n\n"
/*			error_data="'${thecontact.displayName}' is simulated. Please select a differant real contact sensor or tap 'Remove'"
				for some reason the prior line is not seen as a string
*/			}
		else
		if (!iscontactUnique())			
			{
			error_data+="The 'Real Contact Sensor' is already in use. Please select a differant real contact sensor or tap 'Remove'\n\n"
			}
		}	

	if (thesimcontact)
		{
		if (thesimcontact.typeName.matches("(.*)(?i)keypad(.*)"))
			{
			error_data+="Device: ${thesimcontact.displayName} is not a valid simulated contact sensor! Please select a differant device or tap 'Remove'\n\n"
			}
		else
		if (thesimcontact.typeName.matches("(.*)(?i)simulated(.*)") ||
		   (thesimcontact.getManufacturerName() == null && thesimcontact.getModelName()==null &&
		    thesimcontact.currentState("battery") == null && thesimcontact?.currentState("batteryStatus") == null &&
		    !thesimcontact.typeName.matches(ok_names)))
// Jan 3, 2017   !thesimcontact.typeName.matches("(.*)(?i)((C|K)onnect|honeywell|Z[-]Wave|Nortek|RG Linear)(.*)")))
			{
			if (!issimcontactUnique())
				{
				if (parent?.globalSimUnique)
					{
					error_data+="The 'Simulated Contact Sensor' is already in use. Please select a differant simulated contact sensor or tap 'Remove'\n\n"
					}
				else
				if (parent?.globalIntrusionMsg)
					{}
				else	
				if (error_data!="")
					{
					error_data+="Notice: Intrusion messages are off,  but 'Simulated Contact Sensor' already in use. Ignore or tap 'Back' to change device\n\n"
					}
				else
					{
					pageTwoWarning="Notice: Intrusion messages are off, but 'Simulated Contact Sensor' already in use. Ignore or tap 'Back' to change device\n\n"
					}
				}	
			}	
		else
			{
			error_data+="The 'Simulated Contact Sensor' is real. Please select a differant simulated contact sensor or tap 'Remove'\n\n"
			}
		}	
	if (error_data!="")
		{
		state.error_data=error_data.trim()
		pageOne()
		}
	else
		{
		if (pageTwoWarning!=null)			
			{state.error_data=error_data.trim()}
		pageTwo()
		}
	}	

def iscontactUnique()
	{
	def unique = true
	def children = parent?.getChildApps()
//  	log.debug "there are ${children.size()} apps"
//	log.debug "this contact id: ${thecontact.getId()}"
//	log.debug "app install: ${app.getInstallationState()}"
//	log.debug "app id: ${app?.getId()}"
//	def myState = app.currentState()
//	log.debug "current app id: ${myState}"	
//	log.debug current app Id "${myState.getId()}"
	children.each
		{ child ->

//		log.debug "child app id: ${child.getId()} ${child.getLabel()}"	
//		log.debug "child contact Id: ${child.thecontact.getId()}"	
		def childLabel = child.getLabel()
		if (childLabel.matches("(.*)(?i)ModeFix(.*)"))	
			{}
		else
		if (child.thecontact.getId() == thecontact.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}

def issimcontactUnique()
	{
	def unique = true
	def children = parent?.getChildApps()
	children.each
		{ child ->
		def childLabel = child.getLabel()
		if (childLabel.matches("(.*)(?i)ModeFix(.*)"))	
			{}
		else
		if (child.thesimcontact.getId() == thesimcontact.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}

/*  cant make this work in java
def isUnique(contact)
	{
	def unique = true
	def children = parent?.getChildApps()
	children.each
		{ child ->
		if (child.${contact}.getId() == ${contact}.getId() &&
		    child.getId() != app.getId())
			{
			unique=false
			}
		}
	return unique
	}
*/	

def pageTwo()
	{
	dynamicPage(name: "pageTwo", title: "Entry and Exit Data", uninstall: true)
		{
		section("") 
			{
			if (state.error_data)
				{
				paragraph "${state.error_data}"
				state.remove("error_data")
				}
			input "theentrydelay", "number", required: true, range: "0..90", defaultValue: 30,
				title: "Alarm entry delay time in seconds from 0 to 90"
			input "theexitdelay", "number", required: true, range: "0..90", defaultValue: 30,
				title: "When arming in away mode set an exit delay time in seconds from 0 to 90. When using lock-manager app's exit delay, set to 0"
			input "themotiondelay", "number", required: true, range: "0..10", defaultValue: 0,
				title: "When arming in away mode optional motion sensor entry delay time in seconds from 0 to 10, default:0. Usually not needed. Fixes a motion sensor reacting to door movement before contact sensor registers as open. Only when needed, suggested initial value is 5."
			input "thekeypad", "capability.button", required: false, multiple: true,
				title: "Zero or more Optional Keypads: sounds entry delay tone "
			input "thesiren", "capability.alarm", required: false, multiple: true,
				title: "Zero or more Optional Sirens to Beep on entry delay"
			}
		}
	}	

def pageTwoVerify() 				//edit page one info, go to pageTwo when valid
	{
	def error_data=""
	if (thekeypad)
		{
		thekeypad.each		//fails when not defined as multiple contacts
			{
//			log.debug "Current Arm Mode: ${it.currentarmMode} ${it.getManufacturerName()}"
			if (!it.hasCommand("setEntryDelay"))
				{
				error_data="Keypad: ${it.displayName} does not support entry tones. Please remove the device from keypads.\n\n"
				}
			}
		}	
	if (thesiren)
		{
		thesiren.each		//fails when not defined as multiple contacts
			{
			if (it.hasCommand("beep") || (it.hasCommand("siren") && it.hasCommand("off")))
				{}
			else
				{
				error_data+="Siren: ${it.displayName} unable to create a beep with this device. Please remove the device from sirens.\n\n"
				}	
			}
		}	
	if (theentrydelay < 1 && theexitdelay < 1)
		{
		error_data+="Illogical condition: entry and exit delays are both zero\n\n"
		}	
	if (error_data!="")
		{
		state.error_data=error_data.trim()
		pageTwo()
		}
	else 
		{
		pageThree()
		}
	}


def pageThree(error_data)
	{
	dynamicPage(name: "pageThree", title: "Open door monitor and notification settings", install: true, uninstall: true)
		{
		section("")
			{
			input "maxcycles", "number", required: false, range: "1..99", defaultValue: 2,
				title: "Maximum number of open door warning messages"
			input "themonitordelay", "number", required: false, range: "1..15", defaultValue: 1,
				title: "Number of minutes between open door messages from 1 to 15"  	
			paragraph "Following settings are used with Open Door and optional Intrusion messages"
			input "theLog", "bool", required: false, defaultValue:true,
				title: "Log to Notifications?"
    		if (location.contactBookEnabled)
    			{
    			input("recipients", "contact", title: "Notify Contacts",required:false,multiple:true) 
				input "thesendPush", "bool", required: false, defaultValue:false,
					title: "Send Push Notification?"
				}
			else
				{
				input "thesendPush", "bool", required: false, defaultValue:true,
				title: "Send Push Notification?"
				}
			input "phone", "phone", required: false, 
				title: "Send a text message to this number. For multiple SMS recipients, separate phone numbers with a semicolon(;)"
			}
		}
	}	

def pageThreeVerify() 				//edit page three info
	{
	def error_data
	if (theLog || thesendPush || phone|| recipients) 
		{}
	else
		{
		error_data="Please change settings to log the error message"
		}
	if (error_data!=null)
		{
		state.error_data=error_data
		pageThree()
		}
//	else 
//		{
//		pageOne()
//		}
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
	subscribe(location, "alarmSystemStatus", childalarmStatusHandler)
	subscribe(thecontact, "contact.open", doorOpensHandler)
	subscribe(thecontact, "contact.closed", contactClosedHandler)	//open door monitor
	if (parent?.globalMultipleMotion)
		{
		if (themotionsensors)
			{
			subscribe(themotionsensors, "motion.active", motionActiveHandler)
			}
		}	
	else
		{
		if (themotionsensor)
			{
			subscribe(themotionsensor, "motion.active", motionActiveHandler)
			}
		}
	}	

/******** Common Routine monitors the alarm state for changes ********/

def childalarmStatusHandler(evt)
	{
	def theAlarm = evt.value
	def delaydata=evt?.data
//	log.debug "delaydata ${delaydata}"
//	log.debug "alarm state changed stuff ${evt.value} ${evt?.description} ${evt?.name} ${evt.date.time} ${evt?.data} ${delaydata} "
	if (delaydata=="shmtruedelay_rearm")	//True entry delay, rearming is ignored here
		{
//		log.debug "childalarmStatusHandler ignoring the rearm request"
		return false
		}
	else
	if (delaydata=="shmtruedelay_away")		//Process away mode with Entry true delay
		{
//		log.debug "childalarmStatusHandler prepare to rearm away"
		prepare_to_soundalarm("away")
		return false
		}
	else
	if (delaydata=="shmtruedelay_stay")		//Process away mode with Entry true delay
		{
//		log.debug "childalarmStatusHandler prepare to rearm stay"
		prepare_to_soundalarm("stay")
		return false
		}	
	if (theAlarm == "night")	//bad AlarmState processed once by Modefix thats enough
		{return false}		// and we get it almost immediately
	def theMode = location.currentMode	
	log.debug("childalarmStatusHandler1 Alarm: ${theAlarm} Mode: ${theMode} FixMode: ${parent?.globalFixMode}")
	
//	Optionally fix the mode to match the Alarm State. When user sets alarm from dashboard
//	the Mode is not set, resulting in Smarthings having Schizophrenia or cognitive dissonance. 
	if (parent?.globalFixMode)
		{
		def modefix=parent.findChildAppByName("SHM Delay ModeFix")
		log.debug "Modefix: ${modefix.id} ${modefix?.getInstallationState()}"
		if (modefix?.getInstallationState() == 'COMPLETE') 
			{
//			log.debug "going to modefix alarmstatushandler mode: ${theMode}"
			theMode=modefix.alarmStatusHandler(evt)
			log.debug "returned from modefix alarmstatushandler mode: ${theMode}"
			if (!theMode)
				{theMode = location.currentMode}	
			}
		}
		
	if (theAlarm=="off")
		{
		unschedule(soundalarm)		//kill any lingering future tasks for delay or monitor
		killit()				//kill any lingering future tasks for delay or monitor
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
					
		if (parent?.globalKeypad && theAlarm=="stay" && parent?.globalTrueNight && theMode=="Night" && thekeypad)
			{
			thekeypad.each
				{
				if (it.getModelName()=="3400" && it.getManufacturerName()=="CentraLite")
					{
					log.debug "matched, set armrednight issued: ${it.getModelName()} ${it.getManufacturerName()}"
					thekeypad.setArmedNight([delay: 2000])
					}
				}
			}	
		}
	}	
	
// log, send notification, SMS message	
def doNotifications(message)
	{
	def localmsg=message+" at  ${location.name}"	
	if (theLog)
		{
		sendNotificationEvent(localmsg)
		}
	if (location.contactBookEnabled && recipients)
		{
    	sendNotificationToContacts(localmsg, recipients, [event: false])	//Nov 11, 2017 send to selected contacts no log
    	}
	if (thesendPush)
		{
		sendPushMessage(localmsg)
		}
	if (phone)
		{
		def phones = phone.split(";")
//		log.debug "$phones"
		for (def i = 0; i < phones.size(); i++)
			{
			sendSmsMessage(phones[i], localmsg)
			}
		}
	}	

/******** SmartHome Entry Delay Logic ********/

def motionActiveHandler(evt)
	{
//	A motion sensor shows motion
	def triggerDevice = evt.getDevice()
//	log.debug "motionActiveHandler called: $evt by device : ${triggerDevice.displayName}"

//	if not in Away mode, ignore all motion sensor activity
//	When alarm was set less than exit delay time, ignore the motion sensor activity
//	else
//	Entry delay alarm if contact sensor was not opened within entry delay time

//	get alarmstatus and time alarm set in seconds
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	if (alarmstatus != "away")
		{return false}
	def lastupdt = alarm?.date.time
	def alarmSecs = Math.round( lastupdt / 1000)

//	get current time in seconds
	def currT = now()
	def currSecs = Math.round(currT / 1000)	//round back to seconds

//	get status of associated contact sensor
//	def curr_contact = thecontact.currentContact (will be open or closed) not currently in use

	if (theexitdelay > 0 && currSecs - alarmSecs < theexitdelay)
		{return false}
	else
		{
//		process motion sensor event that may occur during an entrydelay		
//		get the last 10 contact sensor events, then find the time the contact was last opened, if open not found: soundalarm
		def events=thecontact.events()
		def esize=events.size()
		def i = 0
//		log.debug "${esize}"
		def open_seconds=999999
		for(i; i < esize; i++)
			{
			if (events[i].value == "open"){
				open_seconds = Math.round((now() - events[i].date.getTime())/1000)
				log.debug("value: ${events[i].value} now: ${now()} startTime: ${events[i].date.getTime()} seconds ${open_seconds}")
				break;
				}
			}	
//		log.debug "scan done ${esize} ${open_seconds}"
		if (open_seconds>theentrydelay)
			{
			def aMap = [data: [lastupdt: lastupdt, shmtruedelay: false, motion: triggerDevice.displayName]]
			log.debug "Away Mode: Intrusion caused by followed motion sensor at ${aMap.data.lastupdt}"
			if (themotiondelay > 0)
				{
				def now = new Date()
				def runTime = new Date(now.getTime() + (themotiondelay * 1000))
				runOnce(runTime, waitfordooropen, [data: aMap]) 
				}
			else	
				{soundalarm(aMap.data)}
			}
		}	

	}	
		
def doorOpensHandler(evt)
	{
/*	def latestDeviceState = thecontact.latestState("closed")  deprecated Jan 08, 2018 data was not used
    	log.debug "latest closed state ${latestDeviceState}"
    	def events=thecontact.events()
	for(def i = 0; i < events.size(); i++) {
		def startTime = events[i].date.getTime()
		log.debug("value: ${events[i].value} startTime: ${startTime}")
		}
*/	
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
//	log.debug "doorOpensHandler ${alarm} ${alarmstatus}"
	if (alarmstatus == "off")
		return false
	def lastupdt = alarm?.date.time
	
	def theMode = location.currentMode
	log.debug "doorOpensHandler called: $evt.value $alarmstatus $lastupdt Mode: $theMode Truenight:${parent.globalTrueNight} "

//	get current time and alarm time in seconds
	def currT = now()
	def currSecs = Math.round(currT / 1000)	//round back to seconds
//	log.debug "${currSecs}"
	def alarmSecs = Math.round( lastupdt / 1000)
//	log.debug "${alarmSecs}"

//	alarmstaus values: off, stay, away
//	check first if this is an exit delay in away mode, if yes monitor the door, else its an alarm
	if (alarmstatus == "away" && currSecs - alarmSecs < theexitdelay)
		{
		new_monitor()
		}
	else	
	if (theentrydelay < 1 || (alarmstatus == "stay" && parent?.globalTrueNight && theMode=="Night"))
		{
		def aMap = [data: [lastupdt: lastupdt, shmtruedelay: false]]
		if (theentrydelay<1)
			{log.debug "EntryDelay is ${settings.theentrydelay}, instant on for alarm ${aMap.data.lastupdt}"}
		else
			{log.debug "Night Mode instant on for alarm ${aMap.data.lastupdt}"}
		soundalarm(aMap.data)
		}
	else
	if (alarmstatus == "stay" || alarmstatus == "away")
		{
		if (themotiondelay > 0)
			{
			unschedule(waitfordooropen)
			}
		if (parent?.globalTrueEntryDelay)
			{
			log.debug "True Entry Mode enabled issuing event SmartHome off"
//			note data object created here is a string not a map, for reasons unknown map field fails						
			def event = [
				name:'alarmSystemStatus',
				value: "off",
				displayed: true,
				description: "SHM Delay True Entry Delay",
				data: "shmtruedelay_"+alarmstatus]
			log.debug "event ${event}"	
			sendLocationEvent(event)	//change alarmstate to stay	
			}
		else
			{prepare_to_soundalarm(false)}
		}
	}	

def prepare_to_soundalarm(shmtruedelay)
	{
	def alarm = location.currentState("alarmSystemStatus")
	def alarmstatus = alarm?.value
	def lastupdt = alarm?.date.time

	log.debug "Prepare to sound alarm entered $shmtruedelay"
//	When keypad is defined: Issue an entrydelay for the delay on keypad. Keypad beeps
	if (settings.thekeypad)
		{
		if (shmtruedelay)
			{thekeypad.setEntryDelay(theentrydelay, [delay: 2000])}
		else
			{thekeypad.setEntryDelay(theentrydelay)}
		}

//		when siren is defined: wait 2 seconds allowing people to get through door, then blast a siren warning beep
//		Aug 31, 2017 add simulated beep when no beep command
	if (settings.thesiren)
		{
		thesiren.each		//fails when not defined as multiple contacts
			{
			if (it.hasCommand("beep"))
				{
				it.beep([delay: 2000])
				}
			else
				{
				it.off([delay: 2500])		//double off the siren to hopefully shut it
				it.siren([delay: 2000])	
				it.off([delay: 2250])
				}
			}
		}	

//		Trigger Alarm in theentrydelay seconds by opening the virtual sensor.
//		Do not delay alarm when additional triggers occur by using overwrite: false
	def now = new Date()
	def runTime = new Date(now.getTime() + (theentrydelay * 1000))
	runOnce(runTime, soundalarm, [data: [lastupdt: lastupdt, shmtruedelay: shmtruedelay], overwrite: false]) 
	}

//	wait for door to open in themotiondelay seconds 
def waitfordooropen(evt)
	{
	log.debug "waitfordooropen entered ${evt}"
	soundalarm (evt.data)
	}

//	Sound the Alarm 
def soundalarm(data)
	{
	def alarm2 = location.currentState("alarmSystemStatus")
	def alarmstatus2 = alarm2.value
	def lastupdt = alarm2.date.time
	log.debug "soundalarm called: $alarmstatus2 $data ${data.lastupdt} $lastupdt"
	if (alarmstatus2=="off" && !data.shmtruedelay) 
		{}
	else
	if (data.lastupdt==lastupdt)		//if this does not match, the system was set off then rearmed in delay period
		{
		if (data.shmtruedelay)
			{
			log.debug "soundalarm rearming in mode ${data.shmtruedelay}"
			def event = [
				name:'alarmSystemStatus',
				value: data.shmtruedelay,
				displayed: true,
				description: "SHM Delay True Delay ReArm in $data.shmtruedelay",
				data: "shmtruedelay_rearm"]
			sendLocationEvent(event)	//change alarmstate to stay	
			thesimcontact.close([delay: 2000])
			log.debug "true entry delay alarm rearmed"	
			thesimcontact.open([delay:2000])
			}
		else	
			{
			thesimcontact.close()		//must use a live simulated sensor or this fails in Simulator
			log.debug "alarm triggered"	
			thesimcontact.open()
			}
//		Aug 19, 2017 issue optional intrusion notificaion messages
		if (parent?.globalIntrusionMsg)
			{
//			log.debug "sending global intrusion message "
//			get names of open contacts for message
			def door_names = thecontact.displayName	//name of each switch in a list(array)
			def message = "${door_names} intrusion"
			if (data?.motion){
				message="${data.motion} motion detected"} 
			if (parent?.global911 > ""  || parent?.globalPolice)
				{
				def msg_emergency
				if (parent?.global911 > "")
					{
					msg_emergency= ", call Police at ${parent?.global911}"
// shows as text 			msg_emergency= "<a href=\"tel://${parent?.global911} \">${parent?.global911}</a>"
					}
				if (parent?.globalPolice)
					{
					if (msg_emergency==null)
						{
						msg_emergency= ", call Police at ${parent?.globalPolice}"
						}
					else
						{
						msg_emergency+= " or ${parent?.globalPolice}"
						}
					}
					
				message+=msg_emergency
				}
			else
				{
				message+=" detected (SHM Delay App)"
				}
			doNotifications(message)	
			}	
		thesimcontact.close([delay: 4000])
		}
	unschedule(soundalarm)					//kill any lingering tasks caused by using overwrite false on runIn
	}

/******** Monitor for Open Doors when SmarthHome is initially Armed *********/	
def new_monitor()
	{
	log.debug "new_monitor called: cycles: $maxcycles"
	unschedule(checkStatus)
	state.cycles = maxcycles
	def now = new Date()
	def runTime = new Date(now.getTime() + (themonitordelay * 60000))
	runOnce (runTime, checkStatus)
	}

def killit()
	{
	log.debug "killit called"
	state.remove('cycles')
	unschedule(checkStatus)	//kill any pending cycles
	}

def countopenContacts() {
//	Aug 19, 2017 returning 0 on open door. comment out multipe support for now
	def curr_contacts = thecontact.currentContact	//status of each contact in a list(array)
	log.debug "countopenContacts entered ${curr_contacts}"
//	count open contacts	
/*	def open_contacts = curr_contacts.findAll 
		{
		contactVal -> contactVal == "open" ? true : false
		}
	log.debug "countopenContacts exit with count: ${open_contacts.size()}"
	return (open_contacts.size())
*/
	if (curr_contacts == "open")
		return 1
	else
		return 0
	}

def contactClosedHandler(evt) 
	{
	log.debug "contactClosedHandler called: $evt.value"
	if (countopenContacts()==0)
		killit()
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
		def runTime = new Date(now.getTime() + (themonitordelay * 60000))

//		get names of open contacts for message
		def curr_contacts= thecontact.currentContact	//status of each switch in a list(array)
/*		def name_contacts= thecontact.displayName		//name of each switch in a list(array)
		def door_names="";
		def door_sep="";
		def ikey=0
		curr_contacts.each		//fails when not defined as multiple contacts
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
*/			
		def door_names = thecontact.displayName
		def message = "${door_names} is open, system armed"
		if (state.cycles<1)
			message+=" (Final Warning)"
		doNotifications(message)
		if (themonitordelay>0 && state.cycles>0)
			{
			log.debug ("issued next checkStatus cycle $themonitordelay ${60*themonitordelay} seconds")
			runOnce(runTime,checkStatus)
			}
		}
	else
		{
		killit()
		}

	}	