/**
 *  Carvoyant Actions
 *
 *  Copyright 2014 Carvoyant
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 * 
 *  Guidance on writing this application was taken from https://github.com/yracine/device-type.myecobee
 */
definition(
    name: "Carvoyant Actions",
    namespace: "carvoyant",
    author: "Carvoyant",
    description: "Create actions to perform when Carvoyant enabled vehicles perform events.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon.png",
    iconX2Url: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon2x.png",
    iconX3Url: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon3x.png")


preferences {

  page(name: "pageOne", title: "Setup Event", nextPage:"pageTwo", uninstall: true)
  {
    section("App Nickname") {
      label title: "Assign a name", required: false
    }
    section("When the selected vehicle(s)") {
      input "vehicles", "device.connectedCar", multiple: true, title:"Select Carvoyant Vehicle(s)...", required: true
    }
    section("perform this event") {
      input "vehicleEvent", "enum", title: "Select Vehicle Event...", required: true, metadata:[values:["Arrival", "Departure", "Ignition On", "Ignition Off"]]
    }
    section("And there is no motion on these sensors (optional)") {
      input "motions", "capability.motionSensor", title: "Select Sensor(s)", required: false,  multiple: true
    }
    section("And the above sensor(s) report no motion for (default=0 minutes)") {
      input "residentsQuietThreshold", "number", title: "Time in minutes", required: false
    }      
  }
  
  page(name: "pageTwo", title: "Setup Action", nextPage:"pageThree")
  {
    section("Set the selected lock(s)") {
      input "locks", "capability.lock", title: "Select Lock(s)...", required:false, multiple: true
      input "lockState", "enum", title: "Locked/Unlocked", metadata:[values:["LOCKED","UNLOCKED"]], required:false
    }
    section("Set the selected light(s)") {
      input "switches", "capability.switch", title: "Select Light(s)...", multiple: true, required: false
      input "switchState", "enum", title: "On/Off", metadata:[values:["ON","OFF"]], required:false
    }
    section("Set the selected camera(s)") {
      input "cameras",  "capability.imageCapture", title: "Select Camera(s)...", multiple: true, required: false
      input "cameraState", "enum", title: "On/Off", metadata:[values:["ON","OFF"]], required:false
    }
    section("Set the alarm system") {
      input "alarmSwitch", "capability.contactSensor", title: "Select Alarm...", required: false
      input "alarmState", "enum", title: "On/Off", metadata:[values:["ON","OFF"]], required:false
    }
  }
    
  page(name: "pageThree", title: "Notifications",  install:true, uninstall: true)
  {
    section() {
      input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
      input "phone", "phone", title: "Send a Text Message?", required: false
      input "detailedNotif", "Boolean", title: "Detailed Notifications?",metadata:[values:["true", "false"]], required:false
    }
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

//subscribe to device attributes depending upon preference selection 
def initialize() {

  if(vehicleEvent == "Arrival" || vehicleEvent == "Departure")
  {
    log.debug "presence"
    subscribe(vehicles, "presence", presence)
  }
  else
  {
    subscribe(vehicles, "ignitionStatus", ignitionStatus)
  }
  
  if(alarmSwitch)
  {
    subscribe(alarmSwitch, "contact", alarmSwitchContact)
  }
  
  if (motions != null && motions != "") {
    subscribe(motions, "motion", motionEvtHandler)
  }
}

//notifies user of alarm set.
def alarmSwitchContact(evt) {
  log.info "alarmSwitchContact, $evt.name: $evt.value"

  if ((alarmSwitch.currentContact == "closed") && residentsHaveBeenQuiet() && everyoneIsAway() && vehicleEvent == "Departure") {
    if (detailedNotif == 'true') {
      send("Carvoyant> alarm system just armed")
    }
  }
}

//updates the time of the last detected motion
def motionEvtHandler(evt) {
  if (evt.value == "active") {
    state.lastIntroductionMotion = now()
    log.debug "Motion at home..."
  }
}

//makes sure the motion sensors have been quiet for a specified time
private residentsHaveBeenQuiet() {

  def threshold = residentsQuietThreshold ?: 0   // By default, the delay is 0 minutes
  Integer delay = threshold * 60 
  
  def result = true
  def t0 = new Date(now() - (threshold * 60 *1000))
  for (sensor in motions) {
    def recentStates = sensor.statesSince("motion", t0)
    if (recentStates.find{it.value == "active"}) {
      result = false
      break
    }
  }
  log.debug "residentsHaveBeenQuiet: $result"
  return result
}

//receives updates of device's ignition status and takes actions based on preferences
def ignitionStatus(evt)
{
  log.debug "$evt.name: $evt.value"
  if((evt.value == "ON" && vehicleEvent == "Ignition On") || (evt.value == "OFF" && vehicleEvent == "Ignition Off"))
  {
    takeActions()
  }
}

//receives updates of device's presence and takes actions based on preferences
def presence(evt) {
  def threshold = residentsQuietThreshold ?: 0   // By default, the delay is 0 minutes
  Integer delay = threshold * 60 

  log.debug "$evt.name: $evt.value"
  if (vehicleEvent == "Departure") {
  
    if (detailedNotif == 'true') {
      send("Carvoyant> not present at home")
    }
      log.debug "checking if everyone is away  and quiet at home"
    if (residentsHaveBeenQuiet()){
  
      if (everyoneIsAway()) {
        if (detailedNotif == 'true') {
          send("Carvoyant> Quiet at home...")
        }    
        runIn(delay, "takeActions")
      } 
      else {
        log.debug "Not everyone is away, doing nothing"
        if (detailedNotif == 'true') {
          send("Carvoyant> Not everyone is away, doing nothing..")
        }
      }
    } 
    else {
    
      log.debug "Things are not quiet at home, doing nothing"
      if (detailedNotif == 'true') {
        send("Carvoyant> Things are not quiet at home...")
      }    
    }     
  }
  else if (vehicleEvent == "Arrival")
  {
    def result = true
    for (v in vehicles) { 
      if (v.currentPresence == "not present") {
        result = false
        break
      }
    }
    if(result)
    {
      runIn(delay, "takeActions")
    }
  }
}

//performs the actions specified in the preferences.
def takeActions() {
  
  def msg
  
  if (alarmSwitch) {
    if(alarmState == "ON" && alarmSwitch.currentContact == "open")
    {
      alarmSwitch.on()
      log.debug "alarm on"
    }
    else if(alarmState == "OFF"  && alarmSwitch.currentContact == "closed")
    {
      alarmSwitch.off()
      log.debug "alarm off"
    }
    if (detailedNotif == 'true') {
      send("Carvoyant> Alarm set to " + alarmState)
    }     
  }    
  
  if(locks)
  {
    if(lockState == "LOCKED")
    {
      locks.lock()
      msg = "Carvoyant> Locked the locks"
      log.debug "locks Locked"
    }
  
    else if(lockState == "UNLOCKED")
    {	
      locks.unlock()
      msg = "Carvoyant> Unlocked the locks"
      log.debug "locks unlocked"
    }
  
    if (detailedNotif == 'true') 
    {
      send(msg)
    }   
  }
  
  if(switches)
  {
    if(switchState == "ON")
    {
      if(switches?.on())
      {	
        msg = "Carvoyant> Switched on all switches"
        log.debug "switches on"
      }
    }
    else if(switchState == "OFF")
    {
      if(switches?.off())
      {	
        msg = "Carvoyant> Switched off all switches"
        log.debug "switches off"
      }
    }
    if (detailedNotif == 'true')
    {
      send(msg)
    }    
  }
  
  if(cameras)
  {
    if(cameraState == "ON")
    {
      if(cameras?.alarmOn())
      {
        // arm the cameras
        msg = "Carvoyant> cameras are now armed"
        log.debug "cameras on"
      }
    }
    if(cameraState == "OFF")
    {
      if(cameras?.alarmOff)
      {
        // disarm the cameras
        msg = "Carvoyant> cameras are now disarmed"
        log.debug "cameras off"
      }
    }
    if (detailedNotif == 'true') 
    {
      send(msg)
    }
  }
}

//checks current presence to make sure eveyone is away
private everyoneIsAway() {
  def result = true
  for (v in vehicles) { 
    if (v.currentPresence == "present") {
      result = false
      break
    }
  }
  log.debug "everyoneIsAway: $result"
  return result
}

//sends messages to either smartthings app via push message, or to a phone over sms
private send(msg) {
  if ( sendPushMessage != "No" ) {
    log.debug( "sending push message" )
    sendPush( msg )
  }
  
  if ( phone ) {
    log.debug( "sending text message" )
    sendSms( phone, msg )
  }
  
  log.debug msg
}
