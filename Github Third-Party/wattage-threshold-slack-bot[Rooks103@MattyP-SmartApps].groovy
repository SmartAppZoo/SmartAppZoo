/**
 *  SmartApp for setting a wattage threshold, then sending a Slack notification when going above then back below
 *  the threshold. Example use cases would be to detect when a Coffee Pot is brewing or when a dishwasher is running
 *
 *  Author: Matt Peterson
 */
 
include 'asynchttp_v1'
 
definition (
    name: "Wattage Threshold Slack Bot",
    namespace: "rooks103",
    author: "Matt Peterson",
    description: "SmartApp for sending Slack Notifications when a wattage threshold is broken (i.e. appliance is on)",
    category: "SmartThings Labs",
    iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances14-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances14-icn@2x.png"
)

preferences {
    page(name: "App Configuration", title: "Setup", install: true, uninstall: true) {
        section("Device Configuration") {
            input "powerMeter", "capability.powerMeter", title: "Coffee Pot Outlet", required: true, multiple: false
            input "threshold", "decimal", title: "Wattage Threshold for Notifications", required: false, default: 500
            input "debounceValue", "decimal", title: "Time value in seconds for ignoring warming cycles", required: true, default: 330
        }
        section("Slack Configuration") {
            input "slackURI", "text", title: "Slack Instance", required: true, description: "URI for Slack Instacne e.g. smartthings.slack.com"
            input "slackChannel", "text", title: "Slack Channel", required: true, description: "Channel to get message e.g. #general"
            input "slackToken", "password", title: "Slack API Token", required: true, description: "API Token for Slackbot" 
            input "message", "text", title: "Finished Message", required: true, description: "Message people get when threshold device go below threshold"
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: true
        }
    }
}

def installed() {   
    setupSmartApp()    
}

def updated() {
    unsubscribe()
    setupSmartApp()
}

def setupSmartApp() {
    state.lastPowerValue = 0
    state.lastThresholdEventTime = new Date()
    subscribe(powerMeter, "power", takeAction)
}

private Map paramsBuilder(msg) {
    Map slackParams = [
        uri: "https://$slackURI/api/chat.postMessage",
        headers: [
            "Authorization": "Bearer $slackToken"
        ],
        body: [
            channel: "$slackChannel",
            text: "$msg",
            icon_emoji: ":coffee:",
            as_user: true
        ]
    ]
    return slackParams
}

def takeAction(evt) {
    def meterValue = evt.value as float
    def thresVal = "$threshold" as float 
    def temp = evt.date - state.lastThresholdEventTime
    log.debug "Event happened at ${evt.date} -- ${temp} seconds since threshold event"

    if ((meterValue > thresVal) && (state.lastPowerValue < thresVal)) {
        // Value went above threshold. Log time of event to determine if this a full pot or just warming cycle
        state.lastThresholdEventTime = evt.date
    } else if ((meterValue < thresVal) && (state.lastPowerValue > thresVal) && (evt.date - state.lastThresholdEventTime > debounceValue)) {
        // Value went below the threshold and debouce value exceed.
        asynchttp_v1.post(processResponse, paramsBuilder("$message"))    
    }
    state.lastPowerValue = meterValue
}

def processResponse(response, data) {
    // Nothing
}
