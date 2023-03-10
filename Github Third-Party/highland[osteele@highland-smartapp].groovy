/**
 *  Highland Automation Service Manager
 *
 *  Oliver Steele
 *  osteele.com
 *  2016-01-28
 *
 *  Relay SmartThings events to Highland Cloud.
 */

definition(
    name: "Highland Home",
    namespace: "osteele.com",
    author: "Oliver Steele",
    description: "Connector to Highland Cloud",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/assets.osteele.com/smartthings/highland-home.png",
    iconX2Url: "https://s3.amazonaws.com/assets.osteele.com/smartthings/highland-home@2x.png",
    iconX3Url: "https://s3.amazonaws.com/assets.osteele.com/smartthings/highland-home@2x.png"
) {
    appSetting "api_url"
    appSetting "api_secret"
}

preferences {
    section("Allow Highland Home access to these things...") {
      input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: false
      input "motions", "capability.motionSensor", title: "Motion sensors", multiple: true, required: false
      input "presences", "capability.presenceSensor", title: "Presence sensors", multiple: true, required: false
      input "switches", "capability.switch", title: "Switches", multiple: true, required: false
    }
}

def installed() {
    log.debug "installed"
    subscribe()
}

def updated() {
    log.debug "updated"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(dimmers, "dimmer", eventHandler)
    subscribe(motions, "motion", eventHandler)
    subscribe(presences, "presence", eventHandler)
    subscribe(switches, "switch", eventHandler)

    subscribe(location, "position", eventHandler)
    subscribe(location, "sunset", eventHandler)
    subscribe(location, "sunrise", eventHandler)
    subscribe(location, "sunsetTime", eventHandler)
    subscribe(location, "sunriseTime", eventHandler)
}

def eventHandler(evt) {
    def api_url = appSettings.api_url
    def api_secret = appSettings.api_secret
    if (!api_url || !api_secret) {
        return
    }

    def device = evt.device

    def body = [
        "secret": api_secret,
        "date": evt.isoDate,
        "deviceId": evt.deviceId,
        "deviceName": device?.displayName,
        "eventName": evt.name,
        "value": evt.value,
        "isStateChange": evt.isStateChange
    ]

    log.debug "POST ${api_url} ${body}"
    httpPostJson(api_url, body) { log.debug "POST ${body} response=${response}" }
}
