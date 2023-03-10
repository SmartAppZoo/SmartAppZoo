/*****************************************************************************************************************
 *
 *  A SmartThings child smartapp which creates the "room" device using the rooms occupancy DTH.
 *  Copyright (C) 2017 bangali
 *
 *  License:
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 *  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.
 *  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Version: 0.02
 *
 *  DONE:
 *  Forked from: https://github.com/adey/bangali/blob/master/smartapps/bangali/rooms-child-app.src/rooms-child-app.groovy
 *****************************************************************************************************************/

definition(
        name: "Occupancy Detector Child App",
        namespace: "makutaku",
        parent: "makutaku:Occupancy Detector",
        author: "makutaku",
        description: "DO NOT INSTALL DIRECTLY OR PUBLISH. Creates the \"room\" device using the Room Occupancy DTH.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
}

private getRoomDeviceId() {
    return "rm_${app.id}"
}

private childCreated() {
    if (getChildDevice(getRoomDeviceId()))
        return true
    else
        return false
}

def mainPage() {

    def childWasCreated = childCreated()

    dynamicPage(name: "mainPage",
            title: "Adding a room",
            install: true, uninstall: childWasCreated) {
        if (!childWasCreated) {
            section {
                label title: "Room name:", defaultValue: app.label, required: true
            }
        } else {
            section {
                paragraph "Room name:\n${app.label}"
            }
        }
        section("Update room state on away mode?") {
            input "awayModes", "mode", title: "Away mode(s)?", required: false, multiple: true
        }
        section("Which sensors will be used to learn if someone might be present in the room?") {
            sensorsInputs()
        }
        section("Revert room back to 'vacant' when motion is not detected?") {
            timeoutInputs()
        }
        section("Does this room contains other rooms?") {
            innerRoomInputs()
        }
        section("Send Notifications?") {
        	input("recipients", "contact", title: "Send notifications to", required: false) {
            	input "phone", "phone", title: "Warn with text message (optional)",
                	description: "Phone Number", required: false
        	}
    	}
    }
}

def sensorsInputs() {
    input "insideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) inside the room", required: false, multiple: true
    input "perimeterContactSensors", "capability.contactSensor", title: "Contact sensor(s) used to access the room", required: false, multiple: true
    input "outsideMotionSensors", "capability.motionSensor", title: "Motion sensor(s) outside the room", required: false, multiple: true
}

def timeoutInputs() {
    input "reservationTimeOutInSeconds", "number", title: "Time out when room is 'reserved'", required: false, multiple: false, defaultValue: 3 * 60, range: "5..*"
    input "occupationTimeOutInSeconds", "number", title: "Time out when room is 'occupied'", required: false, multiple: false, defaultValue: 60 * 60, range: "5..*"
    input "engagementTimeOutInSeconds", "number", title: "Time out when room is 'engaged'", required: false, multiple: false, defaultValue: 12 * 60 * 60, range: "5..*"
    input "outerPerimeterRestorationDelayInSeconds", "number", title: "Outside motion sensor delay'", required: false, multiple: false, defaultValue: 30, range: "0..*"
}

def innerRoomInputs() {
    input "innerRooms", "device.RoomOccupancy", title: "Inner rooms", required: false, multiple: true
}

def installed() {
    logDebug("Installed app ${app.label} with settings: ${settings}")
    logDebug("app label: ${app.label}")
    logDebug("app name: ${app.name}")
}

def uninstalled() {
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updated() {
    logDebug("Updated app ${app.label} with settings: ${settings}")
    logDebug("app label: ${app.label}")
    logDebug("app name: ${app.name}")
    unschedule()
	unsubscribe()
    initialize()
}

def initialize() {
    logDebug("Initializing app ${app.label} ...")
    if (!childCreated()) {
        spawnChildDevice(app.label)
    }
    
    if (awayModes) {
        subscribe(location, modeEventHandler)
    }
    if (insideMotionSensors) {
        logDebug("Subscribing to inside motion sensors ...")
        subscribe(insideMotionSensors, "motion.active", insideMotionActiveEventHandler)
        subscribe(insideMotionSensors, "motion.inactive", insideMotionInactiveEventHandler)
    }
    if (perimeterContactSensors) {
        logDebug("Subscribing to perimeter contact sensors ...")
        subscribe(perimeterContactSensors, "contact.open", perimeterContactOpenEventHandler)
        subscribe(perimeterContactSensors, "contact.closed", perimeterContactClosedEventHandler)
    }
    if (outsideMotionSensors) {
        logDebug("Subscribing to outside motion sensors ...")
        subscribe(outsideMotionSensors, "motion.active", outsideMotionActiveEventHandler)
        subscribe(outsideMotionSensors, "motion.inactive", outsideMotionInactiveEventHandler)
    }
    if (innerRooms) {
        logDebug("Subscribing to inner rooms ...")
        subscribe(innerRooms, "roomOccupancy", innerRoomsEventHandler)
        subscribe(innerRooms, "motion.active", insideMotionActiveEventHandler)
        subscribe(innerRooms, "motion.inactive", insideMotionInactiveEventHandler)
    }
    logDebug("Initialization done.")
}

private sendNotification(message) {
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(message, recipients)
    } else {
        if (phone) {
            sendSms(phone, message)
        }
    }
}

