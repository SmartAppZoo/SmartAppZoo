/**
 *  Genius Hub Integration
 * 
 *  Copyright 2018 Neil Cumpstey
 * 
 *  A SmartThings smart app which integrates with Genius Hub.
 *
 *  ---
 *  Disclaimer:
 *  This smart app and the associated device handlers are in no way sanctioned or supported by Genius Hub.
 *  All work is based on an unpublished api, which may change at any point, causing this smart app or the
 *  device handlers to break. I am in no way responsible for breakage due to such changes. 
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
  name: 'Genius Hub Integration',
  namespace: 'cwm',
  author: 'Neil Cumpstey',
  description: 'Integrate Genius Hub devices with SmartThings.',
  iconUrl: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/genius-hub-integration.src/assets/genius-hub-60.png',
  iconX2Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/genius-hub-integration.src/assets/genius-hub-120.png',
  iconX3Url: 'https://raw.githubusercontent.com/cumpstey/Cwm.SmartThings/master/smartapps/cwm/genius-hub-integration.src/assets/genius-hub-500.png',
  singleInstance: true
)

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
Error communicating with Genius Hub api:
${state.currentError}
Resolve the error if possible and try again.""" 
  }
  
  return dynamicPage(name: 'mainPage', title: null, install: true, uninstall: true) {
    if (error) {
      section {
        paragraph image: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png', "${error}"
      }
    }
    section ('Genius Hub settings') {
      href name: 'toAuthenticationPage', page: 'authenticationPage', title: "Authenticated as ${settings.geniusHubUsername}", description: 'Tap to change' , state: state.authenticated ? 'complete' : null
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
      input 'geniusHubUsername', 'text', title: 'Username', required: true, displayDuringSetup: true
      input 'geniusHubPassword', 'password', title: 'Password', required: true, displayDuringSetup: true
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
    ? 'Authentication successful. If you\'ve connected to a different account, any existing devices may no longer work.'
    : 'Authentication successful. Save to complete installation, then come back into the settings to add devices.'
  return dynamicPage(name: 'authenticatedPage', title: 'Authenticated', install: state.installed ? false : true, nextPage: 'mainPage') {
    section {
      paragraph "${message}"
    }
  }
}

def manageDevicesPage() {
  // Get available devices from api
  fetchZones()

  // If there's an error connecting to the api, return to main page to show it
  if (state.currentError != null) {
    return mainPage()
  }

  // Set up device options
  def options = [:]
  state.devices.each { key, value ->
    options[key] = value.name
  }

  // Set empty list warning message
  def message = null
  if (options == [:]) {
    message = 'No devices available.'
  }

  return dynamicPage (name: "manageDevicesPage", title: "Select devices", install: false, uninstall: false) {
    if (message) {
      section {
        paragraph "${message}"
      }
    }
    section {
      input name: 'selectedDevices', type: "enum", required: false, multiple: true,
            title: "Select devices (${options.size() ?: 0} found)", options: options //, submitOnChange: true
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

  refresh()
}

def uninstalled() {
  removeAllChildDevices()
}

//#endregion App event handlers

//#region Service manager functions

/**
 * Authenticate with Genius Hub with the credentials provided in settings.
 */
private void authenticate() {
  logger "${app.label}: authenticate", 'trace'

  checkIn()
  verifyAuthentication()
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
    def geniusId = it.getGeniusId()

    def id = "id_${geniusId}"

    // `contains` doesn't work. I don't know why.
    if (settings.selectedDevices.disjoint([id])) {
      logger "Deleting device '${it.label}' (${geniusId})"
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
    def geniusDevice = state.devices?.get(it)

    if (!geniusDevice) {
      logger "Inconsistent state: device ${it} selected but not found in Genius Hub devices", 'warn'
      return
    }

    createChildDevice(geniusDevice.type, geniusDevice.id, geniusDevice.name)
  }
}

/**
 * Create a child device of the specified type.
 *
 * @param deviceType  Type of the zone: house, room, switch.
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param label  Label of the new child device.
 */
