/**
 *  Fish Tank Device Manager
 *
 *  Copyright 2018 Stephen Pape
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
  name: "Raspberry Pi Fish Tank Device Manager",
  namespace: "srpape",
  author: "Stephen Pape",
  description: "Manager for Raspberry Pi fish tank devices",
  category: "",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  singleInstance: true)

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

preferences {
  page(name: "page1")
}

def page1() {
  dynamicPage(name: "page1", install: true, uninstall: true) {
    section("SmartThings Hub") {
      input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    }
    section("SmartThings Raspberry") {
      input "piAddress", "text", title: "Pi Address", description: "(ie. 192.168.1.10)", required: true, defaultValue: "192.168.0.50"
      input "piPort", "text", title: "Pi Port", description: "(ie. 5000)", required: true, defaultValue: "5000"
    }
    section("Enable Debug Log at SmartThing IDE"){
	  input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
	}     
  }
}

def installed() {
	writeLog("Installed with settings: ${settings}")
	def childDevice = addChildDevice("Raspberry Pi Fish Tank", getPiAddress(), hostHub.id, ["name": "Fish Tank", label: "Fish Tank", completedSetup: true])
    state.childDeviceID = getPiAddress()
    initialize()
}

def updated() {
  writeLog("Updated with settings: ${settings}")
  query('/subscribe/'+getNotifyAddress())
}

def initialize() {
  writeLog("Initialize")
  subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def uninstalled() {
  writeLog("Removing all child devices")
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def lanResponseHandler(evt) {
  def map = stringToMap(evt.stringValue)

  if (map.headers == null || map.body == null) { 
    return // Not sure why this happens, but it causes annoying log errors
  }

  def headers = getHttpHeaders(map.headers);
  def body = getHttpBody(map.body);

  def deviceId = headers.Device
  if (deviceId == null) {
     return
  }
  def childDevice = getChildDevice(state.childDeviceID)
  childDevice.parse_json(deviceId, body)
}

public query(path) {
  def host = getPiAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")

  def hubAction = new physicalgraph.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
}

public post(path, body) {
  def host = getPiAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/x-www-form-urlencoded")

  def hubAction = new physicalgraph.device.HubAction(
      method: "POST",
      path: path,
      headers: headers,
      body: body,
  )
  sendHubCommand(hubAction)
}

private getPiAddress() {
  return settings.piAddress + ":" + settings.piPort
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private getHttpHeaders(headers) {
  def obj = [:]
  new String(headers.decodeBase64()).split("\r\n").each {param ->
    def nameAndValue = param.split(":|[ ]", 2)
    obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
  }
  return obj
}

private getHttpBody(body) {
  def obj = null;
  if (body) {
    def slurper = new JsonSlurper()
    obj = slurper.parseText(new String(body.decodeBase64()))
  }
  return obj
}

private String convertIPtoHex(ipAddress) {
  return ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
}

private String convertPortToHex(port) {
  return port.toString().format( '%04x', port.toInteger() ).toUpperCase()
}

private writeLog(message)
{
  if(idelog){
    log.debug "${message}"
  }
}
