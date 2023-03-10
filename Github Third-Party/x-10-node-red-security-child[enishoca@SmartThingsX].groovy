/**
 *  X-10 Node Red Button Child Smart App
 *
 * 	Author: Enis Hoca
 *   - enishoca@outlook.com
 *
 *  Copyright 2018 Enis Hoca
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonSlurper

definition(
  name: "X-10 Node Red Security Child",
  namespace: "enishoca",
  author: "Enis Hoca",
  description: "Child Application for 'X-10 Node Red Bridge' - do not install directly.",
  category: "My Apps",
  parent: "enishoca:X-10 Node Red Bridge",
  iconUrl: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
  iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png",
  singleInstance: true
)

preferences {
  page(name: "pageMain")
}

def pageMain() {
  if (state.securityCode) {
    return normalPage() 
  } else {
    return discoveryPage() 
  }
}

def normalPage() {
  def installed = app.installationState == "COMPLETE"
  def sectionTitle = "X-10 Security Device Code: ${state.securityCode}"
  if (!installed)
  		sectionTitle = "New Security Device Found!!!\n\n"  + sectionTitle
  return dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {       
 	section(title: sectionTitle) {
        label(name: "label", title: "Assign a name", defaultValue:"X-10 Sensor ${state.securityCode}", required: true, multiple: false)
        
        input(name: "deviceType", title: "Device Type", type: "enum", submitOnChange: true, options: [
                    "remote" : "Security Remote",
                    "keyfob" : "Security Key Fob",
                    "panic" : "Panic Button",
                    "switch": "Sensor"])
                    
        switch (deviceType) {
          case "remote" : 
           input(name: "armSwitch", type: "capability.switch", title: "Arm Away / Disarm" , required: false, multiple: true)
           input(name: "armHomeSwitch", type: "capability.switch", title: "Arm Home / Disarm" , required: false, multiple: true)
           input(name: "allLights", type: "capability.switch", title: "Security Lights" , required: false, multiple: true)
           input(name: "panic", type: "capability.switch", title: "Panic" , required: false, multiple: true)
           break;
         case "keyfob" : 
           input(name: "armSwitch", type: "capability.switch", title: "Arm Away / Disarm" , required: false, multiple: true)
           input(name: "allLights", type: "capability.switch", title: "Lights On/Off" , required: false, multiple: true)
           input(name: "panic", type: "capability.switch", title: "Panic" , required: false, multiple: true)
           break;
        default:         
          input(name: "buttonSwitch", type: "capability.switch", title: "Select SmartThings switch to pair with" , required: true, multiple: true)
        }
      }
      section("Send low battery and tamper notifications?") {
        input("recipients", "contact", title: "Send notifications to", required: false, multiple: true) 
     }
  }     
}

//input(name: "deviceType", type: "enum", title: "Device Type", options: ["Security Remote","Security Motion Sensor (MS18A, MS10A etc.)","Blue","Yellow"])
//input "buttonSwitch", "capability.switch", title: "Select SmartThings switch to control", required: true, multiple: true

def discoveryPage() {
  subscribe(location, null, lanResponseHandler, [filterEvents: false])
  return dynamicPage(name: "pageMain", title: "", refreshInterval: 5, install: true, uninstall: true) {
    section(title: "Please carry out the sequence for registering the security remote or sensor with an X-10 Console on the sensor device.\n\nAny X-10 security device that sends a message, while you are in this mode, will be associated with this SmartApp.\n") {
       paragraph title: "Device Discovery Mode", required: true,"Looking for X-10 security devices...\n "               
    }
    
  }
}
 
def installed() {
  log.debug "X-10 Security Child Installed  ${getDeviceString()} ${settings}"
  initialize()
}

def updated() {
  log.debug "X-10 Child Updated ${getDeviceString()} ${settings}"
  initialize()
}

def initialize() {
  unsubscribe()
  log.debug "Subscribing to event [X10RemoteEvent-${state.deviceString}]"
  state.nodeRedMac = parent.state.nodeRedMac
  //app.updateLabel("${getDeviceString()} Controlling ${settings.buttonSwitch} ")
  state.deviceString = getDeviceString()
  subscribe(location, "X10RemoteEvent-${state.deviceString}", X10RemoteEventHandler)
}

def uninstalled() {
  log.debug "Child uninstalled"
}

def getDeviceString() { 
 try {
  return "${state.securityCode}-*"
  } catch (e){
   return ""
  }
}

def lanResponseHandler(evt) {
  //log.debug "sec-lanResponseHandler state: ${state}"
  log.debug "sec-lanResponseHandler Event: ${evt.stringValue}"

  def map = stringToMap(evt.stringValue)
  def headers = parent.parseHttpHeaders(map.headers);
  //log.trace "sec-lanResponseHandler Headers: ${headers}"

  //if this is a registration response update the saved mac
  if (headers.X10NodeRed == 'DeviceUpdate') {
      def body = parent.parseHttpBody(map.body);
      log.trace "sec-lanResponseHandler Body: ${body}"
      parseSecurityCode(body)
  }
}

private parseSecurityCode(body) {
  log.trace "processEvent Body: ${body}"
  // [protocol:rfsec, unitcode:*, direction:rx, state:motion_normal_sp554a, housecode:0x6d]
  def deviceString = ""
  def status

  def housecodekey = "housecode"
  if ((body.containsKey(housecodekey)) && (body.unitcode == '*')) {
    state.securityCode = body.housecode.toUpperCase()
    log.trace "New securityCode set: ${state.securityCode}"
  }
}

def X10RemoteEventHandler(evt) {

  def data = parseJson(evt.data)
  log.debug "X-10 security sensor event recieved: [${data.deviceString}], [${data.status}]"
  setDeviceStatus(data.deviceString, data.status)
  return
}


  /* Possible status values returned by mochad
     _low indicates low battery
     _tamper indicates tamper alert
     
     "Motion_alert_MS10A" 
     "Motion_normal_MS10A"
     "Motion_alert_low_MS10A"
     "Motion_normal_low_MS10A"
     "Contact_alert_min_DS10A"
     "Contact_normal_min_DS10A"
     "Contact_alert_min_tamper_DS12A"
     "Contact_normal_min_tamper_DS12A"
     "Contact_alert_max_DS10A"
     "Contact_normal_max_DS10A"
     "Contact_alert_max_tamper_DS12A"
     "Contact_normal_max_tamper_DS12A"
     "Contact_alert_min_low_DS10A"
     "Contact_normal_min_low_DS10A"
     "Contact_alert_max_low_DS10A"
     "Contact_normal_max_low_DS10A"}
     "Arm_KR10A"
     "Disarm_KR10A"
     "Lights_On_KR10A"
     "Lights_Off_KR10A"
     "Panic_KR10A"
     "Panic_KR15A"
  */
  
  /*
    "remote" : "Security Remote",
    "keyfob" : "Security Key Fob",
    "panic" : "Panic Button",
    "switch": "Sensor"])
  */
  
