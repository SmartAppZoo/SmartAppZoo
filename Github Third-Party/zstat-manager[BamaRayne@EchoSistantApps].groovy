/* 
 * zStat Manager - An EchoSistant Smart App 
 *
 *	5/31/2017		Version:1.0 R.0.0.1		Initial release
 *
 *
 *  Copyright 2017 Jason Headley & Bobby Dobrescu
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
/**********************************************************************************************************************************************/
definition(
    name		: "zStat Manager",
    namespace	: "Echo",
    author		: "JH/BD",
    description	: "Control your thermostat based on current temperature",
    category	: "My Apps",
	iconUrl		: "http://icons.iconarchive.com/icons/icons8/windows-8/512/Science-Temperature-icon.png",
	iconX2Url	: "http://icons.iconarchive.com/icons/icons8/windows-8/512/Science-Temperature-icon.png")

/**********************************************************************************************************************************************/
private def textVersion() {
	def text = "1.0"
}
private release() {
    def text = "R.0.0.1"
}
/**********************************************************************************************************************************************/

preferences {
	page(name: "main")
    page(name: "profiles")
    page(name: "advanced")
}

		page name: "main"
            def main() {
                dynamicPage (name: "main", title: "", install: true, uninstall: true) {
                    if (childApps?.size()) {  
                        section("zStat Profiles",  uninstall: false){
                            app(name: "profiles", appName: "zStat Profiles", namespace: "Echo", title: "Create a new Profile", multiple: true,  uninstall: false)
                        }
                    }
                    else {
                        section("zStat Profiles",  uninstall: false){
                            paragraph "NOTE: Looks like you haven't created any profiles yet.\n \nPlease make sure you have installed the Echo : zStat Manager app before creating your first profile!"
                            app(name: "profiles", appName: "zStat Profiles", namespace: "Echo", title: "Create a new Profile", multiple: true,  uninstall: false)
                        }
                    }
					if (state.filterNotif) {
                    	section("Next Scheduled Filter Replacement"){
                        	paragraph ("${state.filterNotif}")
						}
                    }
					section("Settings",  uninstall: false, hideable: true, hidden: true){
						input "debug", "bool", title: "Enable Debug Logging", default: true, submitOnChange: true
                        input "filter", "bool", title: "Enable Filter Replacement Reminders", default: false, submitOnChange: true
						 if(filter){
                         	href "sFilters", title: "Filter Replacement Settings" , description: pFiltersComplete(), state: pFiltersSettings()
                        }
                        if(filter==false && state.filterNotif != null) scheduleHandler()
                        paragraph ("Version: ${textVersion()} | Release: ${release()}")
					}
             	}
	        }
page name: "sFilters"
	def sFilters(){
		dynamicPage(name: "sFilters", title: "", uninstall: false){    
					section ("HVAC Filter(s) Replacement Reminders Options", hideWhenEmpty: true, hideable: false, hidden: false) {
						input "cFilterReplacement", "number", title: "Remind me to replace the HVAC filter(s) in this many days", defaultValue: 90, required: false                        
                    	input "cFilterContact", "capability.contactSensor", title: "Use this contact sensor to schedule reminder (OPTIONAL)", multiple: false, required: false
                        input "speechSynth", "capability.speechSynthesis", title: "Play reminder on Speech Synthesis Type Device(s)", multiple: true, required: false
                        input "sonos", "capability.musicPlayer", title: "Play reminder on Sonos Type Device(s)", required: false, multiple: true , submitOnChange: true 
                        if (cFilterSonosDevice) {
                            input "volume", "number", title: "At this volume", description: "0-100%", required: false
                    		input "resumePlaying", "bool", title: "Resume currently playing music", required: false, defaultValue: false

                        }
						if (location.contactBookEnabled){
                        	input "recipients", "contact", title: "Send reminder to contact(s) ", multiple: true, required: false
           				}
                        else {      
                            input name: "sms", title: "Send SMS to phone(s) ", type: "phone", required: false
                        		paragraph "You may enter multiple phone numbers separated by comma (E.G. 8045551122,8046663344)"
                            input "push", "bool", title: "Send Push Notification too?", required: false, defaultValue: false
                        }
                    } 
	}
}            
           
            
            
/************************************************************************************************************
		Base Process
************************************************************************************************************/
def installed() {
	if (debug) log.debug "Installed with settings: ${settings}"
    state.ParentRelease = release()
    state.filterNotif
    initialize()
}
def updated() { 
	if (debug) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}
