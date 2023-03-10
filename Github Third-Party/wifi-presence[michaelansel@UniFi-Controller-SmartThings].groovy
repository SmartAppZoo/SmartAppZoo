definition(
    name: "WiFi Presence",
    namespace: "michaelansel",
    author: "Michael Ansel",
    description: "API for updating WiFi Presence device",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section(title: "Select Presence Sensor") {
        input "myDevice", "device.wifiPresenceSensor", title: "Select WiFi Presence Sensor", required: true, multiple: false
    }
}

// SmartApp callbacks
def installed() {
  createAccessToken()
  getToken()
  log.debug "$myDevice.name Token: $state.accessToken Base URL: ${apiServerUrl("api/smartapps/installations/$app.id/")}"
}

def updated() {
  log.debug "$myDevice.name Token: $state.accessToken Base URL: ${apiServerUrl("api/smartapps/installations/$app.id/")}"
}

// API callbacks
mappings {
  path("/home") {
    action: [
      POST: "postHome"
    ]
  }
  path("/away") {
    action: [
      POST:"postAway"
    ]
  }
}

def postHome() {
  if (myDevice.currentValue('presence') != 'present') {
    log.debug "arrived: $myDevice"
    myDevice.arrived();
  }
}

def postAway() {
  if (myDevice.currentValue('presence') != 'not present') {
    log.debug "departed: $myDevice"
    myDevice.departed();
  }
}

// Helpers
private def getToken() {
  if (!state.accessToken) {
    try {
      getAccessToken()
    } catch (ex) {
      log.debug "Did you forget to enable OAuth in SmartApp IDE settings?"
      log.debug ex
    }
  }
}