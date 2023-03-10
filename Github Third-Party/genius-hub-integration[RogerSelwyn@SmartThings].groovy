/**
 *  Genius Hub Integration 
 * 
 *  Based on code by Neil Cumpstey - converted to use official Genius Hub API
 * 
 *  A SmartThings device handler which wraps a device on a Genius Hub.
 *
 *  ---
 *  Disclaimer:
 *  This device handler and the associated smart app are in no way sanctioned or supported by Genius Hub.
 *  All work is based on an unpublished api, which may change at any point, causing this device handler or the
 *  smart app to break. I am in no way responsible for breakage due to such changes.
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
  namespace: 'RogerSelwyn',
  author: 'Roger Selwyn',
  category: "My Apps",
  description: 'Integrate Genius Hub devices with SmartThings.',
  iconUrl: 'https://raw.githubusercontent.com/RogerSelwyn/SmartThings/master/smartapps/rogerselwyn/genius-hub-integration.src/assets/genius-hub-60.png',
  iconX2Url: 'https://raw.githubusercontent.com/RogerSelwyn/SmartThings/master/smartapps/rogerselwyn/genius-hub-integration.src/assets/genius-hub-120.png',
  iconX3Url: 'https://raw.githubusercontent.com/RogerSelwyn/SmartThings/master/smartapps/rogerselwyn/genius-hub-integration.src/assets/genius-hub-500.png',
  singleInstance: true
)

//#region Preferences

preferences {
  page(name: 'mainPage')
  page(name: 'authenticationPage')
  page(name: 'authenticatedPage')
  page(name: 'manageDevicesPage')
  page(name: 'triggerPage')
}

def mainPage() {
  // If the app is not yet installed, first step is to authenticate
  if (!state.installed) {
    return authenticationPage(error)
  }

  // If there's an error connecting to the api, show it
  def error = "No current error"
  if (state.currentError != null) {
    error = """\
    Error communicating with Genius Hub api:
    ${state.currentError}
    Resolve the error if possible and try again.""" 
  }
  
  return dynamicPage(name: 'mainPage', title: null, install: true, uninstall: true) {
  //  if (error) {
      section {
        paragraph image: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png', "${error}"
      }
 //   }
    section ('Genius Hub settings') {
      href name: 'toAuthenticationPage', page: 'authenticationPage', title: "Authenticated by Token", description: 'Tap to change' , state: state.authenticated ? 'complete' : null
    }
    section ('Devices') {
      href name: 'tomanageDevicesPage', page: 'manageDevicesPage', title: 'Manage devices'
    }
    section ('Trigger') {
      href name: 'toTriggerPage', page: 'triggerPage', title: 'Trigger'
    }
    section('General') {
      input 'logging', 'bool', title: 'Debug logging', description: 'Enable logging of debug messages.'
      label title: 'Assign a name', required: false
    }
    section ('API') {
      paragraph "Hub software version: ${state.hubSoftwareVersion}"
      paragraph "Earliest API version: ${state.earliestCompatibleAPI}"
      paragraph "Latest API version  : ${state.latestCompatibleAPI}"
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
     input 'logging', 'bool', title: 'Debug logging', description: 'Enable logging of debug messages.'
     input 'geniusToken', 'text', title: 'Token', required: true, displayDuringSetup: true
    }
  }
}

def triggerPage () {
    logger "trigger", 'trace'
	refresh(['geniusId': 0, geniusType: "trigger"])
    //verifyAuthentication()
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

  refresh(['geniusId': 0, geniusType: "Install update"])
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

  verifyAuthentication()
}

/**
 * Create and remove child devices to ensure the child devices
 * match the list selected in settings.
 */
private void updateChildDevices() {
  logger "${app.label}: updateChildDevices", 'warn'

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
      logger "Deleting device '${it.label}' (${geniusId})", 'trace'
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
    logger "Child device ${deviceNetworkId} already exists. Not creating.", 'trace'
    return
  }

  def deviceHandler = getDeviceHandlerFor(deviceType)

  logger "Creating ${deviceType} ${geniusId} with label '${label}' and device handler ${deviceHandler}", 'trace'

  def device = addChildDevice(app.namespace, deviceHandler, deviceNetworkId, null, [ 'label': "Genius " + label ])
  logger "Device created: ${device}", 'trace'
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
 * Make a request to the authentication test api endpoint to verify the credentials.
 */