private logDebug(message) {
	message = "${app.label}: " + message
	log.debug message
    //sendNotification message
}

private logInfo(message) {
	message = "${app.label}: " + message
	log.info message
    //sendNotification message
}

private getRoomState() {
    def roomDevice = getChildDevice(getRoomDeviceId())
    def state = roomDevice.getRoomState()
    return state
}

private setRoomState(requestedState) {
    def roomDevice = getChildDevice(getRoomDeviceId())
    def currentState = roomDevice.getRoomState()

    if (currentState == "kaput") {
        logInfo("Not changing room state to '${requestedState}' because room is not in service.")
        return false
    }

    if (requestedState != currentState) {
        logInfo("Requesting room state change: '${currentState}' => '${requestedState}'")
        roomDevice.generateEvent(requestedState)
        return true
    }
    
    return false
}

def makeRoomVacant() {
    logDebug("Making room vacant.")
    setRoomState('vacant')
}

def makeRoomReserved() {
    logDebug("Making room reserved.")
    setRoomState('reserved')
}

def makeRoomOccupied() {
    logDebug("Making room occupied.")
    setRoomState('occupied')
}

def makeRoomEngaged() {
    logDebug("Making room engaged.")
    setRoomState('engaged')
}

def innerRoomsMaxState() {
    def maxState = "kaput"
    if (innerRooms) {
        innerRooms.each { dev ->
            def state = dev.currentValue('roomOccupancy')
            maxState = greaterState(state, maxState)
        }
    }
    return maxState
}

def greaterState(state1, state2) {
    def stateValue = ['kaput', 'vacant', 'reserved', 'occupied', 'engaged']
    return stateValue.indexOf(state1) > stateValue.indexOf(state2) ? state1 : state2
}

private isOuterPerimeterBreached() {
    if (outsideMotionSensors) {
        return outsideMotionSensors.any { sensor -> sensor.currentValue("motion") == "active" }
    }
    return true
}

private isInnerPerimeterBreached() {
    if (perimeterContactSensors) {
        return perimeterContactSensors.any { sensor -> sensor.currentValue("contact") == "open" }
    }
    return true
}

private isRoomActive() {
    def active = false

    if (insideMotionSensors) {
        active = insideMotionSensors.any { sensor -> sensor.currentValue("motion") == "active" }
    }

    if (!active) {
        def roomsMaxState = innerRoomsMaxState()
        active = (roomsMaxState != 'vacant') && (roomsMaxState != 'kaput')
    }

    return active
}

def getReservationTimeOutInSecondsAsInteger() {
    return (reservationTimeOutInSeconds != null) ? reservationTimeOutInSeconds.toInteger() : 0
}

def getOccupationTimeOutInSecondsAsInteger() {
    return (occupationTimeOutInSeconds != null) ? occupationTimeOutInSeconds.toInteger() : 0
}

def getEngagementTimeOutInSecondsAsInteger() {
    return (engagementTimeOutInSeconds != null) ? engagementTimeOutInSeconds.toInteger() : 0
}

def scheduleTimeout(roomState = null) {
    def timeoutInSeconds = 0
    roomState = roomState ?: getRoomState()

    switch (roomState) {
        case "reserved":
        timeoutInSeconds = getReservationTimeOutInSecondsAsInteger()
        break;
        case "occupied":
        timeoutInSeconds = getOccupationTimeOutInSecondsAsInteger()
        break;
        case "engaged":
        timeoutInSeconds = getEngagementTimeOutInSecondsAsInteger()
        break;
    }

    if (timeoutInSeconds > 0) {
        logDebug("Scheduling timeout for state '${roomState}' to ${timeoutInSeconds} seconds.")
    	runIn(timeoutInSeconds, timeoutExpired)
    }
}

