 /*
 *  SHM Delay ModeFix 
 *  Functions: Fix the mode when it is invalid, generally cause when using Dashboard to switch modes
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
 * 	Sep 23, 2017    v0.1.2  Ignore alarm changes caused by True Entry Delay in SHM Delay Child
 * 	Sep 05, 2017    v0.1.1  minor code change to allow this module to run stand alone
 * 	Sep 02, 2017    v0.1.0  add code to fix bad alarmstate set by unmodified Keypad module
 * 	Sep 02, 2017    v0.1.0  Repackage logic that was in parent into this module for better reliability
 *					and control
 * 	Aug 26/27, 2017 v0.0.0  Create 
 *
 */

definition(
    name: "SHM Delay ModeFix",
    namespace: "arnbme",
    author: "Arn Burkhoff",
    description: "Fix the ST Mode when using ST Dashboard to change AlarmState",
    category: "My Apps",
    iconUrl: "https://www.arnb.org/IMAGES/hourglass.png",
    iconX2Url: "https://www.arnb.org/IMAGES/hourglass@2x.png",
    iconX3Url: "https://www.arnb.org/IMAGES/hourglass@2x.png")

preferences {
	page(name: "pageOne", nextPage: "pageOneVerify")
	page(name: "pageOneVerify")
	page(name: "pageTwo")
	page(name: "aboutPage", nextPage: "pageOne")
}

def pageOne(error_msg)
	{
	dynamicPage(name: "pageOne", title: "For each alarm state, set valid modes and default modes.", install: false, uninstall: true)
		{
		section
			{
			if (error_msg instanceof String )
				{
				paragraph error_msg
				}
			else
				paragraph "Caution! Wrong settings may create havoc. If you don't fully understand Alarm States and Modes, read the Introduction and use the defaults!"
			href(name: "href",
			title: "Introduction",
			required: false,
			page: "aboutPage")
			}
		section ("Alarm State: Disarmed / Off")
			{
			input "offModes", "mode", required: true, multiple: true, defaultValue: "Home",
				title: "Valid Modes for: Disarmed"
			input "offDefault", "mode", required: true, defaultValue: "Home",
				title: "Default Mode for: Disarmed"
			}	
		section ("Alarm State: Armed (Away)")
			{
			if (away_error_data instanceof String )
				{
				paragraph away_error_data
				}
			input "awayModes", "mode", required: true, multiple: true, defaultValue: "Away",
				title: "Valid modes for: Armed Away"
			input "awayDefault", "mode", required: true, defaultValue: "Away",
				title: "Default Mode: Armed Away"
			}	
		section ("Alarm State: Armed (Home) aka Stay or Night")
			{
			input "stayModes", "mode", required: true, multiple: true, defaultValue: "Night",
				title: "Valid Modes for Armed Home"
			input "stayDefault", "mode", required: true, defaultValue: "Night",
				title: "Default Mode for Armed Home"
			}	
		}
	}	

def pageOneVerify() 				//edit page One
	{

//	Verify disarm/off data
	def off_error="Disarmed / Off Default Mode not defined in Valid Modes"
	def children = offModes
	children.each
		{ child ->
		if (offDefault == child)
			{
			off_error=null
			}
		}
	
//	Verify Away data
	def away_error="Armed (Away) Default Mode not defined in Valid Modes"
	children = awayModes
	children.each
		{ child ->
		if (awayDefault == child)
			{
			away_error=null
			}
		}

//	Verify Stay data
	def stay_error="Armed (Home) Default Mode not defined in Valid Modes"
	children = stayModes
	children.each
		{ child ->
		if (stayDefault == child)
			{
			stay_error=null
			}
		}

	if (off_error == null && away_error == null && stay_error == null)
		{
		pageTwo()
		}
	else	
		{
		def error_msg=""
		def newline=""
		if (off_error>"")
			{
			error_msg=off_error
			newline="\n"
			}
		if (away_error >"")
			{
			error_msg+=newline + away_error
			newline="\n"
			}	
		if (stay_error >"")
			{
			error_msg+=newline + stay_error
			newline="\n"
			}
		pageOne(error_msg)
		}
	}

