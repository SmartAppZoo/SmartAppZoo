/**
 *  Refresh All
 *
 *  Copyright 2015 Herbert Carroll
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
        name: "Refresh All",
        namespace: "herbcarroll",
        author: "Herbert Carroll",
        description: "Refresh all devices",
        category: "My Apps",
        iconUrl: "https://raw.githubusercontent.com/herbcarroll/resources/master/Button-Refresh-icon.png",
        iconX2Url: "https://raw.githubusercontent.com/herbcarroll/resources/master/Button-Refresh-icon.png",
        iconX3Url: "https://raw.githubusercontent.com/herbcarroll/resources/master/Button-Refresh-icon.png")


preferences {
    page(name: "selectDevices", install: false, uninstall: true, nextPage: "viewRestUrl") {
        section ("Optional control button to tie to other actions...") {
            input "button", "capability.momentary", required : false
        }
        section ("Devices to refresh...") {
            input "devices", "capability.refresh", required : true, multiple : true
        }
    }
    page(name: "viewRestUrl")

}


mappings {
    path("/refresh") {action: [GET: "refresh"]}
    path("/link") {action: [GET: "link"]}
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
    if ( button )
        subscribe(button, "momentary.pushed", buttonHandler);
    subscribe(app, appTouch);
}

def appTouch(evt)
{
    log.debug "touch~! $evt";
    refresh();
}
def buttonHandler(evt) {
    refresh();
}

def refresh() {
    log.debug "refresh";
    devices*.refresh();
    return [ status : "ok" ];
}

def viewRestUrl() {
    def returnPath ="selectDevices";

    dynamicPage(name: "viewRestUrl", title: "Refresh URL", install:true, nextPage: null) {
        section() {
            paragraph "Here you can aquire the URL to trigger a refresh of all devices.  It can be used in a browser or third party poller."
            href url:"${generateURL("link").join()}", style:"embedded", required:false, title:"URL", description:"Tap to view, then click \"Done\""
        }

        section() {
            paragraph "Optionally, send SMS containing the URLto a phone number. The URL will be sent in two parts because it's too long."
            input "phone", "phone", title: "Which phone?", required: false
        }

        section() {
            href returnPath, title:"Return to settings"
        }
    }
}

def link() {
    def appCommand="refresh";
    if (!params.access_token)
        return ["You are not authorized to view OAuth access token"];
    render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8"/><meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head><body style="margin: 0;"><div style="padding:10px">${app.name} URL:</div><textarea rows="9" cols="30" style="font-size:10px; width: 100%">${generateURL(appCommand).join()}</textarea><div style="padding:10px">Copy the URL above and tap Done.</div></body></html>"""
}

def generateURL(path) {
    log.debug "resetOauth: $settings.resetOauth, $resetOauth, $settings.resetOauth"

    if (settings.resetOauth) {
        log.debug "Reseting Access Token"
        state.accessToken = null
    }

    if (settings.resetOauth || !state.accessToken) {
        try {
            createAccessToken()
            log.debug "Creating new Access Token: $state.accessToken"
        } catch (ex) {
            log.error "Did you forget to enable OAuth?"
            log.error ex
        }
    }

    ["https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/$path", "?access_token=${state.accessToken}"]
}

