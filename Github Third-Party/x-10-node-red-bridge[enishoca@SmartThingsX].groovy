/**
 *  X-10 Node Red Bridge Smart App
 *
 * 	Author: Enis Hoca
 *   - enishoca@outlook.com
 *
 *  Copyright 2018 Enis Hoca
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

import groovy.json.JsonSlurper

definition(
  name: "X-10 Node Red Bridge",
  namespace: "enishoca",
  author: "Enis Hoca",
  description: "A bridge between SmartThings and X-10 via Node Red",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  iconX2Url: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  iconX3Url: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png",
  singleInstance: true
)

preferences {
  page(name: "pageMain")
  page(name: "buttonSettings")
  page(name: "switchSettings")
  page(name: "securitySettings")
}

def pageMain() {
  def installed = app.installationState == "COMPLETE"
  dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {

    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }

    section("Node Red Settings") {
      input "nodeRedAddress", "text", title: "Node Red IP Address", description: "(ie. 192.168.1.10)", required: true
      input "nodeRedPort", "text", title: "Node Red  Port", description: "(ie. 1880)", required: true, defaultValue: "1880"
    }

    if (installed) {
      section(title: "X-10 Switches and Modules") {
        href "switchSettings", title: "Switches and Modules", description: "Tap here to add or manage X-10 Switches and Modules", 
              image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "switchSettings"
      }
      section(title: "X-10 Remotes and Motion Sensors") {
        href "buttonSettings", title: "Remotes and Motion Sensors", description: "Tap here to add or manage X-10 Remotes and Motion Sensors", 
             image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "buttonSettings"
      }
      section(title: "X-10 Security Sensors") {
        href "securitySettings", title: "Security Remotes and Sensors", description: "Tap here to add or manage X-10 Security Remotes and Sensors", 
             image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", required: false, page: "buttonSettings"
      }

    }
  }
}

def buttonSettings() {
  dynamicPage(name: "buttonSettings", title: "X-10 Remotes and Motion Sensors", install: false, uninstall: false) {
    section() {
      app(name: "childButtons", appName: "X-10 Node Red Button Child", namespace: "enishoca", title: "Add a new remote button or motion sensor...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def switchSettings() {
  dynamicPage(name: "switchSettings", title: "X-10 Switches and Modules", install: false, uninstall: false) {
    section() {
      app(name: "childSwitches", appName: "X-10 Node Red Switch Child", namespace: "enishoca", title: "Add a new switches or module...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def securitySettings() {
  dynamicPage(name: "securitySettings", title: "X-10 Security Sensors", install: false, uninstall: false) {
    section() {
      app(name: "childSecurity", appName: "X-10 Node Red Security Child", namespace: "enishoca", title: "Add a new door/window or other security sensor...", 
          image: "https://raw.githubusercontent.com/enishoca/SmartThingsX/master/x1oredb.png", multiple: true)
    }
  }

}

def installed() {
  initialize()
}

def uninstalled() {

}

def initialize() {
  sendCommand('/register/?' + getNotifyAddress())
  //log.debug "there are ${childApps.size()} child smartapps"

  subscribe(location, null, lanResponseHandler, [filterEvents: false])
}

def updated() {
  unsubscribe()
  initialize()
}

def removeChildDevices(delete) {
  getChildDevices().find {
    d -> d.deviceNetworkId.startsWith(theDeviceNetworkId)
  }

  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

def lanResponseHandler(evt) {
  //log.debug "lanResponseHandler settings: ${settings}"
  //log.debug "lanResponseHandler state: ${state}"
  //log.debug "lanResponseHandler Event: ${evt.stringValue}"

  def map = stringToMap(evt.stringValue)
  //MAC is set on both HTTP GET response and NOTIFY
  // if MAC and the SERVER:PORT don't match our server return
  if ((map.mac != state.nodeRedMac) &&
    (map.ip != convertIPtoHex(settings.nodeRedAddress) ||
      map.port != convertPortToHex(settings.nodeRedPort))) {
    //log.debug "lanResponseHandler not us - returning"
    return
  }

  def headers = parseHttpHeaders(map.headers);
  //log.trace "lanResponseHandler Headers: ${headers}"

  //if this is a registration response update the saved mac
  switch (headers.X10NodeRed) {
    case 'Registered':
      log.trace "lanResponseHandler Updating MAC address for Node Red Server: ${state.nodeRedMac}"
      state.nodeRedMac = map.mac
      break;
    case 'DeviceUpdate':
      def body = parseHttpBody(map.body);
      log.trace "lanResponseHandler Body: ${body}"
      processEvent(body)
      break;
    //default:
    //  log.trace "lanResponseHandler Our server - Not our app"
  }
}

private processEvent(body) {
  //log.trace "processEvent Body: ${body}"
  //[protocol:rf, unitcode:6, direction:rx, state:on, housecode:h]
  def deviceString = ""
  def status

  def housecodekey = "housecode"
  if (body.containsKey(housecodekey)) {
    def housecode = body.housecode.toUpperCase()
    def unitcode = body.unitcode
    status = body.state;
    deviceString = "${housecode}-${unitcode}"
    //log.debug "body has housecodekey - status: ${status}"
    updateDevice(deviceString, status)
  }
}

private updateDevice(deviceString, status) {
  //iterate through all child apps and look for state.idX10device
  //compare that with address if it matches return settings.buttonSwitch

  log.debug "updateDevice: Button ${deviceString} ${status} pressed"
  sendLocationEvent(name: "X10RemoteEvent-${deviceString}", value: "updatedX10DeviceStatus", data: ["deviceString": deviceString, 
                    "status": status], source: "DEVICE", isStateChange: true)

}

private sendCommand(path) {
  log.trace "send comand to Node Red Server at: ${path}"

  if (settings.nodeRedAddress.length() == 0 ||
    settings.nodeRedPort.length() == 0) {
    log.error "SmartThings Node Red server configuration not set!"
    return
  }

  def host = getnodeRedAddress()
  def headers = [: ]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  def hubAction = new physicalgraph.device.HubAction(
    method: "GET",
    path: path,
    headers: headers
  )
  //log.trace hubAction
  sendHubCommand(hubAction)
}

private parseHttpHeaders(headers) {
  def obj = [: ]

  try {
    new String(headers.decodeBase64()).split("\r\n").each {
      param ->
        def nameAndValue = param.split(":")
      obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
    }
  } catch (e) {
    //prob not json so return null
    return null
  }
  return obj
}

private parseHttpBody(body) {
  def obj = null;
  try {
    if (body) {
      def slurper = new JsonSlurper()
      obj = slurper.parseText(new String(body.decodeBase64()))
    }
  } catch (e) {
    //prob not json so return null
  }
  return obj
}

private getnodeRedAddress() {
  return settings.nodeRedAddress + ":" + settings.nodeRedPort
}

private getNotifyAddress() {
  return "ip_for_st=" + settings.hostHub.localIP + "&port_for_st=" + settings.hostHub.localSrvPortTCP
}

private String convertIPtoHex(ipAddress) {
  if (!ipAddress) return;
  String hex = ipAddress.tokenize('.').collect {
    String.format('%02x', it.toInteger())
  }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format('%04x', port.toInteger()).toUpperCase()
  return hexport
}

private getDevicebyNetworkId(deviceNetworkId) {
  return getChildDevices().find {
    d -> d.deviceNetworkId.startsWith((String) deviceNetworkId)
  }
}

def sendStatetoX10(deviceString, state) {
  log.debug "sendStatetoX10  deviceString ${deviceString}"
  sendCommand('/push/?' + "device=${deviceString}&action=${state}")
}

def getHostHubId() {
  return settings.hostHub.id
}