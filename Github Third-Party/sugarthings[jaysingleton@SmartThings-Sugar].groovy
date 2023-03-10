/**
 *  SugarThings
 *
 *  Copyright 2019 Mohammed Adenwala
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
    name: "SugarThings",
    namespace: "madenwala",
    author: "Mohammed Adenwala",
    description: "SugarThings for diabetics retrieves your Dexcom CGM readings from Sugarmate.io and allows audio notifications based on your blood sugar levels.",
    category: "My Apps",
    iconUrl: "https://sugarmate.io/assets/sugarmate-bf45d10bb97dfbe9587c0af8efc3e048ec35f7414d125b2af9f3132fd0e363a4.png",
    iconX2Url: "https://sugarmate.io/assets/sugarmate-bf45d10bb97dfbe9587c0af8efc3e048ec35f7414d125b2af9f3132fd0e363a4.png",
    iconX3Url: "https://sugarmate.io/assets/sugarmate-bf45d10bb97dfbe9587c0af8efc3e048ec35f7414d125b2af9f3132fd0e363a4.png")

import groovy.time.TimeCategory;
import groovy.time.*;

preferences {
    section("CGM Data") {
        href(name: "websiteSugarmate",
             title: "Create Sugarmate Account",
             required: false,
             style: "external",
             url: "https://sugarmate.io",
             description: "CGM data is made available from Sugarmate.io, a platform that retrieves your CGM data from Dexcom. You will need a Sugarmate account to run this SmartApp."
             )  
        
        href(name: "jsonWebsite",
             title: "Sugarmate Website",
             required: false,
             style: "external",
             url: "https://sugarmate.io/home/settings",
             description: "Retrieve your personal Sugarmate External JSON URL by logging into your Sugarmate account. Once authenticated, go to Menu > Settings and scroll to the External JSON section and copy and paste your URL below."
             )

        input "jsonUrl", "text", required: true, title: "Sugarmate External Json URL"
    }
    section("Personalization") {
    	paragraph "The name of the person will be announced during audio notifications."
        input "personName", "text", required: true, title: "Name"
    }
    section("Automations") {
    	paragraph "Set to ON if you need to mute all audio."
        input "isMuted", "boolean", title: "Mute"
        input "isPaused", "boolean", title: "Pause Automations"
        input "enableNotifications", "boolean", title: "Send push notifications"
    }
    section("Audio Devices") {
    	paragraph "Audio notifications will play on these devices. You can choose which modes this app works at the bottom of this page."
        input "audioSpeakers", "capability.audioNotification", title: "Audio Devices", multiple: true, required: false
        input "alexaSpeakers", "device.echoSpeaksDevice", title: "Alexa Devices", multiple: true, required: false
    }
    section("Audio Notification for NO DATA") {
    	paragraph "When there is no data from your CGM, meaning that CGM data is not being shared to Sugarmate, then we can announce that there is no data."
    	input "skipNoDataRefresh", "number", title: "Minutes to wait between notification", description: "hello world", range: "5..120", defaultValue: 5
    }
    section("Audio Notification for URGENT-HIGH") {
    	paragraph "CGM data is above your expected range, an announcement will be made."
        input "thresholdTooHigh", "number", title: "Set the level at which you don't want to be above", range: "150..400", defaultValue: 200
    	input "skipTooHighRefresh", "number", title: "Minutes to wait between notifications", range: "5..60", defaultValue: 5
    }
    section("Audio Notification for URGENT-LOW") {
    	paragraph "CGM data is below where it should be, an announcement will be made."
        input "thresholdTooLow", "number", title: "Set the level at which you have symptoms of low blood sugar", range: "40..150", defaultValue: 70
    	input "skipTooLowRefresh", "number", title: "Minutes to wait between notifications", range: "5..60", defaultValue: 5
    }
    section("Audio Notification for SINGLE-ARROW DOWN") {
    	paragraph "When CGM data is single-arrow down below the specified range, an annoucement will be made indicating that the CGM is falling."
        input "thresholdSingleDown", "number", title: "CGM level below", range: "0..400", defaultValue: 100
    	input "skipSingleDownRefresh", "number", title: "Minutes to wait between notifications", range: "5..60", defaultValue: 10
    }
    section("Audio Notification for DOUBLE-ARROW DOWN") {
    	paragraph "When CGM data is double-arrow down below the specified range, an annoucement will be made indicating that the CGM is falling too fast."
    	input "thresholdDoubleDown", "number", title: "CGM level below", range: "0..400", defaultValue: 150
    	input "skipDoubleDownRefresh", "number", title: "Minutes to wait until next notification", range: "5..60", defaultValue: 5
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

def initialize() {
	state.OLD_MESSAGE = "[OLD]"
	state.forceMessage = false
    state.nextMessageDate = new Date() -1;
    state.noDataCount = 0;
    state.doubleDownCount = 0;
    state.singleDownCount = 0;
    state.tooLowCount = 0;
    state.tooHighCount = 0;
    subscribe(app, appHandler)
    runEvery1Minute(refreshData)
}

def appHandler(evt) {
	try {
        log.debug "SugarThings: App Event ${evt.value} received"
    	def data = getData()
    	def message = getMessage(data)
        if(message == null)
        	message = getDefaultMessage(data, true)
    	audioSpeak(message)
    }
    catch(ex) {
    	log.error "SugarThings: Could not execute App Touch event: " + ex
    }
}

def refreshData(){
	//try {
        if(isPaused.equals(true)) {
            // Do nothing while paused
            log.debug "SugarThings: Paused"
        } else {
            // Check how old data is and then if old, get data
            Date nowDate = new Date()
            Date refreshDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss", state.nextMessageDate)
            if(refreshDate <= nowDate) {
                log.info "SugarThings: Refresh data..."   
                def data = getData()
				updateNextReadingTime(data)
                def message = getMessage(data)
                audioSpeak(message)
            } else {
                double totalSeconds = (refreshDate.time - nowDate.time) / 1000
                log.debug "SugarThings: No refresh data for another ${totalSeconds.round(0)} second(s): Next: ${refreshDate} Current: ${nowDate}"
            }
        }
        /*
    }
    catch(ex) {
    	log.error "SugarThings: Could not refresh data: " + ex;
    }
    */
}

