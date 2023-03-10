/**
 *  Foyer Lights
 *
 *  Copyright 2017 Ken Cote
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
    name: "Foyer Lights",
    namespace: "KenCote",
    author: "Ken Cote",
    description: "Turn on lights in foyer based on motion",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light17-icn@3x.png")

preferences {
	section("Which lights?"){
		input "lights", "capability.switchLevel", multiple: true
	}
    section("Downstairs motion sensor to use?") {
		input "downstairsMotion", "capability.motionSensor", multiple: true
	}
     section("Upstairs motion sensor to use?") {
		input "upstairsMotion", "capability.motionSensor"
	}
}

def installed()
{
    initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize()
{
    state.upstairsOn = false;
    subscribe(downstairsMotion, "motion", LightHandler, [filterEvents: false])
    subscribe(upstairsMotion, "motion", LightHandler, [filterEvents: false])
}

def LightHandler(evt)
{
    def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
	def sunTime = getSunriseAndSunset();  
    def sunset = sunTime.sunset.format("yyyy-MM-dd HH:mm:ss", location.timeZone);
    def sunrise = sunTime.sunrise.format("yyyy-MM-dd HH:mm:ss", location.timeZone);
    
    log.debug "Current time is ${now}, sunrise is ${sunrise}, sunset is ${sunset}"

    def isUpstairsMotion = "${evt.device}".contains("Upstairs")
    def isDownstairsMotion = "${evt.device}".contains("Foyer")
    def dmState = downstairsMotion.currentState("motion")
    def umState = upstairsMotion.currentState("motion")

    log.debug "downstairsMotion value: ${dmState.value}"
    log.debug "upstairsMotion value: ${umState.value}"
    log.debug "IsUpstairs:  ${isUpstairsMotion}"
    log.debug "IsDownstairs:  ${isDownstairsMotion}"
    log.debug {evt.value}

    if (((isDownstairsMotion && dmState.value.contains("active")) || (isUpstairsMotion && umState.value == "active")) && (now > sunset || now < sunrise)) {
        log.debug "Foyer lights on"
        lights.setLevel(100)
    } else if (dmState[0].value == "inactive" && dmState[1].value == "inactive" && umState.value == "inactive") {
        log.debug "Foyer lights off"
        lights.setLevel(0)
    }
}

