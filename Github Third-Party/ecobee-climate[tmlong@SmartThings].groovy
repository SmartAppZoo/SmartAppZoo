/**
 *  Ecobee (Climate)
 *
 *  Copyright 2018 Todd Long
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
 *  ###
 *
 *  settings
 *    climates
 *    holdType
 *    modes
 *    thermostats (selected thermostats to use with SmartThings)
 *
 *  state
 *    currentClimateRef
 *    thermostatIds
 *    thermostats (all available ecobee thermostats)
 *
 */
definition(
    name: "Ecobee (Climate)",
    namespace: "tmlong",
    author: "Todd Long",
    description: "Notify ecobee about which climate to use based on your location mode.",
    category: "SmartThings Labs",
    parent: "smartthings:Ecobee (Connect)",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    singleInstance: true,
    pausable: false)

preferences {
    page(name: "pageMode")
    page(name: "pageClimate")
}

def pageMode() {
    // configure debug
    state.debug = [
        enabled: false
    ]

    log.debug "pageMode() debug: ${state.debug}, settings: ${settings}"

    // initialize the available thermostats
    state.thermostats = getThermostats([
        selection: [
            selectionType: "registered",
            selectionMatch: "",
            includeProgram: true
        ]
    ])

    // configure the select metadata
    def select = [
        thermostats: state.thermostats.collectEntries { id, thermostat ->
            [(id): thermostat.name]
        },

        // holdType: dateTime, nextTransition, indefinite, holdHours
        holdTypes: [
            [ indefinite: "Permanent" ],
            [ nextTransition: "Temporary" ]
        ]
    ]

    log.debug "pageMode() select: ${select}"

    dynamicPage(name: "pageMode", title: "", nextPage: "pageClimate", install: false, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee thermostats available and select those you want to connect to SmartThings."
            input(name: "thermostats", title: "Select Thermostats", type: "enum", required: true, multiple: true,
            	description: "Tap to choose", metadata: [values: select.thermostats])
        }

        section {
            paragraph "Tap below to see the list of hold types available and select the one you want to use when changing the climate."
            input(name: "holdType", title: "Select Hold Type", type: "enum", required: true, multiple: false,
            	description: "Tap to choose", metadata: [values: select.holdTypes])
        }

        section {
            paragraph "Tap below to see the list of modes available and select those you want to use for triggering climate changes."
            input "modes", "mode", title: "Select Mode(s)", multiple: true
        }
    }
}