def pageTwo()
	{
	dynamicPage(name: "pageTwo", title: "Mode settings verified, press 'Done' to install, press '<' to change, ", install: true, uninstall: true)
		{
/*		section
			{
			href(name: "href",
			title: "Introduction",
			required: false,
			page: "aboutPage")
			}
*/		section ("Alarm State: Disarmed / Off")
			{
			input "offModes", "mode", required: true, multiple: true, defaultValue: "Home",
				title: "Valid Modes for: Disarmed"
			input "offDefault", "mode", required: true, defaultValue: "Home",
				title: "Default Mode for: Disarmed"
			}	
		section ("Alarm State: Armed (Away)")
			{
			input "awayModes", "mode", required: true, multiple: true, defaultValue: "Away",
				title: "Valid modes for: Armed Away"
			input "awayDefault", "mode", required: true, defaultValue: "Away",
				title: "Default Mode: Armed Away"
			}	
		section ("Alarm State: Armed (Home) aka Stay or Night")
			{
			input "stayModes", "mode", required: true, multiple: true, defaultValue: "Night",
				title: "Valid Modes for Armed Home"
			input "stayDefault", "mode", required: true, defaultValue: "Night",
				title: "Default Mode for Armed Home"
			}	
		}
	}	

	
def aboutPage()
	{
	dynamicPage(name: "aboutPage", title: "Introduction")
		{
		section 
			{
			paragraph "Have you ever wondered why Mode restricted Routines, SmartApps, and Pistons sometimes fail to execute, or execute when they should not?\n\n"+
			"Perhaps you conflated AlarmState and Mode, however they are separate and independent SmartThings settings, "+
			"and when Alarm State is changed using the SmartThings Dashboard Home Solutions---surprise, Mode does not change!\n\n" +
			"SmartHome routines generally, but not always, have a defined SystemAlarm and Mode settings. "+
			"Experienced SmartThings users seem to favor changing the AlarmState using SmartHome routines, avoiding use of the Dashboard's Home Solutions\n\n"+
			"If like me, you can't keep track of all this, or utilize the Dashboard to change the AlarmState, this app may be helpful.\n\n"+
			"For each AlarmState, set the Valid Mode states, and a Default Mode. This SmartApp attempts to correctly set the Mode by monitoring AlarmState for changes. When the current Mode is not defined as a Valid Mode for the AlarmState, the app sets Mode to the AlarmState's Default Mode\n\n"+
			"Please Note: This app does not, directly or (knowingly) indirectly, execute a SmartHome Routine"  
			}
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
	subscribe(location, "alarmSystemStatus", alarmStatusHandler)
	}

def alarmStatusHandler(evt)
	{
	def theAlarm = evt.value
	if (theAlarm == "night")	//bad AlarmState set by unmodified Keypad module
		{
  		def event = [
  		      name:'alarmSystemStatus',
  		      value: "stay",
  		      displayed: true,
  		      description: "SHM Delay Fix System Status from night to stay"]
    		sendLocationEvent(event)	//change alarmstate to stay	
		setLocationMode("Night")	//set the mode
//		sendNotificationEvent("Change the Lock Manager Keypad module to version in github ARNBME lock-master SHMDelay")
		log.warn "Change the Lock Manager Keypad module to version in github ARNBME lock-master SHMDelay ModeFix"
		return "Night"
		}
	if (parent && !parent.globalFixMode)
		{return false}
	def theMode = location.currentMode
	def oldMode = theMode
	def delaydata=evt?.data
	if (delaydata==null)
		{}
	else	
	if (delaydata.startsWith("shmtruedelay"))	//ignore SHM Delay Child "true entry delay" alarm state changes
		{
		log.debug "Modefix ignoring True Entry Delay event, alarm state ${theAlarm}"
		return false}
	log.debug "ModeFix alarmStatusHandler entered alarm status change: ${theAlarm} Mode: ${theMode} "
//	Fix the mode to match the Alarm State. When user sets alarm from dashboard
//	the Mode is not set, resulting in Smarthings having Schizophrenia or cognitive dissonance. 
	def modeOK=false
	if (theAlarm=="off")
		{
		offModes.each
			{ child ->
			if (theMode == child)
				{modeOK=true}
			}
		if (!modeOK)
			{
			setLocationMode(offDefault)
			theMode=offDefault
			}
		}
	else
	if (theAlarm=="stay")
		{
		stayModes.each
			{ child ->
			if (theMode == child)
				{modeOK=true}
			}
		if (!modeOK)
			{
			setLocationMode(stayDefault)
			theMode=stayDefault
			}
		}
	else
	if (theAlarm=="away")
		{
		awayModes.each
			{ child ->
			if (theMode == child)
				{modeOK=true}
			}
		if (!modeOK)
			{
			setLocationMode(awayDefault)
			theMode=awayDefault
			}
		}
	else{
		log.error "ModeFix alarmStatusHandler Unknown alarm mode: ${theAlarm} in "}
	if (theMode != oldMode)
		{
		log.debug("ModeFix alarmStatusHandler Mode was changed From:$oldMode To:$theMode")
		}
	return theMode
	}
	