/**
 *  Cool Remote Control Application
 *	ver 1.1
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
 */
 
import groovy.json.JsonSlurper

definition(
  name: "CoolRemote",
  namespace: "coolautomation",
  author: "coolautomation",
  description: "CoolMasterNet Remote Control Application",
  category: "My Apps",
  singleInstance: true,
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "deviceProperty", title: "Device Setup", install: true, uninstall: true){
  		section("SmartThings Hub") {
        	input "hostHub", "hub", title: "Select Hub", multiple: false, required: true
  		}
  		section("CoolMasterNet") {
    		input "CM_ip", "text", title: "IP Address", description: "(ie. 192.168.1.10)", required: true, defaultValue: "192.168.16.20"
    		input "CM_serial", "text", title: "Serial number", description: "(ie. 283B96002128)", required: true, defaultValue: "283B96002128"
  		}
        section("Misc"){
        	input "userTempUnit","enum",title: "User Temperature Unit", description: "Temperature Unit",requied: false, options:["C","F"], submitOnChange: true, defaultValue: "C"
        }
   }
}

def installed() {
	subscribeToEvents()
    pollDevices()
    schedule("0/20 * * * * ?", pollDevices)
	
}

def subscribeToEvents() {
	subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def uninstalled() {
	unschedule()
    removeChildDevices()
}

def updated(){
	getAllChildDevices().each{dev->
    	dev.updateSettings(settings)
    }
}

def getPort(){
	return "10103"
}

def lanResponseHandler(evt) {
	def map = stringToMap(evt.stringValue)
  	if (map.ip == convertIPtoHex(settings.CM_ip) &&
		map.port == convertPortToHex(getPort())) {
    	if (map.mac) {
    		state.proxyMac = map.mac
    	}
    }

	if (map.mac != state.proxyMac) {
    	return
  	}

  	def headers = getHttpHeaders(map.headers);
  	def body = getHttpBody(map.body);
    log.debug "CM headers: ${headers}"
  	log.debug "CM body: ${body}"
  	processEvent(body)
}

def sendCommandToCoolMaster (Map data) {
	def path
    def deviceId = data.deviceId
    def params = data.params
    def command = data.name
    
	log.debug "CM send command: ${command} to indoor ${deviceId} with params ${params}"
    path = ( command.contains("ls") )? "/v2.0/device/${CM_serial}/${command}"
    								 : "/v1.0/device/${CM_serial}/raw?command=${command}"
    if( deviceId ){
    	deviceId = deviceId.replaceAll('\\.','_')
    	path=path.concat("&${deviceId}")
    }
    
    if(params){
    	path=path.concat("&${params}")
    }
    
    if(settings.CM_ip.length() == 0){
    	log.error "CM: SmartThings CoolAutomation configuration not set!"
    	return
  	}
	log.debug "CM: path: ${path}"
  	def host = getProxyAddress()
  	def headers = [:]
  	headers.put("HOST", host)
   
	def hubAction = new physicalgraph.device.HubAction(
    	method: "GET",
    	path: path,
    	headers: headers
	)
	sendHubCommand(hubAction)
}

private processEvent(evt) {
	if( evt && evt.command ){
  		if (evt.command == "ls") {
    		updateChildDevices(evt.data)
  		}
    }
  
}

private updateChildDevices(units){
	log.debug "CM: fillChildDevicesDB"
	units.each{
    		def device = getChildDevice(it.uid);
    		if(!device){
				device = addChildDevice("CoolMasterIndoor", it.uid, hostHub.id, ["name": "CoolMaster Device", label: "${it.uid}", completedSetup: true])
    			log.debug "CM: Added device: ${it.uid} onoff: ${it.onoff} setpoint: ${it.st} temp:${it.rt} mode:${it.mode} fspeed: ${it.fspeed}"
            }
            it.put("userTempUnit",settings.userTempUnit)
            device.parseData(it)
  	}
    getAllChildDevices().each{dev->
    	if( !units.find{it.uid==dev.deviceNetworkId} ){
        	log.debug "CM: Device ${dev.deviceNetworkId} was deleted"
        	deleteChildDevice(dev.deviceNetworkId)
        }
    }
}

private removeChildDevices() {
  	getAllChildDevices().each{dev-> 
  		deleteChildDevice(dev.deviceNetworkId) 
  	}
}

private getHttpHeaders(headers) {
	def obj = [:]
    if(headers)
    {
    	new String(headers.decodeBase64()).split("\r\n").each {param ->
    		def nameAndValue = param.split(":")
    		obj[nameAndValue[0]] = (nameAndValue.length == 1) ? "" : nameAndValue[1].trim()
		}
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

private getProxyAddress() {
	return settings.CM_ip + ":" + getPort()
}

private getNotifyAddress() {
  return settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP
}

private String convertIPtoHex(ipAddress) {
  String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join().toUpperCase()
  return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() ).toUpperCase()
  return hexport
}

def pollDevices(){
	sendCommandToCoolMaster(name:"ls")
}
