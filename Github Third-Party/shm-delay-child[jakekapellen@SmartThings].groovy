/**
 *  Smart Home Delay and Open Contact Monitor Child
 *	Functions: 
 *		Simulate contact entry delay missing from SmartHome.					
 *		Since contact is no longer monitored by SmartHome, monitor it for "0pen" status when system is armed
 *	Warning: SmartHome is fully armed during operation of this SmartApp. Tripping any non simulated sensor 
 *			immediately triggers an intrusion alert
 *
 *  Copyright 2018 Jake Kapellen
 *  Original code Copyright 2017 Arn Burkhoff
 * 
 * 	Changes to Apache License
 *	4. Redistribution. Add paragraph 4e.
 *	4e. This software is free for Private Use. All derivatives and copies of this software must be free of any charges,
 *	 	and cannot be used for commercial purposes.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  	Jul 27  2018 v2.1.0  major refactoring, adding multi-contact-sensor support, ignoring certain contact sensors
 *						 	in "Stay" mode, and scheduled beeping by sirens during entry/exit delay
 *	Jul 19	2018 v2.0.8  fix logic error created by 2.0.7 in new_monitor now has a true/false flag when called 
 *	Jul 19	2018 v2.0.7  Send open door message immediately on arming Run CheckStatus in new_monitor 
 *	Jun 27	2018 v2.0.6  Add logic to trigger SHM Delay Talker exitDelay when away mode triggered by non_keypad device 
 *	Jun 26	2018 v2.0.6  Add logic to trigger SHM Delay Talker using a location event for entryDelay 
 *	Jun 03	2018 v2.0.5  Show Entry Delay on simulated keypad. 
 *	May 30	2018 v2.0.4  True Night Flag working in reverse of specification. 
 *							modify doorsOpensHandler to correctly set armedNight or armedStay in currKeypadMode 
 *	May 26	2018 v2.0.3  Editing crashed processing ikypd profile, adjust logic
 *	Apr 26	2018 v2.0.2  User unable to add simulated contact sensor
 *							modify pageOneVerify logic allowing real contact devices containing word simulated
 *							to be overridden, but leave simulated contact device logic as is
 *	Apr 25	2018 v2.0.1  When multiple delay profiles and motion sensor in multiple profiles triggers false alarm
 *							Use routine checkOtherDelayProfiles when user turns on globalDuplicateMotionSensors
 *							add Version routine
 *	Mar 21	2018 v2.0.0  add optional beep devices when door contact opens.
 *	Mar 04	2018 v2.0.0  Ignore User profiles in function iscontactUnique().
 *							add support for globalDisable flag in first level event processing functions
 *							add logic supporting keypad entry tones and adjust definititions when globalKeypadControl
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
    namespace: "jakekapellen",
    author: "Jake Kapellen",
    description: "(${version()}) Child Delay Profile, Smart Home Monitor Exit/Entry Delays",
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

def version()
	{
	return "2.1.0";
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
            input "contactList", "capability.contactSensor", required: true, multiple: true,
            	title: "List of contact sensors to monitor (Remove from SmartHome Monitoring)", submitOnChange: true
            }
        section
        	{
            input "awayOnlyContacts", "capability.contactSensor", required: false, multiple: true,
            	title: "List of contact sensors to ignore when in Stay Mode (Remove from SmartHome Monitoring)"
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
					title: "(Optional!) Ignore these Motion Sensors during exit delay, and when the Real Contact Sensor opens during entry delay. These sensors are monitored in Alarm State: Away  (Remove from SmartHome Security Armed (Away) Monitoring)"
				}	
			else
				{	
				input "themotionsensor", "capability.motionSensor", required: false,
					title: "(Optional!) Ignore this Motion Sensor during exit delay, and when the Real Contact Sensor opens during entry delay. The sensor is monitored in Alarm State: Away  (Remove from SmartHome Security Armed (Away) Monitoring)"
				}
			}	
		section
			{
			input "contactname", type: "text", required: false, 
				title: "(Optional!) Contact Name: When Real Contact Sensor is rejected as simulated, enter 4 to 8 alphanumeric characters from the IDE Device Type field to force accept device", submitOnChange: true
			}

		if (contactList)
			{
			section([mobileOnly:true]) 
				{
				label title: "Profile name", defaultValue: "Profile: Delay: ${contactList.displayName}", required: false
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
	if (contactList)
    	{
        	contactList.each
            	{ oneContact ->
                	if (!isContactNameValid(oneContact))
                    	{
                        error_data+="Device: ${oneContact.displayName} is not a valid real contact sensor! Please select a different device or tap 'Remove'\n\n"
                        }
                    else
                    if (contactIsSimulated(oneContact,ok_names))
                    	{
                        error_data+="Device: ${oneContact.displayName} appears to be simulated. Please select a different real contact sensor, or enter data into Contact Name field, or tap 'Remove'\n\n"
                        }
                    else
                    if (contactIsDuplicate(oneContact))
                    	{
                        error_data+="Device: ${oneContact.displayName} is included in another delay profile. Please select a different real contact sensor or tap 'Remove'\n\n"
                        }
                }
        }
    if (awayOnlyContacts)
    	{
        	awayOnlyContacts.each
            	{ oneAwayOnlyContact ->
                	if (!isContactNameValid(oneAwayOnlyContact))
                    	{
                        error_data+="Away-Only Device: ${oneAwayOnlyContact.displayName} is not a valid real contact sensor! Please select a different device or tap 'Remove'\n\n"
                        }
                    else
                    if (contactIsSimulated(oneAwayOnlyContact, ok_names))
                    	{
                        error_data+="Away-Only Device: ${oneAwayOnlyContact.displayName} appears to be simulated. Please select a different real contact sensor, or enter data into Contact Name field, or tap 'Remove'\n\n"
                        }
                    else
                    if (!contactIsMonitored(oneAwayOnlyContact))
                    	{
                        error_data+="Away-Only Device: ${oneAwayOnlyContact.displayName} is not being monitored in any delay profile. Please select a different contact sensor or tap 'Remove'\n\n"
                        }                
                }
        }
	if (thesimcontact)
		{
		if (thesimcontact.typeName.matches("(.*)(?i)keypad(.*)"))
			{
			error_data+="Device: ${thesimcontact.displayName} is not a valid simulated contact sensor! Please select a different device or tap 'Remove'\n\n"
			}
		else
		if (thesimcontact.typeName.matches("(.*)(?i)simulated(.*)") ||
		    (thesimcontact.getManufacturerName() == null && thesimcontact.getModelName()==null &&
		    thesimcontact.currentState("battery") == null && thesimcontact?.currentState("batteryStatus") == null &&
		    !thesimcontact.typeName.matches(ok_names)))
			{
			if (!issimcontactUnique())
				{
				if (parent?.globalSimUnique)
					{
					error_data+="The 'Simulated Contact Sensor' is already in use. Please select a different simulated contact sensor or tap 'Remove'\n\n"
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
			error_data+="The 'Simulated Contact Sensor' is real. Please select a different simulated contact sensor or tap 'Remove'\n\n"
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
def isContactNameValid(oneContact)
	{
    	def valid=true
        if (oneContact.typeName.matches("(.*)(?i)(keypad)(.*)"))
        	{
            valid=false
            }
        return valid
    }
def contactIsSimulated(oneContact, ok_names)
	{
    	def isSimulated = false
    	if ((oneContact.typeName.matches("(.*)(?i)simulated(.*)") ||
		    oneContact.getManufacturerName() == null && oneContact.getModelName()==null &&
		    oneContact?.currentState("battery") == null && oneContact?.currentState("batteryStatus") == null) &&
		    !oneContact.typeName.matches(ok_names))
			{
			isSimulated = true
            }
		return isSimulated
    }
def contactIsMonitored(oneContact)
	{
    	def appId = ""
        appId = getAppIdOfContact(oneContact)
        return (appId != "")
    }
def contactIsDuplicate(oneContact)
	{
    def appId = ""
    appId = getAppIdOfContact(oneContact)
    return (appId != app.getId())
    }

private getAppIdOfContact(oneContact)
	{
    def appId = ""
    def duplicate = false
    def childApps = parent?.getChildApps()
        
    childApps.each
    	{ oneChildApp ->
        
        if (oneChildApp.getName()!="SHM Delay Child")
        	{}
        else
        	{
            oneChildApp.contactList.each
            	{ childAppContact ->
                	if (childAppContact.getId() == oneContact.getId())
                    	{
                        appId = oneChildApp.getId()
                        }
                }
            }
        }
    return appId
    }
    
def issimcontactUnique()
	{
	def unique = true
	def children = parent?.getChildApps()
	children.each
		{ child ->
		def childLabel = child.getLabel()
		if (child.getName()!="SHM Delay Child")	
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
			if (parent.globalKeypadControl)
				{
				input "theexitdelay", "number", required: true, range: "0..90", defaultValue: 30,
					title: "When arming in away mode without the keypad, set a simulated exit delay time in seconds from 0 to 90."
				paragraph "When arming in away mode with a keypad the true exit delay is ${parent?.globalKeypadExitDelay} seconds."
				}
			else
				input "theexitdelay", "number", required: true, range: "0..90", defaultValue: 30,
					title: "When arming in away mode set an exit delay time in seconds from 0 to 90. When using lock-manager app's exit delay, set to 0"
			input "themotiondelay", "number", required: true, range: "0..10", defaultValue: 0,
				title: "When arming in away mode optional motion sensor entry delay time in seconds from 0 to 10, default:0. Usually not needed. Fixes a motion sensor reacting to door movement before contact sensor registers as open. Only when needed, suggested initial value is 5."
			if (parent.globalKeypadControl)
				paragraph "All keypads defined in parent module sound delay tones when supported by device"
			else
				{
				input "thekeypad", "capability.button", required: false, multiple: true,
					title: "Sound entry delay tones on these keypads (Optional)"
				}	
			input "thesiren", "capability.alarm", required: false, multiple: true,
				title: "Beep these devices on entry delay (Optional)"
			input "thebeepers", "capability.tone", required: false, multiple: true, 
				title: "Beep/Chime these devices when real contact sensor opens, and Alarm State is Off (Optional)"
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
			if (it.hasCommand("beep") || (it.hasCommand("siren") && it.hasCommand("off")) || (it.hasCommand("on") && it.hasCommand("off")))
				{}
			else
				{
				error_data+="Entry Delay Beep Device: ${it.displayName} unable to create a beep with this device. Please remove the device from sirens.\n\n"
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
    
    //JMK TODO: This subscription isn't working! I'm triggering this manually!
    subscribe(location, "shmdelaytalk", delayTalkHandler)
    
    contactList.each
    	{ oneContact ->
            subscribe(oneContact, "contact.open", doorOpensHandler)
            subscribe(oneContact, "contact.closed", contactClosedHandler)
        }
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

def getSHMAlarmStatus()
	{
    	return location.currentState("alarmSystemStatus")
    }

def getSHMLastUpdateSeconds()
	{
    	def alarm = getSHMAlarmStatus()
        def lastupdt = alarm?.date.time
        return Math.round( lastupdt / 1000)
    }

def getKSeconds()
	{
    	def kMap=parent?.atomicState.kMap
        return Math.round(kMap.dtim / 1000)
    }

def getSecondsSinceLastSHMUpdate()
	{
		def alarmSecs = getSHMLastUpdateSeconds()
        def kSecs = getKSeconds()
        
        return (alarmSecs - kSecs)
    }
def getCurrentTimeSeconds()
	{
    	def currT = now()
        return Math.round(currT / 1000)
    }

def delayTalkHandler(evt)
	{
    	def value = evt.value
        if (value == "exitDelay")
        	{
            scheduledBeepSirens()
            }
       	else
        if (value == "exitAway")
        	{
            unschedule(scheduledBeepSirens)
            }
         
    }

/******** Common Routine monitors the alarm state for changes ********/
def childalarmStatusHandler(evt)
	{
	if (parent.globalDisable)
		return false
	def theAlarm = evt.value
	def delayType=evt?.data
	if (delayType=="shmtruedelay_rearm")	//True entry delay, rearming is ignored here
		{
//		log.debug "childalarmStatusHandler ignoring the rearm request"
		return false
		}
	else
	if (delayType=="shmtruedelay_away")		//Process away mode with Entry true delay
		{
//		log.debug "childalarmStatusHandler prepare to rearm away"
		prepare_to_soundalarm("away")
		return false
		}
	else
	if (delayType=="shmtruedelay_stay")		//Process away mode with Entry true delay
		{
//		log.debug "childalarmStatusHandler prepare to rearm stay"
		prepare_to_soundalarm("stay")
		return false
		}	
	if (theAlarm == "night")	//bad AlarmState processed once by Modefix thats enough
		{return false}		// and we get it almost immediately
	
//	Jun 27, 2018 add logic to sendLocationEvent for SHM Delay Talk when away mode triggered by non keypad device
	def locevent = [name:"shmdelaytalk", value: "exitDelayNkypd", isStateChange: true,
		displayed: true, descriptionText: "Issue exit delay talk event", linkText: "Issue exit delay talk event",
		data: theexitdelay]
	if (theAlarm == 'away' && theexitdelay > 0)
		{
		if (parent?.globalKeypadControl)
			{
			if (getSecondsSinceLastSHMUpdate() > theexitdelay+4)		//allow 4 second delay for ST delays due to cloud and internet
				{
				sendLocationEvent(locevent)
//				log.debug "Away Talker from non keypad triggered"
				}
			}
		else	
			{
			sendLocationEvent(locevent)
			}
		}	

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
//			reset the map adding childid telling modefix not to log this to notifications
			def evtMap = [value: evt.value, source: evt.source, childid: "childid"]
//			log.debug "${evtMap}"
			theMode=modefix.alarmStatusHandler(evtMap)
//			theMode=modefix.alarmStatusHandler(evt)		//deprecated Mar 11, 2018
			log.debug "returned from modefix alarmstatushandler mode: ${theMode}"
			if (!theMode)
				{theMode = location.currentMode}	
			}
		}
		
	if (theAlarm=="off")
		{
		unschedule(soundalarm)		//kill any lingering future tasks for delay or monitor
        unschedule(scheduledBeepSirens)
		killit()				//kill any lingering future tasks for delay or monitor
		}
	else
		{
		if (countopenContacts(contactList)==0)
			{
			killit()
			}
		else
			{
			new_monitor(false)
			}
		}
	}	
	
