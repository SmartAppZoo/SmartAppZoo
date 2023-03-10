/**
 *
 * ============================================
 *  Cat Room Ventilation Fan Scheduler
 * ============================================
 *
 *  Daily run times and intervals for cat room ventilation fan.
 *
 *  Copyright (c)2017 Mark Page (mark@very3.net)
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
definition (
  name: "Cat Room Ventilation Fan Scheduler",
  namespace: "cat-room-ventilation-fan-scheduler",
  author: "Mark Page",
  description: "Daily run times and intervals for cat room ventilation fan.",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  section {
    paragraph "Daily run times and intervals for cat room ventilation fan. Starts at the top of hour."
  }
  section {
    input "catfanDevice", "capability.switch", title: "Select device to control cat room ventilation fan:", required: true
  }
  section {
    input "runTime","number",title:"Run time in minutes (1-60):", required: true
  }
}

def installed() {
  initialize()
}

def updated() {
  unschedule()
  unsubscribe()
  initialize()
}

def initialize() {
  def fanStartStr  = "11 0 * * * ?"
  def fanStopStr   = "11 ${runTime} * * * ?"

  schedule("${fanStartStr}", fanOn)
  schedule("${fanStopStr}", fanOff)
  log.debug "CRON INIT (fan): [${fanStartStr}] to [${fanStopStr}]"
}

def fanOn() {
//  sendNotificationEvent("Starting cat room fan.")
  catfanDevice.on()
}

def fanOff() {
//  sendNotificationEvent("Stoping cat room fan.")
  catfanDevice.off()
}