def setDeviceStatus(deviceString, status) {
  
  if (deviceString == state.deviceString) {
 
      switch (settings.deviceType) {
          case "remote":
            remoteHandler(status)
            break
            
          case "keyfob": 
            keyfobHandler(status) 
            break

          default:
            switchHandler (status)
            break
      }
      
     if (status.contains('low')) {
        sendNotif("Low battery alert")
        //log.trace ("low battery alert")
     }
     
     if (status.contains('tamper')) {
        sendNotif("Tamper alert")
        //log.trace ("tamper alert")
     }
     
  }
}

def remoteHandler (status) {
try {
    switch (status) {
        case ~/^arm_home.*/:
            //log.trace ("Arm_Home")
            if (armHomeSwitch) armHomeSwitch.on()
            break
        case ~/^arm_awa.*/:
            //log.trace ("Arm_Awa")
            if (armSwitch) armSwitch.on()
            break   
        case ~/^disarm.*/:
            //log.trace ("Disarm")
            if (armSwitch) armSwitch.off()
            if (armHomeSwitch) armHomeSwitch.off()
            break            
        case ~/^panic.*/: 
            //log.trace ("Panic")
            if (panic) panic.on()
            break
        case ~/^lights_on.*/:
            //log.trace ("Lights_On")
            if (allLights) allLights.on()
            break
        case ~/^lights_off.*/:
            //log.trace ("Lights_Off")
            if (allLights) allLights.off()
            break
     }
   } finally {
   }
}

def keyfobHandler (status) {
try {
    switch (status) {
        case ~/^arm.*/:
            //log.trace ("Arm_Awa")
            if (armSwitch) armSwitch.on()
            break   
        case ~/^disarm.*/:
            //log.trace ("Disarm")
            if (armSwitch) armSwitch.off()
            break            
        case ~/^panic.*/: 
            //log.trace ("Panic")
            if (panic) panic.on()
            break
        case ~/^lights_on.*/:
            //log.trace ("Lights_On")
            if (allLights) allLights.on()
            break
        case ~/^lights_off.*/:
            //log.trace ("Lights_Off")
            if (allLights) allLights.off()
            break
     }
   } finally {
   }
}


def switchHandler (status) {
  try {
    switch (status) {
        case ~/.*alert.*/:
        case ~/^panic.*/: 
            //log.trace ("Turning on")
            buttonSwitch.on()
            break
        case ~/.*normal.*/:
            //log.trace ("Turning off")
            buttonSwitch.off()
            break
     }
  } finally {
  }
}

def sendNotif(notification) {
    def message = "${app.getLabel()} ${notification}"
    
    log.debug "Sending message $message to recipients: $recipients"
    sendPushMessage(message)
 
     if (location.contactBookEnabled ) {
        log.debug "Contact Book enabled!"
        sendNotificationToContacts(message, recipients)
    }  
    
}


