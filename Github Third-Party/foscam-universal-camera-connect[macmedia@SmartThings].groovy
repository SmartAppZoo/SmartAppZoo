/**
 *  Foscam Universal Camera (Connect)
 *  Version 1.0.0 - 05/01/2016
 *
 *  Copyright 2016 Mike Elser
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
    name: "Foscam Universal Camera (Connect)",
    namespace: "macmedia",
    author: "Mike Elser",
    description: "Foscam Universal Camera (Connect)",
    category: "Safety & Security",
    iconUrl:   "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon.png",
    iconX2Url: "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon@2x.png",
    iconX3Url: "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon@2x.png")


preferences {
    page(name: "mainPage", title: "Existing Camera's", install: true, uninstall: true) {
        if(state?.installed) {
            section("Add a New Camera") {
                app "Foscam Universal Camera child", "macmedia", "Foscam Universal Camera child", title: "New Camera", page: "mainPage", multiple: true, install: true
            }
        } else {
            section("Initial Install") {
                paragraph "This smartapp installs the Foscam Video Camera Connect App so you can add multiple child video cameras. Click install / done then go to smartapps in the flyout menu and add new cameras or edit existing cameras."
            }
        }
    }
}

def parse(String description){
    info.debug("Connect Parse")
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
    state.installed = true
}
