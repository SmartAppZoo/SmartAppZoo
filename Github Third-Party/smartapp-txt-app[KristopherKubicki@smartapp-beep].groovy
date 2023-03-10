/**
 *  Text App Command
 *
 */
definition(
    name: "Text App Command",
    namespace: "KristopherKubicki",
    author: "Kristopher Kubicki",
    description: "Text an App Command to a phone number",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

	section("Virtual Switch"){
		input "dswitch", "capability.switch", title: "Which?", required: true, multiple: false
	}
    section("Send a text message to...") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Phone number?"
        }
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
	subscribe(dswitch, "switch.on", switchHandler)
}

def switchHandler(evt) {
	if(evt.source == "APP_COMMAND") { 
		log.debug "${evt.source} is the source: ${evt.description} : ${evt.displayName}"
    	sendSms(phone, "${evt.displayName}")
    }
}