def pageClimate() {
    log.debug "pageClimate() settings: ${settings}"

    // configure the select metadata
    def select = [
        climates: settings.thermostats.collectEntries { id ->
            state.thermostats[id].climates.each { ref, name ->
                [(ref), name]
            }
        }
    ]

    log.debug "pageClimate() select: ${select}"

    dynamicPage(name: "pageClimate", title: "", install: true, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee climates available and select the one you want to use with each mode."

            settings.modes.each { mode ->
                def climateRef = settings[mode]
                def climate = select.climates[climateRef] ?: "not configured"

                log.debug "pageClimate() default climate for \"${mode}\" mode is ${climate}"

                input(name: "${mode}", title: "Select \"${mode}\" Climate", type: "enum", required: true, multiple: false,
                	description: "Tap to choose", defaultValue: climateRef, metadata: [values: select.climates])
            }
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"

    if (!state.init) {
        state.init = true;
    }
}

def updated() {
    // prevent updated() from being called twice
    if ((now() - (state.lastUpdated ?: 0)) < 5000) return

    state.lastUpdated = now()

    log.info "Updated with settings: ${settings}"

    initialize()
}

def initialize() {
    log.debug "initialize()"

    // initialize thermostat ids for selection (i.e. request)
    state.thermostatIds = getThermostatIdsForSelection(settings.thermostats)

    log.info "Initialized with state: ${state}"

    // send the initial mode change
    modeChangeHandler([value: location.mode])

    // initialize the child handlers
    initializeHandlers()

    // unsubscribe from previous events
    unsubscribe()

    // subscribe to "mode" events
    subscribe(location, "mode", modeChangeHandler)
}

def initializeHandlers() {
    log.debug "initializeHandlers()"

    // create the thermostat device handlers
    def devices = settings.thermostats.collect { id ->
        def device = getChildDevice(id)

        if (!device) {
            def thermostat = state.thermostats[id]
            def label = "${thermostat.name} (Climate)" ?: "Ecobee Thermostat (Climate)"

            device = addChildDevice(app.namespace, getHandlerName(), id, null, [label: label])

            log.debug "initializeHandlers() created ${device.displayName} with id: $id"
        } else {
            log.debug "initializeHandlers() found ${device.displayName} with id: $id"
        }

        return device
    }

    log.info "Initialized with devices: ${devices}"

    // delete the thermostat device handlers that are no longer used
    def devicesToDelete = getChildDevices().findAll {
        !settings.thermostats.contains(it.deviceNetworkId)
    }

    if (devicesToDelete) {
        log.warn "initializeHandlers() devices to delete: ${devicesToDelete}"

        devicesToDelete.each { deleteChildDevice(it.deviceNetworkId) }
    }

    pollHandlers()

    runEvery5Minutes("pollHandlers")
}

def pollHandlers() {
    log.debug "pollHandlers()"

    settings.thermostats.each { id ->
        def device = getChildDevice(id)

        if (device) {
            def thermostat = state.thermostats[id]
            def climateRef = state.currentClimateRef ?: thermostat.currentClimateRef

            log.debug "pollHandlers() thermostat: ${thermostat}, climateRef: ${climateRef}"

            // configure the device event
            def event = [
                climate: thermostat.climates[climateRef]
            ]

            log.debug "pollHandlers() event: ${event}"

            device.generateEvent(event)
        }
    }
}

def modeChangeHandler(event) {
    log.debug "modeChangeHandler() event: ${event}"

	def mode = event.value
    def climateRef = settings[mode]

    log.debug "modeChangeHandler() mode: ${mode}, climateRef: ${climateRef}"

    if (climateRef) {
        // set the current climate
        state.currentClimateRef = climateRef

        holdClimate(state.thermostatIds, settings.holdType, climateRef)
    }
}

// @see https://www.ecobee.com/home/developer/api/documentation/v1/operations/get-thermostats.shtml
def getThermostats(Map query) {
    log.debug "getThermostats() query: ${query}"

    if (state.debug.enabled) {
        def thermostats = [
            12345: [
                name: "Stat Home",
                currentClimateRef: "away",
                climates: [
                    away: "Away",
                    home: "Home",
                    sleep: "Sleep",
                    awake: "Awake"
                ]
            ],
            98765: [
                name: "Stat Vacay",
                currentClimateRef: "home",
                climates: [
                    away: "Away",
                    home: "Home",
                    sleep: "Sleep",
                    beach: "Beach"
                ]
            ],
        ]

        log.debug "(debug) getThermostats() return: ${thermostats}"

        return thermostats
    }

    def params = [
        uri: apiEndpoint,
        path: "/1/thermostat",
        headers: ["Content-Type": "application/json", Authorization: "Bearer ${authToken}"],
        query: [format: 'json', body: toJson(query)]
    ]

    def thermostats = [:]

    httpGet(params) { resp ->
        if (resp.status == 200) {
            resp.data.thermostatList.each { thermostat ->
                thermostats[thermostat.identifier] = [
                    name: thermostat.name,
                    currentClimateRef: thermostat.program.currentClimateRef,
                    climates: thermostat.program.climates.collectEntries {
                        [it.climateRef, it.name]
                    }
                ]
            }
        } else {
            log.debug "http status: ${resp.status}, data: ${resp.data}"
        }
    }

    log.debug "getThermostats() return: ${thermostats}"

    return thermostats
}

// @see https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetHold.shtml
def holdClimate(thermostatIds, holdType, climateRef) {
    log.debug "holdClimate() thermostatIds: ${thermostatIds}, holdType: ${holdType}, climateRef: ${climateRef}"

    if (state.debug.enabled) {
        log.debug "(debug) holdClimate() return: true"
        return true
    }

    // define the hold climate payload
    def payload = [
        selection: [
            selectionType: "thermostats",
            selectionMatch: thermostatIds,
            includeRuntime: true
        ],
        functions: [[
            type: "setHold",
            params: [
                holdType: holdType,
                holdClimateRef: climateRef
            ]
        ]]
    ]

    // send the hold climate request
    def success = parent.sendCommandToEcobee(payload)

    log.debug "holdClimate() return: ${success}"

    return success
}

def getThermostatIdsForSelection(thermostats) {
    return thermostats.collect { it }.join(',')
}

def toJson(Map m) { return groovy.json.JsonOutput.toJson(m) }

def getHandlerName() { return "Ecobee Thermostat" }
def getAuthToken()   { return parent.atomicState.authToken }
def getApiEndpoint() { return "https://api.ecobee.com" }