/**
 *  Close Garage After Sunset
 *
 *  Copyright 2015 Sean Mooney
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
    name: "Close Garage After Sunset",
    namespace: "srmooney",
    author: "Sean Mooney",
    description: "Close a garage door that was left open after dark",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png")


preferences {
	section("Close if garage door is open...") {
		input "doorSensor", "capability.contactSensor", title: "Which Sensor?"
		input "doorSwitch", "capability.momentary", title: "Which Door?"
	}
	section ("Sunset offset (optional)...") {
        input "sunsetOffsetValue", "number", title: "Minutes", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
	}
    section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", required: false
	}
	//section( "Notifications" ) {
	//	input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
	//	input "message", "text", title: "Message to send...", required: false
	//}
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	log.debug "The time zone for this location is: $location.timeZone"
    subscribe(location, "sunsetTime", sunsetTimeHandler)
    //schedule it to run today too
    log.info "location: ${location}"
    scheduleCloseGarage(location.currentValue("sunsetTime"))
}

def sunsetTimeHandler(evt) {
    log.debug "sunset event $evt"
    //when I find out the sunset time, schedule the Garage Door check with an offset
    scheduleCloseGarage(evt.date)
    send("Sunset: ${evt.date}");
}

def scheduleCloseGarage(sunsetString) {
	//log.info "doorStatus: ${doorSensor.contactState.value}"
	def dateFormat = "yyyy-MM-dd hh:mm:ss z"
    
    log.info "sunsetString: ${sunsetString.format(dateFormat, location.timeZone)}"
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)
    
    //calculate the offset
    //def offsetMinutes = (sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : 0)
    def offsetMinutes = (sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? (sunsetOffsetValue * -1) :  sunsetOffsetValue) : 0)
    
    log.debug "Offset Minutes: $offsetMinutes"
    def sunsetOffset = new Date(sunsetTime.time + (offsetMinutes * 60 * 1000))
          
   	// def sunsetOffset = sunsetOffsetDate.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.timeZone)
    //def sunsetOffset = sunsetOffsetDate
    
    //schedule this to run one time
    log.info "Scheduling for: ${sunsetOffset.format(dateFormat, location.timeZone)} (sunset is ${sunsetTime.format(dateFormat, location.timeZone)})"
    runOnce(sunsetOffset, closeGarage)
    
    //Subtract 24hrs and if it's still later than now set as new time
    use(groovy.time.TimeCategory) {
        def test = sunsetOffset - 24.hours
        log.info("24Hrs Before: ${test.format(dateFormat, location.timeZone)}")
        def isBefore = !test.before(new Date())
        log.info("isBefore: $isBefore")
        if (isBefore == true) {
        	log.info "Scheduling for: ${test.format(dateFormat, location.timeZone)} (sunset is ${sunsetTime.format(dateFormat, location.timeZone)})"
        	runOnce(test, closeGarage)
        }
    }
    
}

def closeGarage() {
	log.info "closeGarage"
    //def Gdoor = checkGarage()
    def doorStatus = doorSensor.contactState.value
    log.trace "closeGarage: $doorStatus"
    sendPushMessage("closeGarage: $doorStatus")

    if (doorStatus == "open") {
       	send(message)
        doorSwitch.push()
    }
    //updated();
}
/*
def checkGarage(evt) {
	def latestValue = contact1.currentContact
}
*/
private send(msg) {
	//if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( message )
	//}

	log.debug message
}

