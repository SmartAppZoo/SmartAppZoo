/**
 *  Test Web Service
 *
 *  Copyright 2019 Alex Minutillo
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
    name: "Test Web Service",
    namespace: "minutillo",
    author: "Alex Minutillo",
    description: "Test Web Service",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section ("Allow external service to control these things...") {
    input "switches", "capability.switch", multiple: true, required: true
  }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
	def startDate = new Date() - 3	// 3 days ago
	def endDate = new Date()	// today

    def resp = []
    switches.each {
        resp << [name: it.displayName, value: it.currentValue("switch"), events: it.eventsBetween(startDate, endDate).toString()]
    }
    return resp
}

def getEventsOfDevice(device) {
	def events = []
    def today = new Date()
	def then = timeToday(today.format("HH:mm"), TimeZone.getTimeZone('UTC')) - 1
	events << device.eventsBetween(then, today, [max: 200])?.findAll{"$it.source" == "DEVICE"}?.collect{[description: it.description, descriptionText: it.descriptionText, displayName: it.displayName, date: it.date, name: it.name, unit: it.unit, source: it.source, value: it.value]}
	return events
}

def installed() {}

def updated() {}