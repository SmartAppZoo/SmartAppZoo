/**
 *  Severe Weather Notifier
 *
 *  Copyright 2017 Rob Leonard
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
    name: "Severe Weather Notifier",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Loosely based on the original ST Severe Weather Alert app but also add TTS and other notification options.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png")


preferences {
	page(name: "settingsPage")
}

def settingsPage() {
	dynamicPage(name: "settingsPage", title: "Turn switches off after some minutes", uninstall: true, install: true) {
        section ("Zip code, defaults to location coordinates...") {
            input "MyZipcode", "text", title: "Zip Code", required: true, defaultValue:"${location.zipCode}"
        }
        section ("Weather items you would like notified about...") {
            input "MyWeather", "enum", title: "Which?", options:WeatherItemStrings(), required: true, multiple:true
        }
        section("How would you like to be notified") {
            input "SendPushAlert", "bool", title: "Send Smartthings Push Alerts?", required: true, multiple:false, defalutvalue:true
            input "notifications", "capability.notification", title: "Notify Sources", multiple: true, required: false, hideWhenEmpty:true
            input "Tts","capability.speechSynthesis", title: "Text to speech devices", multiple: true, required: false, hideWhenEmpty:true
            input "Tones","capability.tone", title: "Tone generators", multiple: true, required: false, hideWhenEmpty:true
            //input "joinNotifiers", "device.JoinNotifier", title: "Join Notifiers", multiple:true, required:false, hideWhenEmpty:true
            //input "alarms", "capability.alarm", title:"Alarms", multiple:true, required:false, hideWhenEmpty:true
            //input "silent", "enum", options: ["Yes","No"], title: "Silent alarm only (Yes/No), i.e. strobe", hideWhenEmpty:true
            //input "clear", "number", title:"Active (seconds)", defaultValue:0, hideWhenEmpty:true
        }
        section("Between These Times...") {
        	input "startingX", "enum", title: "Only between these times", options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: false
            if(startingX == "A specific time") 
            	input "starting", "time", title: "Start time", required: true
            else if(startingX == "Sunrise") 
                input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
            else if(startingX == "Sunset") 
            	input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
        	if(startingX != null) {
            	input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], submitOnChange: true, required: true
           		if(endingX == "A specific time") 
            		input "ending", "time", title: "End time", required: true
            	else if(endingX == "Sunrise") 
            		input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0
            	else if (endingX == "Sunset") 
            		input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: true, defaultValue: 0 
        	}
		}
        section ("Also, Text alerts to...") {
            input("recipients", "contact", title: "Send notifications to",required: false) {
                input "phone1", "phone", title: "Phone Number 1", required: false
                input "phone2", "phone", title: "Phone Number 2", required: false
                input "phone3", "phone", title: "Phone Number 3", required: false
            }
        }
        section ("Other settings") {
            input "modes", "mode", title: "Only in mode(s)", multiple: true, required: false }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
	
    unsubscribe()
    unschedule()
	initialize()
    
    // state.alertKeys = null
    // checkForSevereWeather()
}
def initialize() {
	//def sec = Math.round(Math.floor(Math.random() * 60))
	//def min = Math.round(Math.floor(Math.random() * 60))
	//def cron = "$sec $min * * * ?"
	//schedule(cron, "checkForSevereWeather")
    runEvery5Minutes("checkForSevereWeather")
}

private WeatherItem(String type) {
	return weatherMap().find{it.type==name}
}
private WeatherItemString(String type){
	def i = weatherMap().find{it.type==name}
    return "(${i.type}) ${i.value}"
}
private WeatherItemDefaultStrings() {
    def items = []
    def gg=  weatherMap()
    gg.each{itm->if(itm.default)  {items+= "(${itm.type}) ${itm.value}"}}
    
    //weatherMap.each{itm -> items += WeatherItemString("${itm.type}")}
    return items
}

private SelectedWeatherItems() {
	def Selecteditems = []
    settings.MyWeather.each{itm -> Selecteditems += itm.substring(1,4)}
    return Selecteditems
}
private WeatherItemStrings() {
    def items = []
    def gg=  weatherMap()
    gg.each{itm-> items+= "(${itm.type}) ${itm.value}"}
    
    //weatherMap.each{itm -> items += WeatherItemString("${itm.type}")}
    return items
}
private weatherMap() {
	 def map = null
     map = [
     		[type: "HUR", value: "Hurricane Local Statement", default: true],
           	[type: "TOR", value: "Tornado Warning", default: true],
           	[type: "TOW", value: "Tornado Watch", default: false],
            [type: "WRN", value: "Severe Thunderstorm Warning", default: true],
            [type: "SEW", value: "Severe Thunderstorm Watch", default: false],
            [type: "WIN", value: "Winter Weather Advisory", default: false],
            [type: "FLO", value: "Flood Warning", default: true],
            [type: "WAT", value: "Flood Watch / Statement", default: false],
            [type: "WND", value: "High Wind Advisory", default: false],
            [type: "SVR", value: "Severe Weather Statement", default: true],
            [type: "HEA", value: "Heat Advisory", default: false],
            [type: "FOG", value: "Dense Fog Advisory", default: false],
            [type: "SPE", value: "Special Weather Statement", default: false],
            [type: "FIR", value: "Fire Weather Advisory", default: false],
            [type: "SPE", value: "Special Weather Statement", default: false],
            [type: "VOL", value: "Volcanic Activity Statement", default: true],
            [type: "HWW", value: "Hurricane Wind Warning", default: true],
            [type: "REC", value: "Record Set", default: false],
            [type: "REP", value: "Public Reports", default: false],
            [type: "PUB", value: "Public Information Statement", default: false]
           ]
}

private checkForSevereWeather() {
	log.info "Entered 'checkForSevereWeather'"
	def alerts
	if(locationIsDefined()) {
		if(zipcodeIsValid()) {
			alerts = getWeatherFeature("alerts", MyZipcode)?.alerts
		} else {
			log.warn "Severe Weather Alert: Invalid zipcode entered, defaulting to location's zipcode"
			alerts = getWeatherFeature("alerts")?.alerts
		}
	}
    else {
		log.warn "Severe Weather Alert: Location is not defined"
	}

	def newKeys = alerts?.collect{it.type + it.date_epoch} ?: []
	log.debug "Severe Weather Alert: newKeys: $newKeys"

	def oldKeys = state.alertKeys ?: []
	log.debug "Severe Weather Alert: oldKeys: $oldKeys"

	if (newKeys != oldKeys) {
		state.alertKeys = newKeys

		alerts.each {alert ->
            //def msg = "Weather Alert! ${alert.description} from ${alert.date} until ${alert.expires}"
            def msg = "Weather Alert! ${alert.description} until ${alert.expires}"
            if (!oldKeys.contains(alert.type + alert.date_epoch) && SelectedWeatherItems().contains(alert.type))
				send(msg)
		}
	}
}

private locationIsDefined() {
	zipcodeIsValid() || location.zipCode || ( location.latitude && location.longitude )
}
private zipcodeIsValid() {
	MyZipcode && MyZipcode.isNumber() && MyZipcode.size() == 5
}
private modeOk() {
	if(modes == null) return true
	def result = !modes || modes.contains(location.mode)
	return result
}
private timeToRun() {

	log.debug "Running timeToRun"
    def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
	def start = null
	def stop = null
    
    if(startingX =="A specific time" && starting!= null)
    	start = timeToday(starting,location.timeZone)
    if(endingX == "A specific time" && ending!= null)
        stop = timeToday(ending,location.timeZone)
        
    if(startingX == "Sunrise")
    	start = s.sunrise
     if(startingX == "Sunset")
    	start = s.sunset
     if(endingX == "Sunrise")  
      	stop = s.sunrise
     if(endingX == "Sunset")
     	stop = s.sunset
	
    if(start == null || stop == null)
    	return true
    
     if(stop < start) 
     	stop = stop + 1
    
    log.debug "start: ${start} | stop: ${stop}"
    return timeOfDayIsBetween(start, stop, (new Date()), location.timeZone)
}

private send(message) {
    
    if(!modeOk())
    	return
    if(!timeToRun())
    	return
    
	log.debug "Sending Msg: ${message}"
        
    settings.Tones.each{
        	it?.beep() }
            
    settings.notifications.each {
            it?.deviceNotification(message) }
            
    settings.Tts.each{
        	it?.speak(message) }

// Need to workout how this ends
//    settings.alarms.each{
//    	if(it != null) 
//    		it.strobe() }
            
    if (location.contactBookEnabled) {
        	log.debug("sending notifications to: ${recipients?.size()}")
        	sendNotificationToContacts(msg, recipients) }
    else {
        if (settings.phone1)
          sendSms phone1, message
        
        if (settings.phone2) 
          sendSms phone2, message
        
        if (settings.phone3)
          sendSms phone3, message
        
        if(settings.SendPushAlert)
        	sendPush message
        //try { sendPush message }
       // catch (all) { }
      }
}