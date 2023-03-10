/**
 *  Bright Temperature and Light Sensor Manager
 *  Brightswitch Name, Motion, Temp, and Light Sensor, switch name, software version, a modified motion sensor from Exarlabs
 *   Credit to original SmartApp/deviceHandler developed by ExarLabs, atiyka, and Attila Szasz
 *  Copyright 2018 Shawn McClung
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
    name: "Bright Sensor Manager",
    namespace: "spinn360",
    author: "Shawn McClung",
    description: "Brightswitch Sensor Manager",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Brightswitch setup") {
        input "bsIpAdress", "text", title: "Ip Address"
        input "bsAuthToken", "text", title: "Auth Token"
    }
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

def initialize() {
    def ipHex = convertIPtoHex(bsIpAdress)
    def deviceNetworkId = "$ipHex"
    def nId = state.deviceNetworkId
    log.debug "ms: ${nId}"
    if (nId == null) {
    	def tempSensor = getChildDevice(nId)
        state.deviceNetworkId = deviceNetworkId
        addChildDevice("spinn360", "Bright Sensors2", deviceNetworkId, null, [label:"${app.label}", name:"Bright MotionTempLight", completedSetup: true])
    }
    sendLocalServerCommand()
}

// Send local command for hub to make http request on LAN to get BS data
private sendLocalServerCommand() {
	log.debug "Http request sent"
    sendHubCommand(new physicalgraph.device.HubAction("""GET /getdata?authtoken=$bsAuthToken HTTP/1.1\r\nHOST: $bsIpAdress:3003\r\n\r\n""", physicalgraph.device.Protocol.LAN, "", [callback: parse]))
    // Timer - request brightswitch state in every 3 second
    runIn(3 ,"sendLocalServerCommand")
}

// Parse the response from BS and turn on lamp
def parse(physicalgraph.device.HubResponse hubResponse) {
    def response =  hubResponse.json
    def temp = response.get("temperature");
    def light = response.get("lightValue");
		def switchName = response.get("switchName")
		def sf = response.get("softwareVersion")
    log.debug "Temp: ${temp}"
    def mov = response.get("movement");
    log.debug "motion: ${mov}"
		log.debug "light: ${light}"
    def allSensor = getChildDevice(state.deviceNetworkId)
    if(mov){
        sendEvent(allSensor, [value: "active"])
    }else {
    	sendEvent(allSensor, [value: "inactive"])
    }
    //lastMovement recorded set to date format then passed to devichandler
	//	Date date = new Date(response.get("lastMovement"));
	//	DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
	//	def dateFormatted = formatter.format(date);
	//	sendEvent(allSensor, [time: "${dateFormatted}"])

		sendEvent(allSensor, [name: "${switchName}"])
		sendEvent(allSensor, [sf: "${sf}"])
    sendEvent(allSensor, [temp: "${temp}"])
    sendEvent(allSensor, [light: "${light}"])
 }

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}
