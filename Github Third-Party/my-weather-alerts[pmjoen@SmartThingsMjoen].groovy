/**
 *  My Weather Alerts
 *
 *  Copyright 2018 Jim White
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
    name: "My Weather Alerts",
    namespace: "jwwhite001",
    author: "Jim White",
    description: "Checks Weather Underground For Alerts and Sends Notification",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/sticons/SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/sticons/SevereWeather%402x.png",
    iconX3Url: "https://s3.amazonaws.com/sticons/SevereWeather%402x.png")

import groovy.json.JsonSlurper 

preferences {
        section("Location") {
	          input(name: "longitude", title: "Longitude", description: "https://www.latlong.net/convert-address-to-lat-long.html", required: true)
              input(name: "latitude", title: "Latitude", description: "https://www.latlong.net/convert-address-to-lat-long.html", required: true)
		}
//        section("Notification Types") {
// 		      input(name: "AlertTO", "bool", title: "Tornado Alert", defaultValue: "true", required: false)
// 		      input(name: "AlertIS", "bool", title: "Ice Storm Alert", defaultValue: "true", required: false)
// 		      input(name: "AlertWS", "bool", title: "Winter Storm Alert", defaultValue: "true", required: false)
// 		      input(name: "AlertWW", "bool", title: "Weather Winter Alert", defaultValue: "true", required: false)
// 		      input(name: "AlertZR", "bool", title: "Freezing Rain Alert", defaultValue: "true", required: false)
// 		      input(name: "AlertSV", "bool", title: "Severe Thunderstom Alert", defaultValue: "true", required: false)
//		}
        section("Manual Weather Check") {
 		      input(name: "manlTrigger", title: "Manual Trigger To Repeat Weather Alert", type: "capability.switch", multiple: false, required: false)
		}	
		section("Notification Device") {
	          input(name: "speaker", title: "Speaker", type: "capability.audioNotification", multiple: true, required: true)
		}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	state.prevalert = ""
	state.prevalerts = ["","","",""]
	state.signific = ["","","",""]
	Alerts()
	subscribe(manlTrigger, "switch.on", manlAlerts)
    runEvery15Minutes(Alerts)
}

// handle commands

def parseAlertTime(s) {
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def s2 = s.replaceAll(/([0-9][0-9]):([0-9][0-9])$/,'$1$2')
    dtf.parse(s2)
}

def Alerts(evt)  {
    def alerts = getTwcAlerts("${settings.longitude},${settings.latitude}")
        if (alerts) {
            alerts.eachWithIndex {alert,index ->
                def msg = alert.headlineText
                if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                    msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
                }
                if (alert.expireTimeLocal && !msg.contains(" until ")) {
                    msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
                }
			log.debug "${msg}"
            log.debug "$state.prevalerts"
            def alerttype = alert.phenomena
            def signif = alert.significance
            if (alerttype != state.prevalerts[index] || signif != state.signific[index]) {
       			if (alerttype == "TO" || alerttype == "IS" || alerttype == "WS" || alerttype == "WW" || alerttype == "ZR" || alerttype == "SV") {
                	if (alert.significance == "W" || alert.significance == "A") {
		     	        def msg1 = msg.replaceAll("SUN","SUNDAY")
	    		        def msg2 = msg1.replaceAll("MON","MONDAY")
						def msg3 = msg2.replaceAll("TUE","TUESDAY")
						def msg4 = msg3.replaceAll("WED","WEDNESDAY")
						def msg5 = msg4.replaceAll("THU","THURSDAY")
						def msg6 = msg5.replaceAll("FRI","FRIDAY")
						def msg7 = msg6.replaceAll("SAT","SATURDAY")
    	            	def msg8 = msg7.replaceAll("CST","")
						log.debug "${msg8}"
						sendNotificationEvent("${msg8}")
						speaker.playAnnouncement("wop, wop, wop, wop, wop, ${msg8}")
                   }     
                }    
            }
            state.prevalerts[index] = alerttype
            state.signific[index] = signif
            log.debug "$index, $alerttype, $state.prevalerts"
            }
        }
	manlTrigger.off()
}	

def manlAlerts(evt)  {
    def alerts = getTwcAlerts("${settings.longitude},${settings.latitude}")
        if (alerts) {
            alerts.eachWithIndex {alert,index ->
                def msg = alert.headlineText
                if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                    msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
                }
                if (alert.expireTimeLocal && !msg.contains(" until ")) {
                    msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
                }
            def msg1 = msg.replaceAll("SUN","SUNDAY")
            def msg2 = msg1.replaceAll("MON","MONDAY")
			def msg3 = msg2.replaceAll("TUE","TUESDAY")
			def msg4 = msg3.replaceAll("WED","WEDNESDAY")
			def msg5 = msg4.replaceAll("THU","THURSDAY")
			def msg6 = msg5.replaceAll("FRI","FRIDAY")
			def msg7 = msg6.replaceAll("SAT","SATURDAY")
            def msg8 = msg7.replaceAll("CST","")
			log.debug "${msg8}"
			speaker.playAnnouncement("wop, wop, wop, wop, wop, ${msg8}")
            log.debug "${alert.significance}"
        }    
        }
        else {
            log.debug "No current alerts"
			speaker.playAnnouncement("wop, wop, wop, wop, wop, No Current Weather Alerts")
        }
	manlTrigger.off()
}	

speaker.each {device ->
	if (device.wasLastSpokenToDevice == "repeat weather alert") {
		device.playText("test device")
	}
 }