definition(
    name: "Autolock Doors",
    namespace: "xdumaine",
    author: "Xander Dumaine",
    description: "Automatically locks doors a specific door after X minutes when unlocked). Optionally disable auto locking when in a specific mode.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/xdumaine-random-junk/padlock.png",
    iconX2Url: "https://s3.amazonaws.com/xdumaine-random-junk/padlock.png"
)

preferences{
    section("Select the door locks:") {
        input "locks", "capability.lock", required: true, multiple: true
    }
    section("Automatically lock the door when unlocked...") {
        input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section("Disable auto lock when...") {
        input "modes", "mode", title: "Select mode(s) (optional)", multiple: true
    }
    section( "Notifications for Success" ) {
        input("recipients", "contact", title: "Send notifications to", required: false, multiple: true) {
            input "sendPushSuccess", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneSuccess", "phone", title: "Send a Text Message?", required: false
        }
    }
    section( "Notifications for Errors" ) {
        input("recipients", "contact", title: "Send notifications to", required: false, mulitple: true) {
            input "sendPushError", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phoneError", "phone", title: "Send a Text Message?", required: false
        }
    }
}

def installed () {
    initialize()
}

def updated () {
    unsubscribe()
    unschedule()
    initialize()
}

def notifyError (msg) {
    if (sendPushError == 'Yes') {
        sendPush("Auto Lock Doors: ${msg}")
    }
    if (phoneError) {
        sendSms(phoneError, "Auto Lock Doors: ${msg}")
    }
}
def notifySuccess (msg) {
  if (sendPushSuccess == 'Yes') {
      sendPush("Auto Lock Doors: ${msg}")
  }
  if (phoneSuccess) {
      sendSms(phoneSuccess, "Auto Lock Doors: ${msg}")
  }
}

def initialize () {
    log.debug "Settings: ${settings}"
    try {
        locks.each {
           subscribe(it, "lock", doorHandler, [filterEvents: false])
           it.lock()
        }
    } catch (all) {
        notifyError("failed to initialize")
    }

}

def lockDoors () {
    if (modes.contains(location.mode)) {
        notifyError("doors not locked because mode is ${location.mode}")
    } else {
        log.debug "Locking the doors."
        try {
            locks.each { it.lock() }
            notifySuccess("Doors Locked")
        } catch (all) {
            notifyError("encountered an error locking the door(s)")
        }
    }
}

def doorHandler (evt) {
    log.debug("Handling event: " + evt.value)
    def unlocked = locks.find {it.latestValue("lock") == "unlocked" }
    if (unlocked && evt.value == "unlocked") {
        log.debug("Scheduling lock because ${unlocked} was unlocked")
        try {
            runIn((minutesLater * 60), lockDoors) // ...schedule (in minutes) to lock.
        } catch (all) {
            notifyError("failed to schedule door locking. Doors will not auto lock");
        }
    } else {
        log.debug "Not scheduling because ${evt.value}"
    }
}
