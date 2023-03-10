/**
 *  Adjust Temperature
 *
 *  Copyright 2017 Zach Dunton
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
  name: "Color Temperature Adjuster",
  namespace: "fenfir",
  author: "Zach Dunton",
  description: "Sets color temperature based on brightness, with range limit and bias",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
  section("Settings") {
    input "outMin", "number", title: "Minimum color temperature in K", type: number, defaultValue: 2700
    input "outMax", "number", title: "Maximum color temperature in K", type: number, defaultValue: 6500
    input "levelBias", "number", title: "Bias [-100, 100] (positive is colder negative is warmer)", defaultValue: 0, type: number, range: "-100..100"
  }

  section("Adjust this light") {
    input "thelight", "capability.colorTemperature", required: true
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
  subscribe(thelight, "level", brightnessChangedHandler)
}

def brightnessChangedHandler(evt) {
  log.debug "brightnessChangedHandler called: $evt"

  def level = thelight.currentState("level")
  def colorTemp = mapLevelToColorTemp(level.value.toInteger(), 0, 100)

  log.debug "Setting color temperature to $colorTemp"
  thelight.setColorTemperature(colorTemp)
}

def mapLevelToColorTemp(input, inMin, inMax) {
  def inRange = inMax - inMin
  def outRange = outMax - outMin

  log.debug "Input: $input, Input Range: $inRange, OutputRange: $outRange"

  def level = input / inRange
  def outAdj = (int)(level * outRange) + levelBias

  return outMin + outAdj
}
