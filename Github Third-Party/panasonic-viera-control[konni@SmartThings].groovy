/**
 *  Panasonic Viera Control
 *
 *  Copyright 2017 H&aacute;kon Birgisson
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

import groovy.json.JsonSlurper


definition(
    name: "Panasonic Viera Control",
    namespace: "konni",
    author: "H&aacute;kon Birgisson",
    description: "Control Point for Panasonic Viera TV",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/konni/SmartThings/master/images/konni-viera.png",
    iconX2Url: "https://raw.githubusercontent.com/konni/SmartThings/master/images/konni-viera@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/konni/SmartThings/master/images/konni-viera@2x.png")


preferences {
	section("SmartThings Hub") {
    input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  }
  section("Panasonic TV") {
    input name: "tvName", type: "text", title: "Name", required: true, defaultValue: "TV"
    input name: "tvIp", type: "text", title: "IP", required: true
    input name: "tvInstances", type: "enum", title: "Rooms", required: true, multiple: true, options: ["Living Room","Bedroom","Kitchen"]
  }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	
    addChildDevices()
    log.debug("Subscribing to Events")
    subscribeToEvents()
}

def uninstalled() {
  removeChildDevices()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	addChildDevices()
    subscribeToEvents()
}


def subscribeToEvents() { 
  subscribe(location, "switch.on", onHandler, [filterEvents: false])
  subscribe(location, null, lanResponseHandler, [filterEvents:false])
  log.debug "Subscriptions applied"
}

def onHandler(evt) {
   debug.log "Switch On Event"
}

def lanResponseHandler(evt) {

  log.debug "LAN Response"
  // log.trace ${evt}

  def map = stringToMap(evt.stringValue)
  
  
  //verify that this message is from TV IP
  if (!map.ip || map.ip != convertIPtoHex(settings.tvIp)) {
    return
  }

  def headers = getHttpHeaders(map.headers);
  def body = getHttpBody(map.body);
  log.trace "Headers: ${headers}"
  log.trace "Body: ${body}"

}

private addChildDevices() {
  // add Panasonic Viera device
  settings.tvInstances.each {
    def deviceId = getDeviceId(it)
    if (!getChildDevice(deviceId)) {
      addChildDevice("com.konni.smartthings", "Panasonic Viera TV", deviceId, hostHub.id, ["name": it, label: "${settings.tvName}: ${it}", completedSetup: true])
      log.debug "Added Panasonic TV: ${deviceId}"
    }
  }

  childDevices*.refresh()
}

private getDeviceId(room) {
  return "viera|${settings.tvIp}|${room}"
}


private removeChildDevices() {
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

private sendCommand(command) {
  log.info "TV send command: ${action} to IP: " + getTVAddress()

  def type = "command"
  def action = "X_SendKey"
  
  def urn = getUrn(type);
  def body = getXmlCommand(action, command, urn)
  log.info "XML Body: ${body}"
 
  log.debug "Command URL " + getCommandUrl(type)
  
  def mapbody = [X_KeyEvent: command]
  
  def hubAction = new physicalgraph.device.HubSoapAction(
    path:    getCommandUrl(type),
    urn:     "urn:${urn}",
    action:  action,
    body:    mapbody,
    headers: [Host:getTVAddress(), Connection: "close"]
  )
  
  log.debug "## Hub Action ##"
  log.trace hubAction
  
  sendHubCommand(hubAction)
  log.trace "Hub Command sent"  
}

private sendVolume(volume) {
  log.trace "Adjust volume"
    
  def type = "render"
  def action = "SetVolume"
  
  def urn = getUrn(type);
  def body = getXmlCommand(action, command, urn)
  log.info "XML Body: ${body}"
 
  log.debug "Command URL " + getCommandUrl(type)
  
  def mapbody = [InstanceID: 0, Channel: 'Master',  DesiredVolume: volume]
  
  def hubAction = new physicalgraph.device.HubSoapAction(
    path:    getCommandUrl(type),
    urn:     "urn:${urn}",
    action:  action,
    body:    mapbody,
    headers: [Host:getTVAddress(), Connection: "close"]
  )
  
  log.debug "## Hub Action ##"
  log.trace hubAction
  
  sendHubCommand(hubAction)
  log.trace "Hub Volume Command sent"  
}

private getXmlCommand(action, command, urn) { 
 
 def xml = """ 
<?xml version='1.0' encoding='utf-8'?>\n
 <s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/' s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/'>\n
  <s:Body>\n
   <u:${action} xmlns:u='urn:${urn}'>${command}</u:${action}>\n
  </s:Body>\n
 </s:Envelope>"""
        
  return xml;
}

private getUrn(type) {
    if (type == "command") {
        return "panasonic-com:service:p00NetworkControl:1"
    } else if (type == "render") {
        return "schemas-upnp-org:service:RenderingControl:1"
    }
}

private getCommandUrl(type) {
    if (type == "command") {
        return "/nrc/control_0"
    } else if (type == "render") {
        return "/dmr/control_0"
    }
}

private getTVAddress() {
  return settings.tvIp + ":55000"
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":")
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    obj = new JsonSlurper().parseText(new String(body.decodeBase64()))
  }
  return obj
}