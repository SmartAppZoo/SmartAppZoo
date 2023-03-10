/**
 *  SmartCourtesyLights2
 *
 *  Copyright 2020 James Simmonds
 *
 *  Licensed under GPLv3
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 */

definition(
    name: "SmartCourtesyLights2",
    namespace: "jimbobdog",
    author: "James Simmonds",
    description: "Smart Courtesy Lights v2, if dark (sun is down) and selected switch is off (out of courtesy mode) will run the selected 'on' routine and after N minutes run the 'off' routine",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: false)


preferences {
    page(name: "configure")
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
	init()
	subscribe(location, "sunrise", sunRiseHandler)
    subscribe(location, "sunset", sunSetHandler)
    subscribe(sourceMotion, "motion.active", motionActiveHandler)
    subscribe(sourceMotion, "motion.inactive", motionInActiveHandler)
}

def init() {
	state.appPrefix = "** SCLv2 **: "
	state.sundown = getSunDownState();
    state.autoSwitchOffRequired = false;
}

def configure() {
    dynamicPage(name: "configure", title: "Configure switch and routines", install: true, uninstall: true) {

        section("When motion from...") {
             input "sourceMotion", "capability.motionSensor", required: true, multiple: true
        }

        section("Main courtesy switch/light...") {
            input "targetSwitch", "capability.switch", required: true
            input "targetSwitchOffAfterMinutes", "number", title: "Off after (minutes)?", defaultValue: "2", required: true
        }

        // get the available actions
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            // sort them alphabetically
            log.trace actions
            actions.sort()
            
            section("Select 'On' Routine") {                
                // use the actions as the options for an enum input
                input "onRoutine", "enum", title: "Select a routine to execute", options: actions, required: true
            }
            section("Select 'Off' Routine") {
                log.trace actions
                // use the actions as the options for an enum input
                input "offRoutine", "enum", title: "Select a routine to execute", options: actions, required: true
            }
        }
    }
}

def getSunDownState() {
    def ssToday = getSunriseAndSunset() 
    def now = new Date()
        
    def sundown = (now >= ssToday.sunset) || (now < ssToday.sunrise)
    log.debug "${state.appPrefix} sunrise: ${ssToday.sunrise}, sunset: ${ssToday.sunset}, Is Sun Down? ${sundown}"

    return sundown
}

def motionActiveHandler(evt) {		
	def targetCurrentState = targetSwitch.switchState.value
	log.debug "${state.appPrefix} motion detected! targetSwitch state: ${targetCurrentState}"
        
    unschedule()
    
    if (state.sundown && targetCurrentState == "off") {
    	runOnRoutine()
    } else {
    	log.debug "${state.appPrefix} it's either daylight or the switch is already on, switch-off not required!"
    }    
}

def motionInActiveHandler(evt) {
	log.debug "${state.appPrefix} motion ceased; autoSwitchOffRequired=${state.autoSwitchOffRequired}"
    
	if (state.autoSwitchOffRequired) {
		log.debug "${state.appPrefix} scheduling switch off in ${targetSwitchOffAfterMinutes} minute(s)..."
    	runIn(targetSwitchOffAfterMinutes * 60, runOffRoutine)
    }
}

def runOnRoutine() {
	location.helloHome?.execute(settings.onRoutine)
    
    state.autoSwitchOffRequired = true;
    log.debug "${state.appPrefix} 'on' routine '${settings.onRoutine}' executing!"
}

def runOffRoutine() {
	location.helloHome?.execute(settings.offRoutine)
    
    state.autoSwitchOffRequired = false;
    log.debug "${state.appPrefix} 'off' routine '${settings.offRoutine}' executing!"
}

def sunRiseHandler(evt) {
	state.sundown = false;
    log.debug "${state.appPrefix} sunrise! sundown=${state.sundown}"
}

def sunSetHandler(evt) {
	state.sundown = true;
    log.debug "${state.appPrefix} sunset! sundown=${state.sundown}"
}