/**
 *  Traffic Report
 *
 *  Copyright 2014 Brian Critchlow
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
 *
 *  --This SmartApp has the intention of notifying you of traffic conditions on your Hue bulbs and alerting you of departure time
 *  --based on that traffic. The app will request two locations, the expected time of arrival, and when to start polling for traffic.
 *  --It will also allow you to set the thresholds for traffic and what colors to change the Hue to.
 *
 *  --Special thanks to scottinpollock for code examples
 *
 *
 *  if realTime > time
 *  if (arrivalTime - realTime) >= now
 */
import groovy.time.*


definition(
    name: "Traffic Report",
    namespace: "docwisdom",
    author: "Brian Critchlow",
    description: "notifies of traffic conditions by Hue color and flashes when you should leave based on set arrival time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/docwisdom-smartthings/Transport-traffic-jam-icon.png",
    iconX2Url: "https://s3.amazonaws.com/docwisdom-smartthings/Transport-traffic-jam-icon.png")


preferences {
	//what is the departure location?
	section("Departing From:"){
		input "departFrom", "text", title: "Address?"
	}
    //what is the destination location?
	section("Arriving At:"){
		input "arriveAt", "text", title: "Address?"
	}
    //what time do you need to arrive?
	section("Expected Arrival Time:"){
		input "arrivalTime", "time", title: "When?"
	}
    // //what time should I begin checking traffic?
	section("Begin Checking At:"){
		input "checkTime", "time", title: "When?"
	}
    //some traffic threshold in minutes
	section("Trigger MODERATE TRAFFIC if commute increases this many minutes:") {
		input "threshold2", "number", title: "Minutes?"
	}
    //bad traffic threshold in minutes
	section("Trigger BAD TRAFFIC if commute increases by this many minutes:") {
		input "threshold3", "number", title: "Minutes?"
	}
}

//schedule upon install
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

//reschedule upon update
def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	def checkHour = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", checkTime).format('H', TimeZone.getTimeZone('PST'))
    def checkMinute = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", checkTime).format('m', TimeZone.getTimeZone('PST'))
    log.debug checkHour
    log.debug checkMinute
	schedule("0 ${checkMinute} ${checkHour} * * ?", "checkTimeHandler")
}

//check the time is between the desired check points
def checkTimeHandler(){
	if (now() > timeToday(checkTime).time && now() < timeToday(arrivalTime).time){
    	log.debug "Its time"
        runIn(60, checkTrafficHandler)
    }
    else {
    	log.debug "Its not time anymore"
        initialize()
    }
}


//handles the traffic API call from Mapquest and calcualtes traffic time
def checkTrafficHandler() {

	def tz = TimeZone.getTimeZone('PST')
	def formattedNow = new Date().format("HH:mm:ss", tz)
    def todayFormatted = new Date().format("MM/dd/yyyy")
    def arrivalTimeFormatted = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSX", arrivalTime).format('HH:mm', tz)
    def departFromFormatted = URLEncoder.encode(departFrom.toString(), "UTF-8")
    def arriveToFormatted = URLEncoder.encode(arriveAt.toString(), "UTF-8");

    log.debug "The time right now is ${formattedNow}"
    log.debug "Todays date is ${todayFormatted}"
    log.debug "Requested Arrival Time is ${arrivalTimeFormatted}"
    log.debug "The departure location is ${departFromFormatted}"
    log.debug "The arrival location is ${arriveToFormatted}"

    log.debug "formatted variables are ${departFromFormatted} ${arriveToFormatted} ${todayFormatted} ${arrivalTimeFormatted}"

        httpGet("http://www.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluur20u82u%2Can%3Do5-9ay506&from=${departFromFormatted}&to=${arriveToFormatted}&narrativeType=none&ambiguities=ignore&routeType=fastest&unit=m&outFormat=json&useTraffic=true&timeType=3&dateType=0&date=${todayFormatted}&localTime=${arrivalTimeFormatted}") {resp ->
        if (resp.data) {
        	//debugEvent ("${resp.data}", true)
            def actualTime = resp.data.route.realTime.floatValue()
            def expectedTime = resp.data.route.time.floatValue()
            log.debug "Actual time ${actualTime} and expected time ${expectedTime}"
            makeTheLightsDo(actualTime, expectedTime)
        }
            if(resp.status == 200) {
            	log.debug "poll results returned"
           	}
         else {
            log.error "polling children & got http status ${resp.status}"
        }
    }
}

def makeTheLightsDo(actualTime, expectedTime) {

 	//if the actual travel time exceeds the expected time plus bad traffic threshold

 	if (actualTime > (expectedTime + (threshold3 * 60))) {
    	log.info "Hues to RED!"
    }
    //if the actual travel time exceeds the expected time plus some traffic threshold
    else if (actualTime > (expectedTime + (threshold2 * 60))) {
    	log.info "Hues to YELLOW!"
    }
    //if there is no traffic
    else if (actualTime <= (expectedTime + (threshold2 * 60)) && actualTime >= 0) {
    	log.info "Hues to GREEN!"
    }
    else if (actualTime < 0) {
    	log.info "Its past time to leave!"
    }
    else {
    	log.debug "Something Broke"
    }

checkTimeHandler()
}

//blink the lights when its time to leave
def checkForBlink() {
 if ((hhmmss_to_seconds(arrivalTimeFormatted) - actualTime) > hhmmss_to_seconds(now())) {
 	log.debug "woah thats some math!"
 }

}

def seconds_to_hhmmss(sec) {
    new GregorianCalendar(0, 0, 0, 0, 0, sec, 0).time.format('HH:mm:ss')
}
def hhmmss_to_seconds(hhmmss) {
    (Date.parse('HH:mm:ss', hhmmss).time - Date.parse('HH:mm:ss', '00:00:00').time) / 1000
}
