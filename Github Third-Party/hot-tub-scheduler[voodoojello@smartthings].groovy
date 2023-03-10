/**
 *
 * ============================================ 
 *  Hot Tub Scheduler
 * ============================================ 
 *
 *  Weekly multi-day hot tub scheduler
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
  name: "Hot Tub Scheduler",
  namespace: "hot-tub-scheduler",
  author: "Mark Page",
  description: "Weekly multi-day hot tub scheduler.",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  section {
    paragraph "Weekly multi-day hot tub scheduler"
  }
  section {
    input "hottubDevice", "capability.switch", title: "Select device to control hot tub:", required: true
  }
  section {
    input "startDay", "enum", title: "Start Day", required: true, options: ["MON":"Monday","TUE":"Tuesday","WED":"Wednesday","THU":"Thursday","FRI":"Friday","SAT":"Saturday","SUN":"Sunday"]
    input "startTime","time",title:"Start Time:", required: true
  }
  section {
    input "stopDay", "enum", title: "Stop Day", required: true, options: ["MON":"Monday","TUE":"Tuesday","WED":"Wednesday","THU":"Thursday","FRI":"Friday","SAT":"Saturday","SUN":"Sunday"]
    input "stopTime","time",title:"Stop Time:", required: true
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
  unschedule()

  def tz = location.timeZone

  def cronStartTime = timeToday(startTime,tz)
  def cronStartHour = cronStartTime.format("H",tz)
  def cronStartMin  = cronStartTime.format("m",tz)
  def cronStartStr  = "27 ${cronStartMin} ${cronStartHour} ? * ${startDay}"

  def cronStopTime  = timeToday(stopTime,tz)
  def cronStopHour  = cronStopTime.format("H",tz)
  def cronStopMin   = cronStopTime.format("m",tz)
  def cronStopStr   = "27 ${cronStopMin} ${cronStopHour} ? * ${stopDay}"

  schedule("${cronStartStr}", hotTubOn)
  schedule("${cronStopStr}", hotTubOff)

  log.debug "CRON INIT: On at: ${cronStartStr} - Off at: ${cronStopStr}"
}

def hotTubOn() {
  sendNotificationEvent("Turned on hot tub at: ${startTime}")
  hottubDevice.on()
}

def hotTubOff() {
  sendNotificationEvent("Turned off hot tub at: ${stopTime}")
  hottubDevice.off()
}
