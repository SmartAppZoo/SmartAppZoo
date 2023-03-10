/**
 *  Zipato Integration
 * 
 *  Copyright 2018 Neil Cumpstey
 * 
 *  A SmartThings smart app which integrates with Zipato.
 *  It can be used to interact with devices on a Zipato box, such as LightwaveRF
 *  which is not directly compatible with a SmartThings hub.
 *
 *  ---
 *  Disclaimer: This smart app and the associated device handlers are in no way sanctioned or supported by Zipato.
 *  All development is based on Zipato's published API.
 *
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
include 'asynchttp_v1'

definition(
  name: 'Zipato Integration',
  namespace: 'cwm',
  author: 'Neil Cumpstey',
  description: 'Integrate Zipato devices with SmartThings.',
  iconUrl: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/zipato-integration.src/assets/zipato-60.png',
  iconX2Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/zipato-integration.src/assets/zipato-120.png',
  iconX3Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/zipato-integration.src/assets/zipato-500.png',
  singleInstance: true
)

private apiRootUrl() { return 'https://my.zipato.com/zipato-web/v2/' }

//#region Preferences

preferences {
  page(name: 'mainPage')
  page(name: 'authenticationPage')
  page(name: 'authenticatedPage')
  page(name: 'manageDevicesPage')
}

def mainPage() {
  // If the app is not yet installed, first step is to authenticate
  if (!state.installed) {
    return authenticationPage(error)
  }

  // If there's an error connecting to the api, show it
  def error = null
  if (state.currentError != null) {
    error = """\
Error communicating with Zipato api:
${state.currentError}
Resolve the error if possible and try again.""" 
  }
  
  return dynamicPage(name: 'mainPage', title: null, install: true, uninstall: true) {
    if (error) {
      section {
        paragraph image: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png', "${error}"
      }
    }
    section ('Zipato settings') {
      href name: 'toAuthenticationPage', page: 'authenticationPage', title: "Authenticated as ${settings.zipatoUsername}", description: 'Tap to change' , state: state.authenticated ? 'complete' : null
    }
    section ('Devices') {
      href name: 'tomanageDevicesPage', page: 'manageDevicesPage', title: 'Manage devices'
    }
    section('General') {
      input 'logging', 'bool', title: 'Debug logging', description: 'Enable logging of debug messages.'
      label title: 'Assign a name', required: false
    }
  }
}

def authenticationPage(params) {
  return dynamicPage(name: 'authenticationPage', title: 'Authentication', install: false, nextPage: 'authenticatedPage') {
    if (params?.error) {
        section {
          paragraph image: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png', "${params?.error}"
        }
    }
    section {
      input 'zipatoUsername', 'text', title: 'Username', required: true, displayDuringSetup: true
      input 'zipatoPassword', 'password', title: 'Password', required: true, displayDuringSetup: true
      input 'zipatoSerial', 'text', title: 'Zipabox serial number', description: 'Only needed if you have more than one Zipabox', required: false, displayDuringSetup: true
    }
  }
}

def authenticatedPage() {
  // Attempt authentication
  authenticate()
  
  // Return to authentication page with error if authentication fails
  if (!state.authenticated) {
    return authenticationPage('error': state.currentError)
  }

  def message = state.installed
    ? 'Authentication successful. If you\'ve connected to a different account or different Zipabox, any existing devices may no longer work.'
    : 'Authentication successful. Save to complete installation, then come back into the settings to add devices.'
  return dynamicPage(name: 'authenticatedPage', title: 'Authenticated', install: state.installed ? false : true, nextPage: 'mainPage') {
    section {
      paragraph "${message}"
    }
  }
}

def manageDevicesPage() {
  // Get available devices from api
  fetchDevices()

  // If there's an error connecting to the api, return to main page to show it
  if (state.currentError != null) {
    return mainPage()
  }

  // Generate options from list of available devices
  def options = [:]
  state.devices.each { key, value ->
    options[key] = "${value.deviceName} > ${value.endpointName} > ${value.attributeName}"
  }

  // Set empty list warning message
  def message = null
  if (options == [:]) {
    message = 'No devices available.'
  }

  return dynamicPage (name: 'manageDevicesPage', title: 'Select devices', install: false, uninstall: false) {
    if (switchesMessage) {
      section {
        paragraph "${switchesMessage}"
      }
    }
    section {
      input name: 'selectedDevices', type: 'enum', required: false, multiple: true,
            title: "Select switches (${options.size() ?: 0} found)", options: options
    }
  }
}

//#endregion Preferences

//#region App event handlers

def installed() {
  state.installed = true

  initialize()
}

def updated() {
  logger "${app.label}: updated", 'trace'

  state.logLevel = settings.logging ? 5 : 2

  updateChildDevices()

  def children = getChildDevices()
  children.each {
    it.setLogLevel(state.logLevel)
  }
  
  initialize()
}

def uninstalled() {
  removeAllChildDevices()
}

//#endregion App event handlers

//#region Service manager functions

/**
 * Authenticate with Zipato with the credentials provided in settings.
 */
