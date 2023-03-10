/**
 *
 *  Notify Me When It Opens
 *
 *  Author: Don Yonce
 */
definition(
    name: "When Mail Arrives",
    namespace: "dcyonce",
    author: "Don Yonce",
    description: "Send a Text message when the mail arrives.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-MailboxMonitor.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-MailboxMonitor@2x.png"
)

preferences {
	section("When Mail Arrives - dcyonce"){}
	section("When the door opens..."){
		input "mailbox", "capability.contactSensor", title: "Where?"
	}
  section("Notifications") {
    input "phone1", "phone1", title: "Phone #1", required: false
    input "phone2", "phone2", title: "Phone #2", required: false
  }    
}

def installed()
{
	subscribe(mailbox, "contact.open", contactOpenHandler)
}

def updated()
{
	unsubscribe()
	subscribe(mailbox, "contact.open", contactOpenHandler)
}

def contactOpenHandler(evt) {
	def msg = "Your ${mailbox.label ?: mailbox.name} was opened"
	log.trace "$evt.value: $evt, $settings"

	log.debug "$mailbox was opened, sending push message to user"
	//sendPush(msg)
    log.debug(msg)
    if (phone1) sendSms(phone1, msg)
    if (phone2) sendSms(phone2, msg)
}