def getData() {

    log.debug "SugarThings: Refresh Data from ${jsonUrl}"

    def params = [
        uri: jsonUrl,
        contentType: 'application/json'
    ]
    
	httpGet(params) { resp ->
        if(resp.status == 200){
            // get the data from the response body
            log.debug "SugarThings: Data: ${resp.data}"
            return resp.data;
        } else {
            // get the status code of the response
            log.debug "SugarThings: Status Code: ${resp.status}"
            return null;
        }  
	}
}

def getMessage(data) {
	
	if(data == null)
    	return "No data from Sugarmate";
    String message = null;
    
    if(data.reading.contains(state.OLD_MESSAGE)) {        
    	state.forceMessage = true
        state.noDataCount = state.noDataCount + 1
        log.debug "noDataCount: " + state.noDataCount
    	double dataMod = skipNoDataRefresh / 5
        if(state.noDataCount % dataMod.round(0) == 0) 
        	message = getDefaultMessage(data, false)
        //else 
        	//message = "";
    } else {
        if(state.forceMessage && state.noDataCount > 0) {
        	state.forceMessage = false;
    		message = "Data restored. " + getDefaultMessage(data, false);
        }
    	state.noDataCount = 0;
    }

	if(data.trend_words == "DOUBLE_DOWN" && data.value <= thresholdDoubleDown) {
    	state.forceMessage = true;
        state.doubleDownCount = state.doubleDownCount + 1
        log.debug "doubleDownCount: " + state.doubleDownCount
    	double dataMod = skipDoubleDownRefresh / 5
        
        if(state.doubleDownCount % dataMod.round(0) == 0) {
        	if(data.reading.contains(state.OLD_MESSAGE)) 
            	message = "OLD "
            message = "DOUBLE-ARROW DOWN ALERT! " + getDefaultMessage(data, true);
        }
    } else {
        state.doubleDownCount = 0;
    }
    
    if(data.trend_words == "SINGLE_DOWN" && data.value <= thresholdSingleDown) {
    	state.forceMessage = true;
        state.singleDownCount = state.singleDownCount + 1;
        log.debug "singleDownCount: " + state.singleDownCount;
    	double dataMod = skipSingleDownRefresh / 5
        if(state.singleDownCount % dataMod.round(0) == 0) {
            if(data.reading.contains(state.OLD_MESSAGE)) 
            	message = "OLD "
            message = "SINGLE-ARROW DOWN ALERT! " + getDefaultMessage(data, true);
        }
    } else {
    	state.singleDownCount = 0;
    }
    
    if(data.value <= thresholdTooLow && data.delta < 0) {
    	state.forceMessage = true;
        state.tooLowCount = state.tooLowCount + 1;
        log.debug "tooLowCount: " + state.tooLowCount;
    	double dataMod = skipTooLowRefresh / 5
        if(data.reading.contains(state.OLD_MESSAGE) || state.tooLowCount % dataMod.round(0) == 0) {
            if(data.reading.contains(state.OLD_MESSAGE)) 
            	message = "OLD "
            message = "URGENT-LOW! " + getDefaultMessage(data, true);
        }
    } else {
    	state.tooLowCount = 0;
    }
    
    if(data.value >= thresholdTooHigh && data.delta > 0) {
        state.tooHighCount = state.tooHighCount + 1;
        log.debug "tooHighCount: " + state.tooHighCount;
    	double dataMod = skipTooHighRefresh / 5
        if(state.tooHighCount % dataMod.round(0) == 0)
            message = "URGENT-HIGH! " + getDefaultMessage(data, true);
    } else {
    	state.tooHighCount = 0;
    }
    
    if(message == null && state.forceMessage) {
    	state.forceMessage = false;
        log.debug "ForceMessage is true. Message is: " + message;
    	message = getDefaultMessage(data, true);
    }

    return message;
}

