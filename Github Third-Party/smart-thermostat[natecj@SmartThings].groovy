/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
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
  name: "Smart Thermostat",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Control multiple Thermostat's heating and cooling settings based on the current mode.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page(name: "rootPage")
  page(name: "thermostatPage")
}

def rootPage() {
  dynamicPage(name: "rootPage", title: "", install: true, uninstall: true) {
    section("How many Schedules?") {
      paragraph "A schedule is a heat & cool setpoint for a single thermostat which is applied for the selected mode(s)"
      input name: "scheduleCount", title: "Number of Schedules", type: "number", multiple: false, required: true, submitOnChange: true
    }
    if (scheduleCount > 0) {
      section('Thermostat Schedules') {
        (1..scheduleCount).each { index->
          href([
            name: "toThermostatPage${index}",
            page: "thermostatPage",
            params: [number: index],
            required: false,
            description: thermostatHrefDescription(index),
            title: thermostatHrefTitle(index),
            state: thermostatPageState(index)
          ])
        }
      }
      section {
        label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
      }
    }
  }
}

def thermostatPage(params) {
  dynamicPage(name:"thermostatPage") {
    def i = getThermostat(params);
    section("Thermostat Schedule #${i}") {
      input("thermostatDevice${i}", "capability.thermostat", title: "Thermostat", required: false, defaultValue: settings."thermostatDevice${i}")
      input(name: "thermostatHeatTemp${i}", type: "number", title: "Heat Point", required: false, defaultValue: settings."thermostatHeatTemp${i}")
      input(name: "thermostatCoolTemp${i}", type: "number", title: "Cool Point", required: false, defaultValue: settings."thermostatCoolTemp${i}")
      input(name: "thermostatModes${i}", type: "mode", title: "Modes", multiple: true, required: false, defaultValue: settings."thermostatModes${i}")
    }
  }
}


def installed() {
  log.trace "installed()"
  initialize()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  initialize()
  changeLocationMode(location.mode)
}

def initialize() {
  log.trace "initialize()"
  subscribe(location, changedLocationHandler)
}

def changedLocationHandler(evt) {
  changeLocationMode(evt.value)
}

def changeLocationMode(mode) {
  log.trace "changeLocationMode($mode)"
  (1..scheduleCount).each { index->
    if (settings."thermostatModes${index}"?.contains(mode)) {
      def activeThermostat = settings."thermostatDevice${index}"
      if (activeThermostat) {

        def oldHeat = activeThermostat.latestValue("heatingSetpoint")
        def oldCool = activeThermostat.latestValue("coolingSetpoint")
        def newHeat = settings."thermostatHeatTemp${index}" ?: oldHeat
        def newCool = settings."thermostatCoolTemp${index}" ?: oldCool
        if (oldHeat != newHeat || oldCool != newCool) {
          if (oldHeat != newHeat) {
            activeThermostat.setHeatingSetpoint(newHeat)
          }
          if (oldCool != newCool) {
            activeThermostat.setCoolingSetpoint(newCool)
          }
          log.debug "[$activeThermostat] Changing $oldHeat / $oldCool to $newHeat / $newCool"
          activeThermostat.poll()
        } else {
          log.debug "[$activeThermostat] Already set to $newHeat / $newCool"
        }
        
      }
    }
  }
}




def thermostatHrefTitle(index) {
  if (settings."thermostatDevice${index}") {
    deviceLabel(settings."thermostatDevice${index}")
  } else {
    "Thermostat ${index}"
  }
}

def thermostatHrefDescription(index) {
  def description = ""
  if (settings."thermostatModes${index}") {
    description += ("" + settings."thermostatModes${index}") + " "
  }
  if (settings."thermostatHeatTemp${index}") {
    description += "Low: " + settings."thermostatHeatTemp${index}" + " "
  }
  if (settings."thermostatCoolTemp${index}") {
    description += "High: " + settings."thermostatCoolTemp${index}" + " "
  }
  description
}

def thermostatPageState(index) {
  (isThermostatConfigured(index) ? 'complete' : 'incomplete')
}

def isThermostatConfigured(index) {
  def deviceExists = (settings."thermostatDevice${index}" != null)
  def modesExists = ((settings."thermostatModes${index}"?.size() ?: 0) > 0)
  (deviceExists && modesExists)
}

def deviceLabel(device) {
  try {
    device.label ?: device.name
  } catch(all) {
    "Invalid Device"
  }
}

def getThermostat(params) {
  def i = 1
  // Assign params to i.  Sometimes parameters are double nested.
  if (params.number) {
    i = params.number
  } else if (params.params){
    i = params.params.number
  } else if (state.lastThermostat) {
    i = state.lastThermostat
  }

  //Make sure i is a round number, not a float.
  if ( ! i.isNumber() ) {
    i = i.toInteger();
  } else if ( i.isNumber() ) {
    i = Math.round(i * 100) / 100
  }
  state.lastThermostat = i
  return i
}
