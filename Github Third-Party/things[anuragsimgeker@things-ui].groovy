/**
 *  Things
 *
 *  Copyright 2017 Anurag Simgeker
 *
 */
definition(
    name: "Things",
    namespace: "anuragsimgeker",
    author: "Anurag Simgeker",
    description: "A SmartApp to control Devices using an external web service.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true) {

    // Application setting for the external web service endpoint.
    appSetting "apiServerUrl"
}

preferences {
    section("Allow access to these Dimmer Switches and Outlets") {
        input "switches", "capability.switch", multiple: true
    }
    section("Allow access to these Open/Close Sensors") {
        input "contactSensors", "capability.contactSensor", multiple: true
    }
    section("Allow access to these Motion Sensors") {
        input "motionSensors", "capability.motionSensor", multiple: true
    }
    section("Allow access to these Z-Wave Locks") {
        input "locks", "capability.lock", multiple: true
    }
}

mappings {
    path("/events/subscribe") { action: [ PUT: "subscribeToEvents" ] }
    path("/events/unsubscribe") { action: [ PUT: "unsubscribeFromEvents" ] }
    path("/things") { action: [ GET: "getThings" ] }
    path("/switches/:id") { action: [ PUT: "updateSwitch" ] }
    path("/locks/:id") { action: [ PUT: "updateLock" ] }
    path("/alarmSystemStatus") { action: [ PUT: "updateAlarmSystemStatus" ] }
}

/**
 * Called when a SmartApp is first installed.
*/
def installed() {}

/**
 *  Called when the preferences of an installed smart app are updated.
*/
def updated() {}

/**
 * Subscribes to SmartThings events.
 *
 * Listens to changes in Device attributes and calls call a handler when the Event happens.
*/
def subscribeToEvents() {
    unsubscribeFromEvents()
    subscribe(switches, "switch", eventHandler)
    subscribe(switches, "level", eventHandler)
    subscribe(contactSensors, "contact", eventHandler)
    subscribe(motionSensors, "motion", eventHandler)
    subscribe(locks, "lock", eventHandler)
    subscribe(location, "alarmSystemStatus", eventHandler)
}

/**
 * Unsubscribe from SmartThings events.
 *
 * Stops listening to changes in Device attributes.
*/
def unsubscribeFromEvents() {
    unsubscribe()
}

/**
 * Builds an Array of Devices the SmartApp has access to.
 *
 * Returns a list like [[type: "contactSensor", id: 1, label: "Garage Door", status: 'ONLINE', values: [contact: 'closed']].
 *
 * @return an Array of devices.
*/
def getThings() {
    def things = []

    // Dimmer Switch or Outlet
    switches.each {thing ->
        things.add([type: thing.hasAttribute('level') == true ? 'switchLevel' : 'switch', id: thing.id, label: thing.label, status: thing.getStatus(), values: [ switch: thing.latestValue('switch'), level: thing.latestValue('level') ] ])
    }

    // Open/Closed Sensor
    contactSensors.each {thing ->
        things.add([type: 'contactSensor', id: thing.id, label: thing.label, status: thing.getStatus(), values: [ contact: thing.latestValue('contact') ] ])
    }

    // Motion Sensor
    motionSensors.each {thing ->
        things.add([type: 'motionSensor', id: thing.id, label: thing.label, status: thing.getStatus(), values: [ motion: thing.latestValue('motion') ] ])
    }

    // Z-Wave Lock
    locks.each {thing ->
        things.add([type: 'lock', id: thing.id, label: thing.label, status: thing.getStatus(), values: [ lock: thing.latestValue('lock') ] ])
    }

    // Smart Home Monitor
    def alarmSystemStatus = location.currentState("alarmSystemStatus")
    if (alarmSystemStatus.value != 'unconfigured') {
        things.add([ type: 'alarmSystemStatus', values: [ alarmSystemStatus: alarmSystemStatus.value ] ])
    }

    // Sort A-Z by Device label
    things = things.sort { it.label }

    return things
}

/**
 * Update a Dimmer Switch or Outlet.
 *
 * Uses the built-in request object to retrieve and execute a command.
 *
 * @return empty response or a http error.
*/
def updateSwitch() {
    def switchId = params.id
    def command = request.JSON?.command
    def level = request.JSON?.level
    def device = switches.findAll { it.id == switchId }[0]

    if (!device) {
        httpError(400, "Switch with ID $switchId not found")
    }

    if (!device.hasCommand(command)) {
        httpError(400, "Switch with ID $switchId does not have command $command")
    }

    switch(command) {
        case "setLevel":
            device.setLevel(level)
            break
        case "on":
            device.on()
            break
        case "off":
            device.off()
            break
        default:
            httpError(400, "Invalid command $command")
    }
}

/**
 * Update a Z-Wave Lock.
 *
 * Uses the built-in request object to retrieve and execute a command.
 *
 * @return empty response or a http error.
*/
def updateLock() {
    def lockId = params.id
    def command = request.JSON?.command
    def device = locks.findAll { it.id == lockId }[0]

    if (!device) {
        httpError(400, "Lock with ID $lockId not found")
    }

    if (!device.hasCommand(command)) {
        httpError(400, "Lock with ID $lockId does not have command $command")
    }

    switch(command) {
        case "lock":
            device.lock()
            break
        case "unlock":
            device.unlock()
            break
        default:
            httpError(400, "Invalid command $command")
    }
}

/**
 * Update the Alarm System.
 *
 * Uses the built-in request object to retrieve and update the Alarm System Status.
 *
 * @return empty response or a http error.
*/
def updateAlarmSystemStatus() {
    def lockId = params.id
    def command = request.JSON?.command

    switch(command) {
        case "away":
            sendLocationEvent(name: "alarmSystemStatus", value: "away")
            break
        case "stay":
            sendLocationEvent(name: "alarmSystemStatus", value: "stay")
            break
        case "off":
            sendLocationEvent(name: "alarmSystemStatus", value: "off")
            break
        default:
            httpError(400, "Invalid command $command")
    }
}

/**
 * Send a Synchronous External HTTP Request with details of each Event.
 *
 * @param an Event object.
*/
def eventHandler(evt) {
    def params = [
        uri: appSettings.apiServerUrl,
        path: "/smartthings/event",
        body: [
            locationId: evt.locationId,
            deviceId: evt.deviceId,
            name: evt.name,
            value: evt.value,
            displayName: evt.displayName,
            isStateChange: evt.isStateChange()
        ]
    ]

    try {
        httpPostJson(params)
    } catch (e) {
        log.error "eventHandler httpPostJson Error: $e"
    }
}
