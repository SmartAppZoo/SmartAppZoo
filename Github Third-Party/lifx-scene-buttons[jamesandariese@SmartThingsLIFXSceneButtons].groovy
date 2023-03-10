/**
 *  LIFX Scene Buttons
 *
 *  Copyright 2017 James Andariese
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
    name: "LIFX Scene Buttons",
    namespace: "jamesandariese",
    author: "James Andariese",
    description: "Activate LIFX scenes by pressing a button.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
}


preferences {
    page(name: "page1", title: "LIFX Scene Connect", uninstall: true) {
        section("LIFX API Connect") {
            input "bearer", "text", title: "Bearer Token", submitOnChange: true
        
            href(name: "toSecondPage",
                 page: "page2",
                 title: "Continue",
                 description: "Load scenes from LIFX Cloud")
}
    }
    page(name: "page2", title: "Select Scenes", install:true, uninstall: true)
}

def page2() {
    def params = [
    	uri: "https://api.lifx.com/v1/scenes",
        headers: [
        	"Authorization": "Bearer $bearer"
        ]
    ]
    atomicState.done = false
    log.debug params
    try {
    	httpGet(params, { resp ->
    		if (resp.isSuccess()) {
        		def lights = [:]
        		resp.data.each {
            		def name = it['name']
            		def uuid = it['uuid']
            		log.debug "$name $uuid"
               		lights[uuid] = name
        		}
            	atomicState.lights = lights
        		atomicState.success = true
        	} else {
        		atomicState.success = false
        		atomicState.errorMessage = resp.status
        	}
        	atomicState.done = true
    	})
    } catch (e) {
    	atomicState.success = false
        atomicState.errorMessage = "Failed: ${e}"
    }
    dynamicPage(name: "page2") {
                if (atomicState.success) {
                        section("LIFX Groups") {
            atomicState.lights.each { uuid, name ->
                input(name: "scene_" + uuid, type: "bool", title: name)
            }
        }
        section("Restart") {
        	href(name: "toFirstPage",
                 page: "page1",
                 title: "Restart",
                 description: "Enter a new Bearer token")
		}
            } else {
            section("Error") {
                href(name: "toFirstPage",
                     page: "page1",
                     title: "Retry",
                     description: "There was an error loading your scenes")
            }
            }

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

def activateScene(child, power, duration=0.0) {
	log.debug "activateScene"
    log.debug child
    def uuid = child.deviceNetworkId.split('[|]')[1]
    def body = [:]
    if (!power) {
    	body["ignore"] = ["power"]
    }
    if (duration > 0) {
    	body["duration"] = (double)duration
    }
    httpPutJson([
        "uri": "https://api.lifx.com/v1/scenes/scene_id:$uuid/activate",
        "headers": [
            "Authorization": "Bearer $bearer"
        ],
        "body": body
    ], {resp ->
    	log.debug(resp)
    })
}

def stringListContainsString(l, s) {
    def res = false
    l.each {
        if (it.toString().equals(s.toString())) {
            res = true
        }
    }
    return res
}

def initialize() {
	def lights = atomicState.lights

	def devicesWanted = []

    settings.each { key, value ->
     	if (key.startsWith("scene_")) {
        	if (!lights.containsKey(key.split('_')[1])) {
            	log.debug "removing invalid $key"
            } else if (value) {
            	def uuid = key.split('[_]')[1]
            	log.debug "creating device for $uuid"
            	devicesWanted << "LIFXScene|$uuid|${app.id}"
            }
        } else {
            log.debug "skipping $key"
        }
    }
    
    log.debug "devices to ensure are installed: $devicesWanted"
    getChildDevices().each { device ->
        if (devicesWanted.find {
        	log.debug "comparing $it and ${device.deviceNetworkId}"
        "${it}" == "${device.deviceNetworkId}"} == null) {
            log.debug "want to delete ${device.deviceNetworkId}"
        	deleteChildDevice(device.deviceNetworkId)
        }
    }
    // this step is done separately in case there are two devices with the same dni
    getChildDevices().each { device ->
        devicesWanted.removeAll { "${it}" == "${device.deviceNetworkId}" }
    }
    log.debug "devices left to be installed: $devicesWanted"

    devicesWanted.each {
    	def label = lights[it.split('[|]')[1]]
    	addChildDevice("LIFX Scene Button", it, null, ["label": label, "completedSetup": true])
    }
}

// TODO: implement event handlers