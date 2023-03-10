/**
 *
 * ============================================ 
 *  Cat Feeder Scheduler
 * ============================================ 
 *
 *  Daily run times and intervals for automatic cat feeder. Allows for three feedings per day
 *
 *  Copyright (c)2018 Mark Page (mark@very3.net)
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
  name: "Cat Feeder Scheduler",
  namespace: "cat-feeder-scheduler",
  author: "Mark Page",
  description: "Daily run times and intervals for automatic cat feeder. Allows for three feedings per day.",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  section {
    paragraph "Daily run times and intervals for automatic cat feeder. Allows for three feedings per day."
  }
  section {
    input "catfeederDevice", "capability.switch", title: "Select device to control cat feeder:", required: true
  }
  section {
    input "firstFeeding","time",title:"First Feeding:", required: true
  }
  section {
    input "secondFeeding","time",title:"Second Feeding:", required: true
   }
  section {
    input "thirdFeeding","time",title:"Third Feeding:", required: true
   }
  section {
    input "runTime","number",title:"Motor run time in minutes:", required: true
   }
  section {
    input "minOffset","number",title:"Cron offset minutes (0-60):", required: true
    input "secOffset","number",title:"Cron offset seconds (0-60):", required: true
   }
  section {
    input "msgText","text",title:"Notification message:", required: true
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
  def tz = location.timeZone

  def firstStartTime  = timeToday(firstFeeding,tz)
  def firstStartHour  = firstStartTime.format("H",tz)
  def firstStartMin   = firstStartTime.format("m",tz).toInteger() + minOffset.toInteger()
  def firstStopMin    = firstStartMin.toInteger() + runTime.toInteger() + minOffset.toInteger()
  def firstStartCron  = "${secOffset} ${firstStartMin} ${firstStartHour} ? * *"
  def firstStopCron   = "${secOffset} ${firstStopMin} ${firstStartHour} ? * *"

  schedule("${firstStartCron}", firstFeedingOn)
  schedule("${firstStopCron}", firstFeedingOff)
  log.debug "CRON INIT (First Feeding): [${firstStartCron}] to [${firstStopCron}]"

  def secondStartTime = timeToday(secondFeeding,tz)
  def secondStartHour = secondStartTime.format("H",tz)
  def secondStartMin  = secondStartTime.format("m",tz).toInteger() + minOffset.toInteger()
  def secondStopMin   = secondStartMin.toInteger() + runTime.toInteger() + minOffset.toInteger()
  def secondStartCron = "${secOffset} ${secondStartMin} ${secondStartHour} ? * *"
  def secondStopCron  = "${secOffset} ${secondStopMin} ${secondStartHour} ? * *"

  schedule("${secondStartCron}", secondFeedingOn)
  schedule("${secondStopCron}", secondFeedingOff)
  log.debug "CRON INIT (Second Feeding): [${secondStartCron}] to [${secondStopCron}]"

  def thirdStartTime  = timeToday(thirdFeeding,tz)
  def thirdStartHour  = thirdStartTime.format("H",tz)
  def thirdStartMin   = thirdStartTime.format("m",tz).toInteger() + minOffset.toInteger()
  def thirdStopMin    = thirdStartMin.toInteger() + runTime.toInteger() + minOffset.toInteger()
  def thirdStartCron  = "${secOffset} ${thirdStartMin} ${thirdStartHour} ? * *"
  def thirdStopCron   = "${secOffset} ${thirdStopMin} ${thirdStartHour} ? * *"

  schedule("${thirdStartCron}", thirdFeedingOn)
  schedule("${thirdStopCron}", thirdFeedingOff)
  log.debug "CRON INIT (third Feeding): [${thirdStartCron}] to [${thirdStopCron}]"
}

def firstFeedingOn() {
  catfeederDevice.on()
  sendPush("${msgText}")
}
def firstFeedingOff() {
  catfeederDevice.off()
}

def secondFeedingOn() {
  catfeederDevice.on()
  sendPush("${msgText}")
}
def secondFeedingOff() {
  catfeederDevice.off()
}

def thirdFeedingOn() {
  catfeederDevice.on()
  sendPush("${msgText}")
}
def thirdFeedingOff() {
  catfeederDevice.off()
}
