/**
 *  Garage Door Controller
 *
 *  Copyright 2018 Parul Jain
 *
 *  Licensed under the MIT License
 */

definition(
    name: "Garage Door Controller",
    namespace: "paruljain",
    author: "Parul Jain",
    description: "Combines garage door momentary switch with title sensor",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section ("Garage Door Push Button") {
        input "doorControl", "capability.doorControl", multiple:false, required: true
    }
    section ("Garage Door Tilt Sensor") {
        input "tiltSensor", "capability.contactSensor", multiple: false, required: true
    }
}

def installed() {
    subscribe(tiltSensor, "contact.closed", doorClosedHandler)
    subscribe(tiltSensor, "contact.open", doorOpenHandler)
}

def updated() {
    unsubscribe()
    installed()
}

def doorClosedHandler(evt) {
    doorControl.setClosed()
}

def doorOpenHandler(evt) {
    doorControl.setOpen()
}
