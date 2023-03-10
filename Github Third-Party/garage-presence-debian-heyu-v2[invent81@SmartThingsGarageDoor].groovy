definition(
    name: "Garage presence debian heyu v2",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Open garage, send txt when I arrive",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {	
	
	section("When a presence sensor arrives or departs this location..") {
		input "presence", "capability.presenceSensor", title: "Which sensor?"        
	}    
	section("Send a text message to...") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone1", "phone", title: "Phone number?"
        }
	}    
    section("x10code..") {
		input "x10code", "x10code", title: "What code?"
	}
    section("Garage door") {
		input "garage_sensor", "capability.contactSensor", title: "Which sensor?"
        }   
}

def installed() {
	subscribe(presence, "presence", presenceHandler) 
}
def updated() {
	unsubscribe()
	subscribe(presence, "presence", presenceHandler)
}
def presenceHandler(evt) {
	if (evt.value == "present") {
		log.debug "${presence.label ?: presence.name} has arrived at the ${location}"
        	// Don't send a continuous stream of text messages     
            def deltaSeconds = 3
            def timeAgo = new Date(now() - (1000 * deltaSeconds))
			def recentEvents = presence.eventsSince(timeAgo)
				log.debug "Found ${recentEvents?.size() ?: 0} events in the last $deltaSeconds seconds"
			def alreadySentSms = recentEvents.count { it.value && it.value == "present" } > 1                        
			//if (alreadySentSms) {
				//log.debug "SMS already sent within the last $deltaSeconds seconds"
            	//sendSms(phone1, "Arrival, sms already sent ")
              if (garage_sensor.currentContact == "open") {
				log.debug "Door already open"
            	sendSms(phone1, "Door already open")                
			} else {
        		//sendSms(phone1, "${presence.label ?: presence.name} has arrived ${location}")                
                //pause(1000)
                sendSms(phone1, "opening garage now")
                //pause(1000)
                
           		def params = [
    				uri: "http://vpn.milltrek.net:12125",
    				path: "/cgi-bin/bluelava/bluelava.cgi",
            		contentType: 'application/json',
                    query: [action:'on', device: "${x10code}"]
					]               
				try {
                httpGet(params) { resp ->
        		resp.headers.each {
        		log.debug "${it.name} : ${it.value}"
    			}
    			log.debug "response contentType: ${resp.contentType}"
    			log.debug "response data: ${resp.data}"
            
    			}
				} catch (e) {
                log.error "something went wrong: $e"
          		}
            }            
       	} else if (evt.value == "not present") {
		//log.debug "${presence.label ?: presence.name} has left the ${location}"
        	def deltaSeconds = 3
			def timeAgo = new Date(now() - (1000 * deltaSeconds))
			def recentEvents = presence.eventsSince(timeAgo)
			log.debug "Found ${recentEvents?.size() ?: 0} events in the last $deltaSeconds seconds"
			def alreadySentSms = recentEvents.count { it.value && it.value == "not present" } > 1
			if (alreadySentSms) {
				log.debug "SMS already sent within the last $deltaSeconds seconds"
            	//sendSms(phone1, "Departure, sms already sent ")
			} else {        		
        		//sendSms(phone1, "${presence.label ?: presence.name} has left ${location}")
                log.debug "${presence.label ?: presence.name} has left the ${location}"
        	}
        }
}  
