/**
 *  DSCAlarmApp
 *
 *  Author: Victor Santana
 *  Date: 2017-12-21
 */

definition(
    name: "DSCAlarmV2 App",
    namespace: "dscalarmappv2",
    author: "Victor Santana",
    description: "SmartApp DSCAlarmV2",
    category: "Safety & Security",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=3x",
    singleInstance: true,
    oauth: true
)

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
      input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.1.10)", required: true
      input "proxyPort", "text", title: "Proxy Port", description: "(ie. 3000)", required: true, defaultValue: "3000"
      //input "authCode", "password", title: "Auth Code", description: "", required: true, defaultValue: "secret-key"
    }
    section("DSCAlarm Arm Disarm") {
      //input name: "pluginType", type: "enum", title: "Plugin Type", required: true, submitOnChange: true, options: ["envisalink", "ad2usb"]
      input "securityCode", "password", title: "Security Code", description: "User code to arm/disarm the security panel", required: true
      input "enableDiscovery", "bool", title: "Discover Zones (WARNING: all existing zones will be removed)", required: false, defaultValue: false
    }

    section("Smart Home Monitor") {
      input "enableSHM", "bool", title: "Integrate with Smart Home Monitor", required: true, defaultValue: true
    }

		section("Enable Debug Log at SmartThing IDE"){
			input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
		}     
  }
}

def installed() {
  writeLog("DSCAlarmSmartAppV2 - DSCInstalled with settings: ${settings}")
	initialize()
  addDSCAlarmDeviceType()
}

def updated() {
  if (settings.enableDiscovery) {
    removeZoneChildDevices()
  }  
  writeLog("DSCAlarmSmartAppV2 - Updated with settings: ${settings}")
	//unsubscribe()
	initialize()
  sendCommand('/subscribe/'+getNotifyAddress())
  sendCommand('/config/'+settings.securityCode)
  if (settings.enableDiscovery) {
    //delay discovery for 5 seconds
    runIn(5, discoverChildDevices)
    runIn(15, alarmUpdate)
    settings.enableDiscovery = false
  }  
  
}

def initialize() {
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
    subscribe(location, "alarmSystemStatus", alarmHandler)
    writeLog("DSCAlarmSmartAppV2 - Initialize")
}

def uninstalled() {
    removeChildDevices()
}

private removeChildDevices() {
    getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
    writeLog("DSCAlarmSmartAppV2 - Removing all child devices")
}

private removeZoneChildDevices() {
    getAllChildDevices().each { 
        if(it.deviceNetworkId != 'dscalrpanel'){
          deleteChildDevice(it.deviceNetworkId)
        }
      }
    writeLog("DSCAlarmSmartAppV2 - Removing all Zone child devices")
}

def alarmHandler(evt) {
  if (!settings.enableSHM) {
    return
  }

  if (state.alarmSystemStatus == evt.value) {
    return
  }

  state.alarmSystemStatus = evt.value
  if (evt.value == "stay") {
    sendCommand('/api/alarmArmStay');
  }
  if (evt.value == "away") {
    sendCommand('/api/alarmArmAway');
  }
  if (evt.value == "off") {
    sendCommand('/api/alarmDisarm');
  }
}

def lanResponseHandler(evt) {
    def map = stringToMap(evt.stringValue)

    //verify that this message is from STNP IP:Port
    //IP and Port are only set on HTTP GET response and we need the MAC
    if (map.ip == convertIPtoHex(settings.proxyAddress) &&
        map.port == convertPortToHex(settings.proxyPort)) {
            if (map.mac) {
            state.proxyMac = map.mac
        }
    }

    //verify that this message is from STNP MAC
    //MAC is set on both HTTP GET response and NOTIFY
    if (map.mac != state.proxyMac) {
        //return
    }

    def headers = getHttpHeaders(map.headers);
    def body = getHttpBody(map.body);

    //verify that this message is for this plugin
    //if (headers.'stnp-plugin' != settings.pluginType) {
        //return
    //}

    if (headers.'device' != 'dscalarm') {
      writeLog("DSCAlarmSmartAppV2 - Received event ${evt.stringValue} but it didn't came from DSCAlarm")
      writeLog("DSCAlarmSmartAppV2 - Received event but it didn't came from DSCAlarm headers:  ${headers}")
      writeLog("DSCAlarmSmartAppV2 - Received event but it didn't came from DSCAlarm body: ${body}")      
      return
    }

    //log.trace "Honeywell Security event: ${evt.stringValue}"
    writeLog("DSCAlarmSmartAppV2 - Received event headers:  ${headers}")
    writeLog("DSCAlarmSmartAppV2 - Received event body: ${body}")
    processEvent(body)
}

