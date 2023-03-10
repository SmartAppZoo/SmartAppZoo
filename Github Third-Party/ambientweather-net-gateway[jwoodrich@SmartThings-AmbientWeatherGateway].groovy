/**
 *  AmbientWeather.net Gateway
 *
 *  Copyright 2018 Jason Woodrich (@jwoodrich on github)
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
    name: "AmbientWeather.net Gateway",
    namespace: "jwoodrich",
    author: "Jason Woodrich",
    description: "Gateway for AmbientWeather.net based weather stations.",
    category: "SmartThings Labs",
    singleInstance: true,
    iconUrl: "https://i.imgur.com/sDL2yTW.png",
    iconX2Url: "https://i.imgur.com/NjeusHO.png",
    iconX3Url: "https://i.imgur.com/NjeusHO.png") {
}


preferences {
	section("Keys") {
		input(name:"applicationKey",type:"string",title:"Application Key",required:true)
		input(name:"apiKey",type:"string",title:"API Key",required:true)
	}
	section("Timing") {
		input(name:"pollfreq",type:"number",title:"Frequency (mins)",required:true,defaultValue:5)
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

def pollWeather() {
    def params = [
        uri: "https://api.ambientweather.net/v1/devices?apiKey=${apiKey}&applicationKey=${applicationKey}"
    ]
	log.debug "attempting to retrieve weather from ${params.uri} ..."
    
    try {
        httpGet(params) { resp ->
            resp.headers.each {
            	log.debug "response header - ${it.name}: ${it.value}"
        	}
        	log.debug "response contentType: ${resp.contentType}"
        	log.debug "response data: ${resp.data}"
            
            resp.data.each { dev ->
              log.debug "processing device ${dev}"
              def childDevice=getChildDevice(dev.macAddress);
              if (childDevice==null) {
                  log.debug "Failed to find child device ${dev.macAddress}, creating it ..."
                  childDevice=addChildDevice("jwoodrich", "AmbientWeather.net Station", dev.macAddress, null, [name: dev.macAddress, label: dev.info.name+" outdoor", completedSetup: true])
              }
              // temperature and humidity
              childDevice.sendEvent(name: "temperature", value:dev.lastData.tempf, unit: "F")
              sendEventIfFound(childDevice, "humidity", dev.lastData["humidity"])
              sendEventIfFound(childDevice, "temperatureFeelsLike", dev.lastData["feelsLike"], "F")
              sendEventIfFound(childDevice, "dewPoint", dev.lastData["dewPoint"],"F")
              // wind
              if (dev.lastData["winddir"]) {
                  childDevice.sendEvent(name: "windDirection", value:dev.lastData.winddir)
                  childDevice.sendEvent(name: "windDirectionH", value:humanWindDirection(dev.lastData.winddir)) 
              }
              sendEventIfFound(childDevice, "windSpeed", dev.lastData["windspeedmph"], "MPH")
              sendEventIfFound(childDevice, "windGust", dev.lastData["windgustmph"], "MPH")
              sendEventIfFound(childDevice, "maxDailyGust", dev.lastData["maxdailygust"], "MPH")
              sendEventIfFound(childDevice, "lastRain", dev.lastData["lastRain"])
              // rain
              sendEventIfFound(childDevice, "hourlyRain", dev.lastData["hourlyrainin"], "inches")
              sendEventIfFound(childDevice, "dailyRain", dev.lastData["dailyrainin"], "inches")
              sendEventIfFound(childDevice, "weeklyRain", dev.lastData["weeklyrainin"], "inches")
              sendEventIfFound(childDevice, "monthlyRain", dev.lastData["monthlyrainin"], "inches")
              sendEventIfFound(childDevice, "totalRain", dev.lastData["totalrainin"], "inches")
              // barometer
              sendEventIfFound(childDevice, "barometerRelative", dev.lastData["baromrelin"])
              sendEventIfFound(childDevice, "barometerAbsolute", dev.lastData["baromabsin"])
              // light/sun
              sendEventIfFound(childDevice, "ultravioletIndex", dev.lastData["uv"])
              sendEventIfFound(childDevice, "solarRadiation", dev.lastData["solarradiation"])
              
              // Check if indoor device exists
              if (dev.lastData["tempinf"]) {
                  def childMac="${dev.macAddress}in"
                  childDevice=getChildDevice(childMac);
                  if (childDevice==null) {
                      log.debug "Failed to find child device ${dev.macAddress}in, creating it ..."
                      childDevice=addChildDevice("jwoodrich", "AmbientWeather.net Station", childMac, null, [name: childMac, label: dev.info.name+" indoor", completedSetup: true])
                  }
                  childDevice.sendEvent(name: "temperature", value:dev.lastData.tempinf, unit: "F")
                  sendEventIfFound(childDevice, "humidity", dev.lastData["humidityin"])
              }
            }
        }
    } catch (e) {
        log.error "something went wrong: $e", e
    }
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
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

// TODO: implement event handlers