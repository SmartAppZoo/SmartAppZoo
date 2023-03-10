/**
 *  vThing Delegator
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
 */
definition(
        name: "vThing Delegator",
        namespace: "tmlong",
        author: "Todd Long",
        description: "A virtual delegator of things.",
        category: "SmartThings Labs",
        iconUrl: "https://cdn.rawgit.com/tmlong/SmartThings/master/resources/images/delegator.png",
        iconX2Url: "https://cdn.rawgit.com/tmlong/SmartThings/master/resources/images/delegator@2x.png",
        iconX3Url: "https://cdn.rawgit.com/tmlong/SmartThings/master/resources/images/delegator@2x.png",
        singleInstance: true)

preferences {
    page(name: "pageCheck")
    page(name: "pageMain")
    page(name: "pageCapability")
    page(name: "pageSettings")
}

def pageCheck() {
    isParent ? pageMain() : pageCapability()
}

def pageMain() {
    log.debug "pageMain()"

    dynamicPage(name: "pageMain", title: "", install: true, uninstall: false) {
        section(title: hasThings ? "My vThings" : "") {
        }

        section(title: hasThings ? " " : "") {
            app(name: "vThing", appName: app.name, namespace: "tmlong", title: "Add vThing...", multiple: true, uninstall: false)
        }

        if (isInstalled) {
            section(title: "App Info") {
                href "pageSettings", title: "Settings", description: ""
            }
        }
    }
}

def pageCapability() {
    log.debug "pageCapability()"

    // configure the select metadata
    def select = [
        capabilities: [
            values: _Capability.keySet().collect()
        ]
    ]

    dynamicPage(name: "pageCapability", title: "", install: true, uninstall: false) {
        section {
            input "capability", "enum", title: "Select Capability", required: true, submitOnChange: true, metadata: select.capabilities

            if (capability) {
                input "delegates", capability.type, title: capability.title, multiple: true
            }
        }

        section {
            label title: "Assign Name", required: false, defaultValue: nextThingName
        }
    }
}

def pageSettings() {
    log.debug "pageSettings()"

    dynamicPage(name: "pageSettings", title: "", install: false, uninstall: true) {
        section {
            paragraph "Caution: You are about to uninstall this app and all of your configured things. This action cannot be undone. If you would like to proceed, tap the \"Remove\" button below."
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"
}

def updated() {
    // prevent updated() from being called twice
    if ((now() - (atomicState.lastUpdated ?: 0)) < 5000) return

    atomicState.lastUpdated = now()

    log.info "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize() isParent: ${isParent}"

    if (!isParent) initializeThing()
}

def initializeThing() {
    log.debug "initializeThing()"

    // initialize the device id
    atomicState.deviceId = atomicState.deviceId ?: handlerId

    // initialize the device handler
    initializeHandler()

    // subscribe to the delegates capability event
    subscribe(delegates, capability.event, delegatesHandler)
}

def initializeHandler() {
    log.debug "initializeHandler()"

    if (!device) {
        // add the device handler
        addChildDevice(app.namespace, handlerName, atomicState.deviceId, null, [label: app.label])

        log.debug "initializeHandler() created device: ${device}"
    } else {
        log.debug "initializeHandler() found device: ${device}"
    }

    // delete the device handlers that are no longer used
    def devicesToDelete = getChildDevices().findAll { it.deviceNetworkId != atomicState.deviceId }

    if (devicesToDelete) {
        // delete the device handlers
        devicesToDelete.each { deleteChildDevice(it.deviceNetworkId) }

        log.warn "initializeHandler() deleted devices: ${devicesToDelete}"
    }

    // send the initial event
    delegatesHandler([])
}

def delegatesHandler(event) {
    log.debug "delegatesHandler() event: ${event.name}, value: ${event.value}, displayName: ${event.displayName}"

    // check if we are in a working state
    if (atomicState.working) {
        // update the device state
        def workingState = atomicState.working
        workingState[event.deviceId] = event.value
        atomicState.working = workingState

        // determine if we are still working
        def workingCount = workingState.count { k, v -> (v == event.value) }

        log.trace "delegatesHandler() workingCount: ${workingCount}, delegates: ${delegates.size()}"

        if (workingCount == delegates.size()) {
            atomicState.working = null
        } else {
            return
        }
    }

    // send the device state event
    device.sendEvent(name: capability.event, value: device.determineState(delegates))
}

def doDelegation(command) {
    doDelegation(command, [])
}

def doDelegation(command, args) {
    log.debug "doDelegation() command: ${command}, args: ${args}"

    // initialize the working state
    atomicState.working = delegates.collectEntries { [(it.id): it.currentValue(capability.event)] }

    // send the device transition state event
    device.sendEvent(name: capability.event, value: device.determineTransitionState(command))

    // delegate!
    switch(args?.size()) {
        case 1:
            delegates?."${command}"(args[0])
            break
        case 2:
            delegates?."${command}"(args[0], args[1])
            break
        default:
            delegates?."${command}"()
    }
}

//
// Helper functions.
//

def getCapability() {
    _Capability."${settings.capability}"
}

def getNextThingName() {
    def i = 1

    parent.childApps.any {
        def childApp = parent.childApps.any { it.label == "vThing #${i}" }
        return !childApp ? true : (i++ && false)
    }

    "vThing #${i}"
}

def getDevice() {
    getChildDevice(atomicState.deviceId)
}

def getDelegates() {
    settings.delegates
}

def getHandlerName() {
    capability.handler
}

def getHandlerId() {
    "${capability.event}.${UUID.randomUUID().toString()}"
}

def getIsInstalled() {
    (app.installationState == "COMPLETE")
}

def getHasThings() {
    app.childApps
}

def getIsParent() {
    !parent
}

def get_Capability() {
    [
        contact: [
            event: "contact",
            type: "capability.contactSensor",
            title: "Select Contact Sensors",
            handler: "vThing Contact Sensor"
        ],
        dimmer: [
            event: "switch",
            type: "capability.switchLevel",
            title: "Select Dimmers",
            handler: "vThing Dimmer"
        ],
        outlet: [
            event: "switch",
            type: "capability.outlet",
            title: "Select Outlets",
            handler: "vThing Switch"
        ],
        switch: [
            event: "switch",
            type: "capability.switch",
            title: "Select Switches",
            handler: "vThing Switch"
        ]
    ]
}