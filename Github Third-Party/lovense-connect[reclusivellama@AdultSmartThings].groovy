/**
*  Lovense Connect
*
*  Copyright 2020 reclusivellama
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
  name: "Lovense Connect",
  namespace: "reclusivellama",
  author: "Reclusive Llama",
  description: "Lovense Connect",
  category: "",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  singleInstance: true
) {
  appSetting "token"
}

preferences {
  page(name: "mainPage", title: "Setup", content: "devicePage", install: false)
}

def devicePage() {
  def devices = getDevices();
  dynamicPage(name:"devicePage", title: "Choose Devices", install: true) {
    section("Devices") {
      input "devices", "enum", title: "Select Device(s)", required: false, multiple: true, options: devices
    }
  }
}


import groovy.json.JsonSlurper

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
  // TODO: subscribe to attributes, devices, locations, etc.
}

def getDevices() {
  def getParams = [
    uri: "https://api.lovense.com",
    path: "/api/lan/getToys"
  ]
  def devices = []
  httpGet(getParams) { resp ->
    def data = new JsonSlurper().parseText(bytesToString(resp.data))
    data.each { key, value  ->
      log.debug key
      log.debug value
    }
  }
}
