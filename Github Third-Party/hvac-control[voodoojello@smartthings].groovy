//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Ambient PWS / HVAC Control for SmartThings
//  Copyright (c)2019-2020 Mark Page (mark@very3.net)
//  Modified: Fri Dec  6 19:38:50 CST 2019
//
//  Dynamically control HVAC settings based on presence and published capabilities of the very3 Ambient PWS Device Handler.
//  For more information see: https://github.com/voodoojello/smartthings/tree/master/devicetypes/apws-device-handler
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License. You may obtain a copy of the License at:
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software distributed under the License is
//  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and limitations under the License.
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

definition (
  name: "HVAC Control",
  version: "19.12.6.19",
  namespace: "very3-hvac-control",
  author: "Mark Page",
  description: "Dynamically control HVAC settings based on presence and published capabilities of the very3 Ambient PWS Device Handler.",
  singleInstance: true,
  category: "SmartThings Internal",
  iconUrl: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-256px.png",
  iconX2Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png",
  iconX3Url: "https://raw.githubusercontent.com/voodoojello/smartthings/master/very3-512px.png"
)

preferences {
  page(name: "mainPage", install: true, uninstall: true)
}

def mainPage() {
  dynamicPage(name: "mainPage", title: "") {

    section ("HVAC Control") {
      paragraph "Dynamically control HVAC settings based on presence and published capabilities of the very3 Ambient PWS Device Handler."
    }

    section ("Enable / Disable HVAC Control") {
      paragraph "Enable / disable control from this app for manual control at thermostats"
      input("hvaccEnable", "bool", title: "Enable HVACC Control")
    }

    section ("Select Thermostats") {
      input ("thermostats", "capability.thermostat", title: "Select thermostats:", multiple: true, required: true)
    }
    
    section ("Select Weather Source") {
      input ("temperatureValue", "capability.temperatureMeasurement", title: "Outside Temperature Source:", required: true)
      input ("humidityValue", "capability.relativeHumidityMeasurement", title: "Outside Humidity Source:", required: true)
    }

    section ("Day/Night Temperature Presets") {
      input ("dayCool", "decimal", title: "Day cooling temperature:", required: true)
      input ("nightCool", "decimal", title: "Night cooling temperature:", required: true)
      input ("dayHeat", "decimal", title: "Day heating temperature:", required: true)
      input ("nightHeat", "decimal", title: "Night heating temperature:", required: true)
    }

    section ("Heating/Cooling Changeover Temperatures") {
      input ("modeThresCool", "decimal", title: "Cooling temperature threshold:", required: true)
      input ("modeThresHeat", "decimal", title: "Heating temperature threshold:", required: true)
    }

    section ("Default Away Mode Temperatures") {
	  input ("awayCoolTemp", "number", title: "Default away cooling temperature:", multiple: false, required: true)
	  input ("awayHeatTemp", "number", title: "Default away heating temperature:", multiple: false, required: true)
    }

    section ("Night Start/Stop") {
      input ("nightStart", "time", title: "Night cycle starts at hour:", required: true)
      input ("nightStop", "time", title: "Night cycle stops at hour:", required: true)
    }
    
    section ("Humidity Scaling") {
      paragraph "Inversely scale thermostat set values by humidity changes for comfort. Higher values make it cooler or warmer. Sane ranges for humidity scaling are .001 to .008."
      input ("humidityAdjust", "decimal", title: "Humidity Scaling:", required: true, defaultValue:0.001)
    }
    
    section ("Change Threshold") {
      paragraph "Minimum set temperature change required to trigger thermostat update. Sane ranges for humidity scaling are 0.0 (always) to 1.0."
      input ("changeThreshold", "decimal", title: "Change Threshold:", required: true, defaultValue:0.5)
    }
    
    section ("Notify on change") {
      paragraph "Notify me when the thermostats are changes"
      input("hvaccNotify", "bool", title: "Notify on change")
    }

  }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def installed() {
  initialize()
}

def updated() {
  logger('info','updated',"Updated with settings: ${settings}")
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  state.logMode      = 0
  state.logHandle    = 'HVACC'
  state.shmStatus    = 'off'

  state.prevMode     = thermostats[0].currentValue("thermostatMode")
  state.prevSetPoint = thermostats[0].currentValue("thermostatSetpoint")
  
  subscribe(location, "alarmSystemStatus" , shmHandler)
  subscribe(temperatureValue, "temperatureMeasurement" , pwsHandler)
  subscribe(temperatureValue, "feelsLikeTemp" , pwsHandler)
  subscribe(humidityValue, "relativeHumidityMeasurement" , pwsHandler)
  
  poll()
}

def shmHandler(evt) {
  logger('info','shmHandler',"Smart Home Monitor ${evt.name} changed to ${evt.value}")
  state.shmStatus = evt.value
  poll()
}

def pwsHandler(evt) {
  logger('info','pwsHandler',"PWS ${evt.name} changed to ${evt.value}")
  poll()
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def poll() {
  logger('info','poll',"Polling...")
  
  def adjTemp     = 72
  def hvacMode    = 'off'
  def currMode    = location.mode
  def isNight     = timeOfDayIsBetween(nightStart, nightStop, new Date(), location.timeZone)
  
  def osHumi      = (humidityValue.latestValue("relativeHumidityMeasurement") as BigDecimal)
  def osTemp      = (temperatureValue.latestValue("temperatureMeasurement") as BigDecimal)
  def feelsLike   = (temperatureValue.latestValue("feelsLikeTemp") as BigDecimal)
  
  // Future use...
  def windSpeed   = (temperatureValue.latestValue("windSpeed") as BigDecimal)
  def dewPoint    = (temperatureValue.latestValue("dewPoint") as BigDecimal)
  def absPressure = (temperatureValue.latestValue("absoluteBarometricPressure") as BigDecimal)
  def relPressure = (temperatureValue.latestValue("relativeBarometricPressure") as BigDecimal)

  if (osTemp >= modeThresCool) {
    hvacMode = 'cool'
  }

  if (osTemp <= modeThresHeat) {
    hvacMode = 'heat'
  }
	
  logger('debug','poll',"osTemp: ${osTemp}, osHumi: ${osHumi}, feelsLike: ${feelsLike}, windSpeed: ${windSpeed}, dewPoint: ${dewPoint}, absPressure: ${absPressure}, relPressure: ${relPressure}, modeThresHeat: ${modeThresHeat}, modeThresCool: ${modeThresCool}, hvacMode: ${hvacMode}")
  
  // We're disabled...
  if (hvaccEnable == false) {
    if (hasChange(hvacMode,adjTemp)) {
      sendNotificationEvent("${state.logHandle}: Override is ${hvaccEnable}, no action taken.")
      logger('info','poll-override',"Override is ${hvaccEnable}, no action taken.")
    }
    
    return
  }
  
  // Heating
	if (hvacMode == "heat") {
    adjTemp = adjustTemp(dayHeat,osTemp,feelsLike,osHumi)
    
    if (isNight) {
      adjTemp = adjustTemp(nightHeat,osTemp,feelsLike,osHumi)
    }
    
    if (currMode.toLowerCase() == 'away' || state.shmStatus.toLowerCase() == 'away') {
      adjTemp = awayHeatTemp
    }
  
    if (hasChange(hvacMode,adjTemp)) {
      thermostats.heat()
      thermostats.fanAuto()
      thermostats.setHeatingSetpoint(adjTemp)
      
      def msgStr = "Set HVAC mode to ${hvacMode} (was ${state.prevMode}), set temperature to ${adjTemp} (was ${state.prevSetPoint}) [osTemp: ${osTemp}, feelsLike: ${feelsLike}, osHumi ${osHumi}]"
      
      if (hvaccNotify) {
        sendNotificationEvent("${state.logHandle}: ${msgStr}")
      }
      logger('info','poll-heat',"${msgStr}")
    }
  }

  // Cooling
  if (hvacMode == "cool") {
    adjTemp = adjustTemp(dayCool,osTemp,feelsLike,osHumi)

    if (isNight) {
      adjTemp = adjustTemp(nightCool,osTemp,feelsLike,osHumi)
    }

    if (currMode.toLowerCase() == 'away' || state.shmStatus.toLowerCase() == 'away') {
      adjTemp = awayCoolTemp
    }
  
    if (hasChange(hvacMode,adjTemp)) {
      thermostats.cool()
      thermostats.fanAuto()
      thermostats.setCoolingSetpoint(adjTemp)
      
      def msgStr = "Set HVAC mode to ${hvacMode} (was ${state.prevMode}), set temperature to ${adjTemp} (was ${state.prevSetPoint}) [osTemp: ${osTemp}, feelsLike: ${feelsLike}, osHumi ${osHumi}]"
      
      if (hvaccNotify) {
        sendNotificationEvent("${state.logHandle}: ${msgStr}")
      }
      logger('info','poll-cool',"${msgStr}")
    }
  }

  // Off
  if (hvacMode == "off") {
    if (hasChange(hvacMode,adjTemp)) {
      thermostats.off()
      
      def msgStr = "Set HVAC mode to ${hvacMode} (was ${state.prevMode}), set temperature to ${adjTemp} (was ${state.prevSetPoint}) [osTemp: ${osTemp}, feelsLike: ${feelsLike}, osHumi ${osHumi}]"
      
      if (hvaccNotify) {
        sendNotificationEvent("${state.logHandle}: ${msgStr}")
      }
      logger('info','poll-off',"${msgStr}")
    }
  }
  
  logger('trace','poll',"hvacMode: ${hvacMode}, adjTemp: ${adjTemp}, currMode: ${currMode}, isNight: ${isNight}, hvaccEnable: ${hvaccEnable}")
  logger('debug','poll',getThermStates())
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private hasChange(hvacMode,adjTemp) {
  def stateReturn     = false
  def coolingSetpoint = 0
  def heatingSetpoint = 0
  def currentHvacMode = "none"
  def currentState    = getThermStates()
  def currentAdjTemp  = round(adjTemp,2)

  currentState.each {
    def thisReturn  = false
    def thisChange  = "none"
    
    coolingSetpoint = round(it.value.coolingSetpoint,2)
    heatingSetpoint = round(it.value.heatingSetpoint,2)
    currentHvacMode = it.value.thermostatMode
    state.prevMode  = currentHvacMode

  	if ("${hvacMode}" != "${currentHvacMode}") {
      thisChange     = 'hvacMode'
      thisReturn     = true
      stateReturn    = true
    }
    
    if ("${hvacMode}" == "off") {
      thisChange = "0 (off)"
    }
    
    if ("${hvacMode}" == "cool") {
  	  if ((currentAdjTemp - coolingSetpoint) > changeThreshold) {
        state.prevSetPoint = coolingSetpoint
        thisChange         = (currentAdjTemp - coolingSetpoint)
        thisReturn         = true
        stateReturn        = true
      }
    	
  	  if ((coolingSetpoint - currentAdjTemp) > changeThreshold) {
        state.prevSetPoint = coolingSetpoint
        thisChange         = (coolingSetpoint - currentAdjTemp)
        thisReturn         = true
        stateReturn        = true
      }
    }

    if ("${hvacMode}" == "heat") {
  	  if ((currentAdjTemp - heatingSetpoint) > changeThreshold) {
        state.prevSetPoint = heatingSetpoint
        thisChange         = (currentAdjTemp - heatingSetpoint)
        thisReturn         = true
        stateReturn        = true
      }
    
  	  if ((heatingSetpoint - currentAdjTemp) > changeThreshold) {
        state.prevSetPoint = heatingSetpoint
        thisChange         = (heatingSetpoint - currentAdjTemp)
        thisReturn         = true
        stateReturn        = true
      }
    }
    
    logger('trace','hasChange',"thermostatName: ${it.value.thermostatName}, hvacMode: ${hvacMode} (${state.prevMode}), adjTemp: ${currentAdjTemp} (${state.prevSetPoint}), thisChange: ${thisChange}, changeThreshold ${changeThreshold}, thisReturn: ${thisReturn}, stateReturn: ${stateReturn}")
  }
   
  return stateReturn
}

private getThermStates() {
  def funcReturn = [:]

  thermostats.each {
    def key   = "${it.label}"
  	def inner = [:]
    
    inner.thermostatName           = it.label
    inner.thermostatOperatingState = it.currentValue("thermostatOperatingState")
    inner.temperature              = it.currentValue("temperature")
    inner.thermostatMode           = it.currentValue("thermostatMode")
    inner.thermostatFanMode        = it.currentValue("thermostatFanMode")
    inner.coolingSetpoint          = it.currentValue("coolingSetpoint")
    inner.heatingSetpoint          = it.currentValue("heatingSetpoint")
    
    logger('trace','getThermStates',inner)
    
    funcReturn.put((key),inner)
  }
  
  return funcReturn
}

private adjustTemp(setTemp,osTemp,feelsLike,humidity) {
  def adjustedReturn
  def difTemp = 0
  
  if (feelsLike >= osTemp) {
    difTemp = osTemp - (osTemp - (humidity * humidityAdjust));
    adjustedReturn = (setTemp - difTemp)
  }
  
  if (feelsLike <= osTemp) {
    difTemp = osTemp - (osTemp + (humidity * humidityAdjust));
    adjustedReturn = (setTemp - difTemp)
  }
  
  logger('trace','adjustTemp',"setTemp: ${setTemp}, difTemp: ${difTemp}, return: ${adjustedReturn}, osTemp: ${osTemp}, feelsLike: ${feelsLike}, humidity: ${humidity}")
  
  return round(adjustedReturn,1)
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private static double round(double value, int precision) {
  if (precision == 0) {
   return (int) Math.round(value)
  }
  
  int scale = (int) Math.pow(10,precision)
  return (double) Math.round(value*scale)/scale
}

private logger(type,loc,msg) {
  // type: error, warn, info, debug, trace
  if ("${type}" == 'info') {
    log."${type}" "${state.logHandle} [${loc}]: ${msg}"
  }
  else if (state.logMode > 0 && "${type}" == 'trace') {
    log."${type}" "${state.logHandle} [${loc}]: ${msg}"
  }
  else if (state.logMode > 1) {
    log."${type}" "${state.logHandle} [${loc}]: ${msg}"
  }
}
