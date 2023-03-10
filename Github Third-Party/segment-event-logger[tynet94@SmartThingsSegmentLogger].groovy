/**
 *  Segment Event Logger
 *
 *  Copyright 2022 Ty Alexander
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
    name: "Segment Event Logger",
    namespace: "tynet94",
    author: "Ty Alexander",
    description: "Segment Event Logger",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section ("Segment Write Key") {
    input "segmentWriteKey", "text", title: "Segment Write Key", required: true
    }
    section("Log Switches:") {
      input "switches", "capability.switch", multiple: true, required: false, title: "Forward it Segment for switches"
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
    subscribe(switches, "switch", eventHandler)
}

def eventHandler(evt) {
    def evtId = "${evt.id}"
    def payload = [
      userId: evt.deviceId,
      event: "SmartThings Event",
      context: [
        app: [
          name: "SmartThingsSegmentLogger",
          version: "0.1.0",
        ] // app
      ], // context
      properties: [
        date: evt.date,
        isoDate: evt.isoDate,
        id: evt.id,
        eventId: evtId,
        name: evt.name,
        displayName: evt.displayName,
        hub: evt.hubId,
        locationName: evt.location.name,
        value: evt.value,
      ] // properties
   ] // payload

   def params = [
     uri: "https://api.segment.io",
     path: "/v1/track",
     headers: ["Authorization": " Basic ${segmentWriteKey}"],
     body: payload
   ]

   try {
     httpPostJson(params) { resp ->
       log.debug "Logged event to Segment (${resp.status})\n${params}"
     }
   } catch (e) {
    log.error "Failed to log event to Segment: $e"
   }
}
