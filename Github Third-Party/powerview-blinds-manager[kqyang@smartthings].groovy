/**
 *  Copyright 2017 KongQun Yang
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 *
 */

definition(
  name: "Powerview Blinds Manager",
  namespace: "xyy",
  author: "KongQun Yang",
  description: "Powerview Blinds Manager SmartApp",
  category: "My Apps",
  iconUrl: "https://storage.googleapis.com/powerview/powerview.png",
  iconX2Url: "https://storage.googleapis.com/powerview/powerview.png",
  iconX3Url: "https://storage.googleapis.com/powerview/powerview.png")

preferences {
  page(name: "mainPage")
}

def mainPage() {
  dynamicPage(name: "mainPage", title: "Install Blinds", install: true,
              uninstall: true) {
    section("Main Settings"){
      input("hubName", "hub", title:"Hub", required: true)
      input("numBlinds", "number", title: "Number of Blinds", required: true,
            submitOnChange: true)
      input("powerviewHubIP", "string", title: "Powerview Hub IP Address",
            required: true)
    }
    for (def i = 0; i < numBlinds; ++i) {
      section("Add a Blind") {
        input("blindName$i", "string", title: "Name This Blind", required: true)
        input("blindId$i", "number", title: "Blind ID", required: true)
      }
    }
  }
}

def installed() {
  log.debug "Installed"
  initialize()
}

def updated() {
  log.debug "Updated"
  unsubscribe()
  initialize()
}

def initialize() {
  state.powerviewHubIP = powerviewHubIP
  log.debug "Powerview Hub IP: ${state.powerviewHubIP}"

  try {
    def devices = getChildDevices()
    if (devices) {
      devices.each {
        log.debug "configure device ${it.name}"
        it.configure()
      }
    } else {
      addDeviceIfSet(blindId0, blindName0)
      addDeviceIfSet(blindId1, blindName1)
      addDeviceIfSet(blindId2, blindName2)
      addDeviceIfSet(blindId3, blindName3)
      addDeviceIfSet(blindId4, blindName4)
      addDeviceIfSet(blindId5, blindName5)
      addDeviceIfSet(blindId6, blindName6)
      addDeviceIfSet(blindId7, blindName7)
      addDeviceIfSet(blindId8, blindName8)
      addDeviceIfSet(blindId9, blindName9)
      addDeviceIfSet(blindId10, blindName10)
      addDeviceIfSet(blindId11, blindName11)
    }
  } catch (e) {
    log.error "Error creating device: ${e}"
  }
}

private addDeviceIfSet(blindId, blindName) {
  if (blindId) {
    def hostHex = convertIPtoHex(powerviewHubIP)
    def portHex = convertIntToHex(80)
    def idHex = convertIntToHex(blindId)
    log.debug "Add blind with id: $blindId and name $blindName"
    addChildDevice("xyy", "Powerview Blind", "$hostHex:$portHex:$idHex",
                   hubName.id,
                   [name: blindId, label: blindName, completedSetup: true])
  }
}

private String convertIPtoHex(ipAddress) {
  String hex = ipAddress.tokenize( '.' ).collect {
                 String.format( '%02x', it.toInteger() )
               }.join()
  log.debug "IP address $ipAddress hex $hex"
  return hex
}

private String convertIntToHex(value) {
  String hex = String.format( '%04x', value.toInteger() )
  log.debug "value $value hex $hex"
  return hex
}
