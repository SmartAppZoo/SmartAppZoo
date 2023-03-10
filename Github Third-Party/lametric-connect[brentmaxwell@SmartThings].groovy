/**
 *  LaMetric (Connect)
 *
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
  name: "LaMetric (Connect)",
  namespace: "thebrent",
  author: "Brent Maxwell",
  description: "Control your LaMetric Time",
  category: "",
  iconUrl: "https://developer.lametric.com/assets/smart_things/smart_things_60.png",
  iconX2Url: "https://developer.lametric.com/assets/smart_things/smart_things_120.png",
  iconX3Url: "https://developer.lametric.com/assets/smart_things/smart_things_120.png",
  singleInstance: true
)
{
  appSetting "clientId"
  appSetting "clientSecret"
}

preferences {
  page(name: "auth", title: "LaMetric", nextPage:"", content:"authPage", uninstall: true, install:true)
  page(name:"deviceDiscovery", title:"Device Setup", content:"deviceDiscovery", refreshTimeout:5);
}

mappings {
  path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
  path("/oauth/callback") {action: [GET: "callback"]}
}

import groovy.json.JsonOutput

def getEventNameListOfUserDeviceParsed(){ "EventListOfUserRemoteDevicesParsed" }
def getEventNameTokenRefreshed(){ "EventAuthTokenRefreshed" }

def installed() {
  initialize()
}

def updated() {
  sendEvent(name:"Updated", value:true)
  unsubscribe()
  initialize()
}

def initialize() {
  state.subscribe  = false;
  if (selecteddevice) {
    addDevice()
    subscribeNetworkEvents(true)
    refreshDevices();
  }
}

def getDeviceName() { "LaMetric Time" }
def getNameSpace() { "thebrent" }

def getDevices() {
  state.remoteDevices = state.remoteDevices ?: [:]
}

def getVerifiedDevices() {
  getDevices().findAll{ it?.value?.verified == true }
}

Map getSelectableDevice() {
  def devices = getVerifiedDevices()
  def map = [:]
  devices.each {
    def value = "${it.value.name}"
    def key = it.value.id
    map["${key}"] = value
  }
  return map
}

def refreshDevices(){
  listOfUserRemoteDevices()
  runIn(1800, "refreshDevices")
}

// DEVICE DISCOVERY
def deviceDiscovery() {
  def refreshInterval = 3
  int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
  state.deviceRefreshCount = deviceRefreshCount + refreshInterval

  def devices = getSelectableDevice()
  def numFound = devices.size() ?: 0
  subscribeNetworkEvents()

  if((deviceRefreshCount % 5) == 0) {
    verifyDevices()
  }

  return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:"", refreshInterval:refreshInterval, install:true, uninstall: true) {
    section("Please wait while we discover your ${getDeviceName()}. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
      input "selecteddevice", "enum", required:false, title:"Select ${getDeviceName()} (${numFound} found)", multiple:true, options:devices
    }
  }
}

def subscribeNetworkEvents(force=false) {
  if (force) {
    unsubscribe()
    state.subscribe = false
  }
  if(!state.subscribe) {
    subscribe(location, null, locationHandler, [filterEvents:false])
    state.subscribe = true
  }
}

def verifyDevices() {
  def devices = getDevices();
  for (it in devices) {
    def localIp = it?.value?.ipv4_internal;
    def apiKey = it?.value?.api_key;
    getAllInfoFromDevice(localIp, apiKey);
  }
}

def appHandler(evt) {
  if (evt.name == eventNameListOfUserDeviceParsed) {
    def newRemoteDeviceList
    try {
      newRemoteDeviceList = parseJson(evt.value)
    } catch (e) {
      log.debug "Wrong value ${e}"
    }
    if (newRemoteDeviceList) {
      def remoteDevices = getDevices();
      newRemoteDeviceList.each{deviceInfo ->
        if (deviceInfo) {
          def device = remoteDevices[deviceInfo.id]?:[:];
          deviceInfo.each() {
            device[it.key] = it.value;
          }
          remoteDevices[deviceInfo.id] = device;
        } else {
          log.debug ("empty device info")
        }
      }
      verifyDevices();
    } else {
      log.debug "wrong value ${newRemoteDeviceList}"
    }
  } else if (evt.name == getEventNameTokenRefreshed()) {
    state.refreshToken = evt.refreshToken
    state.authToken = evt.access_token
  }
}

def locationHandler(evt) {
  if (evt.name == "ssdpTerm") {
    log.debug "ignore ssdp"
  } else {
    def lanEvent = parseLanMessage(evt.description, true)
    if (lanEvent.body) {
      def parsedJsonBody;
      try {
        parsedJsonBody = parseJson(lanEvent.body);
      } catch (e) {
        log.debug ("non json response ignore $e");
      }
      if (parsedJsonBody) {
        if (parsedJsonBody.success) {
        } else {
          def deviceId = parsedJsonBody?.id;
          if (deviceId) {
            def devices = getDevices();
            def device = devices."${deviceId}";
            device.verified = true;
            device.dni = [device.serial_number, device.id].join('.')
            device.hub = evt?.hubId;
            device.volume = parsedJsonBody?.audio?.volume;
            def childDevice = getChildDevice(device.dni)
            if (childDevice) {
              childDevice.sendEvent(name: "id", value: parsedJsonBody?.id);
              childDevice.sendEvent(name: "name", value: parsedJsonBody?.name);
              childDevice.sendEvent(name: "serialNumber", value: parsedJsonBody?.serial_number);
              childDevice.sendEvent(name: "currentIP", value: device?.ipv4_internal);
              childDevice.sendEvent(name: "volume", value: parsedJsonBody?.audio?.volume);
              childDevice.sendEvent(name: "lqi", value: parsedJsonBody?.wifi.strength);
              childDevice.sendEvent(name: "level", value: parsedJsonBody?.display?.brightness);
              if(parsedJsonBody?.bluetooth?.active && parsedJsonBody?.bluetooth?.discoverable && parsedJsonBody?.bluetooth?.pairable) {
                childDevice.sendEvent(name: "bluetoothState", value: "pairable")
              }
              else if(parsedJsonBody?.bluetooth?.active && parsedJsonBody?.bluetooth?.discoverable) {
                childDevice.sendEvent(name: "bluetoothState", value: "discoverable")
              }
              else if(parsedJsonBody?.bluetooth?.active) {
                childDevice.sendEvent(name: "bluetoothState", value: "active")
              }
              else {
                childDevice.sendEvent(name: "bluetoothState", value: "off")
              }
            }
          }
        }
      }
    }
  }
}

def addDevice() {
  def devices = getVerifiedDevices()
  def devlist

  if (!(selecteddevice instanceof List)) {
    devlist = [selecteddevice]
  } else {
    devlist = selecteddevice
  }

  devlist.each { dni ->
    def newDevice = devices[dni];
    if (newDevice) {
      def d = getChildDevice(newDevice.dni)
      if(!d) {
        def deviceName = newDevice.name
        d = addChildDevice(getNameSpace(), getDeviceName(), newDevice.dni, newDevice.hub, [label:"${deviceName}"])
        def childDevice = getChildDevice(d.deviceNetworkId)
        childDevice.sendEvent(name: "apiKey", value:newDevice.api_key)
        childDevice.sendEvent(name: "id", value: newDevice.id);
        childDevice.sendEvent(name: "name", value: newDevice.name);
        childDevice.sendEvent(name: "serialNumber", value: newDevice.serial_number);
        childDevice.sendEvent(name: "currentIP", value: newDevice.ipv4_internal);
      } else {
        log.trace "${d.displayName} with id $dni already exists"
      }
    }
  }
}

// DEVICE HANDLER COMMANDs API

def resolveDNI2Device(dni) {
  getDevices().find { it?.value?.dni == dni }?.value;
}

def requestRefreshDeviceInfo (dni) {
  //  def devices = getDevices();
  //    def concreteDevice = devices[dni];
  //    requestDeviceInfo(conreteDevice);
  def device = resolveDNI2Device(dni);
  def localIp = device?.ipv4_internal;
  def apiKey = device?.api_key;
  getAllInfoFromDevice(localIp, apiKey);
}

private poll(dni) {
  def device = resolveDNI2Device(dni);
  def localIp = device?.ipv4_internal;
  def apiKey = device?.api_key;
  getDevice(localIp, apiKey);
}

// LOCAL API

def getLocalApiDeviceInfoPath() { "/api/v2/info" }
def getLocalApiSendNotificationPath() { "/api/v2/device/notifications" }
def getLocalApiIndexPath() { "/api/v2/device" }
def getLocalApiUser() { "dev" }

def getDevice(localIp, apiKey) {
  if (localIp && apiKey) {
    def hubCommand = new physicalgraph.device.HubAction([
      method: "GET",
      path: localApiIndexPath+"?fields=id,name,serial_number,os_version,mode,model,audio,display,bluetooth,wifi",
      headers: [
        HOST: "${localIp}:8080",
        Authorization: "Basic ${"${localApiUser}:${apiKey}".bytes.encodeBase64()}"
      ]
    ])
    sendHubCommand(hubCommand)
  }
}

def requestDeviceInfo(localIp, apiKey) {
  if (localIp && apiKey) {
    def command = new physicalgraph.device.HubAction([
      method: "GET",
      path: localApiDeviceInfoPath,
      headers: [
          HOST: "${localIp}:8080",
          Authorization: "Basic ${"${localApiUser}:${apiKey}".bytes.encodeBase64()}"
    ]])
    sendHubCommand(command)
    return command;
  } else {
    log.debug ("Unknown api key or ip address ${localIp} ${apiKey}")
  }
}

def sendNotificationMessageToDevice(dni, data) {
  def device = resolveDNI2Device(dni);
  def localIp = device?.ipv4_internal;
  def apiKey = device?.api_key;
  if (localIp && apiKey) {
    sendHubCommand(new physicalgraph.device.HubAction([
      method: "POST",
      path: localApiSendNotificationPath,
      body: data,
      headers: [
        HOST: "${localIp}:8080",
        Authorization: "Basic ${"${localApiUser}:${apiKey}".bytes.encodeBase64()}",
        "Content-type":"application/json",
        "Accept":"application/json"
      ]
    ]))
  }
}

def sendApiCallToDevice(dni, method, path, data) {
  def device = resolveDNI2Device(dni);
  def localIp = device?.ipv4_internal;
  def apiKey = device?.api_key;
  if (localIp && apiKey) {
    sendHubCommand(new physicalgraph.device.HubAction([
      method: method,
      path: path,
      body: data,
      headers: [
        HOST: "${localIp}:8080",
        Authorization: "Basic ${"${localApiUser}:${apiKey}".bytes.encodeBase64()}",
        "Content-type":"application/json",
        "Accept":"application/json"
      ]
    ]))
  }
}



// CLOUD METHODS
void listOfUserRemoteDevices() {
  def deviceList = []
  if (state.accessToken) {
    def deviceListParams = [
      uri: apiEndpoint,
      path: apiUserMeDevicesList,
      headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.authToken}"]
    ]
    def result;
    try {
      httpGet(deviceListParams){ resp ->
        if (resp.status == 200) {
          deviceList = resp.data
          def remoteDevices = getDevices();
          for (deviceInfo in deviceList) {
            if (deviceInfo) {
              def device = remoteDevices[deviceInfo.id.toString()]?:[:];
              for (it in deviceInfo ) {
                device."${it.key}" = it.value;
              }
              remoteDevices."${deviceInfo.id}" = device;
            } else {
              log.debug ("empty device info")
            }
          }
          verifyDevices();
        } else {
          log.debug "http status: ${resp.status}"
        }
      }
    } catch (groovyx.net.http.HttpResponseException e) {
      log.debug("failed to get device list ${e}")
      def status = e.response.status
      if (status == 401) {
          state.action = "refreshDevices"
          log.debug "Refreshing your auth_token!"
          refreshAuthToken()
      }
      return;
    }
  } else {
    log.debug ("no access token to fetch user device list");
    return;
  }
}

// OAUTH
def getServerUrl() { "https://graph.api.smartthings.com" }
def getShardUrl() { getApiServerUrl() }
def getCallbackUrl() { "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl() { "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${shardUrl}" }
def getApiEndpoint() { "https://developer.lametric.com" }
def getTokenUrl() { "${apiEndpoint}${apiTokenPath}" }
def getAuthScope() { [ "basic", "devices_read" ] }
def getSmartThingsClientId() { appSettings.clientId }
def getSmartThingsClientSecret() { appSettings.clientSecret }
def getApiTokenPath() { "/api/v2/oauth2/token" }
def getApiUserMeDevicesList() { "/api/v2/users/me/devices" }

def toQueryString(Map m) {
  return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def composeScope(List scopes) {
  def result = "";
  scopes.each(){ scope ->
    result += "${scope} "
  }
  if (result.length())
    return result.substring(0, result.length() - 1);
  return "";
}

def authPage() {
  if(!state.accessToken) { //this is to access token for 3rd party to make a call to connect app
    state.accessToken = createAccessToken()
  }

  def description
  def uninstallAllowed = false
  def oauthTokenProvided = false

  if(state.authToken) {
    description = "You are connected."
    uninstallAllowed = true
    oauthTokenProvided = true
  } else {
    description = "Click to enter LaMetric Credentials"
  }

  def redirectUrl = buildRedirectUrl
  // get rid of next button until the user is actually auth'd
  if (!oauthTokenProvided) {
    return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall:uninstallAllowed) {
      section(){
        paragraph "Tap below to log in to the LaMatric service and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
        href url:redirectUrl, style:"embedded", required:true, title:"LaMetric", description:description
      }
    }
  } else {
    subscribeNetworkEvents()
    listOfUserRemoteDevices()
    return deviceDiscovery();
  }
}

private refreshAuthToken() {
  if(!state.refreshToken) {
    log.warn "Can not refresh OAuth token since there is no refreshToken stored"
  } else {
    def refreshParams = [
      method: 'POST',
      uri   : apiEndpoint,
      path  : apiTokenPath,
      body : [
        grant_type: 'refresh_token',
        refresh_token: "${state.refreshToken}",
        client_id : smartThingsClientId,
        client_secret: smartThingsClientSecret,
        redirect_uri: callbackUrl
      ],
    ]

    def notificationMessage = "is disconnected from SmartThings, because the access credential changed or was lost. Please go to the LaMetric (Connect) SmartApp and re-enter your account login credentials."
    //changed to httpPost
    try {
      def jsonMap
      httpPost(refreshParams) { resp ->
        if(resp.status == 200) {
          jsonMap = resp.data
          if(resp.data) {
            state.refreshToken = resp?.data?.refresh_token
            state.authToken = resp?.data?.access_token
            if(state.action && state.action != "") {
              "${state.action}"()
              state.action = ""
            }
          } else {
            log.warn ("No data in refresh token!");
          }
          state.action = ""
        }
      }
    } catch (groovyx.net.http.HttpResponseException e) {
      log.error "refreshAuthToken() >> Error: e.statusCode ${e.statusCode}"
      log.debug e.response.data;
      def reAttemptPeriod = 300 // in sec
      if (e.statusCode != 401) { // this issue might comes from exceed 20sec app execution, connectivity issue etc.
        runIn(reAttemptPeriod, "refreshAuthToken")
      } else if (e.statusCode == 401) { // unauthorized
        state.reAttempt = state.reAttempt + 1
        log.warn "reAttempt refreshAuthToken to try = ${state.reAttempt}"
        if (state.reAttempt <= 3) {
          runIn(reAttemptPeriod, "refreshAuthToken")
        } else {
          sendPushAndFeeds(notificationMessage)
          state.reAttempt = 0
        }
      }
    }
  }
}

def callback() {
  def code = params.code
  def oauthState = params.state

  if (oauthState == state.oauthInitState){
    def tokenParams = [
      grant_type: "authorization_code",
      code      : code,
      client_id : smartThingsClientId,
      client_secret: smartThingsClientSecret,
      redirect_uri: callbackUrl
    ]
    try {
      httpPost(uri: tokenUrl, body: tokenParams) { resp ->
        state.refreshToken = resp.data.refresh_token
        state.authToken = resp.data.access_token
      }
    } catch (e) {
      log.debug "fail ${e}";
    }
    if (state.authToken) {
      success()
    } else {
      fail()
    }
  } else {
    log.error "callback() failed oauthState != state.oauthInitState"
  }
}

def oauthInitUrl() {
  state.oauthInitState = UUID.randomUUID().toString()

  def oauthParams = [
    response_type: "code",
    scope: composeScope(authScope),
    client_id: smartThingsClientId,
    state: state.oauthInitState,
    redirect_uri: callbackUrl
  ]
  redirect(location: "${apiEndpoint}/api/v2/oauth2/authorize?${toQueryString(oauthParams)}")
}

def success() {
  def message = """
    <p>Your LaMetric Account is now connected to SmartThings!</p>
    <p>Click 'Done' to finish setup.</p>
  """
  connectionStatus(message)
}

def fail() {
  def message = """
    <p>The connection could not be established!</p>
    <p>Click 'Done' to return to the menu.</p>
  """
  connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
  def redirectHtml = ""
  if (redirectUrl) {
    redirectHtml = """
      <meta http-equiv="refresh" content="3; url=${redirectUrl}" />
    """
  }

  def html = """
    <!DOCTYPE html>
    <html lang="en"><head>
      <meta charset="UTF-8">
      <meta content="width=device-width" id="viewport" name="viewport">
      <style>
        @font-face {
        font-family: 'latoRegular';
        src: url("https://developer.lametric.com/assets/fonts/lato-regular-webfont.eot");
        src: url("https://developer.lametric.com/assets/fonts/lato-regular-webfont.eot?#iefix") format("embedded-opentype"),
        url("https://developer.lametric.com/assets/fonts/lato-regular-webfont.woff") format("woff"),
        url("https://developer.lametric.com/assets/fonts/lato-regular-webfont.ttf") format("truetype"),
        url("https://developer.lametric.com/assets/fonts/lato-regular-webfont.svg#latoRegular") format("svg");
        font-style: normal;
        font-weight: normal; }
        .clearfix:after, .mobile .connect:after {
        content: "";
        clear: both;
        display: table; }

        .transition {
        transition: all .3s ease 0s; }
        html, body {
        height: 100%;
        }
        body{
        margin: 0;
        padding: 0;
        background: #f0f0f0;
        color: #5c5c5c;
        min-width: 1149px;
        font-family: 'latoRegular', 'Lato';
        }
        .fixed-page #page {
        min-height: 100%;
        background: url(https://developer.lametric.com/assets/smart_things/page-bg.png) 50% 0 repeat-y;
        }
        .mobile {
        min-width: 100%;
        color: #757575; }
        .mobile .wrap {
        margin: 0 auto;
        padding: 0;
        max-width: 640px;
        min-width: inherit; }
        .mobile .connect {
        width: 100%;
        padding-top: 230px;
        margin-bottom: 50px;
        text-align: center; }
        .mobile .connect img {
        max-width: 100%;
        height: auto;
        vertical-align: middle;
        display: inline-block;
        margin-left: 2%;
        border-radius: 15px; }
        .mobile .connect img:first-child {
        margin-left: 0; }
        .mobile .info {
        width: 100%;
        margin: 0 auto;
        margin-top: 50px;
        margin-bottom: 50px; }
        .mobile .info p {
        max-width: 80%;
        margin: 0 auto;
        margin-top: 50px;
        font-size: 28px;
        line-height: 50px;
        text-align: center; }

        @media screen and (max-width: 639px) {
        .mobile .connect{
        padding-top: 100px; }
        .mobile .wrap {
        margin: 0 20px; }
        .mobile .connect img {
        width: 16%; }
        .mobile .connect img:first-child, .mobile .connect img:last-child {
        width: 40%; }
        .mobile .info p{
        font-size: 18px;
        line-height: 24px;
        margin-top: 20px; }
        }
      </style>
    </head>
    <body class="fixed-page mobile">
      <div id="page">
        <div class="wrap">
          <div class="connect">
            <img src="https://developer.lametric.com/assets/smart_things/product.png" width="190" height="190"><img src="https://developer.lametric.com/assets/smart_things/connected.png" width="87" height="19"><img src="https://developer.lametric.com/assets/smart_things/product-1.png" width="192" height="192">
          </div>
          <div class="info">${message}</div>
        </div>
      </div>
    </body>
  </html>
  """
  render contentType: 'text/html', data: html
}