// log, send notification, SMS message	
def doNotifications(message)
	{
	def localmsg=message+" at ${location.name}"	
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
	if (parent.globalDisable)
		return false
//	A motion sensor shows motion
	def triggerDevice = evt.getDevice()
	log.debug "motionActiveHandler called: $evt by device : ${triggerDevice.displayName}"

//	if not in Away mode, ignore all motion sensor activity
//	When alarm was set less than exit delay time, ignore the motion sensor activity
//	else
//	Entry delay alarm if contact sensor was not opened within entry delay time

//	get alarmstatus and time alarm set in seconds
	def alarm = getSHMAlarmStatus()
	def alarmstatus = alarm?.value
	if (alarmstatus != "away")
		{return false}
        
	def alarmSecs = getSHMLastUpdateSeconds()
	def currSecs = getCurrentTimeSeconds()	//round back to seconds
	def kSecs=getKSeconds()
    
	if (parent.globalKeypadControl && theexitdelay > 0 && 
		alarmSecs - kSecs > 4 && currSecs - alarmSecs < theexitdelay)
		{
//		log.debug "motionActiveHandler return1"
		return false
		}
	else
	if (!parent?.globalKeypadControl && theexitdelay > 0 && currSecs - alarmSecs < theexitdelay)
		{
//		log.debug "motionActiveHandler return2"
		return false
		}
	else
		{
//		process motion sensor event that may occur during an entrydelay		
        def open_seconds = getSecondsSinceLastOpenContact()
		if (open_seconds>theentrydelay)
			{
					
			def aMap = [data: [lastupdt: lastupdt, shmtruedelay: false, motion: triggerDevice.displayName]]
			if (themotiondelay > 0)
				{
				def now = new Date()
				def runTime = new Date(now.getTime() + (themotiondelay * 1000))
				runOnce(runTime, waitfordooropen, [data: aMap]) 
				}
			else
				{
				log.debug "*****testing duplicate sensor flag*******"
				if (parent?.globalDuplicateMotionSensors)
					{
					log.debug "*****Calling checkOtherDelayProfile*******"
     
                    contactList.each
                    	{ oneContact ->
                        	if (checkOtherDelayProfiles(oneContact, triggerDevice, theentrydelay))
								{return false}
                        }
					}
			log.debug "Away Mode: Intrusion caused by followed motion sensor at ${aMap.data.lastupdt}"
			soundalarm(aMap.data)
			}
		}	

	}	
}
def getSecondsSinceLastOpenContact()
	{
    	//get the last 10 contact sensor events, then find the time the contact was last opened, if open not found: soundalarm
        def events=getAllEvents(contactList)
        
		def esize=events.size()
		def i = 0
//		log.debug "motionActiveHandler scanning events ${esize}"
		def open_seconds=999999
		for(i; i < esize; i++)
			{
			if (events[i].value == "open"){
				open_seconds = Math.round((now() - events[i].date.getTime())/1000)
				log.debug("value: ${events[i].value} now: ${now()} startTime: ${events[i].date.getTime()} seconds ${open_seconds}")
				break;
				}
			}
        return open_seconds
    }

