/**
 *  TeslaFi (Connect)
 *
 *  Copyright 2019 Joe Hansche
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
        name: "TeslaFi (Connect)",
        namespace: "jhansche.teslafi",
        author: "Joe Hansche",
        description: "Integrates your Tesla vehicle using the TeslaFi service middleware. A TeslaFi subscription and API token are required.",
        category: "SmartThings Labs",
        singleInstance: true, // TODO: support multiple instances to allow for multiple cars?
        usesThirdPartyAuthentication: true,
        iconUrl: "https://teslafi.com/favicon.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://teslafi.com/images/LogoNew.png"
)

preferences {
    page(name: "introTeslaFiToken", title: "TeslaFi Subscription Required")
    page(name: "selectCars", title: "Select Your Tesla")
}

def introTeslaFiToken() {
    log.debug("introTeslaFiToken")
    def showUninstall = fiToken != null
    return dynamicPage(name: "introTeslaFiToken", title: "Connect to TeslaFi", nextPage: "selectCars", uninstall: showUninstall) {
        section("TeslaFi") {
            paragraph "A TeslaFi subscription is required."
            href(
                    name: "TeslaFi API link",
                    title: "View TeslaFi API settings",
                    required: false,
                    style: "external",
                    url: "https://teslafi.com/api.php",
                    // If you're not already logged in, this will take you to the login page and then dump you on the main home screen.
                    // You have to be logged in when tapping this, in order to be taken to the right page.
                    description: "Tap to view TeslaFi API settings and generate your API token (should be logged in first)."
            )
            input(
                    name: "fiToken", title: "TeslaFi API Token", type: "text",
                    description: "Copy the `Current Token' from the TeslaFi API settings.",
                    required: true
            )
        }
    }
}

def selectCars() {
    log.debug("selectCars()")
    refreshVehicles()
    // XXX: TeslaFi only allows a single car per account, so this is kind of pointless.
    return dynamicPage(name: "selectCars", title: "TeslaFi Car", install: true, uninstall: true) {
        section("Select Tesla") {
            input(name: "selectedVehicles", type: "enum", required: true, multiple: false, options: state.accountVehicles)
        }
    }
}

def doCommand(command, expectResponse = "application/json") {
    def queryParams = [
            token: fiToken,
            command: command
    ]
    def params = [
            uri: "https://www.teslafi.com/feed.php",
            query: queryParams,
            contentType: expectResponse ?: "application/json"
    ]
    def result = [:]

    try {
        httpGet(params) { resp ->
            log.trace "GET response for ${command}: ${resp.data}"

            if (resp.data.response?.vehicle_id) {
                // the wake_up command returns a `response:{..}` object straight from Tesla API, which will have a vehicle_id matching the DNI
                result = resp.data.response
            } else {
                // Other commands are mostly in a flattened KVP format
                result = resp.data.collectEntries { key, value ->
                    if (value == "None")
                        [key, null]
                    else if (value == "True" || value == "False")
                        [key, Boolean.parseBoolean(value)]
                    else [key, value]
                }
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        // This might be because the response is not in JSON format
        log.error "Failed to make request to ${params}: ${e.getStatusCode()} ${e}", e
        return null
    } catch (e) {
        log.error "Failed to make request to ${params}: ${e}", e
        return null
    }
    return result
}

def refreshVehicles() {
    state.accountVehicles = [:]
    state.vehicleData = [:]

    def result = [:]
    def data = doCommand("lastGood")

    if (!data) {
        log.info("No response from `lastGood` command")
        return
    }

    result.id = data.vehicle_id
    result.name = data.display_name
    result.vin = data.vin
    result.state = data.state // "online", even when carState==sleeping; when would state be anything else?
    result.car_state = data.carState // "Sleeping"
    result.sleep_state = data.carState == "Sleeping" ? "sleeping" : "not sleeping"
    result.version = data.car_version

    result.driveState = [
            latitude: data.latitude,
            longitude: data.longitude,
            speed: (data.speed?.toInteger() ?: -1),
            heading: data.heading?.toInteger(),
            lastUpdateTime: data.Date,
    ]
    result.motion = result.driveState.speed > 0 ? "active" : "inactive"

    result.chargeState = [
            chargingState: data.charging_state,
            batteryRange: data.battery_range?.toFloat(),
            battery: data.battery_level?.toInteger(),

            hoursRemaining: data.time_to_full_charge?.toFloat(),

            chargerVoltage: data.charger_voltage?.toInteger(), // = ~110 V
            chargerCurrent: data.charger_actual_current?.toInteger(), // = 12A
            chargerPowerKw: data.charger_power?.toFloat(), // = 1 kW; FIXME: this is horribly rounded,
            chargerPower: (data.charger_voltage == null || data.charger_actual_current == null ? null : (
                    data.charger_voltage.toInteger() * data.charger_actual_current.toInteger()
            )), // = ~1320 W
            chargeEnergyAdded: data.charge_energy_added?.toFloat(), // charge_energy_added // 1.57 kWh
            chargeRate: data.charge_rate?.toFloat(), // = ~0.6 MPH

            fastChargerPresent: data.fast_charger_present == "1",
            fastChargerBrand: data.fast_charger_brand,
            fastChargerType: data.fast_charger_type,

            // chargerType: L1, L2, L3
            // Tesla Supercharging (see chargeNumber=632), powerSource=dc
            // fast_charger_type == "Tesla" or fast_charger_brand = "Tesla"? If that's brand, what is type?
            // fast_charger_present == "1" ?
            // voltage >= 300V
            // current (A) is missing?

            // L2 fast charger (see chargeNumber=625), powerSource=mains
            // fast_charger_type == ""
            // fast_charger_present == "0" ?
            // voltage >= 200V
            // current =~ 30A

            // L1 Home/mobile charger, powerSource=mains
            // fast="", present="0"
            // fast_charger_type == "MCSingleWireCAN"
            // voltage =~ 110V
            // current =~ 12A
    ]

    result.location = [
            homeLink: data.homelink_nearby,
            tagged: data.location,
    ]
    def isHome = result.location.tagged?.toLowerCase()?.contains("home") ||
            result.location.homeLink == "1"

    result.vehicleState = [
            presence: isHome ? "present" : "not present",
            lock: data.locked == "1" ? "locked" : "unlocked",
            odometer: data.odometer?.toFloat(),
    ]

    result.doors = [
            charger: data.charge_port_door_open?.equals("1"),
            // TODO: FL, FR, RL, RR | RT, FT
            //  Not supported by TeslaFi
    ]

    result.climateState = [
            temperature: data.inside_tempF?.toInteger(),
            thermostatSetpoint: data.driver_temp_settingF,
            thermostatMode: data.is_climate_on == "1" ? "auto" : "off",
            outsideTemp: data.outside_tempF?.toInteger(), // FIXME: this isn't actually part of "climate"
    ]

    state.accountVehicles[result.id] = result.name
    state.vehicleData[result.id] = result

    log.debug("Parsed result: ${result}")

    return result
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
    log.debug("initialized")
    ensureDevicesForSelectedVehicles()
    removeNoLongerSelectedChildDevices()
}

def refresh(child) {
    def data = [:]
    def id = child.device.deviceNetworkId
    refreshVehicles()
    return state.vehicleData[id]
}

private ensureDevicesForSelectedVehicles() {
    if (selectedVehicles) {
        def dni = selectedVehicles
        if (dni instanceof org.codehaus.groovy.grails.web.json.JSONArray) dni = dni[0]
        log.debug("Looking at vehicle: ${dni}")

        def d = getChildDevice(dni)
        if (!d) {
            def vehicleName = state.accountVehicles[dni]
            device = addChildDevice("jhansche.teslafi", "Tesla", dni, null, [name: "Tesla ${dni}", label: vehicleName])
            log.debug "created device ${device} with id ${dni}"
            device.initialize()
        } else {
            log.debug "device for ${d.label} with id ${dni} already exists"
        }
    }
}

private removeNoLongerSelectedChildDevices() {
    // Delete any that are no longer in settings
    def delete = getChildDevices().findAll { !selectedVehicles }
    removeChildDevices(delete)
}

private removeChildDevices(delete) {
    log.debug "deleting ${delete.size()} vehicles"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

// region Child commands

def beep(child) {
    log.debug "Honking child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("honk")
    log.debug "Result: ${data}"
    return data?.response?.result ?: false
}

def lock(child) {
    log.debug "Locking child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("door_lock", "text/plain")
    log.debug "Lock result: ${data}"
    return data?.response?.result ?: false
}

def unlock(child) {
    log.debug "Unlocking child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("door_unlock", "text/plain")
    log.debug "Unlock result: ${data}"
    return data?.response?.result ?: false
}

def wake(child) {
    log.debug "Waking child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("wake_up")
    log.debug "Wake result: ${data}"
    return data?.vehicle_id == dni ?: false
}

def startCharge(child) {
    log.debug "Starting charge on child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("charge_start")
    log.debug "Start Charge result: ${data}"
    return data?.response?.result ?: false
}

def stopCharge(child) {
    log.debug "Stopping charge on child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("charge_stop")
    log.debug "Stop Charge result: ${data}"
    return data?.response?.result ?: false
}

def climateAuto(child) {
    log.debug "Turning on climate on child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("auto_conditioning_start")
    log.debug "Climate Auto result: ${data}"
    return data?.response?.result ?: false
}

def climateOff(child) {
    log.debug "Turning off climate on child ${child.device}"
    def dni = child.device.deviceNetworkId
    def data = doCommand("auto_conditioning_stop")
    log.debug "Climate Off result: ${data}"
    return data?.response?.result ?: false
}

// endregion