def cancelTimeout() {
    logDebug("Cancelling timeout.")
    unschedule(timeoutExpired)
}

def timeoutExpired() {
    logInfo("Timeout has expired.")
    makeRoomVacant()
}

def innerRoomsEventHandler(evt) {
    def roomDevice = getChildDevice(getRoomDeviceId())
    def latestvalue = roomDevice.latestValue('roomOccupancy')
    logDebug("Inside: ${evt.displayName} has changed from ${latestvalue} to ${evt.value}")
}

def insideMotionActiveEventHandler(evt) {
    logDebug("Inside: ${evt.displayName} has changed to ${evt.value}")
    cancelTimeout()

    if (isInnerPerimeterBreached()) {
        if (isOuterPerimeterBreached())
            makeRoomReserved()
        else
            makeRoomOccupied()
    } else {
        makeRoomEngaged()
    }
}

def insideMotionInactiveEventHandler(evt) {
    logDebug("Inside: ${evt.displayName} has changed to ${evt.value}")
    if (isRoomActive()) {
	    logInfo("Inside motion still active.")
    }
    else {
    	logInfo("Inside motion is now inactive.")
    	scheduleTimeout()
    }
}

def perimeterContactOpenEventHandler(evt) {
	logDebug("Inner perimeter: ${evt.displayName} has changed to ${evt.value}")
    logInfo("Inner perimeter has been breached")
    if (isOuterPerimeterBreached())
        makeRoomReserved()
    else
        makeRoomOccupied()
        
    if (!isRoomActive()) {
    	logInfo("Inside motion was inactive when inner perimeter breached.")
    	scheduleTimeout()
    }
}

def perimeterContactClosedEventHandler(evt) {
	logDebug("Inner perimeter:${evt.displayName} has changed to ${evt.value}")
    logDebug(isInnerPerimeterBreached() ?
            "Inner perimeter is still breached"
            : "Inner perimeter is restored")
}

def outsideMotionActiveEventHandler(evt) {
    logDebug("Outer perimeter: ${evt.displayName} has changed to ${evt.value}")
    logInfo("Outer perimeter:  has been breached")
    unschedule(outsidePerimeterRestorationHandler)
    def state = getRoomState()
    if (state == "occupied") {
        makeRoomReserved()
     	if (!isRoomActive()) {
    		scheduleTimeout()
    	}
    }
}

def outsideMotionInactiveEventHandler(evt) {
    logDebug("Outer perimeter: ${evt.displayName} has changed to ${evt.value}")

    def outerPerimeterBreached = isOuterPerimeterBreached()
    if (outerPerimeterBreached) {
        logDebug("Outer perimeter is still breached")
        return
    }

    runIn(outerPerimeterRestorationDelayInSeconds, outsidePerimeterRestorationHandler)
}

def outsidePerimeterRestorationHandler() {
    logInfo("Outer perimeter is restored")

    if (getRoomState() == "engaged" || !isRoomActive()) {
        //nothing to do
        return
    }

    if (isInnerPerimeterBreached()) {
 		logDebug("Making room occupied because outer perimeter has been restored while inner perimeter is still breached.")
        makeRoomOccupied()
    } else {
    	logDebug("Making room engaged because outer perimeter has been restored while inner perimeter is not breached.")
        makeRoomEngaged()
    }
        
    if (!isRoomActive()) {
        scheduleTimeout()
    }
}

def modeEventHandler(evt) {
    if (awayModes && awayModes.contains(evt.value)) {
        makeRoomVacant()
    }
}

def spawnChildDevice(roomName) {
    if (!childCreated()) {
        logDebug("Spawning child device.")
        def device = addChildDevice("makutaku", "Room Occupancy", getRoomDeviceId(), null,
                [name: getRoomDeviceId(), label: roomName, completedSetup: true])
        logInfo("Child device created: name=${device.name} label=${device.label}")
    }
}

def handleRoomStateChange(oldState = null, state = null) {
    logInfo("Room has changed state: '${oldState}' => '${state}'")
    if (state && oldState != state) {
        scheduleTimeout(state)
        return true
    }
    return false
}
