/**
 *  Change Mode from URL
 *
 *  Copyright 2014 Eric Roberts
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
    name: "Change Mode from URL",
    namespace: "baldeagle072",
    author: "Eric Roberts",
    description: "This will allow you to change to the specified mode when you GET the url associated with the app.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Mode") {
    	input "changeFromMode", "mode", title: "Mode to change from", multiple: false, required: true
		input "changeToMode", "mode", title: "Mode to change to", multiple: false, required: true
        input "changeFromModeDark", "mode", title: "Mode to change from - dark", multiple: false, required: true
		input "changeToModeDark", "mode", title: "Mode to change to - dark", multiple: false, required: true
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
	if (!state.accessToken) {
        createAccessToken()
    }
    getURL(null)
}

mappings {
    path("/change") {
    	action: [GET: "changeMode"]
    }
}

def getURL(e) {
    log.debug("getURL")
    def url = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/change?access_token=${state.accessToken}"
    log.debug " url: $url"
}

def changeMode() {
	def newMode
    if (settings.changeFromMode == location.mode) {
    	newMode = settings.changeToMode
    } else if (settings.changeFromModeDark == location.mode) {
    	newMode = settings.changeToModeDark
    } else {
    	newMode == location.mode
    }
	
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			log.debug("has changed the mode to '${newMode}'")
		}
		else {
			log.debug("tried to change to undefined mode '${newMode}'")
		}
	}
}