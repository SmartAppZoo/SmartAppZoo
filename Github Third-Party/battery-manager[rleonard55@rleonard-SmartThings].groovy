/**
 *  Battery Monitor
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
    name: "Battery Manager",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Monitors battery levels and allows you to record the battery types, count & warning % for each device. Optionally warns you with that info if it gets to low",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png")

preferences {
	 page(name: "mainPage")
     page(name: "batteryPage")
     page(name: "finalPage")
}

def mainPage(){
    dynamicPage(name: "mainPage", title: "", install: false, uninstall: true, nextPage:"batteryPage") {
        section("Monitor data") {
            input name: "batteryDevices", title: "Battery Devices", type: "capability.battery", required: true, multiple: true, submitOnChange:true
			input name: "warningPercent", title: "Default Warning Percent", type: "number", range: "1..100", defaultValue: 30
        }
	}
}
def batteryPage(){
	 dynamicPage(name: "batteryPage", install: false, uninstall: true, nextPage:"finalPage") {
         (batteryDevices.sort{a,b -> a.displayName <=> b.displayName}).each {d->
             batteryDetailSection(d)
         }
     }
}
def finalPage(){
	dynamicPage(name: "finalPage", install: true, uninstall: true) {
    	section(""){
            input name: "checkTime", title: "Check Time", type: "time", required: true
            //input name: "pushMessage", title: "Send push notifications?", type: "bool", defaultValue: true
        }
        dayOfWeekSection()
    	//nameModeSection()
        notificationSection()
    }
}

def batteryDetailSection(device){
	def per = device.currentBattery
    if(per == null || per =="null"||per=="") per = "?"
    
	 section("${device.displayName} | Currently: ${per}%") {
     	def percent = settings."${device.id}warningPercent"
        def battery = settings."${device.id}batteryType"
        def count = settings."${device.id}batteryCount"
        if(count == null)
        	count =1
        
        if(percent == null) percent = warningPercent
			input name: "${device.id}batteryCount", title: "Battery Count", type: "number",range: "1..8", required: true, defaultValue:count
            input name: "${device.id}batteryType", title: "Battery Type", type: "text", required: false,defaultValue:battery
			input name: "${device.id}warningPercent", title: "Warning Percent", type: "number", range: "1..100", defaultValue:percent
        }
}
def dayOfWeekSection(prompt= "Only on these day(s)", itemRequired= false, multiple= true) {
	section() {
    	input "daysToRun", "enum", title: prompt, options: ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday", "Saturday"], submitOnChange: true, required: itemRequired, multiple: multiple
	}
}
def nameModeSection() {
    section([mobileOnly:true]) {
        label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
    }
}
def notificationSection(prompt= "Send Notifications?"){
    section(prompt) {
    	input name: "pushMessage", title: "Send ST's push notifications?", type: "bool", defaultValue: true
        input("recipients", "contact", title: "Send additional notifications to", required: false) {
            input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}
def initialize() {
	schedule(checkTime, updateStatus)
	// TODO: subscribe to attributes, devices, locations, etc.
}

def updateStatus() {
	if(!dayToRun()) return
	batteryDevices.each(){
    	
        def percent = settings."${it.id}warningPercent"
        def batteryType = settings."${it.id}batteryType"
        def batteryCount = settings."${it.id}batteryCount"
        
        def BatteryTypeMsg = ", make sure you have at least [${batteryCount}] ${batteryType} batterie(s) on hand."
        if(batteryType == null) BatteryTypeMsg = ""
        
    	if(it.currentBattery == null) 
        	sendNotifications("${it.displayName} isn't reporting it's battery level"+BatteryTypeMsg)
        else if(it.currentBattery < percent) 
			sendNotifications("${it.displayName} battery is at ${it.currentBattery}%"+BatteryTypeMsg)
    }
}
private sendNotifications(msg){
	if (location.contactBookEnabled && recipients) {
        log.debug "Contact Book enabled!"
        sendNotificationToContacts(msg, recipients)
    } else {
        log.debug "Contact Book not enabled"
        if (phone) {
            sendSms(phone, msg)
        }
    }
    
	if (pushMessage) {
        sendPush(msg)
    } else {
        sendNotificationEvent(msg)
    }
}
private dayToRun() {
	log.trace "Entering 'dayToRun'"
	//▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
    
    log.debug("Today is ${(new Date().format("EEEE"))}")
    
    if(daysToRun == null) return true;
	return daysToRun.contains(new Date().format("EEEE"))
    
	//▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲  
	log.trace "Exiting 'dayToRun'"
}