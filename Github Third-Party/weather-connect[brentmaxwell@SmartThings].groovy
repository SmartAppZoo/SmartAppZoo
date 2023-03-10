/**
 *  Weather (Connect)
 *
 *  Copyright 2019 Brent Maxwell
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
definition (
  name: "Weather (Connect)",
  namespace: "thebrent",
  author: "Brent Maxwell",
  description: "Weather status",
  category: "SmartThings Labs",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
  {
    appSetting "apiKey"
  }

preferences {
  page name: "mainPage", title: "Click save to create a new virtual device.", install: true, uninstall: true
}

import groovy.json.JsonSlurper

def geocodingUrl() { "https://maps.googleapis.com/maps/api/geocode/json" }
def weatherApiEndpoint() { "https://api.weather.gov" }

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
  httpGet([
    uri: "${geocodingUrl()}?address=${address}&key=${appSettings.apiKey}",
  ]) { resp ->
    def data = resp.data.results[0];
    def location = data.geometry.location;
    def deviceId = "weather-${data.place_id}"
    def childDevice = getChildDevice(deviceId)
    if(!childDevice){
      childDevice = addChildDevice("thebrent", "Weather", deviceId, null, [label: "Weather: ${data.formatted_address}"])
      childDevice.sendEvent(name: "latitude", value: location.lat)
      childDevice.sendEvent(name: "longitude", value: location.lng)
      setChildDeviceSettings(childDevice)
    }
    refresh()
  }
  runEvery30Minutes("poll")
}

def mainPage() {
  dynamicPage(name: "mainPage") {
    section("Location") {
      input "address", title: "Enter", required: true
    }
    section("Preferences") {
      input "temperatureUnits", "enum", title: "Temperature Unit", description: "Please select temperature units", required: true, options: ["F", "C"]
      input "pressureUnits", "enum", title: "Pressure Units", description: "Please select pressure units", required: true, options: ["mbar", "inHg"]
      input "speedUnits", "enum", title: "Wind Units", description: "Please select wind units", required: true, options: ["k/h", "mph", "kts"]
    }
    section("Devices Created") {
      paragraph "${getAllChildDevices().inject("") {result, i -> result + (i.label + "\n")} ?: ""}"
    }
    remove("Remove (Includes Devices)", "This will remove all virtual devices created through this app.")
  }
}

def poll() {
  refresh()
}

def refresh() {
  def children = getChildDevices()
  for(device in children){
    getCurrentObservations(device)
    getAlerts(device)
    device.sendEvent(name: 'lastUpdated', value: new Date().format("yyyy/MM/dd HH:mm:ss"))
  }
}

def setChildDeviceSettings(childDevice) {
  childDevice.sendEvent(name: "temperatureUnits", value: settings.temperatureUnits)
  childDevice.sendEvent(name: "pressureUnits", value: settings.pressureUnits)
  childDevice.sendEvent(name: "speedUnits", value: settings.speedUnits)
  getLocationData(childDevice)
}

def getLocationData(childDevice) {
  def lat = childDevice.currentState("latitude").value
  def lng = childDevice.currentState("longitude").value
  def params = [
    uri: "${weatherApiEndpoint()}/points/${lat},${lng}",
    contentType: "application/ld+json"
  ]
  httpGet(params) { resp ->
    def data = new JsonSlurper().parseText(bytesToString(resp.data))
    childDevice.sendEvent(name: "city", value: data.city.toString())
    childDevice.sendEvent(name: "state", value: data.state.toString())
    childDevice.sendEvent(name: "countyId", value: data.county.toString().replace("https://api.weather.gov/zones/county/",""))
    childDevice.sendEvent(name: "stationId", value: data.cwa.toString())
    childDevice.sendEvent(name: "zoneId", value: data.forecastZone.toString().replace("https://api.weather.gov/zones/forecast/",""))
    childDevice.sendEvent(name: "radarStationId", value: data.radarStation.toString())
    childDevice.sendEvent(name: "gridPointX", value: data.gridX.toString())
    childDevice.sendEvent(name: "gridPointY", value: data.gridY.toString())
  }
}

def getCurrentObservations(childDevice){
  def zoneId = childDevice.currentState("zoneId").value
  def params = [
    uri: "${weatherApiEndpoint()}/zones/forecast/${zoneId}/observations?limit=1",
    contentType: "application/geo+json"
  ]
  httpGet(params) { resp ->
    def data = new JsonSlurper().parseText(bytesToString(resp.data)).features[0].properties
    setHumidity(childDevice, data.relativeHumidity)
    setTemperature(childDevice, data.temperature)
    setPressure(childDevice, data.barometricPressure)
    setWindSpeed(childDevice, data.windSpeed)
    setWindGust(childDevice, data.windGust)
    setWindDirection(childDevice, data.windDirection)
    childDevice.sendEvent(name: "lastUpdated", value: data.timestamp, unit: "")
  }
}

def setHumidity(childDevice, data) {
  childDevice.sendEvent(name: "humidity", value: data.value.toDouble().trunc(2), unit: "%")
}

def setTemperature(childDevice, data) {
  def value = data.value
  def unit = childDevice.currentState("temperatureUnits").value
  switch(unit) {
    case "C":
      if(data.unitCode == "unit:degC") {
        value = value
      }
      break;
    case "F":
      if(data.unitCode == "unit:degC") {
        value = (value * (9/5)) + 32
      }
      break;
  }
  childDevice.sendEvent(name: "temperature", value: value.toDouble().trunc(2), unit: unit)
}

def setPressure(childDevice, data) {
  def value = data.value
  def unit = childDevice.currentState("pressureUnits").value
  switch(unit) {
    case "mbar":
      unit = "mbar"
      if(data.unitCode == "unit:Pa") {
        value = value / 100
      }
      break;
    case "inHg":
      unit = "inHg"
      if(data.unitCode == "unit:Pa") {
        value = value / 3386
      }
      break;
  }
  childDevice.sendEvent(name: "pressure", value: value.toDouble().trunc(2), unit: unit)
}

def setWindSpeed(childDevice, data) {
  def value = data.value
  def unit = childDevice.currentState("speedUnits").value
  switch(unit) {
    case "mph":
      unit = "mph"
      if(data.unitCode == "unit:m_s-1") {
        value = value * 2.237
      }
      break;
    case "k/h":
      if(data.unitCode == "unit:m_s-1") {
        value = value * 3.6
      }
      break;
    case "kts":
      if(data.unitCode == "unit:m_s-1") {
        value = value * 1.944
      }
      break;
   }
   childDevice.sendEvent(name: "windSpeed", value: value.toDouble().trunc(2), unit: unit)
}

def setWindGust(childDevice, data) {
if(data.value != null) {
  def value = data.value
  def unit = childDevice.currentState("speedUnits").value
  switch(settings.speedUnits) {
    case "mph":
      unit = "mph"
      if(data.unitCode == "unit:m_s-1") {
        value = value * 2.237
      }
      break;
    case "k/h":
      if(data.unitCode == "unit:m_s-1") {
        value = value * 3.6
      }
      break;
    case "kts":
      if(data.unitCode == "unit:m_s-1") {
        value = value * 1.944
      }
      break;
    }
    childDevice.sendEvent(name: "windGust", value: value.toDouble().trunc(2), unit: unit)
  }
}

def setWindDirection(childDevice, data) {
  def angle = data.value
  def name = ""
  if(angle < 23) {
    name = angle + "° North"
  } else if (angle < 68) {
    name = angle + "° NorthEast"
  } else if (angle < 113) {
    name = angle + "° East"
  } else if (angle < 158) {
    name = angle + "° SouthEast"
  } else if (angle < 203) {
    name = angle + "° South"
  } else if (angle < 248) {
    name = angle + "° SouthWest"
  } else if (angle < 293) {
    name = angle + "° West"
  } else if (angle < 338) {
    name = angle + "° NorthWest"
  } else if (angle < 361) {
    name = angle + "° North"
  }
  childDevice.sendEvent(name: "windDirection", value: value, unit: "")
}

def getAlerts(childDevice) {
  def zoneId = childDevice.currentState("zoneId").value
  def params = [
    uri: "${weatherApiEndpoint()}/alerts/active/zone/${zoneId}",
    contentType: "application/geo+json"
  ]
  httpGet(params) { resp ->
    def data = new JsonSlurper().parseText(bytesToString(resp.data)).features;
    def alerts = ""
    for(i in data) {
      alerts += i.properties.event + "\n"
    }
    childDevice.sendEvent(name: "alerts", value: alerts)
  }
}

def bytesToString(data){
  int n = data.available();
  byte[] bytes = new byte[n];
  data.read(bytes, 0, n);
  String s = new String(bytes)
  return s
}
