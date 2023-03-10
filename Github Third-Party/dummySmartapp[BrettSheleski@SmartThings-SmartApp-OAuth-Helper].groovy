definition(
    name: "DummySmartApp",
    namespace: "BrettSheleski",
    author: "Brett Sheleski",
    description: "Dummy SmartApp for demonstration purposes",
    category: "SmartThings Labs",
    iconUrl: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor1-icn@2x.png"
)

preferences {
    input "switches", "capability.switch", title: "Switches", required: true, multiple: true
}

mappings {
  path("/all-on") {
    action: [
      POST: "allOn"
    ]
  }

  path("/all-off") {
    action: [
      POST: "allOff"
    ]
  }
}

def allOn(){
    switches.each{
        it.on();
    }
}

def allOff(){
    switches.each{
        it.off();
    }
}