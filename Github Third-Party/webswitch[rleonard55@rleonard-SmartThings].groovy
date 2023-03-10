/**
 *  WebSwitch
 *
 *  Copyright 2017 Rob Leonard
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
    name: "WebSwitch",
    namespace: "rleonard55",
    author: "Rob Leonard",
    description: "Adds endpoints to enable a switch to be controlled by a webrequest.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name:"page1", install: true, uninstall: true){
        section ("Allow external service to control these things...") {
            input "switches", "capability.switch", multiple: true, required: true
            input "momCode", "text"
        }
    }
    page(name:"page2")
}

def page2() {
    page(name:"page2",install: true, uninstall: true){
    	apiSection()
    }
}


mappings {
    path("/switches/:code/:command") {
        action: [
            GET: "updateSwitches"
        ]
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}
def updated() {
	log.debug "Updated with settings: ${settings}"
    log.debug "app: ${app}"
	log.debug "id: ${app.id}"
    log.debug "secret: ${app.secret}"
    log.debug  apiServerUrl("/api/token/${state.AccessToken}/smartapps/installations/${app.id}/")
	unsubscribe()
	initialize()
}
def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

def HtmlSection(url, prompt= "Click here to open", desctiption=null, itemRequired= false) {
    if(description == null)
    {
    	section() {
            href(title: prompt,
                 description: "Opens ${url}",
                 required: itemRequired,  
                 style: "page",
                // image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                 url: url)
        }
    }
    else
    {
        section() {
            href(title: prompt,
                 description: desctiption,
                 required: itemRequired,
                 style: "page",
                // image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
                 url: url)
        }
	}
}
def apiSection(prompt= "Click here to see the API page") {
	
    if(state?.AccessToken == null)
		state.AccessToken = createAccessToken()
    
    def url = apiServerUrl("/api/token/${state.AccessToken}/smartapps/installations/${app.id}/")
    
    section() {
    	href(title: prompt, required: true, style: "page", url: url)
	}
}

void updateSwitches() {
	log.debug("Update")
    // use the built-in request object to get the command parameter
    def command = params.command
	def code = params.code
    log.debug "Received command: ${command} from code: ${code}"
    
    if(code != momCode)
    	return
    
    switch(command) {
        case "on":
            switches.on()
            break
        case "off":
            switches.off()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}
