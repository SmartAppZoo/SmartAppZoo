/**
 *  X-10 Node Red Switch Child Smart App
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
  name: "X-10 Node Red Switch Child",
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
  page(name: "pageMain", title: "", install: true, uninstall: true) {
    section(title: "X-10 Switches, Modules and Relays") {
      input "deviceName", "text", title: "Device Name", required: true
      input "deviceType", "enum", title: "Device Type RF/PL", required: true, options: [
        "RF": "(RF) X-10 wireless",
        "PL": "(PL) X-10 Powerline"
      ]
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
    }
  }
}

def installed() {
  log.debug "Child Installed ${settings.deviceName} ${settings.deviceType} ${settings.deviceHouseCode} ${settings.deviceUnitCode}"
  initialize()
  addX10Device();
}

def updated() {
  unsubscribe()
  log.debug "Child update with settings: ${settings}"
  initialize()
  updateX10Device()
  state.level = 0;
  subscribe(location, "X10RemoteEvent-${state.deviceString}", X10RemoteEventHandler)
}

def initialize() {
  //log.debug "My ID = ${app.getId()} Initialized with settings: ${settings}"
  app.updateLabel("${settings.deviceHouseCode}-${settings.deviceUnitCode} ${settings.deviceName} ")
}

def uninstalled() {
  log.debug "Child Switch uninstalled"
}

def removeChildDevices(delete) {
  log.debug "removeChildDevices"
  getChildDevices().find {
    d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
  }
  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

def addX10Device() {
  //log.debug "Adding Device ${deviceName}"
  if (!deviceName) return

  def getHostHubId = parent.settings.getHostHubId
  def theDeviceNetworkId = getX10DeviceID()
  def theDevice = addChildDevice("enishoca", "X-10 Node Red Device", theDeviceNetworkId, getHostHubId, [label: deviceName, name: deviceName])
  setX10DeviceID(theDevice)
  theDevice.off();
  log.debug "New Device added ${deviceName}"
}

def updateX10Device() {
  //log.debug "Updating Device ${deviceName}"
  // If user didn't fill this device out, skip it
  if (!deviceName) return;

  def theDeviceNetworkId = getX10DeviceID();
  def theDevice = getDevicebyNetworkId(getX10DeviceID())
  if (theDevice) { // The switch already exists
    setX10DeviceID(theDevice)
    theDevice.label = deviceName
    theDevice.name = deviceName
    subscribe(theDevice, "switch", switchChange)
    subscribe(theDevice, "switch.setLevel", switchSetLevelHandler)
  } 
}

def switchChange(evt) {
  log.debug "Switch event!setting state now for ${state.x10DeviceID} to ${evt.value}"
  parent.sendStatetoX10(state.x10DeviceID, evt.value)
}

def switchSetLevelHandler(evt)
{	
	if ((evt.value == "on") || (evt.value == "off" ))
		return
	int level = evt.value.toInteger()
    int delta = 0;
    def command = "";
    if ( state.level > level ) {
    	command = "-dim"
        delta = state.level - level
    } else {
    	command = "-bright"
        delta = level - state.level 
    }
    state.level = evt.value.toInteger()
    parent.sendStatetoX10(state.x10DeviceID+command, delta)
	log.info "switchSetLevelHandler Event: ${level}"
}


def X10RemoteEventHandler(evt) {
  log.debug "X10RemoteEventHandler Event: ${evt.stringValue}"
  def data = parseJson(evt.data)
  setDeviceStatus(data.deviceString, data.status)
}

def setDeviceStatus(deviceString, status) {
  log.debug "Child setDeviceStatus deviceString:[${deviceString}]  state.deviceString:[${state.deviceString}]"
  def theDevice = getDevicebyNetworkId(getX10DeviceID())
  if ((theDevice) && (deviceString == state.deviceString)) { // The switch already exists
      int level = theDevice.currentValue("level").toInteger()
      switch (status) {
      case "on":
        theDevice.on()
        //log.trace ("Turning on")
        break
      case "off":
        theDevice.off()
        //log.trace ("Turning off")
        break
      case "dim":   
        theDevice.setLevel(level - 1)
        break
      case "bright":
        theDevice.setLevel(level + 1)
        break
     } 
  } 
}

def getX10DeviceID() {
  if (!state.x10DeviceID) {
    setX10DeviceID()
  }
  return state.x10DeviceID
}

def setX10DeviceID(theDevice) {
  state.x10DeviceID = "${settings.deviceType}-${settings.deviceHouseCode}${settings.deviceUnitCode}"
  state.deviceString = "${settings.deviceHouseCode}-${settings.deviceUnitCode}"
  if (theDevice) theDevice.deviceNetworkId = state.x10DeviceID
}

private getDevicebyNetworkId(String theDeviceNetworkId) {
  return getChildDevices().find {
    d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
  }
}