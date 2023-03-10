/**
 *  Take Photo When
 *
 *  Copyright 2019 Indu Prakash
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Take Photo When",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Take photo when something happens",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/photo-burst-when@2x.png"
)

preferences {
  section("When") {
		input "contactSensors", "capability.contactSensor", title: "Contact is open", required: false, multiple: true
		input "motionSensors", "capability.motionSensor", title: "Motion is sensed", required: false, multiple: true
		input "cameras", "capability.imageCapture", title: "Take pictures on", required: false, multiple: true
		input "delayTakePicture", "number", title: "After (seconds)", required: false, defaultValue: 3, range: "0..20"
		input "appEnabled", "bool", title: "Enabled", defaultValue:false
	}
}

def installed() {
	log.debug "Installed: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	if (appEnabled) {
		subscribe(contactSensors, "contact.open", takePicture)
		subscribe(motionSensors, "motion.active", takePicture)	
	}
}

def takePicture(evt) {
	def msg = "${evt.displayName} ${evt.stringValue} at ${ getFormattedTime(evt.date) }"
	log.debug "takePicture after $delayTakePicture seconds: $msg"
	
	//Only serializable data can be passed as parameter to runIn, data is special
	runIn(delayTakePicture, takePictureDelayed, [data: [msg:msg]])
}

def takePictureDelayed(data) {
	log.trace "takePictureDelayed: ${data.msg}"
	cameras.each { cam ->        
		if (cam.hasCommand("takePicture")) {
			cam.takePicture(data.msg)
		}
		else {
			cam.take()
		}
	}
}

private String getFormattedTime(Date dt) {
	if (!dt) {
		return ""
	}
	def tz = location.getTimeZone()
	if (!tz) {
		tz = TimeZone.getTimeZone("CST")
	}
	return dt.format('h:mm a', tz)
}