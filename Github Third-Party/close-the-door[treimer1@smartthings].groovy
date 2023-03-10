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
 *  Close the Door
 *
 *  Author: SmartThings
 *
 *  Close a door at a specified time.
 */

definition(
    name: "Close the Door",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Close a door at a specified time.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Select door to control...") {
		input name: "door", type: "capability.doorControl"
	}
	section("Close it at...") {
		input name: "closeTime", title: "Close Time?", type: "time"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(closeTime, "closeTimerCallback")

}

def updated(settings) {
	unschedule()
	schedule(closeTime, "closeTimerCallback")
}

def closeTimerCallback() {
	log.debug "Closing door"
	door.close()
}