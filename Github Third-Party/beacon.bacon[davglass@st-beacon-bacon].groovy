/**
 *  Beacon Bacon
 *
 *  Copyright 2015 Dav Glass
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
    name: "Beacon Bacon",
    namespace: "davglass",
    author: "Dav Glass",
    description: "Track BT Beacons..",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
    page(name: "viewURL", uninstall: true)
}

mappings {
    if (params.access_token && params.access_token != state.accessToken) {
        path("/link") {action: [GET: "oauthError"]}
        path("/beacon") {action: [GET: "oauthError", POST: "oauthError"]}
    } else if (!params.access_token) {
        path("/link") {action: [GET: "viewLinkError"]}
        path("/beacon") {action: [GET: "oauthError"]}
    } else {
        path("/beacon") {
            action: [
                GET: "beaconHandler",
                POST: "beaconHandler"
            ]
        }
        path("/link") {action: [GET: "link"]}
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
    
}

def beaconHandler() {
    log.debug "Params: ${params}"
    log.debug "Region: ${params.region}"
    log.debug "Distance: ${params.distance}"
    log.debug "Beacon: ${params.beacon}"
    log.debug "app.distance: ${distance.toLowerCase()}"
    
    if (params.distance && params.distance.toLowerCase() == distance.toLowerCase()) {
        log.debug "Turning on lights.."
        switches.each {
            it."${status.toLowerCase()}"();
        }
    }
}

def oauthError() {[error: "OAuth token is invalid or access has been revoked"]}

def viewLinkError() {[error: "You are not authorized to view OAuth access token"]}

def viewURL() {
    dynamicPage(name: "viewURL", title: "${title ?: location.name} URL", install:true, nextPage: null) {
        section("Light to turn on?") {
            input "switches", "capability.switch", multiple: true
        }
        section("Light On or Off") {
            input(name: "status", type: "enum", title: "On/Off", options: ["On", "Off"], required: true)
        }
        section("Minimum Distance") {
            input(name: "distance", type: "enum", title: "Distance", options: ["Immediate", "Near", "Far"], required: true)
        }
        section() {
            paragraph "Copy the URL below and add it to the Beecon app for alerts."
            href url:"${generateURL("link").join()}", style:"embedded", required:false, title:"URL", description:"Tap to view, then click \"Done\""
        }
    }
}

def generateURL(path) {
    log.debug "Generating URL for /${path}"
    
    if (!state.accessToken) {
        try {
            createAccessToken()
            log.debug "Creating new Access Token: $state.accessToken"
        } catch (ex) {
            log.error "Did you forget to enable OAuth in SmartApp IDE settings?"
            log.error ex
        }
    }
    
    def url = ["https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/$path", "?access_token=${state.accessToken}"]
    log.debug url.join()
    
    return url;
}
def link() {render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8" />
<meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head><body>Beacon Ping URL:<br/><textarea rows="9" cols="30" style="font-size:10px;">${generateURL("beacon").join()}</textarea><br/><br/>Copy the URL above and click Done.<br/></body></html>"""}
