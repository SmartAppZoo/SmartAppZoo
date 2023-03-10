/**
 *  Big Enum Test
 *
 *  Copyright 2014 Peter Dunshee
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
    name: "Big Enum Test",
    namespace: "Petezah",
    author: "Peter Dunshee",
    description: "Big giant enum test",
    category: "SmartThings Internal",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "inputPage")
}

def inputPage() {
	dynamicPage(name: "inputPage", title: "Every 'input' type") {
		section("enum") {
			input(type: "enum", name: "bigEnum", title: "bigEnum", required: false, multiple: false, options: ["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven"])
			input(type: "enum", name: "biggerEnum", title: "biggerEnum", required: false, multiple: false, options: ["a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k","one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven"])
		}
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
	// TODO: subscribe to attributes, devices, locations, etc.
}