private void createChildDevice(String deviceType, Integer geniusId, String label) {
  logger "${app.label}: createChildDevice", 'trace'

  def deviceNetworkId = "GENIUS-${geniusId}"
  def child = getChildDevice(deviceNetworkId)
  if (child) {
    logger "Child device ${deviceNetworkId} already exists. Not creating."
    return
  }

  def deviceHandler = getDeviceHandlerFor(deviceType)

  logger "Creating ${deviceType} ${geniusId} with label '${label}' and device handler ${deviceHandler}"

  def device = addChildDevice(app.namespace, deviceHandler, deviceNetworkId, null, [ 'label': label ])
  logger "Device created: ${device}"
  if (device) {
    device.setGeniusId(geniusId)
    device.setLogLevel(state.logLevel)
  }
}

/**
 * Remove all child devices belonging to this app.
 */
private void removeAllChildDevices() {
  logger "${app.label}: removeAllChildDevices", 'trace'

  def devices = getChildDevices()
  devices.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

//#endregion Service manager functions

//#region Synchronous API requests

/**
 * Make a request to the checkin api endpoint to retrieve the tunnel server hostname
 */
private void checkIn() {
  logger "${app.label}: checkIn", 'trace'
  
  def requestParams = [
    uri: 'https://hub.geniushub.co.uk/',
    path: 'checkin',
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ]
  ]

  try {
    httpGet(requestParams) { response ->
      logger "Response: ${response.status}; ${response.data}"

      if (response.status == 200) {
        state.apiServer = response.data.data.tunnel.server_name

        logger "Tunnel server url: ${response.data.data.tunnel.server_name}"
      }
      else {
        handleApiError(response)
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    logApiError(e.statusCode, "${e}")
  }
}

/**
 * Make a request to the authentication test api endpoint to verify the credentials.
 */
private void verifyAuthentication() {
  logger "${app.label}: verifyAuthentication", 'trace'
  
  def requestParams = [
    uri: getApiRootUrl(),
    path: 'auth/test',
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ]
  ]

  try {
    httpGet(requestParams) { response ->
      logger "Response: ${response.status}; ${response.data}"

      if (response.status == 200) {
        state.authenticated = true
        state.currentError = null

        logger 'Authentication succeeded'
      }
      else {
        handleApiError(response)
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    logApiError(e.statusCode, "${e}")
  }
}

/**
 * Fetch information about all zones from the api, and store it in state.
 */
private void fetchZones() {
  logger "${app.label}: fetchZones", 'trace'
 
  def requestParams = [
    uri: getApiRootUrl(),
    path: 'zones',
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  try {
    httpGet(requestParams) { response ->
      if (response.status == 200 && response.data) {
        logger "${response.data.data.size()} devices returned by the api"

        // We've had a successful request, so reset the current error
        state.currentError = null

        def devices = [:]
        response.data.data?.each {
          switch (it?.iType) {
            case 1:
              devices["id_${it.iID}"] = mapHouse(it)
              break;
            case 2:
              devices["id_${it.iID}"] = mapSwitch(it)
              break;
            case 3:
              devices["id_${it.iID}"] = mapRoom(it)
              break;
            default:
              logger "Unknown device type: ${it.iType} ${it.strName}", 'warn'
              break;
          }
        }
        
        logger "Found: ${devices.size()} devices"
        state.devices = devices
      }
      else {
        handleApiError(response)
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    logApiError(e.statusCode, e.message)
  }
}

//#endregion Synchronous API requests

//#region Asynchronous API requests

/**
 * Refresh the data on all devices.
 *
 * @param handler  Name of the response handler function.
 */
private void fetchZonesAsync(String handler) {
  logger "${app.label}: fetchZonesAsync", 'trace'

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones",
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.get(handler, requestParams)
}

/**
 * Make a request to the api to set the mode of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param mode  Mode which the zone should be switched to.
 */
private void pushModeAsync(Integer geniusId, String mode) {
  logger "${app.label}: pushModeAsync(${geniusId}, ${mode})", 'trace'

  def geniusMode = mapMode(mode)
  if (!geniusMode) {
    return
  }

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zone/${geniusId}",
    contentType: 'application/json',
    body: [
      'iMode': geniusMode
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.patch('updateZoneResponseHandler', requestParams, [ 'geniusId': geniusId ] )
}

/**
 * Make a request to the api to set the override period of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param period  Period in seconds.
 */
private void pushOverridePeriodAsync(Integer geniusId, Integer period) {
  logger "${app.label}: pushOverridePeriodAsync(${geniusId}, ${period})", 'trace'

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zone/${geniusId}",
    contentType: 'application/json',
    body: [
      'iBoostTimeRemaining': period
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.patch('updateZoneResponseHandler', requestParams, [ 'geniusId': geniusId ] )
}

/**
 * Make a request to the api to override the room temperature.
 *
 * @param geniusId  Id of the room zone within the Genius Hub.
 * @param value  Temperature in Celsius.
 * @param overridePeriod  Period in seconds for which the default temperature should be overridden.
 */
private void pushRoomTemperatureAsync(Integer geniusId, Double value, Integer overridePeriod) {
  logger "${app.label}: pushRoomTemperatureAsync(${geniusId}, ${value})", 'trace'

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zone/${geniusId}",
    contentType: 'application/json',
    body: [
      'fBoostSP': value,
      'iBoostTimeRemaining': overridePeriod,
      'iMode': 16
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.patch('updateZoneResponseHandler', requestParams, [geniusId: geniusId] )
}

/**
 * Make a request to the api to override the state of a switch.
 *
 * @param geniusId  Id of the switch zone within the Genius Hub.
 * @param value  On/off state which the switch should be switched to.
 */
private void pushSwitchStateAsync(Integer geniusId, Boolean value, Integer overridePeriod) {
  logger "${app.label}: pushSwitchStateAsync(${geniusId}, ${value})", 'trace'

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zone/${geniusId}",
    contentType: 'application/json',
    body: [
      'fBoostSP': value,
      'iBoostTimeRemaining': overridePeriod,
      'iMode': 16
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.patch('updateZoneResponseHandler', requestParams, [ 'geniusId': geniusId, updates: ['switchState': value ]] )
}

//#endregion Asynchronous API requests

//#region Methods available to child devices

/**
 * Override the temperature for the whole house.
 *
 * @param value  Temperature in Celsius.
 * @param overridePeriod  Period in seconds for which the default temperature should be overridden.
 */
void pushHouseTemperature(Double value, Integer overridePeriod = 3600) {
  state.devices.values().findAll{ it.type == 'room' }.each{
    pushRoomTemperatureAsync(it.id, value, overridePeriod)
  }
}

/**
 * Set the mode of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param mode  Mode which the zone should be switched to.
 */
void pushMode(Integer geniusId, String mode) {
  pushModeAsync(geniusId, mode)
}

/**
 * Set the override period of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param period  Period in seconds.
 */
void pushOverridePeriod(Integer geniusId, Integer period) {
  pushOverridePeriodAsync(geniusId, period)
}

/**
 * Override the room temperature.
 *
 * @param geniusId  Id of the room zone within the Genius Hub.
 * @param value  Temperature in Celsius.
 * @param overridePeriod  Period in seconds for which the default temperature should be overridden.
 */
void pushRoomTemperature(Integer geniusId, Double value, Integer overridePeriod = 3600) {
  pushRoomTemperatureAsync(geniusId, value, overridePeriod)
}

/**
 * Set the state of a switch.
 *
 * @param geniusId  Id of the switch zone within the Genius Hub.
 * @param value  On/off state which the switch should be switched to.
 * @param overridePeriod  Period in seconds for which the switch's default state should be overridden.
 */
void pushSwitchState(Integer geniusId, Boolean value, Integer overridePeriod = 3600) {
  pushSwitchStateAsync(geniusId, value, overridePeriod)
}

/**
 * Refresh the data on all devices.
 */
void refresh() {
  logger "${app.label}: refresh", 'trace'

  fetchZonesAsync('updateAllZonesResponseHandler')
}

/**
 * Revert an overridden device to its base state.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 */
void revert(Integer geniusId) {
  logger "${app.label}: refresh", 'trace'

  // Ensure we have the correct default operating mode.
  // There's no simple 'revert to base mode' api request as far as I can see.
  fetchZones()

  def mode = state.devices["id_${geniusId}"].defaultOperatingMode
  pushModeAsync(geniusId, mode)
}

//#endregion Methods available to child devices

//#region API response handlers

/**
 * Handles an api response containing data about all zones.
 *
 * @param response  Response.
 * @param data  Additional data passed from the calling method.
 */
private void updateAllZonesResponseHandler(response, data) { 
  if (response.hasError()) {
    handleAsyncApiError(response)
    return
  }

  logger "updateAllZonesResponseHandler: ${response.json}"

  // We've had a successful request, so reset the current error
  state.currentError = null

  def children = getChildDevices()
  children.each { child ->
    def geniusId = child.getGeniusId()
    def geniusType = child.getGeniusType()

    def zone = response.json.data.find({ it.iID == geniusId })
    if (!zone) {
      logger "Not found zone for ${geniusId}", 'warn'
      return
    }

    switch (geniusType) {
      case 'house':
        def updates = mapHouseUpdates(zone)
        child.updateState(updates)
        break;
      case 'room':
        def updates = mapRoomUpdates(zone)
        child.updateState(updates)
        break;
      case 'switch':
        def updates = mapSwitchUpdates(zone)
        child.updateState(updates)
        break;
      default:
        logger "Unknown device type: ${geniusType}", 'warn'
        break;
    }
  }
}

/**
 * Handles an api response containing data about a single zone.
 *
 * @param response  Response.
 * @param data  Additional data passed from the calling method.
 */
private void updateZoneResponseHandler(response, data) { 
  if (response.hasError()) {
    handleAsyncApiError(response)
    return
  }

  logger "updateZoneResponseHandler: ${response.json}"

  // We've had a successful request, so reset the current error
  state.currentError = null

  def child = getChildDevice("GENIUS-${data.geniusId}")
  if (!child) {
    logger "Child device for ${data.geniusId} not found"
  }

  def geniusType = child.getGeniusType()
  def updates = data.updates as Map ?: [:]

  switch (geniusType) {
    case 'house':
      updates << mapHouseUpdates(response.json.data)
      child.updateState(updates)
      break;
    case 'room':
      updates << mapRoomUpdates(response.json.data)
      child.updateState(updates)
      break;
    case 'switch':
      updates << mapSwitchUpdates(response.json.data)
      child.updateState(updates)
      break;
  }
}

//#endregion

//#region Helpers

/**
 * Set up schedulers.
 */
private void initialize() {
  unschedule()

  // Refresh data for all devices every 5 minutes
  runEvery5Minutes('refresh')

  // Check the tunnel url every so often, in order to fix any
  // auth errors which aren't caught by the error 308 handling.
  runEvery3Hours('checkIn')
}

private getApiRootUrl() {
  if (!state.apiServer) {
    checkIn()
  }

  def apiServer = state.apiServer ?: 'hub-server-1.heatgenius.co.uk'
  return "https://${apiServer}/v3/"
}

private void handleAsyncApiError(response) {
  if (response.status == 308) {
    // The api proxy server has changed url.
    logApiServerChange(response.headers['X-Genius-ProxyLocation'])
    return
  }

  logApiError(response.status, "API error received: ${response.getErrorMessage()}")
}

private void handleApiError(response) {
  if (response.status == 308) {
    // The api proxy server has changed url.
    logApiServerChange(response.headers['X-Genius-ProxyLocation'].value)
    return
  }

  logApiError(response.status, "Unexpected status code: ${response.status}")
}

/**
 * Handle a url change for the api proxy server.
 * Update the value in state, and log that the request should be retried.
 * We can't feasibly automatically retry the request, but mostly it'll be retried
 * shortly anyhow at which time it should work.
 */
private void logApiServerChange(String newServer) {
  state.apiServer = newServer
  def message = "API server location changed to ${newServer}. Please retry the request."
  state.currentError = message
  logger message, 'info'
}

private void logApiError(statusCode, message) {
  logger "Api error: ${statusCode}; ${message}", 'warn'
  
  state.currentError = "${message}"
  if (statusCode >= 300) {
    state.authenticated = false
  }
}

private String getAuthorizationHeader() {
  def hash = sha256(settings.geniusHubUsername + settings.geniusHubPassword)
  def encoded = "${settings.geniusHubUsername}:${hash}".bytes.encodeBase64()
  return "Basic ${encoded}"
}

private String sha256(String value) {  
  def bytesOfPassword = value.getBytes("UTF-8");  
  def md = java.security.MessageDigest.getInstance("SHA-256");  
  md.update(bytesOfPassword);  
  def bytesOfEncryptedPassword = md.digest();  
  return new BigInteger(1, bytesOfEncryptedPassword).toString(16);
}  

/**
 * Extract basic data about a house zone from the data returned by the API.
 */
private Map mapHouse(data) {
  return [
    id: data.iID,
    type: 'house',
    name: data.strName,
  ]
}

/**
 * Extract basic data about a room zone from the data returned by the API.
 */
private Map mapRoom(data) {
  return [
    id: data.iID,
    type: 'room',
    name: data.strName,
    defaultOperatingMode: mapMode(data.iBaseMode),
  ]
}

/**
 * Extract basic data about a switch zone from the data returned by the API.
 */
private Map mapSwitch(data) {
  return [
    id: data.iID,
    type: 'switch',
    name: data.strName,
    defaultOperatingMode: mapMode(data.iBaseMode),
  ]
}

/**
 * Extract the current state of a house zone from the data returned by the API, to update the device.
 */
private Map mapHouseUpdates(data) {
  def updates = [:]

  if (data.containsKey('nodes')) {
    // Use minimum battery level from all child devices as house battery level.
    updates.minBattery = data.nodes
                             .findAll { it.childValues.containsKey('Battery') }
                             .collect { it.childValues.Battery.val }
                             .min { it }
  }

  if (data.containsKey('fPV')) {
    updates.sensorTemperature = data.fPV
  }

  return updates
}

/**
 * Extract the current state of a room zone from the data returned by the API, to update the device.
 */
private Map mapRoomUpdates(data) {
  def updates = [:]

  if (data.containsKey('nodes')) {
    // Use minimum battery level from all child devices (eg. motion sensor and radiator valves) as room battery level.
    updates.minBattery = data.nodes
                             .findAll { it.childValues.containsKey('Battery') }
                             .collect { it.childValues.Battery.val }
                             .min { it }

    // Use maximum illuminance level from all child motion sensor devices as room illuminance level.
    updates.illuminance = data.nodes
                              .findAll { it.childValues.containsKey('LUMINANCE') }
                              .collect { it.childValues.LUMINANCE.val }
                              .max { it }
  }

  if (data.containsKey('iMode')) {
    updates.operatingState = mapMode(data.iMode)
  }
  
  if (data.containsKey('iBoostTimeRemaining')) {
    updates.overrideEndTime = now() + data.iBoostTimeRemaining * 1000
  }

  if (data.containsKey('fPV')) {
    updates.sensorTemperature = data.fPV
  }

  return updates
}

/**
 * Extract the current state of a switch zone from the data returned by the API, to update the device.
 */
private Map mapSwitchUpdates(data) {
  def updates = [:]

  if (data.containsKey('iMode')) {
    updates.operatingState = mapMode(data.iMode)
  }
  
  if (data.containsKey('iBoostTimeRemaining')) {
    updates.overrideEndTime = now() + data.iBoostTimeRemaining * 1000
  }

  // This is the override switch state, which only applies in override mode.
  // But we can't apply only in override mode, as the mode isn't always present in the response.
  if (data.containsKey('fBoostSP')) {
    updates.switchState = data.fBoostSP.asBoolean()
  }
  
  // This is the actual switch state, which applies whenever it's present.
  if (data.containsKey('fSP')) {
    updates.switchState = data.fSP.asBoolean()
  }

  return updates
}

private mapMode(mode) {
  switch (mode) {
    case 1: return 'off'
    case 2: return 'timer'
    case 4: return 'footprint'
    case 16: return 'override'
    case 'off': return 1
    case 'timer': return 2
    case 'footprint': return 4
    case 'override': return 16
    default:
      if (mode != null) {
        logger "Unknown operating mode: ${mode}", 'warn'
      }

      return null
  }
}

private String getDeviceHandlerFor(String deviceType) {
  switch (deviceType) {
    case 'house':
      return 'Genius Hub House'
    case 'room':
      return 'Genius Hub Room'
    case 'switch':
      return 'Genius Hub Switch'
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
