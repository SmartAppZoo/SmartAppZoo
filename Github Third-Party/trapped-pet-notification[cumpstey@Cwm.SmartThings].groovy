/**
 *  Trapped Pet Notification
 *
 *  Author: Neil Cumpstey
 *
 *  Copyright 2018 Neil Cumpstey
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
definition(
  name: 'Trapped Pet Notification',
  namespace: 'cwm',
  author: 'Neil Cumpstey',
  description: 'Get a notification when motion is detected in a room when the lights are off and the doors are shut.',
  category: 'Pets',
  iconUrl: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/trapped-pet-notification.src/assets/pawprint-60.png',
  iconX2Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/trapped-pet-notification.src/assets/pawprint-120.png',
  iconX3Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/trapped-pet-notification.src/assets/pawprint-180.png'
)

//#region Preferences

preferences {
  section('When motion is detected...') {
    input name: 'motions', type: 'capability.motionSensor', title: 'Where?', multiple: true
  }
  section('And the doors are shut...') {
    input name: 'contacts', type: 'capability.contactSensor', title: 'Which door?', required: false, multiple: true
  }
  section('And the lights are off...') {
    input name: 'lights', type: 'capability.light', title: 'Which light?', required: false, multiple: true
  }
  section('Notification') {
    // TODO: paragraph "Use placeholder {device} for device name."
    input name: 'message', type: 'text', title: 'Notification text', required: false
  }
}

//#endregion Preferences

//#region App event handlers

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

//#endregion App event handlers

//#region Device event handlers

def motionHandler(evt) {
  logger "Motion detected on ${evt.device}"
    
  if (contacts) {
    def contactsOpen = contacts.currentContact.findAll { val ->
      val == 'open' ? true : false
    }
    
    if (contactsOpen.size() > 0) {
      logger "Door open, so nobody's trapped"
      return
    }
  }
  
  if (lights) {
    def lightsOn = lights.currentSwitch.findAll { val ->
      val == 'on' ? true : false
    }
    
    if (lightsOn.size() > 0) {
      logger "Light on, so it's probably a human"
      return
    }

    // def lightState = light1.currentState("switch")
    // if (lightState.value == "on") {
    //   return
    // }
  }

  def message = settings.message || "Unexpected motion on ${evt.device}. Is there a pet trapped?"
  logger "Sending push notification: ${message}"
  sendPush(message)
}

//#endregion Device event handlers

//#region Private helpers

private initialize() {
  subscribe(motions, 'motion.active', motionHandler)
}

private logger(msg, level = 'debug') {
  switch (level) {
    case 'error':
      if (state.logLevel >= 1) log.error msg
      break
    case 'warn':
      if (state.logLevel >= 2) log.warn msg
      break
    case 'info':
      if (state.logLevel >= 3) log.info msg
      break
    case 'debug':
      if (state.logLevel >= 4) log.debug msg
      break
    case 'trace':
      if (state.logLevel >= 5) log.trace msg
      break
    default:
      log.debug msg
      break
  }
}

//#endregion Private helpers