/**
 *  Scene Selector
 *
 *  Copyright 2019 Ryan Gruss
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
    name: "Scene Selector",
    namespace: "grussr",
    author: "Ryan Gruss",
    description: "Set a list of scenes to choose from by pressing a button",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "apikey"
}


preferences {
	page name: "mainPage", title: "Select scenes and trigger button", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Name", install: true, uninstall: true
}

def mainPage() {
	dynamicPage(name: "mainPage"){
    	section("When this button is pressed...") {
        	input name: "triggerButton", type: "capability.button", title: "Select Button", required: true, multiple: false, submitOnChange: true
			state.buttonCount = triggerButton.currentValue('numberOfButtons')
            if(state.buttonCount<1) {
            	state.buttonCount = 1
            }
            def buttonList = []
            for(i in 1..state.buttonCount) {
            	buttonList << i
            }
            if (state.buttonCount > 1) {
            	log.debug buttonList
            	input name: "whichButton", type: "enum", title: "Which button?", required: true, multiple: false, options: buttonList
            }
        }
		section("Choose your scenes") {
            def sceneList = getScenes()
            input name: "selectedScene1", type: "enum", title: "Scene #1", required: true, multiple: false, options: sceneList
            input name: "selectedScene2", type: "enum", title: "Scene #2", required: false, multiple: false, options: sceneList
            input name: "selectedScene3", type: "enum", title: "Scene #3", required: false, multiple: false, options: sceneList
            input name: "selectedScene4", type: "enum", title: "Scene #4", required: false, multiple: false, options: sceneList
 		}
        section("(Optional) When this device is off, the scene counter resets") {
        	input name: "canaryDevices", type: "capability.switch", title: "Canary device", required: false, multiple: true, submitOnChange: true
        }
	}
}

def namePage() {
    if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def defaultLabel() {
    "Selectascene"
}

def getScenes() {
	def result = []
    def sceneIdMap = [:]
    def params = [
        uri: "https://api.smartthings.com/v1/scenes",
        headers: [ Authorization: "Bearer " + appSettings.apikey ]
    ]

    try {
        httpGet(params) { resp ->
        log.debug "response data: ${resp.data}"
        resp.data.items.each {
        	result << it.sceneName
            sceneIdMap[it.sceneName] = it.sceneId
            }
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
    state.idMap = sceneIdMap
    return result.sort()
}

def setScene(sceneId) {
    def params = [
        uri: "https://api.smartthings.com/v1/scenes/${sceneId}/execute",
        headers: [ Authorization: "Bearer " + appSettings.apikey ]
    ]

    try {
        httpPost(params) { resp ->
        log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }	
}

def getSceneId(sceneName) {
	return state.idMap[sceneName]
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
    state.lastScene = 0
    def numberOfScenes = 1
    state.activeScenes = [:]
    state.activeScenes << addScene(selectedScene1, state.activeScenes)
    state.activeScenes << addScene(selectedScene2, state.activeScenes)
    state.activeScenes << addScene(selectedScene3, state.activeScenes)
    state.activeScenes << addScene(selectedScene4, state.activeScenes)
    
    log.debug "active scenes: ${state.activeScenes}"
    if (triggerButton.currentValue("numberOfButtons") > 1) {
    	state.whichButton = whichButton
    }
    else {
    	state.whichButton = 1
    }
    subscribe(triggerButton, "button.pushed", onPush)
	subscribe(canaryDevices, "switch.off", onSwitchOff)
}

def addScene(sceneSlot, sceneMap) {
	if (sceneSlot) {
    	log.debug "Adding scene ${sceneSlot} to position ${sceneMap.size() + 1}"
    	return ["${sceneMap.size() + 1}": getSceneId(sceneSlot)]
    }
    else {
    	return null
    }
}

def onPush(evt) {
	log.debug "button pushed"
    if (evt.jsonData.buttonNumber.toInteger() != state.whichButton.toInteger()) {
    	log.debug "other button pushed.  get out of here expected ${state.whichButton} got ${evt.jsonData.buttonNumber}"
        state.lastScene = 0
        return
    }
    
    state.lastScene = state.lastScene + 1
    if (!state.activeScenes["${state.lastScene}"])
    {
    	state.lastScene = 1 
    }
    log.debug "activating scene ${state.lastScene} with id ${state.activeScenes["${state.lastScene}"]}"
    setScene(state.activeScenes["${state.lastScene}"])
}

def onSwitchOff(evt) {
	log.debug "canary device turned off"
    def allOff = true
    canaryDevices.each { device ->
    	if (device.currentValue('switch') == 'on') {
        	allOff = false
        }
    }
    if (allOff) {
    	log.debug "all canary devices off, resetting scene count"
        state.lastScene = 0
    }
}