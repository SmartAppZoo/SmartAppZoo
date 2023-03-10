/**
 *  Make Sure It's Closed
 *  based upon 'Lock it at a specific time' from Erik Thayer
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
    name: "Make Sure It's Closed",
    namespace: "pkananen",
    author: "Peter Kananen",
    description: "Make sure a door is closed at a specific time.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  section("At this time every day") {
    input "time", "time", title: "Time of Day"
  }

  section("Make sure it's closed") {
    input "contacts", "capability.contactSensor", title: "Which contact sensor?", multiple: true, required: true
  }

  section( "Notifications" ) {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a text message?", required: false
  } 
}

def installed() {
  schedule(time, "setTimeCallback")
}

def updated(settings) {
  unschedule()
  schedule(time, "setTimeCallback")
}

def setTimeCallback() {
  if (contacts) {
    doorOpenCheck()
  }
}

def doorOpenCheck() {
  contacts.each() {
    def currentState = it.contactState
    if (currentState?.value == "open") {
      def msg = "${it.displayName} is open."
      log.info msg
      if (sendPushMessage) {
        sendPush msg
      }
      if (phone) {
        sendSms phone, msg
      }
    }
  }
}
