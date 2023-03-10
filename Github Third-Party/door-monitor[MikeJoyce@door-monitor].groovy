definition(
    name: "Door Monitor",
    namespace: "MikeJoyce",
    author: "Mike Joyce",
    description: "Be notified when: a door is open too long, you leave with a door open, or a door opens while you are not present",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-active.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-active@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Solution/doors-active@2x.png")

preferences {
    section {
        input "doors", "capability.contactSensor", title: "Which doors?", multiple: true, required: true
        input "doorTimeout", "number", title: "How many hours before notifying?", defaultValue: 2, required: true
        
        // TODO: Make them choose if they want this feature, then pop up device selections
        input "presenceSensors", "capability.presenceSensor", title: "Which people?", multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(doors, "contact.open", openEventHandler)
    subscribe(doors, "contact.closed", closedEventHandler)
    subscribe(presenceSensors, "presence.not present", noLongerPresentEventHandler)
    state.doorTimeoutInSeconds = settings.doorTimeout * 3600
}

/******************/
/* Event Handlers */

def openEventHandler(event) {
    if(getNumOfPeoplePresent() == 0)
    {
        sendPush("Door ${event.displayName} just opened while you are gone!")
    }

    if(!state.doorTimeoutScheduled) {
        runIn(state.doorTimeoutInSeconds, openDoorTimeoutHandler)
        state.doorTimeoutScheduled = true
    }
}

def closedEventHandler(event) {
    def openDoors = getOpenDoors()
    if(openDoors?.empty) {
        unschedule(openDoorTimeoutHandler)
        state.doorTimeoutScheduled = false
    }
}

def noLongerPresentEventHandler(event)
{
    def openDoors = getOpenDoors()
    if(!openDoors?.empty) {
        sendPush("${openDoors} door(s) have been left open!!")
    }
}

/******************/
/* Helper Methods */

def getNumOfPeoplePresent() {
    def present = settings.presenceSensors.findAll{ presence -> presence.currentPresence == "present"}
     return present.size()
}

def getOpenDoors() {
    return settings.doors.findAll { door -> door.currentContact == "open"}
}

/******************/
/* Timer Handlers */

def openDoorTimeoutHandler() {
    def openDoors = getOpenDoors()
    state.doorTimeoutScheduled = false
    
    sendPush("${openDoors} door(s) have been open for ${settings.doorTimeout} hours!")
}

