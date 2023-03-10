/**
 *  August Connect
 *
 *  Copyright 2019 brent@thebrent.net
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
    name: "August Connect",
    namespace: "thebrent",
    author: "brent@thebrent.net",
    description: "Connect August Locks via WiFi",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "apiKey"
    appSetting "installId"
}


preferences {
  page(name: "Login", title: "Login", content: "loginPage", install: false)
  page(name: "Validate", title: "Validate", content: "validatePage", install: false)
  page(name: "ListDevices", title: "Locks", content: "listDevices", install: false)
}

def loginPage() {
  if(!state.accessToken) {
    return dynamicPage(name: "Login", title: "Login", install: false, nextPage: "Validate") {
      section("Enter login credentials") {
        input name: "identifier_type", type: "enum", title: "Identifier Type", options: ["phone","email"]
        input name: "identifier", type: "string", title: "Identifier", description: "Email or phone number"
        input name: "password", type: "password", title: "Password"
      }
    }
  }
}

def validatePage() {
  getSession()
  return dynamicPage(name: "Login", title: "Validate", install: false, nextPage: "ListDevices") {
    section("Enter validation code") {
      input name: "code", type: "string", title: "Code"
    }
  }
}

def listDevices() {
}

def getSession() {
  def params = [
    uri: "https://api-production.august.com/session",
    headers: [
      'x-august-api-key': appSettings.apiKey,
      'x-kease-api-key': appSettings.apiKey,
      'Accept-Version': '0.0.1',
      'User-Agent': 'August/Luna-3.2.2',
    ],
    contentType: 'application/json',
    body: [
      installId: appSettings.installId,
      password: password,
      identifier: identifier
    ]
  ]
  httpPostJson(params, { response -> 
    state.accessToken = response.headers['x-august-access-token']
  })
}

def sendValidationCode() {
  def params = [
    uri: "https://api-production.august.com/validation/${identifier_type}",
    headers: [
      'x-august-api-key': appSettings.apiKey,
      'x-kease-api-key': appSettings.apiKey,
      'Accept-Version': '0.0.1',
      'User-Agent': 'August/Luna-3.2.2',
      'x-august-access-token': state.accessToken
    ],
    contentType: 'application/json',
    body: [
      value: identifier
    ]
  ]
  httpPostJson(params, { response -> 
    log.debug response.headers['x-august-access-token']
  })
}

def validate() {
  def params = [
    uri: "https://api-production.august.com/validate/${identifier_type}",
    headers: [
      'x-august-api-key': appSettings.apiKey,
      'x-kease-api-key': appSettings.apiKey,
      'Accept-Version': '0.0.1',
      'User-Agent': 'August/Luna-3.2.2',
      'x-august-access-token': state.accessToken
    ],
    contentType: 'application/json',
    body: [
      phone: identifier,
      code: validation_code
    ]
  ]
  httpPostJson(params, { response -> 
    log.debug response.headers['x-august-access-token']
  })
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
  // TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers