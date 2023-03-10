/**
 *  LG Smart TV Discovery
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
 * -Modified feb, 2021 by Mariano Colmenarejo.
 * INSTRUCTIONS TO INSTALL:
 * -Create and publish samrtapp and Controller DTH in IDE.
 * -Open The App, click on "Discovery" to found TV.
 * -If Show 'Select LG TV(0 found)' click next until 'Select LG TV(1 found)', then 'Tap to set' to select IP TV"
 * -Select the TV app found, just find 1. click on "done", then key code appears on the TV screen, take note of it.
 * -(Every 15 seconds refresch TV found and clean the previous selection)
 * -Write an App Name to control One TV device. DTH created with same name.
 * -Click on "TV Key" and write the copied code TV pairing key.
 * -NOTE: Device controller works to Activate and Deactivate TV Power Save: screen ON-OFF and Mute OFF-ON. (TV 42" 12watt.consumption, TV 29" 9watt)
 * -Can turn TV Power Off with power button. When these LG TVs 2012 are power OFF, the network does not work and therefore it is impossible to turn them ON.
 * IMPORTANT: Must be configure in your router the Fix IP for your TV or you must write the TV IP in th device configuration even 12Hours normally
 * (jun 2021) MODIFICATION for manual entry IP TV
 * Steps to install manual with IP:
 * Open smartApp “LG TV Discovery 2012 + IP” in your smartthings app
 * Select IP Manual entry = Yes
 * Write the IP of the TV in 192.xxx.x.xx format
 * Click on “Discovery”, it will show another page with Select LG TV (1 found)
 * Click on next
 * Write the name for the smartApp and the TV controller
 * Press on “TV key”: the pairing key will appear on the TV, (if the key does not appear on the TV, it is not compatible)
 * Write the pairing key and press “Done”
 * The smartApp will be added in your smartApps list and a device will be created for that TV at the end of the smartthings main page.
 */
 
definition(
    name: "LG TV Discovery 2012+IP",
    namespace: "smartthings",
    author: "Sam Lalor & modified by MCC",
    description: "Discovers an LG Smart TV (2012+)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    pausable: true
)

preferences {
	page(name: "rootPage")
    page(name: "televisionDiscovery")
    page(name: "televisionAuthenticate")
}

def rootPage() {
   dynamicPage(name: "rootPage", title: "Root Page TV Discovery", install: true, uninstall: true) {
    log.debug "Rootpage"
    section("Select IP Manual entry") {
      input("ManualIP", "enum", title: "Select IP manual entry", description: "", options: ["Yes", "No"], defaultValue: "No", required: true)
      input(name:"selectedTv", type:"text", required:false, title:"Write IP of your TV", multiple:false)
    }
    section("LG TV Setup. Click on Discovery") {
      href(name: "TotelevisionDiscovery", page: "televisionDiscovery", title: "Discovery", description:selectedTv)
    }
	section {
	 label(title: "Label This SmartApp", required: true, defaultValue: "", description: "Highly recommended", submitOnChange: true, state: "complete")
	}   
   section("LG TV key Show") {
     href(name: "TotelevisionAuthenticate", page: "televisionAuthenticate", title: "TV key", description:pairingKey)    
    }
 } 
}

def televisionDiscovery() {
    log.debug "Discovery TV"
    
   int tvRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
    state.bridgeRefreshCount = tvRefreshCount + 1
    def refreshInterval = 15

    def options = televisionsDiscovered() ?: []
    def numFound = options.size() ?: 0
    log.debug "numFound= $numFound; options= $options"
    
    if(!state.subscribe) {
        subscribe(location, null, deviceLocationHandler, [filterEvents:false])    
        state.subscribe = true
    }
      // call findTV every refreshInterval
        if (selectedTv == null) { 
         findTv()
         } else {
          numFound = 1
         }         
        
        log.debug "tvRefreshCount= ${tvRefreshCount}"
     return dynamicPage(name:"televisionDiscovery", title:"LG TV Search Started!", refreshInterval: refreshInterval, uninstall: true){    
        section("If Show 'Select LG TV(0 found)' click next until 'Select LG TV(1 found)', then 'Tap to set' to select IP TV"){
            input (name:"selectedTv", type:"enum", required:false, title:"Select LG TV (${numFound} found)", multiple:false, options:options)
        }
    }
}

