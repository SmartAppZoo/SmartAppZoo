/**
 *  ESP Easy (Connect)
 *
 *  Copyright 2017 mparentes
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
    name: "ESP Easy (Connect)",
    namespace: "mparentes",
    author: "mparentes",
    description: "Service Manager for ESP Easy",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	page(name: "mainPage")
    page(name: "manuallyAdd")
    page(name: "manuallyAddConfirm")
    page(name: "discoveryPage", title: "Device Discovery", content: "discoveryPage", refreshTimeout:5)
    page(name: "addDevices", title: "Add ESP EASY Devices", content: "addDevices")
    page(name: "deviceDiscovery")
	section("Title") {
		// TODO: put inputs here
	}
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Manage your ESP Easy devices", nextPage: null, uninstall: true, install: true) {
        section("Configure"){
           href "deviceDiscovery", title:"Discover Devices", description:""//, params: [pbutton: i]
           href "manuallyAdd", title:"Manually Add Device", description:""//, params: [pbutton: i]
        }
        section("Installed Devices"){
            getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                   if(it.typeName != "ESP Easy Virtual Switch"){
                      href "configurePDevice", title:"$it.label", description:"", params: [did: it.deviceNetworkId]
                   }

            }
        }    
    }
}

def manuallyAdd(){
   dynamicPage(name: "manuallyAdd", title: "Manually add a ESP Easy Controller", nextPage: "manuallyAddConfirm") {
		section {
			paragraph "This process will manually create a SmartLife ESP Easy Controller based on the entered IP address. The SmartApp needs to then communicate with the controller to obtain additional information from it. Make sure the device is on and connected to your wifi network."
            input "ipAddress", "text", title:"IP Address", description: "", required: false 
		}
    }
}

def manuallyAddConfirm(){
   if ( ipAddress =~ /^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/) {
       log.debug "Creating ESP Easy Controller device with dni: ${convertIPtoHex(ipAddress)}:${convertPortToHex("80")}"
       addChildDevice("mparentes", "ESP Easy Controller", "${convertIPtoHex(ipAddress)}:${convertPortToHex("80")}", location.hubs[0].id, [
           "label": "ESP Easy Controller (${ipAddress})",
           "data": [
           "ip": ipAddress,
           "port": "80" 
           ]
       ])
   
       app.updateSetting("ipAddress", "")
            
       dynamicPage(name: "manuallyAddConfirm", title: "Manually add a ESP Easy Controller", nextPage: "mainPage") {
		   section {
			   paragraph "The controller has been added. Press next to return to the main page."
	    	}
       }
    } else {
        dynamicPage(name: "manuallyAddConfirm", title: "Manually add a ESP Easy Controller", nextPage: "mainPage") {
		    section {
			    paragraph "The entered ip address is not valid. Please try again."
		    }
        }
    }
}


//Device Discovery

def discoveryPage(){
   return deviceDiscovery()
}

def deviceDiscovery(params=[:])
{
  def devices = devicesDiscovered()
    
  int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
  state.deviceRefreshCount = deviceRefreshCount + 1
  def refreshInterval = 3
    
  def options = devices ?: []
  def numFound = options.size() ?: 0

  if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
      log.trace "Cleaning old device memory"
      state.devices = [:]
        state.deviceRefreshCount = 0
        app.updateSetting("selectedDevice", "")
    }

  ssdpSubscribe()

  //bridge discovery request every 15 //25 seconds
  if((deviceRefreshCount % 5) == 0) {
    discoverDevices()
  }

  //setup.xml request every 3 seconds except on discoveries
  if(((deviceRefreshCount % 3) == 0) && ((deviceRefreshCount % 5) != 0)) {
    verifyDevices()
  }

  return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:"addDevices", refreshInterval:refreshInterval, uninstall: true) {
    section("Please wait while we discover your ESP Easy devices. Discovery can take five minutes or more, so sit back and relax! Select your device below once discovered.") {
      input "selectedDevices", "enum", required:false, title:"Select ESP Easy Device (${numFound} found)", multiple:true, options:options
    }
        section("Options") {
      href "deviceDiscovery", title:"Reset list of discovered devices", description:"", params: ["reset": "true"]
    }
  }
}

Map devicesDiscovered() {
  def vdevices = getVerifiedDevices()
  def map = [:]
  vdevices.each {
    def value = "${it.value.name}"
    def key = "${it.value.mac}"
    map["${key}"] = value
  }
  map
}

def getVerifiedDevices() {
  getDevices().findAll{ it?.value?.verified == true }
}


private discoverDevices() {
  sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
}



// End Device Discovery


// Devices

def addDevices() {
    def devices = getDevices()
    def sectionText = ""
    
    selectedDevices.each { dni ->bridgeLinking
        def selectedDevice = devices.find { it.value.mac == dni }
        
        log.debug "Selected Devices: ${selectedDevice}"
        
        def d
        if (selectedDevice) {
            d = getChildDevices()?.find {
                it.deviceNetworkId == selectedDevice.value.mac
            }
        }
        
        if (!d && selectedDevice != null) {
            log.debug "Creating ESP Easy Controller device with dni: ${selectedDevice.value.mac}"
            addChildDevice("mparentes", "ESP Easy Controller", selectedDevice.value.mac, selectedDevice?.value.hub, [
                "label": selectedDevice?.value?.name ?: "SmartLife RGBW Controller",
                "data": [
                    "mac": selectedDevice.value.mac,
                    "ip": convertHexToIP(selectedDevice.value.networkAddress),
                    "port": "" + Integer.parseInt(selectedDevice.value.deviceAddress,16)
                ]
            ])
            sectionText = sectionText + "Succesfully added ESP Easy device with ip address ${convertHexToIP(selectedDevice.value.networkAddress)} \r\n"
        }
        
  } 
        return dynamicPage(name:"addDevices", title:"Devices Added", nextPage:"mainPage",  uninstall: true) {
        if(sectionText != ""){
    section("Add ESP Easy Results:") {
      paragraph sectionText
    }
        }else{
        	section("No devices added") {
      			paragraph "All selected devices have previously been added"
    		}
        }
	}
}



//SSDP Routines

void ssdpSubscribe() {
  //subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:basic:1", ssdpHandler)
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:Basic:1", ssdpHandler)
    
}

void ssdpDiscover() {
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    //def parsedEvent = parseEventMessage(description)
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]
    
    //log.debug parsedEvent

    def devices = getDevices()
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    
    if (devices."${ssdpUSN}") {
        def d = devices."${ssdpUSN}"
        def child = getChildDevice(parsedEvent.mac)
        def childIP
        def childPort
        if (child) {
            childIP = child.getDeviceDataByName("ip")
            childPort = child.getDeviceDataByName("port").toString()
            log.debug "Device data: ($childIP:$childPort) - reporting data: (${convertHexToIP(parsedEvent.networkAddress)}:${convertHexToInt(parsedEvent.deviceAddress)})."
            if(childIP != convertHexToIP(parsedEvent.networkAddress) || childPort != convertHexToInt(parsedEvent.deviceAddress).toString()){
            //if(child.getDeviceDataByName("ip") != convertHexToIP(parsedEvent.networkAddress) || child.getDeviceDataByName("port") != convertHexToInt(parsedEvent.deviceAddress)){
               log.debug "Device data (${child.getDeviceDataByName("ip")}) does not match what it is reporting(${convertHexToIP(parsedEvent.networkAddress)}). Attempting to update."
               child.sync(convertHexToIP(parsedEvent.networkAddress), convertHexToInt(parsedEvent.deviceAddress).toString())
            }
        }

        if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            d.networkAddress = parsedEvent.networkAddress
            d.deviceAddress = parsedEvent.deviceAddress
        }
    } else {
        devices << ["${ssdpUSN}": parsedEvent]
    }
}

void verifyDevices() {
log.debug "verifyDevices()"
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        def ip = convertHexToIP(it.value.networkAddress)
        def port = convertHexToInt(it.value.deviceAddress)
        String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
        
    }
}

def getDevices() {
    state.devices = state.devices ?: [:]
    //state.devices = [:] ?: [:]
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
  log.trace "description.xml response (application/xml)"
  def body = hubResponse.xml
  
  //log.debug "${body}"
  
  if (body?.device?.modelName?.text().startsWith("ESP Easy") || body?.device?.modelName?.text().startsWith("AriLux")) {
    def devices = getDevices()
    def device = devices.find {it?.key?.contains(body?.device?.UDN?.text())}
    if (device) {
      device.value << [name:body?.device?.friendlyName?.text() + " (" + convertHexToIP(hubResponse.ip) + ")", serialNumber:body?.device?.serialNumber?.text(), verified: true]
        } else {
      log.error "/description.xml returned a device that didn't exist"
    }
  }
}

//end SSDP Routines
def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}



private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}