def getDefaultMessage(data, showDelta) {

    def trendWords = [
        "NONE":"",
        "NOT_COMPUTABLE":"",
        "OUT_OF_RANGE":"",
        "DOUBLE_UP":"double-arrow up", 
        "SINGLE_UP":"up", 
        "FORTY_FIVE_UP":"slight up", 
        "FLAT":"steady", 
        "FORTY_FIVE_DOWN":"slight down", 
        "SINGLE_DOWN":"down", 
        "DOUBLE_DOWN":"double-arrow down"
    ];
    
    String message = null;
    if(data.reading.contains(state.OLD_MESSAGE))
    	message = "No data from ${personName} for the last ${convertTimespanToMinutes(data.timestamp)} minutes. Last reading was ${data.value} ${trendWords[data.trend_words]}"
    else {
    	message = "${personName} is ${data.value} ${trendWords[data.trend_words]}";
    
        if(showDelta) {
        	if(data.delta >= 0)
            	message = message + " +${data.delta}"
            else if(data.delta < 0)
            	message = message + " ${data.delta}"
        }

        def minutesAgo = convertTimespanToMinutes(data.timestamp);
        if(minutesAgo >= 2)
            message = message + " from ${minutesAgo} minutes ago";
    }
    
    message = message + "."
        
    return message;
}

def audioSpeak(message) {
    log.info "SugarThings: Message: " + message;
    if(isMuted != "true" && message) {
    	log.debug "SugarThings: Audio Speak: " + message
        log.debug alexaSpeakers[0]
        if(alexaSpeakers) {
        	alexaSpeakers*.playAnnouncement(message)
        }
        audioSpeakers*.playTextAndRestore(message)
    }
    if(enableNotifications == 'true')
    	sendNotification(message)
}

def sendNotification(message){
    if (sendPush && message) {
        sendPush(message)
    }
}

def convertTimespanToMinutes(timestamp) {
    // Parse the data timestamp
    def dataDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss", timestamp);
    def nowDate = new Date();
    // Convert milliseconds to seconds
    double seconds = (nowDate.time - dataDate.time) / 1000;
    // Round sounds down to the minute(s)
    int minutes = (seconds - (seconds % 60)) / 60;
    // Return the minutes
    return minutes;
}

def convertTimespanToSeconds(timestamp) {
    // Parse the data timestamp
    def dataDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss", timestamp)
    def nowDate = new Date()
    // Convert milliseconds to seconds
    double seconds = (nowDate.time - dataDate.time) / 1000
    return seconds.round(0)
}

def updateNextReadingTime(data) {
    use(TimeCategory) {
        int readingTimeGap = 315
        int secs = convertTimespanToSeconds(data.timestamp)
        secs = (secs - (secs % readingTimeGap)) + readingTimeGap
        state.nextMessageDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss", data.timestamp) + secs.seconds
        log.debug "SugarThings: NEXT REFRESH: ${state.nextMessageDate}  CURRENT: ${nowDate}"
    } 
}