private void verifyAuthentication() {
  logger "${app.label}: verifyAuthentication", 'trace'
  
  def requestParams = [
    uri: getApiRootUrl(),
    path: 'version',
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ]
  ]

  try {
    httpGet(requestParams) { response ->
      logger "Response: ${response.status}; ${response.data}", 'trace'

      if (response.status == 200) {
        state.authenticated = true
        state.currentError = null
        state.hubSoftwareVersion = response.data.hubSoftwareVersion
        state.earliestCompatibleAPI = response.data.earliestCompatibleAPI
        state.latestCompatibleAPI = response.data.latestCompatibleAPI

        logger 'Authentication succeeded', 'trace'
      }
      else {
        handleApiError(response, "verifyAuthentication", "No data")
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    logApiError(e.statusCode, "From: verifyAuthentication - Message: ${e.message}")
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
        logger "${response.data.size()} devices returned by the api", 'trace'

        // We've had a successful request, so reset the current error
        state.currentError = null

        def devices = [:]
        response.data?.each {
         switch (it?.type) {
            case "manager":
              devices["id_${it.id}"] = mapHouse(it)
              break;
            case "on / off":
              devices["id_${it.id}"] = mapSwitch(it)
              break;
            case "radiator":
              devices["id_${it.id}"] = mapRoom(it)
              break;
            default:
              logger "Unknown device type: ${it.type} ${it.name}", 'warn'
              break;
          }
        }

	    logger "Found: ${devices.size()} devices", 'trace'
        state.devices = devices
      }
      else {
        handleApiError(response, "fetchZones", "No data")
      }
    }
  } catch (groovyx.net.http.HttpResponseException e) {
    logApiError(e.statusCode, "From: fetchZones - Message: ${e.message}")
  }
}

//#endregion Synchronous API requests

//#region Asynchronous API requests

/**
 * Refresh the data on all devices.
 *
 * @param handler  Name of the response handler function.
 */
private void fetchZonesAsync(String handler, data) {
  logger "${app.label}: fetchZonesAsync ${handler} - ${data}", 'trace'
	def zonePath = ""
  if (data.geniusType == 'room' || data.geniusType == 'switch') {
  	zonePath = "/" + data.geniusId
  }
  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones" + zonePath,
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.get(handler, requestParams, data)
}

/**
 * Make a request to the api to set the mode of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param mode  Mode which the zone should be switched to.
 */
private void pushModeAsync(Integer geniusId, String mode) {
  logger "${app.label}: pushModeAsync(${geniusId}, ${mode})", 'trace'

  def geniusMode = mode
  if (!geniusMode) {
    return
  }

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones/${geniusId}/mode",
    contentType: 'application/json',
    body: '"' + geniusMode + '"',
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.put('updateZoneResponseHandler', requestParams, [ 'geniusId': geniusId, 'geniusMode' : geniusMode ] )
}

/**
 * Make a request to the api to set the override period of a zone.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 * @param period  Period in seconds.
 */
