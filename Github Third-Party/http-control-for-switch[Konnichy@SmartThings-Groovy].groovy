/**
 *  HTTP control for Switch
 *
 *  Note: Send requests on TCP port 39500.
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

import groovy.json.JsonSlurper

definition(
    name: "HTTP control for Switch",
    namespace: "konnichy",
    author: "Konnichy",
    description: "Control a Switch device with HTTP requests.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment15-icn@2x.png"
)

preferences {
    section("Select your device") {
        input(name: "switch_to_control", type: "capability.switch", title: "Switch to control", required: false, submitOnChange: true)
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
    // Source: https://community.smartthings.com/t/poll-or-subscribe-example-to-network-events/72862/15
    subscribe(location, null, handleLANEvent, [filterEvents:false])
}

def handleLANEvent(event)
{
    def prefix = "/switch"
    def message = parseLanMessage(event.value)
    
    // Get the HTTP path used on the first header line
    if (!message.header) {
        log.error "Message with no header received: ${message}"
        return
    }
    def path = message.header.split("\n")[0].split()[1]

    def decoded_path = URLDecoder.decode(path, "UTF-8")
    log.info "Received HTTP request on URL ${decoded_path}"
    if (decoded_path == prefix + "/" + switch_to_control + "?on") {
        log.info "Switching '${switch_to_control}' ON..."
        switch_to_control.on()
    } else if (decoded_path == prefix + "/" + switch_to_control + "?off") {
        log.info "Switching '${switch_to_control}' OFF..."
        switch_to_control.off()
    } else if (decoded_path == prefix || decoded_path.startsWith(prefix + "/")) {
        log.error "URL is invalid or incomplete"
    } else {
        log.warn "Message not for this SmartApp, ignoring."
    }
    
    return "yes"
}
