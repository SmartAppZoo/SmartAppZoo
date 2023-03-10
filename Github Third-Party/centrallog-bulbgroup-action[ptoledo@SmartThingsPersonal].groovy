/**
 *  CentralLog - BulbGroup Action
 *
 *  Copyright 2018 Pedro Toledo Correa
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
 
definition(
  name: "CentralLog - BulbGroup Action",
  namespace: "ptoledo",
  author: "Pedro Toledo Correa",
  description: "To inspect the \"CentralLog - BulbGroup Device\" and execute their refresh actions.",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
  section("Select your devices") {
    input "bulbGroups",  "capability.refresh", title: "Pick your BulbGroup devices",  multiple: true,  required: false
    input "bulbs",       "capability.switch",  title: "Pick your Bulbs",              multiple: true,  required: false
    input "clearSwitch", "capability.switch",  title: "Pick your Alexa clear switch", multiple: false, required: true
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

def initialize() {
  state.check = []
  state.mutex = 0
  subscribe(bulbGroups, "refresh", refreshBulbs)
  subscribe(clearSwitch, "switch.on", clear)
}

def clear(evt){
  bulbGroups.each{
    it.clear()
  }
  clearSwitch.off()
}

def getBulb(id){
  def runBulb = null
  bulbs.each{
    if(it.getId() == id){
      runBulb = it
      return true
    }
  }
  return runBulb
}

def refreshBulbs(evt) {
  def data = [name:  parseJson(evt.data).name, 
              bulb:  parseJson(evt.data).bulb, 
              coun:  0,
              check: false,
              value: parseJson(evt.data).value,
              setti: parseJson(evt.data).setting,
              tempe: parseJson(evt.data).colorTemp,
              cohue: parseJson(evt.data).hue,
              cosat: parseJson(evt.data).saturation]
  action(data)
}

def action(data) {
  if(data.coun > 20){
    log.debug "Too many tries for: ${data}"
  } else {
    data.coun = data.coun+1
    switch (data.name) {
      case "switch":
        mySwitch(data)
        break
      case "color":
        mySetColor(data)
        break
      case "level":
        mySetLevel(data)
        break
      case "colorTemp":
        mySetColorTemperature(data)
        break
      default:
        log.debug "Uncatched: ${data.name} for: ${data.bulb} with value: ${data.value}"
        break
    }
  }
}

def actionCheck() {
  if(state.check != []){
    if(state.mutex != 0){
      runIn(1, actionCheck)
    } else {
      state.mutex = 1
      action(state.check[0])
      state.check.remove(0)
      state.mutex = 0
      runIn(0, actionCheck)      
    }
  }
}

def addToCheck(data){
  data.check = true
  while(state.mutex != 0) {
    log.debug "mutex taken"
  }
  state.mutex = 1
  state.check << data
  state.mutex = 0
  runIn(2, "actionCheck") 
}

def mySwitch(data) {
  def bulb = getBulb(data.bulb)
  log.debug "try ${data} (${bulb.displayName}) Current: ${bulb.currentValue("switch")}"
  if(bulb != null && bulb.currentValue("switch") != data.value) {
    if (data.value == "on") {
      bulb.on()
    } else {
      bulb.off()
    }
    data.check = false
    runIn((data.coun/5)+2, "action", [data: data, overwrite: false])
  } else if (data.check == false) {
    addToCheck(data)
  }
}

def mySetLevel(data) {
  def bulb = getBulb(data.bulb)
  log.debug "try ${data} (${bulb.displayName}) Current: ${bulb.currentValue("level")}"
  if(bulb != null){
    if(bulb.currentValue("switch") != "on" ) {
      bulb.on()
    }
    if(bulb.currentValue("level") != data.value) {
      bulb.setLevel(data.value)
      data.check = false
      runIn((data.coun/5)+2, "action", [data: data, overwrite: false])
    } else if (data.check == false && bulb.currentValue("switch") == "on") {
      addToCheck(data)
    }
  }
}

def mySetColor(data) {
  def bulb = getBulb(data.bulb)
  log.debug "try ${data} (${bulb.displayName}) Current: ${[hue: bulb.currentValue("hue"), saturation: bulb.currentValue("saturation")]}"
  if(bulb.currentValue("switch") != "on" ) {
    bulb.on()
  }
  if(bulb != null && [hue: bulb.currentValue("hue"), saturation: bulb.currentValue("saturation")]) {
    bulb.setColor(data.value)
    data.check = false
    runIn((data.coun/5)+2, "action", [data: data, overwrite: false]) 
  } else if (data.check == false && bulb.currentValue("switch") == "on") {
    addToCheck(data)
  }
}

def mySetColorTemperature(data) {
  def bulb = getBulb(data.bulb)
  log.debug "try ${data} (${bulb.displayName}) Current: ${bulb.currentValue("colorTemperature")}"
  if(bulb.currentValue("switch") != "on" ) {
    bulb.on()
  }
  if(bulb != null && bulb.currentValue("colorTemperature") != data.value) {
    bulb.setColorTemperature(data.value)
    data.check = false
    runIn((data.coun/5)+2, "action", [data: data, overwrite: false]) 
  } else if (data.check == false && bulb.currentValue("switch") == "on") {
    addToCheck(data)
  }
}