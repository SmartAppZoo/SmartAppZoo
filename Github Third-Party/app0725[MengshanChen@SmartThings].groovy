definition(
    name: "DoorSensor02",
    namespace: "smartthings",
    author: "Seattle University IoT Security Research",
    description: "SmartDoor sensor that incorporates a presence sensor device and a button that actuates on door lock",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Lock") {
        input "lock", "capability.lock", title: "Door lock", description: "Actuator for door state", required: true
        //input "unlock", "capability.lock", title: "Door unlock", description: "Actuator for door state", required: true
    }

    section("Notifications"){
        input("recipients", "contact", title: "Send notifications to") {
        input "phone", "phone", title: "Enter a phone number to get SMS", required: false
        //input "pushAndPhone", "enum", title: "Notify me via Push Notification", required: false, options: ["Yes", "No"]
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(lock, "lock.unlock", eventHandler)
    //subscribe(unlock, "lock.unlock", eventHandler)
}

def eventHandler(evt) {
    log.debug "Notify got evt ${evt}"
    if (frequency) {
        def lastTime = state[evt.deviceId]
        if (lastTime == null || now() - lastTime >= frequency * 60000) {
        sendMessage(evt)
        }
    }
    else {
        sendMessage(evt)
    }
}

private sendMessage(evt) {
    log.trace "$evt.value: $evt, $settings"
    log.debug "$lock was unlock, sending text"
    if (location.contactBookEnabled) {
        sendNotificationToContacts("Your ${lock.label ?: lock.name} was unlock", recipients)
    }
    else {
        sendSms(phone, "Your ${lock.label ?: lock.name} was unlock")
    }
}

mappings {
path("/message/:command"){
    action: [
        PUT: "sendToPhone"
    ]
}

path("/keys") {
    action: [
        GET: "listKeyOwners"
    ]
}

path("/switches/:command") {
    action: [
        PUT: "updateSwitches"
    ]
    }
}

void sendToPhone() {
    // use the built-in request object to get the command parameter
    def command = params.command

    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    if (location.contactBookEnabled) {
        sendNotificationToContacts("Your ${lock.label ?: lock.name} was unlock", recipients)
    }
    else {
        sendSms(command, "Your ${lock.label ?: lock.name} was unlock")
    }
}

//change it into lock
def listKeyOwners() {
    def resp = []
    lock.each {
        resp << [name: it.displayName, value: it.currentValue("lock")]
    }
    return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command

    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "unlock":
            lock.unlock()
            break
        case "lock":
            lock.lock()
            break
        default:
            httpError(400, "$command is not a valid command!")
    }
}