private void pushOverridePeriodAsync(Integer geniusId, Integer period) {
  logger "${app.label}: pushOverridePeriodAsync(${geniusId}, ${period}, ${temperature})", 'trace'

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones/${geniusId}/override",
    contentType: 'application/json',
    body: [
      'duration': period,
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
    path: "zones/${geniusId}/override",

    contentType: 'application/json',
    body: [
      'duration': overridePeriod,
	  'setpoint': value
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.post('updateZoneResponseHandler', requestParams, ['geniusId': geniusId] )
}

/**
 * Make a request to the api to override the state of a switch.
 *
 * @param geniusId  Id of the switch zone within the Genius Hub.
 * @param value  On/off state which the switch should be switched to.
 */
private void pushSwitchStateAsync(Integer geniusId, Boolean value, Integer overridePeriod) {
  logger "${app.label}: pushSwitchStateAsync(${geniusId}, ${value})", 'trace'
  def requiredState = (value) ? 1 : 0;

  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones/${geniusId}/override",
    contentType: 'application/json',
    body: [
      'duration': overridePeriod,
      'setpoint': requiredState
    ],
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]

  asynchttp_v1.post('updateZoneResponseHandler', requestParams, [ 'geniusId': geniusId ] )

}

/**
 * Make a requeste to the api to get battery room info.
 *
 * @param geniusId  Id of the switch zone within the Genius Hub.
 */
private Map mapRoomUpdatesAsync(geniusId) {
  def requestParams = [
    uri: getApiRootUrl(),
    path: "zones/${geniusId}/devices",
    contentType: 'application/json',
    headers: [
      'Authorization': getAuthorizationHeader()
    ],
  ]
  def handler = 'mapRoomUpdatesHandler'
  asynchttp_v1.get(handler, requestParams, [ 'geniusId': geniusId ] )
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
  def children = getChildDevices()
  children.each { child ->
    def geniusId = child.getGeniusId()
    def geniusType = child.getGeniusType()
	if (geniusType == 'room') {
	  pushOverridePeriodAsync(geniusId, overridePeriod, value)
    }
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
void refresh(data) {
  fetchZonesAsync('updateAllZonesResponseHandler', data)
}


/**
 * Revert an overridden device to its base state.
 *
 * @param geniusId  Id of the zone within the Genius Hub.
 */
void revert(Integer geniusId) {
  logger "${app.label}: revert", 'trace'

  // Ensure we have the correct default operating mode.
  // There's no simple 'revert to base mode' api request as far as I can see.
  // fetchZones() //<-- Do I need this? RWJS

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
    handleAsyncApiError(response, "updateAllZonesResponseHandler", data)
    return
  }

  logger "updateAllZonesResponseHandler - Data: ${data} - Response: ${response.json}", "trace"


  // We've had a successful request, so reset the current error
  state.currentError = null

  if (data.geniusType != 'room' && data.geniusType != 'switch') {
    def children = getChildDevices()
    children.each { child ->
      def geniusId = child.getGeniusId()
      def zone = response.json.find({ it?.id == geniusId })
      if (!zone) {
        logger "Not found zone for ${geniusId}", 'warn'
        return
      }
      updateZone(child, zone)
    }
  } 
  else {
  	def zone = response.json
    def child = getChildDevice("GENIUS-${data.geniusId}")
  	updateZone(child, zone)
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
    handleAsyncApiError(response, "updateZoneResponseHandler", data)
    return
  }

  // We've had a successful request, so reset the current error
  state.currentError = null
  
  //refresh(['geniusId': data.geniusId, geniusType: "updateZoneResponseHandler"])
  runIn(30, 'refresh',[data: [geniusId: 0, geniusType: "updateZoneResponseHandler"]])
}

private void mapRoomUpdatesHandler(response, data) {
  if (response.hasError()) {
    handleAsyncApiError(response, "mapRoomUpdatesHandler", data)
    return
  }

  logger "mapRoomUpdatesHandler: ${response.json}", 'trace'
  
  // We've had a successful request, so reset the current error
  state.currentError = null
  
  if (response.json) {
  	def child = getChildDevice("GENIUS-${data.geniusId}")
	def updates = [:]
    // Use minimum battery level from all child devices (eg. motion sensor and radiator valves) as room battery level.
    
    def minBattery = 100
    response.json.each { device ->
    	if (device.state.containsKey('batteryLevel')) {
        	def deviceBattery = 100
            if (device.state.batteryLevel == 255) {deviceBattery = 0}
            else {deviceBattery = device.state.batteryLevel} 
            
            if (deviceBattery < minBattery) {minBattery = deviceBattery}
        }
    }
    updates.minBattery = minBattery

    // Use maximum illuminance level from all child motion sensor devices as room illuminance level.
    updates.illuminance = response.json
      .findAll { it.state.containsKey('luminance') }
      .collect { it.state.luminance }
      .max { it }
 
    child.updateState(updates)
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
  runEvery5Minutes('refresh',[data: [geniusId: 0, geniusType: "Scheduled"]])
  runEvery3Hours('verifyAuthentication')


}

private getApiRootUrl() {
  return "https://my.geniushub.co.uk/v1/"
}

private void handleAsyncApiError(response, caller = "Unknown", data) {
  logApiError(response.status, "API error received from: ${caller} - Message: ${response.getErrorMessage()} - Data: ${data}")
}

private void handleApiError(response, caller = "Unknown", data) {
  if (response.status == 308) {
    // The api proxy server has changed url.
    logApiServerChange(response.headers['X-Genius-ProxyLocation'].value)
    return
  }

  logApiError(response.status, "Unexpected error received from: ${caller} - Message: ${response.getErrorMessage()} - Data: ${data}")
}

private void logApiError(statusCode, message) {
  logger "Api error: ${statusCode}; ${message}", 'warn'
  
  state.currentError = "${message}"
  if (statusCode >= 300) {
    state.authenticated = false
  }
}

private String getAuthorizationHeader() {
  return "Bearer ${geniusToken}"
}

private void updateZone(child, zone) {
    def geniusType = child.getGeniusType()
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

/**
 * Extract basic data about a house zone from the data returned by the API.
*/
private Map mapHouse(data) {
  return [
    id: data.id,
    type: 'house',
    name: data.name,
  ]
}

/**
 * Extract basic data about a room zone from the data returned by the API.
*/
private Map mapRoom(data) {
  def defaultOperatingMode = state.devices?.get("id_${data.id}")?.defaultOperatingMode
  def defaultSetpoint = state.devices?.get("id_${data.id}")?.defaultSetpoint

  if (defaultOperatingMode == null) { defaultOperatingMode = 'timer' }
  if (defaultSetpoint == null) { defaultSetpoint = 4.0 }

  if (data.mode != 'override') {defaultOperatingMode = data.mode}
  if (data.mode != 'override') {defaultSetpoint = data.setpoint}

  return [
    id: data.id,
    type: 'room',
    name: data.name,
    defaultOperatingMode: defaultOperatingMode,
    defaultSetpoint: defaultSetpoint,
  ]
}

/**
 * Extract basic data about a switch zone from the data returned by the API.
 */
private Map mapSwitch(data) {
  return [
    id: data.id,
    type: 'switch',
    name: data.name,
    defaultOperatingMode: data.mode,
  ]
}

/**
 * Extract the current state of a house zone from the data returned by the API, to update the device.
 */
private Map mapHouseUpdates(data) {
  def updates = [:]

  if (data.containsKey('temperature')) {
    updates.sensorTemperature = data.temperature
  }

    def children = getChildDevices()
    def minBattery = 100
    children.each { child ->
        if (child.getGeniusType() == 'room') {
        	def zoneBattery = child.currentBattery 
            if (zoneBattery < minBattery) {minBattery = zoneBattery}
        }
    }
    updates.minBattery = minBattery

  return updates
}

/**
 * Extract the current state of a switch zone from the data returned by the API, to update the device.
 */
private Map mapSwitchUpdates(data) {
  def updates = [:]
  
  if (data.containsKey('mode')) {
    updates.operatingState = data.mode
  }
  
  if (updates.operatingState == 'override') {
    updates.overrideEndTime = now() + (data.override.duration * 1000)
    updates.switchState = data.override.setpoint.asBoolean()
  }

  // This is the override switch state, which only applies in override mode.
  // But we can't apply only in override mode, as the mode isn't always present in the response.
  if (updates.operatingState == 'override') {
    updates.switchState = data.override.setpoint.asBoolean()
  }
  
  // This is the actual switch state, which applies whenever it's present.
  else if (data.containsKey('setpoint')) {
    updates.switchState = data.setpoint.asBoolean()
  }

  return updates
}

/**
 * Extract the current state of a room zone from the data returned by the API, to update the device.
 */
private Map mapRoomUpdates(data) {
  def updates = [:]

  if (data.containsKey('mode')) {
    updates.operatingState = data.mode
  }
  
  if (updates.operatingState == 'override') {
    updates.overrideEndTime = now() + (data.override.duration * 1000)
    updates.setpoint = data.override.setpoint
  } else if (data.containsKey('setpoint')) {
    updates.setpoint = data.setpoint
    updates.defaultsetpoint = state.devices["id_${data.id}"].defaultSetpoint
  }

  if (data.containsKey('temperature')) {
    updates.sensorTemperature = data.temperature
  }
  
  def geniusId = data.id
  mapRoomUpdatesAsync(geniusId)

  return updates
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