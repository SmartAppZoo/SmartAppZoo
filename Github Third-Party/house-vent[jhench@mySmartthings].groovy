/**
 *  Whole House Fan
 *
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
    name: "House Vent",
    namespace: "jhench",
    author: "Jacob Hench",
    description: "Power on the wemo swtiches when outside is cooler than inside and toggle is on",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png"
)


preferences {
    section("On/Off") {
        input "appEnabled", type: "bool", title: "true=On, false=Off", required: true, defaultValue: true
    }
    section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer"
	}
    
    section("Automatic Control") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer"
        input "minTemp", "number", title: "Minimum Indoor Temperature"
        input "fans", "capability.switch", title: "Vent Fan", multiple: true
    }
    section("Manual Control") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer"
        input "minTemp", "number", title: "Minimum Indoor Temperature"
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
	state.fanRunning = false;

    subscribe(outTemp, "temperature", "checkThings");
    subscribe(inTemp, "temperature", "checkThings");
}

def checkThings(evt) {
	def outsideTemp = settings.outTemp.currentValue('temperature')
    def insideTemp = settings.inTemp.currentValue('temperature')
    
    log.debug "Inside: $insideTemp, Outside: $outsideTemp, AppEnabled: $appEnabled"
    
    def shouldRun = true;
    
    if(settings.appEnabled != true) {
    	log.debug "App disabled"
    	shouldRun = false;
    }
    
    if(insideTemp < outsideTemp) {
    	log.debug "Not running due to insideTemp > outdoorTemp"
    	shouldRun = false;
    }
    
    if(insideTemp < settings.minTemp) {
    	log.debug "Not running due to insideTemp < minTemp"
    	shouldRun = false;
    }
    
    if(shouldRun && !state.fanRunning) {
    	fans.on();
        state.fanRunning = true;
    } else if(!shouldRun && state.fanRunning) {
    	fans.off();
        state.fanRunning = false;
    }
}