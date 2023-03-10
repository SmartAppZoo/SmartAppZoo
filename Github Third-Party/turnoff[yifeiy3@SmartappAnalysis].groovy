/**
 *  Copyright 2015 SmartThings
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
 *  Big Turn ON
 *
 *  Author: SmartThings
 */

definition(
    name: "Big Turn ON",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Turn your lights on when the SmartApp is tapped or activated.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("When this gets turned off...") {
		input "switches", "capability.switch", multiple: true
	}
    section("Turn off..."){
    	input "switchesoff", "capability.switch", multiple: true
    }
    section("When I touch the app, be active after..."){
    	input "timer", "number", required: true, title: "seconds?"
    }
}

def installed()
{
	subscribe(switches, "switch.off", offhandler)
	subscribe(location, changedLocationMode)
	subscribe(app, timedTouch)
}

def updated()
{
	unsubscribe()
    subscribe(switches, "switch.off", offhandler)
	subscribe(location, changedLocationMode)
	subscribe(app, timedTouch)
}

def offhandler(evt){
	log.debug "$switches"
    monitor?.execute("AppName: Big Turn ON, ($switches switch : on)")
    location.setMode("Away")
	switchesoff?.off()
}

def changedLocationMode(evt) {
	log.debug "changedLocationMode: $evt"
	switches?.on()
    switchesoff?.off()
}

def timedTouch(evt){
	log.debug "a timed modification of this app"
    runIn(timer, appTouch)
}

def appTouch(evt) {
	log.debug "appTouch: $evt, $switches"
	switches?.on()
    switchesoff?.off()
    location.setMode("Away")
    def att2 = switches.supportedAttributes
    att2.each{
    	log.debug "wtf... ${it.name}"
    }
    def attr = monitor.supportedAttributes
    attr.each{
    	log.debug "ok. ${it.name}, ${it.values}"
    }
}