/**
 *  Expando Test
 *
 *  Copyright 2015 Peter Dunshee
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
	name: "Expando Test",
	namespace: "Petezah",
	author: "Peter Dunshee",
	description: "Expando section demonstration app",
	category: "SmartThings Internal",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "firstPage")
}

def firstPage() {
	dynamicPage(name: "firstPage", title: "Where to first?", install: true, uninstall: true) {
		section(title: "Hideable, hidden", hideable: true, hidden: true) {
			input(type: "enum", name: "enumSegmented", title: "style:segmented", required: false, multiple: true, options: ["one", "two", "three"], style: "segmented")
			input(type: "enum", name: "enum", title: "required:false, multiple:false", required: false, multiple: false, options: ["one", "two", "three"])
		}
		section(title: "Hideable, not hidden", hideable: true, hidden: false) {
			input(type: "password", name: "password", title: "required:false, multiple:false", required: false, multiple: false)
			input(type: "password", name: "passwordRequired", title: "required:true", required: true, multiple: false)
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