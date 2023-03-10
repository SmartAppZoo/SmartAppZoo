/*
 *  Device Information
 *
 *  Copyright 2014 Paul Spee All Rights Reserved
 *
 */
definition(
    name: "Device Information",
    namespace: "pspee",
    author: "Paul Spee",
    description: "Display Device Information",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name: "selectCapability"
	page name: "selectDevice"
    page name: "displayDevice"
}

def selectCapability() {
	def pageProperties = [
    	name: "selectCapability",
		title: "Select Capability of Device",
		nextPage: "selectDevice",
        install: false,
		uninstall: true
    ]
    
    return dynamicPage(pageProperties) {
		section() {
        	input "mycapability", "enum", title: "Which Capability?", metadata:[values:["alarm", "battery", "configuration", "contactSensor", "illuminanceMeasurement", "motionSensor", "polling", "presenceSensor", "relativeHumidityMeasurement", "sensor", "switch", "temperatureMeasurement", "thermostatMode", "threeAxisMeasurement", "waterSensor"]], required: true, multiple: false
		}
	}
}


def selectDevice() {
	def pageProperties = [
    	name: "selectDevice",
		title: "Select Device with ${mycapability} Capability",
		nextPage: "displayDevice",
        install: false,
		uninstall: true
    ]
    
    return dynamicPage(pageProperties) {
		section() {
        	input "mydevice", "capability.${mycapability}", title: "Which Device?", required: true, multiple: false
		}
	}
}

def displayDevice() {
	def pageProperties = [
    	name: "displayDevice",
		title: "${mydevice.displayName} (${mydevice.name})",
        install: true,
		uninstall: false
    ]
    
    def mydeviceState = ""
    def cr = false
    mydevice.supportedAttributes.each {
        if (cr) mydeviceState += "\n"
        def myvalue = mydevice.currentValue("$it")
    	mydeviceState += "$it: $myvalue"
        cr = true
    }
    
    return dynamicPage(pageProperties) {
		section("Capabilities") {
        	paragraph "${mydevice.capabilities}"
		}
        section("Attributes") {
        	paragraph "$mydeviceState"
        }
        section("Commands") {
        	paragraph "${mydevice.supportedCommands}"
		}
	}
}

def installed() {
}

def updated() {
}
