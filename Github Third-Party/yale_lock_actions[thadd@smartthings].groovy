definition(
  name: "Yale Lock Actions",
  namespace: "thadd",
  author: "thadd",
  description: "Triggers Hello, Home actions when the door is locked or unlocked",
  category: "Convenience",
  iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png"
)

preferences {
  page(name: "locksAndActions")
}

def locksAndActions() {
  def phrases = location.helloHome?.getPhrases()*.label

  dynamicPage(name: "locksAndActions", title: "Select devices and action", install: true, uninstall: true) { 
    section("Which door locks?") {
      input "lock1", "capability.lock", multiple: true
    }

    section("Action when door is unlocked via PIN?") {
      if (phrases) {
        input "pinUnlockPhrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
      }
    }

    section("Action when door is unlocked from inside?") {
      if (phrases) {
        input "tumblerUnlockPhrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
      }
    }
    
    section("Action when door is locked from outside?") {
      if (phrases) {
        input "pinLockPhrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
      }
    }
    
    section("Action when door is locked from inside?") {
      if (phrases) {
        input "tumblerLockPhrase", "enum", title: "Trigger Hello Home Action", required: false, options: phrases
      }
    }
  }
}

def installed() {
  subscribe(lock1, "lock", parsePayload)
}

def updated() {
  unsubscribe()
  subscribe(lock1, "lock", parsePayload)
}

def parsePayload(evt) {
  def payload = evt.description.substring(evt.description.lastIndexOf('payload')+9).split(" ");

  // Locked
  if (payload[3] == "15") {

    // Locked via inner knob
    if (payload[4] == "01") {
      if (settings.tumblerLockPhrase) {
        location.helloHome.execute(settings.tumblerLockPhrase)
      }

    // Locked by pushing a button on the keypad
    } else if (payload[4] == "02") {
      if (settings.pinLockPhrase) {
        location.helloHome.execute(settings.pinLockPhrase)
      }
    }

  // Unlocked via inner knob
  } else if (payload[3] == "16") {
    if (settings.tumblerUnlockPhrase) {
      location.helloHome.execute(settings.tumblerUnlockPhrase)
    }

  // Unlocked via PIN
  } else if (payload[3] == "13") {

    // We read the PIN here but aren't doing anything with it yet
    def pin = payload[4];

    if (settings.pinUnlockPhrase) {
      location.helloHome.execute(settings.pinUnlockPhrase)
    }
  }
}