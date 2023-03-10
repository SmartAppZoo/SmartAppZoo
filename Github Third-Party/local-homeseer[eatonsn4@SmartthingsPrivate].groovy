/**
 *  Local Homeseer
 *
 *  Copyright 2017 S E
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
    name: "Local Homeseer",
    namespace: "NetworkGod/Homeseer Integration",
    author: "Sidney Eaton",
    description: "Provides simple local connectivity into a primary Homeseer controller without having to perform Z-Wave or other configuration.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    //appSetting "HomeseerServer"
}


preferences {
    //page(name: "auth", title: "Homeseer API Information", install: false, uninstall: true, nextPage: "")
    page(name: "HomeseerSetup", title: "Homeseer API Information", nextPage:"auth", content:"SetupPage", uninstall: true)
	page(name: "auth", title: "Homeseer", nextPage:"", content:"DevicePickPage", uninstall: true, install:true)
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

// This method is called when the SmartApp is uninstalled and should delete all child devices.
def uninstalled() {
    log.debug "Uninstalled method called!"
    removeChildDevices(getChildDevices())
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
   //subscribe(location, result, myCallbackHandler, [filterEvents:false])
   // Gather existing Homeseer devices
   log.trace "GUI SWITCH SELECTED DNI(s): " + Switches
   Switches.each { dni -> 
   //def devices = Switches.collect { dni ->
		def d = getChildDevice(dni)
        log.trace "Now examining GUI selected DNI: " + dni + " Existing Child (null = create): " + d
		if(!d) {
			d = addChildDevice(app.namespace, getChildName(), dni, null, ["label":"${atomicState.HomeseerDevices[dni].name}" ?: getChildName() ])
			log.debug "created ${atomicState.HomeseerDevices[dni].name} with id $dni"
		} else {
			log.debug "found ${atomicState.HomeseerDevices[dni].name} with id $dni already exists"
		}
		return d
   }
   
   cleanupOldChildDevices()
   getHomeseerDevices()
   //automatically update devices status every 5 mins
   runEvery5Minutes("poll")
}

// This function will remove an array of devices and is a helper function.
private removeChildDevices(delete) {
    delete.each {
        log.trace "Deleting child device ${it.deviceNetworkId}."
        deleteChildDevice(it.deviceNetworkId)
    }
}

// This function will need to be modified later on as to not delete different device types, but for now it
// cleans up unselected or non-existing child devices.
private cleanupOldChildDevices() {
  log.info "Looking for child devices to cleanup...."
  def delete  // Delete any that are no longer in settings
   if(!Switches) {
     log.debug "All input controls are non-existant, all child devices elgible for deletion."
	 delete = getAllChildDevices() //inherits from SmartApp (data-management)
   } else {
     log.debug "Searching for child switches that are not in the child devices."
     delete = getChildDevices().findAll { !Switches.contains(it.deviceNetworkId) }
   }
   // Executing removal
   removeChildDevices(delete)
}

// A bunch of simple helper methods and handlers
def getDisplayName(device) {  return device.location + " " + device.name }
def getApiHostHeader()         { return "${getApiServerIP()}:${HomeseerPort}" }
def getApiServerIP()         { return HomeseerServer }
def getApiServerPort()       { return HomeseerPort }
def getChildName()           { return "Homeseer Switch Child Device" }
def formatChildDeviceNetworkId(Integer HomeseerRefId) {  return "LOCAL:HOMESEER:${getApiHostHeader()}:${HomeseerRefId}" }
def formatChildDeviceNetworkId(String HomeseerRefId) {  return "LOCAL:HOMESEER:${getApiHostHeader()}:${HomeseerRefId}" }
def getChildDeviceNetworkId(String ChildDeviceNetworkId) {
  def myToken = ChildDeviceNetworkId.split(":")
  return myToken[4]
}


private def getHomeseerDevices() {
    def headers = [:] 
    headers.put("HOST", getApiHostHeader())
    def result = new physicalgraph.device.HubAction([
      method: "GET",
      path: "/JSON",
      headers: headers,
      query: [request: "getstatus", ref: "all"]
   ], null, [protocol: "LAN_PROTOCOL_TCP", type: "LAN_TYPE_SERVER", callback: myCallbackHandler])
    //def myMac = "C0A8053D:0051"
    sendHubCommand(new physicalgraph.device.HubAction("""GET /JSON?request=getstatus&ref=all HTTP/1.1\r\nHOST: ${getApiHostHeader()}\r\n\r\n""", physicalgraph.device.Protocol.LAN, null, [callback: calledBackHandler]))
}


//def locationHandler(results) {
//  log.debug "Got hub results" + results.description
//  def msg = parseLanMessage(results.description)
//  log.debug "Hub data received: " + msg.data
//  results.description.split(",").inject([:]) { map, param -> 
//    def nameAndValue = param.split(":") 
//    map += [(nameAndValue[0].trim()):nameAndValue[1].trim()] 
//  } 
//  log.debug map
//}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
  log.debug "Received data back from Homeseer query: " + hubResponse.data
  def devSwitches = [:]
  def HomeseerDevices = [:]
  hubResponse.data.Devices.each { dev ->
    def devattribs = parseHomeseerSingleDeviceHubResponse(dev)
    //def devattribs = [:]
    def dni = formatChildDeviceNetworkId(dev.ref) // Assign a device network id
    //devattribs.put("name",getDisplayName(dev))
    //devattribs.put("value",dev.value)
    //devattribs.put("valueString",dev.status)
    //devattribs.put("type",getDeviceTypeName(dev))
    //devattribs.put("ref",dev.ref)
    HomeseerDevices[dni] = devattribs
  }
  atomicState.HomeseerDevices = HomeseerDevices
  //atomicState.Switches = devSwitches  // DON't THINK I NEED
}

// This method attempts to map Homeseer devices into common Samsung Smartthing device types so that standard APIs and normalization
// can be applied.
def getDeviceTypeName(device) {
  def result
  switch (device.device_type_string) {
    case 'Z-Wave Switch Binary':
      result = "Switch"
      break
    default:
      result = "Other"
      break
  }
  return result
}

def parse(String description) { 
    log.debug "Parsing '${description}'" 
}

// main page to select lights, the action, and turn on/off times
def SetupPage() {
    dynamicPage(name: "HomeseerSetup") {
        section {
            //HomeseerNetInputs()
            paragraph "Please enter the local network connectivity information of your Homeseer API."
            input(name: "HomeseerServer", type: "string", title: "Homeseer API IP", description: "Enter Homeseer IP Address", defaultValue: "192.168.", required: true, displayDuringSetup: true)
            input(name: "HomeseerPort", type: "number", title: "Homeseer API Port", description: "Enter Homeseer Port", defaultValue: "80", required: true, displayDuringSetup: true)
            input(name: "HomeseerUsername", type: "string", title: "Homeseer API Username", description: "Enter Homeseer username if required (not supported yet)", required: false, displayDuringSetup: true)
            input(name: "HomeseerPassword", type: "password", title: "Homeseer API Password", description: "Enter Homeseer password if required (not supported yet)", required: false, displayDuringSetup: true)
        }
    }
}

// Homeseer Network Information Inputs
//def HomeseerNetInputs() {
//
//}


def DevicePickPage() {
	log.debug "DevicePickPage()"
	// get rid of next button until the user is actually auth'd
	log.debug "Trying to retrieve Homeseer devices on inital setup...."
    getHomeseerDevices()
    // Check call back mechanisms and minimize wait time as much as possible.
    if (!atomicState.HomeseerDevices) { pause(1000) }
    if (!atomicState.HomeseerDevices) { pause(2000) }
    if (!atomicState.HomeseerDevices) { pause(2000) }
    if (!atomicState.HomeseerDevices) { pause(5000) }
	return dynamicPage(name: "auth", title: "Select Your Homeseer Devices", uninstall: true) {
		section("") {
			paragraph "Tap below to see the list of Homeseer devices available and select the ones you want to connect to SmartThings."
			input(name: "Switches", title:"Select Homeseer Switches to Import", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:getSwitches()])
            input(name: "AutoPollOnCmd", title:"Auto poll on command execution", type: "bool", required:true, defaultValue: true)
            input(name: "AutoPollMilliseconds", type: "number", title: "Auto polling delay in milliseconds", description: "Milliseconds to wait before auto polling after command", defaultValue: "1500", range: "1000..5000", required: true)
		}
	}
}

// Searches the atomicSate Homeseer devices and finds all of them that are switches then returns a list of names.
def getSwitches() {
  return getDeviceTypeHelper("Switch")
}

def getDeviceTypeHelper(String deviceType) {
  def newCollection = [:]
  atomicState.HomeseerDevices.each { dev ->
    if (dev.getValue().type == deviceType) {
      newCollection[dev.key] = dev.getValue().name
    }
  }
  log.trace "COLLECTION: " + newCollection
  return newCollection
}

// Base poll method to tell children to poll
void poll() {
	pollChild()
}

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
def pollChild() {
    getHomeseerDevices()
    def devices = getChildDevices()
    pause(2000)
    if (!atomicState.HomeseerDevices) { pause(3000) }
    
    devices.each { child ->
      log.debug "Sending data to child ${child.device.deviceNetworkId} : ${child}"
      child.generateEventInformation(atomicState.HomeseerDevices[child.device.deviceNetworkId])
    }

}

def cmdByLabel(String childRefID, String label) {
  log.debug "Executing lable ${label} on Homeseer ref id ${childRefID}"
  sendHubCommand(new physicalgraph.device.HubAction("""GET /JSON?request=controldevicebylabel&ref=${childRefID}&label=${label} HTTP/1.1\r\nHOST: ${getApiHostHeader()}\r\n\r\n""", physicalgraph.device.Protocol.LAN, null, [callback: cmdCallBackHandler]))

  // This will auto poll the device a few seconds after a command has been executed.
  if (AutoPollOnCmd) {
    pause(AutoPollMilliseconds)
    poll()
  }
}

//  This handler is used to parse the response of a command with the assumption that Homeseer will send back instant status, thus
//  this instant status can be relayed back to the device.  However in practice this actually only seems to happen 15% of the time
//  which is disappointing and that is on homeseer instant status products.
void cmdCallBackHandler(physicalgraph.device.HubResponse hubResponse) {
  log.debug "Received command call back from Homeseer: " + hubResponse.data
  def devices = getChildDevices()
  log.debug "DEVICES: " + devices
  hubResponse.data.Devices.each { dev ->
    def devattribs = parseHomeseerSingleDeviceHubResponse(dev)
    //def devattribs = [:]
    def dni = formatChildDeviceNetworkId(dev.ref) // Assign a device network id
    //devattribs.put("name",getDisplayName(dev))
    //devattribs.put("value",dev.value)
    //devattribs.put("valueString",dev.status)
    //devattribs.put("type",getDeviceTypeName(dev))
    //devattribs.put("ref",dev.ref)
    sendDataToChildById(formatChildDeviceNetworkId(dev.ref),devattribs)
    atomicState.HomeseerDevices[dni] = devattribs
  }
}

// This method will take a single array device response from homeseer and put it into a map and return it.  It is a helper object.
private parseHomeseerSingleDeviceHubResponse(dev) {
  def devattribs = [:]
  devattribs.put("name",getDisplayName(dev))
  devattribs.put("value",dev.value)
  devattribs.put("valueString",dev.status)
  devattribs.put("type",getDeviceTypeName(dev))
  devattribs.put("ref",dev.ref)
  return devattribs
}

// This method allows sending specific data or update to just one child.  I'd like to find a more efficient and re-usable way but seems
// documentation is limited.
private sendDataToChildById(String id, java.util.LinkedHashMap data) {
  def devices = getChildDevices()
  devices.each {child ->
    if (child.device.deviceNetworkId == id) {
      log.trace "Found child ${child.device.deviceNetworkId} and sending data."
      child.generateEventInformation(data)
    }
  }
}