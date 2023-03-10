/**
 *  MyQ Connect
 *
 *  Copyright 2016 Eric Mey
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
  name: "MyQ",
  namespace: "ericmey",
  author: "Eric Mey",
  description: "MyQ SmartApp",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page(name: "prefLogin", title: "MyQ Connect")
  page(name: "prefDeviceList", title: "MyQ Devices")
}

def prefLogin() {
  def showUninstall = username != null && password != null

  return dynamicPage(name: "prefLogin", title: "Login to MyQ Connect", nextPage: "prefDeviceList", uninstall: showUninstall, install: false) {
    section("Login Credentials") {
      input("username", "email", title: "Email", description: "MyQ Email")
      input("password", "password", title: "Password", description: "MyQ Password")
    }

    section("Gateway Brand") {
      input(name: "brand", title: "Gateway Brand", type: "enum", metadata: [values: ["Liftmaster", "Chamberlain", "Craftsman"]])
    }

    section("Advanced Options") {
      input(name: "contactSensorTrigger", title: "Contact Sensor to Trigger Refresh", type: "capability.contactSensor", required: "false", multiple: "true")
      input(name: "accelerationSensorTrigger", title: "Acceleration Sensor to Trigger Refresh", type: "capability.accelerationSensor", required: "false", multiple: "true")
    }
  }
}

def prefDeviceList() {
  if (loginForce()) {
    def doorList = deviceDoorList()
    def lightList = deviceLightList()

    if ((doorList) || (lightList)) {
      return dynamicPage(name: "prefDeviceList", title: "Devices", install: true, uninstall: true) {
        if (doorList) {
          section("Select Garage Door Opener / Gate Operator") {
            input(name: "doors", type: "enum", required: false, multiple: true, metadata: [values: doorList])
          }
        }

        if (lightList) {
          section("Select Remote Light") {
            input(name: "lights", type: "enum", required: false, multiple: true, metadata: [values: lightList])
          }
        }
      }
    } else {
      def devList = deviceList()

      return dynamicPage(name: "prefDeviceList",  title: "Error", install: true, uninstall: true) {
        section("") {
          paragraph "Could not find supported device(s). Verify compatability with these devices: " +  devList
        }
      }
    }
  } else {
    return dynamicPage(name: "prefDeviceList",  title: "Error", install: false, uninstall: true) {
      section("") {
        paragraph "The email or password you entered is incorrect. Try again."
      }
    }
  }
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

def uninstalled() {
  unsubscribe()
  unschedule()
  getAllChildDevices.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

def initialize() {
  loginCheckStatus()

  state.polling = [last: 0, rescheduler: now()]
  state.data = [:]

  def doorsList = deviceDoorList()
  def lightsList = deviceLightList()
  def selectedDevices = [] + deviceSelected("doors") + deviceSelected("lights")

  selectedDevices.each {
    if (!getChildDevice(it)) {
      if (it.contains("GarageDoorOpener")) {
        addChildDevice("ericmey", "MyQ Garage Door Opener", it, null, ["name": "MyQ: " + doorsList[it]])
      }
      if (it.contains("LightController")) {
        addChildDevice("ericmey", "MyQ Remote Light", it, null, ["name": "MyQ: " + lightsList[it]])
      }
    }
  }

  def deleteDevices = (selectedDevices) ? (getChildDevices().findAll { !selectedDevices.contains(it.deviceNetworkId) }) : getAllChildDevices()
  deleteDevices.each { deleteChildDevice(it.deviceNetworkId) }

  subscribe(location, "sunrise", runRefresh)
  subscribe(location, "sunset", runRefresh)
  subscribe(location, "mode", runRefresh)
  subscribe(location, "sunriseTime", runRefresh)
  subscribe(location, "sunsetTime", runRefresh)

  if (settings.contactSensorTrigger) {
    subscribe(settings.contactSensorTrigger, "contact", runRefresh)
  }
  if (settings.accelerationSensorTrigger) {
    subscribe(settings.accelerationSensorTrigger, "acceleration", runRefresh)
  }

  runRefresh()
}

private apiAppID() {
  if (settings.brand == "Craftsman") {
    return "QH5AzY8MurrilYsbcG1f6eMTffMCm3cIEyZaSdK/TD/8SvlKAWUAmodIqa5VqVAs"
  } else {
    return "JVM/G9Nwih5BwKgNCjLxiFUQxQijAebyyg8QUHr7JOrP+tuPb8iHfRHKwTmDzHOu"
  }
}

private apiGet(apiPath, apiQuery=[], callback={}) {
  apiQuery = [appId: apiAppID()] + apiQuery

  if (state.session.securityToken) {
    apiQuery = apiQuery + [SecurityToken: state.session.securityToken]
  }

  try {
    httpGet([uri: apiURL(), path: apiPath, query: apiQuery]) { response ->
      callback(response)
    }
  }  catch (SocketException e)  {
    log.debug "API Error: $e"
  }
}

private apiPut(apiPath, apiBody=[], callback={}) {
  apiBody = [ApplicationId: getApiAppID()] + apiBody

  if (state.session.securityToken) {
    apiBody = apiBody + [SecurityToken: state.session.securityToken]
  }

  def apiQuery = [appId: apiAppID()]

  if (state.session.securityToken) {
    apiQuery = apiQuery + [SecurityToken: state.session.securityToken]
  }

  try {
    httpPut([uri: apiURL(), path: apiPath, contentType: "application/json; charset=utf-8", body: apiBody, query: apiQuery]) { response ->
      callback(response)
    }
  } catch (SocketException e) {
    log.debug "API Error: $e"
  }
}

def apiSendCommand(child, attributeName, attributeValue) {
  if (loginCheckStatus()) {
    apiPut("/api/v4/deviceattribute/putdeviceattribute", [MyQDeviceId: childDeviceID(child), AttributeName: attributeName, AttributeValue: attributeValue])
    return true
  }
}

private apiURL() {
  if (settings.brand == "Craftsman") {
    return "https://craftexternal.myqdevice.com"
  } else {
    return "https://myqexternal.myqdevice.com"
  }
}

def childDeviceID(child) {
  return child.device.deviceNetworkId.split("\\|")[2]
}

private deviceDoorList() {
  def deviceList = [:]
  apiGet("/api/v4/userdevicedetails/get", []) { response ->
    if (response.status == 200) {
      response.data.Devices.each { device ->
        // 02: MyQ Garage Door
        // 05: Gate Controller
        // 07: MyQ Garage Door (WiFi)
        // 17: Garage Door Opener WGDO
        if (device.MyQDeviceTypeId == 2 || device.MyQDeviceTypeId == 5 || device.MyQDeviceTypeId == 7 || device.MyQDeviceTypeId == 17) {
          def dni = [app.id, "GarageDoorOpener", device.MyQDeviceId].join('|')
          device.Attributes.each {
            if (it.AttributeDisplayName=="desc") {
              deviceList[dni] = it.Value
            }
            if (it.AttributeDisplayName=="doorstate") {
              state.data[dni] = [status: it.Value, lastAction: it.UpdatedTime]
            }
          }
        }
      }
    }
  }
  return deviceList
}

def deviceLastActivity(child) {
  return state.data[child.device.deviceNetworkId].lastAction.toLong()
}

private deviceLightList() {
  def deviceList = [:]
  apiGet("/api/v4/userdevicedetails/get", []) { response ->
    if (response.status == 200) {
      response.data.Devices.each { device ->
        if (device.MyQDeviceTypeId == 3) {
          def dni = [app.id, "LightController", device.MyQDeviceId].join('|')
          device.Attributes.each {
            if (it.AttributeDisplayName == "desc") {
              deviceList[dni] = it.Value
            }
            if (it.AttributeDisplayName == "lightstate") {
              state.data[dni] = [status: it.Value]
            }
          }
        }
      }
    }
  }
  return deviceList
}

private deviceList() {
  def deviceList = []
  apiGet("/api/v4/userdevicedetails/get", []) { response ->
    if (response.status == 200) {
      response.data.Devices.each { device ->
        log.debug "MyQDeviceTypeId : " + device.MyQDeviceTypeId.toString()
        if (!(device.MyQDeviceTypeId == 1 || device.MyQDeviceTypeId == 2 || device.MyQDeviceTypeId == 3 || device.MyQDeviceTypeId == 5 || device.MyQDeviceTypeId == 7)) {
          deviceList.add(device.MyQDeviceTypeId.toString() + "|" + device.TypeID)
        }
      }
    }
  }
  return deviceList
}

def deviceSelected(settingsName) {
  def selectedDevices = []
  (!settings.get(settingsName))?:((settings.get(settingsName)?.getAt(0)?.size() > 1) ? settings.get(settingsName)?.each { selectedDevices.add(it) } : selectedDevices.add(settings.get(settingsName)))
  return selectedDevices
}

def deviceStatus(child) {
  return state.data[child.device.deviceNetworkId].status
}

private deviceUpdateData() {
  if (login()) {
    state.polling["last"] = now()
    return (deviceDoorList() || deviceLightList()) ? true : false
  } else {
    return false
  }
}

private login() {
  apiGet("/api/user/validate", [username: settings.username, password: settings.password]) { response ->
    if (response.status == 200) {
      if (response.data.SecurityToken != null) {
        state.session.brandID = response.data.BrandId
        state.session.brandName = response.data.BrandName
        state.session.securityToken = response.data.SecurityToken
        state.session.expiration = now() + 150000
        return true
      } else {
        return false
      }
    } else {
      return false
    }
  }
}

private loginCheckStatus() {
  return !(state.session.expiration > now()) ? login() : true
}

private loginForce() {
  state.session = [brandID: 0, brandName: settings.brand, securityToken: null, expiration: 0]
  state.polling = [last: 0, rescheduler: now()]
  state.data = [:]
  return login()
}

def refresh() {
  if (updateDeviceData()) {
    log.info "Refreshing data..."
    getAllChildDevices().each {
      it.updateDeviceStatus(state.data[it.deviceNetworkId].status)
      if (it.deviceNetworkId.contains("GarageDoorOpener")) {
        it.updateDeviceLastActivity(state.data[it.deviceNetworkId].lastAction.toLong())
      }
    }
  }

  if ((state.polling["rescheduler"]?:0) + 2400000 < now()) {
    log.info "Rescheduling..."
    runEvery30Minutes(refreshEvent)
    state.polling["rescheduler"] = now()
  }
}

def refreshEvent(evt) {
  if (evt) {
    log.info "Event " + evt.displayName + " triggered refresh"
    runIn(30, refreshTrigger)
  }

  log.info "Last refresh was "  + ((now() - state.polling["last"]) / 60000) + " minutes ago"

  if (((state.polling["last"]?:0) + 600000 < now()) && canSchedule()) {
    log.info "Scheduling..."
    schedule("* */5 * * * ?", refresh)
  }

  refresh()

  if (!evt) {
    state.polling["rescheduler"] = now()
  }
}

def refreshTrigger() {
  log.info "Refresh Triggered"
  refresh()
}
