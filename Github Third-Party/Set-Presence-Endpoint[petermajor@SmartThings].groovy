definition(
    name: "Presence endpoint",
    namespace: "petermajor",
    author: "Peter Major",
    description: "External endpoint for setting presence that can be called via Tasker",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Which presence sensor?") {
        input "presence", "capability.presenceSensor", title: "presence", required: true, multiple: false
    }
}

mappings {
  path("/presence/:present") {
    action: [
      PUT: "updatePresence"
    ]
  }
}

def installed() {}
    
def updated() {}

def updatePresence() {
    log.debug "updatePresence() executed"

    def isPresent = params.present
    log.debug "isPresent = $isPresent"

    if (isPresent == "1") {
        log.debug "setPresence('present')"
        presence.setPresence("present")
    } else {
        log.debug "setPresence('not present')"
        presence.setPresence("not present")
    }
}