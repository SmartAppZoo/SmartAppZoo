/**
 *  Hubitat SmartApp: DSC SmartApp
 *
 *  Original Author: redloro@gmail.com, updated for Hubitat by bubba@bubba.org
 *  Forked by  github.com/Manuriver from the work of github.com/Welasco/NodeAlarm
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 
 *  Version: 1.0.1
 */
import groovy.json.JsonSlurper

definition(
  name: "DSC SmartApp",
  namespace: "manuriver",
  author: "manuriver@river.org",
  description: "DSC manager SmartApp",
  category: "My Apps",
  iconUrl: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
  iconX2Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
  iconX3Url: "https://raw.githubusercontent.com/redloro/smartthings/master/images/honeywell-security.png",
  singleInstance: true
)

preferences {
	page(name: "page1")
}

def page1() {
  state.isDebug = isDebug
  dynamicPage(name: "page1", install: true, uninstall: true) {
	// Only support single hub
    //section("SmartThings Hub") {
    //  input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
    //}
    section("SmartThings Node Proxy") {
      input "proxyAddress", "text", title: "Proxy Address", description: "(ie. 192.168.1.10)", required: true
      input "proxyPort", "text", title: "Proxy Port", description: "(ie. 8080)", required: true, defaultValue: "8080"
     // input "authCode", "password", title: "Auth Code", description: "", required: true, defaultValue: "secret-key"
      input "macAddr", "text", title: "MacAddr of Proxy Server", description: "", required: true, defaultValue: "ABCA1234ABA"
    }
    section("Honeywell Panel") {
      input name: "pluginType", type: "enum", title: "Plugin Type", required: true, submitOnChange: true, options: ["envisalink", "ad2usb"]
      input "securityCode", "password", title: "Security Code", description: "User code to arm/disarm the security panel", required: false
      input "enableDiscovery", "bool", title: "Discover Zones (WARNING: All existing zones will be removed and recreated and mess up any existing rules that rely on zones.)", required: false, defaultValue: false
    }

    if (pluginType == "envisalink") {
      section("Envisalink Vista TPI") {
        input "evlAddress", "text", title: "Host Address", description: "(ie. 192.168.1.11)", required: false
        input "evlPort", "text", title: "Host Port", description: "(ie. 4025)", required: false
        input "evlPassword", "password", title: "Password", description: "", required: false
      }
    }

    section("Hubitat Safety Monitor") {
      input "enableHSM", "bool", title: "Integrate with Hubitat Safety Monitor", required: true, defaultValue: true
    }
    section("") {
       input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
    }
  }
}

def installed() {
	updated()
}

def subscribeToEvents() {
	subscribe(location, null, lanResponseHandler, [filterEvents:false])
  	subscribe(location, "hsmStatus", alarmHandler)
}

def uninstalled() {
 	removeChildDevices()
}

def updated() {
  unsubscribe()
  subscribeToEvents()
  if (settings.enableDiscovery) {
    //remove child devices as we will reload
    removeChildDevices()
  }

  state.alarmSystemStatus

  //subscribe to callback/notifications from STNP
  sendCommand('/subscribe/'+getNotifyAddress())

  //save envisalink settings to STNP config
  if (settings.pluginType == "envisalink" && settings.evlAddress && settings.evlPort && settings.evlPassword && settings.securityCode) {
    sendCommandPlugin('/config/'+settings.evlAddress+":"+settings.evlPort+":"+settings.evlPassword+":"+settings.securityCode)
  }

  //save ad2usb settings to STNP config
  if (settings.pluginType == "ad2usb" && settings.securityCode) {
    sendCommand('/config/'+settings.securityCode)
  }

  if (settings.enableDiscovery) {
    //delay discovery for 5 seconds
	def DNI=macAddr.replace(":","").toUpperCase()
	ifDebug("Creating DSC SmartApp Child")
	addChildDevice("manuriver", "DSC Alarm Panel", DNI)
	state.installed = true
    runIn(5, discoverChildDevices)
    settings.enableDiscovery = false
  }
}

