/*
* SanderSoft Hot Tub Service Manager for Balboa 20P WiFi Cloud Access Module
* Tested on BullFrog Model A7L
* 2017 (c) SanderSoft
*  Author: Kurt Sanders
*  Email:	Kurt@KurtSanders.com
*  Date:	3/2017
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
import groovy.time.*
import java.text.SimpleDateFormat;

// Start Version Information
def version() {
//    return ["V1.0", "Original Code Base"]
    return ["V2.0", "Hot Tub Service Mgr App Implementation"]
}
// End Version Information
String platform() { return "SmartThings" }
String DTHName() { return "bwa" }
String DTHDNI() { return "MyBwaHotTub" }
String appVersion()	 { return "2.0" }
String appModified() { return "2018-12-11" } 
String appAuthor()	 { return "Kurt Sanders" }
Boolean isST() { return (platform() == "SmartThings") }
String getAppImg(imgName) { return "https://raw.githubusercontent.com/KurtSanders/MySmartThingsPersonal/master/smartapps/kurtsanders/hot-tub-service-manager.src/$imgName" }
Map minVersions() { //These define the minimum versions of code this app will work with.
    return [bwaDevice: 200]
}
// {
//		Not Used in this version
//    	appSetting "IP"
//    	appSetting "devID"
// }

definition(
    name: 		"Hot Tub (Service Manager)",
    namespace: 	"kurtsanders",
    author: 	"Kurt@KurtSanders.com",
    description:"My BWA Hot Tub Service Manager",
    category: 	"My Apps",
    iconUrl: 	getAppImg("MyHotTubSmall.png"),
    iconX2Url: 	getAppImg("MyHotTubLarge.png"),
    iconX3Url: 	getAppImg("MyHotTubLarge.png"),
    singleInstance: true
)


preferences {
    page(name:"mainNetwork")
    page(name:"mainDevice")
    page(name:"mainSchedule")
    page(name:"mainNotifications")
}

def mainNetwork() {
    dynamicPage(name: "mainNetwork",
                title: "Hot Tub Network Location Information",
                nextPage: "mainDevice",
                install: false,
                uninstall: true)
    {
        section ("Hot Tub WiFi Information") {
            input name: "hostName", type: "enum",
                title: "Select the FQDN or public IP Address of your network?",
                options: ["kurtsanders.mynetgear.com", "kurtsanders.myXnetgear.com"],
                capitalization: "none",
                multiple: false,
                required: true
        }
        section("App Version Information") {
            input name: "VersionInfo", type: "text",
                title: "Updates: " + version()[1], 
                description: "Version: " + version()[0], 
                required: false
        }
    }
}

def mainDevice() {
    def nextPageName = null
    def ipAddress = "Unknown DNS Hostname or IP Address"
    def devID = "Return to Main Page"
    def imageName = "failure-icon.png"
    log.debug "state.devID = ${state.devID}"
    log.debug "state.ipAddress = ${state.ipAddress}"

    if((state.hostName==null) || (hostName!=state.hostName)) {
        log.info "Calling subroutine: getHotTubDeviceID(${hostName})"
        getHotTubDeviceID(hostName)
    }
    if((state.devID!=null) && (state.devID!="")) {
        ipAddress    = "${state.ipAddress}"
        devID        = "${state.devID}"
        nextPageName = "mainSchedule"
        imageName    = "success-icon.png"
        state.hostName = hostName
    } else {
        state.hostName = ""
    }
    dynamicPage(name: "mainDevice",
                title: "My Hot Tub Virtual Device",
                nextPage: nextPageName,
                install: false,
                uninstall: false)
    {
        section("Your Hot Tub's Router Public IP") {
            paragraph "${ipAddress}", 
            image: getAppImg(imageName)
            
        }
        section("Your Hot Tub's Cloud Control Device ID") {
            paragraph "${devID}",
            image: getAppImg(imageName)
        }
    }
}

def mainSchedule() {
    dynamicPage(name: "mainSchedule",
                title: "Hot Tub Status Update Frequency",
                nextPage: "mainNotifications",
                uninstall: true)
    {
        section("Hot Tub Polling Interval") {
            input name: "schedulerFreq", type: "enum",
                title: "Run Refresh on a X Min Schedule?",
                options: ['0','1','5','10','15','30','60','180'],
                required: true
            mode(title: "Limit Polling Hot Tub to specific ST mode(s)",
                 image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png")
        }
    }
}
def mainNotifications() {
    dynamicPage(name: "mainNotifications",
                title: "Notifications and Alerts",
                install: true,
                uninstall: true)
    {
        section("Send Hot Tub Alert Notifications?") {
            paragraph "Hot Tub Alerts"
            input("recipients", "contact", title: "Send notifications to") {
                input "phone", "phone", title: "Warn with text message (optional)",
                    description: "Phone Number", required: false
            }
        }
        section("IDE Logging Messages Preferences") {
            input name: "debugVerbose", type: "bool",
                title: "Show Debug Messages in IDE", 
                description: "Verbose Mode", 
                required: false
            input name: "infoVerbose", type: "bool",
                title: "Show Info Messages in IDE", 
                description: "Verbose Mode", 
                required: false
            input name: "errorVerbose", type: "bool",
                title: "Show Error Info Messages in IDE", 
                description: "Verbose Mode", 
                required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    state.deviceId = DTHDNI()
    add_bwa_ChildDevice()
}
def uninstalled() {
    log.debug "uninstalled:------- Started"
    remove_bwa_ChildDevice()
    log.debug "uninstalled:------- Ended"
}
def updated() {
    log.debug "updated:------- Started"
    installed()
    setScheduler(schedulerFreq)
    log.debug "updated:------- Ended"
}

private add_bwa_ChildDevice() {
    // add Hot Tub BWA device
    if (!getChildDevice(state.deviceId)) {
        log.debug "Adding Hot Tub 'bwa' DTH device: ${state.deviceId}"
        try { 
            addChildDevice("kurtsanders", DTHName(), DTHDNI(), null, ["name": "Hot Tub", label: "Hot Tub", completedSetup: true])
        } catch(physicalgraph.app.exception.UnknownDeviceTypeException ex) {
            errorVerbose("The Device Handler '${DTHName()}' was not found in your 'My Device Handlers', Error-> '${ex}'.  Please install this bwa DTH device in the IDE's 'My Device Handlers'")
            return false
        }
        debugVerbose("Added a new device named 'Hot Tub' as ${DTHName()} with DNI: ${DTHDNI()}")
    } else {
        debugVerbose("Device exists named 'Hot Tub' as ${DTHName()} with DNI: ${DTHDNI()}")
    }
}
private remove_bwa_ChildDevice() {
    getAllChildDevices().each { 
        debugVerbose("Deleting BWA Hot Tub device: ${it.deviceNetworkId}")
        if (deleteChildDevice(it.deviceNetworkId)) {
            debugVerbose("Successly Deleted Hot Tub bwa DTH: ${it.deviceNetworkId}")
        } else {
            errorVerbose("Error Deleting BWA device: ${it} -> ${it.deviceNetworkId}")
        }
    }
}

def refresh() {
    infoVerbose("SmartApp Refresh----- Started")
    updateHotTubStatus()
    infoVerbose("SmartApp Refresh----- Ended")
}

def tubAction(feature, command) {
    infoVerbose("SmartApp tubAction----- Started") 
    infoVerbose("tubAction command -> ${feature} ${command}")
    def d = getChildDevice(state.deviceId)
    switch(feature) {
        case 'switch':
        if (d.switchState.value!=command) {
            infoVerbose("Turning Hot Tub '${feature.toUpperCase()}' from '${d.switchState.value.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${d.switchState.value.toUpperCase()}'")     
        }
        break
        case 'heatMode':
        if (state.heatMode!=command) {
            infoVerbose("Turning Heat '${feature.toUpperCase()}' from '${state.heatMode.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.heatMode.toUpperCase()}'")     
        }
        break
        case 'spaPump1':
        if (state.spaPump1!=command) {
            infoVerbose("Turning '${feature.toUpperCase()}' from '${state.spaPump1.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.spaPump1.toUpperCase()}'")     
        }
        break
        case 'spaPump2':
        if (state.spaPump2!=command) {
            infoVerbose("Turning '${feature.toUpperCase()}' from '${state.spaPump2.toUpperCase()}' to '${command.toUpperCase()}'")
            d.sendEvent(name: "${feature}", value: "${command}")
        } else {
            infoVerbose("Hot Tub '${feature.toUpperCase()}' already '${state.spaPump2.toUpperCase()}'")     
        }
        break
        default :
        infoVerbose("default tubAction action for ${feature} ${command}")
    }
    infoVerbose("SmartApp tubAction----- End")
}

def updateHotTubStatus() {
    infoVerbose("handler.updateHotTubStatus----Started")

// Get array values from cloud for Hot Tub Status
	def byte[] B64decoded = null
    if (!B64decoded) {
        for (int i = 1; i < 4; i++) {
            debugVerbose("getOnlineData: ${i} attempt...")
            B64decoded = getOnlineData()
            if (B64decoded!=null) {break}
        }
    }
    if (B64decoded==null) {
    	errorVerbose("getOnlineData: returned Null:  Exiting...")
        updateDeviceStates()
    	return
    }
    // Decode the array status values into operational statuses
    decodeHotTubB64Data(B64decoded)

// Send Update to Hot Tub Virtual Device
    updateDeviceStates()
    infoVerbose("handler.updateHotTubStatus----Ended")
}

def updateDeviceStates() {
    infoVerbose("Start: updateDeviceStates-------------")
    infoVerbose("Sending Device Updates to Virtual Hot Tub Tile")
    Date now = new Date()
    def timeString = new Date().format("M/d 'at' h:mm:ss a",location.timeZone).toLowerCase()
    def d = getChildDevice(state.deviceId)
    infoVerbose("state->${state}")
    d.sendEvent(name: "temperature", value: state.temperature, displayed: true)
    d.sendEvent(name: "contact",   	value: state.contact, displayed: true)
    d.sendEvent(name: "switch",    	value: state.switch, displayed: true)
    d.sendEvent(name: "heatMode", 	value: state.heatMode, displayed: true)
    d.sendEvent(name: "contact", 	value: state.contact, displayed: true)
    d.sendEvent(name: "light", value: state.light, displayed: true)
    d.sendEvent(name: "thermostatOperatingState", value: state.thermostatOperatingState, displayed: true)
    d.sendEvent(name: "thermostatMode", value: state.thermostatMode, displayed: true)
    d.sendEvent(name: "spaPump1", value: state.spaPump1, displayed: true)
    d.sendEvent(name: "spaPump2", value: state.spaPump2, displayed: true)
    d.sendEvent(name: "heatingSetpoint", value: state.heatingSetpoint, displayed: true)
    d.sendEvent(name: "statusText", value: "${state.statusText}", displayed: false)
    d.sendEvent(name: "schedulerFreq", value: "${state.schedulerFreq}", displayed: false)
    d.sendEvent(name: "tubStatus", value: "${state.heatMode} - ${state.thermostatOperatingState.capitalize()} to ${state.heatingSetpoint}ºF on ${timeString}", displayed: false)
    infoVerbose("End: updateDeviceStates-------------")
}

def byte[] getOnlineData() {
    debugVerbose("getOnlineData: Start")
    def httpPostStatus = resp
    def byte[] B64decoded
    Date now = new Date()
    def timeString = new Date().format('EEE MMM d, h:mm:ss a',location.timeZone)
    def Web_idigi_post  = "https://developer.idigi.com/ws/sci"
    def Web_postdata 	= '<sci_request version="1.0"><file_system cache="false" syncTimeout="15">\
    <targets><device id="' + "${state.devID}" + '"/></targets><commands><get_file path="PanelUpdate.txt"/>\
    <get_file path="DeviceConfiguration.txt"/></commands></file_system></sci_request>'
	def respParams = [:]
    def params = [
        'uri'			: Web_idigi_post,
        'headers'		: state.header,
        'body'			: Web_postdata
    ]
    infoVerbose("Start httpPost =============")
    try {
        httpPost(params) {
            resp ->
            infoVerbose("httpPost resp.status: ${resp.status}")
            httpPostStatus = resp
        }
    }
    catch (Exception e)
    {
        debugVerbose("Catch HttpPost Error: ${e}")
        return null
    }
    if (httpPostStatus==null) {
        return null
    }
    def resp = httpPostStatus
    if(resp.status == 200) {
        debugVerbose("HttpPost Request was OK ${resp.status}")
        if(resp.data == "Device Not Connected") {
            errorVerbose("HttpPost Request: ${resp.data}")
            unschedule()
            state.statusText 	= "Hot Tub Fatal Error\n${resp.data}\n${timeString}"
            state.contact 		= "open"
            updateDeviceStates()
            def message = "Hot Tub Error: ${resp.data}! at ${timeString}."
            if (phone) {
                sendSms(phone, message)
            }
            return null
        }
        else {
            // log.info "response data: ${resp.data}"
            state.statusText	= "Refreshed at\n${timeString}"
            state.contact		= "closed"
            def B64encoded = resp.data
            B64decoded = B64encoded.decodeBase64()
            infoVerbose("B64decoded: ${B64decoded}")
            // def byte[] B64decoded = B64encoded.decodeBase64()
            // def hexstring = B64decoded.encodeHex()
            // log.info "hexstring: ${hexstring}"
        }
    }
    else {
        errorVerbose("HttpPost Request got http status ${resp.status}")
        state.statusText	= "Hot Tub Fatal Error\nHttp Status ${resp.status} at ${timeString}."
        return null
    }
    infoVerbose("getOnlineData: End")
    return B64decoded
}

def decodeHotTubB64Data(byte[] d) {
    infoVerbose("Entering decodeHotTubB64Data")
    def byte[] B64decoded = d
    def params = [:]
    def offset = 0

    //	Hot Tub Current Temperature ( <0 is Unavailable )
    offset = 6
    def spaCurTemp = B64decoded[offset]
    if (spaCurTemp < 0) {
        spaCurTemp = 0
    }
    state.temperature	= "${spaCurTemp}"

    //  Hot Tub Mode State
    offset = 9
    def modeStateDecodeArray = ["Ready","Rest","Ready/Rest"]
    state.heatMode = modeStateDecodeArray[B64decoded[offset]]
    //	Hot Tub Pump1 and Pump2 Status
    offset = 15
    def pumpDecodeArray = []
    state.switch = "on"
    switch (B64decoded[offset]) {
        case 0:
        infoVerbose("Pump1: Off, Pump2: Off")
        pumpDecodeArray=["Off","Off"]
        state.switch = "off"
        break
        case 1:
        infoVerbose("Pump1: Low, Pump2: Off")
        pumpDecodeArray=["Low","Off"]
        break
        case 2:
        infoVerbose("Pump1: High, Pump2: Off")
        pumpDecodeArray=["High","Off"]
        break
        case 4:
        infoVerbose("Pump1: Off, Pump2: Low")
        pumpDecodeArray=["Off","Low"]
        break
        case 5:
        infoVerbose("Pump1: Low, Pump2: Low")
        pumpDecodeArray=["Low","Low"]
        break
        case 6:
        infoVerbose("Pump1: High, Pump2: Low")
        pumpDecodeArray=["High","Low"]
        break
        case 8:
        infoVerbose("Pump1: Off, Pump2: High")
        pumpDecodeArray=["Off","High"]
        break
        case 9:
        infoVerbose("Pump1: Low, Pump2: High")
        pumpDecodeArray=["Low","High"]
        break
        case 10:
        infoVerbose("Pump1: High, Pump2: High")
        pumpDecodeArray=["High","High"]
        break
        default :
        infoVerbose("Pump Mode: Unknown")
        pumpDecodeArray=["Off","Off"]
        state.switch = "off"
    }
    state.spaPump1 = pumpDecodeArray[0]
    state.spaPump2 = pumpDecodeArray[1]

    //	Hot Tub Heat Mode
    offset = 17
    if (B64decoded[offset]>0) {
        state.thermostatOperatingState = "heating"
        state.thermostatMode = "heat"
    }
    else {
        state.thermostatOperatingState = "idle"
        state.thermostatMode = "off"
}

//	Hot Tub LED Lights
    offset = 18
    if (B64decoded[offset]>0) {
        infoVerbose("LED On")
        state.light = "on"
    }
    else {
        state.light = "off"
    }

	// Hot Tub Set Temperature
    offset = 24
    // params << ["heatingSetpoint": B64decoded[offset] + '°F\nSet Mode']
    state.heatingSetpoint = B64decoded[offset].toInteger()
}

def setScheduler(schedulerFreq) {
    state.schedulerFreq = "${schedulerFreq}"
    switch(schedulerFreq) {
        case '0':
        unschedule()
        break
        case '1':
        runEvery1Minute(refresh)
        break
        case '5':
        runEvery5Minutes(refresh)
        break
        case '10':
        runEvery10Minutes(refresh)
        break
        case '15':
        runEvery15Minutes(refresh)
        break
        case '30':
        runEvery30Minutes(refresh)
        break
        case '60':
        runEvery1Hour(refresh)
        break
        case '180':
        runEvery3Hours(refresh)
        break
        default :
        infoVerbose("Unknown Schedule Frequency")
        unschedule()
        return
    }
    if(schedulerFreq=='0'){
        infoVerbose("UNScheduled all RunEvery")
    } else {
        infoVerbose("Scheduled RunEvery${schedulerFreq}Minute")
    }

}

def boolean isIP(String str)
{
    try {
        String[] parts = str.split("\\.");
        if (parts.length != 4) return false;
        for (int i = 0; i < 4; ++i)
        {
            int p = Integer.parseInt(parts[i]);
            if (p > 255 || p < 0) return false;
        }
        return true;
    } catch (Exception e){return false}
}

def getHotTubDeviceID(hostName) {
    def boolean isIPbool = isIP(hostName)
    infoVerbose("IP for HostName?: ${isIPbool}")
    if(isIPbool){
        infoVerbose("Valid IP4: ${hostName}")
        state.ipAddress = hostName
    } else {
        def dns2ipAddress = convertHostnameToIPAddress(hostName)
        if (dns2ipAddress != null) {
            infoVerbose("Valid ${hostName} -> IP4: ${dns2ipAddress}")
            state.ipAddress = dns2ipAddress
        } else { 
            state.ipAddress = ""
            state.devID = ""
            return false 
        }
    }
    if (state.ipAddress!=null) {
        getDevId()
        debugVerbose("getDevID() state.devID: '${state.devID}'")
    }
}

private String convertHostnameToIPAddress(hostName) {
    def params = [
        uri: "https://dns.google.com/resolve?name=" + hostName,
        contentType: 'application/json'
    ]
    infoVerbose("Using Google for DNS -> IP: ${params}")
    def retVal = null
    try {
        retVal = httpGet(params) { response ->
            infoVerbose("DNS Lookup Request, data=$response.data, status=$response.status")
            infoVerbose("DNS Lookup Result Status : ${response.data?.Status}")
            if (response.data?.Status == 0) { // Success
                for (answer in response.data?.Answer) { // Loop through results looking for the first IP address returned otherwise it's redirects
                    infoVerbose("Processing response: ${answer}")
                    infoVerbose("HostName ${answer?.name} has IP Address of '${answer?.data}'")
                    return answer?.data
                }
            } else {
                errorVerbose("DNS unable to resolve hostName ${response.data?.Question[0]?.name}, Error: ${response.data?.Comment}")
                state.ipAddress=""
                state.hostName=""
                state.devID=""
            }
        }
    } catch (Exception e) {
        state.ipAddress=""
        state.hostName=""
        state.devID=""
        errorVerbose("Unable to convert hostName to IP Address, Error: $e")
    }
    infoVerbose("Returning IP $retVal for HostName $hostName")
    return retVal
}

def getDevId() {
    infoVerbose("getOnlineStatus(): Begin-----------")
    def devID = ""
    state.header = [
        'UserAgent': 'Spa / 48 CFNetwork / 758.5.3 Darwin / 15.6.0',
        'Cookie': 'JSESSIONID = BC58572FF42D65B183B0318CF3B69470; BIGipServerAWS - DC - CC - Pool - 80 = 3959758764.20480.0000',
        'Authorization': 'Basic QmFsYm9hV2F0ZXJJT1NBcHA6azJuVXBSOHIh'
    ]
    def url   	= "https://my.idigi.com/ws/DeviceCore/.json?condition=dpGlobalIp='" + state.ipAddress + "'"
    def params = [
        'uri'			: url,
        'headers'		: state.header,
        'contentType'	: 'application/json'
    ]
    infoVerbose("Start httpGet =============")
    try {
        httpGet(params)
        { resp ->
            // log.debug "response data: ${resp.data}"
            devID = resp.data.items.devConnectwareId?.get(0)
            infoVerbose("devID = ${devID}")
            if(resp.status == 200) {
                debugVerbose("HttpGet Request was OK")
            }
            else {
                infoVerbose("HttpGet Request got http status ${resp.status}")
                return null
            }
        }
    }
    catch (Exception e)
    {
        errorVerbose(e)
        return null
    }
    state.devID = devID
    infoVerbose("getOnlineStatus(): End----------")

    return
}
def errorVerbose(String message) {if (errorVerbose){log.info "${message}"}}
def debugVerbose(String message) {if (debugVerbose){log.info "${message}"}}
def infoVerbose(String message)  {if (infoVerbose){log.info "${message}"}}
