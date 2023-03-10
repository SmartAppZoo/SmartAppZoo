definition(
    name: "ButtonFunctionActionHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Execute a function on some device, when a button is pushed",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
        
	section("Choose switch to monitor... ") {
		input "myswitch",  "capability.switch", required: false, multiple: true
	}
    section("Choose button to monitor... ") {
		input "mybutton",  "capability.button", required: false, multiple: true
	}
    section("Choose device to control... ") {
		input "mydevice",  "capability.switch"
	}
	section("function") {
		input "myfunc", "string", title: "Execute this function on device when button pushed..."
        input "myfuncon", "string", title: "Execute this function on device when switched on..."
        input "myfuncoff", "string", title: "Execute this function on device when switched off..."
	}
}

def installed(){
    log.debug("installed")
    subscribe(myswitch, "switch",  handler)
    subscribe(mybutton, "button", handler)
}

def updated(){
    unsubscribe()
    installed()
}

def handler(evt){
    log.debug "button action app: $evt.name, $evt.value, $settings"
    def level = evt.value
    log.debug("current action is $level")
    if (level=="off"){
       log.debug("switch off ")
       mydevice."$myfuncoff"()
    }
    else if (level=="on"){
       log.debug("switch on ")
       mydevice."$myfuncon"()
    }
    else if (level=="pushed"){
       log.debug("button pushed ")
       mydevice."$myfunc"()
    }
    else {
       log.debug("unknown action: $level ")
    }    
}