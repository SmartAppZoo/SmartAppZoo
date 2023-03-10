definition(
    name: "LR2 Switchboard",
    namespace: "pmckinnon",
    author: "patrick@ojolabs.com",
    description: "Connect LR2 Panel to Installed Devices",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    section("Devices...") {
        input "panel", "capability.switch",
            title: "Switch Panel...",
            required: true

        input "receiver", "capability.switch",
            title: "Stereo Receiver...",
            required: true

        input "masterLighting", "capability.switch",
            title: "Master Lighting...",
            required: true
    }
}

def log(msg) {
    log.debug "[LR2 Switchboard] $msg"
}

def installed() {
    log("installed()")
    initialize()
}

def updated() {
    log("updated()")
    unsubscribe()
    initialize()
}

def initialize() {
    log("initialize()")
    subscribe(panel, "mediaConfigured", mediaConfigured);
    subscribe(panel, "switchAutoLighting", autoLightingChanged);
    configureReceiver()
}

def autoLightingChanged(evt) {
    log("autoLightingChanged($evt.value)")
    evt.value == 'on' ? masterLighting.enable() : masterLighting.disable()
}

def mediaConfigured(evt) {
    log("mediaConfigured($evt.value)")
    configureReceiver()
}

def configureReceiver() {
    def config = [
        input: panel.currentInput,
        level: panel.currentLevel,
        mute:  panel.currentMute,
        zone1: panel.currentState('switchZone1').value,
        zone2: panel.currentState('switchZone2').value
    ]

    log("configureReceiver: $config")
    receiver.configure(config)
}
