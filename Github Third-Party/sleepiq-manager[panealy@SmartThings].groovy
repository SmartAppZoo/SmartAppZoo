/**
 *  Original work:
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
 *
 *  Modified work, expanding beyond presence capabilities:
 *  Copyright 2019 Peter Nealy <panealy@gmail.com>
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
  name: "SleepIQ Manager",
  namespace: "panealy",
  author: "Peter Nealy",
  description: "Manage sleepers across multiple beds through your SleepIQ account.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page name: "rootPage"
  page name: "findDevicePage"
  page name: "selectDevicePage"
  page name: "createDevicePage"
}

def rootPage() {
  log.trace "rootPage()"
  
  def devices = getChildDevices()
  
  dynamicPage(name: "rootPage", install: true, uninstall: true) {
    section("Settings") {
      input("login", "text", title: "Username", description: "Your SleepIQ username")
      input("password", "password", title: "Password", description: "Your SleepIQ password")
      input("interval", "number", title: "Refresh Interval", description: "How many minutes between refresh", defaultValue: 15)
    }
    section("Devices") {
      if (devices.size() > 0) {
        devices.each { device ->
          paragraph title: device.label, "${device.currentBedId} / ${device.currentMode} / ${device.currentSleepNumber}"
        }
      }
      href "findDevicePage", title: "Create New Device", description: null
    }
    section {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def statusText(status) {
  "Current Status: " + (status ? "Present" : "Not Present")
}

def findDevicePage() {
  log.trace "findDevicePage()"

  def responseData = getBedData()
  log.debug "Response Data: $responseData"
  
  dynamicPage(name: "findDevicePage") {
    if (responseData.beds.size() > 0) {
      responseData.beds.each { bed ->
        section("Bed: ${bed.bedId}") {
          def leftStatus = bed.leftSide.isInBed
          def rightStatus = bed.rightSide.isInBed
          def leftSleepNumber = bed.leftSide.sleepNumber
          def rightSleepNumber = bed.rightSide.sleepNumber
          def bothStatus = leftStatus && rightStatus
          def eitherStatus = leftStatus || rightStatus
          href "selectDevicePage", title: "Both Sides", description: statusText(bothStatus), params: [bedId: bed.bedId, mode: "Both", status: bothStatus, sleepNumber: "N/A"]
          href "selectDevicePage", title: "Either Side", description: statusText(eitherStatus), params: [bedId: bed.bedId, mode: "Either", status: eitherStatus, sleepNumber: "N/A"]
          href "selectDevicePage", title: "Left Side", description: statusText(leftStatus), params: [bedId: bed.bedId, mode: "Left", status: leftStatus, sleepNumber: leftSleepNumber]
          href "selectDevicePage", title: "Right Side", description: statusText(rightStatus), params: [bedId: bed.bedId, mode: "Right", status: rightStatus, sleepNumber: rightSleepNumber]
        }
      }
    } else {
      section {
        paragraph "No Beds Found"
      }
    }
  }
}

def selectDevicePage(params) {
  log.trace "selectDevicePage()"
  
  settings.newDeviceName = null
  
  dynamicPage(name: "selectDevicePage") {
    section {
      paragraph "Bed ID: ${params.bedId}"
      paragraph "Mode: ${params.mode}"
      paragraph "Status: ${params.present ? 'Present' : 'Not Present'}"
      paragraph "Sleep Number: ${params.sleepNumber}"
      input "newDeviceName", "text", title: "Device Name", description: "What do you want to call this presence sensor?", defaultValue: ""
    }
    section {
      href "createDevicePage", title: "Create Device", description: null, params: [bedId: params.bedId, mode: params.mode, status: params.status, sleepNumber: params.SleepNumber]
    }
  }
}

def createDevicePage(params) {
  log.trace "createDevicePage()"

  def deviceId = "sleepiq.${params.bedId}.${params.mode}"
  def device = addChildDevice("natecj", "SleepIQ Presence Sensor", deviceId, null, [label: settings.newDeviceName])
  device.setStatus(params.status)
  device.setBedId(params.bedId)
  device.setMode(params.mode)
  settings.newDeviceName = null
  
  dynamicPage(name: "selectDevicePage") {
    section {
      paragraph "Name: ${device.name}"
      paragraph "Label: ${device.label}"
      paragraph "Bed ID: ${device.currentBedId}"
      paragraph "Mode: ${device.currentMode}"
      paragraph "Presence: ${device.currentPresnce}"
    }
    section {
      href "rootPage", title: "Back to Device List", description: null
    }
  }
}


def installed() {
  log.trace "installed()"
  initialize()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  log.trace "initialize()"
  refreshChildDevices()
  schedule("* /${settings.interval} * * * ?", "refreshChildDevices")
}

def refreshChildDevices() {
  log.trace "refreshChildDevices()" 
  getBedData()
}

def getBedData() {
  log.trace "getBedData()"
    
  // Make request and wait for completion
  state.statusRequestData = null
  doStatus()
  while(state.statusRequestData == null) { sleep(1000) }
  def statusRequestData = state.statusRequestData
  state.statusRequestData = null
  
  // Process data
  processBedData(statusRequestData)
  
  // Return data
  statusRequestData
}

def getFavoriteDataCb(response, callBackData) {
  log.trace "getFavoriteDataCb()"
  
  if (response?.hasError()) {
  	log.debug "getFavoriteDataCb() - response error (${response.getErrorMessage()})"
    return
  }
 
  try {
    log.debug "getFavoriteDataCb() RESPONSE (${response}) DATA (${callBackData})"
    def resp = response?.getJson()
    def mode = callBackData.get("mode")
    def bedId = callBackData.get("bedId")
    if(response?.status == 200) {
      // update only for device associated with this callback
      for(def device : getChildDevices()) {
        if ((device.currentBedId == bedId) && (device.currentMode == mode)) {
          if (mode == "Right") {
            device.updateFavSleepNumber(resp.sleepNumberFavoriteRight)
          } else if (mode == "Left") {
            device.updateFavSleepNumber(resp.sleepNumberFavoriteLeft)
          }
        }
      }
    } else {
      log.debug "getFavoriteDataCb() Error - RESPONSE (${resp}) DATA (${callBackData})"
    }
  } catch (Exception err) {
    log.debug "getFavoriteDataCb() exception (${err})"
  }
}
  
def processBedData(responseData) {
  log.trace "processBedData()"
  if (!responseData || responseData.size() == 0) {
    log.debug "processBedData() - no response data"
    return
  }
  for(def device : getChildDevices()) {
    for(def bed : responseData.beds) {
      if (device.currentBedId == bed.bedId) {
        def statusMap = [:]
        statusMap["Both"] = bed.leftSide.isInBed && bed.rightSide.isInBed
        statusMap["Either"] = bed.leftSide.isInBed || bed.rightSide.isInBed
        statusMap["Left"] = bed.leftSide.isInBed
        statusMap["Right"] = bed.rightSide.isInBed
        statusMap["leftNumber"] = bed.leftSide.sleepNumber
        statusMap["rightNumber"] = bed.rightSide.sleepNumber
        if (statusMap.containsKey(device.currentMode)) {
          log.debug "Setting ${device.label} (${device.currentMode}) to ${statusMap[device.currentMode] ? "Present" : "Not Present"}"
          device.setStatus(statusMap[device.currentMode])
          if (device.currentMode == "Right") {
      	  	  device.updateSleepNumber(statusMap["rightNumber"])                      
              getFavoriteData(device.currentBedId, device.currentMode)
          } else if (device.currentMode == "Left") {
        	  device.updateSleepNumber(statusMap["leftNumber"])   
              getFavoriteData(device.currentBedId, device.currentMode)
          }

          break
        }
      }
    }
  }
}

private def ApiHost() { "prod-api.sleepiq.sleepnumber.com" }

private def ApiUriBase() { "https://prod-api.sleepiq.sleepnumber.com" }

private def ApiUserAgent() { "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36" }

private def getFavoriteData(bedId, mode) {
  log.trace "getFavoriteData(${bedId}, ${mode})"
  include 'asynchttp_v1'
  
  def params = [:]
  def callbackData = [:]
  try {
    params = [
      uri: ApiUriBase() + '/rest/bed/' + bedId + '/sleepNumberFavorite?_k=' + state.session?.key,
      headers: [
        'Content-Type': 'application/json;charset=UTF-8',
        'Host': ApiHost(),
        'Cookie': state.session?.cookies,
        'DNT': '1',
      ],
    ]

    // We must use async so that we can get a callback when completed. We cannot sleep for this data as we're in a for loop
    callbackData = [bedId: bedId, mode: mode]
    asynchttp_v1.get("getFavoriteDataCb", params, callbackData)
  } catch(Exception e) {
      log.trace "getFavoriteData() Error ($e) PARAMS (${params}) DATA (${callbackData})"
      doLogin()    
  }
}

def doSetSleepNumber(value, bedSide, bedId, favorite = false, alreadyLoggedIn = false) {
  log.debug "doSetSleepNumber(value=${value}, bedSide=${bedSide}, bedId=${bedId}, favorite=${favorite}, alreadyLoggedIn=${alreadyLoggedIn}"

  // Login if there isn't an active session
  if (!state.session || !state.session?.key) {
    if (alreadyLoggedIn) {
      log.error "doSetSleepNumber() Already attempted login, giving up for now"
      return
    } else {
      doLogin()
    }
  } 
  
  if (bedSide != "Right" && bedSide != "Left") {
    log.error "doSetSleepNumber() cannot set level if mode/side is set to ${bedSide}"
    return
  }
  
  def setParams, path, uri, bodyJson
  try {
    if (favorite) {
      log.trace "doSetSleepNumber requested to set favorite to " + Math.round(value/5)*5 + " on ${bedSide} side of bed for bed ID ${bedId}"
      path = "sleepNumberFavorite"
      bodyJson = new groovy.json.JsonOutput().toJson([
  		bed: bedId,
    	side: (bedSide == "Right") ? 'R' : 'L',
        sleepNumberFavorite: Math.round(value/5)*5,
        ])
    } else {
      log.trace "doSetSleepNumber requested to set number to " + Math.round(value/5)*5 + " on ${bedSide} side of bed for bed ID ${bedId}"
      path = "sleepNumber"
      bodyJson = new groovy.json.JsonOutput().toJson([
  		bed: bedId,
    	side: (bedSide == "Right") ? 'R' : 'L',
        sleepNumber: Math.round(value/5)*5,
        ])
    }
	uri = ApiUriBase() + '/rest/bed/' + bedId + '/' + path + '?_k=' + state.session?.key
    setParams = [
      uri: uri,
      body: bodyJson,
      headers: [
        'Content-Type': 'application/json',
        'Host': ApiHost(),
        'Cookie': state.session?.cookies,
        'DNT': '1',
      ],
    ]

	httpPutJson(setParams) { response ->
      if (response.status == 200) {
        log.trace "doSetSleepNumber() Success - Request was successful: ($response.status) $response.data"
      } else {
        log.trace "doSetSleepNumber() Failure - Request was unsuccessful: ($response.status) $response.data"
        state.session = null
        state.sleepNumberRequestData = [:]
      }
    }
  } catch(Exception e) {
  	log.error "doSetSleepNumber() Error ($e) PARAMS (${setParams})"
    if (!alreadyLoggedIn) {
      doLogin()    
    }
  }
}

private def doStatus(alreadyLoggedIn = false) {
  log.trace "doStatus()"

  // Login if there isnt an active session
  if (!state.session || !state.session?.key) {
    if (alreadyLoggedIn) {
      log.error "doStatus() Already attempted login, giving up for now"
      return
    } else {
      doLogin()
    }
  }

  // Make the request
  try {
    def statusParams = [
      uri: ApiUriBase() + '/rest/bed/familyStatus?_k=' + state.session?.key,
      headers: [
        'Content-Type': 'application/json;charset=UTF-8',
        'Host': ApiHost(),
        'Cookie': state.session?.cookies,
        'DNT': '1',
      ],
    ]
    httpGet(statusParams) { response -> 
      if (response.status == 200) {
        log.trace "doStatus(), FamilyStatus, Success -  Request was successful: ($response.status) $response.data"
        state.statusRequestData = response.data
      } else {
        log.trace "doStatus(), FamilyStatus, Failure - Request was unsuccessful: ($response.status) $response.data"
        state.session = null
        state.statusRequestData = [:]
      }
    }
  } catch(Exception e) {
    if (alreadyLoggedIn) {
      log.error "doStatus(), FamilyStatus, Error ($e)"
    } else {
      log.trace "doStatus(), FamilyStatus, Error ($e)"
      doLogin()    
    }
  }
}

private def doLogin() {
  log.trace "doLogin()"
  state.session = null
  state.requestData = [:]
  try {
    def loginParams = [
      uri: ApiUriBase() + '/rest/login',
      headers: [
        'Content-Type': 'application/json;charset=UTF-8',
        'Host': ApiHost(),
        'User-Agent': ApiUserAgent(),
        'DNT': '1',
      ],
      body: '{"login":"' + settings.login + '","password":"' + settings.password + '"}='
    ]
    httpPut(loginParams) { response ->
      if (response.status == 200) {
        log.trace "doLogin() Success - Request was successful: ($response.status) $response.data"
        state.session = [:]
        state.session.key = response.data.key
        state.session._k = response.data.key
        state.session.cookies = ''
        response.getHeaders('Set-Cookie').each {
          state.session.cookies = state.session.cookies + it.value.split(';')[0] + ';'
        }
        doStatus(true)
        log.trace "doLogin() session.cookies ->" + state.session.cookies
      } else {
        log.trace "doLogin() Failure - Request was unsuccessful: ($response.status) $response.data"
        state.session = null
        state.requestData = [:]
      }
    }
  } catch(Exception e) {
    log.error "doLogin() Error ($e)"
    state.session = null
    state.requestData = [:]
  }
}
