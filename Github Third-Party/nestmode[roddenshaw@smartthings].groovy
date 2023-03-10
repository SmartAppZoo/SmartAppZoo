/**
 *  Nest Auto Home/Away
 *
 *  Copyright 2015 Rodden Shaw
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
    name: "Nest Auto Home/Away",
    namespace: "roddenshaw",
    author: "Rodden Shaw",
    description: "Sets Nest thermostat to 'present' or 'away' based on ST 'away' mode.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

  section("Change this thermostat's mode...") {
    input "thermostat", "capability.thermostat"
  }
}

def installed() {
  subscribe(location, modeChangeHandler)
}
def updated() {
  unsubscribe()
  subscribe(location, modeChangeHandler)
}
def modeChangeHandler(evt) 
{
	log.debug "mode changed to ${evt.value}"
  if(evt.value == "Away") {
    log.info("Changing to away")
    thermostat?.away()
  }
  else{
    log.info("Changing to present")
    thermostat?.present()
  }
}