def lanResponseHandler(fromChildDev) {
	try {
    	def parsedEvent = parseLanMessage(fromChildDev).json
		def description = parsedEvent?.description
		def map = parseLanMessage(fromChildDev)
  		//if (map.headers.'stnp-plugin' != settings.pluginType) {
      	//	return
  		//}
  		processEvent(parsedEvent)
	} catch(MissingMethodException) {
		// these are events with description: null and data: null, so we'll just pass.
		pass
	}
}

private sendCommandPlugin(path) {
  //sendCommand("/api/"+settings.pluginType+path)
  sendCommand("/api"+path)
}

private sendCommand(path) {
  ifDebug("send command: ${path}")

  if (settings.proxyAddress.length() == 0 ||
    settings.proxyPort.length() == 0) {
    ifDebug("SmartThings Node Proxy configuration not set!")
    return
  }

  def host = getProxyAddress()
  def headers = [:]
  headers.put("HOST", host)
  headers.put("Content-Type", "application/json")
  //headers.put("stnp-auth", settings.authCode)

  def hubAction = new hubitat.device.HubAction(
      method: "GET",
      path: path,
      headers: headers
  )
  sendHubCommand(hubAction)
}
def stateToDisplay
def zoneToDisplay
def panelalpha
def panelstate
private processEvent(evt) {
  
  if (evt.type == "discover") {
    ifDebug("discovering DSC alarm ${evt}")
    addChildDevices(evt.partitions, evt.zones)
  }
    
  if (evt.command.substring(0, 2) == "ZN") {
     ifDebug("Running Process Event for zones ${evt}")                   
              //parser
            if ( evt.command.substring(3, 9) == "609001" ){
                stateToDisplay = "open"
                zoneToDisplay = "001"
            }
            else if ( evt.command.substring(3, 9) == "610001" ){
                stateToDisplay = "closed"
                zoneToDisplay ="001"
            }
            else if ( evt.command.substring(3, 9) == "609002" ){
                stateToDisplay = "open"
                zoneToDisplay ="002"
            }
            else if ( evt.command.substring(3, 9) == "610002" ){
                stateToDisplay = "closed"
                zoneToDisplay ="002"
            }
            else if ( evt.command.substring(3, 9) == "609003" ){
                stateToDisplay = "open"
                zoneToDisplay ="003"
            }
            else if ( evt.command.substring(3, 9) == "610003" ){
                stateToDisplay = "closed"
                zoneToDisplay ="003"
            }
            else if ( evt.command.substring(3, 9) == "609004" ){
                stateToDisplay = "open"
                zoneToDisplay ="004"
            }
            else if ( evt.command.substring(3, 9) == "610004" ){
                stateToDisplay = "closed"
                zoneToDisplay ="004"
            }
            else if ( evt.command.substring(3, 9) == "609005" ){
                stateToDisplay = "open"
                zoneToDisplay ="005"
            }
            else if ( evt.command.substring(3, 9) == "610005" ){
                stateToDisplay = "closed"
                zoneToDisplay ="005"
            }
            else if ( evt.command.substring(3, 9) == "609006" ){
                stateToDisplay = "open"
                zoneToDisplay ="006"
            }
            else if ( evt.command.substring(3, 9) == "610006" ){
                stateToDisplay = "closed"
                zoneToDisplay ="006"
            }     

    //ifDebug("${stateToDisplay}")
    //ifDebug("${zoneToDisplay}")
    evt.state = stateToDisplay 
    evt.zone = zoneToDisplay
    updateZoneDevices(evt.zone, evt.state)
  }
  
  if (evt.command.substring(0, 2) != "ZN" ) {
    log.debug("Running Process Event for panel ${evt}")
    //ifDebug("${evt.command.substring(0, 2)}")
    //ifDebug("${evt.command.substring(3)}")
   
    
    //evt.state = panelstate 
    //evt.alpha = panelalpha
     
    //updatePartitions(evt.partition, evt.command, evt.alpha)
   evt.state = evt.command

   updatePartitions(evt.partition, evt.state, evt.alpha)
   updateAlarmSystemStatus(evt.state,evt.alpha)
   
  }
}

