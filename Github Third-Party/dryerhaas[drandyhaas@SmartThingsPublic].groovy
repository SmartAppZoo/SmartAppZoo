definition(
    name: "DryerHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Warn when the dryer fan turns on",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
	section("Choose dryer switches to montior on and off... ") {
		input "myswitch",  "capability.switch"
	}
/*
	section("Warning level...") {
		input "warnlevel", "number", title: "Warning level at least?"
	}
*/
}

def checkforchanges(){
    log.debug("checking for changes")
}

def installed(){
    log.debug("installed")
    state.warned = 0
    subscribe(myswitch, "switch.on",  handler)
    subscribe(myswitch, "switch.off", handler)
    subscribe(myswitch, "greeting", greetings)
}

def updated(){
    unsubscribe()
    installed()
}

def greetings(evt){
    log.debug "dryer switch warning app: $evt.name, $evt.value, $settings"
    def level = evt.value.substring(0, evt.value.length()-2).toInteger()
    log.debug "level is '${level}'"
    if (level<120){
       log.debug("dryer switch $level : power reset?")
       sendPush("dryer switch $level : power reset?")
    }
}

def handler(evt){
    log.debug "dryer switch warning app: $evt.name, $evt.value, $settings"
    //log.debug("warnlevel is $warnlevel")
    def level = evt.value
    log.debug("current level is $level")
    //if (level.toInteger() > warnlevel){
    if (level=="on"){
       log.debug("switch on and warned = $state.warned")
       if (state.warned==0) sendPush("dryer switch is : $level")
       state.warned = 1 // record that we already warned about this
    }
    else if (level=="off"){
       log.debug("switch off")
	   if (state.warned==1) sendPush("dryer switch is now : $level")
       state.warned = 0 // record that we have not already warned about this now
    }
    else {
       log.debug("switch unknown: $level ")
	   if (state.warned==0) sendPush("dryer switch is now : $level")
       state.warned = 1 // record that we already warned about this
    }
    
}