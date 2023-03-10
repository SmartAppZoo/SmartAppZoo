/**
 *  webservice app
 *
 *  Copyright 2016 Samar Acharya
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
    name: "webservice app",
    namespace: "com.techgaun.wsapp",
    author: "Samar Acharya",
    description: "simple webservice app",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Awesome Samar App", displayLink: "https://github.com/techgaun"])


preferences {
	section("Allow external service to control these things?") {
		input "locks", "capability.lock", multiple: true, required: true
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
    subscribe(locks, "lock", lockHandler)
}

def lockHandler(evt) {
	log.debug "got event with ${evt.value}"
}

mappings {
	path("/locks") {
    	action: [
        	GET: "listLocks"
        ]
    }
    path("/locks/:command") {
    	action: [
        	PUT: "updateLocks"
        ]
    }
}

def listLocks() {
	def resp = []
    locks.each {
    	resp << [name: it.displayName, value: it.currentValue("lock")]
    }
    return resp
}
def updateLocks() {
	log.debug "obtained ${params.command}"
	def cmd = params.command
    switch (cmd) {
    	case "on":
        	locks.lock()
            break
        case "off":
        	locks.unlock()
            break
        default:
        	httpError(400, "$cmd is not a valid command")
    }
}
