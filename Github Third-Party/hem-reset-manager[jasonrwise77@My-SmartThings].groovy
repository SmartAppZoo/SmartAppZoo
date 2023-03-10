/**
 *  HEM Reset Manager
 *
 *  Copyright 2018
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
 *
 */
definition(
    name: "HEM Reset Manager",
    namespace: "jasonrwise77",
    author: "jasonrwise77",
    description: "Resets the Energy Monitor Daily on a specified time",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Daily.png",
    iconX2Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Daily.png",
    iconX3Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Daily.png")

/**********************************************************************************************************************************************/
preferences {
	page(name: "main")
}

page name: "main"
            def main() {
                dynamicPage (name: "main", title: "", install: true, uninstall: true) { 
                section("Hem Profiles",  uninstall: false){
                app(name: "profiles daily", appName: "HEM Daily Reset", namespace: "jasonrwise77", title: "Create a new Daily Reset Profile", multiple: true,  uninstall: false, image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Daily.png")
		app(name: "profiles monthly", appName: "HEM Monthly Reset", namespace: "jasonrwise77", title: "Create a new Monthly Reset Profile", multiple: true,  uninstall: false, image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/HEM%20Monthly.png")
                 }
		 }
}

/************************************************************************************************************
		Base Process
************************************************************************************************************/
def installed() {
	if (debug) log.debug "Installed with settings: ${settings}"
    initialize()
}
def updated() { 
	if (debug) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}
def initialize() {
		state.esProfiles = state.esProfiles ? state.esProfiles : []
        def children = getChildApps()
}

def getProfileList(){
		return getChildApps()*.label
}
