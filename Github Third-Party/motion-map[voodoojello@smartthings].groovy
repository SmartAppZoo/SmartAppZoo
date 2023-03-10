//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Motion Map for SmartThings
//  Copyright (c)2019-2020 Mark Page (mark@very3.net)
//  Modified: Fri Nov 29 08:41:13 CST 2019
//
//  Show motion trail based on PIR events
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at:
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software distributed under the License is
//  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and limitations under the License.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

definition (
  name: "Motion Map",
  version: "19.11.29.8",
  namespace: "very3-motion-map",
  author: "Mark Page",
  description: "Show motion trail based on PIR events",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  page(name: "mainPage", install: true, uninstall: true)
}

def mainPage() {
  dynamicPage(name: "mainPage", title: "") {

    section ("HVAC Control") {
      paragraph "Show motion trail based on PIR events."
    }

    section ("Select Motion Sensors") {
      input ("motionSensors", "capability.motionSensor", title: "Select PIR Devices:", multiple: true, required: true)
    }
  }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def installed() {
  initialize()
}

def updated() {
  logger('info','updated',"Updated with settings: ${settings}")
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  state.logMode      = 1
  state.logHandle    = 'PIRM'
  state.shmStatus    = 'off'

  subscribe(location, "alarmSystemStatus" , shmHandler)
  subscribe(location, null, shmHandler, [filterEvents:false])
  subscribe(motionSensors, "motion", msHandler)
}

def shmHandler(evt) {
  logger('debug','shmHandler',"Smart Home Monitor ${evt.name} changed to ${evt.value}")
  state.shmStatus = evt.value
}

def msHandler(evt) {
  logger('debug','pirHandler',"PIR ${evt.displayName} changed to ${evt.value}")
  poll(evt.displayName,evt.value)
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def poll(name,state) {
  def msgStr = "Motion: ${name} is ${state}"
  
  logger('info','poll', msgStr)
  
  if ("${state}" == 'active') {
    sendNotificationEvent(msgStr)
  }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private getThermStates() {
  def funcReturn = [:]

  thermostats.each {
    def key   = "${it.label}"
  	def inner = [:]
    
    inner.thermostatName           = it.label
    inner.thermostatOperatingState = it.currentValue("thermostatOperatingState")
    inner.temperature              = it.currentValue("temperature")
    inner.thermostatMode           = it.currentValue("thermostatMode")
    inner.thermostatFanMode        = it.currentValue("thermostatFanMode")
    inner.coolingSetpoint          = it.currentValue("coolingSetpoint")
    inner.heatingSetpoint          = it.currentValue("heatingSetpoint")
    
    logger('trace','getThermStates',inner)
    
    funcReturn.put((key),inner)
  }
  
  return funcReturn
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private static double round(double value, int precision) {
  if (precision == 0) {
   return (int) Math.round(value)
  }
  
  int scale = (int) Math.pow(10,precision)
  return (double) Math.round(value*scale)/scale
}

private logger(level,loc,msg) {
  // level: error, warn, info, debug, trace
  if ("${level}" == 'info') {
    log."${level}" "${state.logHandle} [${loc}]: ${msg}"
  }
  else if (state.logMode > 0) {
    log."${level}" "${state.logHandle} [${loc}]: ${msg}"
  }
}