private addChildDevices(partitions, zones) {
  zones.each {
    def deviceId = 'dsc|zone'+it.zone
	def hub = location.hubs[0]
    ifDebug("Adding Child Device (zone): ${deviceId}")
    if (!getChildDevice(deviceId)) {
      it.type = it.type.capitalize()
      addChildDevice("manuriver", "DSC Zone "+it.type, deviceId, hub.id, ["name": it.name, label: it.name, completedSetup: true])
		ifDebug("Added zone device: ${deviceId} ${hub.id} ${hub.name}")
    }
  }
}

private removeChildDevices() {
  getAllChildDevices().each { deleteChildDevice(it.deviceNetworkId) }
}

def discoverChildDevices() {
  sendCommand('/discover')
}




private updateZoneDevices(zonenum,zonestatus) {
  ifDebug("updateZoneDevices: ${zonenum} is ${zonestatus}")
  def zonedevice = getChildDevice("dsc|zone${zonenum}")
  if (zonedevice) {
    zonedevice.zone("${zonestatus}")
  }
}





private updatePartitions(partitionnum, partitionstatus, panelalpha) {
  // since our main partition is based on MAC address of the SmartThings Node Proxy
  // we already know which partition to update. Again, only supports single partition.
  def DNI=macAddr.replace(":","").toUpperCase()
  ifDebug("updatePartitions: ${DNI} is ${partitionstatus}")
  def partitionDevice = getChildDevice(DNI)
  ifDebug("Partition Device: ${partitionDevice}")
  if (partitionDevice) {
    partitionDevice.partition("${partitionstatus}", "${panelalpha}")
  }
}

def alarmHandler(evt) {
  if (!settings.enableHSM) {
    return
  }

  // deal with when you have just set hsmSetArm=disarm  
  // and received the event hsmStatus=disarmed 
  if ((state.alarmSystemStatus == "disarm") && (evt.value == "disarmed")) {
	return
  }
  if (state.alarmSystemStatus == evt.value) {
    return
  }

  ifDebug("Received HSM event: Value: ${evt.value} state.alarmSystemStatus: ${state.alarmSystemStatus}")
  state.alarmSystemStatus = evt.value
  if (evt.value == "armedHome") {
    sendCommandPlugin('/armStay')
  }
  if (evt.value == "armedNight") {
    sendCommandPlugin('/armStay')
  }
  if (evt.value == "armedAway") {
    sendCommandPlugin('/armAway')
  }
  if (evt.value == "alarmisarmed") {
    sendCommandPlugin('/disarm')
  }
}

private updateAlarmSystemStatus(partitionstatus,alpha) {
  if (!settings.enableHSM || partitionstatus == "arming" || alpha.contains("May Exit")) {
    return
  }

  def lastAlarmSystemStatus = state.alarmSystemStatus
  if (partitionstatus == "armedstay" || partitionstatus == "armedinstant") {
    state.alarmSystemStatus = "armHome"
  }
  if (partitionstatus == "armedaway" || partitionstatus == "armedmax") {
    state.alarmSystemStatus = "armAway"
  }
  if (partitionstatus == "ready") {
    state.alarmSystemStatus = "disarm"
  }

  if (lastAlarmSystemStatus != state.alarmSystemStatus) {
	ifDebug("Sending HSM Event to hsmSetArm: ${state.alarmSystemStatus} (partition status: ${partitionstatus})")
    sendLocationEvent(name: "hsmSetArm", value: state.alarmSystemStatus)
  }
}

private getProxyAddress() {
  return settings.proxyAddress + ":" + settings.proxyPort
}

private getNotifyAddress() {
  // only support single hub.
  def hub = location.hubs[0] 
  ifDebug("Hubitat IP: ${hub.getDataValue("localIP")}")
  ifDebug("Hubitat LAN Port: ${hub.getDataValue("localSrvPortTCP")}")
  return hub.getDataValue("localIP") + ":" + hub.getDataValue("localSrvPortTCP")
}

private ifDebug(msg) {  
    if (msg && state.isDebug)  log.debug 'DSC SmartApp: ' + msg  
}
