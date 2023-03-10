/**
 *  send button presses to HASS
 *
 *  Copyright 2020 Iesus Sonesosn
 */
definition(
    name: "Home assistant event push",
    namespace: "tekhass",
    author: "Iesus",
    description: "send SmartThings pushbutton events to home assistant",
    category: "Convenience",
    iconUrl: "http://teknisksupport.se/aqara_wall_switch_double_rocker_1.jpg",
    iconX2Url: "http://teknisksupport.se/aqara_wall_switch_double_rocker_1.jpg",
    iconX3Url: "http://teknisksupport.se/aqara_wall_switch_double_rocker_1.jpg")


preferences {
     section("Track these Buttons:") {
        input "buttons", "capability.button", multiple: true, required: true
    }
     section("Track these Actuators:") {
        input "actuators", "capability.actuator", multiple: true, required: false
    }
    section ("hass Server") {
        input "hass_host", "text", title: "Home assistant Hostname/IP"
        input "hass_port", "number", title: "Home assistant Port"
        input "hass_token", "text", title: "Home assistant long lived token"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    doSubscriptions()
}

def doSubscriptions() {
      subscribe(buttons, "button", genericHandler)
      subscribe(actuators, "actuator", genericHandler)
}

def genericHandler(evt) {
    def buttonNumber = parseJson(evt.data)
    def json = "{"
    json += "\"name\":\"${evt.name}\","
    json += "\"event\":\"${evt.value}\","
    json += "\"button_number\":\"${buttonNumber.buttonNumber}\","
    json += "\"display_name\":\"${evt.displayName}\","
    json += "\"device\":\"${evt.device}\","
    json += "\"device_id\":\"${evt.deviceId}\","
    json += "\"description\":\"${evt.description}\","
    json += "\"description_text\":\"${evt.descriptionText}\""
    json += "}"
    log.debug("JSON: ${json}")

    def request = sendCommand(json)
}

private sendCommand(payload) {
    def path = "/api/events/tekhass.button"
    def method = "POST"
    def headers = [:] 

    headers.put("HOST", "${hass_host}:${hass_port}")
    headers.put("Content-Type", "application/json")
    headers.put("Authorization", "Bearer ${hass_token}") 
    
    try {
        def result = new physicalgraph.device.HubAction(
            [
                method: method,
                path: path,
                body: payload,
                headers: headers
            ], null, [callback: parse]
        )
        
        return sendHubCommand(result)
    } catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def parse(result) {
    log.debug(result.json)
}

