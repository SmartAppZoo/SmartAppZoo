definition(
    name: "Notify Me When Things Open/Close",
    namespace: "findmory",
    author: "Mike Ory",
    description: "Get a text message sent to your phone when an open/close sensor is opened or closed.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png"
)

preferences {
  section("When the door closes..."){
    input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
  }
  section("When the door opens..."){
    input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
  }
  section("Send a text message to...") {
    input("recipients", "contact", title: "Send notifications to") {
      input "phone1", "phone", title: "Phone number?"
      input "phone2", "phone", title: "Phone number?", required: false
    }
  }
}

def installed()
{
  subscribe(contactClosed, "contact.closed", eventHandler)
  subscribe(contact, "contact.open", eventHandler)
}

def updated()
{
  unsubscribe()
  subscribe(contactClosed, "contact.closed", eventHandler)
  subscribe(contact, "contact.open", eventHandler)
}

def eventHandler(evt) { 
 
  // log.trace("atomicState:  ${atomicState.lastExecution}")
  // log.trace("atomicDevice: ${atomicState.lastDevice}")
  // log.trace("evt:  ${evt.value}")
  // log.trace("device: ${evt.displayName}")
  
  // if the evt.value is a repeat FOR THE SAME device - ignore
  if (evt.value == atomicState.lastExecution && evt.displayName == atomicState.lastDevice) {
  	log.trace('repeat so skipping')
  	return
  } else {
  	// update state with value ('open' / 'close') for latest event 
    atomicState.lastExecution = evt.value
    atomicState.lastDevice = evt.displayName
  }

  
  def now = new Date()
  def nowFormatted = now.format("EEE, MMM d h:mm:ss a",TimeZone.getTimeZone('America/New_York'))
  // log.info("sending SMS at ${nowFormatted}")
  // log.info("contact device: ${evt.displayName}")
  // log.info("contact value: ${evt.value.toUpperCase()}")
  
  sendSms(phone1, "${nowFormatted} \n${evt.displayName} ${evt.value.toUpperCase()}")
  if (phone2 != "") {
	sendSms(phone2, "${nowFormatted} \n${evt.displayName} ${evt.value.toUpperCase()}")
  }
  

}

