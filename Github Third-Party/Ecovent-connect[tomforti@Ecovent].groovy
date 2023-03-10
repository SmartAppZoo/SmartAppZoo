/**
 *  Ecovent Connect
 *
 *  Copyright 2017, Tom Forti
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
 * 24/04/2017 V1.0 initial release
 */

import java.text.DecimalFormat
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

private apiUrl() 			{ "https://cloud.ecovent.io" }
private getVendorName() 	{ "Ecovent" }
private getVendorIcon()		{ "https://dl.dropboxusercontent.com/s/63qw2yte5gdsjrk/Ecovent_Logo128.png" }
private getClientId() 		{ appSettings.clientId }
private getClientSecret() 	{ appSettings.clientSecret }
private getServerUrl() 		{ if(!appSettings.serverUrl){return getApiServerUrl()} }

 // Automatically generated. Make future change here.
definition(
    name: "Ecovent (Connect)",
    namespace: "Tomforti",
    author: "Tom Forti",
    description: "Ecovent Integration, This SmartApp supports all Ecovent Products",
    category: "SmartThings Labs",
	  iconUrl:   "https://dl.dropboxusercontent.com/s/63qw2yte5gdsjrk/Ecovent_Logo128.png",
    iconX2Url: "https://dl.dropboxusercontent.com/s/63qw2yte5gdsjrk/Ecovent_Logo128.png",
	  oauth: true,
    singleInstance: false
) {
	appSetting "clientId"
	appSetting "clientSecret"
	appSetting "serverUrl"
}

preferences {
	page(name: "startPage", title: "Ecovent (Connect) Integration", content: "startPage", install: false)
	page(name: "Credentials", title: "Fetch Ecovent Credentials", content: "authPage", install: false)
	page(name: "mainPage", title: "Ecovent (Connect) Integration", content: "mainPage")
	page(name: "completePage", title: "${getVendorName()} is now connected to SmartThings!", content: "completePage")
	page(name: "listDevices", title: "Ecovent Devices", content: "listDevices", install: false)
	page(name: "badCredentials", title: "Invalid Credentials", content: "badAuthPage", install: false)
}
mappings {
	path("/receivedHomeId"){action: [POST: "receivedHomeId", GET: "receivedHomeId"]}
}

def startPage() {
    if (state.vendorAccessToken) { return mainPage() }
    else { return authPage() }
}

def authPage() {
	//log.debug "In authPage"
	def description = null
	log.debug "Prompting for Auth Details."
	description = "Tap to enter Credentials."
	return dynamicPage(name: "Credentials", title: "Authorize Connection", nextPage:mainPage, uninstall: false , install:false) {
	   section("Generate Username and Password") {
				input "username", "text", title: "Your Ecovent Username (Email Address)", required: true
				input "password", "password", title: "Your Ecovent Password", required: true
			}
	}
}

def mainPage() {
 	if (!state.accessToken){
  		createAccessToken()
  		getToken()
    }
    if (!state.vendorAccessToken){
        getVendorToken()
      }
  	log.debug "Ecovent System logged in"
	   if (state.vendorAccessToken) {
       return completePage()
       } else {
         return badAuthPage()
       }
}

def completePage(){
	def description = "Tap 'Next' to proceed"
			return dynamicPage(name: "completePage", title: "Credentials Accepted!", uninstall:true, install:false,nextPage: listDevices) {
				section { href url: buildRedirectUrl("receivedHomeId"), style:"embedded", required:false, title:"${getVendorName()} is now connected to SmartThings!", description:description }
			}
}

def badAuthPage(){
	log.debug "In badAuthPage"
    log.error "login result false"
       		return dynamicPage(name: "badCredentials", title: "Bad Tado Credentials", install:false, uninstall:true, nextPage: Credentials) {
				section("") {
					paragraph "Please check your username and password"
           		}
            }
}

