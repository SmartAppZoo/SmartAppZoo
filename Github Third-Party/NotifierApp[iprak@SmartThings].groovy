/**
 *  Notifier
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
	name: "Notifier",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Display notification in SmartThings mobile app from device.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-NotifyWhenNotHere.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-NotifyWhenNotHere@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-NotifyWhenNotHere@2x.png")

preferences {
	section("Devices") {
		input "sources", "capability.sensor", title: "Devices to monitor?", required: true, multiple: true
	}
	section("Operation") {
		input "sendMsg", "boolean", title: "Send notification?", defaultValue: true, displayDuringSetup: true
	}
}

//Public
def installed() {
	initialize()
}
def updated() {
	unsubscribe()
	initialize()
}
private initialize() {
	//subscribe(sources, "notify", notificationHandler)
	sources.each { source ->
		if (source.hasAttribute("notify")) {
			log.info "Subscribed ${source}"
			subscribe(source, "notify", notificationHandler)
		}
		else {
			log.warn("Unsupported device ${source}, it does not have 'notify' attribute.")
		}
	}
}

/**
 * 'notify' event handler.
 */
def notificationHandler(evt) {
	log.info "Received ${evt.displayName} ${evt.value}"
	if (sendMsg && evt.value) {
		//sendNotification() method allows you to send both push and/or SMS messages, in one convenient method call. It can also optionally display the message in the Notifications feed.
		sendNotification("${evt.displayName}: ${evt.value}")
	}
}
