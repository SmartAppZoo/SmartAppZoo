definition(
    name: "SwitchActionHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Change mode, after a delay, when a switch is turned off",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
    
    page(name: "options", nextPage: "selectActions", install: false){
	section("Choose switch to monitor for off... ") {
		input "myswitch",  "capability.switch"
	}
	section("Delay") {
		input "mydelay", "number", title: "Wait this many minutes..."
	}
    }

    page(name: "selectActions")

}


def selectActions() {
    dynamicPage(name: "selectActions", title: "Select Hello Home Action to Execute", install: true, uninstall: true) {
        // get the available actions
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {
            // sort them alphabetically
            actions.sort()
            section("Hello Home Actions") {
              log.trace actions
              // use the actions as the options for an enum input
              input "myaction", "enum", title: "Select an action to execute", options: actions
            }
        }
    }
}

def checkforchanges(){
    log.debug("checking for changes")
}

def installed(){
    log.debug("installed")
    //subscribe(myswitch, "switch.on",  handler)
    subscribe(myswitch, "switch.off", handler)
    //subscribe(myswitch, "greeting", greetings)
}

def updated(){
    unsubscribe()
    installed()
}

def handler(evt){
    log.debug "switch delay action app: $evt.name, $evt.value, $settings"
    def level = evt.value
    log.debug("current action is $level")
    if (level=="off"){
       log.debug("switch off : in $mydelay minutes do action $myaction")
       sendPush("switch delay : in $mydelay minutes do action $myaction")
       runIn(mydelay * 60, domyaction)
    }
    else {
       log.debug("switch unknown action: $level ")
    }    
}

def domyaction() {
    log.debug "execute action $myaction"
    location.helloHome?.execute(myaction)
}
