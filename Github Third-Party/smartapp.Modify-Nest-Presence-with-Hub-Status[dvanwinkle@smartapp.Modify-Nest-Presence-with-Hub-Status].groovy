/**
 *  Modify Nest Presence with Hub Status
 *
 *  Copyright 2014 Dan VanWinkle
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
  name: "Modify Nest Presence with Hub Status",
  namespace: "dvanwinkle",
  author: "Dan VanWinkle",
  description: "Modify Nest Presence with Hub Status",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
  section("Change the status of the following Nest...") {
   input("nest", "device.Nest", required: true)
  }
  section("To home with the following mode...") {
   input("presentMode", "mode", title: "Mode?", required: false)
  }
  section("And to away with the following mode...") {
    input("awayMode", "mode", title: "Mode?", required: false)
  }
}

def installed() {
  log.debug("Installed with settings: ${settings}")
    
  initialize()
}

def updated() {
  log.debug("Updated with settings: ${settings}")
  
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(location, locationChanged)
  log.debug("Starting to update ${nest.displayName} to home with mode ${presentMode} and to away with mode ${awayMode}.")
}

def locationChanged(evt) {
  def currentPresence = nest.currentPresence
  
  if (currentPresence != "present" && presentMode == evt.value) {
    log.debug("Changing ${nest} to home")
    nest.present()
  } else if (currentPresence != "away" && awayMode == evt.value) {
    log.debug("Changing ${nest} to away")
    nest.away()
  }
}
