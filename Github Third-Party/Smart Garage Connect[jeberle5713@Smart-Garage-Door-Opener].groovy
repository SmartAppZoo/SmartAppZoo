/**
 *  Smart Garage 2 Door Controller (Connect)
 *
 *  Copyright 2018 John Eberle
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
    name: "Smart Garage 2 Door Controller (Connect)",
    namespace: "jeberle5713",
    author: "John Eberle",
    description: "Provides Connection to Smart Garage 2 Door Device",
    category: "My Apps",
    singleInstance: true,
    version: "1.0.0",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "mainPage")
    page(name: "configurePDevice")
    page(name: "deletePDevice")
    page(name: "changeName")
    page(name: "discoveryPage", title: "Device Discovery", content: "discoveryPage", refreshTimeout:5)
    page(name: "addDevices", title: "Add Irrigation Controller", content: "addDevices")
    page(name: "manuallyAdd")
    page(name: "manuallyAddConfirm")
    page(name: "deviceDiscovery")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Manage your Smart garage Door Controllers", nextPage: null, uninstall: true, install: true) {
        section("Configure"){
           href "deviceDiscovery", title:"Discover Devices", description:""	//Link to Device Discovery page
           href "manuallyAdd", title:"Manually Add Device", description:""	//Manually add device page
        }
        section("Installed Devices"){
            getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
                href "configurePDevice", title:"$it.label", description:"", params: [did: it.deviceNetworkId]
            }
        }
        //section("Irrigation Schedules"){
        //}
        //section("Irrigation Schedule Creator"){
        //    app(name: "Irrigation Schedule", appName: "Smart Sprinkler Scheduler", namespace: "anienhuis", title: "Create New Schedule", multiple: true)
        //    
        //    //getChildDevices().sort({ a, b -> a["deviceNetworkId"] <=> b["deviceNetworkId"] }).each {
        //    //    href "configurePDevice", title:"$it.label", description:"", params: [did: it.deviceNetworkId]
        //    //}
        //}
    }
}

def configurePDevice(params){
   def currentDevice
   getChildDevices().each {
       if(it.deviceNetworkId == params.did){
           state.currentDeviceId = it.deviceNetworkId
           state.currentDisplayName = it.displayName
       }      
   }
   if (getChildDevice(state.currentDeviceId) != null) getChildDevice(state.currentDeviceId).configure()
   dynamicPage(name: "configurePDevice", title: "Configure Garage Door Controllers created with this app", nextPage: null) {
		section {
            app.updateSetting("${state.currentDeviceId}_label", getChildDevice(state.currentDeviceId).label)
            input "${state.currentDeviceId}_label", "text", title:"Device Name", description: "", required: false
            href "changeName", title:"Change Device Name", description: "Edit the name above and click here to change it", params: [did: state.currentDeviceId]
            //href "schedulePage", title:"Configure Irrigation Schedule", description: "Click here to configure the irrigation schedule", params: [did: state.currentDeviceId]
        }
        section {
              href "deletePDevice", title:"Delete $state.currentDisplayName", description: "", params: [did: state.currentDeviceId]
        }
   }
}

//Link page when manually adding a device is selected from main.  When user enters inputs and hits next goes to add confirm page
def manuallyAdd(){
   dynamicPage(name: "manuallyAdd", title: "Manually add a Garage Controller", nextPage: "manuallyAddConfirm") {
		section {
			paragraph "This process will manually create a Garage Controller based on the entered IP address. The SmartApp needs to then communicate with the device to obtain additional information from it. Make sure the device is on and connected to your wifi network."
            input "deviceType", "enum", title:"Device Type", description: "", required: false, options: ["Smart Garage 2 Door Controller"]
            input "ipAddress", "text", title:"IP Address", description: "", required: false 
		}
    }
}

//Called as next step in manually adding a device
def manuallyAddConfirm(){
   if ( ipAddress =~ /^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$/) {
       log.debug "Creating Smart Garage Door Controller with dni: ${convertIPtoHex(ipAddress)}:${convertPortToHex("80")}"
       addChildDevice("jeberle5713", deviceType ? deviceType : "Smart Garage Door Opener", "${convertIPtoHex(ipAddress)}:${convertPortToHex("80")}", location.hubs[0].id, [
           "label": (deviceType ? deviceType : "Smart Garage Door Opener") + " (${ipAddress})",
           "data": [
           "ip": ipAddress,
           "port": "80" 
           ]
       ])
   
       app.updateSetting("ipAddress", "")
            
       dynamicPage(name: "manuallyAddConfirm", title: "Manually add a Smart Garage Door Controller", nextPage: "mainPage") {
		   section {
			   paragraph "The device has been added. Press next to return to the main page."
	    	}
       }
    } else {
        dynamicPage(name: "manuallyAddConfirm", title: "Manually add a Smart Garage Door Controller", nextPage: "mainPage") {
		    section {
			    paragraph "The entered ip address is not valid. Please try again."
		    }
        }
    }
}

def deletePDevice(params){
    try {
        unsubscribe()
        deleteChildDevice(state.currentDeviceId)
        dynamicPage(name: "deletePDevice", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "The device has been deleted. Press next to continue"
            } 
        }
    
	} catch (e) {
        dynamicPage(name: "deletePDevice", title: "Deletion Summary", nextPage: "mainPage") {
            section {
                paragraph "Error: ${(e as String).split(":")[1]}."
            } 
        }
    
    }
}

def changeName(params){
    def thisDevice = getChildDevice(state.currentDeviceId)
    thisDevice.label = settings["${state.currentDeviceId}_label"]

    dynamicPage(name: "changeName", title: "Change Name Summary", nextPage: "mainPage") {
	    section {
            paragraph "The device has been renamed. Press \"Next\" to continue"
        }
    }
}

//Per the refreshTimeout setting (5) in the dynamic page setup under preferences,
//This will be called every 5 seconds
def discoveryPage(){
   return deviceDiscovery()
}

//Called repetedly while on the deviceDiscovery page
def deviceDiscovery(params=[:])
{
	def devices = devicesDiscovered()		//Get any SSDP/UPnP devices which are verified devices as map of mac:name
    
    //Track number of times this method called
	int deviceRefreshCount = !state.deviceRefreshCount ? 0 : state.deviceRefreshCount as int
	state.deviceRefreshCount = deviceRefreshCount + 1
    
    
	def refreshInterval = 3
    
	def options = devices ?: []						//If there are verified devices then options = devices, else empty map
	def numFound = options.size() ?: 0				//Track number of devices found.
    
    //if there have been no devices found after (5 x 25 = 125 seconds) or reset 
    //Remove old devices
	if ((numFound == 0 && state.deviceRefreshCount > 25) || params.reset == "true") {
    	log.trace "Cleaning old device memory"
    	state.devices = [:]
        state.deviceRefreshCount = 0
        app.updateSetting("selectedDevice", "")
    }

	ssdpSubscribe()				//Point to ssdpHandler for events responding with our URN  (this simply sets event handler but causes no action)

	// discovery request every 15 //25 seconds
    //Causes the SSDP discovery to broadcast our URN  (responses go to subscibed event)
    // %5 = 0 on multiples of 5 (5, 10, 15, etc)
	if((deviceRefreshCount % 5) == 0) {
		discoverDevices()
	}

	//Try to do verify every 15 (3 x 5 = 15) seconds unless a discovery command just sett
	//setup.xml request every 3 seconds except on discoveries
    //3,6,9,12, ,18,21,27, ,33
	if(((deviceRefreshCount % 3) == 0) && ((deviceRefreshCount % 5) != 0)) {
		verifyDevices()
	}

	//Return a dynamic page with current status etc.  Note:options has currently verified devices...selectedDevices may be one of options
    //selectedDevices doesn't contain all info, just mac:name
	return dynamicPage(name:"deviceDiscovery", title:"Discovery Started!", nextPage:"addDevices", refreshInterval:refreshInterval, uninstall: true) {
		section("Please wait while we discover your Smart Garage Door Controller devices. Discovery can take five minutes or more. Select your device below once discovered.") {
			input "selectedDevices", "enum", required:false, title:"Select Garage Door Controller (${numFound} found)", multiple:true, options:options
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

//List of devices with the proper urn and Modle Name
def getVerifiedDevices() {
	getDevices().findAll{ it?.value?.verified == true }
}

//Note:  The ESP8266 SSDP library uses a default search string (deviceType) of: urn:schemas-upnp-org:device:Basic:1
//We didn't change this and will discriminate devices based on other schema information ie. modelName later afetr
//the initial discovery phase.  We could change these to any urn of our liking: ie. urn:schemas-upnp-org:device:GarageDoorOpener:1
private discoverDevices() {
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
}

def configured() {
	
}

def isConfigured(){
   if(getChildDevices().size() > 0) return true else return false
}

def isVirtualConfigured(did){ 
    def foundDevice = false
    getChildDevices().each {
       if(it.deviceNetworkId != null){
       if(it.deviceNetworkId.startsWith("${did}/")) foundDevice = true
       }
    }
    return foundDevice
}

private virtualCreated(number) {
    if (getChildDevice(getDeviceID(number))) {
        return true
    } else {
        return false
    }
}

private getDeviceID(number) {
    return "${state.currentDeviceId}/${app.id}/${number}"
}



void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.urn:schemas-upnp-org:device:Basic:1", ssdpHandler)
}


//For SSDP: urn - uniform resource name.  This is the name of a particular resource we're looking for.  Could be many like mediaPlayer
//			usn - uniform service name.  This is a unique ID for each instance of a device

void ssdpDiscover() {
    log.debug "Health: Doing lan Discovery at interval: lan discovery urn:schemas-upnp-org:device:Basic:1"
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:Basic:1", physicalgraph.device.Protocol.LAN))
    
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId
    
    //Parse description into Map which includes: devicetype, mac,network address,device address, ssdpPath,ssdpUSN, 
    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]						//Append HubID from hub that received message
    
    
    log.debug "ssdpHandler Called"
    log.debug "ssdpHandler:description $description"
    log.debug "ssdpHandler:parsedEvent $parsedEvent"

    def devices = getDevices()
    log.debug "devices: $devices"
    
    String ssdpUSN = parsedEvent.ssdpUSN.toString()		//Get the usn for the device that caused event (it it unique to a device instance)
    
    //If we already have the USN
    if (devices."${ssdpUSN}") {
        def d = devices."${ssdpUSN}"
        def child = getChildDevice(parsedEvent.mac)
        def childIP
        def childPort
        if (child) {
        	log.debug "found child!  Checking IP etc"
            
            childIP = child.getDeviceDataByName("ip")
            childPort = child.getDeviceDataByName("port").toString()
            log.debug "Device data: ($childIP:$childPort) - reporting data: (${convertHexToIP(parsedEvent.networkAddress)}:${convertHexToInt(parsedEvent.deviceAddress)})."
            if(childIP != convertHexToIP(parsedEvent.networkAddress) || childPort != convertHexToInt(parsedEvent.deviceAddress).toString()){
           		 //ip/port of existing changed
               log.debug "Device data (${child.getDeviceDataByName("ip")}) does not match what it is reporting(${convertHexToIP(parsedEvent.networkAddress)}). Attempting to update."
               child.sync(convertHexToIP(parsedEvent.networkAddress), convertHexToInt(parsedEvent.deviceAddress).toString())
            }
        }

        if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            d.networkAddress = parsedEvent.networkAddress
            d.deviceAddress = parsedEvent.deviceAddress
        }
    } 
    //if this is a new USN, then store the parsed event:  contains (with examples) : devicetype: 04, mac:000E58F0FFFF, networkAddress:0A00010E, deviceAddress:0578, stringCount:04, 
    																//ssdpPath:/xml/device_description.xml, ssdpUSN:uuid:RINCON_000E58F0FFFFFF400::urn:schemas-upnp-org:device:ZonePlayer:1, 
                                                                    //ssdpTerm:urn:schemas-upnp-org:device:ZonePlayer:1 + Also the hub

    else {
        devices << ["${ssdpUSN}": parsedEvent]
    }
}

void verifyDevices() {
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        def ip = convertHexToIP(it.value.networkAddress)		//Convert network Address to IP
        def port = convertHexToInt(it.value.deviceAddress)		//Convert deviceAddress to integer format
        String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
    }
}

def getDevices() {
    state.devices = state.devices ?: [:]
}


//SSDP Schema settings (or similar)
//  SSDP.setSchemaURL("esp8266ic.xml");
//  SSDP.setHTTPPort(80);
//  SSDP.setName("Smart Garage 2 Door Controller");
//  SSDP.setSerialNumber("0000000000001");
//  SSDP.setURL("index.html");
//  SSDP.setModelName("ESP8266_SMART_2_DOOR_GARAGE_CONTROLLER");  //This is checked by Smartthings
//  SSDP.setModelNumber("1");
//  SSDP.setModelURL("https://github.com/jeberle5713/");
//  SSDP.setManufacturer("John Eberle");
//  SSDP.setManufacturerURL("https://TractorEnvy.com");

//Callback during SSDP Verify Phase
void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
	log.trace "esp8266ic.xml response (application/xml)"
	def body = hubResponse.xml
    //log.debug body?.device?.friendlyName?.text()
    
    //Check for match of model name
	if (body?.device?.modelName?.text().startsWith("ESP8266_SMART_2_DOOR_GARAGE_CONTROLLER")) {
    	//Get all devices verified and unverified
		def devices = getDevices()	
        log.debug "SSDP Successful modelName Match!"
        //If a device matches the current respone's UDN, then we have a verify match.  Store
        //The IP and Serial Number
		def device = devices.find {it?.key?.contains(body?.device?.UDN?.text())}
		if (device) {
			device.value << [name:body?.device?.friendlyName?.text() + " (" + convertHexToIP(hubResponse.ip) + ")", serialNumber:body?.device?.serialNumber?.text(), verified: true]
		} else {
			log.error "/esp8266ic.xml returned a device that didn't exist"
		}
	}
}

def addDevices() {
    def devices = getDevices()
    def sectionText = ""
	//selectedDevices is a map of mac:name pairs which comes from (subset) of all verified devices
    
    //Look through our list of all known devices (devices) and match mac id of our selection with that device
    
    //Loop through all selected from discoveryPage screen.  dni will represent that value
    selectedDevices.each { dni ->bridgeLinking
        def selectedDevice = devices.find { it.value.mac == dni }	//look for the current selectedDevice (dni) in our device list
        def d	//it substitutes for the current item in the find process
        
        //A ChildDevice associates a device handler with this appliction.  
        //See if the currently selected device is already set up.   We store deviceNetworkId as 3rd parameter
        //when adding a child.  We use the mac address as a unique id
        if (selectedDevice) {
            d = getChildDevices()?.find {
                it.deviceNetworkId == selectedDevice.value.mac
            }
        }

		//If Not already a Child Device, then make it one.
        //Note, we pass current mac, ip, and port to Device Handler
        if (!d) {
        	log.debug "Selected Device:"
            log.debug selectedDevice
            log.debug "Creating Smart Garage Controller with dni: ${selectedDevice.value.mac}"
            log.debug Integer.parseInt(selectedDevice.value.deviceAddress,16)
            addChildDevice("jeberle5713", (selectedDevice?.value?.name?.startsWith("Smart Garage 2 Door") ? "Smart Garage Door Opener" : "Smart Garage Door Opener"), selectedDevice.value.mac, selectedDevice?.value.hub, [
                "label": selectedDevice?.value?.name ?: "Smart Garage Door ",
                "data": [
                    "mac": selectedDevice.value.mac,
                    "ip": convertHexToIP(selectedDevice.value.networkAddress),
                    "port": "" + Integer.parseInt(selectedDevice.value.deviceAddress,16)
                ]
            ])
            //User Message
            sectionText = sectionText + "Successfully added Smart Garage Door Controller with ip address ${convertHexToIP(selectedDevice.value.networkAddress)} \r\n"
        }
        
	} 
    	//Return this dynamic page
        return dynamicPage(name:"addDevices", title:"Devices Added", nextPage:"mainPage",  uninstall: true) {
            if(sectionText != ""){
                section("Add Smart Garage Door Controller Results:") {
                    paragraph sectionText
                }
            }
            else{
                section("No devices added") {
                    paragraph "All selected devices have previously been added"
                }
            }
		}
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





def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def uninstalled() {
    unsubscribe()
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
    ssdpSubscribe()
    //runEvery5Minutes("ssdpDiscover")
    runEvery1Minute("ssdpDiscover")
}