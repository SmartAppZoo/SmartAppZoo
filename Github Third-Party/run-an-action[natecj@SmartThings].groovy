/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
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
  name: "Run an Action",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Run an Action.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page(name: "selectActions")
}

def selectActions() {
  dynamicPage(name: "selectActions", title: "Select Hello Home Action to Execute", install: true, uninstall: true) {
    def actions = location.helloHome?.getPhrases()*.label
    if (actions) {
      actions.sort()
      section("Hello Home Actions") {
        input "action", "enum", title: "Select an action to execute", options: actions
      }
    }
    section() {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(location, changedLocationMode)
  subscribe(app, appTouch)
}

def changedLocationMode(evt) {
  runAction()
}

def appTouch(evt) {
  runAction()
}

def runAction() {
  location.helloHome?.execute(action)
}