def televisionAuthenticate() {
	log.debug "input Key"
    
    tvRequestPairingKey()
     return dynamicPage(name:"televisionAuthenticate", title:"LG TV Search Started!", uninstall: true, install:true){
        section("We sent an pairing request to your TV. Please enter the pairing key and click Done."){
          input (name:"pairingKey", type:"text", required:true, title:"Pairing Key", multiple:false)
        }
    }
}

Map televisionsDiscovered() {
  log.debug "televisionsDiscovered()"
	def vbridges = getLGTvs()
	def map = [:]
	vbridges.each {
    	log.debug "Discovered List: $it"
        def value = "$it"
        def key = it.value
        
        if (key.contains("!")) {
            def settingsInfo = key.split("!")
            def deviceIp = convertHexToIP(settingsInfo[1])
            value = "LG TV (${deviceIp})"
        }
        
        map["${key}"] = value
	}
	map
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
	initialize()
}

def appHandler(evt) {
    log.debug "app event ${evt.name}:${evt.value} received"
}

def updated() {
	log.debug "Updated with settings: ${settings}"

// ****** Turn TV OFF with app toggle previous version
     //def deviceSettings = selectedTv.split("!")
     //def ipAddressHex = deviceSettings[1]
     //def ipAddress = convertHexToIP(ipAddressHex)   
     //def dni = "${ipAddressHex}:${convertPortToHex(8080)}"
     //log.debug "dni= $dni"
     //def d = getChildDevice(dni)
     //log.debug "d= $d"
     //d.home()
     
     initialize() 
}

def initialize() {
    // Remove UPNP Subscription
	unsubscribe()
	state.subscribe = false
    
    addDevice()

    log.debug "Application Initialized"
    log.debug "Selected TV: $selectedTv"
}

def addDevice() { 
  log.debug "ADD ChildDevice: selectedTv= ${selectedTv}"
  
 if (ManualIP == "No") {  
  log.debug "No"
  def deviceSettings = selectedTv.split("!")
  def ipAddressHex = deviceSettings[1]
  def ipAddress = convertHexToIP(ipAddressHex)
    
  def dni = "${ipAddressHex}:${convertPortToHex(8081)}"
  def d = getChildDevice(dni)
  log.debug "dni= $dni"
  
    if(!d) {
  	log.debug "Hub: " + location.hubs[0].id
    log.debug "deviceSettings[2]= " + deviceSettings[2]
    addChildDevice("smartthings", "LG-TV 2012+ Control Power Save", dni, deviceSettings[2],
    	[
        	name: "LG Smart TV 2012", 
            label: "$app.label", 
            completedSetup: true,
            preferences: [
            	pairingKey: "$pairingKey",
            	televisionIp: "$ipAddress"
            ]
        ])
    d = getChildDevice(dni)
    log.debug "created ${d.displayName} with id $dni"
  } 
  else 
  {
    log.debug "Device with id $dni already created"
  }
  } 

if (ManualIP == "Yes") { 
  def ipAddress = selectedTv
  def ipAddressHex = convertIPtoHex(ipAddress)
  log.debug "Yes"
  def dni = "${ipAddressHex}:${convertPortToHex(8080)}"
  def d = getChildDevice(dni)
  log.debug "dni= $dni"

  if(!d) {
  	log.debug "Hub: " + location.hubs[0].id
    addChildDevice("smartthings", "LG-TV 2012+ Control Power Save", dni, location.hubs[0].id,
    	[
        	name: "LG Smart TV 2012", 
            label: "$app.label", 
            completedSetup: true,
            preferences: [
            	pairingKey: "$pairingKey",
            	televisionIp: "$ipAddress"
            ]
        ])
    d = getChildDevice(dni)
    log.debug "created ${d.displayName} with id $dni"
  } 
  else 
  {
    log.debug "Device with id $dni already created"
  }
}
}
// Returns a list of the found LG TVs from UPNP discovery
def getLGTvs() {
	state.televisions = state.televisions ?: [:]
}

