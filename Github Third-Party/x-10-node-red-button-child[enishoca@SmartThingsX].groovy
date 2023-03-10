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
  name: "X-10 Node Red Button Child",
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
  dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {

    section(title: "Pair X-10 remote button or sensor with SmartThing devices") {
      input "deviceHouseCode", "enum", title: "X-10 House Code", required: true, options: [
        "A": "A",
        "B": "B",
        "C": "C",
        "D": "D",
        "E": "E",
        "F": "F",
        "G": "G",
        "H": "H",
        "I": "I",
        "J": "J",
        "K": "K",
        "L": "L",
        "M": "M",
        "N": "N",
        "O": "O",
        "P": "P"
      ]
      input "deviceUnitCode", "enum", title: "X-10 Unit Code", required: true, options: [
        "1": "1",
        "2": "2",
        "3": "3",
        "4": "4",
        "5": "5",
        "6": "6",
        "7": "7",
        "8": "8",
        "9": "9",
        "10": "10",
        "11": "11",
        "12": "12",
        "13": "13",
        "14": "14",
        "15": "15",
        "16": "16"
      ]
      input "buttonSwitch", "capability.switch", title: "Select SmartThings switch to control", required: true, multiple: true
    }
  }
}


def installed() {
  log.debug "Child Installed  ${settings.deviceHouseCode} ${settings.deviceUnitCode} ${settings.buttonSwitch}"
  initialize()
}

def updated() {
  log.debug "Child updated with settings: ${settings}"
  initialize()
}

def initialize() {
  unsubscribe()
  log.debug "Initialized with settings: ${settings}"
  state.nodeRedMac = parent.state.nodeRedMac
  app.updateLabel("${getDeviceString()} Controlling ${settings.buttonSwitch} ")
  state.deviceString = getDeviceString()
  subscribe(location, "X10RemoteEvent-${state.deviceString}", X10RemoteEventHandler)
}

def uninstalled() {
  log.debug "Child uninstalled"
}

def getDeviceString() {
  return "${settings.deviceHouseCode}-${settings.deviceUnitCode}"
}

def X10RemoteEventHandler(evt) {
  def data = parseJson(evt.data)
  //log.debug "X-10 remote event recieved - data: ${data}"
  setDeviceStatus(data.deviceString, data.status)
  return
}

def setDeviceStatus(deviceString, status) {
  log.debug "Child setDeviceStatus  ${buttonSwitch}   state.deviceString:[${state.deviceString}]"
  
  if (deviceString == state.deviceString) {
  log.trace status
    switch (status) {
        case "on":
            buttonSwitch.on()
            //log.trace ("Turning on")
            break
        case "off":
            buttonSwitch.off()
            //log.trace ("Turning off")
            break
        case "dim":
             
            dimSwitches(buttonSwitch, true)
            log.trace ("Dim " + buttonSwitch.currentValue("level") )
            break
        case "bright":
            dimSwitches(buttonSwitch, false)
            log.trace ("Bright " + buttonSwitch.currentValue("level") )
            break
     }   
  }
}

private dimSwitches(buttonSwitches, dimming)
{
  for (button in buttonSwitches) {
    float level = button.currentValue("level")
    if (dimming) {
      button.setLevel (Math.max(0, level - 7));
    } else 
    {
       button.setLevel (Math.min ( 100, level + 7));
    } 
  }
}