def getAllEvents(contactList)
	{
    	def events
        contactList.each
        	{ oneContact ->
            	events = (events+oneContact.currentContact)
            }
        return events
    }

def doorOpensHandler(evt)
	{
	if (parent.globalDisable)
		return false

	def alarm = getSHMAlarmStatus()
	def alarmstatus = alarm?.value
//	log.debug "doorOpensHandler ${alarm} ${alarmstatus}"
	if (alarmstatus == "off")
		{
		thebeepers?.each
			{
            if (settings.thesiren)
            	{
                beepSirens()
                }
            else
			if (it?.currentValue("armMode")!="exitDelay")		//bypass keypads beeping exit delay tones
				{
					it.beep()
                }
			}	
		return false
		}
        
    if (alarmstatus == "stay")
    	{
        awayOnlyContacts.each
        	{ oneAwayOnlyContact ->
            	if (oneAwayOnlyContact == evt.getDevice())
                	{
                    	log.debug "doorOpensHandler ignoring away-only contact: ${evt.getDevice()}"
                    	return false
                    }
            }
        }
	def lastupdt = alarm?.date.time
	
	def theMode = location.currentMode
	log.debug "doorOpensHandler called: $evt.value $alarmstatus $lastupdt Mode: $theMode Truenight:${parent.globalTrueNight} "

	def currSecs = getCurrentTimeSeconds()
	def alarmSecs = getSHMLastUpdateSeconds()
	def kSecs = getKSeconds()
    
//	alarmstaus values: off, stay, away
//	check first if this is an exit delay in away mode, if yes monitor the door, else its an alarm
	
	def currkeypadmode=""
	if (parent?.globalKeypadControl)
		{
//		Get the status of the first (non-Iris) 3400 keypad
		parent?.globalKeypadDevices?.each
			{
			if (it.getModelName()=="3400" && currkeypadmode=="")
				{
				currkeypadmode = it?.currentValue("armMode")
				log.debug "keypad set currkeypadmode to $currkeypadmode"
				}
			}	
		}	

//	no 3400 keypad found or set currkeypad mode from globalTrueNight for stay mode
//	updated V2.0.4 fixes incorrect operation of globalTrueNight flag 
	if (currkeypadmode=="")
		{
		if (parent?.globalTrueNight)
			currkeypadmode='armedNight'
		else	
			currkeypadmode='armedStay'
		log.debug "globalTrueNight set currkeypadmode to $currkeypadmode"
		}
    
	if (alarmstatus == "away" && parent.globalKeypadControl && theexitdelay > 0 && 
		alarmSecs - kSecs > 4 && currSecs - alarmSecs < theexitdelay)
		{
        scheduledBeepSirens()
        new_monitor(true)
		}
	else
	if (alarmstatus == "away" && !parent.globalKeypadControl && currSecs - alarmSecs < theexitdelay)
		{
        scheduledBeepSirens()
		new_monitor(true)
		}
	else	
//	if (theentrydelay < 1 || (alarmstatus == "stay" && parent?.globalTrueNight && theMode=="Night")) Mar 23, 2018
	if (theentrydelay < 1 || (alarmstatus == "stay" && currkeypadmode!="armedStay"))
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
	def alarm = getSHMAlarmStatus()
	def alarmstatus = alarm?.value
	def lastupdt = alarm?.date.time
	log.debug "Prepare to sound alarm entered $shmtruedelay"
//	When keypad is defined: Issue an entrydelay for the delay on keypad. Keypad beeps
	if (parent?.globalKeypadControl)
		{
		parent.globalKeypadDevices.each()
			{
			if (it.hasCommand("setEntryDelay"))
				{
				if (shmtruedelay)
					{it.setEntryDelay(theentrydelay, [delay: 2000])}
				else
					{it.setEntryDelay(theentrydelay)}
				}
			}
		parent.qsse_status_mode(false,"Entry%20Delay")
		}	
	else
		{
		if (settings.thekeypad)
			{
			if (shmtruedelay)
				{thekeypad.setEntryDelay(theentrydelay, [delay: 2000])}
			else
				{thekeypad.setEntryDelay(theentrydelay)}
			}
		}
//		when siren is defined: wait 2 seconds allowing people to get through door, then blast a siren warning beep
//		Aug 31, 2017 add simulated beep when no beep command

    
    scheduledBeepSirens()

//		Trigger Alarm in theentrydelay seconds by opening the virtual sensor.
//		Do not delay alarm when additional triggers occur by using overwrite: false
	def now = new Date()
	def runTime = new Date(now.getTime() + (theentrydelay * 1000))
	runOnce(runTime, soundalarm, [data: [lastupdt: lastupdt, shmtruedelay: shmtruedelay], overwrite: false]) 
	def locevent = [name:"shmdelaytalk", value: "entryDelay", isStateChange: true,
    		displayed: true, descriptionText: "Issue entry delay talk event", linkText: "Issue entry delay talk event",
    		data: theentrydelay]
    sendLocationEvent(locevent)
//	log.debug "sent location event for shmdelaytalk"
	}

