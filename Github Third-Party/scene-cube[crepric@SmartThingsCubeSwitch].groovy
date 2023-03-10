/**
 *  Scene Cube
 *
 *  Copyright 2017 Riccardo Crepaldi
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
    name: "Scene Cube",
    namespace: "crepric",
    author: "Riccardo Crepaldi",
    description: "Uses the motion sensor inside a plastic photo cube to control 6 scenes in a given room.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
    section("Cube sensor:") {
         input "cube_sensor", "capability.accelerationSensor", required: true, title: "Which sensor is inside the cube?"
    }
    section("Which lights?") {
        input "lights", "capability.colorTemperature", multiple: true
    }
    section("Scene 1") {
		input (name:"temp_1", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_1", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
    }
    section("Scene 2") {
		input (name:"temp_2", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_2", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
    }
    section("Scene 3") {
		input (name:"temp_3", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_3", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
    }
    section("Scene 4") {
		input (name:"temp_4", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_4", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
    }
    section("Scene 5") {
		input (name:"temp_5", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_5", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
    }
    section("Scene 6") {
		input (name:"temp_6", type:"number", range: "2200..6500", required: true, title: "What is the light temperature?")
        input (name:"bright_6", type:"number", range: "0..100", required: true, title: "What is the brightness level?")
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
	log.debug("Initializing")
	unsubscribe()
    subscribe(cube_sensor, "currentFace", cubeMotionHandler)
}

def cubeMotionHandler(evt) {
	log.debug (evt.name + " " + evt.value + " " + evt.isStateChange())
	// Delete previously scheduled event if face is moved in the meantime.
	unschedule(setScene)
    // Schedule new event if cube is not moved within the next 2 seconds.
	runIn(2, "setScene", [data: [current_face: evt.value]])
}

def getSceneValues(scene_code) { 
    def temp
    def brightness
    def on
    log.debug "Adding scene code: " + scene_code
    switch(scene_code) {
    	case "1":
        	temp = temp_1
            brightness = bright_1
            break
        case "2":
            temp = temp_2
            brightness = bright_2
            break
        case "3":
            temp = temp_3
            brightness = bright_3
            break
        case "4":
            temp = temp_4
            brightness = bright_4
            break
        case "5":
            temp = temp_5
            brightness = bright_5
            break
        case "6":
            temp = temp_6
            brightness = bright_6
            break
        default:
            log.debug "This should never happen."
    }
    on = brightness > 0
    def res = ['temp': temp, 'brightness': brightness, 'on': on]
    log.debug "New setting " + res
    return res
}

def setScene(data) {
	def current_face = data.current_face
    def new_scene_values = getSceneValues(current_face)
    log.debug "New Scene: " + new_scene_values
    if (current_face == state.current_face) {
        log.debug "No change. Returning."
        return
    }
    if (new_scene_values.on) {
        lights*.setLevel(new_scene_values.brightness)
        lights*.setColorTemperature(new_scene_values.temp)
    } else {
        lights*.off()
    }
    state.current_face = scene_code
}