private void authenticate() {
  logger "${app.label}: authenticate", 'trace'

  initialiseSession()
  if (!state.currentError) {
    login()
  }
}

/**
 * Create and remove child devices to ensure the child devices
 * match the list selected in settings.
 */
private void updateChildDevices() {
  logger "${app.label}: updateChildDevices", 'trace'

  if (!settings.selectedDevices) {
    removeAllChildDevices()
    return
  }

  // Remove child devices for unselected options
  def children = getChildDevices()
  children.each {
    def zipatoId = it.getZipatoId()

    if (!settings.selectedDevices.contains(zipatoId)) {
      logger "Deleting device ${it.name} (${it.deviceNetworkId})"
      try {
        deleteChildDevice(it.deviceNetworkId)
      }
      catch(e) {
        logger "Error deleting device '${it.label}' (${it.deviceNetworkId}): ${e}", 'error'
      }
    }
  }

  // Create child devices for selected options
  settings.selectedDevices.each {
    def zipatoDevice = state.devices?.get(it)

    if (!zipatoDevice) {
      logger "Inconsistent state: device ${it} selected but not found in Zipato devices", 'warn'
      return
    }

    createChildDevice(zipatoDevice.type, zipatoDevice.id, zipatoDevice.deviceName)
  }
}

/**
 * Create a child device of the specified type.
 *
 * @param deviceType  Type of the device: house, room, switch.
 * @param zipatoId  Id of the device within Zipato.
 * @param label  Label of the new child device.
 */
private void createChildDevice(deviceType, zipatoId, label) {
  logger "${app.label}: createChildDevice", 'trace'

  def deviceNetworkId = "ZIPATO-${zipatoId}"
  def child = getChildDevice(deviceNetworkId)
  if (child) {
    logger "Child device ${deviceNetworkId} already exists. Not creating."
    return
  }

  def deviceHandler = getDeviceHandlerFor(deviceType)

  logger "Creating ${deviceType} ${zipatoId} with label '${label}' and device handler ${deviceHandler}"

  def device = addChildDevice(app.namespace, deviceHandler, deviceNetworkId, null, [ 'label': label ])
  if (device) {
    device.setZipatoId(zipatoId)
    device.setLogLevel(state.logLevel)
  }
}

/**
 * Remove all child devices belonging to this app.
 */
