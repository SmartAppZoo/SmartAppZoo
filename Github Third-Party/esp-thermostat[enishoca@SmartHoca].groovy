/**
 *  ESP Thermostat
 *
 * 	Author: Enis Hoca  - enishoca@outlook.com
 *  Copyright 2018 Enis Hoca
 *  
 *  Derived from:
 *     Virtual thermostat by @eliotstocker and dht22 redux by geko@statusbits.com
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

definition(
    name: "ESP Thermostat",
    namespace: "enishoca/SmartHoca",
    author: "Enis Hoca",
    description: "Control a heater in conjunction with a ESP8266 Thermostat.",
    category: "Green Living",
    iconUrl: "https://raw.githubusercontent.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH/master/logo-small.png",
    iconX2Url: "https://raw.githubusercontent.com/eliotstocker/SmartThings-VirtualThermostat-WithDTH/master/logo.png"
)

preferences {
    section("Device IP Addreess... ") {
        input("confIpAddr", "string", title: "Thermostat IP Address", required: true, displayDuringSetup: true)
        input("confTcpPort", "number", title: "Thermostat TCP Port", required: true, displayDuringSetup: true)
    }
    section("Select the heater outlet(s)... ") {
        input "outlets", "capability.switch", title: "Outlets", multiple: true
    }
    section("Only heat when contact isnt open (optional, leave blank to not require contact sensor)...") {
        input "motion", "capability.contactSensor", title: "Contact", required: false
    }
    section("Never go below this temperature: (optional)") {
        input "emergencySetpoint", "decimal", title: "Emergency Temp", required: false
    }
    section("Temperature Threshold (Don't allow heating to go above or bellow this amount from set temperature)") {
        input "threshold", "decimal", "title": "Temperature Threshold", required: false, defaultValue: 1.0
    }
}

def installed() {
    log.debug "running installed"
    
    state.lastTemp = null
    state.contact = true
}

def createDevice() {
    def thermostat
    def label = app.getLabel() + " Thermostat"
    // Commenting out hub refernce - breaks in several thermostat DHs
    log.debug "create device with id:$state.deviceID, named: $label" //, hub: $sensor.hub.id"
    try {
        thermostat = addChildDevice("enishoca", "ESP Thermostat Device", state.deviceID, null, [label: label, name: label, completedSetup: true])
    } catch (e) {
        log.error("caught exception", e)
    }
    return thermostat
}

def getThermostat() {
    return getChildDevice(state.deviceID)
}

def uninstalled() {
    deleteChildDevice(state.deviceID)
}

def updated() {
    log.debug "running updated: $app.label"
    unsubscribe()
    state.deviceID = "${convertIPtoHex(settings.confIpAddr)}:${convertPortToHex(settings.confTcpPort)}"
    def thermostat = getThermostat()
    if (thermostat == null) {
        thermostat = createDevice()
    }
    state.contact = true
    state.lastTemp = null


    subscribe(location, null, lanResponseHandler, [filterEvents: false])

    if (motion) {
        subscribe(motion, "contact", motionHandler)
    }
    subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatSetpoint", thermostatTemperatureHandler)
    subscribe(thermostat, "thermostatMode", thermostatModeHandler)

    startPoll(10)
    //thermostat.setVirtualTemperature(sensor.currentValue("temperature"))
}

def temperatureHandler(evt) {
    if (state.contact || emergencySetpoint) {
        evaluate(evt.doubleValue, thermostat.currentValue("thermostatSetpoint"))
        state.lastTemp = evt.doubleValue
    } else {
        heatingOff()
    }
}

def lanResponseHandler(evt) {
    //log.debug "lanResponseHandler settings: ${settings}"
    log.debug "lanResponseHandler state: ${state}"
    //log.debug "lanResponseHandler Event: ${evt.stringValue}"

    def thermostat = getThermostat()
    def map = stringToMap(evt.stringValue)
    log.debug "map.mac ${map.mac} : map.ip ${map.ip} : map.port ${map.port}"

    // IP and port match the settings update MAC address
    if (map.mac != state.ESPMac) {
        if ((map.ip == convertIPtoHex(settings.confIpAddr) && map.port == convertPortToHex(settings.confTcpPort))) {
            log.debug "Updating remote MAC"
            state.ESPMac = map.mac
            log.debug "Updating remote MAC ${state.ESPMac}"
        } else {
            log.debug "Not ours"
            return
        }
    }
    if (!map.body) {
        log.error "HTTP response has no body"
        return null
    }
    def body = new String(map.body.decodeBase64())
    //log.info "has body ${body}"
    return thermostat.parseTstatData(body)
}

def motionHandler(evt) {
    def thermostat = getThermostat()
    if (evt.value == "closed") {
        state.contact = true
        def thisTemp = thermostat.currentTemperature
        if (thisTemp != null) {
            evaluate(thisTemp, thermostat.currentValue("thermostatSetpoint"))
            state.lastTemp = thisTemp
        }
    } else if (evt.value == "open") {
        state.contact = false
        heatingOff()
    }
}

def thermostatTemperatureHandler(evt) {
    def temperature = evt.doubleValue
    //setpoint = temperature
    log.debug "Desired Temperature set to: $temperature $state.contact"
    def thermostat = getThermostat()
    def thisTemp = thermostat.currentTemperature
    if (state.contact) {
        evaluate(thisTemp, temperature)
    } else {
        heatingOff()
    }
}

def thermostatModeHandler(evt) {
    def mode = evt.value
    log.debug "Mode Changed to: $mode"
    def thermostat = getThermostat()

    def thisTemp = thermostat.currentTemperature
    if (state.contact) {
        evaluate(thisTemp, thermostat.currentValue("thermostatSetpoint"))
    } else {
        heatingOff(mode == 'heat' ? false : true)
    }
}

private evaluate(currentTemp, desiredTemp) {
    log.debug "EVALUATE(current - $currentTemp, desired - $desiredTemp, threshold - ${threshold})"
    // heater
    if ((desiredTemp - currentTemp >= threshold)) {
        heatingOn()
    }
    if ((currentTemp - desiredTemp >= threshold)) {
        heatingOff()
    }
}


def heatingOn() {
    log.debug "heatingOn " + thermostat.currentValue('thermostatMode')
    if (thermostat.currentValue('thermostatMode') == 'heat' || force) {
        log.debug "Heating on Now"
        outlets.on()
        thermostat.setHeatingStatus(true)
    } else {
        heatingOff(true)
    }
}

def heatingOff(heatingOff) {
    def thisTemp = thermostat.currentTemperature
    if (thisTemp <= emergencySetpoint) {
        log.debug "Heating in Emergency Mode Now"
        outlet.on()
        thermostat.setEmergencyMode(true)
    } else {
        log.debug "Heating off Now"
        outlets.off()
        if (heatingOff) {
            thermostat.setHeatingOff(true)
        } else {
            thermostat.setHeatingStatus(false)
        }
    }
}

def startPoll(interval) {
    unschedule()
    pullData()
    // Schedule polling based on preference setting
    def sec = Math.round(Math.floor(Math.random() * 60))
    //def min = Math.round(Math.floor(Math.random() * settings.pollingInterval.toInteger()))
    def min = Math.round(Math.floor(Math.random() * interval))
    //def cron = "${sec} ${min}/${settings.pollingInterval.toInteger()} * * * ?" // every N min
    def cron = "${sec} ${min}/interval * * * ?" // every N min
    log.trace("startPoll - startPoll: schedule('$cron', pullData)")
    schedule(cron, pullData)
}

def pullData() {
    def uri = "/tstat"
    log.debug "Requesting latest data…"
    def hubAction = new physicalgraph.device.HubAction([
        method: "GET",
        path: uri,
        headers: [HOST: getHostAddress()]
    ])
    sendHubCommand(hubAction)
}

private getHostAddress() {
    def ip = settings.confIpAddr
    def port = settings.confTcpPort
    log.debug "getHostAddress Using IP: $ip and port: $port"
    return ip + ":" + port
}

private String convertIPtoHex(ipAddress) {
    if (!ipAddress) return;
    String hex = ipAddress.tokenize('.').collect {
        String.format('%02x', it.toInteger())
    }.join().toUpperCase()
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger()).toUpperCase()
    return hexport
}