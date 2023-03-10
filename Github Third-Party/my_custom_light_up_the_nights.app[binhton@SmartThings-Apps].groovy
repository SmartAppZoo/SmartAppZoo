/**
 *  Copyright 2015 SmartThings
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
 *  Light Up The Night
 *
 *  Author: SmartThings
 */
definition(
    name: "My Custom Light Up the Night",
    namespace: "jscgs350",
    author: "SmartThings",
    description: "Turn your lights on when it gets dark and off when it becomes light again.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png"
)

preferences {
	section("Which light sensor?") {
		input "lightSensor", "capability.illuminanceMeasurement"
	}
    section("What LUX level is considered dark?") {
    	input "luxLevel", "number", required: true
    }
	section("Which lights to turn on/off?") {
		input "lights", "capability.switch", multiple: true
	}
    section( "Notifications" ) {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["No", "Yes"]]
        input "phoneNumber", "phone", title: "Send a text message?", required: false
    }
}

def installed() {
	subscribe(lightSensor, "illuminance", illuminanceHandler)
}

def updated() {
	unsubscribe()
	subscribe(lightSensor, "illuminance", illuminanceHandler)
}

def illuminanceHandler(evt) {
    def darkness = settings.luxLevel.toInteger()
    log.debug "Lux level for darkness is $darkness and the sensor sent $evt.integerValue"
	if (evt.integerValue < darkness) {
		send "Lux level for darkness is $darkness and the sensor sent $evt.integerValue, so turning ON the following switches: $lights"
        lights.on()
	}
	else {
		send "Lux level for darkness is $darkness and the sensor sent $evt.integerValue, so turning OFF the following switches: $lights"
        lights.off()
	}
}

private send(msg) {
        if ( sendPushMessage != "No" ) {
            log.debug( "sending push message" )
            sendPush( msg )
        }

        if ( phoneNumber ) {
            log.debug( "sending text message" )
            sendSms( phoneNumber, msg )
        }

        log.debug msg
}
