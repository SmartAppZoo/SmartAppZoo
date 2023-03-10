/**
 *  Washer/Dryer Finished Alert
 *
 *  Copyright 2015 Joe Angell
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
    name: "Washer and Dryer Finished Alert",
    namespace: "",
    author: "Joe Angell",
    description: "Sends a notification when the washer or dryer have finished.  Linked to my custom ThingShield.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances1-icn@2x.png")

preferences {
	section("Title") {
		input "wdmon", "device.WasherAndDryerFinishedMonitor"
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
	subscribe( wdmon, "washer", handleWasher )
    subscribe( wdmon, "dryer",  handleDryer  )
}

def handleWasher(evt) {
	log.debug "washer monitor reports done"
    if (wdmon.currentValue("washer") == "done") {
 		sendPush( "Washer is done!" )
	}
}

def handleDryer(evt) {
	log.debug "dyer monitor reports done"
    if (wdmon.currentValue("dryer") == "done") {
 		sendPush( "Dryer is done!" )
	}
}