def listDevices() {
	//log.debug "In listDevices"
	def options = getDeviceList()
	dynamicPage(name: "listDevices", title: "Choose devices", install:true, uninstall:true) {
		section("Devices") {
			input "devices", "enum", title: "Select Device(s)", required: false, multiple: true, options: options, submitOnChange: true
		}
	}
}

def getToken(){
  if (!state.accessToken) {
		try {
			getAccessToken()
			DEBUG("Creating new Access Token: $state.accessToken")
		} catch (ex) {
			DEBUG("Did you forget to enable OAuth in SmartApp IDE settings")
            DEBUG(ex)
		}
	}
}

def receivedHomeId() {
	//log.debug "In receivedToken"

	def html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${getVendorName()} Connection</title>
        <style type="text/css">
            * { box-sizing: border-box; }
            @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            .container {
                width: 560px;
                padding: 40px;
                /*background: #eee;*/
                text-align: center;
            }
            img {
                vertical-align: middle;
            }
            img:nth-child(2) {
                margin: 0 30px;
            }
            p {
                font-size: 2.2em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                margin-bottom: 0;
            }
        /*
            p:last-child {
                margin-top: 0px;
            }
        */
            span {
                font-family: 'Swiss 721 W01 Light';
            }
        </style>
        </head>
        <body>
            <div class="container">
                <img src=""" + getVendorIcon() + """ alt="Vendor icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                <p>Tap 'Done' to continue to Devices.</p>
			</div>
        </body>
        </html>
        """
	render contentType: 'text/html', data: html
}

def buildRedirectUrl(endPoint) {
	//log.debug "In buildRedirectUrl"
	//log.debug("returning: " + getServerUrl() + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/${endPoint}")
	return getServerUrl() + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/${endPoint}"
}

def getDeviceList() {
  def EcoventDevices = getRoomsCommand()
  return EcoventDevices.sort()
}

def getRoomsCommand(){
	//log.debug "Executing 'Get system structure'"
	api('structure', [])
}

def installed() {
  log.debug "Rooms Installed"
	//log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
  log.debug "Rooms Updated"
	//log.debug "Updated with settings: ${settings}"
  	unsubscribe()
	unschedule()
	initialize()
}

def uninstalled() {
  log.debug "Uninstalling Ecovent (Connect)"
  revokeAccessToken()
  removeChildDevices(getChildDevices())
  log.debug "Ecovent (Connect) Uninstalled"
}

def initialize() {
  log.debug "Rooms Initialized"
	//log.debug "Initialized with settings: ${settings}"
	// Pull the latest device info into state
	getDeviceList();
  def children = getChildDevices()
  if(settings.devices) {
    	settings.devices.each { device ->
        //log.debug("Devices Inspected ${device.inspect()}")
	def item = device.tokenize('|')
        int deviceId = Integer.parseInt(item[0])
        def deviceName = item[1]
        def existingDevices = children.find{ d -> d.deviceNetworkId.contains(deviceId + "|" + deviceName) }
        //log.debug("existingDevices Inspected ${existingDevices.inspect()}")
    	if(!existingDevices)
      {
          log.debug("Some Devices were not found....creating Child Device ${deviceName}")
          try {
                log.debug("Creating Room ${deviceName}")
                createChildDevice("Ecovent Room", deviceId + "|" + deviceName, "${deviceName}", deviceName)
 			        }
              catch (Exception e)
              {
					           log.error "Error creating device: ${e}"
			        }
      }
		}
    }

  statusPoll()
  prefsPoll()
	// Schedule it to run every 5 minutes
	runEvery5Minutes("prefsPoll")
  runEvery1Minute("statusPoll")
}


def getHubID(){
	def hubID
    if (myHub){
        hubID = myHub.id
    } else {
        def hubs = location.hubs.findAll{ it.type == physicalgraph.device.HubType.PHYSICAL }
        if (hubs.size() == 1) hubID = hubs[0].id
    }
    return hubID
}

def statusPoll() {
  log.debug "Executing Status Poll"
  api('status', [])
}

def prefsPoll() {
  log.debug "Executing Prefs Poll"
  api('prefs', [])
}

def devicePoll(childDevice) {
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  def deviceName = item[1]
  log.debug deviceName + " requested a poll"
  api('status', [])
  api('prefs', [])
}

def createChildDevice(deviceFile, dni, name, label) {
	//log.debug "In createChildDevice"
    try{
		def childDevice = addChildDevice("Tomforti", deviceFile, dni, getHubID(), [name: name + " Thermostat", label: label + " Thermostat", completedSetup: true])
	} catch (e) {
		log.error "Error creating device: ${e}"
	}
}

private removeChildDevices(delete) {
	try {
    	delete.each {
        	deleteChildDevice(it.deviceNetworkId)
            log.info "Successfully Removed Child Device: ${it.displayName} (${it.deviceNetworkId})"
    		}
   		}
    catch (e) { log.error "There was an error (${e}) when trying to delete the child device" }
}

def away(childDevice) {
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  log.debug "Room Status: Away"
  childDevice?.sendEvent(name: 'ignore', value: true)
   def myArray=[
    id:deviceId,
    ignore:true,
  ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('ignore', childDevice, jsonStr)
}

def home(childDevice) {
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  log.debug "Room Status: In Use"
  childDevice?.sendEvent(name: 'ignore', value: false)
  def myArray=[
    id:deviceId,
    ignore:false,
  ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('ignore', childDevice, jsonStr)
}

def setpointUp(childDevice){
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  int newSetpoint = childDevice.device.currentValue("thermostatSetpoint") + 1
  log.debug "Setting set point up to: ${newSetpoint}"
  childDevice?.sendEvent(name: 'thermostatSetpoint', value: newSetpoint)
  def newSetpointCel = (newSetpoint - 32) / 1.8
  def myArray=[
    id:deviceId,
    setpoint:newSetpointCel,
    ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('temperature', childDevice, jsonStr)
}

def setpointDown(childDevice){
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  int newSetpoint = childDevice.device.currentValue("thermostatSetpoint") - 1
  log.debug "Setting set point up to: ${newSetpoint}"
  childDevice?.sendEvent(name: 'thermostatSetpoint', value: newSetpoint)
  def newSetpointCel = (newSetpoint - 32) / 1.8
  def myArray=[
    id:deviceId,
    setpoint:newSetpointCel,
  ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('temperature', childDevice, jsonStr)
}

def setCoolingSetpoint(temp, childDevice) {
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  int newSetpoint = temp
  log.debug "Setting set point to: ${newSetpoint}"
  def newSetpointCel = (newSetpoint - 32) / 1.8
  def myArray=[
    id:deviceId,
    setpoint:newSetpointCel,
  ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('temperature', childDevice, jsonStr)
}

def setHeatingSetpoint(temp, childDevice) {
  def item = (childDevice.device.deviceNetworkId).tokenize('|')
  int deviceId = Integer.parseInt(item[0])
  def deviceName = item[1]
  int newSetpoint = temp
  log.debug "Setting set point to: ${newSetpoint}"
  def newSetpointCel = (newSetpoint - 32) / 1.8
  def myArray=[
    id:deviceId,
    setpoint:newSetpointCel,
  ]
  def myData = [ room_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('temperature', childDevice, jsonStr)
}

def heat(childDevice) {
  childDevice?.sendEvent(name: 'thermostatMode', value: heat)
  log.debug "Setting Mode to heat"
  def myArray=[
    id:state.statid,
    mode:"heat",
  ]
  def myData = [ thermostat_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('stat', childDevice, jsonStr)
}

def cool(childDevice) {
  childDevice?.sendEvent(name: 'thermostatMode', value: cool)
  log.debug "Setting Mode to cool"
  def myArray=[
    id:state.statid,
    mode:"cool",
  ]
  def myData = [ thermostat_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('stat', childDevice, jsonStr)
}

def fanOn(childDevice) {
  childDevice?.sendEvent(name: 'thermostatFanMode', value: on)
  log.debug "Setting Fan to on"
  def myArray=[
    id:state.statid,
    fan:"on",
  ]
  def myData = [ thermostat_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('stat', childDevice, jsonStr)
}

def fanAuto(childDevice) {
  childDevice?.sendEvent(name: 'thermostatFanMode', value: auto)
  log.debug "Setting Fan to auto"
  def myArray=[
    id:state.statid,
    fan:"auto",
  ]
  def myData = [ thermostat_prefs:[myArray]]
  def builder = new groovy.json.JsonBuilder(myData)
  def jsonStr = builder.toString()
  api('stat', childDevice, jsonStr)
}

def api(method, childDevice, args = [], success = {}) {
  //log.debug "In API"
  def methods = [
    'status': [uri: apiUrl() + "/remote/v1/status", type: 'getstatus'],
    'prefs': [uri: apiUrl() + "/remote/v1/prefs", type: 'getprefs'],
    'structure': [uri: apiUrl() + "/remote/v1/structure", type: 'getstructure'],
    'ignore': [uri: apiUrl() + "/remote/v1/room_prefs", type: 'putroom'],
    'stat': [uri: apiUrl() + "/remote/v1/thermostat_prefs", type: 'putstat'],
    'temperature': [uri: apiUrl() + "/remote/v1/room_prefs", type: 'putroom']
  ]
  def request = methods.getAt(method)
  doRequest(request.uri, args, request.type, childDevice, success)
}

def doRequest(uri, args, type, childDevice , success) {
  //log.debug "Calling $type : $uri : $args"
  def params = [
    uri: uri,
    requestContentType: "application/json",
    headers: ['Authorization': "token=${state.vendorAccessToken}"],
    body: args
  ]
    if (type == 'putroom') {
      httpPut(params) { resp ->
      log.debug("Sent to Ecovent")
      putroomResponse(resp)
      }
    } else if (type == 'putstat') {
      httpPut(params) { resp ->
        putstatResponse(resp)
        }
       }
      else if (type == 'getstatus') {
      httpGet(params) { resp ->
        statusResponse(resp)
        }
	     }
      else if (type == 'getprefs') {
      httpGet(params) { resp ->
        prefsResponse(resp)
        }
      }
      else if (type == 'getstructure') {
         httpGet(params) { resp ->
           structureResponse(resp)
           }
         }
      else {
        log.debug("error")
      }
}
private putroomResponse(resp) {
  if(resp.status == 200) {
        if(settings.devices) {
          def children = getChildDevices()
          settings.devices.each { device ->
          log.debug("Room ${device.inspect()} updating rooms")
          def item = device.tokenize('|')
          int deviceId = Integer.parseInt(item[0])
          def deviceName = item[1]
          def roomset = Math.round((resp.data.room_prefs[deviceId - 1].setpoint * 1.8 ) + 32) //improves this
          def ignore = resp.data.room_prefs[deviceId - 1].ignore //improve this
          def childDevice = children.find{ d -> d.deviceNetworkId.contains(deviceId + "|" + deviceName) }
          childDevice?.sendEvent(name: 'thermostatSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'heatingSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'coolingSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'ignore', value: ignore)
         }
       }   
    }else{
        log.debug("Get status: "+resp.status)
    }
  }

private putstatResponse(resp) {
  if(resp.status == 200) {
        if(settings.devices) {
          def children = getChildDevices()
          settings.devices.each { device ->
          log.debug("Room ${device.inspect()} updating rooms")
          def item = device.tokenize('|')
          int deviceId = Integer.parseInt(item[0])
          def deviceName = item[1]
          def statMode = resp.data.thermostat_prefs[0].mode
          def fanMode = resp.data.thermostat_prefs[0].fan //improve this
          def childDevice = children.find{ d -> d.deviceNetworkId.contains(deviceId + "|" + deviceName) }
          childDevice?.sendEvent(name: 'thermostatFanMode', value: fanMode)
          childDevice?.sendEvent(name: 'thermostatMode', value: statMode)
         }
       }
    }else{
        log.debug("Get status: "+resp.status)
    }
  }

private statusResponse(resp) {
  if(resp.status == 200) {
        if(settings.devices) {
          def children = getChildDevices()
          settings.devices.each { device ->
          log.debug("Room ${device.inspect()} updating status")
          def item = device.tokenize('|')
          int deviceId = Integer.parseInt(item[0])
          def deviceName = item[1]
          def humid = Math.round(resp.data.room_status[deviceId - 1].humidity)
          def temp = Math.round((resp.data.room_status[deviceId - 1].temp * 1.8 ) + 32)
          def childDevice = children.find{ d -> d.deviceNetworkId.contains(deviceId + "|" + deviceName) }
          childDevice?.sendEvent(name: 'humidity', value: humid)
          childDevice?.sendEvent(name: 'temperature', value: temp)
         }
       } 	
    }else{
        log.debug("Get status: "+resp.status)
    }
}

private prefsResponse(resp) {
  if(resp.status == 200) {
        if(settings.devices) {
          def children = getChildDevices()
          settings.devices.each { device ->
          log.debug("Room ${device.inspect()} updating Prefs")
          def item = device.tokenize('|')
          int deviceId = Integer.parseInt(item[0])
          def deviceName = item[1]
          def statMode = resp.data.thermostat_prefs[0].mode
          def fanMode = resp.data.thermostat_prefs[0].fan //improve this
          def roomset = Math.round((resp.data.room_prefs[deviceId - 1].setpoint * 1.8 ) + 32) //improves this
          def ignore = resp.data.room_prefs[deviceId - 1].ignore //improve this
          def childDevice = children.find{ d -> d.deviceNetworkId.contains(deviceId + "|" + deviceName) }
          childDevice?.sendEvent(name: 'thermostatFanMode', value: fanMode)
          childDevice?.sendEvent(name: 'thermostatMode', value: statMode)
          childDevice?.sendEvent(name: 'thermostatSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'heatingSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'coolingSetpoint', value: roomset)
          childDevice?.sendEvent(name: 'ignore', value: ignore)
         }
       } 	
    }else{
        log.debug("Get status: "+resp.status)
    }
}

private structureResponse(resp) {
  if(resp.status == 200) {
        //log.debug("Get Structure: "+resp.status)
        state.statid = resp.data.home.zones[0].thermostat.id
    	  def restDevices = resp.data.home.zones[0].rooms
        def ecoventDevices = []
        //log.debug("Executing parseZoneResponse.successTrue")
        restDevices.each { ecovent -> ecoventDevices << ["${ecovent.id}|${ecovent.name}":"${ecovent.name}"] }
        //log.debug(ecoventDevices)
        return ecoventDevices
        }
        else if(resp.status == 201)
        {
          log.debug("Something was created/updated")
        }
}

def getVendorToken(method = null, args = [], success = {}) {
  def jsonbody = new groovy.json.JsonOutput().toJson([email: settings.username, password: settings.password])
  def params = [
    uri: apiUrl() + "/api/v1/session",
    requestContentType: "application/json",
    body: jsonbody
  ]
  httpPost(params) {response ->
    if (response.status != 200 ) {
      log.debug "Ecovent logging failed, status = ${response.status}"
    }
    else {
    state.vendorAccessToken = response.data.cloud_authorization
    log.debug "Setting up " + response.data.first_name + " " + response.data.last_name + "'s Ecovent System"
    //log.debug "Ecovent Token is " + response.data.cloud_authorization
    //api(method, args, success)
  }
  }
}
