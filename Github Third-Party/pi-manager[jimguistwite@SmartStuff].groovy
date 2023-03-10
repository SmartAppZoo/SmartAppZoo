/**
 *  Pi Manager
 *  Copyright 2016 James Guistwite
 *
 *  Links devices to the pi for actions to take place.  Device handlers cannot talk to one another.
 *  I would like to have the IR device send a command to the Pi device to perform some operation but
 *  that is not possible, probably for security reasons.  However, devices can send events to their
 *  "manager" and the manager can call commands on devices.   So, everything routes through this Pi
 *  manager.
 */

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

definition(
    name: "Pi Manager",
    namespace: "jgui",
    author: "James Guistwite",
    description: "Manage events from the Raspberry Pi",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
        input "piIP", "text", "title": "Raspberry Pi IP", multiple: false, required: true, defaultValue:"10.0.0.240"
    	input "piPort", "number", "title": "Raspberry Pi Port", multiple: false, required: true, defaultValue:9000
	}

    section("Choose Pi:") {
        input "thepi", "capability.temperatureMeasurement", required: true, title: "Pi"
    }

    section("Choose Devices:") {
        input "lights", "capability.switch", required: true, title: "Lights", multiple:true
        input "temperatureSensors", "capability.sensor", required: false, title: "Temperature Sensors", multiple:true
        input "garageDoors", "capability.garageDoorControl", required: false, title: "Garage Doors", multiple:true
        input "irDevices", "capability.switch", required: false, title: "IR Devices", multiple:true
        input "commandSequences", "capability.momentary", required: false, title: "Command Sequences", multiple:true
    }

}

def installed() {
	log.debug "Mgr: Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Mgr: Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

/*
def x10Update(device,code,state) {
  log.debug "Mgr: x10Update for ${device} ${code} and ${state} on pi ${thepi}"
  thepi.x10Update(device,code,state);
}

def x10Refresh(device,code) {
  log.debug "Mgr: x10Refresh for ${device} ${code} on pi ${thepi}"
  thepi.x10Refresh(device,code);
}
*/

def initialize() {
  log.debug "Mgr: the devices ${lights} ${temperatureSensors} ${garageDoors} ${irDevices} ${commandSequences}"

  subscribe(location, handlerMethod)
  
  lights.each {
    subscribe(thepi, it.name, x10EventHandler)
    subscribe(it, "x10command", x10CommandHandler)
  }
  temperatureSensors.each {
    subscribe(it, "tempSensorCommand", temperatureCommandHandler)
    subscribe(thepi, it.name, temperatureEventHandler)
  }
  garageDoors.each {
    log.debug("Mgr: subscribe to ${it.name}")
    subscribe(it, "garageDoorCommand", garageDoorCommandHandler)
    //subscribe(thepi, it.deviceNetworkId, garageDoorEventHandler)
    subscribe(thepi, it.deviceNetworkId + ".sensor", garageDoorEventHandler)
  }
  irDevices.each {
    log.debug("Mgr: subscribe to ${it.name}")
    subscribe(it, "irCommand", irCommandHandler)
  }
  commandSequences.each {
    log.debug("Mgr: subscribe to ${it.name}")
    subscribe(it, "irCommand", irCommandHandler)
  }
}

// all event handler methods must accept an event parameter
def locationModeChangeHandler(evt) {
    log.debug "Mgr: event name: ${evt.name}"
    log.debug "Mgr: event value: ${evt.value}"
}

def temperatureCommandHandler(evt) {
  log.debug "Mgr: temp event name: ${evt.name}"
  log.debug "Mgr: temp event value: ${evt.value}"
  def data = new JsonSlurper().parseText(evt.value)
  if (data.command == "refresh") {
    thepi.temperatureSensorRefresh(data.sensorName)
  }
  else {
    log.error "Mgr: unknown command in request"
  }
}

def temperatureEventHandler(evt) {
  log.debug "Mgr: temperature event name: ${evt.name} = ${evt.value}"
  def d = temperatureSensors.find{ d -> d.name == evt.name }
  if (d == null) {
    log.error "Mgr: could not find temperature sensor ${evt.name}"
  }
  else {
    d.setTemp(evt.value)
  }
}

def garageDoorCommandHandler(evt) {
  log.debug "Mgr: garage door event: ${evt.name} ${evt.value}"
  def data = new JsonSlurper().parseText(evt.value)
  if (data.command == "refresh") {
    thepi.refreshInputState(data.pinName)
  }
  else if (data.command == "toggle") {
    thepi.toggleRelay(data.pinName)
  }
  else {
    log.error "Mgr: unknown garage door command in request"
  }
}

def garageDoorEventHandler(evt) {
  log.debug "Mgr: garage door event name: ${evt.name} = ${evt.value}"
  def door = evt.name.replace(".sensor","")
  def d = garageDoors.find{ d -> d.deviceNetworkId == door }
  if (d == null) {
    log.error "Mgr: could not find garage door ${door}"
  }
  else {
    d.setIoState(evt.value)
  }
}


def x10CommandHandler(evt) {
  log.debug "Mgr: x10 command with ${evt.value}"
  def data = new JsonSlurper().parseText(evt.value)
  if (data.command == "refresh") {
    def code = data.houseCodeUnit
    thepi.x10Refresh(code);
  }
  else if (data.command == "update") {
    def code = data.houseCodeUnit
    def state = data.state
    thepi.x10Update(code,state);
  }
  else {
    log.error "Mgr: unknown command in request"
  }
}

/**
 * Proxy the ir command to the Pi for processing.
 */
def irCommandHandler(evt) {
  log.debug "Mgr: ir command with ${evt.value}"
  def data = evt.value;
  if (evt.value instanceof String) {
    // convert string to json structure.
    data = new JsonSlurper().parseText(evt.value)
  }
  thepi.irCommand(data);
}


def x10EventHandler(evt) {
  log.debug "Mgr: x10 event name: ${evt.name} = ${evt.value}"
  def d = lights.find{ d -> d.name == evt.name }
  if (d == null) {
    log.error "Mgr: could not find ${evt.name}"
  }
  else {
    d.setX10State(evt.value)
  }

}
