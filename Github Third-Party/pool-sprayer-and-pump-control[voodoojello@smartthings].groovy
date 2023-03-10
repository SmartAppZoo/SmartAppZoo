//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Pool Sprayer and Pump Control
//  Copyright (c)2019-2020 Mark Page (mark@very3.net)
//  Modified: Sat Nov 30 06:42:39 CST 2019
//
//  Control pool pump and sprayer by air temperature and wind speed based on published capabilities of the very3 
//  Ambient PWS Device Handler. For more information see:
//
//      https://github.com/voodoojello/smartthings/tree/master/devicetypes/apws-device-handler
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
  name: "Pool Sprayer and Pump Control",
  namespace: "very3-pool-sprayer-and-pump-control",
  author: "Mark Page",
  description: "Control pool pump and sprayer by air temperature and wind speed based on published capabilities of the very3 Ambient PWS Device Handler",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  section {
    paragraph "Control pool pump and sprayer by air temperature and wind speed based on published capabilities of the very3 Ambient PWS Device Handler."
  }
  
  section ("Select Weather Source") {
    input ("temperatureValue", "capability.temperatureMeasurement", title: "Temperature / Wind Source:", required: true)
  }

  section ("Pool Pump Settings") {
    input "pumpSwitch", "capability.switch", required: true, title: "Choose the pump switch to control:"
    input "pumpHighTempThres", "number", required: true, title: "Turn off the pump if temperature goes above:"
    input "pumpLowTempThres", "number", required: true, title: "Turn off the pump if the temperature drops below:"
    input "pumpHoldSwitch", "capability.switch", required: true, title: "Choose the pool pump virtual hold switch:"
  }
  
  section ("Pool Sprayer Settings") {
    input "sprayerSwitch", "capability.switch", required: true, title: "Choose the sprayer switch to control:"
    input "sprayerHighWindThres", "number", required: true, title: "Turn off the sprayer if windspeed goes above:"
    input "sprayerLowTempThres", "number", required: true, title: "Turn off the sprayer if the temperature drops below:"
    input "sprayerHoldSwitch", "capability.switch", required: true, title: "Choose the sprayer virtual hold switch:"
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
  state.logMode   = 0
  state.logHandle = 'PSPC'

  subscribe(temperatureValue, "temperatureMeasurement" , pwsHandler)
  subscribe(temperatureValue, "windSpeed" , pwsHandler)
  subscribe(temperatureValue, "windGust" , pwsHandler)
  
  poll()
}

def pwsHandler(evt) {
  logger('trace','pwsHandler',"PWS ${evt.name} changed to ${evt.value}")
  poll()
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def poll() {
  logger('trace','poll',"Polling...")

  def windSpeed = (temperatureValue.latestValue("windSpeed") as BigDecimal)
  def osTemp    = (temperatureValue.latestValue("temperatureMeasurement") as BigDecimal)

  // Pump Switch
  if (osTemp >= pumpHighTempThres) {
    state.pumpSwitchState = 'OFF'
    state.pumpSwitchCause = "osTemp: ${osTemp}, pumpHighTempThres: ${pumpHighTempThres}"
  }
  else if (osTemp <= pumpLowTempThres) {
    state.pumpSwitchState = 'OFF'
    state.pumpSwitchCause = "osTemp: ${osTemp}, pumpLowTempThres: ${pumpLowTempThres}"
  }
  else {
    state.pumpSwitchState = 'ON'
    state.pumpSwitchCause = "osTemp: ${osTemp}, pumpLowTempThres: ${pumpLowTempThres}, pumpHighTempThres: ${pumpHighTempThres}"
  }

  if (pumpHoldSwitch.currentSwitch == 'on') {
   state.pumpSwitchCause = "Pool Pump is in a hold state"
  }

  if (state.pumpSwitchState == 'ON' && pumpSwitch.currentSwitch == 'off' && pumpHoldSwitch.currentSwitch == 'off') {
    pumpSwitch.on()
    sendNotificationEvent("Pool Pump turned ${state.pumpSwitchState} [${state.pumpSwitchCause}]")
  }
  
  if (state.pumpSwitchState == 'OFF' && pumpSwitch.currentSwitch == 'on' && pumpHoldSwitch.currentSwitch == 'off') {
    pumpSwitch.off()
    sendNotificationEvent("Pool Pump turned ${state.pumpSwitchState} [${state.pumpSwitchCause}]")
  }
  
  logger('trace','poolPump',"Pool Pump turned ${state.pumpSwitchState} [${state.pumpSwitchCause}]")

  // Sprayer Switch
  if (osTemp <= sprayerLowTempThres) {
    state.sprayerSwitchState = 'OFF'
    state.sprayerSwitchCause = "osTemp: ${osTemp}, sprayerLowTempThres: ${sprayerLowTempThres}"
  }
  else if (windSpeed >= sprayerHighWindThres) {
    state.sprayerSwitchState = 'OFF'
    state.sprayerSwitchCause = "windSpeed: ${windSpeed}, sprayerHighWindThres: ${sprayerHighWindThres}"
  }
  else {
    state.sprayerSwitchState = 'ON'
    state.sprayerSwitchCause = "osTemp: ${osTemp}, sprayerLowTempThres: ${sprayerLowTempThres}, windSpeed: ${windSpeed}, sprayerHighWindThres: ${sprayerHighWindThres}"
  }

  if (sprayerHoldSwitch.currentSwitch == 'on') {
   state.sprayerSwitchCause = "Pool Sprayer is in a hold state"
  }
 
  if (state.sprayerSwitchState == 'ON' && sprayerSwitch.currentSwitch == 'off' && sprayerHoldSwitch.currentSwitch == 'off') {
    sprayerSwitch.on()
    sendNotificationEvent("Pool Sprayer turned ${state.sprayerSwitchState} [${state.sprayerSwitchCause}]")
  }
  
  if (state.sprayerSwitchState == 'OFF' && sprayerSwitch.currentSwitch == 'on' && sprayerHoldSwitch.currentSwitch == 'off') {
    sprayerSwitch.off()
    sendNotificationEvent("Pool Sprayer turned ${state.sprayerSwitchState} [${state.sprayerSwitchCause}]")
  }
  
  logger('trace','poolSprayer',"Pool Sprayer turned ${state.sprayerSwitchState} [${state.sprayerSwitchCause}]")
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
