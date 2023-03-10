/**
 *  State of the World
 *
 *  Copyright 2014 Alex Malikov
 *
 */
definition(
    name: "State of the World",
    namespace: "625alex",
    author: "Alex Malikov",
    description: "Get state of the world",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "State of the World", displayLink: ""])


preferences {
	section("Get state of these things...") {
        input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
        input "contacts", "capability.contactSensor", title: "Which Contact?", multiple: true, required: false
        input "presence", "capability.presenceSensor", title: "Which Presence?", multiple: true, required: false
    }
    section("Report to the following Pushbullet devices...") {
    	input "pushbullets", "device.pushbullet", title: "Which Pushbulet Devices?", multiple: true, required: false
    }
    section("Polling Interval") {
        input "interval", "number", title:"Set polling interval (in minutes)", defaultValue:15
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	subscribe(location, handler)
    subscribe(app, handler2)
    subscribe(switches, "switch", handler)
    subscribe(locks, "lock", handler)
    subscribe(contacts, "contact", handler)
    subscribe(presence, "presence", handler)
    
    reportState()
}

def handler(event) {
    log.debug "event"
    runIn(60, reportIfChanged, [overwrite: true])
}
def handler2(event) {
    log.debug "event2"
    reportState()
}

def reportIfChanged() {
	updateState()
    log.debug "entered reportIfChanged, state.currentState: $state.currentState"
	if (state.currentState != state.lastState) {
    	report()
    }
}

def reportState() {
	updateState()
	log.debug "entered reportState, state.currentState: $state.currentState"
    report()
}

def report() {
	state.lastState = state.currentState
    unschedule()
    runIn(interval * 60, reportState, [overwrite: true])
    
    log.debug "report message: ${getMessage()}"
    pushbullets.each() {it ->
    	it.push("SmartThings $location State", getMessage())
    }
}

def getMessage() {
    //new TreeMap(state.currentState + [updated : getTS()]) as String
    //"Present: $state.currentState.present | Closed: $state.currentState.closed | Locked : $state.currentState.locked | On: $state.currentState.on | $state.currentState.mode @ ${getTS()}"
    new groovy.json.JsonBuilder(new TreeMap(state.currentState + [updated : getTS()])) as String
}

def updateState() {

	[switches, locks, contacts, presence].flatten().each{
    log.debug "updateState $it - ${it?.supportedCommands}"
    	if (it?.supportedCommands?.name?.contains("refresh")) {
        log.debug "refreshing $it"
        	it.refresh()
        }
    }
    
	state.currentState = [mode: location.mode]
    
	if (presence) {
   		state.currentState << [present : find(presence, "presence", "present")]
        state.currentState << [notPresent : find(presence, "presence", "not present")]
    }
    if (locks) {
    	state.currentState << [unlocked : find(locks, "lock", "unlocked")]
        state.currentState << [locked : find(locks, "lock", "locked")]
    }
    if (switches) {
    	state.currentState << [on : find(switches, "switch", "on")]
        state.currentState << [off : find(switches, "switch", "off")]
    }
    if (contacts) {
    	state.currentState << [open : find(contacts, "contact", "open")]
        state.currentState << [closed : find(contacts, "contact", "closed")]
    }
}

def find(devices, attribute, value) {
	def found = devices?.findAll{it.latestValue(attribute) == value}
    if (!found) return "none"
    if (devices == found) return "all"
    
    found?.collect{it.displayName}.sort().join(", ")
}

def getTS() {
	def tf = new java.text.SimpleDateFormat("h:mm a")
    tf.setTimeZone(location.timeZone)
    "${tf.format(new Date())}"
}
