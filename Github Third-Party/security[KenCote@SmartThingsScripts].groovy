definition(
    name: "Security",
    namespace: "KenCote",
    author: "Ken Cote",
    description: "Is someone in the house?",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn.png"
)

preferences {
	section("Phones?") {
    	input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
    }
    section("Which door?") {
        input "doors", "capability.contactSensor", required: true, title: "Which doors?", multiple: true
    }
    section("Open/close garage door..."){
		input "gdc1", "capability.garageDoorControl", multiple: true
	}
    section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Push Notification when Opened?"
    }
    section("Send Texts?") {
    	input "phones", "enum", 
			title: "Phones",
			multiple: true,
			options: [ 
            	['15083200375': 'Yucchi'],
                ['15083536659': 'Ken']
			],
            required: false
    }
}

def installed() {
    initialize()
        log.debug "init"  
}

def updated() {
    initialize()
}

def initialize() {
    subscribe(doors, "contact.open", doorOpenHandler)
    subscribe(gdc1, "door.open", doorOpenHandler)
}

def doorOpenHandler(evt)
{   
	if (evt.isStateChange() && evt.value == "open")
	{
        def message = "SMARTTHINGS: The ${evt.displayName} is open!"

        if (sendPush) 
        {
            log.info "Pushing ${message}"
            sendPush(message)
        }	

        for (def p = 0; p < phones.size(); p++) 
        {
            log.info "Sending SMS to ${phones[p]}: ${message}"
            sendSms(phones[p], message)
        }  
	}
}

/*
def doorOpenHandler(evt) 
{   
	def list = []
	log.info "${evt.displayName}"
    	log.info "${evt.name}"
        	log.info "${evt.value}"
            
    for (int i = 0; i < presence1.size(); i++)
    {
    	if (presence1[i].currentPresence == "present")
        {
        	log.info "${presence1[i]} is home!"
        }
    }
        
	for (int i = 0; i < doors.size(); i++)
    {
    	log.info "Initial eval"
    	log.info "${doors[i]}"
        log.info "${doors[i].currentContact == "open"}"
        
    	if (doors[i].currentContact == "open")
        {
           	log.info "${doors[i].displayName} is showing as open."
            list << doors[i]
        }
    }
    
    pause(1000)
    log.info "Secondary eval"
    
    for (int i = 0; i < list.size(); i++)
    {
    	log.info "${list[i]}"
        log.info "${list[i].currentContact == "open"}"
        
    	if (list[i].currentContact == "open")
        {
            def message = "SMARTTHINGS: The ${list[i].displayName} is open!"
           	log.info "${list[i].displayName} is still showing as open, alerting."
            
            if (sendPush) 
            {
                sendPush(message)
            }
           
			for (def p = 0; p < phones.size(); p++) 
            {
            	log.info "sending SMS to ${phones[p]}: ${message}"
            	sendSms(phones[p], message)
            }   
        }
    } 
} */