//JMK TODO: not used yet... can you use the Dome's built-in delay/beeping feature for a better experience?
def unscheduleBeepSirens()
	{
    if (settings.thesiren)
    	{
        settings.thesiren.each
        	{ oneSiren ->
            	oneSiren.off()
            }
        }
    }

def scheduledBeepSirens(interval = 3)
	{
    	beepSirens()
   		runIn(interval, scheduledBeepSirens)
    }

def beepSirens()
	{
    if (settings.thesiren)
    	{
        settings.thesiren.each
        	{ oneSiren ->
            
            if (oneSiren.hasCommand("beep"))
				{
				//it.beep([delay: 2000])
                oneSiren.beep()
				}
			else
				{
				//it.off([delay: 2500])		//double off the siren to hopefully shut it
                //it.siren([delay: 2000])	
				//it.off([delay: 2250])
				oneSiren.off()
                oneSiren.on()
				oneSiren.off()
				}
            }
        }
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
	def alarm2 = getSHMAlarmStatus()
	def alarmstatus2 = alarm2.value
	def lastupdt = alarm2.date.time
	log.debug "soundalarm called: $alarmstatus2 $data ${data.lastupdt} $lastupdt"
	unschedule(scheduledBeepSirens)  //JMK DOES THIS WORK?
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
			parent.qsse_status_mode(false,"**Intrusion**")
			}
		else	
			{
			thesimcontact.close()		//must use a live simulated sensor or this fails in Simulator
			log.debug "alarm triggered"	
			thesimcontact.open()
			parent.qsse_status_mode(false,"**Intrusion**")
			}