private void removeAllChildDevices(delete) {
  logger "${app.label}: removeAllChildDevices", 'trace'

  def devices = getChildDevices()
  devices.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

//#endregion Service manager functions

//#region Synchronous API requests

/**
 * Make a request to the session initialization api endpoint
 * to get a nonce and session id.
 */
private void initialiseSession() {
  logger "${app.label}: initialiseSession", 'trace'
  
  def requestParams = [
    uri: apiRootUrl(),
    path: 'user/init',
    contentType: 'application/json',
  ]

  try {
    httpGet(requestParams) { response ->
      logger "Response: ${response.status}; ${response.data}"

      if (response.status == 200 && response.data && response.data.success) {
        state.authenticated = false
        state.authSessionId = response.data.jsessionid
        state.authNonce = response.data.nonce
        state.currentError = null

        logger 'User init succeeded'
      }
      else {
        apiError(response.status, "Unexpected status code: ${response.status}")
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    apiError(e.statusCode, "${e}")
  }
}

/**
 * Make a request to the login api endpoint to log in the session.
 */
private void login() {
  logger "${app.label}: login", 'trace'

  def token = sha1(state.authNonce + sha1(settings.zipatoPassword))

  def requestParams = [
    uri: apiRootUrl(),
    path: 'user/login',
    query: [ 'username': settings.zipatoUsername, 'token': token, 'serial': settings.zipatoSerial ],
    contentType: 'application/json',
    headers: [
      'Cookie': "JSESSIONID=${state.authSessionId}"
    ],
  ]

  try {
    httpGet(requestParams) { response ->
      logger "Response: ${response.status}; ${response.data}"

      if (response.status == 200 && response.data && response.data.success) {
        state.authenticated = true
        state.authNonce = response.data.nonce
        state.currentError = null

        logger 'Login succeeded'
      }
      else {
        apiError(response.status, "${response.data?.error}")
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    apiError(e.statusCode, e.message)
  }
}

/**
 * Fetch information about all devices from the api, and store it in state.
 */
private void fetchDevices() {
  logger "${app.label}: fetchDevices", 'trace'
  
  def requestParams = [
    uri: apiRootUrl(),
    path: 'attributes/full',
    query: [
      full: true
    ],
    contentType: 'application/json',
    headers: [
      'Cookie': "JSESSIONID=${state.authSessionId}",
    ],
  ]

  try {
    httpGet(requestParams) { response ->
      if (response.status == 200 && response.data) {
        logger "${response.data.size()} devices returned by the api"

        def devices = [:]
        response.data.each {
          // At the moment only support switches.
          if (it?.definition?.cluster == 'com.zipato.cluster.OnOff') {
            devices[it.uuid] = mapSwitch(it)
          }
        }
        
        logger "Found: ${devices.size()} devices"
        state.devices = devices
      }
      else {
        apiError(response.status, "Unexpected status code: ${response.status}")
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    apiError(e.statusCode, e.message)
  }
}

//#endregion Synchronous API requests

//#region Asynchronous API requests

/**
 * Refresh the data on all devices.
 *
 * @param handler  Name of the response handler function.
 */
private void fetchAllValuesAsync(String handler) {
  logger "${app.label}: fetchAllValues", 'trace'
  
  def requestParams = [
    uri: apiRootUrl(),
    path: 'attributes/values',
    contentType: 'application/json',
    headers: [
      'Cookie': "JSESSIONID=${state.authSessionId}",
    ],
  ]

  asynchttp_v1.get(handler, requestParams)
}

/**
 * Make a request to the api to override the state of a switch.
 *
 * @param zipatoId  Id of the switch device within Zipato.
 * @param value  On/off state which the switch should be switched to.
 */
private void pushSwitchStateAsync(String zipatoId, Boolean value) {
  logger "${app.label}: pushSwitchStateAsync", 'trace'

  def requestParams = [
    uri: apiRootUrl(),
    path: "attributes/${zipatoId}/value",
    contentType: 'application/json',
    body: [ 'value': value ? 1 : 0 ],
    headers: [
      'Cookie': "JSESSIONID=${state.authSessionId}",
    ],
  ]

  asynchttp_v1.put('pushSwitchStateResponseHandler', requestParams, [ 'zipatoId': zipatoId, updates: ['switchState': value ]] )
}

//#endregion Asynchronous API requests

//#region Methods available to child devices

/**
 * Set the state of a switch.
 *
 * @param zipatoId  Id of the switch device within Zipato.
 * @param value  On/off state which the switch should be switched to.
 */
void pushSwitchState(String zipatoId, Boolean value) {
  pushSwitchStateAsync(zipatoId, value)
}

/**
 * Refresh the data on all devices.
 */
void refresh() {
  logger "${app.label}: refresh", 'trace'

  fetchAllValuesAsync('updateAllDevicesResponseHandler')
}

//#endregion Methods available to child devices

//#region API response handlers

/**
 * Handles an api response containing data about all devices.
 *
 * @param response  Response.
 * @param data  Additional data passed from the calling method.
 */
private void updateAllDevicesResponseHandler(response, data) {
  if (response.hasError()) {
    logger "API error received: ${response.getErrorMessage()}"
    return
  }

  logger "updateAllDevicesResponseHandler: ${response.json}"

  def attributeValues = [:]
  response.json.each {
    attributeValues[it.uuid] = it.value.value
  }
  
  def children = getChildDevices()
  children.each { child ->
    def zipatoId = child.getZipatoId()
    def zipatoType = child.getZipatoType()

    def zone = response.json.find({ it.uuid == zipatoId })
    if (!zone) {
      logger "Not found zone for ${zipatoId}", 'warn'
      return
    }

    switch (zipatoType) {
      // At the moment we're only supporting switches.
      case 'switch':
        def updates = mapSwitchUpdates(zone)
        child.updateState(updates)
        break;
      default:
        logger "Unknown device type: ${zipatoType}", 'warn'
        break;
    }
  }
}

/**
 * Handles a response from an api request to switch a switch.
 *
 * @param response  Response.
 * @param data  Additional data passed from the calling method.
 */
private void pushSwitchStateResponseHandler(response, data) { 
  if (response.hasError()) {
    logger "API error received: ${response.getErrorMessage()}"
    return
  }

  logger "pushSwitchStateResponseHandler: ${response.data}"

  def child = getChildDevice("ZIPATO-${data.zipatoId}")

  def updates = [ switchState: data?.updates?.switchState ]
  child.updateState(updates)
}

//#region API response handlers

//#region Helpers: private

/**
 * Initialise any schedulers or subscriptions.
 */
private void initialize() {
  unschedule()

  // Refresh data every 5 minutes
  runEvery5Minutes('refresh')

  // Reauthenticate every 3 hours
  runEvery3Hours('authenticate')
}

private void apiError(statusCode, message) {
  logger "Api error: ${statusCode}; ${message}", 'warn'
  
  state.currentError = "${message}"
  if (statusCode >= 300) {
    state.authenticated = false
  }
}

private String sha1(String value) {
  def sha1 = java.security.MessageDigest.getInstance('SHA1')
  def digest  = sha1.digest(value.getBytes())
  return new BigInteger(1, digest).toString(16)
}

/**
 * Extract basic data about a switch device from the data returned by the API.
 */
private Map mapSwitch(data) {
  return [
    id: data.uuid,
    type: 'switch',
    deviceName: data.device?.name,
    endpointName: data.endpoint?.name,
    attributeName: data.name,
  ]
}

/**
 * Extract the current state of a switch device from the data returned by the API, to update the device.
 */
private Map mapSwitchUpdates(data) {
  def updates = [:]

  if (data.value && data.value.containsKey('value')) {
    updates.switchState = "${data.value.value}".toBoolean()
  }

  return updates
}

private String getDeviceHandlerFor(String deviceType) {
  switch (deviceType) {
    case 'switch':
      return 'Zipato Switch'
    default:
      return null
  }
}

/**
 * Log message if logging is configured for the specified level.
 */
private void logger(message, String level = 'debug') {
  switch (level) {
    case 'error':
      if (state.logLevel >= 1) log.error message
      break
    case 'warn':
      if (state.logLevel >= 2) log.warn message
      break
    case 'info':
      if (state.logLevel >= 3) log.info message
      break
    case 'debug':
      if (state.logLevel >= 4) log.debug message
      break
    case 'trace':
      if (state.logLevel >= 5) log.trace message
      break
    default:
      log.debug message
      break
  }
}

//#endregion Helpers
