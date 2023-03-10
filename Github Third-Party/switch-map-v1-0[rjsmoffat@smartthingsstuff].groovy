/**
 *  Switch Map v1.0
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
    name: "Switch Map v1.0",
    namespace: "rjsm",
    author: "Stuart Moffat",
    description: "This app creates collective switches",  
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {

    section("Select switches to marry") {
        input "switches", "capability.switch", multiple: true, required: true, title: "Select"
    }

}

def installed(){
	log.debug "Subscribing ${switches}"
    subscribe(switches, "switch.on", switchOnHandler)
    subscribe(switches, "switch.off", switchOffHandler)  
}

def updated(){
    unsubscribe()
    log.debug "Subscribing ${switches}"
    subscribe(switches, "switch.on", switchOnHandler)
    subscribe(switches, "switch.off", switchOffHandler) 
}

def switchOnHandler(evt) {
	
    log.debug "Sending switches on."
	switches*.on()
}

def switchOffHandler(evt) {

    log.debug "Sending switches off."
    switches*.off()
}