// Check if the received event is for descover or update zone/alarm status
private processEvent(evt) {
  if (evt.type == "discover") {
    addChildDevices(evt.zones)
  }
  if (evt.type == "zone") {
    parserDSCCommand(evt.command)
  }
}

def parserDSCCommand(cmd) {
    writeLog("DSCAlarmSmartAppV2 - Received Alarm Command: ${cmd}")
    if(cmd.length() >= 4){
    	if(cmd.substring(0,2) == "ZN"){
        	updateZoneDeviceType(cmd)
          updateAlarmDeviceType(cmd)
        }
        else{
        	updateAlarmDeviceType(cmd)
        }
    }
}

private updateZoneDeviceType(String cmd) {
	def zoneidx = cmd.substring(6,9)
	def zonedeviceNetworkID = "dscalrzone" + zoneidx
  def zonedevice = getChildDevice(zonedeviceNetworkID)
  if (zonedevice) {
    zonedevice.updatedevicezone("${cmd}")
    writeLog("DSCAlarmSmartAppV2 - Updating zone ${zonedeviceNetworkID} using Command: ${cmd}")
  }
}

private updateAlarmDeviceType(String cmd) {
	def alarmdeviceNetworkID = "dscalrpanel"
  def alarmdevice = getChildDevice(alarmdeviceNetworkID)
  if (alarmdevice) {
    alarmdevice.dscalarmparse("${cmd}")
    writeLog("DSCAlarmSmartAppV2 - Updating Alarm Device ${alarmdeviceNetworkID} using Command: ${cmd}")
  }
}

private updateAlarmSystemStatus(partitionstatus) {
  if (!settings.enableSHM || partitionstatus == "arming") {
    return
  }

  def lastAlarmSystemStatus = state.alarmSystemStatus
  if (partitionstatus == "armedstay") {
    state.alarmSystemStatus = "stay"
  }
  if (partitionstatus == "armedaway") {
    state.alarmSystemStatus = "away"
  }
  if (partitionstatus == "ready") {
    state.alarmSystemStatus = "off"
  }

  if (lastAlarmSystemStatus != state.alarmSystemStatus) {
    sendLocationEvent(name: "alarmSystemStatus", value: state.alarmSystemStatus)
  }
}

def alarmUpdate() {
  sendCommand('/api/alarmUpdate')
  writeLog("DSCAlarmSmartAppV2 - Sending Alarm Update request")
}

def discoverChildDevices() {
  sendCommand('/discover')
  writeLog("DSCAlarmSmartAppV2 - Sending discovery request")
}

private addChildDevices(zones) {
  zones.each {
    def deviceId = 'dscalrzone'+it.zone
    if (!getChildDevice(deviceId)) {
      it.type = it.type.capitalize()
      addChildDevice("DSCAlarmV2", "DSCAlarmV2 Zone "+it.type, deviceId, hostHub.id, ["name": it.name, label: it.name, completedSetup: true])
      writeLog("DSCAlarmSmartAppV2 - Added zone device: ${deviceId}")
    }
  }
}

private addDSCAlarmDeviceType() {
  def deviceId = 'dscalrpanel'
  if (!getChildDevice(deviceId)) {
    addChildDevice("DSCAlarmV2", "DSCAlarmV2 Alarm Panel", deviceId, hostHub.id, ["name": "DSCAlarmV2 Alarm Panel", label: "DSCAlarmV2 Alarm Panel", completedSetup: true])
    writeLog("DSCAlarmSmartAppV2 - Added DSCAlarmDeviceType device: ${deviceId}")
  }
}

private getProxyAddress() {
  return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private sendCommand(path) {
  if (settings.proxyAddress.length() == 0 ||
    settings.proxyPort.length() == 0) {
    log.error "SmartThings Node Proxy configuration not set!"
    return
  }

  def host = getProxyAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  headers.put("stnp-auth", settings.authCode)

  def hubAction = new physicalgraph.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
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