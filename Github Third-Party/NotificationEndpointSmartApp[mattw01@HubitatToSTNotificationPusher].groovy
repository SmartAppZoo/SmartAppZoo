/**
 *  Notification Endpoint
 *
 *  Copyright 2018 mattw01
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
    name: "Notification Endpoint",
    namespace: "mattw01",
    author: "mattw01",
    description: "Endpoint Notification Pusher",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  page(name:"mainPage")
  page(name:"disableAPIPage")
  page(name:"enableAPIPage")
}
mappings {
  path("/notification/") {
    action: [
      GET: "handleNotification"
    ]
  }
}
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}
def uninstalled() {
	if (state.endpoint) {
		try {
			logDebug "Revoking API access token"
			revokeAccessToken()
		}
		catch (e) {
			log.warn "Unable to revoke API access token: $e"
		}
	}
}
def updated() {
	log.debug "Updated with the following settings:\n${settings}"
    log.debug "##########################################################################################"
    log.debug "smartAppURL = \"${state.endpointURL}\""
    log.debug "smartAppSecret = \"${state.endpointSecret}\""
    log.debug "The API has been setup."
    log.debug "##########################################################################################"

	unsubscribe()
	initialize()
}

def initialize() {
}

def handleNotification() {
	log.debug "handleNotification called with params: ${params}"
    if(params) {
    	if(params.message) {
        	def message = params.message
            log.debug "handleNotification sending push notification: \"${message}\""
            sendPush(message)
            return [ result: "success" ]
        }
    	else
        	log.debug "handleNotification called with no message"
    }
    else
    	log.debug "handleNotification called with no params object"
    return [ result: "error" ]
}

private mainPage() {
	dynamicPage(name: "mainPage", uninstall:true, install:true) {
		section("API Setup") {
			if (state.endpoint) {
					paragraph "API has been setup."
                    paragraph "URL:\n${state.endpointURL}"
                    paragraph "Secret:\n${state.endpointSecret}"
                    paragraph "Login to graph.api.smartthings.com to view full URL"
                    href "disableAPIPage", title: "Disable API", description: ""
			}
            else {
			paragraph "API has not been setup. Tap below to enable it."
            href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"
            }

		}
	}
}


def disableAPIPage() {
	dynamicPage(name: "disableAPIPage", title: "") {
		section() {
			if (state.endpoint) {
				try {
					revokeAccessToken()
				}
				catch (e) {
					log.debug "Unable to revoke access token: $e"
				}
				state.endpoint = null
			}
			paragraph "API token has been revoked. Tap Done to continue."
		}
	}
}

def enableAPIPage() {
	dynamicPage(name: "enableAPIPage") {
		section() {
			if (initializeAppEndpoint()) {
				paragraph "The API is now enabled. Tap Done to continue"
			}
			else {
				paragraph "It looks like OAuth is not enabled. Please login to your SmartThings IDE, click the My SmartApps menu item, click the 'Edit Properties' button for the BitBar Output App. Then click the OAuth section followed by the 'Enable OAuth in Smart App' button. Click the Update button and BAM you can finally tap Done here.", title: "Looks like we have to enable OAuth still", required: true, state: null
			}
		}
	}
}
private initializeAppEndpoint() {
	if (!state.endpoint) {
		try {
			def accessToken = createAccessToken()
			if (accessToken) {
				state.endpoint = apiServerUrl("/api/token/${accessToken}/smartapps/installations/${app.id}/")	
                state.endpointURL = apiServerUrl("/api/smartapps/installations/${app.id}/")	
                state.endpointFullURL = apiServerUrl("/api/smartapps/installations/${app.id}/notification/?message=[message text]&access_token=${accessToken}")	
                state.endpointSecret = accessToken
			}
		}
		catch(e) {
			state.endpoint = null
		}
	}
	return state.endpoint
}