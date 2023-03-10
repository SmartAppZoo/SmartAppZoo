definition(
    name                : "CountSwitchons",
    namespace           : "Thestfield",
    author              : "Andy Sheen",
    description         : "This add on smartApp just counts switch turnons and logs them.",
    category            : "Convenience",
    iconUrl             : "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url           : "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url           : "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@3x.png"
)


preferences {
    section() {
        input "theswitch", "capability.switch", multiple: true
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    // initialize counter
    for ( x in theswitch ) {
        //nid = $x.deviceNetworkId
                log.debug "Switch ID: $x.deviceNetworkId, class: $x.getClass"
        state.$nid.count = 0
    }
    subscribe(theswitch, "switch.on", incrementCounter)
}

def incrementCounter(evt) {
        //state.${evt.displayName}.count = state.${evt.displayName}.count + 1
        //log.debug "switch has been turned on state.${evt.displayName}.count times"
    def data = parseJson(evt.data)
    log.debug "Event data: $data"
    log.debug "Event time: $evt.date"
    log.debug "Event desc: $evt.description"
    log.debug "Event dtxt: $evt.descriptionText"
    log.debug "Event dev.: $evt.device"
    log.debug "Event name: $evt.displayName"
    log.debug "Event dvid: $evt.deviceId"
    log.debug "Event id  : $evt.id"
    log.debug "Event hub : $evt.hubId"
    log.debug "Event SAid: $evt.installedSmartAppId"
}
