definition(
    name: "Control Hue bulbs with Aeon Minimote",
    namespace: "treythomas123",
    author: "trey@treythomas.me",
    description: "Choose a button on your Aeon Minimote to change one or more hue bulbs to a certain hue/saturation/lightness.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)
 
preferences {
    section("Select Hue bulbs") {
        input "bulbs", "capability.colorControl", multiple: true
    }
    section("Select remote") {
        input "remote", "capability.button"
    }
    section("Select remote button...") {
        input "buttonNumber", "number", title: "Button Number"
    }
    
    section("Hue (0-100, or 101 for random color)") {
        input "hue", "number", title: "Hue"
    }
    section("Saturation (0-100)") {
        input "saturation", "number", title: "Saturation"
    }
    section("Level (0-100)") {
        input "level", "number", title: "Level"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(remote, "button", buttonHandler)
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(remote, "button", buttonHandler)
}

def buttonHandler(evt) {
    def pressedButtonNumber = evt.jsonData.buttonNumber as Integer

    if ( pressedButtonNumber == buttonNumber ) {
        setBulbs()
    }
}

private setBulbs() {
    if ( level == 0 ) {
        bulbs.off()
    }
    else {
        bulbs.setColor(
            hue: (hue == 101 ? (new Random()).nextInt(100) : hue),
            saturation: saturation,
            level: level
        )
    }
}
