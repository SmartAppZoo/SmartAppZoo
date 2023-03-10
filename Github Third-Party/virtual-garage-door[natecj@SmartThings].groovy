/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
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

/**
 *  Original Author: LGKahn kahn-st@lgk.com
 *  Source: http://mail.lgk.com/lgkvirtualgaragedoorsmartappv2.txt
 *  version 2 user defineable timeout before checking if door opened or closed correctly. Raised default to 25 secs. You can reduce it to 15 secs. if you have custom simulated door with < 6 sec wait.
 *
 */

definition(
  name: "Virtual Garage Door",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Sync a Virtual Garage Door device with a Tilt/Contact Sensor and a Switch/Relay for single-device control of a garage door",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the opener device..."){
		input "opener", "capability.switch", title: "Garage Door Opener", required: true
	}

	section("Choose the sensor device..."){
		input "sensor", "capability.contactSensor", title: "Garage Door Sensor", required: true
	}

	section("Choose the virtual device..."){
		input "virtual", "capability.doorControl", title: "Virtual Garage Door", required: true
	}

  section("Timeout before checking if the door opened/closed correctly?"){
		input "checkTimeout", "number", title: "Seconds:", required: true, defaultValue: 25
	}
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  subscribe(virtual, "open", virtualOpenHandler, [filterEvents: false])
  subscribe(virtual, "close", virtualCloseHandler, [filterEvents: false])

  subscribe(openor, "push", openerHandler, [filterEvents: false])
  subscribe(openor, "on", openerHandler, [filterEvents: false])

  syncVirtual()
}

def openerHandler(evt) {
  if (sensor.currentContact == "open") {
    virtual.close()
    runIn(checkTimeout, syncVirtual)
  } else if (sensor.currentContact == "closed") {
    virtual.open()
    runIn(checkTimeout, syncVirtual)
  }
}

def syncVirtual() {
  if (sensor.currentContact != virtual.currentContact) {
    if (sensor.currentContact == "closed") {
      virtual.finishClosing()
    } else if (sensor.currentContact == "open") {
      virtual.finishOpening()
    }
  }
}

def virtualOpenHandler(evt) {
  if (sensor.currentContact == "closed") {
    opener.on()
    runIn(checkTimeout, syncPhysical)
  }
}

def virtualCloseHandler(evt) {
  if (sensor.currentContact == "open") {
    opener.on()
    runIn(checkTimeout, syncPhysical)
  }
}

def syncPhysical() {
  if (sensor.currentContact != virtual.currentContact) {
    if (virtual.currentContact == "closed") {
      sensor.close()
    } else if (virtual.currentContact == "open") {
      sensor.open()
    }
  }
}
