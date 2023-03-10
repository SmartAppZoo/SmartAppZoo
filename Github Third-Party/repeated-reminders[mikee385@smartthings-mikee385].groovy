/**
 *  Repeated Reminders
 *
 *  Copyright 2019 Michael Pierce
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
    name: "Repeated Reminders",
    namespace: "mikee385",
    author: "Michael Pierce",
    description: "Provides reminders when a switch is turned on that repeat at a specified interval until the switch is turned off.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Family/App-QualityTimeTracker@2x.png",
    singleInstance: true)

preferences {
    page(name:"settings")
}

def settings() {
    dynamicPage(name: "settings", title: "Reminders", install: true, uninstall: true, submitOnChange: true) {
        section() {
            app(name: "Reminders", appName: "Repeated Reminders - Child", namespace: "mikee385", title: "Add New Reminder", multiple: true)
        }
    }
}

def installed() {}

def updated() {}