def initialize() {
		subscribe(app, appHandler)
        subscribe(cFilterContact, "contact.closed", appHandler)
        //Other Apps Events
        state.esEvent = [:]
        subscribe(location, "echoSistant", echoSistantHandler)
		state.esProfiles = state.esProfiles ? state.esProfiles : []
        //CoRE and other 3rd party apps
        sendLocationEvent(name: "zStat", value: "refresh", data: [profiles: getProfileList()] , isStateChange: true, descriptionText: "zStat list refresh")
        def children = getChildApps()
}
/************************************************************************************************************
		3RD Party Integrations
************************************************************************************************************/
def echoSistantHandler(evt) {
	def result
	if (!evt) return
    log.warn "received event from EchoSistant with data: $evt.data"
	switch (evt.value) {
		case "refresh":
		state.esProfiles = evt.jsonData && evt.jsonData?.profiles ? evt.jsonData.profiles : []
			break
		case "runReport":
			def profile = evt.jsonData
            	result = runReport(profile)
            break	
    }
    return result
}
def listEchoSistantProfiles() {
log.warn "child requesting esProfiles"
	return state.esProfiles = state.esProfiles ? state.esProfiles : []
}

def getProfileList(){
		return getChildApps()*.label
}
def childUninstalled() {
	if (debug) log.debug "Refreshing Profiles for 3rd party apps, ${getChildApps()*.label}"
    sendLocationEvent(name: "remindR", value: "refresh", data: [profiles: getProfileList()] , isStateChange: true, descriptionText: "RemindR list refresh")
}
/***********************************************************************************************************************
    App Touch
***********************************************************************************************************************/
def appHandler(evt) {
    scheduleHandler()
    log.debug "app event ${evt.name}:${evt.value} received"
}
/***********************************************************************************************************************
    TOGGLE SCHEDULE REMINDER
***********************************************************************************************************************/
def scheduleHandler(){
    def rowDate = new Date(now())
    def cDay = rowDate.date
    def cHour= rowDate.hours
	def cMin = rowDate.minutes   
    def result
    if(state.filterNotif == null){
    	if (debug) log.debug "Received filter replacement request"
        def xDays = settings.cFilterReplacement
        def tDays = new Date(now() + location.timeZone.rawOffset) + xDays 
        def schTime = tDays.format("h:mm aa")                       
		def schDate = tDays.format("EEEE, MMMM d")
       		runOnce(new Date() + xDays , "filtersHandler")
        	result = "Ok, scheduled reminder to replace the filters on " + schDate + " at " + schTime
        	state.filterNotif = "Filters are scheduled to be changed on ${schDate}"
	}
   	else {
		state.filterNotif = null
		unschedule("filtersHandler")
		result = "Ok, canceling reminder to replace HVAC filters"
	}
   	filtersHandler(result)
}
/***********************************************************************************************************************
    PLAY REMINDER
***********************************************************************************************************************/
def filtersHandler(tts){
	log.warn "message received: $tts"
    if(recipients?.size()>0 || sms?.size()>0){        
    	sendtxt(tts)
    }
	if(speechSynth) {
		speechSynth.playTextAndResume(tts)
	}
	else{
		if(sonos) {
			def sCommand = resumePlaying == true ? "playTrackAndResume" : "playTrackAndRestore"
			def sTxt = textToSpeech(tts instanceof List ? tts[0] : tts)
			def sVolume = settings.sonosVolume ?: 20
			sonos."${sCommand}"(sTxt.uri, sTxt.duration, sVolume)
		}
	}
}
/***********************************************************************************************************************
    SMS HANDLER
***********************************************************************************************************************/
private void sendtxt(message) {
	def stamp = state.lastTime = new Date(now()).format("h:mm aa", location.timeZone)
    if (debug) log.debug "Request to send sms received with message: '${message}'"
    if (recipients) { 
        sendNotificationToContacts(message, recipients)
            if (debug) log.debug "Sending sms to selected reipients"
    } 
    else {
    	if (push) {
        	message = timeStamp==true ? message + " at " + stamp : message
    		sendPush message
            	if (debug) log.debug "Sending push message to selected reipients"
        }
    } 
    if (notify) {
        sendNotificationEvent(message)
             	if (debug) log.debug "Sending notification to mobile app"
    }
    if (sms) {
        sendText(sms, message)
	}
}
private void sendText(number, message) {
    if (sms) {
        def phones = sms.split("\\,")
        for (phone in phones) {
            sendSms(phone, message)
            if (debug) log.debug "Sending sms to selected phones"
        }
    }
}

def pFiltersSettings() {def result = ""
    if (cFilterReplacement || cFilterContact || speechSynth || sonos || recipients || sms) {
    	result = "complete"}
    	result}
def pFiltersComplete() {def text = "Tap here to Configure" 
    if (pFiltersSettings()== "complete") {
    	text = "Configured"}
        else text = "Tap here to Configure"
        text
        }