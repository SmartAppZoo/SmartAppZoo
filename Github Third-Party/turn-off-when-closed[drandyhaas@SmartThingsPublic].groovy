//Turn It Off When Closed (for fireplace, originally)

definition(
    name: "Turn Off When Closed",
    namespace: "drandyhaas",
    author: "Andy Haas",
    description: "Turn something off when a door is closed",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When door(s) close(s)..."){
		input "doors", "capability.contactSensor", title: "Which door(s)?", multiple:true
	}
    section("Turn off switch(es)..."){
		input "switches", "capability.switch", multiple: true
	}
}

def installed()
{
    subscribe(doors, "contact.closed", closedHandler)
    log.debug "subscribed to doors $doors closing to turn off switches $switches "
}

def updated()
{
	unsubscribe()
	installed()
}

def closedHandler(evt) {
    def val = evt.value
    log.debug "value: $val, settings: $settings "
    switches.off()     
}
