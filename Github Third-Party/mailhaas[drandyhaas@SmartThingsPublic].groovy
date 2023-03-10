definition(
    name: "MailHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Warn when the mail arrives",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
	section("Choose switch to monitor... ") {
		input "myswitch",  "capability.switch"
	}
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
    log.debug "warning app: $evt.name, $evt.value, $settings"
    if (evt.value == "mail"){
       log.debug "mailbox opened!"
       sendPush("mailbox opened!")
    }
    if (evt.value == "ping") {
       log.debug "doing nuttin"
    }
}

def handler(evt){
    log.debug "warning app: $evt.name, $evt.value, $settings"
}