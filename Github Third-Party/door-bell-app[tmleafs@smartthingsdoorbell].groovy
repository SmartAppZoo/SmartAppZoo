/**
 *  Door Bell App
 *
 *  Author: Steven Dale (tmldale)
 *  Date: 2016-02-17
 *	
 *
 *  Change Log:
 *  18-02-16 - Change State to atomicState 
 *
 *
 */
definition(
    name: "Door Bell App",
    namespace: "tmleafs",
    author: "tmleafs",
    description: "Get a push notification for Custom Made Door Bell see http://tinyurl.com/hl6dh8l.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Select Your Door Bell"){
		input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: true
    }
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(contactClosed, "contact.closed", eventHandler)
}

def eventHandler(evt) {
	log.debug "Notify got evt ${evt}"
    def lastTime = atomicState[evt.deviceId]
    log.debug "lasttime $lastTime"
if(lastTime == null){
            atomicState[evt.deviceId] = now()
            log.debug "null message sent"
			sendMessage(evt)
}
else if(now() - lastTime >= 15000) {
			atomicState[evt.deviceId] = now()
            def nowlastTime = now() - lastTime
            log.debug "nowlasttime $nowlastTime"	
            log.debug "message sent"
			sendMessage(evt)
		}
else{
log.debug "message NOT sent button pressed within 15 seconds"	
            def nowlastTime = now() - lastTime
	        log.debug "nowlasttime $nowlastTime"
		}

}

private sendMessage(evt) {
	def msg = messageText ?: defaultText(evt)
	log.debug "$evt.name:$evt.value, '$msg'"
	log.debug "sending push"
	atomicState[evt.deviceId] = now()
 	sendPush(msg)
}

private defaultText(evt) {
	evt.descriptionText
}

private getIncludeArticle() {
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}
