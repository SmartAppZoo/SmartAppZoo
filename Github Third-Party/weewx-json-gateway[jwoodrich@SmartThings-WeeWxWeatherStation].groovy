/**
 *  Weewx JSON Gateway
 *
 *  Copyright 2017/2018 Jason Woodrich (@jwoodrich on GitHub)
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
 *  Requires an extension for Weewx to generate xml output in the format detailed here: https://groups.google.com/forum/#!topic/weewx-user/wWO4wmrD-yE
 */
definition(
    name: "Weewx JSON Gateway",
    namespace: "jwoodrich",
    author: "Jason Woodrich",
    description: "Gateway for Weewx based weather stations using JSON.",
    category: "SmartThings Labs",
    singleInstance: true,
    iconUrl: "https://i.imgur.com/0B6OM6q.png",
    iconX2Url: "https://i.imgur.com/ajpxsCh.png",
    iconX3Url: "https://i.imgur.com/ajpxsCh.png") {
}


preferences {
	section("Access") {
		input(name:"weewxHost",type:"string",title:"IP Address",required:true,defaultValue:"192.168.0.10")
		input(name:"weewxPort",type:"number",title:"Port",required:true,defaultValue:"80")
		input(name:"weewxSSL",type:"boolean",title:"https",required:true,defaultValue:false)
		input(name:"weewxLAN",type:"boolean",title:"Local LAN",required:true,defaultValue:true)        
	}
	section("Timing") {
		input(name:"pollfreq",type:"number",title:"Frequency (mins)",required:true,defaultValue:5)
	}
    section("Developer") {
    	input "deviceDebug", "boolean", title: "Device Debugging:", defaultValue: false, required: true
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

def sendEventIfFound(device, name, value, units = null) {
    if (device != null) {
        if (units!=null) {
            device.sendEvent(name: name, value: value, units: units)
        } else {
            device.sendEvent(name: name, value: value)
        }
    }
}
def humanWindDirection(value) {
  int[] dirs=[0,45,90,135,180,225,270,315,365]
  String[] names=["N","NE","E","SE","S","SW","W","NW"]  
  for (int i=0; i<8; i++) { // smartthings won't allow array.length
    if (value>=dirs[i] && value<=dirs[i+1]) { return names[i]; }
  }
  return null;
}

def handleUpdate(data) {
    def outside = getChildDevices().find { it?.deviceNetworkId?.equals("outside") }
    def inside = getChildDevices().find { it?.deviceNetworkId?.equals("inside") }
    if (deviceDebug) { 
    	log.debug "received response: ${data}"
    }
    if (outside) {
        log.debug "update device ${outside} with ${data.outTemp} ${data.outHumidity}"
        outside.sendEvent(name: "temperature", value: data.outTemp, unit: "F")
        outside.sendEvent(name: "humidity", value: data.outHumidity)
        outside.sendEvent(name: "wind", value: data.windSpeed)
        if (data["windDir"]) {
        	outside.sendEvent(name: "windDirH", value:humanWindDirection(data.windDir))
        }
        sendEventIfFound(outside, "windAvg", data.windSpeed10)
        sendEventIfFound(outside, "hourlyRain", data.rain)
        sendEventIfFound(outside, "rainToday", data.dayRain)
        sendEventIfFound(outside, "windGust", data.windGust)
    } else {
        log.debug "no outside device"
    }
    if (inside && data.inTemp != null) {
        inside.sendEvent(name: "temperature", value: data.inTemp, unit: "F")        
        sendEventIfFound(inside, "humidity", data.inHumidity)
    } else {
        log.debug "no inside device"
    }
}
// not sure if this works
def pollWeatherFromInternet() {
    def params = [
        uri: (weewxSSL?"https":"http")+"://${weewxHost}:${weewxPort}/current.json"
    ]
	if (deviceDebug) { log.debug "attempting to retrieve weather from ${params.uri}" }
    
    try {
        httpGet(params) { resp ->
            if (deviceDebug) {
                resp.headers.each {
                    log.debug "response header - ${it.name}: ${it.value}"
                }
                log.debug "response contentType: ${resp.contentType}"
                log.debug "response data: ${resp.data}"
            }
            handleUpdate(resp.data)
        }
    } catch (e) {
        log.error "failed to retrieve current weather via internet: $e", e
    }
}
def pollWeatherLANCallback(physicalgraph.device.HubResponse resp) {
    if (deviceDebug) { log.debug "${app.label}: weewx call returned: ${resp.data}" }
    if (resp.data != null) {
        handleUpdate(resp.data)
    } else {
        log.error "${app.label}: failed to find data in resp.data: ${resp.data}"
    }
}
def pollWeather() {
    if (weewxLAN) {
        pollWeatherFromLAN()
    } else {
        pollWeatherFromInternet()
    }
}

def pollWeatherFromLAN() {
    def params = [path: "/current.json", method: "GET", HOST: String.valueOf(weewxHost)+":"+weewxPort, headers: [ HOST: String.valueOf(weewxHost)+":"+weewxPort ] ]
    def opts = [callback: pollWeatherLANCallback]
    if (deviceDebug) { log.debug("HTTP GET: ${params}") }
    sendHubCommand(new physicalgraph.device.HubAction(params,LAN_TYPE_CLIENT,opts))
    if (deviceDebug) { log.debug("HTTP GET DONE") }
}

def initialize() {
    addChildDevice("jwoodrich","Weewx Weather Station","outside",null,[name: "weewx-out", label: "Weewx Outside", completedSetup: true])
    addChildDevice("jwoodrich","Weewx Weather Station","inside",null,[name: "weewx-in", label: "Weewx Inside", completedSetup: true])
    
    schedule("0 */${pollfreq} * * * ?",pollWeather)
    pollWeather()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
