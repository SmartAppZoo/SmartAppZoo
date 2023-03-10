/**
 *  zmote Button Creator
 *  Category: My apps
 *  Copyright 2017 (C) chancsc@gmail.com
 *
 *  Credit: Eric Roberts (baldeagle072) - Virtual switch creator
 *  
 */

definition(
    name: "zmote Button Creator",
    namespace: "csc",
    author: "Soon Chye",
    description: "Creates zmote button on the fly!",
    category: "My Apps",
    iconUrl: "https://github.com/chancsc/icon/raw/master/standard-tile%401x.png",
    iconX2Url: "https://github.com/chancsc/icon/raw/master/standard-tile@2x.png",
    iconX3Url: "https://github.com/chancsc/icon/raw/master/standard-tile@3x.png")


preferences {
	section("Create zmote Button") {
		input "switchLabel", "text", title: "Button Label", required: true
	}
    section("on this hub...") {
        input "theHub", "hub", multiple: false, required: true
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
    def deviceId = app.id
    log.debug(deviceId)
    def existing = getChildDevice(deviceId)
    if (!existing) {
        def childDevice = addChildDevice("csc", "ZMote Button", deviceId, theHub.id, [label: switchLabel])
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