// Sends out a UPNP request, looking for the LG TV. Results are sent to [deviceLocationHandler]
private findTv() {
	//sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", physicalgraph.device.Protocol.LAN))
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-udap:service:netrcu:1", physicalgraph.device.Protocol.LAN))
    log.debug "Looking for TV's"
}

// Parses results from [findTv], looking for the specific UPNP result that clearly identifies the TV we can use
def deviceLocationHandler(evt) {
	log.debug "Device Location Event: $evt.description"
	def upnpResult = parseEventMessage(evt.description)
    log.debug "upnp: $upnpResult"
    
    def hub = evt?.hubId
    log.debug "hub: $hub"
    
    if (upnpResult?.ssdpPath?.contains("udap/api/data?target=netrcu.xml")) {
        log.debug "Found TV: ${upnpResult}"
        state.televisions << [device:"${upnpResult.mac}!${upnpResult.ip}!${hub}"]
       log.debug "televisionDiscubierta"
    }
}

// Display pairing key on TV
private tvRequestPairingKey(){
	log.debug "Display pairing key"
    log.debug "ManualIP= $ManualIP"
  if (ManualIP == "No") {  
    log.debug "No"
    log.debug "selectedTv= $selectedTv"
    def deviceSettings = selectedTv.split("!")
    def ipAddressHex = deviceSettings[1]
    def ipAddress = convertHexToIP(ipAddressHex)
    log.debug "ipAddressHex= $ipAddressHex, ipAddress= $ipAddress"
    
    def reqKey = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthKeyReq</type></auth>"
    
    def httpRequest = [
      	method:		"POST",
        path: 		"/roap/api/auth",
        body:		"$reqKey",
        headers:	[
        				HOST:			"$ipAddress:8080",
                        "Content-Type":	"application/atom+xml",
                    ]
	]

	log.debug "HTTP REQUEST"
    log.debug "${httpRequest}"
    
	def hubAction = new physicalgraph.device.HubAction(httpRequest)
	sendHubCommand(hubAction)
  }
   if (ManualIP == "Yes") { 
    def ipAddressHex = convertIPtoHex(selectedTv)
    def ipAddress = convertHexToIP(ipAddressHex)
    log.debug "selectedTv= $selectedTv, ipAddress= $ipAddress, ipAddressHex= $ipAddressHex"
   
    def reqKey = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthKeyReq</type></auth>"
    
    def httpRequest = [
      	method:		"POST",
        path: 		"/roap/api/auth",
        body:		"$reqKey",
        headers:	[
        				HOST:			"$ipAddress:8080",
                        "Content-Type":	"application/atom+xml",
                    ]
	]

	log.debug "HTTP REQUEST"
    log.debug "${httpRequest}"
    
	def hubAction = new physicalgraph.device.HubAction(httpRequest)
	sendHubCommand(hubAction)
  }
}

private def parseEventMessage(String description) {
    log.debug "parsEventMessage"
	def event = [:]
	def parts = description.split(',')
	parts.each { part ->
		part = part.trim()
		if (part.startsWith('devicetype:')) {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ip = valueString
			}
		}
		else if (part.startsWith('ssdpPath:')) {
			def valueString = part.split(":")[1].trim()
			if (valueString) {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:')) {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpUSN = valueString
			}
		}
		else if (part.startsWith('ssdpTerm:')) {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString) {
				event.ssdpTerm = valueString
			}
		}
	}

	event
}

private Integer convertHexToInt(hex)  {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.trace("IP address entered is $ipAddress and the converted hex code is $hex")
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