//		Aug 19, 2017 issue optional intrusion notificaion messages
		if (parent?.globalIntrusionMsg)
			{
//			log.debug "sending global intrusion message "
//			get names of open contacts for message
			def door_names = getOpenContactNameList(contactList)
            if (door_names == "")
            	{door_names = "One or more doors or windows were"}
			def message = "${door_names} open - intrusion"
			if (data?.motion){
				message="${data.motion} motion detected"} 
			if (parent?.global911 > ""  || parent?.globalPolice)
				{
				def msg_emergency
				if (parent?.global911 > "")
					{
					msg_emergency= ", call ${parent?.global911}"
// shows as text 			msg_emergency= "<a href=\"tel://${parent?.global911} \">${parent?.global911}</a>"
					}
				if (parent?.globalPolice)
					{
					if (msg_emergency==null)
						{
						msg_emergency= ", call the police at ${parent?.globalPolice}"
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
	
    //JMK BEGIN UNUSED CODE SECTION
def setSHMAway() {setSHMAlarmStatus("away")}
def setSHMStay() {setSHMAlarmStatus("stay")}
def setSHMOff() {setSHMAlarmStatus("off")}
def setSHMEntryDelay() {setSHMAlarmStatus("off","entry")}
def setSHMExitDelay() {setSHMAlarmStatus("off","exit")}

    /* Updates SHM's alarm system status. This is used to synchronize the keypad/delay system
     * with SHM.
     * Allowed values:
     * "away", "stay", "off"
     */
private setSHMAlarmStatus(status, delayType = "")
	{
    if (status!="away" && status !="stay" && status!="off")
    	return false
    def event = [name:'alarmSystemStatus',
				value: "$status",
				displayed: true,
				description: "SHM Delay True Entry Delay",
				data: delayType]
    sendLocationEvent(event)
    return true
    }
    // JMK END UNUSED CODE SECTION

/******** Monitor for Open Doors when SmarthHome is initially Armed *********/
//	on July 19, 2018 changed to instant check when delay = false coming from open doors check at arming
//	changed all executions to include true or false on new_monitor call
def new_monitor(delay)
	{
	log.debug "new_monitor called: cycles: $maxcycles"
	unschedule(checkStatus)
	state.cycles = maxcycles
	if (!delay)
		checkStatus()
	else
		{
		def now = new Date()
		def runTime = new Date(now.getTime() + (themonitordelay * 60000))
		runOnce (runTime, checkStatus)
		}
	}	

def killit()
	{
	log.debug "killit called"
	state.remove('cycles')
	unschedule(checkStatus)	//kill any pending cycles
	}
def getOpenContacts(contactList)
	{
    	def open_contacts =[]
        contactList.each
        	{ oneContact->
            if (oneContact.currentContact == "open")
            	{
                open_contacts=(open_contacts+oneContact)
                }
            }
    	return open_contacts
    }

def countopenContacts(contactList) {
    def open_contacts = getOpenContacts(contactList)
	log.debug "countopenContacts exit with count: ${open_contacts.size()}"
	return (open_contacts.size())
	}

def contactClosedHandler(evt) 
	{
	if (parent.globalDisable)
		return false
	log.debug "contactClosedHandler called: $evt.value"
	if (countopenContacts(contactList)==0)
		killit()
	}

def getOpenContactNameList(contactList)
	{
    	def open_contacts=getOpenContacts(contactList)
        def door_names=""
        open_contacts.each
        	{ oneOpenContact ->
            	if (door_names=="")
                	{
                    door_names=oneOpenContact.displayName
                    }
                else
                	{
                    door_names+=", "+oneOpenContact.displayName
                    }
            }
        return door_names
    }

def checkStatus()
	{
	// get the current state for alarm system
	def alarmstate = getSHMAlarmStatus()
	def alarmvalue = alarmstate.value
	def door_count=countopenContacts(contactList)		//get open contact count
	log.debug "In checkStatus: Alarm: $alarmvalue Doors Open: ${door_count} MessageCycles remaining: $state.cycles"
//	Check if armed and one or more contacts are open
	if ((alarmvalue == "stay" || alarmvalue == "away") && door_count>0)
		{
		state.cycles = state.cycles - 1	//decrement cycle count
//		state.cycles--  note to self this does not work

//		calc standard next runOnce time
		def now = new Date()
		def runTime = new Date(now.getTime() + (themonitordelay * 60000))
        def door_names=getOpenContactNameList(contactList)
        
        def message = ""
		if (door_names>"")
			{
			if (door_count > 1)
				message = door_names + " are open, system armed"
			else	
				message = door_names + " is open, system armed"
			}
			
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
	
/*
When a motion sensor is defined in multiple delay profiles, it may trigger a false alarm when one of the contact 
sensors opens, since the other profile's contact is closed giving an instant alarm. This routine is called prior to the 
child motion sensor alert issueing an alarm.
return true = Suppress Alarm
otherwise return false

Moved from parent to child. Makes debugging easier since debug messages are contained in single thread
*/
def checkOtherDelayProfiles(baseContact, baseMotion, baseEntryDelay)
	{
	def	ignoreSensor=false
	log.debug "checkOtherDelayProfiles entered Contact: ${baseContact}, Motion: ${baseMotion}, Delay: ${baseEntryDelay}"
	def profiles=parent.findAllChildAppsByName('SHM Delay Child')
//	Beginning of ***FIND*** loop
	profiles.find
		{
		if (it?.getInstallationState()!='COMPLETE')
			{
			log.debug "Incomplete profile skipped: ${it?.contactList.displayName}"
			return false	//this continues the ***find*** loop, does not end function
			}			
		log.debug "looping on profile: ${it?.contactList.displayName} Comparing: ${baseContact.displayName}"

        it?.contactList.each
        	{ oneContact ->
            	if (oneContact.displayName==baseContact.displayName)
                	{
                    log.debug "Active Profile skipped"
                    return false  //this continues the ***find*** loop, does not end function
                    }
            }
 
		if (parent.globalMultipleMotion)	
			{
			log.debug "finding motion in multiple motion profile: ${it?.themotionsensors} Comparing: ${baseMotion.displayName}"
			if (it?.themotionsensors.displayName.contains(baseMotion.displayName))			//is this the active profile
				{}
			else
				{
				log.debug "Profile ${it?.contactList.displayName} skipped motion sensor: baseMotion.displayName  not found in multiple" 
				return false	//this continues the ***find*** loop, does not end function
				}			
			}
		else
			{
			log.debug "finding motion in single motion profile: ${it?.themotionsensor.displayName} Comparing: ${baseMotion.displayName}"
			if (it?.themotionsensor.displayName!=baseMotion.displayName)			//is this the active profile
				{
				log.debug "Profile skipped motion sensor not found" 
				return false	//this continues the ***find*** loop, does not end function
				}			
			}
        log.debug "Motion ${baseMotion.displayName} sensor was found in ${it?.contactList.displayName} Profile that is ${it?.contactList.currentContact}" 
		if (countOpenContacts(it?.contactList)==0)		//ignore this motion sensor other profile contact is open
			{}
		else	
			{
//			get the last 10 contact sensor events, then find the time the contact was last opened
			def events=getAllEvents(it.contactList)
			def esize=events.size()
			def i = 0
//			log.debug "motionActiveHandler scanning events ${esize}"
			def open_seconds=999999
			for(i; i < esize; i++)
				{
				if (events[i].value == "open"){
					open_seconds = Math.round((now() - events[i].date.getTime())/1000)
//					log.debug("value: ${events[i].value} now: ${now()} startTime: ${events[i].date.getTime()} seconds ${open_seconds}")
					break;
					}
				}	
//			log.debug "motionActiveHandler scan done ${esize} ${open_seconds}"
			if (open_seconds > baseEntryDelay)
				return false	//this continues the ***find*** loop, does not end function
			}
		ignoreSensor=true		//set to ignore this sensors motion
		return true				//this terminates the ***find*** loop, does not return to caller
		}						
//		end of ***FIND*** loop logic		

	return ignoreSensor			//return to caller
	}
