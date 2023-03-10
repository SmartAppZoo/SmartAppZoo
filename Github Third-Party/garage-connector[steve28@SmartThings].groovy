/**
 *  Garage Connector
 *
 *  Author: steve.sell@gmail.com
 *  
 *
 *  Date: 2013-10-15
 */

definition(
    name: "Garage Connector",
    namespace: "steve28",
    author: "steve.sell@gmail.com",
    description: "This a glue app between my ThingShield garage door controller and the garage door tiles",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Select the Garage Controller") {
		input "theGarage", "device.garagedoor", title: "Pick The Garage", required: true
	}
   	section("Select Door Tiles") {
		input "leftDoorTile", "device.garageDoorTile", title: "Pick The Left Door Tile", required: true
        input "rightDoorTile", "device.garageDoorTile", title: "Pick The Right Door Tile", required: true
	}

}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
    initialize()
}

def initialize()
{
	subscribe(theGarage, "leftDoor", evtHandler)
    subscribe(theGarage, "rightDoor", evtHandler)
    
    // subscribe to the tile events so we can "push" the garage door buttons
    subscribe(leftDoorTile, "switch", leftDoorButtonHandler)
    subscribe(rightDoorTile, "switch", rightDoorButtonHandler)

    log.debug "Left door is ${theGarage.currentLeftDoor}"
    log.debug "Right door is ${theGarage.currentRightDoor}"
}

def evtHandler(evt) {
	if (evt.name == "leftDoor")
    {
    	unschedule(checkLeft)
    	if (evt.value == "open")
        {
            // Open the virtual contact sensor
            leftDoorTile.contactOpen()
        }
        if (evt.value == "closed") {
        	// Close the virtual constact sensor
        	leftDoorTile.contactClose()
        }
    }
    else if (evt.name == "rightDoor")
    {
    	unschedule(checkRight)
        if (evt.value == "open")
        {
            // Open the virtual contact sensor
            rightDoorTile.contactOpen()
        }
        if (evt.value == "closed") {
        	// Close the virtual constact sensor
        	rightDoorTile.contactClose()
        }
    }
    else
    {
    	// No idea how it would get here
        log.warn "This is weird - don't know how I got here"
    }
}

def leftDoorButtonHandler(evt) {
	log.debug evt.value
	if ((evt.value == "opening") || (evt.value == "closing")) {
    	theGarage.pushLeft()
    }
}

def rightDoorButtonHandler(evt) {
	log.debug evt.value
	if ((evt.value == "opening") || (evt.value == "closing")) {
    	theGarage.pushRight()
    }
}

