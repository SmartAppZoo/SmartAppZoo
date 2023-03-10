/**
 *  Flume Smart Home Water Monitor
 *
 *  Copyright 2021 tronikos
 *
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
    name: "Flume Smart Home Water Monitor",
    namespace: "tronikos",
    author: "tronikos",
    description: "SmartApp for Flume Smart Home Water Monitor",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/tronikos/FlumeSmartThings/main/icons/flume_logo.png",
    iconX2Url: "https://raw.githubusercontent.com/tronikos/FlumeSmartThings/main/icons/flume_logo.png",
    iconX3Url: "https://raw.githubusercontent.com/tronikos/FlumeSmartThings/main/icons/flume_logo.png",
    singleInstance: true,
) {
    appSetting "FlumeAPI_Key"
    appSetting "FlumeAPI_Secret"
}

preferences {
    page(name: "pageOne", title: "Options", uninstall: true, install: true) {
        section("Login") {
            paragraph("You MUST set the 'API Key' and 'API Secret' via App Settings in IDE")
            input(
                name: "username",
                type: "email",
                required: true,
                title: "Email Address",
            )
            input(
                name: "password",
                type: "password",
                required: true,
                title: "Password",
            )
        }
        section("Alert options") {
            input(
                name: "wetOnAnyUsageAlert",
                type: "bool",
                title: "Wet on any usage alert?",
                description: "Wet on any usage alert and not just leaks?",
                defaultValue: true,
                required: false,
            )
            input(
                name: "wetOnlyOnUnreadAlerts",
                type: "bool",
                title: "Wet only on unread alerts?",
                description: "If yes, wet status will be cleared once you have read the alert in the Flume app. " +
                             "If no, you need to delete the alert in the Flume app.",
                defaultValue: false,
                required: false,
            )
        }
        section("Polling options") {
            paragraph("You must stay under 120 requests per hour to avoid rate limiting")
            input(
                name: "pollAlertsEveryMinutes",
                type: "enum",
                title: "How often to poll for alerts?",
                options: runEveryMinuteOptions(),
                defaultValue: 5,
                required: false,
            )
            input(
                name: "pollWaterUsageEveryMinutes",
                type: "enum",
                title: "How often to poll for water usage?",
                options: runEveryMinuteOptions(),
                defaultValue: 15,
                required: false,
            )
            input(
                name: "pollDevicesEveryMinutes",
                type: "enum",
                title: "How often to poll for new devices and infrequently changed attributes such as battery?",
                options: runEveryMinuteOptions(),
                defaultValue: 60,
                required: false,
            )
        }
        section("Local Proxy") {
            paragraph("The limit of 120 requests per hour is per IP address. " +
                      "SmartThings SmartApps run on the cloud so this limit is shared among other users of this " +
                      "SmartApp. To avoid rate limiting it's recommended to setup a proxy running in your local " +
                      "network that the SmartThings hub will locally connect to.")
            input(
                name: "proxyHost",
                type: "text",
                required: false,
                title: "Proxy Host",
                description: "e.g. 192.168.0.5:3006",
            )
        }
        section("Other options") {
            label(
                title: "Assign a name for Service Manager",
                required: false,
            )
            input(
                name: "awayModes",
                type: "mode",
                title: "SmartThings modes when water meter should be set to Away",
                multiple: true,
                required: false
            )
            input(
                name: "configLoggingLevelIDE",
                title: "IDE Live Logging Level",
                type: "enum",
                options: [
                    0 : "None",
                    1 : "Error",
                    2 : "Warn or above",
                    3 : "Info or above",
                    4 : "Debug or above",
                    5 : "Trace or above",
                ],
                defaultValue: "5",
                required: false,
            )
        }
    }
}

Map runEveryMinuteOptions() {
    return [
        0 : "Never",
        1 : "Every 1 minute",
        5 : "Every 5 minutes",
        10 : "Every 10 minutes",
        15 : "Every 15 minutes",
        30 : "Every 30 minutes",
        60 : "Every 1 hour",
        180 : "Every 3 hours",
    ]
}

void installed() {
    logger("installed() called with settings: $settings", "trace")
    runIn(3, initialize)
}

void updated() {
    logger("updated() called with settings: $settings", "trace")
    cleanup()
    // deleteChildDevice seems to take a while to delete; wait before re-creating
    runIn(10, initialize)
}

void uninstalled() {
    logger("uninstalled() called", "trace")
    cleanup()
}

void cleanup() {
    logger("cleanup() called", "trace")
    unschedule(pollDevices)
    unschedule(pollAlerts)
    unschedule(pollWaterUsage)
    childDevices.each { device ->
        logger("cleanup- deleting $device with id: ${device.deviceNetworkId}", "info")
        try {
            deleteChildDevice(device.deviceNetworkId)
        } catch (e) {
            logger("cleanup- caught and ignored deleting $device: $e", "warn")
        }
    }
    state.access_token = null
    state.access_token_expires_at = null
}

void initialize() {
    logger("initialize() called with settings: $settings", "trace")
    pollDevices()
    runEvery(pollDevicesEveryMinutes, "pollDevices")
    runEvery(pollAlertsEveryMinutes, "pollAlerts")
    runEvery(pollWaterUsageEveryMinutes, "pollWaterUsage")
    if (settings.awayModes) {
        logger("Subscribing to location mode changes", "info")
        subscribe(location, "mode", modeChangeHandler)
    }
}

// Handling new/modified devices.

void pollDevices() {
    logger("pollDevices() called", "trace")
    fetchDevices("processFetchDevices")
}

void processFetchDevices(physicalgraph.scheduling.AsyncResponse resp, Map unusedCallbackData) {
    logger("processFetchDevices called with response from cloud", "trace")
    if (hasError(resp, "fetchDevices")) {
        return
    }
    processFetchDevicesResponse(resp.json)
}

void processFetchDevices(Map data) {
    logger("processFetchDevices called with response from cache", "trace")
    processFetchDevicesResponse(data.respData)
}

void processFetchDevices(physicalgraph.device.HubResponse resp) {
    logger("processFetchDevices called with response from local proxy", "trace")
    if (hasError(resp, "fetchDevices")) {
        return
    }
    processFetchDevicesResponse(resp.json)
}

void processFetchDevicesResponse(Map respData) {
    logger("fetchDevices response is: $respData", "debug")
    Map flumeDevices = parseFetchDevicesResponse(respData)
    logger("parseFetchDevicesResponse returned: $flumeDevices", "info")
    flumeDevices.each { deviceId, dev ->
        physicalgraph.app.ChildDeviceWrapper device = getChildDevice(deviceId)
        if (device) {
            logger("Updating existing $device data: ${dev.data}", "info")
            dev.data.each { k, v ->
                device.updateDataValue(k, v.toString())
            }
        } else {
            device = addChildDevice(
                "Flume Smart Home Water Monitor DH", deviceId, null,
                [label: "${dev.locationName}-Flume Meter", completedSetup: true, data: dev.data])
            logger("device added: $device with id: $deviceId", "debug")
        }
        logger("Sending to $device attributes: ${dev.attributes}", "info")
        dev.attributes.each { k, v ->
            device.sendEvent(name: k, value: v)
        }
    }
}

// Handling alerts.

void pollAlerts() {
    logger("pollAlerts() called", "trace")
    fetchUsageAlertNotifications("processFetchUsageAlertNotifications")
}

void processFetchUsageAlertNotifications(physicalgraph.scheduling.AsyncResponse resp, Map unusedCallbackData) {
    logger("processFetchUsageAlertNotifications called with response from cloud", "trace")
    if (hasError(resp, "fetchUsageAlertNotifications")) {
        return
    }
    processFetchUsageAlertNotificationsResponse(resp.json)
}

void processFetchUsageAlertNotifications(Map data) {
    logger("processFetchUsageAlertNotifications called with response from cache", "trace")
    processFetchUsageAlertNotificationsResponse(data.respData)
}

void processFetchUsageAlertNotifications(physicalgraph.device.HubResponse resp) {
    logger("processFetchUsageAlertNotifications called with response from local proxy", "trace")
    if (hasError(resp, "fetchUsageAlertNotifications")) {
        return
    }
    processFetchUsageAlertNotificationsResponse(resp.json)
}

void processFetchUsageAlertNotificationsResponse(Map respData) {
    logger("fetchUsageAlertNotifications response is: $respData", "debug")
    Set alertingDeviceIds = getAlertingDeviceIds(respData)
    logger("getAlertingDeviceIds returned: $alertingDeviceIds", "info")
    childDevices.each { device ->
        String wetDry = (device.deviceNetworkId in alertingDeviceIds) ? "wet" : "dry"
        logger("Setting $device to $wetDry", "info")
        device.setWater(wetDry)
    }
}

// Handling water usage stats.

void pollWaterUsage() {
    logger("pollWaterUsage() called", "trace")
    childDevices.each { device -> pollWaterUsage(device) }
}

// parameter is physicalgraph.app.ChildDeviceWrapper when called from app,
// physicalgraph.device.cache.DeviceDTO when called from DH
void pollWaterUsage(device) {
    logger("pollWaterUsage($device) called", "trace")
    String deviceId = device.deviceNetworkId
    String locationTZ = device.getDataValue("locationTZ")
    Map callbackData = [deviceId: deviceId]
    queryWaterUsage(deviceId, locationTZ, "processQueryWaterUsage", callbackData)
}

void processQueryWaterUsage(physicalgraph.scheduling.AsyncResponse resp, Map callbackData) {
    logger("processQueryWaterUsage called with response from cloud", "trace")
    if (hasError(resp, "queryWaterUsage")) {
        return
    }
    processQueryWaterUsageResponse(resp.json, callbackData)
}

void processQueryWaterUsage(Map data) {
    logger("processQueryWaterUsage called with response from cache", "trace")
    processQueryWaterUsageResponse(data.respData, data.callbackData)
}

void processQueryWaterUsage(physicalgraph.device.HubResponse resp) {
    logger("processQueryWaterUsage called with response from local proxy", "trace")
    if (hasError(resp, "queryWaterUsage")) {
        return
    }
    processQueryWaterUsageResponse(resp.json, getCallbackDataFromHttpHeader(resp.headers))
}

void processQueryWaterUsageResponse(Map respData, Map callbackData) {
    logger("queryWaterUsage($callbackData) response is: $respData", "debug")
    Map flowData = parseQueryWaterUsageResponse(respData)
    String deviceId = callbackData.deviceId
    logger("parseQueryWaterUsageResponse for $deviceId returned: $flowData", "info")
    physicalgraph.app.ChildDeviceWrapper device = getChildDevice(deviceId)
    flowData.each { k, v ->
        device.sendEvent(name: k, value: v)
    }
    device.setListElementWithLastArgument()
}

// Handling away mode changes.

void modeChangeHandler(physicalgraph.app.EventWrapper evt) {
    logger("mode changed to ${evt.value}", "info")
    boolean awayMode = evt.value in settings.awayModes
    Set locationIds = childDevices*.getDataValue("locationId").toSet()
    logger("Updating locations: $locationIds to awayMode: $awayMode", "info")
    locationIds.each { locationId ->
        Map callbackData = [locationId: locationId, awayMode: awayMode]
        updateLocation(locationId, awayMode, "processUpdateLocation", callbackData)
    }
}

void processUpdateLocation(physicalgraph.scheduling.AsyncResponse resp, Map callbackData) {
    logger("processUpdateLocation called with response from cloud", "trace")
    if (hasError(resp, "updateLocation")) {
        return
    }
    processUpdateLocationResponse(resp.json, callbackData)
}

void processUpdateLocation(physicalgraph.device.HubResponse resp) {
    logger("processUpdateLocation called with response from local proxy", "trace")
    if (hasError(resp, "updateLocation")) {
        return
    }
    processUpdateLocationResponse(resp.json, getCallbackDataFromHttpHeader(resp.headers))
}

void processUpdateLocationResponse(Map respData, Map callbackData) {
    logger("updateLocation($callbackData) response is: $respData", "debug")
    if (!respData.success) {
        logger("Non successful response: $respData", "warn")
        return
    }
    childDevices.each { device ->
        if (device.getDataValue("locationId") != callbackData.locationId) {
            return
        }
        logger("Setting $device awayMode to ${callbackData.awayMode}", "info")
        device.sendEvent(name: "awayMode", value: callbackData.awayMode)
    }
}

// Helper methods

boolean hasError(physicalgraph.scheduling.AsyncResponse resp, String method) {
    if (resp.hasError()) {
        logger("$method ${resp.errorMessage} , headers: ${resp.headers}, data: ${resp.errorData}", "error")
        return true
    }
    return false
}

boolean hasError(physicalgraph.device.HubResponse resp, String method) {
    if (resp.status != 200) {
        logger("$method failed with status: ${resp.status} , headers: ${resp.headers}", "error")
        return true
    }
    return false
}

void runEvery(String minutes, String handlerMethod) {
    logger("runEvery($minutes, $handlerMethod) called", "trace")
    switch (minutes.toInteger()) {
        case 0:
            break
        case 1:
            runEvery1Minute(handlerMethod)
            break
        case 5:
            runEvery5Minutes(handlerMethod)
            break
        case 10:
            runEvery10Minutes(handlerMethod)
            break
        case 15:
            runEvery15Minutes(handlerMethod)
            break
        case 30:
            runEvery30Minutes(handlerMethod)
            break
        case 60:
            runEvery1Hour(handlerMethod)
            break
        case 180:
            runEvery3Hours(handlerMethod)
            break
        default:
            logger("Unhandled $minutes in runEvery", "error")
            break
    }
}

void logger(String msg, String level = "debug") {
    int loggingLevelIDE = (settings.configLoggingLevelIDE != null) ? settings.configLoggingLevelIDE.toInteger() : 5
    switch (level) {
        case "error":
            if (loggingLevelIDE >= 1) { log.error msg }
            break
        case "warn":
            if (loggingLevelIDE >= 2) { log.warn msg }
            break
        case "info":
            if (loggingLevelIDE >= 3) { log.info msg }
            break
        case "debug":
            if (loggingLevelIDE >= 4) { log.debug msg }
            break
        case "trace":
            if (loggingLevelIDE >= 5) { log.trace msg }
            break
        default:
            log.debug msg
            break
    }
}

// Interactions with https://flumetech.readme.io/reference API

String getApiBase() { return "https://api.flumewater.com" }
String getFlumeAPIKey() { return appSettings.FlumeAPI_Key }
String getFlumeAPISecret() { return appSettings.FlumeAPI_Secret }

Map getDefaultHeaders() {
    return [
        "User-Agent": "okhttp/3.12.3",
        "Content-Type": "application/json"
    ]
}

Map getDefaultAuthHeaders() {
    loginIfNeeded()
    Map headers = getDefaultHeaders()
    headers["Authorization"] = "Bearer ${state.access_token}"
    return headers
}

void loginIfNeeded() {
    if (now() / 1000 + 600 > state.access_token_expires_at) {
        // Expired or expires within 10 minutes
        login()
    }
}

void login() {
    logger("login() called", "trace")
    Map body = [
        grant_type: "password",
        client_id: getFlumeAPIKey(),
        client_secret: getFlumeAPISecret(),
        username: settings.username,
        password: settings.password,
    ]
    Map params = [
        uri: getApiBase(),
        path: "/oauth/token",
        headers: getDefaultHeaders(),
        body: body,
    ]
    logger("Fetching $params", "debug")
    httpPostJson(params) { resp ->
        Map respData = resp.data
        logger("Response from ${params.path}: $respData", "debug")
        if (!respData.success) {
            logger("Non successful login: $respData", "error")
            return
        }
        state.access_token = respData.data[0].access_token
        state.access_token_expires_at = now() / 1000 + respData.data[0].expires_in
    }
}

void fetchDevices(String callbackMethod) {
    logger("fetchDevices($callbackMethod) called", "trace")
    Map params = [
        uri: getApiBase(),
        path: "/me/devices",
        query: [
            user: "false",
            location: "true",
        ],
        headers: getDefaultAuthHeaders(),
    ]
    issueHttpRequest("GET", params, callbackMethod, null)
}

Map parseFetchDevicesResponse(Map respData) {
    if (!respData.success) {
        logger("Non successful response from fetchDevices: $respData", "warn")
        return [:]
    }
    Map flumeDevices = [:]
    respData.data.each { dev ->
        if (dev.type == 2) {
            flumeDevices[dev.id] = [
                locationName: dev.location.name,
                data: [
                    locationId: dev.location.id,
                    locationTZ: dev.location.tz,
                ],
                attributes: [
                    // Keys need to match attributes in DH
                    batteryLevel: dev.battery_level,
                    connected: dev.connected,
                    awayMode: dev.location.away_mode,
                ],
            ]
        }
    }
    return flumeDevices
}

void fetchUsageAlertNotifications(String callbackMethod) {
    logger("fetchUsageAlertNotifications($callbackMethod) called", "trace")
    Map params = [
        uri:  getApiBase(),
        path: "/me/notifications",
        query: [
            type: 1,
        ],
        headers: getDefaultAuthHeaders(),
    ]
    issueHttpRequest("GET", params, callbackMethod, null)
}

Set getAlertingDeviceIds(Map respData) {
    if (!respData.success) {
        logger("Non successful response from fetchUsageAlertNotifications: $respData", "warn")
        return []
    }
    Set alertingDeviceIds = []
    respData.data.each { notification ->
        if (settings.wetOnlyOnUnreadAlerts && notification.read) {
            return
        }
        if (notification.message.contains("Leak") || settings.wetOnAnyUsageAlert) {
            alertingDeviceIds.add(notification.device_id)
        }
    }
    return alertingDeviceIds
}

void queryWaterUsage(String deviceId, String locationTZ, String callbackMethod, Map callbackData) {
    logger("queryWaterUsage($deviceId, $locationTZ, $callbackMethod, $callbackData) called", "trace")
    TimeZone tz = TimeZone.getTimeZone(locationTZ)
    String dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
    Date now = new Date()
    String nowDateTime = now.format(dateTimeFormat, tz)
    String nowMinusOneDateTime
    use (groovy.time.TimeCategory) {
        nowMinusOneDateTime = (now - 1.minutes).format(dateTimeFormat, tz)
    }
    Map body = [
        queries: [
            [
                request_id: "currentMin",
                bucket: "MIN",
                since_datetime: nowMinusOneDateTime,
            ],
            [
                request_id: "today",
                bucket: "DAY",
                since_datetime: nowDateTime,
            ],
            [
                request_id: "thisMonth",
                bucket: "MON",
                since_datetime: nowDateTime,
            ],
            [
                request_id: "thisYear",
                bucket: "YR",
                since_datetime: nowDateTime,
            ],
        ],
    ]
    Map params = [
        uri: getApiBase(),
        path: "/me/devices/$deviceId/query",
        headers: getDefaultAuthHeaders(),
        body: body,
    ]
    issueHttpRequest("POST", params, callbackMethod, callbackData)
}

Map parseQueryWaterUsageResponse(Map respData) {
    if (!respData.success) {
        logger("Non successful response from queryWaterUsage: $respData", "warn")
        return [:]
    }
    return [
        // Keys need to match argument values of setListElement in DH and attributes in DH
        thisCurrentMinFlow: respData.data.currentMin.value[0][0],
        todayFlow: respData.data.today.value[0][0],
        monthFlow: respData.data.thisMonth.value[0][0],
        yearFlow: respData.data.thisYear.value[0][0],
    ]
}

void updateLocation(String locationId, boolean newAwayMode, String callbackMethod, Map callbackData) {
    logger("updateLocation($locationId, $newAwayMode, $callbackMethod, $callbackData) called", "trace")
    Map params = [
        uri: getApiBase(),
        path: "/me/locations/$locationId",
        headers: getDefaultAuthHeaders(),
        body: [
            away_mode: newAwayMode,
        ],
    ]
    issueHttpRequest("PATCH", params, callbackMethod, callbackData)
}

void issueHttpRequest(String httpMethod, Map params, String callbackMethod, Map callbackData) {
    // Manually change this to true to cache API responses.
    // Useful only while debugging to avoid rate limit errors.
    boolean cacheResponsesForDebugging = false
    if (cacheResponsesForDebugging && httpMethod == "PATCH") {
        cacheResponsesForDebugging = false
    }
    if (cacheResponsesForDebugging) {
        logger("Requesting via cache: $params", "debug")
        Map respData = state[params.path]
        if (respData) {
            logger("Using cached response for ${params.path}", "info")
        } else {
            switch (httpMethod) {
                case "GET":
                    httpGet(params) { resp ->
                        respData = resp.data
                    }
                    break
                case "POST":
                    httpPostJson(params) { resp ->
                        respData = resp.data
                    }
                    break
                default:
                    logger("Unhandled $httpMethod in issueHttpRequest", "error")
                    break
            }
            state[params.path] = respData
        }
        runIn(1, callbackMethod, [data: [respData: respData, callbackData: callbackData]])
        return
    }

    if (settings.proxyHost) {
        params.remove("uri")
        params.method = httpMethod
        params.path = "/flumewater" + params.path
        params.headers["HOST"] = settings.proxyHost
        addCallbackDataToHttpHeader(params.headers, callbackData)
        logger("Requesting via local proxy: $params", "debug")
        sendHubCommand(new physicalgraph.device.HubAction(params, null, [callback: callbackMethod]))
        return
    }

    logger("Requesting: $params", "debug")
    switch (httpMethod) {
        case "GET":
            asynchttp_v1.get(callbackMethod, params, callbackData)
            break
        case "POST":
            asynchttp_v1.post(callbackMethod, params, callbackData)
            break
        case "PATCH":
            asynchttp_v1.patch(callbackMethod, params, callbackData)
            break
        default:
            logger("Unhandled $httpMethod in issueHttpRequest", "error")
            break
    }
}

void addCallbackDataToHttpHeader(Map headers, Map callbackData) {
    if (!callbackData) {
        return
    }
    headers["x-callback-data"] = groovy.json.JsonOutput.toJson(callbackData)
}

Map getCallbackDataFromHttpHeader(Map headers) {
    if (!headers.containsKey("x-callback-data")) {
        return [:]
    }
    return new groovy.json.JsonSlurper().parseText(headers["x-callback-data"])
}

// Local proxy code:
/*
const express = require('express');
const morgan = require('morgan');
const {createProxyMiddleware} = require('http-proxy-middleware');

const app = express();

// Configuration
const PORT = 3006;

// For logging requests
app.use(morgan('dev'));

app.use('/flumewater', createProxyMiddleware({
  target: 'https://api.flumewater.com',
  changeOrigin: true,
  pathRewrite: {
    [`^/flumewater`]: '',
  },
  onProxyReq: function(proxyReq, req, res) {
    proxyReq.removeHeader('x-callback-data');
  },
  onProxyRes: function(proxyRes, req, res) {
    proxyRes.headers['x-callback-data'] = req.headers['x-callback-data'];
  },
}));

app.listen(PORT, () => {
  console.log(`Proxy listening at port ${PORT}`);
});
*/