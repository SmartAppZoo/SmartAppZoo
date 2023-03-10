/**
 *  Multi-Way Switch
 *
 *  Copyright 2016 Guannan Wang
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
    name: "Multi-Way Switch",
    namespace: "snailium",
    author: "Guannan Wang",
    description: "Group multiple switches to form a multi-way switch. Operation on one switch triggers all the other switches.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Choose switches to group one multi-way switch.") {
        paragraph "Generally, if the device is a physically operable dimmer switch, select it in both category. If it is a virtual dimmer (e.g. light bulb), select only in 'dimmers' category."
        input name: "switches", type: "capability.switch",      title: "Select Switches", multiple: true, required: false
        input name: "dimmers",  type: "capability.switchLevel", title: "Select Dimmers",  multiple: true, required: false
        input name: "dimonoff", type: "bool",                   title: "Use 'dim max/min' to 'turn on/off' a dimmer switch.", description: "Useful if the switch's ramp up rate is not configurable."
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
    subscribe(switches, "switch", switchesHandler)
    subscribe(dimmers, "level",  dimmersHandler)

    state.silent = []
}

def switchesHandler(evt) {
    def evtSource = evt.source
    def evtDevice = evt.device
    def swTarget = evt.value
    def dimLevel = (swTarget == "on") ? 99 : 0;

    if(!state.silent.contains(evt.device.id)) {
        log.debug "Process event '${evt.value}' from ${evtDevice.displayName} (${evt.source})"
        def effectiveSwitches = [];
        def effectiveDimmers = [];
        
        if(dimonoff == true) {
            switches.each({ sw -> if(sw.id  != evt.device.id && !deviceInList(sw, dimmers) && sw.currentValue("switch") != evt.value) effectiveSwitches.add(sw) })
            dimmers.each({ dim -> if(dim.id != evt.device.id && dim.currentValue("level").toInteger() != dimLevel) effectiveDimmers.add(dim) })
        } else {
            switches.each({ sw -> if(sw.id  != evt.device.id && sw.currentValue("switch") != evt.value) effectiveSwitches.add(sw) })
            dimmers.each({ dim -> if(dim.id != evt.device.id && !deviceInList(dim, switches) && dim.currentValue("level").toInteger() != dimLevel) effectiveDimmers.add(dim) })
        }

        state.silent = [];
        if(effectiveSwitches != null && effectiveSwitches.size() > 0)
            state.silent = state.silent + effectiveSwitches.id;
        if(effectiveDimmers  != null && effectiveDimmers.size() > 0)
            state.silent = state.silent + effectiveDimmers.id;
        log.debug "Device ${state.silent} are put into silence mode."

        operateSwitchDimmer(effectiveSwitches, swTarget, effectiveDimmers, dimLevel)
    } else {
        log.debug "Process event '${evt.value}' from ${evtDevice.displayName} (${evt.source}) since device is silenced"
        state.silent.remove(evt.device.id);
    }
}

def dimmersHandler(evt) {
    def evtSource = evt.source
    def evtDevice = evt.device
    def dimLevel = evt.value.toInteger()
    def swTarget = (dimLevel == 0) ? "off" : "on"

    if(!state.silent.contains(evt.device.id)) {
        log.debug "Process event '${evt.value}' from ${evtDevice.displayName} (${evt.source})"
        def effectiveSwitches = [];
        def effectiveDimmers = [];

        switches.each({ sw -> if(sw.id  != evt.device.id && !deviceInList(sw, dimmers) && sw.currentValue("switch") != evt.value) effectiveSwitches.add(sw) })
        dimmers.each({ dim -> if(dim.id != evt.device.id && dim.currentValue("level").toInteger() != dimLevel) effectiveDimmers.add(dim) })

        state.silent = [];
        if(effectiveSwitches != null && effectiveSwitches.size() > 0)
            state.silent = state.silent + effectiveSwitches.id;
        if(effectiveDimmers  != null && effectiveDimmers.size() > 0)
            state.silent = state.silent + effectiveDimmers.id;
        log.debug "Device ${state.silent} are put into silence mode."

        operateSwitchDimmer(effectiveSwitches, swTarget, effectiveDimmers, dimLevel)
    } else {
        log.debug "Process event '${evt.value}' from ${evtDevice.displayName} (${evt.source}) since device is silenced"
        state.silent.remove(evt.device.id);
    }
}

def operateSwitchDimmer(listOfSwitches, switchTarget, listOfDimmers, dimLevel) {
    // Operate switches
    if (switchTarget == "on") {
        if(listOfSwitches != null && listOfSwitches.size() > 0) {
            log.debug "Turn on ${listOfSwitches}"
            listOfSwitches.each { sw -> sw.on() };
        }
    } else if (switchTarget == "off") {
        if(listOfSwitches != null && listOfSwitches.size() > 0) {
            log.debug "Turn off ${listOfSwitches}"
            listOfSwitches.each { sw -> sw.off() };
        }
    }
    // Operate dimmers
    if(listOfDimmers  != null && listOfDimmers.size() > 0) {
        log.debug "Dim ${listOfDimmers} to level ${dimLevel}"
        listOfDimmers.each { dim -> dim.setLevel(dimLevel) };
    }
}

def deviceInList(device, listOfDevices) {
    if(listOfDevices != null && listOfDevices.size() > 0) {
        for(dev in listOfDevices)
            if(dev.id == device.id)
                return true
    }
    return false;
}