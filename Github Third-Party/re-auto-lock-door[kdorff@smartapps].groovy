definition(
    name: "Re-Auto Lock Door",
    namespace: "kdorff",
    author: "Arnaud-and-kdorff",
    description: "Automatically locks a specific door after X seconds a sensor closes or after an unlock.",
    category: "Safety & Security",
    iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
    iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg",
    pausable: true
)

preferences{
    page name: "mainPage", install: true, uninstall: true
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        section("Select the door lock:") {
            input "lock1", "capability.lock", required: true
        }
        section("Select the door contact sensor:") {
            input "contact", "capability.contactSensor", required: true
        }
        section("Automatically lock the door when closed...") {
            input "secondsLater", "number", title: "Delay (in seconds):", required: true
        }
        if (location.contactBookEnabled || phoneNumber) {
            section("Notifications") {
                input("recipients", "contact", title: "Send notifications to", required: false) {
                    input "phoneNumber", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false
                }
            }
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)"
        }
    }
}

def installed(){
    initialize()
}

def updated(){
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(contact, "contact.open", doorHandler)
    subscribe(contact, "contact.closed", doorHandler)
}

def lockDoor(){
    log.debug "Locking the door."
    lock1.lock()
    if(location.contactBookEnabled) {
        if ( recipients ) {
            log.debug ( "Sending Push Notification..." ) 
            sendNotificationToContacts( "${lock1} locked after ${contact} was closed for ${secondsLater} seconds!", recipients)
        }
    }
    if (phoneNumber) {
        log.debug("Sending text message...")
        sendSms( phoneNumber, "${lock1} locked after ${contact} was closed for ${secondsLater} seconds!")
    }
}

def doorHandler(evt){
    if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }   
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
        runIn( (secondsLater), lockDoor ) // ...schedule (in seconds) to lock.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "open")) { // If a person opens an unlocked door...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
        runIn( (secondsLater), lockDoor ) // ...schedule (in seconds) to lock.
    }
}
